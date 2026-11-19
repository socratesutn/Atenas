package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EstudianteDto;

import java.util.List;

/**
 * Adaptador de salida hacia ms-catedra (rutas {@code /internal/**} de docs/06).
 *
 * <p>ms-catedra es la fuente de verdad de personas, equipos y pertenencia. Todas las
 * operaciones propagan el JWT del usuario y el correlation-id. Errores: 404 se traduce a
 * {@code NoEncontradoException}; timeout, conexión, 5xx, otro 4xx o cuerpo ilegible, a
 * {@code ServicioNoDisponibleException}.</p>
 */
public interface CatedraClient {

    /** I01: equipo con estado, comisión, curso, representante e integrantes. */
    EquipoDto obtenerEquipo(Long equipoId);

    /** I02: equipos de una comisión lectiva. */
    List<EquipoDto> listarEquiposPorComision(Long comisionLectivaId);

    /** I02: equipos de un curso lectivo. */
    List<EquipoDto> listarEquiposPorCurso(Long cursoLectivoId);

    /** I03: comisión, curso y equipo vigentes de un alumno, según su {@code sub}. */
    EstudianteDto obtenerEstudiantePorSub(String sub);

    /**
     * I04: indica si el docente está asignado a la comisión.
     *
     * @throws ar.edu.utn.frc.atenas.commons.error.NoEncontradoException si la comisión no existe
     */
    boolean docenteAsignado(Long comisionLectivaId, String docenteSub);
}
