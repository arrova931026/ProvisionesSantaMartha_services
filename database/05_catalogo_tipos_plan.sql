SET search_path TO funeraria, public;

ALTER TABLE planes_funerarios
    ADD COLUMN IF NOT EXISTS tipo_plan VARCHAR(20) NOT NULL DEFAULT 'HUMANOS';
ALTER TABLE plan_articulos
    ADD COLUMN IF NOT EXISTS modalidad VARCHAR(20);
ALTER TABLE contratos
    ADD COLUMN IF NOT EXISTS frecuencia_pago VARCHAR(15) NOT NULL DEFAULT 'MENSUAL';

UPDATE planes_funerarios SET tipo_plan = 'HUMANOS' WHERE tipo_plan IS NULL;

INSERT INTO categorias_articulo (clave, nombre) VALUES
    ('VELACION', 'Equipo de velación'),
    ('CARROZA', 'Carroza funeraria'),
    ('PERSONAL', 'Personal para maniobras')
ON CONFLICT (clave) DO NOTHING;

WITH articulos_seed (categoria_clave, nombre, descripcion) AS (
    VALUES
        ('TRASLADO', 'Recolección del cuerpo', 'Recolección en domicilio o clínica.'),
        ('EMBALSAMADO', 'Embalsamamiento del cuerpo', 'Preparación del cuerpo para el servicio.'),
        ('VELACION', 'Equipo de velación a domicilio', 'Equipo para velación en domicilio.'),
        ('CARROZA', 'Servicio de carroza', 'Traslado en carroza para el servicio funerario.'),
        ('TRAMITES', 'Gestoría de trámites funerarios', 'Asesoría y gestión de trámites funerarios.'),
        ('CREMACION', 'Servicio de cremación', 'Servicio de cremación.'),
        ('URNA', 'Urna básica para restos', 'Urna básica para entrega de restos cremados.'),
        ('OTRO', 'Constancia de cremación digital', 'Constancia digital del servicio de cremación.'),
        ('PERSONAL', 'Personal para maniobras', 'Personal capacitado para maniobras del servicio.'),
        ('ATAUD', 'Ataúd metálico estándar', 'Ataúd metálico estándar para inhumación.'),
        ('ATAUD', 'Ataúd de madera MDF', 'Ataúd de madera MDF para inhumación.'),
        ('TRASLADO', 'Recolección de mascota', 'Recolección de mascota en domicilio o clínica.'),
        ('CREMACION', 'Cremación individual de mascota', 'Cremación individual de mascota.'),
        ('URNA', 'Urna para restos de mascota', 'Entrega de los restos de la mascota en urna.'),
        ('OTRO', 'Constancia digital de cremación de mascota', 'Constancia digital del servicio de cremación de mascota.')
)
INSERT INTO catalogo_articulos (categoria_id, nombre, descripcion)
SELECT categoria.id, seed.nombre, seed.descripcion
FROM articulos_seed seed
JOIN categorias_articulo categoria ON categoria.clave = seed.categoria_clave
WHERE NOT EXISTS (
    SELECT 1 FROM catalogo_articulos existente WHERE existente.nombre = seed.nombre
);

INSERT INTO planes_funerarios
    (nombre, descripcion, tipo_plan, precio_total, mensualidad, duracion_meses, numero_beneficiarios)
SELECT 'Mutualista',
       'Cobertura de cremación o inhumación con aportación anual de $1,200, mensual de $100, quincenal de $50 o semanal de $25.',
       'MUTUALISTA', 1200.00, 100.00, 12, 1
WHERE NOT EXISTS (
    SELECT 1 FROM planes_funerarios WHERE tipo_plan = 'MUTUALISTA' AND nombre = 'Mutualista'
);

-- Precios Estándar y Premium son propuestas iniciales editables; el anuncio solo confirma "desde $2,500".
INSERT INTO planes_funerarios
    (nombre, descripcion, tipo_plan, precio_total, mensualidad, duracion_meses, numero_beneficiarios)
SELECT seed.nombre, seed.descripcion, 'MASCOTAS', seed.precio, seed.precio, 1, 0
FROM (VALUES
    ('Básico',   'Recolección en domicilio o clínica, cremación, entrega de urna y constancia digital.', 2500.00::NUMERIC),
    ('Estándar', 'Recolección en domicilio o clínica, cremación, entrega de urna y constancia digital.', 3500.00::NUMERIC),
    ('Premium',  'Recolección en domicilio o clínica, cremación, entrega de urna y constancia digital.', 5000.00::NUMERIC)
) AS seed(nombre, descripcion, precio)
WHERE NOT EXISTS (
    SELECT 1 FROM planes_funerarios existente
    WHERE existente.tipo_plan = 'MASCOTAS' AND existente.nombre = 'Mascotas - ' || seed.nombre
);

UPDATE planes_funerarios
SET nombre = 'Mascotas - ' || nombre
WHERE tipo_plan = 'MASCOTAS' AND nombre IN ('Básico', 'Estándar', 'Premium');

WITH incluidos (plan_tipo, plan_nombre, articulo_nombre, modalidad) AS (
    VALUES
        ('MUTUALISTA', 'Mutualista', 'Recolección del cuerpo', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Embalsamamiento del cuerpo', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Equipo de velación a domicilio', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Servicio de carroza', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Gestoría de trámites funerarios', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Servicio de cremación', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Urna básica para restos', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Constancia de cremación digital', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Personal para maniobras', 'CREMACION'),
        ('MUTUALISTA', 'Mutualista', 'Ataúd metálico estándar', 'INHUMACION'),
        ('MUTUALISTA', 'Mutualista', 'Ataúd de madera MDF', 'INHUMACION'),
        ('MUTUALISTA', 'Mutualista', 'Recolección del cuerpo', 'INHUMACION'),
        ('MUTUALISTA', 'Mutualista', 'Embalsamamiento del cuerpo', 'INHUMACION'),
        ('MUTUALISTA', 'Mutualista', 'Gestoría de trámites funerarios', 'INHUMACION'),
        ('MUTUALISTA', 'Mutualista', 'Equipo de velación a domicilio', 'INHUMACION'),
        ('MUTUALISTA', 'Mutualista', 'Servicio de carroza', 'INHUMACION'),
        ('MUTUALISTA', 'Mutualista', 'Personal para maniobras', 'INHUMACION'),
        ('MASCOTAS', 'Mascotas - Básico', 'Recolección de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Básico', 'Cremación individual de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Básico', 'Urna para restos de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Básico', 'Constancia digital de cremación de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Estándar', 'Recolección de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Estándar', 'Cremación individual de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Estándar', 'Urna para restos de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Estándar', 'Constancia digital de cremación de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Premium', 'Recolección de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Premium', 'Cremación individual de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Premium', 'Urna para restos de mascota', NULL),
        ('MASCOTAS', 'Mascotas - Premium', 'Constancia digital de cremación de mascota', NULL)
)
INSERT INTO plan_articulos (plan_id, articulo_id, cantidad, modalidad)
SELECT plan.id, articulo.id, 1, incluidos.modalidad
FROM incluidos
JOIN planes_funerarios plan
  ON plan.tipo_plan = incluidos.plan_tipo AND plan.nombre = incluidos.plan_nombre
JOIN catalogo_articulos articulo ON articulo.nombre = incluidos.articulo_nombre
ON CONFLICT (plan_id, articulo_id) DO UPDATE SET modalidad = EXCLUDED.modalidad;