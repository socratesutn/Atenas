package ar.edu.utn.frc.atenas.documentacion.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/**
 * Respuesta de ms-catedra para I05.
 *
 * Se usa para validar que las fechas de un hito caigan dentro del período del
 * curso lectivo. Las fechas se reciben como fechas locales, tal como las
 * expone el contrato interno.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CursoLectivoDto(
        Long id,
        Integer anio,
        LocalDate fechaInicio,
        LocalDate fechaFin) {
}
