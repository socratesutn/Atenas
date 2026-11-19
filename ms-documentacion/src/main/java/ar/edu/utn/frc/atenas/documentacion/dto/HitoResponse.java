package ar.edu.utn.frc.atenas.documentacion.dto;

import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;

import java.time.OffsetDateTime;

/** Respuesta pública e interna de un hito. */
public record HitoResponse(
        Long id,
        Long cursoLectivoId,
        String nombre,
        TipoHito tipo,
        Integer orden,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaLimite) {
}
