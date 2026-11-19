package ar.edu.utn.frc.atenas.documentacion.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Respuesta de ms-catedra para I01 y cada elemento de I02.
 *
 * Es una copia propia del contrato de docs/06 (ADR-006): no se comparte con
 * ms-catedra ni con otros servicios. Solo se modelan los campos que necesita
 * este servicio, y se ignoran las propiedades nuevas que pueda agregar el
 * proveedor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquipoDto(
        Long id,
        String nombre,
        Long comisionLectivaId,
        Long cursoLectivoId,
        String estado,
        Boolean asegurado,
        String representanteSub,
        List<IntegranteDto> integrantes) {

    /**
     * Integrante vigente de un equipo. El contrato interno lo expone con una
     * forma resumida para que los consumidores no dependan de la entidad
     * completa de ms-catedra.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntegranteDto(
            Long estudianteId,
            String sub,
            String legajo,
            String usuarioGithub) {
    }
}
