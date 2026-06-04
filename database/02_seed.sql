-- =============================================================================
-- SOCIEDAD HUMANISTA SANTA MARTHA
-- Datos semilla (seed) – ejecutar después de 01_schema.sql
-- Idempotente: se puede ejecutar más de una vez sin romper relaciones
-- =============================================================================
SET search_path TO funeraria, public;

-- ---------------------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------------------
INSERT INTO roles (clave, nombre, descripcion) VALUES
    ('ADMIN',   'Administrador',     'Acceso total al sistema'),
    ('AGENTE',  'Agente de ventas',  'Gestiona contratos y cobros'),
    ('CLIENTE', 'Cliente',           'Acceso al portal de cliente')
ON CONFLICT (clave) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Parentescos
-- ---------------------------------------------------------------------------
INSERT INTO parentescos (clave, nombre) VALUES
    ('CONYUGE',   'Cónyuge / Pareja'),
    ('HIJO',      'Hijo(a)'),
    ('PADRE',     'Padre'),
    ('MADRE',     'Madre'),
    ('HERMANO',   'Hermano(a)'),
    ('ABUELO',    'Abuelo(a)'),
    ('NIETO',     'Nieto(a)'),
    ('TIO',       'Tío(a)'),
    ('SOBRINO',   'Sobrino(a)'),
    ('OTRO',      'Otro parentesco')
ON CONFLICT (clave) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Estados de contrato
-- ---------------------------------------------------------------------------
INSERT INTO estados_contrato (clave, nombre, descripcion) VALUES
    ('VIGENTE',      'Vigente',     'Contrato activo y al corriente'),
    ('SUSPENDIDO',   'Suspendido',  'Pagos atrasados, cobertura en pausa'),
    ('CANCELADO',    'Cancelado',   'Contrato dado de baja'),
    ('SINIESTRADO',  'Siniestrado', 'El servicio ya fue utilizado'),
    ('VENCIDO',      'Vencido',     'Período de pago concluido, cobertura permanente')
ON CONFLICT (clave) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Estados de pago
-- ---------------------------------------------------------------------------
INSERT INTO estados_pago (clave, nombre) VALUES
    ('PENDIENTE',  'Pendiente'),
    ('PAGADO',     'Pagado'),
    ('VENCIDO',    'Vencido'),
    ('CANCELADO',  'Cancelado')
ON CONFLICT (clave) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Métodos de pago
-- ---------------------------------------------------------------------------
INSERT INTO metodos_pago (clave, nombre) VALUES
    ('MERCADO_PAGO',    'Mercado Pago'),
    ('TRANSFERENCIA',   'Transferencia bancaria'),
    ('OXXO',            'Ficha OXXO'),
    ('EFECTIVO',        'Efectivo en sucursal'),
    ('TARJETA_CREDITO', 'Tarjeta de crédito'),
    ('TARJETA_DEBITO',  'Tarjeta de débito')
ON CONFLICT (clave) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Tipos de documento
-- ---------------------------------------------------------------------------
INSERT INTO tipos_documento (clave, nombre) VALUES
    ('INE',             'Credencial INE / IFE'),
    ('CURP',            'CURP'),
    ('ACTA_NAC',        'Acta de nacimiento'),
    ('CONTRATO_PDF',    'Contrato firmado (PDF)'),
    ('COMPROBANTE_DOM', 'Comprobante de domicilio'),
    ('FOTO_PERFIL',     'Fotografía de perfil'),
    ('ACTA_DEF',        'Acta de defunción'),
    ('OTRO',            'Otro documento')
ON CONFLICT (clave) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Categorías de artículo
-- ---------------------------------------------------------------------------
INSERT INTO categorias_articulo (clave, nombre) VALUES
    ('ATAUD',       'Ataúd'),
    ('URNA',        'Urna cremación'),
    ('FLORES',      'Arreglos florales'),
    ('CAPILLA',     'Servicio de capilla'),
    ('TRASLADO',    'Traslado / Transporte'),
    ('CREMACION',   'Servicio de cremación'),
    ('EMBALSAMADO', 'Embalsamado y preparación'),
    ('TRAMITES',    'Trámites y documentación'),
    ('ESQUELA',     'Esquela e impresos'),
    ('OTRO',        'Otros servicios')
ON CONFLICT (clave) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Sucursal principal
-- ---------------------------------------------------------------------------
INSERT INTO sucursales (nombre, colonia, municipio, estado, codigo_postal, telefono, correo)
SELECT
    'Sociedad Humanista Santa Martha – Oficina Central',
    'Santa Martha Acatitla',
    'Iztapalapa',
    'Ciudad de México',
    '09510',
    '5500000000',
    'contacto@sociedadhumanistasantamartha.com'
WHERE NOT EXISTS (
    SELECT 1
    FROM sucursales
    WHERE nombre = 'Sociedad Humanista Santa Martha – Oficina Central'
);

