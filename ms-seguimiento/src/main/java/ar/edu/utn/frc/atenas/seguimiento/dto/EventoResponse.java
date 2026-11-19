package ar.edu.utn.frc.atenas.seguimiento.dto;

import java.time.OffsetDateTime;

/** Evento del calendario de una comisión. */
public record EventoResponse(
        Long id,
        Long comisionLectivaId,
        String tipo,
        OffsetDateTime fecha,
        String descripcion) {
}
