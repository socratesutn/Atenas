package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.time.OffsetDateTime;

/** Asistencia registrada de un alumno en un evento. {@code equipoId} es nulo si no tenía equipo. */
public record AsistenciaResponse(
        Long eventoId,
        String estudianteSub,
        Long equipoId,
        boolean presente,
        OffsetDateTime fechaRegistro) {
}
