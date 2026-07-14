-- Migración: agrega columna foto_url a la tabla personas
-- Ejecutar UNA VEZ en la base de datos antes de desplegar la versión con FTP.

SET search_path TO funeraria, public;

ALTER TABLE personas
    ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500);

COMMENT ON COLUMN personas.foto_url IS
    'URL pública del archivo de foto de perfil almacenado en el servidor FTP';
