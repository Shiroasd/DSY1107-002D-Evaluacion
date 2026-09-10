-- =========================================================================
-- Datos Iniciales de Prueba: Sistema de Inventario para Locales Comerciales
-- =========================================================================

-- Insercion de Categorias Comerciales (10 categorias completas, caracteres simples)
INSERT INTO categories (id, name, description) VALUES
(1, 'Puntos de Venta (POS)', 'Terminales tactiles, lectores de codigo de barras, gavetas de dinero e impresoras de tickets'),
(2, 'Equipos de Computo y Redes', 'Servidores locales, switches, routers comerciales y terminales de inventario'),
(3, 'Papeleria e Insumos de Caja', 'Rollos termicos de recibos, etiquetas adhesivas de precios y suministros de caja'),
(4, 'Bebidas y Refrigerados', 'Bebidas energeticas, refrescos, jugos y aguas listos para consumo en tienda'),
(5, 'Snacks y Confiteria', 'Chocolates, galletas, frutos secos y aperitivos empaquetados de rotacion rapida'),
(6, 'Seguridad y Camaras CCTV', 'Camaras domo IP, sensores de movimiento, grabadores DVR comerciales y alarmas antirrobo'),
(7, 'Audio e Iluminacion Comercial', 'Parlantes de ambientacion, amplificadores de sonido y paneles LED de bajo consumo'),
(8, 'Limpieza y Desinfeccion', 'Dispensadores de alcohol gel, toallas humedas industriales y quimicos de saneamiento'),
(9, 'Accesorios y Conectividad', 'Cables HDMI, adaptadores USB-C, soportes articulados para tablets y hubs multipuerto'),
(10, 'Embalaje y Despacho', 'Bolsas biodegradables con asa, cinta de embalaje reforzada y cajas de carton corrugado');

-- Insercion de Productos Comerciales con Precios en Pesos Chilenos (CLP)
INSERT INTO products (id, sku, name, description, price, stock, category_id) VALUES
-- Categoria 1: POS
(1, 'POS-1001', 'Terminal Tactil All-in-One 15.6"', 'Pantalla tactil capacitiva True-Flat, Intel Celeron J4125 8GB RAM, SSD 128GB, base de aluminio', 459990, 12, 1),
(2, 'POS-1002', 'Lector Laser 2D QR/Barcode Omnidireccional', 'Escaneo automatico manos libres de alta velocidad para codigos 1D y 2D QR/Datamatrix en pantalla y papel', 79990, 25, 1),
(3, 'POS-1003', 'Impresora Termica de Recibos 80mm ESC/POS', 'Velocidad de impresion 260 mm/s con autocorte silencioso, interfaces USB, Serial y Ethernet LAN', 119990, 18, 1),
(4, 'POS-1004', 'Gaveta Portamonedas Metalica Pesada', 'Estructura de acero con 5 compartimentos de billetes y 8 de monedas, apertura automatica con conector RJ11', 58990, 14, 1),
(5, 'POS-1005', 'Balanza Electronica Comercial 30kg', 'Doble visor LCD cliente/cajero con calculo de precio tara, precision de 5g y conexion serie RS-232', 189990, 6, 1),

-- Categoria 2: Equipos y Redes
(6, 'NET-2001', 'Router Wi-Fi 6 Gigabit Comercial Dual-Band', 'Router de alta concurrencia con soporte para portal cautivo de clientes, VLANs y firewall integrado', 89990, 15, 2),
(7, 'NET-2002', 'Switch 16 Puertos Gigabit Ethernet Metalico', 'Switch Plug-and-Play de montaje en rack o sobremesa con carcasa metalica robusta y disipacion pasiva', 64990, 10, 2),
(8, 'NET-2003', 'Sistema UPS / Respaldo de Energia 1500VA', 'Bateria de respaldo interactiva de 900W con 8 tomas protegidas y estabilizacion automatica de voltaje AVR', 165000, 8, 2),
(9, 'NET-2004', 'Terminal Colector de Datos Movil Rugged', 'Dispositivo Android 11 para toma de inventario con escaner laser Zebra 2D, certificacion IP67 anticaidas', 329000, 4, 2),

-- Categoria 3: Papeleria e Insumos
(10, 'SUP-3001', 'Pack x50 Rollos Papel Termico 80mm x 70mm', 'Papel termico libre de Bisfenol A (BPA Free) con alta nitidez de impresion y conservacion prolongada', 38990, 85, 3),
(11, 'SUP-3002', 'Pack x100 Rollos Termicos POS 57mm x 40mm', 'Rollos estandar compatibles con terminales bancarias Transbank y calculadoras termicas de cobro', 42990, 120, 3),
(12, 'SUP-3003', 'Pistola Etiquetadora Manual 8 Digitos', 'Incluye rodillo entintador de repuesto y 10.000 etiquetas autoadhesivas color rojo fluor', 24990, 30, 3),
(13, 'SUP-3004', 'Rollo Etiquetas Termicas Directas 50x25mm', 'Rollo de 1000 etiquetas autoadhesivas blancas para identificacion de productos y codigo de barras', 9990, 5, 3),

