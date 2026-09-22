# Guía Completa de Pruebas en Postman
## Sistema de Gestión de Inventario (Cloud Native & Microsoft Entra ID)

Esta guía detalla el paso a paso para ejecutar las pruebas de la API RESTful en **Postman**, obtener el Token Bearer de Microsoft Entra ID (Azure AD), y todos los códigos HTTP de respuesta esperados para tu presentación.

---

## 🚀 Método Rápido: Colección Lista para Importar

Hemos creado el archivo listo para importar en Postman:
📁 **[Retail_Inventory_API.postman_collection.json](file:///c:/Users/gtoro/Desktop/Cloud%20Native%201/Retail_Inventory_API.postman_collection.json)**

1. Abre **Postman**.
2. Haz clic en el botón superior **Import**.
3. Arrastra y suelta el archivo `Retail_Inventory_API.postman_collection.json`.
4. En las variables de la colección encontrarás:
   - `BASE_URL`: `https://32.193.45.223/api/v1` (o local `http://localhost:8080/api/v1`).
   - `BEARER_TOKEN`: El token JWT obtenido en el paso siguiente.

---

## 🔑 1. ¿Cómo Obtener el Bearer Token para Postman?

El backend está protegido con **OAuth 2.0 / JWT**. La forma más rápida y confiable de obtener un token válido es:

### Método A: Copiar el Token desde el Navegador (Recomendado)
1. Abre tu aplicación Angular en el navegador (en AWS: `https://32.193.45.223/` o en local `http://localhost:4200/`).
2. Haz clic en **Iniciar Sesión (Azure AD)** e ingresa tus credenciales institucionales de DuocUC.
3. Presiona **F12** para abrir las *Herramientas de Desarrollador* (DevTools).
4. Ve a la pestaña **Red (Network)**.
5. Filtra por `Fetch/XHR` y haz clic en cualquier petición (por ejemplo `products` o `categories`).
6. En la pestaña **Encabezados (Headers)** > **Encabezados de solicitud (Request Headers)**, busca la cabecera:
   ```text
   Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIs...
   ```
7. Copia todo el texto que comienza después de la palabra `Bearer ` (la cadena `eyJ...`).
8. En Postman:
   - En tu petición o en la Colección, ve a la pestaña **Authorization**.
   - Selecciona **Type**: `Bearer Token`.
   - Pega el token en el campo **Token**.

*(Alternativa: En DevTools ve a **Aplicación (Application)** > **Almacenamiento Local (Local Storage)** y copia el `accessToken` almacenado por MSAL).*

---

## 📋 2. Catálogo de Endpoints, Cuerpos y Códigos HTTP

### A. Módulo de Categorías (`/api/v1/categories`)

#### 1. Listar todas las Categorías
* **Método:** `GET`
* **URL:** `{{BASE_URL}}/categories`
* **Código Esperado:** `200 OK`
* **Descripción:** Retorna el listado completo de categorías en formato JSON.

#### 2. Obtener Categoría por ID
* **Método:** `GET`
* **URL:** `{{BASE_URL}}/categories/1`
* **Código Esperado:** `200 OK`
* **Descripción:** Retorna los datos de la categoría con ID 1.

#### 3. Crear Nueva Categoría
* **Método:** `POST`
* **URL:** `{{BASE_URL}}/categories`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**
  ```json
  {
    "name": "Iluminación y Energía",
    "description": "Generadores de emergencia, paneles solares portátiles y linternas de alta potencia"
  }
  ```
* **Código Esperado:** `201 Created`
* **Descripción:** Requiere rol `ROLE_Admin` o permisos de creación. Retorna la categoría creada con su ID asignado.

#### 4. Actualizar Categoría
* **Método:** `PUT`
* **URL:** `{{BASE_URL}}/categories/1`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**
  ```json
  {
    "name": "Puntos de Venta (POS) y Cajas",
    "description": "Terminales táctiles actualizadas, escáneres láser e impresoras térmicas de alto rendimiento"
  }
  ```
* **Código Esperado:** `200 OK`

#### 5. Eliminar Categoría
* **Método:** `DELETE`
* **URL:** `{{BASE_URL}}/categories/10`
* **Código Esperado:** `204 No Content`
* **Descripción:** Elimina la categoría especificada. No retorna cuerpo.

---

### B. Módulo de Productos (`/api/v1/products`)

#### 1. Listar Todos los Productos
* **Método:** `GET`
* **URL:** `{{BASE_URL}}/products`
* **Código Esperado:** `200 OK`

#### 2. Filtrar Productos por Categoría
* **Método:** `GET`
* **URL:** `{{BASE_URL}}/products?categoryId=1`
* **Código Esperado:** `200 OK`

#### 3. Buscar Productos por Texto
* **Método:** `GET`
* **URL:** `{{BASE_URL}}/products?search=POS`
* **Código Esperado:** `200 OK`

#### 4. Obtener Producto por ID
* **Método:** `GET`
* **URL:** `{{BASE_URL}}/products/1`
* **Código Esperado:** `200 OK`

#### 5. Crear Nuevo Producto
* **Método:** `POST`
* **URL:** `{{BASE_URL}}/products`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**
  ```json
  {
    "sku": "POS-NEW-99",
    "name": "Terminal POS Inteligente Android 12",
    "description": "Terminal portátil inalámbrico con impresora térmica integrada de 58mm y conectividad 4G/Wi-Fi",
    "price": 189990.00,
    "stock": 15,
    "categoryId": 1
  }
  ```
* **Código Esperado:** `201 Created`

#### 6. Actualizar Producto Existente
* **Método:** `PUT`
* **URL:** `{{BASE_URL}}/products/1`
* **Headers:** `Content-Type: application/json`
* **Body (raw JSON):**
  ```json
  {
    "sku": "POS-1001",
    "name": "Terminal Táctil All-in-One 15.6\" Pro",
    "description": "Pantalla táctil capacitiva True-Flat, Intel Celeron J4125 16GB RAM, SSD 256GB, base de aluminio reforzado",
    "price": 499990.00,
    "stock": 20,
    "categoryId": 1
  }
  ```
* **Código Esperado:** `200 OK`

#### 7. Eliminar Producto
* **Método:** `DELETE`
* **URL:** `{{BASE_URL}}/products/29`
* **Código Esperado:** `204 No Content`

---

## 🛡️ 3. Códigos de Estado HTTP para la Presentación (Respuestas de la API)

Para tu presentación, los evaluadores suelen exigir demostrar tanto el **camino feliz (Happy Path)** como el **manejo de excepciones y seguridad**:

| Código HTTP | Nombre Estándar | ¿Cuándo ocurre en el Proyecto? | Ejemplo de Prueba en Postman |
| :--- | :--- | :--- | :--- |
| **`200 OK`** | Exitoso | Consultas `GET` exitosas o actualizaciones `PUT`. | `GET /api/v1/products` |
| **`201 Created`** | Creado | Registro exitoso de producto o categoría con `POST`. | `POST /api/v1/products` |
| **`204 No Content`** | Sin Contenido | Eliminación exitosa de un recurso con `DELETE`. | `DELETE /api/v1/products/29` |
| **`400 Bad Request`** | Petición Inválida | Fallo de validación Jakarta (`@NotBlank`, precio `<= 0`, stock negativo). | `POST /api/v1/products` con `price: -100` |
| **`401 Unauthorized`** | No Autorizado | Petición sin cabecera `Authorization`, token ausente, expirado o con firma inválida. | `GET /api/v1/products` seleccionando *No Auth* |
| **`403 Forbidden`** | Prohibido | Usuario autenticado pero que carece del rol `ROLE_Admin` para operaciones de escritura. | `POST /api/v1/products` con usuario sin rol |
| **`404 Not Found`** | No Encontrado | Solicitud de un ID de producto o categoría inexistente. | `GET /api/v1/products/99999` |
