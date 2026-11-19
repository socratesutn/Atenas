package ar.edu.utn.frc.atenas.documentacion.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta de ms-catedra para I04: indica si el docente está asignado a la
 * comisión lectiva consultada.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DocenteAsignadoDto(Boolean asignado) {
}
