package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.ComisionLectivaResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CrearComisionLectivaRequest;
import ar.edu.utn.frc.atenas.catedra.entity.AsignacionDocente;
import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.repository.AsignacionDocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionLectivaRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionRepository;
import ar.edu.utn.frc.atenas.catedra.repository.CursoLectivoRepository;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComisionLectivaServiceTest {

    private static final Long CURSO_ID = 2L;
    private static final Long COMISION_ID = 5L;

    @Mock
    private ComisionLectivaRepository comisionLectivaRepository;

    @Mock
    private CursoLectivoRepository cursoLectivoRepository;

    @Mock
    private ComisionRepository comisionRepository;

    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private ComisionLectivaService comisionLectivaService;

    @Test
    @DisplayName("Habilita una comisión en un curso cuando ambos existen y no está repetida")
    void deberiaCrearComisionLectiva_cuandoDatosValidos() {
        CursoLectivo curso = curso();
        Comision comision = new Comision(COMISION_ID, "Comisión 1");
        when(cursoLectivoRepository.findById(CURSO_ID)).thenReturn(Optional.of(curso));
        when(comisionRepository.findById(COMISION_ID)).thenReturn(Optional.of(comision));
        when(comisionLectivaRepository.existsByComisionIdAndCursoLectivoId(COMISION_ID, CURSO_ID))
                .thenReturn(false);
        when(comisionLectivaRepository.saveAndFlush(any(ComisionLectiva.class)))
                .thenReturn(new ComisionLectiva(10L, comision, curso));

        ComisionLectivaResponse respuesta = comisionLectivaService.crear(
                CURSO_ID, new CrearComisionLectivaRequest(COMISION_ID));

        assertEquals(10L, respuesta.id());
        assertEquals(COMISION_ID, respuesta.comisionId());
        assertEquals("Comisión 1", respuesta.comisionNombre());
        assertEquals(CURSO_ID, respuesta.cursoLectivoId());
    }

    @Test
    @DisplayName("Responde 404 cuando el curso lectivo no existe")
    void deberiaLanzarNoEncontrado_cuandoCursoNoExiste() {
        when(cursoLectivoRepository.findById(CURSO_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> comisionLectivaService.crear(CURSO_ID, new CrearComisionLectivaRequest(COMISION_ID)));
        verify(comisionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Responde 404 cuando la comisión no existe")
    void deberiaLanzarNoEncontrado_cuandoComisionNoExiste() {
        when(cursoLectivoRepository.findById(CURSO_ID)).thenReturn(Optional.of(curso()));
        when(comisionRepository.findById(COMISION_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> comisionLectivaService.crear(CURSO_ID, new CrearComisionLectivaRequest(COMISION_ID)));
    }

    @Test
    @DisplayName("Rechaza una comisión lectiva duplicada con 409")
    void deberiaRechazarComisionLectiva_cuandoYaEstaHabilitada() {
        when(cursoLectivoRepository.findById(CURSO_ID)).thenReturn(Optional.of(curso()));
        when(comisionRepository.findById(COMISION_ID))
                .thenReturn(Optional.of(new Comision(COMISION_ID, "Comisión 1")));
        when(comisionLectivaRepository.existsByComisionIdAndCursoLectivoId(COMISION_ID, CURSO_ID))
                .thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> comisionLectivaService.crear(CURSO_ID, new CrearComisionLectivaRequest(COMISION_ID)));
        verify(comisionLectivaRepository, never()).saveAndFlush(any(ComisionLectiva.class));
    }

    @Test
    @DisplayName("Traduce a 409 la violación de unicidad detectada por la base")
    void deberiaTraducirViolacionDeUnicidad_cuandoSaveFalla() {
        when(cursoLectivoRepository.findById(CURSO_ID)).thenReturn(Optional.of(curso()));
        when(comisionRepository.findById(COMISION_ID))
                .thenReturn(Optional.of(new Comision(COMISION_ID, "Comisión 1")));
        when(comisionLectivaRepository.existsByComisionIdAndCursoLectivoId(COMISION_ID, CURSO_ID))
                .thenReturn(false);
        when(comisionLectivaRepository.saveAndFlush(any(ComisionLectiva.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoException.class,
                () -> comisionLectivaService.crear(CURSO_ID, new CrearComisionLectivaRequest(COMISION_ID)));
    }

    @Test
    @DisplayName("C06: el jefe ve todas las comisiones lectivas del curso")
    void deberiaListarTodasLasComisiones_cuandoEsJefe() {
        ComisionLectiva comisionLectiva = comisionLectiva(10L, new Comision(COMISION_ID, "Comisión 1"));
        when(cursoLectivoRepository.existsById(CURSO_ID)).thenReturn(true);
        when(comisionLectivaRepository.findByCursoLectivoId(CURSO_ID)).thenReturn(List.of(comisionLectiva));
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);

        List<ComisionLectivaResponse> respuesta = comisionLectivaService.listarPorCurso(CURSO_ID);

        assertEquals(1, respuesta.size());
        assertEquals(COMISION_ID, respuesta.getFirst().comisionId());
    }

    @Test
    @DisplayName("RN-01: el docente solo ve sus comisiones lectivas asignadas")
    void deberiaFiltrarComisionesDelDocente_cuandoTieneAsignaciones() {
        ComisionLectiva asignada = comisionLectiva(10L, new Comision(COMISION_ID, "Comisión 1"));
        ComisionLectiva noAsignada = comisionLectiva(11L, new Comision(6L, "Comisión 2"));
        Docente docente = new Docente(3L, "Docente", "Prueba", "docente@atenas.local", "sub-docente", true);

        when(cursoLectivoRepository.existsById(CURSO_ID)).thenReturn(true);
        when(comisionLectivaRepository.findByCursoLectivoId(CURSO_ID))
                .thenReturn(List.of(asignada, noAsignada));
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-docente");
        when(docenteRepository.findBySub("sub-docente")).thenReturn(Optional.of(docente));
        when(asignacionDocenteRepository
                .findByDocenteIdAndComisionLectivaCursoLectivoId(docente.getId(), CURSO_ID))
                .thenReturn(List.of(new AsignacionDocente(20L, docente, asignada)));

        List<ComisionLectivaResponse> respuesta = comisionLectivaService.listarPorCurso(CURSO_ID);

        assertEquals(1, respuesta.size());
        assertEquals(COMISION_ID, respuesta.getFirst().comisionId());
    }

    @Test
    @DisplayName("RN-01: un docente sin fila local no recibe comisiones")
    void deberiaDevolverListaVacia_cuandoElDocenteNoExisteLocalmente() {
        when(cursoLectivoRepository.existsById(CURSO_ID)).thenReturn(true);
        when(comisionLectivaRepository.findByCursoLectivoId(CURSO_ID))
                .thenReturn(List.of(comisionLectiva(10L, new Comision(COMISION_ID, "Comisión 1"))));
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-desconocido");
        when(docenteRepository.findBySub("sub-desconocido")).thenReturn(Optional.empty());

        assertTrue(comisionLectivaService.listarPorCurso(CURSO_ID).isEmpty());
    }

    @Test
    @DisplayName("RN-01: un rol ajeno no puede consultar comisiones lectivas")
    void deberiaLanzarAccesoDenegado_cuandoNoEsJefeNiDocente() {
        when(cursoLectivoRepository.existsById(CURSO_ID)).thenReturn(true);
        when(comisionLectivaRepository.findByCursoLectivoId(CURSO_ID)).thenReturn(List.of());

        assertThrows(AccesoDenegadoException.class,
                () -> comisionLectivaService.listarPorCurso(CURSO_ID));
    }

    @Test
    @DisplayName("Responde 404 al listar comisiones de un curso inexistente")
    void deberiaLanzarNoEncontrado_cuandoCursoNoExisteAlListar() {
        when(cursoLectivoRepository.existsById(CURSO_ID)).thenReturn(false);

        assertThrows(NoEncontradoException.class,
                () -> comisionLectivaService.listarPorCurso(CURSO_ID));
    }

    private CursoLectivo curso() {
        return new CursoLectivo(CURSO_ID, 2026,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15));
    }

    private ComisionLectiva comisionLectiva(Long id, Comision comision) {
        return new ComisionLectiva(id, comision, curso());
    }
}
