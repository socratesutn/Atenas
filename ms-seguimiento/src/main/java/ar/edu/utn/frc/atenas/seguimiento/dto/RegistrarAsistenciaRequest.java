package ar.edu.utn.frc.atenas.seguimiento.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de S03 y S04. Es obligatorio: no se asume presente ni ausente. */
public record RegistrarAsistenciaRequest(
        @NotNull(message = "Debe indicar si el alumno estuvo presente")
        Boolean presente) {
}
