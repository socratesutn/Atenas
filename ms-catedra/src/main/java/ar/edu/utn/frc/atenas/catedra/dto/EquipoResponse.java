package ar.edu.utn.frc.atenas.catedra.dto;

import java.util.List;

/**
 * Detalle público de un equipo con sus integrantes vigentes.
 */
public record EquipoResponse(
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
        String representanteSub,
        List<IntegranteResponse> integrantes) {
}
