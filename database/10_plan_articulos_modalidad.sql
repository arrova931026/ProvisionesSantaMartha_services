-- Agrega la modalidad opcional de servicio a la relacion plan-articulo.
-- Compatible con bases existentes y seguro para ejecutar mas de una vez.
ALTER TABLE funeraria.plan_articulos
    ADD COLUMN IF NOT EXISTS modalidad VARCHAR(20);
