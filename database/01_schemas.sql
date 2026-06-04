-- =============================================================================
-- SOCIEDAD HUMANISTA SANTA MARTHA
-- Esquema base de base de datos – PostgreSQL 18
-- =============================================================================
-- Convenciones:
--   • Nombres de tablas y columnas en snake_case, plural para tablas.
--   • PK siempre llamada "id" de tipo BIGINT GENERATED ALWAYS AS IDENTITY.
--   • Toda tabla lleva created_at / updated_at con zona horaria.
--   • Columnas de auditoría (activo, deleted_at) para soft-delete.
--   • Los catálogos de tipo/estado usan tablas de referencia en lugar de ENUM
--     para facilitar futuros cambios sin migraciones DDL destructivas.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Extensiones útiles
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid(), crypt()
-- Normalización de texto sin dependencias de extensiones para índices

-- ---------------------------------------------------------------------------
-- Esquema de aplicación
-- ---------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS funeraria;
SET search_path TO funeraria, public;

-- ===========================================================================
-- 1. CATÁLOGOS / TABLAS DE REFERENCIA
-- ===========================================================================

-- 1.1 Roles de usuario
CREATE TABLE roles (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave       VARCHAR(50) NOT NULL UNIQUE,   -- 'ADMIN', 'AGENTE', 'CLIENTE'
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.2 Parentescos (para beneficiarios)
CREATE TABLE parentescos (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave       VARCHAR(50)  NOT NULL UNIQUE,   -- 'CONYUGE', 'HIJO', 'PADRE', etc.
    nombre      VARCHAR(100) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 1.3 Estados de contrato
CREATE TABLE estados_contrato (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave       VARCHAR(50)  NOT NULL UNIQUE,   -- 'VIGENTE', 'SUSPENDIDO', 'CANCELADO', 'SINIESTRADO', 'VENCIDO'
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 1.4 Estados de pago / cobro
CREATE TABLE estados_pago (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave       VARCHAR(50)  NOT NULL UNIQUE,   -- 'PENDIENTE', 'PAGADO', 'VENCIDO', 'CANCELADO'
    nombre      VARCHAR(100) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 1.5 Métodos de pago
CREATE TABLE metodos_pago (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave       VARCHAR(50)  NOT NULL UNIQUE,   -- 'MERCADO_PAGO', 'TRANSFERENCIA', 'OXXO', 'EFECTIVO'
    nombre      VARCHAR(100) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 1.6 Tipos de documento
CREATE TABLE tipos_documento (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave       VARCHAR(50)  NOT NULL UNIQUE,   -- 'INE', 'CURP', 'ACTA_NAC', 'CONTRATO_PDF'
    nombre      VARCHAR(100) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 1.7 Categorías de artículos del catálogo
CREATE TABLE categorias_articulo (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave       VARCHAR(50)  NOT NULL UNIQUE,   -- 'ATAUD', 'URNA', 'FLORES', 'TRASLADO', etc.
    nombre      VARCHAR(100) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ===========================================================================
-- 2. INFRAESTRUCTURA / ORGANIZACIÓN
-- ===========================================================================

-- 2.1 Sucursales
CREATE TABLE sucursales (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(150)    NOT NULL,
    calle           VARCHAR(200),
    numero_ext      VARCHAR(20),
    numero_int      VARCHAR(20),
    colonia         VARCHAR(150),
    municipio       VARCHAR(150),
    estado          VARCHAR(100),
    codigo_postal   CHAR(5),
    pais            VARCHAR(100)    NOT NULL DEFAULT 'México',
    telefono        VARCHAR(20),
    correo          VARCHAR(254),
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ===========================================================================
-- 3. PERSONAS Y USUARIOS
-- ===========================================================================

-- 3.1 Personas (clientes, empleados, beneficiarios: todos son personas)
CREATE TABLE personas (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(100)    NOT NULL,
    ap_paterno      VARCHAR(100)    NOT NULL,
    ap_materno      VARCHAR(100),
    fecha_nacimiento DATE,
    sexo            CHAR(1)         CHECK (sexo IN ('M','F','O')),
    curp            CHAR(18)        UNIQUE,
    rfc             VARCHAR(13)     UNIQUE,
    telefono        VARCHAR(20),
    telefono_alt    VARCHAR(20),
    correo          VARCHAR(254)    UNIQUE,
    calle           VARCHAR(200),
    numero_ext      VARCHAR(20),
    numero_int      VARCHAR(20),
    colonia         VARCHAR(150),
    municipio       VARCHAR(150),
    estado          VARCHAR(100),
    codigo_postal   CHAR(5),
    pais            VARCHAR(100)    NOT NULL DEFAULT 'México',
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Wrapper IMMUTABLE para normalizar texto sin depender de extensiones
CREATE OR REPLACE FUNCTION immutable_unaccent(input_text TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT lower(
        translate(
            input_text,
            'ÁÀÄÂáàäâÉÈËÊéèëêÍÌÏÎíìïîÓÒÖÔóòöôÚÙÜÛúùüûÑñÇç',
            'AAAAaaaaEEEEeeeeIIIIiiiiOOOOooooUUUUuuuuNnCc'
        )
    );
$$;

CREATE INDEX idx_personas_correo     ON personas (correo) WHERE activo = TRUE;
CREATE INDEX idx_personas_curp       ON personas (curp)   WHERE curp IS NOT NULL;
CREATE INDEX idx_personas_nombre
    ON personas (
        immutable_unaccent(ap_paterno),
        immutable_unaccent(ap_materno),
        immutable_unaccent(nombre)
    );

-- 3.2 Usuarios (credenciales de acceso)
CREATE TABLE usuarios (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    persona_id          BIGINT          NOT NULL REFERENCES personas (id),
    rol_id              BIGINT          NOT NULL REFERENCES roles (id),
    username            VARCHAR(100)    NOT NULL UNIQUE,
    password_hash       TEXT            NOT NULL,          -- bcrypt/argon2 desde la app
    ultimo_acceso       TIMESTAMPTZ,
    intentos_fallidos   SMALLINT        NOT NULL DEFAULT 0,
    bloqueado_hasta     TIMESTAMPTZ,
    requiere_cambio_pw  BOOLEAN         NOT NULL DEFAULT FALSE,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuarios_persona ON usuarios (persona_id);

-- 3.3 Tokens de sesión / recuperación de contraseña
CREATE TABLE tokens_sesion (
    id          BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT          NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    token       TEXT            NOT NULL UNIQUE DEFAULT encode(gen_random_bytes(32), 'hex'),
    tipo        VARCHAR(30)     NOT NULL,   -- 'SESION', 'RECUPERACION', 'VERIFICACION_EMAIL'
    expira_en   TIMESTAMPTZ     NOT NULL,
    usado       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tokens_usuario ON tokens_sesion (usuario_id);

-- ===========================================================================
-- 4. EMPLEADOS
-- ===========================================================================

CREATE TABLE empleados (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    persona_id      BIGINT          NOT NULL REFERENCES personas (id),
    sucursal_id     BIGINT          REFERENCES sucursales (id),
    num_empleado    VARCHAR(30)     UNIQUE,
    puesto          VARCHAR(150),
    fecha_ingreso   DATE,
    fecha_baja      DATE,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ===========================================================================
-- 5. CATÁLOGO DE PLANES FUNERARIOS
-- ===========================================================================

-- 5.1 Planes funerarios (los productos/planes que ofrece la funeraria)
CREATE TABLE planes_funerarios (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre              VARCHAR(150)    NOT NULL,
    descripcion         TEXT,
    precio_total        NUMERIC(12,2)   NOT NULL,          -- precio de lista del plan completo
    mensualidad         NUMERIC(10,2)   NOT NULL,          -- cuota mensual estándar
    duracion_meses      SMALLINT        NOT NULL,          -- período de pago (ej. 60, 120 meses)
    numero_beneficiarios SMALLINT       NOT NULL DEFAULT 1, -- máx. beneficiarios permitidos
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 5.2 Artículos/servicios incluidos en cada plan (catálogo)
CREATE TABLE catalogo_articulos (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    categoria_id        BIGINT          NOT NULL REFERENCES categorias_articulo (id),
    nombre              VARCHAR(200)    NOT NULL,
    descripcion         TEXT,
    precio_unitario     NUMERIC(10,2),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 5.3 Relación plan ↔ artículos incluidos
CREATE TABLE plan_articulos (
    plan_id             BIGINT          NOT NULL REFERENCES planes_funerarios (id),
    articulo_id         BIGINT          NOT NULL REFERENCES catalogo_articulos (id),
    cantidad            SMALLINT        NOT NULL DEFAULT 1,
    PRIMARY KEY (plan_id, articulo_id)
);

-- ===========================================================================
-- 6. CONTRATOS
-- ===========================================================================

-- 6.1 Contratos entre cliente y plan
CREATE TABLE contratos (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_contrato     VARCHAR(30)     NOT NULL UNIQUE,   -- folio legible, ej. SHSM-2025-00001
    persona_id          BIGINT          NOT NULL REFERENCES personas (id),    -- titular
    plan_id             BIGINT          NOT NULL REFERENCES planes_funerarios (id),
    sucursal_id         BIGINT          REFERENCES sucursales (id),
    empleado_vendedor_id BIGINT         REFERENCES empleados (id),
    estado_id           BIGINT          NOT NULL REFERENCES estados_contrato (id),
    fecha_inicio        DATE            NOT NULL,
    fecha_vencimiento   DATE,           -- calculado: fecha_inicio + duracion_meses
    precio_contratado   NUMERIC(12,2)   NOT NULL,          -- precio pactado (puede diferir del catálogo)
    mensualidad_pactada NUMERIC(10,2)   NOT NULL,
    notas               TEXT,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contratos_persona ON contratos (persona_id);
CREATE INDEX idx_contratos_estado  ON contratos (estado_id);

-- 6.2 Beneficiarios del contrato
CREATE TABLE beneficiarios (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id         BIGINT          NOT NULL REFERENCES contratos (id) ON DELETE CASCADE,
    persona_id          BIGINT          NOT NULL REFERENCES personas (id),  -- datos del beneficiario
    parentesco_id       BIGINT          REFERENCES parentescos (id),
    porcentaje_cobertura NUMERIC(5,2)   NOT NULL DEFAULT 100.00
                        CHECK (porcentaje_cobertura > 0 AND porcentaje_cobertura <= 100),
    es_titular          BOOLEAN         NOT NULL DEFAULT FALSE,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_beneficiarios_contrato ON beneficiarios (contrato_id);

-- 6.3 Documentos adjuntos al contrato (INE, CURP, actas, PDF firmado, etc.)
CREATE TABLE documentos (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id         BIGINT          REFERENCES contratos (id),
    persona_id          BIGINT          REFERENCES personas (id),
    tipo_id             BIGINT          NOT NULL REFERENCES tipos_documento (id),
    nombre_archivo      VARCHAR(255)    NOT NULL,
    ruta_almacenamiento TEXT            NOT NULL,          -- URL o path en el storage
    mime_type           VARCHAR(100),
    tamano_bytes        BIGINT,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ===========================================================================
-- 7. COBROS Y PAGOS
-- ===========================================================================

-- 7.1 Calendario de cobros programados (una fila por mensualidad esperada)
CREATE TABLE cobros_programados (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id         BIGINT          NOT NULL REFERENCES contratos (id) ON DELETE CASCADE,
    numero_mensualidad  SMALLINT        NOT NULL,          -- 1, 2, 3 …
    fecha_programada    DATE            NOT NULL,
    monto               NUMERIC(10,2)   NOT NULL,
    estado_id           BIGINT          NOT NULL REFERENCES estados_pago (id),
    fecha_limite        DATE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (contrato_id, numero_mensualidad)
);

CREATE INDEX idx_cobros_contrato ON cobros_programados (contrato_id);
CREATE INDEX idx_cobros_fecha    ON cobros_programados (fecha_programada, estado_id);

-- 7.2 Pagos realizados
CREATE TABLE pagos (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cobro_id            BIGINT          REFERENCES cobros_programados (id),   -- puede ser NULL para pagos no programados
    contrato_id         BIGINT          NOT NULL REFERENCES contratos (id),
    metodo_id           BIGINT          NOT NULL REFERENCES metodos_pago (id),
    monto_pagado        NUMERIC(10,2)   NOT NULL,
    fecha_pago          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    referencia_externa  VARCHAR(200),   -- ID de transacción en Mercado Pago, OXXO, etc.
    notas               TEXT,
    registrado_por      BIGINT          REFERENCES usuarios (id),             -- usuario que registró el pago
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pagos_contrato ON pagos (contrato_id);
CREATE INDEX idx_pagos_cobro    ON pagos (cobro_id);

-- 7.3 Recibos / comprobantes de pago
CREATE TABLE recibos (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pago_id             BIGINT          NOT NULL REFERENCES pagos (id),
    folio_recibo        VARCHAR(50)     NOT NULL UNIQUE,   -- ej. REC-2025-000123
    ruta_pdf            TEXT,                              -- URL al PDF generado
    enviado_correo      BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_emision       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ===========================================================================
-- 8. SERVICIOS FUNERARIOS (cuando se activa el plan)
-- ===========================================================================

-- 8.1 Cabecera del servicio funerario prestado
CREATE TABLE servicios_funerarios (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id         BIGINT          NOT NULL REFERENCES contratos (id),
    beneficiario_id     BIGINT          NOT NULL REFERENCES beneficiarios (id),  -- quién falleció
    sucursal_id         BIGINT          REFERENCES sucursales (id),
    empleado_responsable BIGINT         REFERENCES empleados (id),
    fecha_fallecimiento DATE            NOT NULL,
    fecha_servicio      DATE,
    lugar_servicio      TEXT,
    notas               TEXT,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_servicios_contrato ON servicios_funerarios (contrato_id);

-- 8.2 Detalle de artículos/servicios usados en el servicio funerario
CREATE TABLE detalle_servicio (
    id                  BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    servicio_id         BIGINT          NOT NULL REFERENCES servicios_funerarios (id) ON DELETE CASCADE,
    articulo_id         BIGINT          NOT NULL REFERENCES catalogo_articulos (id),
    cantidad            SMALLINT        NOT NULL DEFAULT 1,
    precio_unitario     NUMERIC(10,2),                     -- precio al momento del servicio
    incluido_en_plan    BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ===========================================================================
-- 9. NOTIFICACIONES
-- ===========================================================================

CREATE TABLE notificaciones (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT          NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    tipo            VARCHAR(50)     NOT NULL,   -- 'PAGO_PROXIMO', 'PAGO_VENCIDO', 'BIENVENIDA', etc.
    titulo          VARCHAR(200)    NOT NULL,
    cuerpo          TEXT,
    leida           BOOLEAN         NOT NULL DEFAULT FALSE,
    canal           VARCHAR(30)     NOT NULL DEFAULT 'APP',  -- 'APP', 'EMAIL', 'SMS', 'PUSH'
    enviada         BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_envio     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notificaciones_usuario ON notificaciones (usuario_id, leida);

-- ===========================================================================
-- 10. AUDITORÍA
-- ===========================================================================

CREATE TABLE auditoria (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT          REFERENCES usuarios (id),
    tabla           VARCHAR(100)    NOT NULL,
    registro_id     BIGINT          NOT NULL,
    accion          VARCHAR(20)     NOT NULL,   -- 'INSERT', 'UPDATE', 'DELETE'
    datos_anteriores JSONB,
    datos_nuevos    JSONB,
    ip_origen       INET,
    user_agent      TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auditoria_tabla    ON auditoria (tabla, registro_id);
CREATE INDEX idx_auditoria_usuario  ON auditoria (usuario_id);
CREATE INDEX idx_auditoria_fecha    ON auditoria (created_at DESC);

-- ===========================================================================
-- 11. FUNCIÓN: actualizar updated_at automáticamente
-- ===========================================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

-- Aplicar trigger a todas las tablas que tienen updated_at
DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN
        SELECT table_name
        FROM information_schema.columns
        WHERE table_schema = 'funeraria'
          AND column_name   = 'updated_at'
    LOOP
        EXECUTE format(
            'CREATE OR REPLACE TRIGGER trg_%s_updated_at
             BEFORE UPDATE ON funeraria.%I
             FOR EACH ROW EXECUTE FUNCTION funeraria.set_updated_at()',
            t, t
        );
    END LOOP;
END;
$$;