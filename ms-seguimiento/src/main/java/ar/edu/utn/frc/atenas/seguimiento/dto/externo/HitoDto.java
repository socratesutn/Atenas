package ar.edu.utn.frc.atenas.seguimiento.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/** Elemento de la respuesta de ms-documentacion para I06 (hitos de tipo SPRINT de un curso). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HitoDto(
        Long id,
        Long cursoLectivoId,
        String nombre,
        String tipo,
        Integer orden,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaLimite) {
}
