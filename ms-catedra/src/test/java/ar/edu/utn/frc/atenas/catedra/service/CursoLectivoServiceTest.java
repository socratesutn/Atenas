package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.CrearCursoLectivoRequest;
import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoResponse;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import ar.edu.utn.frc.atenas.catedra.repository.CursoLectivoRepository;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoLectivoServiceTest {

    private static final Integer ANIO = 2026;
    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 15);

    @Mock
    private CursoLectivoRepository cursoLectivoRepository;

    @InjectMocks
    private CursoLectivoService cursoLectivoService;

    @Test
    @DisplayName("Crea un curso lectivo cuando el año es nuevo y las fechas son válidas")
    void deberiaCrearCursoLectivo_cuandoDatosValidos() {
        CrearCursoLectivoRequest request = new CrearCursoLectivoRequest(ANIO, INICIO, FIN);
        CursoLectivo guardado = new CursoLectivo(1L, ANIO, INICIO, FIN);
        when(cursoLectivoRepository.existsByAnio(ANIO)).thenReturn(false);
        when(cursoLectivoRepository.saveAndFlush(any(CursoLectivo.class))).thenReturn(guardado);

        CursoLectivoResponse respuesta = cursoLectivoService.crear(request);

        assertEquals(1L, respuesta.id());
        assertEquals(ANIO, respuesta.anio());
        assertEquals(INICIO, respuesta.fechaInicio());
        assertEquals(FIN, respuesta.fechaFin());
        verify(cursoLectivoRepository).saveAndFlush(any(CursoLectivo.class));
    }

    @Test
    @DisplayName("Rechaza un curso lectivo duplicado por año con 409")
    void deberiaRechazarCurso_cuandoAnioDuplicado() {
        CrearCursoLectivoRequest request = new CrearCursoLectivoRequest(ANIO, INICIO, FIN);
        when(cursoLectivoRepository.existsByAnio(ANIO)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> cursoLectivoService.crear(request));
        verify(cursoLectivoRepository, never()).saveAndFlush(any(CursoLectivo.class));
    }

    @Test
    @DisplayName("Traduce a 409 la violación de unicidad detectada por la base")
    void deberiaTraducirViolacionDeUnicidad_cuandoSaveFalla() {
        CrearCursoLectivoRequest request = new CrearCursoLectivoRequest(ANIO, INICIO, FIN);
        when(cursoLectivoRepository.existsByAnio(ANIO)).thenReturn(false);
        when(cursoLectivoRepository.saveAndFlush(any(CursoLectivo.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoException.class, () -> cursoLectivoService.crear(request));
    }

    @Test
    @DisplayName("Rechaza un curso lectivo con fecha de fin anterior o igual a la de inicio con 400")
    void deberiaRechazarCurso_cuandoFechaFinNoEsPosterior() {
        CrearCursoLectivoRequest request = new CrearCursoLectivoRequest(ANIO, FIN, INICIO);

        assertThrows(SolicitudInvalidaException.class, () -> cursoLectivoService.crear(request));
        verify(cursoLectivoRepository, never()).existsByAnio(ANIO);
    }

    @Test
    @DisplayName("Lista los cursos lectivos existentes")
    void deberiaListarCursosLectivos() {
        when(cursoLectivoRepository.findAll()).thenReturn(List.of(
                new CursoLectivo(1L, ANIO, INICIO, FIN)));

        List<CursoLectivoResponse> respuesta = cursoLectivoService.listar();

        assertEquals(1, respuesta.size());
        assertEquals(ANIO, respuesta.getFirst().anio());
    }

    @Test
    @DisplayName("I05 devuelve el curso lectivo cuando existe")
    void deberiaObtenerCursoInterno_cuandoExiste() {
        when(cursoLectivoRepository.findById(1L))
                .thenReturn(Optional.of(new CursoLectivo(1L, ANIO, INICIO, FIN)));

        CursoLectivoInternoResponse respuesta = cursoLectivoService.obtenerInterno(1L);

        assertEquals(1L, respuesta.id());
        assertEquals(ANIO, respuesta.anio());
        assertEquals(INICIO, respuesta.fechaInicio());
        assertEquals(FIN, respuesta.fechaFin());
    }

    @Test
    @DisplayName("I05 responde 404 cuando el curso lectivo no existe")
    void deberiaLanzarNoEncontrado_cuandoCursoNoExiste() {
        when(cursoLectivoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> cursoLectivoService.obtenerInterno(99L));
    }
}
