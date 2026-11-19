package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Respuesta de S08: qué pasó con cada sprint del equipo al actualizar su actividad. */
public record ActualizacionActividadResponse(
        Long equipoId,
        OffsetDateTime fechaActualizacion,
        List<SnapshotActividadResumen> snapshots) {
}
