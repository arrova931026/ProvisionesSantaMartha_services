-- =============================================================================
-- SOCIEDAD HUMANISTA SANTA MARTHA
-- Vistas y consultas frecuentes
-- =============================================================================
SET search_path TO funeraria, public;

-- ---------------------------------------------------------------------------
-- Vista: resumen de contrato (útil para el portal del cliente)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW funeraria.v_contratos_resumen AS
SELECT
    c.id                        AS contrato_id,
    c.numero_contrato,
    -- Titular
    p.id                        AS titular_id,
    p.nombre || ' ' || p.ap_paterno ||
        COALESCE(' ' || p.ap_materno, '') AS titular_nombre_completo,
    -- Plan
    pf.nombre                   AS plan_nombre,
    pf.duracion_meses,
    -- Fechas
    c.fecha_inicio,
    c.fecha_vencimiento,
    -- Financiero
    c.mensualidad_pactada,
    c.precio_contratado,
    -- Estado
    ec.nombre                   AS estado,
    ec.clave                    AS estado_clave,
    -- Progreso de pago
    (SELECT COUNT(*) FROM funeraria.cobros_programados cp
     WHERE cp.contrato_id = c.id
      AND cp.estado_id   = (SELECT id FROM funeraria.estados_pago WHERE clave = 'PAGADO'))
                                AS mensualidades_pagadas,
    pf.duracion_meses           AS total_mensualidades,
    ROUND(
        (SELECT COUNT(*) FROM funeraria.cobros_programados cp
         WHERE cp.contrato_id = c.id
           AND cp.estado_id   = (SELECT id FROM funeraria.estados_pago WHERE clave = 'PAGADO'))
        * 100.0 / NULLIF(pf.duracion_meses, 0), 2
    )                           AS porcentaje_completado,
    -- Próximo cobro
    (SELECT cp.fecha_programada
     FROM funeraria.cobros_programados cp
     JOIN funeraria.estados_pago ep ON ep.id = cp.estado_id
     WHERE cp.contrato_id = c.id
       AND ep.clave IN ('PENDIENTE', 'VENCIDO')
     ORDER BY cp.fecha_programada
     LIMIT 1)                   AS proxima_fecha_cobro,
    (SELECT cp.monto
     FROM funeraria.cobros_programados cp
     JOIN funeraria.estados_pago ep ON ep.id = cp.estado_id
     WHERE cp.contrato_id = c.id
       AND ep.clave IN ('PENDIENTE', 'VENCIDO')
     ORDER BY cp.fecha_programada
     LIMIT 1)                   AS proximo_monto_cobro,
    -- Saldo vencido
    COALESCE((
        SELECT SUM(cp.monto)
        FROM funeraria.cobros_programados cp
        JOIN funeraria.estados_pago ep ON ep.id = cp.estado_id
        WHERE cp.contrato_id = c.id
          AND ep.clave = 'VENCIDO'
    ), 0)                       AS saldo_vencido
FROM funeraria.contratos c
JOIN funeraria.personas          p  ON p.id  = c.persona_id
JOIN funeraria.planes_funerarios pf ON pf.id = c.plan_id
JOIN funeraria.estados_contrato  ec ON ec.id = c.estado_id
WHERE c.activo = TRUE;

-- ---------------------------------------------------------------------------
-- Vista: beneficiarios con datos completos
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW funeraria.v_beneficiarios AS
SELECT
    b.id                        AS beneficiario_id,
    b.contrato_id,
    c.numero_contrato,
    p.id                        AS persona_id,
    p.nombre || ' ' || p.ap_paterno ||
        COALESCE(' ' || p.ap_materno, '') AS nombre_completo,
    p.fecha_nacimiento,
    par.nombre                  AS parentesco,
    b.porcentaje_cobertura,
    b.es_titular,
    b.activo
FROM funeraria.beneficiarios b
JOIN funeraria.contratos  c   ON c.id   = b.contrato_id
JOIN funeraria.personas   p   ON p.id   = b.persona_id
LEFT JOIN funeraria.parentescos par ON par.id = b.parentesco_id;

-- ---------------------------------------------------------------------------
-- Vista: cobros pendientes / vencidos (útil para dashboard de cobros)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW funeraria.v_cobros_pendientes AS
SELECT
    cp.id                       AS cobro_id,
    cp.contrato_id,
    c.numero_contrato,
    p.nombre || ' ' || p.ap_paterno ||
        COALESCE(' ' || p.ap_materno, '') AS titular,
    p.telefono,
    p.correo,
    cp.numero_mensualidad,
    cp.fecha_programada,
    cp.fecha_limite,
    cp.monto,
    ep.clave                    AS estado_clave,
    ep.nombre                   AS estado
FROM funeraria.cobros_programados cp
JOIN funeraria.contratos      c  ON c.id  = cp.contrato_id
JOIN funeraria.personas       p  ON p.id  = c.persona_id
JOIN funeraria.estados_pago   ep ON ep.id = cp.estado_id
WHERE ep.clave IN ('PENDIENTE', 'VENCIDO')
  AND c.activo = TRUE
ORDER BY cp.fecha_programada;

-- ---------------------------------------------------------------------------
-- Vista: historial de pagos por contrato
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW funeraria.v_historial_pagos AS
SELECT
    pg.id                       AS pago_id,
    pg.contrato_id,
    c.numero_contrato,
    cp.numero_mensualidad,
    cp.fecha_programada,
    pg.fecha_pago,
    pg.monto_pagado,
    mp.nombre                   AS metodo_pago,
    pg.referencia_externa,
    r.folio_recibo,
    r.ruta_pdf                  AS recibo_pdf
FROM funeraria.pagos pg
JOIN funeraria.contratos          c  ON c.id  = pg.contrato_id
JOIN funeraria.metodos_pago       mp ON mp.id = pg.metodo_id
LEFT JOIN funeraria.cobros_programados cp ON cp.id = pg.cobro_id
LEFT JOIN funeraria.recibos            r  ON r.pago_id = pg.id
ORDER BY pg.fecha_pago DESC;