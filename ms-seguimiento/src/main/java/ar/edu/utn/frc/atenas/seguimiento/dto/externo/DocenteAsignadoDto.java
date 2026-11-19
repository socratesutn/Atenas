package ar.edu.utn.frc.atenas.seguimiento.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta de ms-catedra para I04: si el docente está asignado a la comisión. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DocenteAsignadoDto(Boolean asignado) {
}
