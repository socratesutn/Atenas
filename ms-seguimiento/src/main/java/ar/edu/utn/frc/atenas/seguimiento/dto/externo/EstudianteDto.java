package ar.edu.utn.frc.atenas.seguimiento.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta de ms-catedra para I03 ({@code GET /internal/estudiantes/por-sub/{sub}}).
 * {@code equipoId} es {@code null} si el alumno no integra ningún equipo.
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
