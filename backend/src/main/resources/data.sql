-- =========================================================================
-- Datos Iniciales de Prueba: Sistema de Inventario para Locales Comerciales
-- =========================================================================

-- Inserción de Categorías
INSERT INTO categories (id, name, description) VALUES
(1, 'Puntos de Venta (POS)', 'Terminales táctiles, lectores de código de barras, gavetas de dinero e impresoras de tickets'),
(2, 'Equipos de Cómputo y Redes', 'Servidores locales, switches, routers comerciales y terminales de inventario'),
(3, 'Papelería e Insumos Comerciales', 'Rollos térmicos de recibos, etiquetas adhesivas de precios y suministros de caja'),
(4, 'Bebidas y Refrigerados', 'Bebidas energéticas, refrescos y lácteos listos para consumo en tienda'),
(5, 'Snacks y Confitería', 'Chocolates, galletas, frutos secos y aperitivos empaquetados');

-- Inserción de Productos
INSERT INTO products (id, sku, name, price, stock, category_id) VALUES
-- Categoría 1: POS
(1, 'POS-1001', 'Terminal Táctil All-in-One 15.6" Intel J4125 8GB/128GB SSD', 459.99, 12, 1),
(2, 'POS-1002', 'Lector Láser 2D QR/Barcode Omnidireccional USB', 79.50, 25, 1),
(3, 'POS-1003', 'Impresora Térmica de Recibos 80mm ESC/POS con Autocorte', 115.00, 18, 1),
(4, 'POS-1004', 'Gaveta Portamonedas Metálica Pesada 5B/8M con Apertura RJ11', 58.00, 14, 1),
(5, 'POS-1005', 'Balanza Electrónica de Mostrador 30kg con Conexión Serie/USB', 189.00, 6, 1),

-- Categoría 2: Equipos y Redes
(6, 'NET-2001', 'Router Dual-Band Wi-Fi 6 Gigabit para Negocios con Red de Clientes', 89.90, 15, 2),
(7, 'NET-2002', 'Switch No Administrable 16 Puertos Gigabit Ethernet Metálico', 64.99, 10, 2),
(8, 'NET-2003', 'Sistema UPS / Respaldo de Energía 1500VA 900W 8 Tomas', 165.00, 8, 2),
(9, 'NET-2004', 'Terminal Colector de Datos Móvil Android Rugged con Escáner Láser', 320.00, 4, 2),

-- Categoría 3: Papelería e Insumos
(10, 'SUP-3001', 'Pack x50 Rollos de Papel Térmico Premium 80mm x 70mm', 38.50, 85, 3),
(11, 'SUP-3002', 'Pack x100 Rollos de Papel Térmico para Terminal POS 57mm x 40mm', 42.00, 120, 3),
(12, 'SUP-3003', 'Pistola Etiquetadora de Precios Manual 8 Dígitos + 10k Etiquetas', 24.90, 30, 3),
(13, 'SUP-3004', 'Rollo de Etiquetas Térmicas Directas 50x25mm (1000 etiquetas)', 9.99, 5, 3),

-- Categoría 4: Bebidas y Refrigerados
(14, 'BEV-4001', 'Agua Mineral de Manantial Sin Gas 500ml (Pack x24)', 18.00, 45, 4),
(15, 'BEV-4002', 'Bebida Energizante Premium Citrus 355ml (Pack x12)', 22.80, 32, 4),
(16, 'BEV-4003', 'Té Helado Melocotón Orgánico 450ml (Pack x12)', 19.50, 0, 4),

-- Categoría 5: Snacks y Confitería
(17, 'SNK-5001', 'Caja Barras de Cereal y Avena con Miel (Pack x30)', 27.50, 22, 5),
(18, 'SNK-5002', 'Almendras Tostadas con Sal Marina Bolsa 150g (Pack x10)', 34.00, 18, 5),
(19, 'SNK-5003', 'Chocolate Negro 70% Cacao Fino de Aroma 80g (Pack x12)', 29.90, 7, 5);

-- Reinicio de secuencias de identidad para evitar conflicto de IDs al insertar nuevos registros desde la API
ALTER TABLE categories ALTER COLUMN id RESTART WITH 100;
ALTER TABLE products ALTER COLUMN id RESTART WITH 100;

