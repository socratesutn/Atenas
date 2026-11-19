package ar.edu.utn.frc.atenas.documentacion.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta de ms-catedra para I03.
 *
 * {@code equipoId} es {@code null} cuando el alumno todavía no integra un
 * equipo. El DTO es propio de ms-documentacion y se mantiene aislado del
 * modelo de ms-catedra.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EstudianteDto(
        Long estudianteId,
        String sub,
        Boolean activo,
        Long comisionLectivaId,
        Long cursoLectivoId,
        Long equipoId) {
}
