package ar.edu.utn.frc.atenas.catedra.dto;

/**
 * Resumen público para listar equipos y su disponibilidad.
 */
public record EquipoResumenResponse(
        Long id,
        String nombre,
        Long comisionLectivaId,
        Long cursoLectivoId,
        String estado,
        boolean asegurado,
        Integer cupoMinimo,
        Integer cupoMaximo,
        int cantidadIntegrantes,
        boolean disponible,
        String representanteSub) {
}
