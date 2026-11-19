package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.AsignacionDocenteResponse;
import ar.edu.utn.frc.atenas.catedra.dto.AsignarDocenteRequest;
import ar.edu.utn.frc.atenas.catedra.dto.DocenteAsignadoResponse;
import ar.edu.utn.frc.atenas.catedra.entity.AsignacionDocente;
import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.repository.AsignacionDocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionLectivaRepository;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionDocenteServiceTest {

    private static final Long COMISION_LECTIVA_ID = 10L;
    private static final Long DOCENTE_ID = 3L;
    private static final String SUB = "sub-docente";

    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private ComisionLectivaRepository comisionLectivaRepository;

    @InjectMocks
    private AsignacionDocenteService asignacionDocenteService;

    @Test
    @DisplayName("C10: asigna un docente a una comisión lectiva")
    void deberiaAsignarDocente_cuandoDatosValidos() {
        ComisionLectiva comisionLectiva = comisionLectiva();
        Docente docente = docente();
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID)).thenReturn(Optional.of(comisionLectiva));
        when(docenteRepository.findById(DOCENTE_ID)).thenReturn(Optional.of(docente));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(false);
        when(asignacionDocenteRepository.saveAndFlush(any(AsignacionDocente.class)))
                .thenReturn(new AsignacionDocente(20L, docente, comisionLectiva));

        AsignacionDocenteResponse respuesta = asignacionDocenteService.asignar(
                COMISION_LECTIVA_ID, new AsignarDocenteRequest(DOCENTE_ID));

        assertEquals(20L, respuesta.id());
        assertEquals(DOCENTE_ID, respuesta.docenteId());
        assertEquals(COMISION_LECTIVA_ID, respuesta.comisionLectivaId());
    }

    @Test
    @DisplayName("C10: responde 404 si no existe la comisión lectiva")
    void deberiaLanzarNoEncontrado_cuandoComisionLectivaNoExiste() {
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> asignacionDocenteService.asignar(
                        COMISION_LECTIVA_ID, new AsignarDocenteRequest(DOCENTE_ID)));
        verify(docenteRepository, never()).findById(any());
    }

    @Test
    @DisplayName("C10: responde 404 si no existe el docente")
    void deberiaLanzarNoEncontrado_cuandoDocenteNoExiste() {
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID)).thenReturn(Optional.of(comisionLectiva()));
        when(docenteRepository.findById(DOCENTE_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> asignacionDocenteService.asignar(
                        COMISION_LECTIVA_ID, new AsignarDocenteRequest(DOCENTE_ID)));
    }

    @Test
    @DisplayName("C10: rechaza una asignación duplicada con 409")
    void deberiaRechazarAsignacion_cuandoYaExiste() {
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID)).thenReturn(Optional.of(comisionLectiva()));
        when(docenteRepository.findById(DOCENTE_ID)).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> asignacionDocenteService.asignar(
                        COMISION_LECTIVA_ID, new AsignarDocenteRequest(DOCENTE_ID)));
        verify(asignacionDocenteRepository, never()).saveAndFlush(any(AsignacionDocente.class));
    }

    @Test
    @DisplayName("C10: traduce a 409 la violación de unicidad de la base")
    void deberiaTraducirViolacionDeUnicidad_cuandoSaveFalla() {
        when(comisionLectivaRepository.findById(COMISION_LECTIVA_ID)).thenReturn(Optional.of(comisionLectiva()));
        when(docenteRepository.findById(DOCENTE_ID)).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(false);
        when(asignacionDocenteRepository.saveAndFlush(any(AsignacionDocente.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoException.class,
                () -> asignacionDocenteService.asignar(
                        COMISION_LECTIVA_ID, new AsignarDocenteRequest(DOCENTE_ID)));
    }

    @Test
    @DisplayName("I04: devuelve true cuando el docente está asignado")
    void deberiaIndicarAsignado_cuandoExisteLaAsignacion() {
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(true);
        when(docenteRepository.findBySub(SUB)).thenReturn(Optional.of(docente()));
        when(asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(DOCENTE_ID, COMISION_LECTIVA_ID))
                .thenReturn(true);

        DocenteAsignadoResponse respuesta = asignacionDocenteService.estaAsignado(COMISION_LECTIVA_ID, SUB);

        assertTrue(respuesta.asignado());
    }

    @Test
    @DisplayName("I04: devuelve false cuando el sub no existe")
    void deberiaIndicarNoAsignado_cuandoElSubNoExiste() {
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(true);
        when(docenteRepository.findBySub(SUB)).thenReturn(Optional.empty());

        DocenteAsignadoResponse respuesta = asignacionDocenteService.estaAsignado(COMISION_LECTIVA_ID, SUB);

        assertFalse(respuesta.asignado());
    }

    @Test
    @DisplayName("I04: responde 404 cuando la comisión lectiva no existe")
    void deberiaLanzarNoEncontrado_cuandoComisionNoExisteEnI04() {
        when(comisionLectivaRepository.existsById(COMISION_LECTIVA_ID)).thenReturn(false);

        assertThrows(NoEncontradoException.class,
                () -> asignacionDocenteService.estaAsignado(COMISION_LECTIVA_ID, SUB));
    }

    private ComisionLectiva comisionLectiva() {
        return new ComisionLectiva(COMISION_LECTIVA_ID,
                new Comision(5L, "Comisión 1"),
                new CursoLectivo(2L, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15)));
    }

    private Docente docente() {
        return new Docente(DOCENTE_ID, "Docente", "Prueba", "docente@atenas.local", SUB, true);
    }
}
