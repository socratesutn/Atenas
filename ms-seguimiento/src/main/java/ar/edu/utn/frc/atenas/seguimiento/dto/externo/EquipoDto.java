package ar.edu.utn.frc.atenas.seguimiento.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Respuesta de ms-catedra para I01 ({@code GET /internal/equipos/{id}}) y cada elemento de
 * I02. Es una copia propia del contrato de docs/06 (ADR-006): no se comparte con ms-catedra.
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntegranteDto(Long estudianteId, String sub, String legajo, String usuarioGithub) {
    }
}
