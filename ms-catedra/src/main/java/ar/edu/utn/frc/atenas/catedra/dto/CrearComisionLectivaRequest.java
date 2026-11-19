package ar.edu.utn.frc.atenas.catedra.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Datos para habilitar una comisión existente dentro de un curso lectivo.
 * El curso se recibe por la ruta.
 */
public record CrearComisionLectivaRequest(
        @NotNull(message = "La comisión es obligatoria")
        Long comisionId) {
}
