package ar.edu.utn.frc.atenas.seguimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de S07: repositorio público de GitHub y rama que se va a seguir. */
public record VincularRepositorioRequest(
        @NotBlank(message = "La URL del repositorio es obligatoria")
        @Size(max = 255, message = "La URL del repositorio no puede superar los 255 caracteres")
        String url,

        @NotBlank(message = "La rama de seguimiento es obligatoria")
        @Size(max = 255, message = "La rama no puede superar los 255 caracteres")
        String rama) {
}
