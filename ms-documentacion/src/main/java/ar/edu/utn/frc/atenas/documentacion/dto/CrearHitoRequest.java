package ar.edu.utn.frc.atenas.documentacion.dto;

import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Body de D01. */
public record CrearHitoRequest(
        @NotNull Long cursoLectivoId,
        @NotBlank String nombre,
        @NotNull TipoHito tipo,
        @NotNull Integer orden,
        @NotNull Instant fechaInicio,
        @NotNull Instant fechaLimite) {
}