-- Categoria 4: Bebidas y Refrigerados
(14, 'BEV-4001', 'Agua Mineral de Manantial Sin Gas 500ml (x24)', 'Pack de 24 botellas pet 100% reciclables, agua mineral de origen cordillerano baja en sodio', 18990, 45, 4),
(15, 'BEV-4002', 'Bebida Energizante Premium Citrus 355ml (x12)', 'Pack de 12 latas con cafeina natural, taurina y complejo de vitaminas B para recarga de energia', 22990, 32, 4),
(16, 'BEV-4003', 'Te Helado Melocoton Organico 450ml (x12)', 'Bebida de te negro infusionado con extracto natural de melocoton, sin conservantes artificiales', 19990, 0, 4),

-- Categoria 5: Snacks y Confiteria
(17, 'SNK-5001', 'Caja Barras Cereal y Avena con Miel (x30)', 'Display de 30 unidades individuales, snack saludable rico en fibra con avena integral y miel de ulmo', 27990, 22, 5),
(18, 'SNK-5002', 'Almendras Tostadas con Sal Marina 150g (x10)', 'Frutos secos seleccionados sin gluten, envasados al vacio con atmosfera modificada para maxima frescura', 34990, 18, 5),
(19, 'SNK-5003', 'Chocolate Negro 70% Cacao Fino 80g (x12)', 'Pack de 12 tabletas de chocolate amargo artesanal con notas florales y bajo contenido de azucar', 29990, 7, 5),

-- Categoria 6: Seguridad y Camaras CCTV
(20, 'SEC-6001', 'Camara Domo IP 4MP con Vision Nocturna Color', 'Resolucion 2K con audio bidireccional, sensor infrarrojo de 30m y ranura MicroSD para grabacion local', 72990, 14, 6),
(21, 'SEC-6002', 'Grabador NVR 8 Canales PoE 4K con HDD 2TB', 'Servidor de grabacion digital PoE para 8 camaras simultaneas con compresion inteligente H.265+', 245000, 5, 6),

-- Categoria 7: Audio e Iluminacion Comercial
(22, 'LGT-7001', 'Panel LED Cuadrado 60x60cm 40W Luz Neutra (x4)', 'Pack de 4 paneles LED ultra delgados para cielo americano con driver anti-parpadeo y 4000 lumenes', 54990, 16, 7),
(23, 'AUD-7002', 'Amplificador Comercial Bluetooth 60W con Mezclador', 'Amplificador de audio ambiental para locales de retail con entradas de microfono y reproductor USB/SD', 129990, 8, 7),

-- Categoria 8: Limpieza y Desinfeccion
(24, 'CLN-8001', 'Dispensador Automatico de Alcohol Gel 1000ml', 'Sensor infrarrojo sin contacto con bomba de pulverizacion para sanitizacion rapida en acceso de clientes', 36990, 20, 8),
(25, 'CLN-8002', 'Caja Toallas Humedas Desinfectantes Industriales', 'Cubo con 450 toallas cloradas de alta resistencia para desinfeccion de mesones y canastas de compras', 28990, 25, 8),

-- Categoria 9: Accesorios y Conectividad
(26, 'ACC-9001', 'Soporte Metalico Antirrobo para Tablet POS', 'Gabinete con cerradura de llave para iPad o Galaxy Tab 10.1", rotacion completa multidireccional y base para mostrador', 48990, 15, 9),
(27, 'ACC-9002', 'Hub USB 3.0 Industrial 7 Puertos con Alimentacion', 'Concentrador USB metalico con proteccion contra sobretensiones y transformador de 12V para perifericos POS', 39990, 18, 9),

-- Categoria 10: Embalaje y Despacho
(28, 'PKG-10001', 'Fardo x500 Bolsas Kraft Ecologicas con Asa', 'Bolsas de papel kraft reforzadas de 32x22x10cm con alta capacidad de carga para entrega en caja', 45990, 40, 10),
(29, 'PKG-10002', 'Caja x36 Cintas de Embalaje Transparente 48mm x 100m', 'Cinta adhesiva acrilica de alta adherencia para sellado de cajas y paquetes de inventario', 32990, 28, 10);

-- Reinicio de secuencias de identidad para evitar conflicto de IDs al insertar nuevos registros desde la API
ALTER TABLE categories ALTER COLUMN id RESTART WITH 100;
ALTER TABLE products ALTER COLUMN id RESTART WITH 100;
