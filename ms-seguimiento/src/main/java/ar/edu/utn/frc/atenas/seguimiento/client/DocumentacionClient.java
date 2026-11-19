package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EntregaEquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.HitoDto;

import java.util.List;

/**
 * Adaptador de salida hacia ms-documentacion (rutas {@code /internal/**} de docs/06).
 *
 * <p>Mismas reglas de propagación y de traducción de errores que {@link CatedraClient}.</p>
 */
public interface DocumentacionClient {

    /** I06: hitos de tipo SPRINT de un curso lectivo, para calcular la actividad por sprint. */
    List<HitoDto> listarHitosSprint(Long cursoLectivoId);

    /**
     * I07: estado y nota de las entregas de los equipos indicados en un curso, incluidas las
     * {@code PENDIENTE} calculadas por ms-documentacion.
     */
    List<EntregaEquipoDto> listarEntregas(Long cursoLectivoId, List<Long> equipoIds);
}
