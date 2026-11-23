# Movimientos, analítica y alertas de inventario con Kafka

El historial registra el stock anterior, el nuevo y su diferencia para cada alta o cambio de stock confirmado. Se consulta con el JWT de Microsoft Entra ID que ya utiliza la API de inventario. Las ediciones que conservan el stock y las eliminaciones no generan movimientos. Los datos precargados tampoco generan eventos retrospectivos.

## Flujo y clases

1. `ProductServiceImpl` registra el alta o cambio en la transacción JPA. `ProductRepository.findForStockUpdate` bloquea la fila durante la actualización para leer el stock anterior de forma consistente bajo escrituras concurrentes.
2. `InventarioMovimientoEvent` incluye UUID, producto, SKU, nombre, stocks, diferencia, tipo, fecha UTC y el identificador opaco `sub` del JWT cuando existe. Un alta genera `CREACION` desde cero, incluso si el stock inicial es cero. Los cambios generan `ENTRADA` o `SALIDA` según el signo. El contrato admite `AJUSTE`.
3. `InventarioMovimientoProducer` publica el JSON después del commit con el ID de producto como clave. Spring Boot configura `KafkaTemplate`, `ProducerFactory` y `ConsumerFactory` con las dependencias y propiedades de `pom.xml` y `application.yml`.
4. `InventarioKafkaConfig` crea `inventario.movimientos` con tres particiones, dos réplicas por defecto, retención de 604800000 ms (7 días) y política `delete`. Los mensajes con la misma clave se escriben en la misma partición para una configuración de particiones dada.
5. `InventarioMovimientoConsumer` usa `@KafkaListener`, el grupo `inventario-historial-group` y confirmación de offsets por registro. Acepta el DTO fijo y omite las cabeceras de tipo del productor. Valida campos, clave, diferencia y tipo antes de invocar el servicio.
6. `MovimientoInventarioService` y `MovimientoInventarioRepository` guardan `MovimientoInventario` en la base actual. El UUID es la clave primaria y garantiza unicidad. Si una entrega concurrente pierde la carrera de inserción, el reintento puede reconocer el UUID ya guardado. El historial conserva una copia de los datos del producto aunque se elimine el original.
7. `MovimientoInventarioController` expone las consultas paginadas. Requiere autenticación con las reglas existentes de lectura. Los permisos actuales para crear y modificar productos siguen aplicando.

El flujo RabbitMQ publica `StockModificadoEvent` después de confirmar un cambio de stock. Un rollback ya no genera notificaciones de un cambio que no se guardó. Los exchanges, colas, bindings y DLQ siguen definidos en `RabbitMQConfig`, incluidas las colas complementarias `stock-alert`, `audit-log` y `dead-letter`. El consumidor Kafka solo escribe en la base de datos.

`InventarioAlertaProducer` escucha el mismo evento transaccional y, después del commit, envía un `InventarioAlertaEvent` a `inventario.alertas` cuando `stockNuevo <= umbral`. Un alta con stock crítico genera alerta porque representa la creación del inventario del producto. El evento conserva el UUID del movimiento y lleva `productoId`, `sku`, `nombre`, `stockActual`, `umbral`, `fecha` y `usuarioResponsable`. La clave Kafka sigue siendo el ID del producto. El productor Kafka no interactúa con RabbitMQ.

`InventarioAlertaConsumer` usa el grupo `inventario-alertas-group`. Valida el DTO, la clave del registro y la condición de stock crítico. Guarda las alertas en `alertas_inventario`, cuya clave primaria es el UUID del evento. La lectura autenticada `GET /api/v1/alertas?page=0&size=20` ordena por fecha descendente.

Ambos tópicos se declaran con tres particiones, dos réplicas por defecto, siete días de retención y política `delete`. Los listeners usan `ErrorHandlingDeserializer` con un tipo fijo por suscripción. El historial recibe el tipo predeterminado de `application.yml`. El listener de alertas lo sustituye por `InventarioAlertaEvent`. Se ignoran los encabezados de tipos.

### Diagnóstico de alertas al pasar de 17 a 5