-- ---------------------------------------------------------------------------
-- Planes funerarios de ejemplo
-- ---------------------------------------------------------------------------
WITH planes_seed (nombre, descripcion, precio_total, mensualidad, duracion_meses, numero_beneficiarios) AS (
    VALUES
        ('Plan Básico',
         'Servicio funerario esencial: ataúd estándar, traslado local, trámites básicos.',
         18000.00, 300.00, 60, 1),
        ('Plan Familiar',
         'Cubre al titular y hasta 4 beneficiarios. Incluye arreglos florales y capilla 24 h.',
         45000.00, 450.00, 100, 4),
        ('Plan Premium',
         'Servicio completo con ataúd premium, capilla VIP, flores, música y video memorial.',
         80000.00, 800.00, 100, 6),
        ('Plan Cremación',
         'Servicio de cremación, urna de lujo, traslado y trámites incluidos.',
         22000.00, 275.00, 80, 1)
)
INSERT INTO planes_funerarios
    (nombre, descripcion, precio_total, mensualidad, duracion_meses, numero_beneficiarios)
SELECT
    ps.nombre,
    ps.descripcion,
    ps.precio_total,
    ps.mensualidad,
    ps.duracion_meses,
    ps.numero_beneficiarios
FROM planes_seed ps
WHERE NOT EXISTS (
    SELECT 1
    FROM planes_funerarios pf
    WHERE pf.nombre = ps.nombre
);

-- ---------------------------------------------------------------------------
-- Artículos del catálogo base
-- ---------------------------------------------------------------------------
WITH articulos_seed (categoria_clave, nombre, precio_unitario) AS (
    VALUES
        ('ATAUD',       'Ataúd estándar de madera',     8500.00),
        ('ATAUD',       'Ataúd metálico premium',       18000.00),
        ('URNA',        'Urna de mármol',               4500.00),
        ('URNA',        'Urna de madera grabada',       2800.00),
        ('FLORES',      'Arreglo floral básico',        1200.00),
        ('FLORES',      'Arreglo floral premium',       2500.00),
        ('CAPILLA',     'Uso de capilla 24 h',          3000.00),
        ('TRASLADO',    'Traslado local (hasta 30 km)', 1500.00),
        ('TRASLADO',    'Traslado foráneo',             5000.00),
        ('CREMACION',   'Servicio de cremación',        7000.00),
        ('EMBALSAMADO', 'Embalsamado y preparación',    2500.00),
        ('TRAMITES',    'Gestión de acta de defunción', 1000.00)
)
INSERT INTO catalogo_articulos (categoria_id, nombre, precio_unitario)
SELECT
    ca.id,
    ars.nombre,
    ars.precio_unitario
FROM articulos_seed ars
JOIN categorias_articulo ca ON ca.clave = ars.categoria_clave
WHERE NOT EXISTS (
    SELECT 1
    FROM catalogo_articulos cat
    WHERE cat.nombre = ars.nombre
);

-- ---------------------------------------------------------------------------
-- Asociar artículos a planes (ejemplo)
-- ---------------------------------------------------------------------------
-- Plan Básico
INSERT INTO plan_articulos (plan_id, articulo_id, cantidad)
SELECT
    pf.id,
    ca.id,
    1
FROM planes_funerarios pf
JOIN catalogo_articulos ca
    ON ca.nombre IN (
        'Ataúd estándar de madera',
        'Traslado local (hasta 30 km)',
        'Gestión de acta de defunción'
    )
WHERE pf.nombre = 'Plan Básico'
ON CONFLICT (plan_id, articulo_id) DO UPDATE
SET cantidad = EXCLUDED.cantidad;

-- Plan Familiar
INSERT INTO plan_articulos (plan_id, articulo_id, cantidad)
SELECT
    pf.id,
    ca.id,
    1
FROM planes_funerarios pf
JOIN catalogo_articulos ca
    ON ca.nombre IN (
        'Ataúd estándar de madera',
        'Traslado local (hasta 30 km)',
        'Arreglo floral básico',
        'Uso de capilla 24 h',
        'Gestión de acta de defunción'
    )
WHERE pf.nombre = 'Plan Familiar'
ON CONFLICT (plan_id, articulo_id) DO UPDATE
SET cantidad = EXCLUDED.cantidad;

-- Plan Premium
INSERT INTO plan_articulos (plan_id, articulo_id, cantidad)
SELECT
    pf.id,
    ca.id,
    1
FROM planes_funerarios pf
JOIN catalogo_articulos ca
    ON ca.nombre IN (
        'Ataúd metálico premium',
        'Traslado local (hasta 30 km)',
        'Arreglo floral premium',
        'Uso de capilla 24 h',
        'Embalsamado y preparación',
        'Gestión de acta de defunción'
    )
WHERE pf.nombre = 'Plan Premium'
ON CONFLICT (plan_id, articulo_id) DO UPDATE
SET cantidad = EXCLUDED.cantidad;

-- Plan Cremación
INSERT INTO plan_articulos (plan_id, articulo_id, cantidad)
SELECT
    pf.id,
    ca.id,
    1
FROM planes_funerarios pf
JOIN catalogo_articulos ca
    ON ca.nombre IN (
        'Servicio de cremación',
        'Urna de madera grabada',
        'Traslado local (hasta 30 km)',
        'Gestión de acta de defunción'
    )
WHERE pf.nombre = 'Plan Cremación'
ON CONFLICT (plan_id, articulo_id) DO UPDATE
SET cantidad = EXCLUDED.cantidad;