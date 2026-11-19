package ar.edu.utn.frc.atenas.seguimiento.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Elemento de la respuesta de ms-documentacion para I07: estado y nota de un equipo en un
 * hito. Incluye las entregas {@code PENDIENTE}, que ms-documentacion calcula sin guardarlas.
 * {@code nota} es {@code null} mientras la entrega no esté corregida.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntregaEquipoDto(
        Long equipoId,
        Long hitoId,
        String estado,
        Boolean tardia,
        Integer nota) {
}
