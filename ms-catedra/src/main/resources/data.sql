CREATE UNIQUE INDEX IF NOT EXISTS uk_integrante_estudiante_vigente
ON integrante(estudiante_id)
WHERE fecha_baja IS NULL;
