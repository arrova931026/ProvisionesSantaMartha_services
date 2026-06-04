-- Paso 1: Habilitar pgcrypto (una sola vez)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Paso 2: Actualizar el hash directamente (genera Y guarda en un solo paso)
UPDATE funeraria.usuarios
SET password_hash = crypt('Admin123!', gen_salt('bf', 10))
WHERE username = 'admin';

-- Paso 3: Verificar que quedó guardado
SELECT id, username, LEFT(password_hash, 7) AS hash_inicio
FROM funeraria.usuarios
WHERE username = 'admin';
-- Debe mostrar: $2a$10$