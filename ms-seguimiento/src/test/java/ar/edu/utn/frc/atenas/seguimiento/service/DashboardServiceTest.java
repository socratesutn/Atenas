package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.client.DocumentacionClient;
import ar.edu.utn.frc.atenas.seguimiento.config.ActividadProperties;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EntregaEquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.ActividadIntegrante;
import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;
import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import ar.edu.utn.frc.atenas.seguimiento.entity.TipoEvento;
import ar.edu.utn.frc.atenas.seguimiento.repository.ActividadIntegranteRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.AsistenciaRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.EventoRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.RepositorioVinculadoRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.SnapshotActividadRepository;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.DetalleEquipo;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** F13, S09 y S10: orquestación del dashboard y degradación controlada (RN27, RN28, RN33). */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Long EQUIPO_ID = 7L;
    private static final Long COMISION_ID = 10L;
    private static final Long CURSO_ID = 2L;
    private static final Long HITO_ID = 3L;
    private static final Instant AHORA = Instant.parse("2026-11-19T03:00:00Z");

    @Mock
    private CatedraClient catedraClient;
    @Mock
    private DocumentacionClient documentacionClient;
    @Mock
    private AccesoComision acceso;
    @Mock
    private UsuarioActual usuarioActual;
    @Mock
    private EventoRepository eventos;
    @Mock
    private AsistenciaRepository asistencias;
    @Mock
    private RepositorioVinculadoRepository repositorios;
    @Mock
    private SnapshotActividadRepository snapshots;
    @Mock
    private ActividadIntegranteRepository actividades;

    private DashboardService servicio;
    private ActividadProperties propiedadesActividad;

    @BeforeEach
    void preparar() {
        propiedadesActividad = new ActividadProperties();
        servicio = new DashboardService(catedraClient, documentacionClient, acceso, usuarioActual,
                eventos, asistencias, repositorios, snapshots, actividades,
                new CalculadorActividad(), propiedadesActividad);
    }

    private EquipoDto equipo() {
        return new EquipoDto(EQUIPO_ID, "Equipo 1", COMISION_ID, CURSO_ID, "ASEGURADO", true, "sub-a",
                List.of(new EquipoDto.IntegranteDto(5L, "sub-a", "411626", "UsuarioA")));
    }

    private Evento evento() {
        return Evento.builder().id(11L).comisionLectivaId(COMISION_ID).tipo(TipoEvento.CLASE)
                .fecha(AHORA).build();
    }

    private Asistencia asistencia(Evento evento) {
        return Asistencia.builder().evento(evento).estudianteSub("sub-a").equipoId(EQUIPO_ID)
                .presente(true).registradaPorSub("sub-doc").fechaRegistro(AHORA).build();
    }

    private RepositorioVinculado repositorio() {
        return RepositorioVinculado.builder().id(1L).equipoId(EQUIPO_ID).build();
    }

    private SnapshotActividad snapshot(EstadoSnapshot estado, Integer totales, Integer sinAtribuir) {
        return SnapshotActividad.builder().id(20L).repositorio(repositorio()).hitoId(HITO_ID)
                .fechaConsulta(AHORA).estado(estado).commitsTotales(totales)
                .commitsSinAtribuir(sinAtribuir).build();
    }

    private ActividadIntegrante fila(SnapshotActividad snapshot, String sub, String usuario, int commits) {
        return ActividadIntegrante.builder().snapshot(snapshot).estudianteSub(sub)
                .usuarioGithub(usuario).commits(commits).build();
    }

    private void prepararDatosPropios() {
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo());
        when(eventos.findByComisionLectivaIdOrderByFechaAsc(COMISION_ID)).thenReturn(List.of(evento()));
        when(asistencias.findDelEquipoEnComision(EQUIPO_ID, COMISION_ID))
                .thenReturn(List.of(asistencia(evento())));
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.of(repositorio()));
        when(snapshots.findHistorialPorEquipo(EQUIPO_ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("RN-33: si ms-documentacion no responde, S10 responde 200 con entregas no disponibles")
    void deberiaDegradarSoloEntregas_cuandoDocumentacionNoResponde() {
        prepararDatosPropios();
        when(documentacionClient.listarEntregas(CURSO_ID, List.of(EQUIPO_ID)))
                .thenThrow(new ServicioNoDisponibleException("ms-documentacion caído"));

        DetalleEquipo respuesta = servicio.dashboardDeEquipo(EQUIPO_ID);

        assertThat(respuesta.entregas().disponible()).isFalse();
        assertThat(respuesta.entregas().entregas()).isEmpty();
        assertThat(respuesta.asistencia().registros()).isEqualTo(1);
        assertThat(respuesta.actividad().disponible()).isTrue();
        verify(acceso).exigirJefeODocente(COMISION_ID);
    }

    @Test
    @DisplayName("RN-33: S09 también marca entregas no disponibles cuando falla ms-documentacion")
    void deberiaDegradarEntregasDelResumen_cuandoDocumentacionNoResponde() {
        when(catedraClient.listarEquiposPorComision(COMISION_ID)).thenReturn(List.of(equipo()));
        when(eventos.findByComisionLectivaIdOrderByFechaAsc(COMISION_ID)).thenReturn(List.of());
        when(asistencias.findDelEquipoEnComision(EQUIPO_ID, COMISION_ID)).thenReturn(List.of());
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.empty());
        when(documentacionClient.listarEntregas(CURSO_ID, List.of(EQUIPO_ID)))
                .thenThrow(new ServicioNoDisponibleException("ms-documentacion caído"));

        var respuesta = servicio.dashboardDeEquipos(COMISION_ID, null);

        assertThat(respuesta.entregas().disponible()).isFalse();
        assertThat(respuesta.equipos()).singleElement().satisfies(equipo -> {
            assertThat(equipo.entregas().disponible()).isFalse();
            assertThat(equipo.actividad().disponible()).isFalse();
        });
        verify(acceso).exigirJefeODocente(COMISION_ID);
    }

    @Test
    @DisplayName("Si ms-catedra no responde, el dashboard termina en 503 sin pedir entregas")
    void deberiaPropagarServicioNoDisponible_cuandoCatedraNoResponde() {
        when(catedraClient.obtenerEquipo(EQUIPO_ID))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra caído"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.dashboardDeEquipo(EQUIPO_ID));

        verifyNoInteractions(documentacionClient, eventos, asistencias, repositorios, snapshots, actividades);
    }

    @Test
    @DisplayName("RN-01: un docente de otra comisión recibe 403 y no se arma el dashboard")
    void deberiaLanzarAccesoDenegado_cuandoElDocenteEsDeOtraComision() {
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo());
        doThrow(new AccesoDenegadoException("Docente de otra comisión", "RN-01"))
                .when(acceso).exigirJefeODocente(COMISION_ID);

        AccesoDenegadoException excepcion = assertThrows(AccesoDenegadoException.class,
                () -> servicio.dashboardDeEquipo(EQUIPO_ID));

        assertThat(excepcion.getRegla()).isEqualTo("RN-01");
        verifyNoInteractions(documentacionClient, eventos, asistencias, repositorios, snapshots, actividades);
    }

    @Test
    @DisplayName("RN-01: el filtro por curso lectivo solo lo puede usar el jefe")
    void deberiaLanzarAccesoDenegado_cuandoUnDocentePideElDashboardPorCurso() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);

        assertThrows(AccesoDenegadoException.class, () -> servicio.dashboardDeEquipos(null, CURSO_ID));

        verifyNoInteractions(catedraClient, documentacionClient);
    }

    @Test
    @DisplayName("RN-27: un snapshot DESACTUALIZADO conserva sus datos y recalcula las marcas al consultar")
    void deberiaCalcularMarcas_cuandoElSnapshotEstaDesactualizado() {
        prepararDatosPropios();
        SnapshotActividad desactualizado = snapshot(EstadoSnapshot.DESACTUALIZADO, 5, 0);
        when(snapshots.findHistorialPorEquipo(EQUIPO_ID)).thenReturn(List.of(desactualizado));
        when(actividades.findPorSnapshots(List.of(20L))).thenReturn(List.of(
                fila(desactualizado, "sub-a", "UsuarioA", 4),
                fila(desactualizado, "sub-b", "UsuarioB", 1)));
        when(documentacionClient.listarEntregas(CURSO_ID, List.of(EQUIPO_ID))).thenReturn(List.of());

        DetalleEquipo respuesta = servicio.dashboardDeEquipo(EQUIPO_ID);

        var instantes = respuesta.actividad().snapshots();
        assertThat(instantes).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.estado()).isEqualTo(EstadoSnapshot.DESACTUALIZADO);
            assertThat(snapshot.commitsTotales()).isEqualTo(5);
            assertThat(snapshot.sinActividad()).isFalse();
            assertThat(snapshot.integrantes())
                    .extracting(ResultadosDashboard.ContribucionEquipo::bajaContribucion)
                    .containsExactly(false, true);
        });
    }

    @Test
    @DisplayName("RN-27: el umbral configurable se aplica al recalcular las marcas al consultar")
    void deberiaUsarElUmbralConfigurado_cuandoSeArmaElDashboard() {
        prepararDatosPropios();
        propiedadesActividad.setUmbralBajaContribucion(1.0);
        SnapshotActividad actualizado = snapshot(EstadoSnapshot.ACTUALIZADO, 12, 0);
        when(snapshots.findHistorialPorEquipo(EQUIPO_ID)).thenReturn(List.of(actualizado));
        when(actividades.findPorSnapshots(List.of(20L))).thenReturn(List.of(
                fila(actualizado, "sub-a", "UsuarioA", 5),
                fila(actualizado, "sub-b", "UsuarioB", 4),
                fila(actualizado, "sub-c", "UsuarioC", 3)));
        when(documentacionClient.listarEntregas(CURSO_ID, List.of(EQUIPO_ID))).thenReturn(List.of());

        DetalleEquipo respuesta = servicio.dashboardDeEquipo(EQUIPO_ID);

        assertThat(respuesta.actividad().snapshots()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.integrantes())
                    .extracting(ResultadosDashboard.ContribucionEquipo::bajaContribucion)
                    .containsExactly(false, false, true);
        });
    }

    @Test
    @DisplayName("RN-28: un snapshot NO_DISPONIBLE se muestra sin contadores ni marcas informativas")
    void deberiaMostrarSinMarcas_cuandoElSnapshotNoEstaDisponible() {
        prepararDatosPropios();
        SnapshotActividad noDisponible = snapshot(EstadoSnapshot.NO_DISPONIBLE, null, null);
        when(snapshots.findHistorialPorEquipo(EQUIPO_ID)).thenReturn(List.of(noDisponible));
        when(actividades.findPorSnapshots(List.of(20L))).thenReturn(List.of());
        when(documentacionClient.listarEntregas(CURSO_ID, List.of(EQUIPO_ID))).thenReturn(List.of());

        DetalleEquipo respuesta = servicio.dashboardDeEquipo(EQUIPO_ID);

        assertThat(respuesta.actividad().snapshots()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.estado()).isEqualTo(EstadoSnapshot.NO_DISPONIBLE);
            assertThat(snapshot.commitsTotales()).isNull();
            assertThat(snapshot.sinActividad()).isFalse();
            assertThat(snapshot.integrantes()).isEmpty();
        });
    }

    @Test
    @DisplayName("RN-28: un equipo sin repositorio vinculado muestra la actividad no disponible")
    void deberiaMostrarActividadNoDisponible_cuandoElEquipoNoTieneRepositorio() {
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo());
        when(eventos.findByComisionLectivaIdOrderByFechaAsc(COMISION_ID)).thenReturn(List.of());
        when(asistencias.findDelEquipoEnComision(EQUIPO_ID, COMISION_ID)).thenReturn(List.of());
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.empty());
        when(documentacionClient.listarEntregas(CURSO_ID, List.of(EQUIPO_ID)))
                .thenReturn(List.of(new EntregaEquipoDto(EQUIPO_ID, HITO_ID, "PENDIENTE", false, null)));

        DetalleEquipo respuesta = servicio.dashboardDeEquipo(EQUIPO_ID);

        assertThat(respuesta.actividad().disponible()).isFalse();
        assertThat(respuesta.actividad().snapshots()).isEmpty();
        assertThat(respuesta.entregas().entregas()).singleElement()
                .extracting(EntregaEquipoDto::estado).isEqualTo("PENDIENTE");
        verifyNoInteractions(snapshots, actividades);
    }

    @Test
    @DisplayName("S09: el jefe consulta por curso y recibe las entregas de todos los equipos")
    void deberiaResponderResumenPorCurso_cuandoLoPideElJefe() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.listarEquiposPorCurso(CURSO_ID)).thenReturn(List.of(equipo()));
        when(eventos.findByComisionLectivaIdOrderByFechaAsc(COMISION_ID)).thenReturn(List.of());
        when(asistencias.findDelEquipoEnComision(EQUIPO_ID, COMISION_ID)).thenReturn(List.of());
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.empty());
        when(documentacionClient.listarEntregas(CURSO_ID, List.of(EQUIPO_ID)))
                .thenReturn(List.of(new EntregaEquipoDto(EQUIPO_ID, HITO_ID, "CORREGIDA", false, 8)));

        var respuesta = servicio.dashboardDeEquipos(null, CURSO_ID);

        assertThat(respuesta.cursoLectivoId()).isEqualTo(CURSO_ID);
        assertThat(respuesta.entregas().disponible()).isTrue();
        assertThat(respuesta.equipos()).singleElement().satisfies(equipo -> {
            assertThat(equipo.entregas().entregas()).singleElement()
                    .extracting(EntregaEquipoDto::nota).isEqualTo(8);
        });
        verify(acceso, never()).exigirJefeODocente(any());
    }
}
