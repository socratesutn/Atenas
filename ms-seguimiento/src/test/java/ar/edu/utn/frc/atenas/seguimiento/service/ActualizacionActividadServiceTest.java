package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.client.CommitGitHub;
import ar.edu.utn.frc.atenas.seguimiento.client.DocumentacionClient;
import ar.edu.utn.frc.atenas.seguimiento.client.GitHubClient;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.HitoDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import ar.edu.utn.frc.atenas.seguimiento.exception.RespuestaInesperadaException;
import ar.edu.utn.frc.atenas.seguimiento.repository.RepositorioVinculadoRepository;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.IntegranteEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.Resultado;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadoActualizacion.DetalleSprint;
import ar.edu.utn.frc.atenas.seguimiento.service.SnapshotService.MotivoFalla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** F12, S08: orquestación de la actualización y degradación ante fallas (RN27, RN28, RN31). */
@ExtendWith(MockitoExtension.class)
class ActualizacionActividadServiceTest {

    private static final Long EQUIPO_ID = 7L;
    private static final Long HITO_ID = 3L;
    private static final Instant INICIO = Instant.parse("2026-11-01T03:00:00Z");
    private static final Instant LIMITE = Instant.parse("2026-11-15T03:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-20T03:00:00Z");
    private static final List<IntegranteEquipo> INTEGRANTES =
            List.of(new IntegranteEquipo("sub-a", "UsuarioA"));

    @Mock
    private CatedraClient catedraClient;
    @Mock
    private DocumentacionClient documentacionClient;
    @Mock
    private GitHubClient gitHubClient;
    @Mock
    private RepositorioVinculadoRepository repositorios;
    @Mock
    private CalculadorActividad calculador;
    @Mock
    private SnapshotService snapshots;
    @Mock
    private AccesoComision accesoComision;

    private ActualizacionActividadService servicio;
    private RepositorioVinculado repositorio;
    private EquipoDto equipo;
    private HitoDto hito;
    private Resultado resultado;

    @BeforeEach
    void preparar() {
        servicio = new ActualizacionActividadService(catedraClient, documentacionClient, gitHubClient,
                repositorios, calculador, snapshots, accesoComision);
        repositorio = repositorio(EQUIPO_ID);
        equipo = equipo(EQUIPO_ID);
        hito = hito(HITO_ID);
        resultado = new Resultado(5, 1, List.of(new CalculadorActividad.CommitsDeIntegrante("sub-a", "UsuarioA", 4)));
    }

    private RepositorioVinculado repositorio(Long equipoId) {
        return RepositorioVinculado.builder().id(equipoId).equipoId(equipoId)
                .propietario("Usuario").nombre("Repo").ramaSeguimiento("main").build();
    }

    private EquipoDto equipo(Long equipoId) {
        return new EquipoDto(equipoId, "Equipo", 10L, 2L, "ASEGURADO", true, "sub-a",
                List.of(new EquipoDto.IntegranteDto(5L, "sub-a", "411626", "UsuarioA")));
    }

    private HitoDto hito(Long hitoId) {
        ZoneId zona = ZoneId.of(ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria.ZONA_POR_DEFECTO);
        return new HitoDto(hitoId, 2L, "Sprint 1", "SPRINT", 1,
                OffsetDateTime.ofInstant(INICIO, zona),
                OffsetDateTime.ofInstant(LIMITE, zona));
    }

    private SnapshotActividad snapshot(EstadoSnapshot estado, Integer totales, Integer sinAtribuir) {
        return SnapshotActividad.builder().id(20L).repositorio(repositorio).hitoId(HITO_ID)
                .fechaConsulta(AHORA).estado(estado)
                .commitsTotales(totales).commitsSinAtribuir(sinAtribuir).build();
    }

