package ar.edu.utn.frc.atenas.documentacion.dto;

import ar.edu.utn.frc.atenas.documentacion.entity.EstadoEntrega;

import java.time.OffsetDateTime;

/** Respuesta de D03 al registrar una entrega. */
public record EntregaResponse(
        Long id,
        Long hitoId,
        Long equipoId,
        EstadoEntrega estado,
        Boolean tardia,
        OffsetDateTime fechaEntrega,
        String enlace,
        Long archivoId) {
}