Angular clasifica como **Stock Bajo** las existencias entre 1 y 10. Kafka y las notificaciones críticas de RabbitMQ usan `app.rabbitmq.stock-critico-umbral`, enlazado con `RABBITMQ_STOCK_CRITICO_UMBRAL` y predeterminado en **3**. Con ese umbral, el cambio de 17 a 5 genera un movimiento y la notificación RabbitMQ de stock modificado. Kafka no publica una alerta crítica para ese cambio. Un cambio confirmado de 5 a 3 sí debe publicar una alerta Kafka y solicitar reposición en RabbitMQ.

`INVENTARIO_KAFKA_ENABLED=true` se resuelve a `app.kafka.enabled=true` en `application.yml`. Con el productor habilitado, al iniciar aparece `Alertas Kafka habilitadas topic=inventario.alertas umbralCritico=3`, con el valor efectivo. El argumento de Spring Boot `--logging.level.com.retail.inventory.service.impl.InventarioAlertaProducer=DEBUG` permite ver decisiones de omisión sin imprimir el payload. Usa el log de inicio de la instancia correspondiente para confirmar posibles overrides.

En la instalación local se observaron los tres grupos Kafka con miembros activos, lag cero para historial y analítica en la partición con datos y offsets finales en cero en alertas. RabbitMQ tenía los bindings esperados y un consumidor conectado a cada cola principal. Esa inspección fue de solo lectura. El valor exacto de un posible override de entorno del proceso Java existente no se obtuvo de la inspección de parámetros de lanzamiento.

Los cambios de esta revisión afectan `InventarioAlertaProducer.java`, `ProductServiceImpl.java`, `inventory.component.html` e `InventarioKafkaIntegrationTest.java`. Se agregaron `InventarioAlertaProducerTest.java` e `InventarioMensajeriaAisladaTest.java`.

`mvn verify` con JDK 21 compiló el backend y aprobó 45 pruebas. La prueba con RabbitMQ dedicado se omitió en la suite estándar y se ejecutó aparte con éxito. En esa prueba, el cambio de 5 a 3 llegó a un RabbitMQ real descartable, con ACK de notificaciones y reposición, y a Kafka embebido, que guardó la alerta, el historial y las métricas en H2 en memoria. `npm run build` completó la compilación Angular de producción. La suite estándar también verificó que 17 → 5 conserva el movimiento sin publicar una alerta. El archivo H2 persistente del entorno local se conservó. La prueba conjunta no utilizó el JWT ni los datos del inventario activo.

Para inspeccionar el entorno activo sin generar eventos:

```powershell
docker compose -f docker/docker-compose.kafka.yml exec -T kafka1 kafka-get-offsets --bootstrap-server kafka1:29092 --topic inventario.alertas
docker compose -f docker/docker-compose.kafka.yml exec -T kafka1 kafka-consumer-groups --bootstrap-server kafka1:29092 --describe --all-groups
docker exec rabbit1 rabbitmqctl list_queues name consumers messages
docker exec rabbit1 rabbitmqctl list_bindings source_name destination_name routing_key
```

## Ejecutar en Windows PowerShell

Desde la raíz, con Docker Desktop y JDK 17 o 21. Selecciona el JDK con `JAVA_HOME` si la terminal usa otra versión. El Maven Wrapper descarga Maven 3.9.8 en su primera ejecución.

Con un clúster RabbitMQ existente, conserva sus contenedores y usa el usuario y la contraseña vigentes. Si están detenidos, usa `docker start rabbit1 rabbit2`. Para crear un clúster nuevo, carga `RABBITMQ_USER` y `RABBITMQ_PASS` mediante `Get-Credential` antes de ejecutar `docker compose -f docker/docker-compose.yml up -d`. Esas variables inicializan el usuario de un broker nuevo. Cambiarlas no actualiza la contraseña de un usuario que ya existe en el volumen.

```powershell
docker compose -f docker/docker-compose.kafka.yml up -d --wait
.\backend\mvnw.cmd -f backend/pom.xml verify
```

