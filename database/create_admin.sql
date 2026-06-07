-- ...existing code...
SET search_path TO funeraria, public;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

WITH rol_admin AS (
    SELECT id
    FROM roles
    WHERE clave = 'ADMIN'
),
persona_admin AS (
    INSERT INTO personas (nombre, ap_paterno, ap_materno, correo, telefono, activo)
    SELECT 'Admin', 'Sistema', '', 'admin@santamartha.local', '5500000000', TRUE
    WHERE NOT EXISTS (
        SELECT 1
        FROM personas
        WHERE correo = 'admin@santamartha.local'
    )
    RETURNING id
),
persona_objetivo AS (
    SELECT id FROM persona_admin
    UNION ALL
    SELECT id
    FROM personas
    WHERE correo = 'admin@santamartha.local'
    LIMIT 1
)
INSERT INTO usuarios (persona_id, rol_id, username, password_hash, requiere_cambio_pw, activo)
SELECT
    p.id,
    r.id,
    'admin',
    crypt('Admin123!', gen_salt('bf', 10)),
    TRUE,
    TRUE
FROM persona_objetivo p
CROSS JOIN rol_admin r
WHERE NOT EXISTS (
    SELECT 1
    FROM usuarios
    WHERE username = 'admin'
);

COMMIT;

-- Verificación
SELECT
    u.id,
    u.username,
    p.nombre,
    p.ap_paterno,
    r.clave AS rol
FROM usuarios u
JOIN personas p ON p.id = u.persona_id
JOIN roles r ON r.id = u.rol_id
WHERE u.username = 'admin';
-- ...existing code...