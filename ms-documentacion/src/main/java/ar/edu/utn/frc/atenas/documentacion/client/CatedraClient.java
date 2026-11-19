package ar.edu.utn.frc.atenas.documentacion.client;

import ar.edu.utn.frc.atenas.documentacion.dto.externo.CursoLectivoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EstudianteDto;

import java.util.List;

/**
 * Adaptador de salida hacia ms-catedra.
 *
 * Es la interfaz propia que consume el resto de ms-documentacion. Ocultar los
 * detalles de las rutas internas, la propagación del JWT y el correlation-id,
 * y la traducción de errores HTTP permite testear los servicios sin red.
 *
 * Los contratos utilizados son I01 a I05 de docs/06.
 */
public interface CatedraClient {

    /** I01: equipo con estado, comisión, curso, representante e integrantes. */
    EquipoDto obtenerEquipo(Long equipoId);

    /**
     * I02: lista los equipos de una comisión lectiva o de un curso lectivo.
     *
     * Se debe indicar exactamente uno de los dos parámetros. El contrato
     * interno no permite consultar ambos a la vez.
     */
    List<EquipoDto> listarEquipos(Long comisionLectivaId, Long cursoLectivoId);

    /** I03: comisión, curso y equipo vigentes del alumno según su {@code sub}. */
    EstudianteDto obtenerEstudiantePorSub(String sub);

    /**
     * I04: indica si el docente está asignado a la comisión lectiva.
     *
     * @throws ar.edu.utn.frc.atenas.commons.error.NoEncontradoException
     *         si la comisión no existe
     */
    boolean docenteAsignado(Long comisionLectivaId, String sub);

    /** I05: curso lectivo y sus fechas. */
    CursoLectivoDto obtenerCurso(Long cursoLectivoId);
}