Antes de arrancar el backend, carga las credenciales con el [bloque PowerShell del README](README.md#paso-1-ejecución-del-backend-spring-boot). Spring prioriza `SPRING_RABBITMQ_USERNAME` y `SPRING_RABBITMQ_PASSWORD` sobre `RABBITMQ_USER` y `RABBITMQ_PASS`. Usa las variables de Entra ID del README si tu aplicación utiliza otro tenant o client ID.

```powershell
$env:INVENTARIO_KAFKA_ENABLED = 'true'
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:file:./data/inventory-kafka;DB_CLOSE_ON_EXIT=FALSE'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'update'
$env:SPRING_SQL_INIT_MODE = 'never'
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Este bloque es para el archivo H2 existente. Usa `SPRING_SQL_INIT_MODE=always` solo al crear un archivo vacío, porque el script inserta datos semilla con IDs fijos. Con el H2 en memoria predeterminado el historial desaparece al cerrar el backend. El perfil `aws` conserva la conexión PostgreSQL del proyecto y puede habilitar Kafka con las mismas variables. Conserva la base y el grupo de consumidores juntos, ya que los offsets Kafka sobreviven a la pérdida de una base H2 en memoria. Si reconstruyes la base y quieres reprocesar lo que aún conserva Kafka, usa un grupo nuevo y revisa los IDs de producto que existían en esa base.

| Variable | Predeterminado | Uso |
|---|---|---|
| `INVENTARIO_KAFKA_ENABLED` | `false` | Habilita productores, consumidores, declaración de ambos tópicos y administración Kafka |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092,localhost:9093` | Brokers desde Spring Boot |
| `KAFKA_HISTORIAL_GROUP` | `inventario-historial-group` | Grupo del consumidor |
| `KAFKA_ANALITICA_GROUP` | `inventario-analitica-group` | Grupo dedicado a la analítica. Debe diferir del de historial |
| `KAFKA_ALERTAS_GROUP` | `inventario-alertas-group` | Grupo del consumidor de alertas |
| `KAFKA_TOPIC_REPLICATION_FACTOR` | `2` | Réplicas al crear ambos tópicos. Usa 1 para un broker de pruebas |
| `RABBITMQ_STOCK_CRITICO_UMBRAL` | `3` | Umbral compartido para stock crítico |
| `KAFKA_ALLOW_TOPIC_DELETION` | `false` | Habilita eliminación administrativa de tópicos de demostración |
| `KAFKA_PORT_1`, `KAFKA_PORT_2` | `9092`, `9093` | Puertos externos del Compose. Actualiza también los bootstrap servers si cambias los puertos |
| `KAFKA_ADVERTISED_HOST` | `localhost` | Host externo anunciado por los brokers |
| `KAFKA_UI_PORT` | `8085` | Puerto local de Kafka UI |

`docker/docker-compose.kafka.yml` usa Confluent Platform 7.6.11 en modo ZooKeeper, dos brokers y Kafka UI v0.7.2. Los brokers y ZooKeeper tienen volúmenes y comprobaciones de salud. Kafka UI usa `kafka1:29092,kafka2:29092` en la red privada del Compose. Los puertos del host están vinculados a loopback para la demostración local. [Referencia de configuración Docker de Confluent](https://docs.confluent.io/platform/7.6/installation/docker/config-reference.html)

## Demostración con Postman y Kafka UI

1. Envía `POST http://localhost:8080/api/v1/products` con un Bearer JWT autorizado para crear productos y un SKU libre. Confirma primero que la categoría existe. Ejemplo de cuerpo para la demostración:

   ```json
   {"sku":"KAFKA-DEMO-01","name":"Producto demo Kafka","price":1000,"stock":8,"categoryId":1}
   ```

2. Copia el `id` de la respuesta 201. Envía `PUT http://localhost:8080/api/v1/products/{id}` con un JWT autorizado para editar y el mismo cuerpo con `"stock":3`. Espera HTTP 200.
3. Envía `GET http://localhost:8080/api/v1/movimientos?productoId={id}&page=0&size=20&orden=desc` con un JWT válido. Cuando el consumidor termine verás un movimiento `SALIDA` de 8 a 3, diferencia −5, seguido por `CREACION`. Prueba también `GET /api/v1/products/{id}/movimientos?orden=asc`. Las respuestas incluyen `content` y `page`. El límite de página es 100 y el criterio secundario de orden es el UUID. Sin token ambas rutas devuelven 401.
4. Abre Kafka UI en `http://localhost:8085`. En **Topics → inventario.movimientos**, revisa las tres particiones, sus réplicas y los JSON en **Messages**. Busca la clave con el ID del producto. En **Consumers**, revisa `inventario-historial-group` y su lag.
5. Consulta `GET /api/v1/alertas?page=0&size=20`, luego revisa **Topics → inventario.alertas** y **Consumers → inventario-alertas-group**. Correlaciona el `eventId` de la alerta con la salida de stock del historial. Una edición posterior sin cambio de stock conserva el mismo historial y las mismas alertas.
6. Revisa la topología RabbitMQ existente y los logs de auditoría y notificaciones tras el PUT. Las colas pueden verse vacías si los consumidores procesan de inmediato.

Puedes inspeccionar la configuración efectiva del tópico con:

```powershell
docker compose -f docker/docker-compose.kafka.yml exec -T kafka1 kafka-topics --bootstrap-server kafka1:29092 --describe --topic inventario.movimientos
docker compose -f docker/docker-compose.kafka.yml exec -T kafka1 kafka-topics --bootstrap-server kafka1:29092 --describe --topic inventario.alertas
docker compose -f docker/docker-compose.kafka.yml exec -T kafka1 kafka-consumer-groups --bootstrap-server kafka1:29092 --describe --group inventario-alertas-group
```

## API de administración

La API usa el cliente oficial Kafka en el backend. Requiere un rol explícito `Admin` o `ADMIN` en el JWT de Entra ID. Sin token devuelve 401 y con un token que carece de ese rol devuelve 403. El acceso de administrador por defecto del proyecto no se aplica a estas rutas.

```powershell
$topics = 'http://localhost:8080/api/v1/admin/kafka/topics'
Invoke-RestMethod $topics -Authentication Bearer -Token $token
Invoke-RestMethod "$topics/inventario.alertas" -Authentication Bearer -Token $token
Invoke-RestMethod $topics -Method Post -Authentication Bearer -Token $token -ContentType 'application/json' `
  -Body '{"name":"demo.ep5","partitions":3,"replicationFactor":2,"retentionMs":86400000}'
Invoke-RestMethod "$topics/demo.ep5" -Authentication Bearer -Token $token
Invoke-RestMethod "$topics/demo.ep5?confirm=demo.ep5" -Method Delete -Authentication Bearer -Token $token
```

Usa PowerShell 7 y carga `$token` como `SecureString` desde un JWT válido. También puedes usar las mismas rutas en Postman. El detalle devuelve líder, réplicas, réplicas sincronizadas, retención y política de limpieza. La creación devuelve 201 y admite nombres `demo.*`, entre 1 y 12 particiones, entre 1 y 2 réplicas y retención positiva. La eliminación devuelve 204 y exige habilitar el flag antes de arrancar, usar un nombre `demo.*` y confirmarlo en el parámetro `confirm`. Reserva ese prefijo para datos descartables. Los tópicos de negocio e internos están protegidos.

La API devuelve 400 ante parámetros inválidos, 404 cuando el tópico no existe, 409 si el tópico ya existe o Kafka tiene la eliminación deshabilitada, y 503 si Kafka no confirma la operación. Tras un timeout de escritura, consulta el estado antes de repetir la petición.

## Analítica de inventario

`InventarioAnaliticaConsumer` escucha `inventario.movimientos` con `inventario-analitica-group`. El historial consume el mismo tópico en `inventario-historial-group` y conserva cada movimiento. La analítica usa el mismo DTO y validador, pero actualiza un resumen global y un registro por producto en `metricas_inventario`. Las consultas leen esas filas acumuladas. Los eventos y los movimientos históricos quedan fuera de las consultas de métricas.

Cada transacción de analítica bloquea la fila del resumen y guarda a la vez el resumen, la métrica del producto y el UUID en `eventos_analitica_procesados`. Una repetición del UUID conserva los mismos contadores. La variación real `stockNuevo - stockAnterior` determina entrada o salida y su cantidad de unidades. Un alta con stock cero cuenta como movimiento sin entrada ni salida. `ultimoMovimiento` es la mayor fecha del evento y `ultimoProcesamiento` indica cuándo el consumidor aplicó el último evento único.

Kafka confirma el offset después de completar la transacción. El listener omite JSON ilegible y eventos inválidos con un log de tópico, partición y offset. Ante un fallo de base de datos intenta la transacción hasta tres veces. Si persiste el fallo, se detiene solo el listener de analítica y el offset del evento queda pendiente. Tras corregir la causa, reinicia el backend conservando el grupo y la base. Para un grupo nuevo, la analítica comienza por el primer evento aún retenido por Kafka. El historial anterior a esa retención queda disponible en su consulta, pero no se incorpora retroactivamente a la analítica.

Todos los endpoints requieren un JWT válido de CloudStock. Cualquier usuario autenticado puede consultar.

| Ruta GET | Respuesta |
|---|---|
| `/api/v1/analytics/inventory/summary` | Movimientos, entradas, salidas, unidades ingresadas y retiradas, fecha del último movimiento y fecha de procesamiento |
| `/api/v1/analytics/inventory/products?page=0&size=20` | Métricas paginadas por ID de producto. `size` admite 1 a 100 |
| `/api/v1/analytics/inventory/top-products?limit=5` | Productos ordenados por movimientos descendentes e ID ascendente en caso de empate. `limit` admite 1 a 100 |

El modo H2 en memoria pierde sus métricas al salir. Para demostrarlas tras un reinicio, usa el mismo archivo H2 y el mismo grupo. Usa `SPRING_JPA_HIBERNATE_DDL_AUTO=update` y, para un archivo ya inicializado, `SPRING_SQL_INIT_MODE=never`. Mantén los offsets y el archivo de datos juntos. Los UUID procesados crecen con los eventos únicos y el resumen global serializa las escrituras. Si el broker elimina un evento por retención mientras el consumidor está detenido, ese grupo ya no puede recuperarlo de Kafka.

### Demostración de las tres ideas

1. **Historial.** Inicia el Compose Kafka y el backend con Kafka y H2 persistente. Inicia Angular con `npm start` en `frontend` e inicia sesión. Crea un producto de demostración con stock 0, súbelo a 8 y bájalo a 3. Comprueba en `GET /api/v1/products/{id}/movimientos` el alta sin variación, la entrada de 8 unidades y la salida de 5. En Kafka UI (`http://localhost:8085`), busca la clave del producto en `inventario.movimientos` y captura los offsets de `inventario-historial-group`.
2. **Analítica.** Antes de los cambios, abre **Analítica** y anota el resumen. Después, pulsa **Actualizar** o espera el refresco de 15 segundos. Sin otros cambios simultáneos, compara el incremento de 3 movimientos, 1 entrada, 1 salida, 8 unidades ingresadas y 5 retiradas. Consulta el conteo del producto y captura el offset de `inventario-analitica-group` en el mismo tópico. Reinicia la instancia existente con el mismo H2 para mostrar que conserva las métricas.
3. **Alertas y administración.** Con el umbral predeterminado, el alta en cero y la reducción a 3 generan alertas. Busca sus UUID en `inventario.alertas` y en `GET /api/v1/alertas`. Con un Admin explícito, consulta el detalle de ambos tópicos mediante la API administrativa. Usa las colas y los logs RabbitMQ para mostrar que el cambio de stock sigue alimentando auditoría y notificaciones. Kafka conserva los flujos de historial, analítica y alertas persistidas. RabbitMQ mantiene la entrega de notificaciones y tareas operativas.

## Errores y límites

- Los productores usan `acks=all`, idempotencia del cliente Kafka y tiempos de bloqueo y entrega acotados. Registran el UUID y el producto en caso de error. Si el proceso cae o Kafka queda indisponible después del commit, se puede perder el evento. Cada publicación es independiente. No hay outbox transaccional ni garantía de entrega exactamente una vez.
- El consumidor reintenta fallos recuperables hasta dos veces con un segundo de backoff. Los registros inválidos o con JSON ilegible se omiten sin reintento. Si se agotan los reintentos se registra tópico, partición, offset y tipo de error, se avanza el offset y continúa el consumo. Para recuperar un registro omitido se requiere releer desde su offset antes de que venza la retención. [Manejo de errores de Spring Kafka](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html)
- El orden por clave es el orden de publicación en Kafka. Los callbacks posteriores al commit de escrituras concurrentes pueden ejecutarse en otro orden. La API ordena por la fecha del evento.
- La retención Kafka se aplica al tópico. El historial en la base de datos se conserva según el ciclo de vida de esa base.
- `InventarioKafkaIntegrationTest` usa Kafka embebido, MockMvc, JWT de prueba y H2 aislado. Cubre 17 → 5 → 3, rollback, ambos DTO, persistencia, consulta, duplicados, JSON inválido, reintentos y API administrativa. Comprueba con un mock que RabbitMQ recibe los cambios confirmados. `InventarioAlertaProducerTest` verifica el umbral por defecto y un valor externo de 5.
- `InventarioMensajeriaAisladaTest` usa un RabbitMQ dedicado, Kafka embebido y H2 en memoria para probar el mismo cambio contra ambos flujos. Para ejecutarla, apunta las variables `SPRING_RABBITMQ_*` a un broker descartable, activa `CLOUDSTOCK_ISOLATED_RABBIT_TEST=true` en esa terminal y ejecuta `mvn -f backend/pom.xml -Dtest=InventarioMensajeriaAisladaTest test`. La prueba declara la topología de la aplicación. Mantén esa variable desactivada cuando apuntes a servicios compartidos.

## Evidencias para la Evaluación Parcial N.º 5

1. Captura los servicios de Compose y los detalles de ambos tópicos con tres particiones, dos réplicas y retención de siete días.
2. Captura el alta con stock normal, el cambio a stock crítico, ambos mensajes en Kafka UI y el UUID compartido en las consultas HTTP.
3. Captura ambos grupos con sus offsets y lag una vez procesados los mensajes.
4. Muestra 401 sin JWT, 403 con JWT sin rol administrador, y 200/201 con el rol correcto. Demuestra 204 solo con un tópico descartable y 403 al intentar gestionar la eliminación de un tópico de negocio. Oculta tokens y credenciales.
5. Adjunta el resumen de Maven y evidencia de la continuidad de RabbitMQ. Los logs de las pruebas con Kafka embebido muestran el procesamiento tras un registro inválido.

## Verificación de ambos flujos

- Maven `verify` con JDK 21 generó el JAR y ejecutó 39 pruebas, sin fallas, errores ni omisiones. Kafka embebido y H2 verificaron el flujo de ambos tópicos, el avance de los offsets y las operaciones administrativas de lectura y creación. La prueba de eliminación verifica la llamada al cliente con una respuesta simulada, porque el broker embebido en Windows falla al renombrar el directorio del tópico.
- En el clúster Docker local se declaró `inventario.alertas` y se inspeccionaron ambos tópicos. Los dos tenían tres particiones, dos réplicas sincronizadas por partición, retención de 604800000 ms y política `delete`. Kafka UI devolvió HTTP 200 en `/actuator/health`.
- La prueba con JWT de Microsoft Entra ID y publicación contra el clúster Docker queda como evidencia de la demostración manual.

## Verificación histórica del flujo de movimientos

- `mvn -f backend/pom.xml clean verify` generó el JAR y ejecutó 37 pruebas sin fallas, errores ni omisiones.
- En Docker Desktop, se levantaron ZooKeeper, los dos brokers y Kafka UI. El JAR creó `inventario.movimientos` con tres particiones, dos réplicas sincronizadas por partición, retención de siete días y política `delete`. El grupo se observó en estado `Stable` con un consumidor.
- Kafka UI respondió HTTP 200 en `/actuator/health`. El endpoint de historial del JAR devolvió HTTP 401 sin token. RabbitMQ tenía un consumidor conectado a cada cola principal de auditoría, notificaciones y reposición, con las tres DLQ declaradas.
- Las consultas autenticadas se ejecutaron con JWT de prueba en MockMvc. La demostración manual con un token real de Entra ID queda descrita en los pasos de Postman.
- Una segunda ejecución de Maven pasó las 37 pruebas del repositorio y dos verificaciones adicionales con Kafka y RabbitMQ en contenedores aislados. La lectura directa de Kafka y del archivo H2 cerrado confirmó `CREACION` de 0 a 8 y `SALIDA` de 8 a 3 para el mismo producto. RabbitMQ procesó auditoría, alerta de stock, reposición y el rechazo de un mensaje de prueba derivado a la DLQ.