    private void prepararEquipoYSprint() {
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.of(repositorio));
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo);
        when(documentacionClient.listarHitosSprint(2L)).thenReturn(List.of(hito));
        when(snapshots.ultimoActualizadoDespuesDelLimite(repositorio, HITO_ID, LIMITE))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("RN-27: actualiza el sprint con los commits calculados y guarda el snapshot")
    void deberiaGuardarSnapshotActualizado_cuandoTodoResponde() {
        prepararEquipoYSprint();
        List<CommitGitHub> commits = List.of(new CommitGitHub("sha", INICIO, "usuariob", false));
        when(gitHubClient.listarCommits("Usuario", "Repo", "main", INICIO, LIMITE)).thenReturn(commits);
        when(calculador.calcular(commits, INICIO, LIMITE, INTEGRANTES)).thenReturn(resultado);
        SnapshotActividad actualizado = snapshot(EstadoSnapshot.ACTUALIZADO, 5, 1);
        when(snapshots.guardarActualizado(repositorio, HITO_ID, resultado, AHORA)).thenReturn(actualizado);

        ResultadoActualizacion respuesta = servicio.actualizarEquipo(EQUIPO_ID, AHORA);

        assertThat(respuesta.equipoId()).isEqualTo(EQUIPO_ID);
        assertThat(respuesta.detalles()).singleElement().satisfies(detalle -> {
            assertThat(detalle.hitoId()).isEqualTo(HITO_ID);
            assertThat(detalle.nombreHito()).isEqualTo("Sprint 1");
            assertThat(detalle.snapshot()).isSameAs(actualizado);
            assertThat(detalle.omitido()).isFalse();
        });
        verify(accesoComision).exigirJefeODocente(10L);
        verify(gitHubClient).listarCommits("Usuario", "Repo", "main", INICIO, LIMITE);
        verify(snapshots).guardarActualizado(repositorio, HITO_ID, resultado, AHORA);
    }

    @Test
    @DisplayName("RN-28: si GitHub no responde, conserva el último snapshot como DESACTUALIZADO")
    void deberiaMarcarDesactualizado_cuandoGitHubNoResponde() {
        prepararEquipoYSprint();
        when(gitHubClient.listarCommits("Usuario", "Repo", "main", INICIO, LIMITE))
                .thenThrow(new ServicioNoDisponibleException("GitHub no respondió"));
        SnapshotActividad desactualizado = snapshot(EstadoSnapshot.DESACTUALIZADO, 9, 1);
        when(snapshots.registrarFalla(repositorio, HITO_ID, MotivoFalla.NO_RESPONDE, AHORA))
                .thenReturn(desactualizado);

        ResultadoActualizacion respuesta = servicio.actualizarEquipo(EQUIPO_ID, AHORA);

        assertThat(respuesta.detalles()).singleElement()
                .extracting(DetalleSprint::snapshot).isSameAs(desactualizado);
        verify(calculador, never()).calcular(anyList(), any(), any(), anyList());
    }

    @Test
    @DisplayName("RN-28: si un servicio interno no responde, degrada los snapshots conocidos y no llama a GitHub")
    void deberiaDegradarSprintsConocidos_cuandoUnServicioInternoNoResponde() {
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.of(repositorio));
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo);
        when(documentacionClient.listarHitosSprint(2L))
                .thenThrow(new ServicioNoDisponibleException("ms-documentacion no respondió"));
        SnapshotActividad desactualizado = snapshot(EstadoSnapshot.DESACTUALIZADO, 9, 1);
        when(snapshots.registrarFallaDeServicio(repositorio, MotivoFalla.NO_RESPONDE, AHORA))
                .thenReturn(List.of(desactualizado));

        ResultadoActualizacion respuesta = servicio.actualizarEquipo(EQUIPO_ID, AHORA);

        assertThat(respuesta.detalles()).singleElement().satisfies(detalle -> {
            assertThat(detalle.snapshot().getEstado()).isEqualTo(EstadoSnapshot.DESACTUALIZADO);
            assertThat(detalle.hitoId()).isEqualTo(HITO_ID);
        });
        verifyNoInteractions(gitHubClient);
    }

    @Test
    @DisplayName("RN-01: con I01 caído un docente no degrada snapshots sin verificar su comisión")
    void deberiaPropagarFallaDeCatedra_cuandoUnDocenteNoPuedeVerificarLaComision() {
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.of(repositorio));
        when(catedraClient.obtenerEquipo(EQUIPO_ID))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra no respondió"));
        when(accesoComision.esJefe()).thenReturn(false);

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.actualizarEquipo(EQUIPO_ID, AHORA));

        verify(accesoComision, never()).exigirJefeODocente(any());
        verify(snapshots, never()).registrarFallaDeServicio(any(), any(), any());
        verifyNoInteractions(documentacionClient, gitHubClient);
    }

    @Test
    @DisplayName("RN-28: con I01 caído el jefe sí degrada los snapshots conocidos")
    void deberiaDegradarSprintsConocidos_cuandoCatedraNoRespondeYEsJefe() {
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.of(repositorio));
        when(catedraClient.obtenerEquipo(EQUIPO_ID))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra no respondió"));
        when(accesoComision.esJefe()).thenReturn(true);
        SnapshotActividad desactualizado = snapshot(EstadoSnapshot.DESACTUALIZADO, 9, 1);
        when(snapshots.registrarFallaDeServicio(repositorio, MotivoFalla.NO_RESPONDE, AHORA))
                .thenReturn(List.of(desactualizado));

        ResultadoActualizacion respuesta = servicio.actualizarEquipo(EQUIPO_ID, AHORA);

        assertThat(respuesta.detalles()).singleElement()
                .extracting(DetalleSprint::snapshot).isSameAs(desactualizado);
        verify(accesoComision, never()).exigirJefeODocente(any());
        verifyNoInteractions(documentacionClient, gitHubClient);
    }

    @Test
    @DisplayName("S08: un 404 de I01 se propaga sin degradar snapshots")
    void deberiaPropagar404_cuandoCatedraNoEncuentraElEquipo() {
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.of(repositorio));
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenThrow(new NoEncontradoException("No existe el equipo"));

        assertThrows(NoEncontradoException.class, () -> servicio.actualizarEquipo(EQUIPO_ID, AHORA));

        verify(snapshots, never()).registrarFallaDeServicio(any(), any(), any());
        verifyNoInteractions(documentacionClient, gitHubClient);
    }

    @Test
    @DisplayName("RN-28: una respuesta inesperada de GitHub deja el snapshot NO_DISPONIBLE")
    void deberiaCrearNoDisponible_cuandoLaRespuestaEsInesperada() {
        prepararEquipoYSprint();
        when(gitHubClient.listarCommits("Usuario", "Repo", "main", INICIO, LIMITE))
                .thenThrow(new RespuestaInesperadaException("JSON inesperado"));
        SnapshotActividad noDisponible = snapshot(EstadoSnapshot.NO_DISPONIBLE, null, null);
        when(snapshots.registrarFalla(repositorio, HITO_ID, MotivoFalla.RESPUESTA_INESPERADA, AHORA))
                .thenReturn(noDisponible);

        ResultadoActualizacion respuesta = servicio.actualizarEquipo(EQUIPO_ID, AHORA);

        assertThat(respuesta.detalles()).singleElement().satisfies(detalle -> {
            assertThat(detalle.snapshot().getEstado()).isEqualTo(EstadoSnapshot.NO_DISPONIBLE);
            assertThat(detalle.snapshot().getCommitsTotales()).isNull();
        });
        verify(snapshots, never()).registrarFalla(repositorio, HITO_ID, MotivoFalla.NO_RESPONDE, AHORA);
    }

    @Test
    @DisplayName("RN-28: sin snapshot previo y con GitHub caído se crea uno NO_DISPONIBLE")
    void deberiaCrearNoDisponible_cuandoNoHaySnapshotPrevio() {
        prepararEquipoYSprint();
        when(gitHubClient.listarCommits("Usuario", "Repo", "main", INICIO, LIMITE))
                .thenThrow(new ServicioNoDisponibleException("GitHub no respondió"));
        SnapshotActividad noDisponible = snapshot(EstadoSnapshot.NO_DISPONIBLE, null, null);
        when(snapshots.registrarFalla(repositorio, HITO_ID, MotivoFalla.NO_RESPONDE, AHORA))
                .thenReturn(noDisponible);

        ResultadoActualizacion respuesta = servicio.actualizarEquipo(EQUIPO_ID, AHORA);

        assertThat(respuesta.detalles()).singleElement()
                .extracting(DetalleSprint::snapshot)
                .extracting(SnapshotActividad::getEstado)
                .isEqualTo(EstadoSnapshot.NO_DISPONIBLE);
        verify(snapshots).registrarFalla(repositorio, HITO_ID, MotivoFalla.NO_RESPONDE, AHORA);
    }

    @Test
    @DisplayName("RN-27: se omite el sprint cuyo último snapshot se calculó después de la fecha límite")
    void deberiaOmitir_cuandoElUltimoSnapshotEsPosteriorAlLimite() {
        prepararEquipoYSprint();
        SnapshotActividad cerrado = snapshot(EstadoSnapshot.ACTUALIZADO, 12, 2);
        when(snapshots.ultimoActualizadoDespuesDelLimite(repositorio, HITO_ID, LIMITE))
                .thenReturn(Optional.of(cerrado));

        ResultadoActualizacion respuesta = servicio.actualizarEquipo(EQUIPO_ID, AHORA);

        assertThat(respuesta.detalles()).singleElement().satisfies(detalle -> {
            assertThat(detalle.omitido()).isTrue();
            assertThat(detalle.snapshot()).isSameAs(cerrado);
        });
        verifyNoInteractions(gitHubClient);
        verify(snapshots, never()).guardarActualizado(any(), any(), any(), any());
    }

    @Test
    @DisplayName("RN-28: la falla de un equipo no corta la actualización de los demás")
    void deberiaSeguirConLosDemasEquipos_cuandoUnoFalla() {
        RepositorioVinculado otro = repositorio(8L);
        when(repositorios.findAll()).thenReturn(List.of(repositorio, otro));
        when(catedraClient.obtenerEquipo(EQUIPO_ID))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra no respondió"));
        SnapshotActividad desactualizado = snapshot(EstadoSnapshot.DESACTUALIZADO, 9, 1);
        when(snapshots.registrarFallaDeServicio(repositorio, MotivoFalla.NO_RESPONDE, AHORA))
                .thenReturn(List.of(desactualizado));

        when(catedraClient.obtenerEquipo(8L)).thenReturn(equipo(8L));
        when(documentacionClient.listarHitosSprint(2L)).thenReturn(List.of(hito));
        when(snapshots.ultimoActualizadoDespuesDelLimite(otro, HITO_ID, LIMITE)).thenReturn(Optional.empty());
        when(gitHubClient.listarCommits("Usuario", "Repo", "main", INICIO, LIMITE)).thenReturn(List.of());
        when(calculador.calcular(anyList(), eq(INICIO), eq(LIMITE), anyList())).thenReturn(resultado);
        SnapshotActividad actualizado = snapshot(EstadoSnapshot.ACTUALIZADO, 5, 1);
        when(snapshots.guardarActualizado(otro, HITO_ID, resultado, AHORA)).thenReturn(actualizado);

        List<ResultadoActualizacion> respuesta = servicio.actualizarTodos(AHORA);

        assertThat(respuesta).hasSize(2);
        assertThat(respuesta.get(0).detalles()).singleElement()
                .extracting(DetalleSprint::snapshot)
                .extracting(SnapshotActividad::getEstado)
                .isEqualTo(EstadoSnapshot.DESACTUALIZADO);
        assertThat(respuesta.get(1).detalles()).singleElement()
                .extracting(DetalleSprint::snapshot)
                .extracting(SnapshotActividad::getEstado)
                .isEqualTo(EstadoSnapshot.ACTUALIZADO);
    }

    @Test
    @DisplayName("S08: responde 404 cuando el equipo no tiene repositorio vinculado")
    void deberiaLanzar404_cuandoNoHayRepositorioVinculado() {
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> servicio.actualizarEquipo(EQUIPO_ID, AHORA));
        verifyNoInteractions(catedraClient, documentacionClient, gitHubClient);
    }
}
