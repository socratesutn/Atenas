package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;

import java.time.Instant;
import java.util.List;

/**
 * Resultado interno de actualizar la actividad de un equipo. Guarda la entidad del snapshot
 * para que el mapper arme la respuesta con la zona horaria configurada (S08).
 */
public record ResultadoActualizacion(Long equipoId, Instant fechaConsulta, List<DetalleSprint> detalles) {

    /** Un sprint puede haberse actualizado, degradado o simplemente omitido por estar cerrado. */
    public record DetalleSprint(Long hitoId, String nombreHito, SnapshotActividad snapshot, boolean omitido) {
    }
}
