package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.time.OffsetDateTime;

/** Una fila de la asistencia propia de un alumno (S06): el evento y si estuvo presente. */
public record MiAsistenciaResponse(
        Long eventoId,
        Long comisionLectivaId,
        String tipo,
        OffsetDateTime fecha,
        String descripcion,
        boolean presente) {
}
