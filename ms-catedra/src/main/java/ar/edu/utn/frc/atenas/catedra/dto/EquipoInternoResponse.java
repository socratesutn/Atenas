package ar.edu.utn.frc.atenas.catedra.dto;

import java.util.List;

/**
 * Formato exacto del contrato interno I01. I02 devuelve una lista de estos
 * mismos objetos.
 */
public record EquipoInternoResponse(
        Long id,
        String nombre,
        Long comisionLectivaId,
        Long cursoLectivoId,
        String estado,
        boolean asegurado,
        String representanteSub,
        List<IntegranteInternoResponse> integrantes) {
}
