-- WhatsApp Cloud API: consentimiento y clave idempotente por notificacion.
ALTER TABLE funeraria.personas
    ADD COLUMN IF NOT EXISTS whatsapp_opt_in BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE funeraria.notificaciones
    ADD COLUMN IF NOT EXISTS referencia_externa VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notificaciones_referencia_externa
    ON funeraria.notificaciones (referencia_externa)
    WHERE referencia_externa IS NOT NULL;