package ar.edu.utn.frc.atenas.seguimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Cuerpo de S01. El tipo llega como texto y se valida acá (400) en lugar de dejar que un
 * valor desconocido falle al convertir el JSON.
 */
public record CrearEventoRequest(
        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "CLASE|PRESENTACION", message = "El tipo debe ser CLASE o PRESENTACION")
        String tipo,

        @NotNull(message = "La fecha es obligatoria")
        OffsetDateTime fecha,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion) {
}
