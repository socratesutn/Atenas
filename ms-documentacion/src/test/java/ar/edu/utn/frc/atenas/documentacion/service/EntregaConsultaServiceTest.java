package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaEstadoResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.EntregaInternaResponse;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.EstudianteDto;
import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import ar.edu.utn.frc.atenas.documentacion.entity.EstadoEntrega;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import ar.edu.utn.frc.atenas.documentacion.repository.EntregaRepository;
import ar.edu.utn.frc.atenas.documentacion.repository.HitoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaConsultaServiceTest {

    @Mock
    private CatedraClient catedraClient;

    @Mock
    private HitoRepository hitoRepository;

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private UsuarioActual usuarioActual;

    private EntregaConsultaService service;

    @BeforeEach
    void preparar() {
        CalculadorEntregas calculador =
                new CalculadorEntregas(new ZonaHoraria("America/Argentina/Cordoba"));
        service = new EntregaConsultaService(catedraClient, hitoRepository, entregaRepository,
                calculador, usuarioActual);
    }

    @Test
    @DisplayName("D04: el jefe consulta sin validar asignación docente y obtiene pendientes")
    void deberiaListarPorComision_cuandoEsJefe() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.listarEquipos(10L, null)).thenReturn(List.of(equipo(7L)));
        when(hitoRepository.findByCursoLectivoIdOrderByOrdenAscIdAsc(2L))
                .thenReturn(List.of(hito(3L)));
        when(entregaRepository.findByHitoIdInAndEquipoIdIn(List.of(3L), List.of(7L)))
                .thenReturn(List.of());

        List<EntregaEstadoResponse> resultado = service.listarPorComision(10L, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().estado()).isEqualTo("PENDIENTE");
        verify(catedraClient, never()).docenteAsignado(10L, "sub-doc");
    }

    @Test
    @DisplayName("RN-01: un docente asignado puede consultar las entregas de la comisión")
    void deberiaListarPorComision_cuandoElDocenteEstaAsignado() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(10L, "sub-doc")).thenReturn(true);
        when(catedraClient.listarEquipos(10L, null)).thenReturn(List.of(equipo(7L)));
        when(hitoRepository.findByCursoLectivoIdOrderByOrdenAscIdAsc(2L))
                .thenReturn(List.of(hito(3L)));
        when(entregaRepository.findByHitoIdInAndEquipoIdIn(List.of(3L), List.of(7L)))
                .thenReturn(List.of());

        assertThat(service.listarPorComision(10L, null)).hasSize(1);
    }

    @Test
    @DisplayName("RN-01: un docente no asignado recibe 403")
    void deberiaRechazarConsulta_cuandoElDocenteNoEstaAsignado() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(10L, "sub-doc")).thenReturn(false);

        assertThrows(AccesoDenegadoException.class,
                () -> service.listarPorComision(10L, null));
        verify(catedraClient, never()).listarEquipos(10L, null);
    }

    @Test
    @DisplayName("D04: si ms-catedra falla, responde 503")
    void deberiaPropagarServicioNoDisponible_cuandoFallaCatedraEnD04() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.listarEquipos(10L, null))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> service.listarPorComision(10L, null));
    }

    @Test
    @DisplayName("D04: filtra por hitoId cuando se informa")
    void deberiaFiltrarPorHito_cuandoSeInformaHitoId() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.listarEquipos(10L, null)).thenReturn(List.of(equipo(7L)));
        Hito hito = hito(3L);
        when(hitoRepository.findByIdAndCursoLectivoId(3L, 2L)).thenReturn(Optional.of(hito));
        when(entregaRepository.findByHitoIdInAndEquipoIdIn(List.of(3L), List.of(7L)))
                .thenReturn(List.of());

        assertThat(service.listarPorComision(10L, 3L)).hasSize(1);
        verify(hitoRepository, never()).findByCursoLectivoIdOrderByOrdenAscIdAsc(2L);
    }

    @Test
    @DisplayName("D04: un hito de otro curso responde 404")
    void deberiaLanzarNoEncontrado_cuandoElHitoNoPerteneceAlCurso() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.listarEquipos(10L, null)).thenReturn(List.of(equipo(7L)));
        when(hitoRepository.findByIdAndCursoLectivoId(99L, 2L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> service.listarPorComision(10L, 99L));
    }

    @Test
    @DisplayName("D05: un alumno sin equipo recibe una lista vacía")
    void deberiaDevolverListaVacia_cuandoElAlumnoNoTieneEquipo() {
        when(usuarioActual.sub()).thenReturn("sub-alumno");
        when(catedraClient.obtenerEstudiantePorSub("sub-alumno"))
                .thenReturn(estudiante(null));

        assertThat(service.listarMiEquipo()).isEmpty();
        verifyNoInteractions(hitoRepository, entregaRepository);
    }

    @Test
    @DisplayName("D05: un alumno con equipo recibe sus entregas y pendientes")
    void deberiaListarLasEntregasDelEquipo_cuandoElAlumnoTieneEquipo() {
        when(usuarioActual.sub()).thenReturn("sub-alumno");
        when(catedraClient.obtenerEstudiantePorSub("sub-alumno"))
                .thenReturn(estudiante(7L));
        Hito hito = hito(3L);
        when(hitoRepository.findByCursoLectivoIdOrderByOrdenAscIdAsc(2L))
                .thenReturn(List.of(hito));
        when(entregaRepository.findByHitoIdInAndEquipoIdIn(List.of(3L), List.of(7L)))
                .thenReturn(List.of(entrega(hito)));

        List<EntregaEstadoResponse> resultado = service.listarMiEquipo();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().estado()).isEqualTo("CORREGIDA");
        assertThat(resultado.getFirst().nota()).isEqualTo(8);
    }

    @Test
    @DisplayName("D05: si ms-catedra falla, responde 503")
    void deberiaPropagarServicioNoDisponible_cuandoFallaCatedraEnD05() {
        when(usuarioActual.sub()).thenReturn("sub-alumno");
        when(catedraClient.obtenerEstudiantePorSub("sub-alumno"))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        assertThrows(ServicioNoDisponibleException.class, () -> service.listarMiEquipo());
    }

    @Test
    @DisplayName("I07: devuelve el contrato mínimo para los equipos indicados")
    void deberiaListarInterno_cuandoSeInformanCursoYEquipos() {
        Hito hito = hito(3L);
        when(hitoRepository.findByCursoLectivoIdOrderByOrdenAscIdAsc(2L))
                .thenReturn(List.of(hito));
        when(entregaRepository.findByHitoIdInAndEquipoIdIn(List.of(3L), List.of(7L)))
                .thenReturn(List.of(entrega(hito)));

        List<EntregaInternaResponse> resultado = service.listarInterno(2L, List.of(7L, 7L));

        assertThat(resultado).containsExactly(
                new EntregaInternaResponse(7L, 3L, "CORREGIDA", false, 8));
        verify(entregaRepository).findByHitoIdInAndEquipoIdIn(List.of(3L), List.of(7L));
    }

    private EquipoDto equipo(Long id) {
        return new EquipoDto(id, "Equipo " + id, 10L, 2L, "ASEGURADO", true, "sub-rep", List.of());
    }

    private EstudianteDto estudiante(Long equipoId) {
        return new EstudianteDto(5L, "sub-alumno", true, 10L, 2L, equipoId);
    }

    private Hito hito(Long id) {
        return new Hito(id, 2L, "Sprint 1", TipoHito.SPRINT, 1,
                Instant.parse("2026-03-01T03:00:00Z"),
                Instant.parse("2026-03-16T02:59:59Z"));
    }

    private Entrega entrega(Hito hito) {
        return new Entrega(30L, hito, 7L, EstadoEntrega.CORREGIDA, false, 8);
    }
}
