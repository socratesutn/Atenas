package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.seguimiento.entity.ActividadIntegrante;
import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import ar.edu.utn.frc.atenas.seguimiento.repository.ActividadIntegranteRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.SnapshotActividadRepository;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.CommitsDeIntegrante;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.Resultado;
import ar.edu.utn.frc.atenas.seguimiento.service.SnapshotService.MotivoFalla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    private static final Long HITO = 7L;
    private static final Instant ANTES = Instant.parse("2026-11-10T03:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-11T03:00:00Z");

    @Mock
    private SnapshotActividadRepository snapshots;
    @Mock
    private ActividadIntegranteRepository actividades;
    @Captor
    private ArgumentCaptor<SnapshotActividad> snapshotGuardado;
    @Captor
    private ArgumentCaptor<List<ActividadIntegrante>> filasGuardadas;

    private SnapshotService servicio;
    private RepositorioVinculado repositorio;

    @BeforeEach
    void preparar() {
        servicio = new SnapshotService(snapshots, actividades);
        repositorio = RepositorioVinculado.builder().id(1L).equipoId(10L).build();
    }

    private void devolverLoGuardado() {
        when(snapshots.save(any(SnapshotActividad.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void ultimoSnapshotEs(SnapshotActividad snapshot) {
        when(snapshots.findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(1L, HITO))
                .thenReturn(Optional.ofNullable(snapshot));
    }

    private SnapshotActividad snapshotPrevio(EstadoSnapshot estado) {
        boolean conDatos = estado != EstadoSnapshot.NO_DISPONIBLE;
        return SnapshotActividad.builder().id(5L).repositorio(repositorio).hitoId(HITO)
                .fechaConsulta(ANTES).estado(estado)
                .commitsTotales(conDatos ? 9 : null).commitsSinAtribuir(conDatos ? 1 : null).build();
    }

    // ---------------------------------------------------------------- guardarActualizado

    @Test
    @DisplayName("RN-27: guarda un snapshot ACTUALIZADO con una fila por integrante, incluso con 0 commits")
    void deberiaGuardarSnapshotActualizado_conUnaFilaPorIntegrante() {
        devolverLoGuardado();
        Resultado resultado = new Resultado(5, 1, List.of(
                new CommitsDeIntegrante("sub-a", "usuarioa", 3),
                new CommitsDeIntegrante("sub-b", "usuariob", 1),
                new CommitsDeIntegrante("sub-c", null, 0)));

        SnapshotActividad guardado = servicio.guardarActualizado(repositorio, HITO, resultado, AHORA);

        verify(snapshots).save(snapshotGuardado.capture());
        SnapshotActividad snapshot = snapshotGuardado.getValue();
        assertThat(snapshot.getEstado()).isEqualTo(EstadoSnapshot.ACTUALIZADO);
        assertThat(snapshot.getFechaConsulta()).isEqualTo(AHORA);
        assertThat(snapshot.getHitoId()).isEqualTo(HITO);
        assertThat(snapshot.getRepositorio()).isSameAs(repositorio);
        assertThat(snapshot.getCommitsTotales()).isEqualTo(5);
        assertThat(snapshot.getCommitsSinAtribuir()).isEqualTo(1);
        assertThat(guardado).isSameAs(snapshot);

        verify(actividades).saveAll(filasGuardadas.capture());
        assertThat(filasGuardadas.getValue())
                .extracting(ActividadIntegrante::getEstudianteSub, ActividadIntegrante::getUsuarioGithub,
                        ActividadIntegrante::getCommits)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("sub-a", "usuarioa", 3),
                        org.assertj.core.groups.Tuple.tuple("sub-b", "usuariob", 1),
                        org.assertj.core.groups.Tuple.tuple("sub-c", null, 0));
        assertThat(filasGuardadas.getValue()).allMatch(f -> f.getSnapshot() == snapshot);
    }

    @Test
    @DisplayName("RN-27: no guarda un snapshot cuyos contadores no cierran")
    void deberiaRechazar_cuandoLosContadoresNoCierran() {
        Resultado incoherente = new Resultado(10, 1, List.of(new CommitsDeIntegrante("sub-a", "usuarioa", 3)));

        assertThrows(IllegalStateException.class,
                () -> servicio.guardarActualizado(repositorio, HITO, incoherente, AHORA));

        verify(snapshots, never()).save(any());
        verify(actividades, never()).saveAll(any());
    }

    // ---------------------------------------------------------------- registrarFalla

    @Test
    @DisplayName("RN-28: si no responde, el último snapshot pasa a DESACTUALIZADO y conserva sus datos")
    void deberiaMarcarDesactualizado_cuandoNoRespondeYHabiaDatos() {
        SnapshotActividad previo = snapshotPrevio(EstadoSnapshot.ACTUALIZADO);
        ultimoSnapshotEs(previo);
        devolverLoGuardado();

        SnapshotActividad resultado = servicio.registrarFalla(repositorio, HITO, MotivoFalla.NO_RESPONDE, AHORA);

        assertThat(resultado).isSameAs(previo);
        assertThat(previo.getEstado()).isEqualTo(EstadoSnapshot.DESACTUALIZADO);
        assertThat(previo.getCommitsTotales()).isEqualTo(9);
        assertThat(previo.getCommitsSinAtribuir()).isEqualTo(1);
        assertThat(previo.getFechaConsulta()).isEqualTo(ANTES);
        verify(snapshots).save(previo);
    }

    @Test
    @DisplayName("RN-28: si ya estaba DESACTUALIZADO y vuelve a fallar, no se modifica nada")
    void deberiaNoCambiarNada_cuandoYaEstabaDesactualizado() {
        SnapshotActividad previo = snapshotPrevio(EstadoSnapshot.DESACTUALIZADO);
        ultimoSnapshotEs(previo);

        SnapshotActividad resultado = servicio.registrarFalla(repositorio, HITO, MotivoFalla.NO_RESPONDE, AHORA);

        assertThat(resultado).isSameAs(previo);
        verify(snapshots, never()).save(any());
    }

    @Test
    @DisplayName("RN-28: si no responde y nunca hubo datos, se crea un snapshot NO_DISPONIBLE con contadores nulos")
    void deberiaCrearNoDisponible_cuandoNoRespondeYNuncaHuboDatos() {
        ultimoSnapshotEs(null);
        devolverLoGuardado();

        SnapshotActividad creado = servicio.registrarFalla(repositorio, HITO, MotivoFalla.NO_RESPONDE, AHORA);

        assertThat(creado.getEstado()).isEqualTo(EstadoSnapshot.NO_DISPONIBLE);
        assertThat(creado.getCommitsTotales()).isNull();
        assertThat(creado.getCommitsSinAtribuir()).isNull();
        assertThat(creado.getFechaConsulta()).isEqualTo(AHORA);
        assertThat(creado.getHitoId()).isEqualTo(HITO);
        assertThat(creado.getRepositorio()).isSameAs(repositorio);
        verify(actividades, never()).saveAll(any());
    }

    @Test
    @DisplayName("RN-28: una respuesta inesperada deja la evidencia NO_DISPONIBLE aunque hubiera datos previos")
    void deberiaCrearNoDisponible_cuandoLaRespuestaEsInesperada() {
        SnapshotActividad previo = snapshotPrevio(EstadoSnapshot.ACTUALIZADO);
        ultimoSnapshotEs(previo);
        devolverLoGuardado();

        SnapshotActividad creado = servicio.registrarFalla(repositorio, HITO, MotivoFalla.RESPUESTA_INESPERADA, AHORA);

        assertThat(creado).isNotSameAs(previo);
        assertThat(creado.getEstado()).isEqualTo(EstadoSnapshot.NO_DISPONIBLE);
        assertThat(creado.getCommitsTotales()).isNull();
        // El snapshot anterior queda intacto como historial.
        assertThat(previo.getEstado()).isEqualTo(EstadoSnapshot.ACTUALIZADO);
    }

    @Test
    @DisplayName("RN-28: si el último snapshot ya es NO_DISPONIBLE no se agrega otro igual")
    void deberiaNoCrearOtro_cuandoElUltimoYaEsNoDisponible() {
        SnapshotActividad previo = snapshotPrevio(EstadoSnapshot.NO_DISPONIBLE);
        ultimoSnapshotEs(previo);

        assertThat(servicio.registrarFalla(repositorio, HITO, MotivoFalla.NO_RESPONDE, AHORA)).isSameAs(previo);
        assertThat(servicio.registrarFalla(repositorio, HITO, MotivoFalla.RESPUESTA_INESPERADA, AHORA)).isSameAs(previo);

        verify(snapshots, never()).save(any());
    }

    // ---------------------------------------------------------------- omisión y falla de servicio

    @Test
    @DisplayName("RN-27: detecta un sprint cerrado cuando el último snapshot se calculó después del límite")
    void deberiaDevolverElSnapshot_cuandoSeActualizoDespuesDelLimite() {
        Instant limite = Instant.parse("2026-11-10T12:00:00Z");
        SnapshotActividad posterior = SnapshotActividad.builder().id(5L).repositorio(repositorio).hitoId(HITO)
                .fechaConsulta(AHORA).estado(EstadoSnapshot.ACTUALIZADO)
                .commitsTotales(9).commitsSinAtribuir(1).build();
        when(snapshots.findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(1L, HITO))
                .thenReturn(Optional.of(posterior));

        assertThat(servicio.ultimoActualizadoDespuesDelLimite(repositorio, HITO, limite)).contains(posterior);
    }

    @Test
    @DisplayName("RN-27: no omite el sprint si el snapshot es anterior al límite o no está ACTUALIZADO")
    void deberiaNoDevolverSnapshot_cuandoNoEsUnSprintCerrado() {
        Instant limite = Instant.parse("2026-11-21T03:00:00Z");
        when(snapshots.findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(1L, HITO))
                .thenReturn(Optional.of(snapshotPrevio(EstadoSnapshot.ACTUALIZADO)),
                        Optional.of(snapshotPrevio(EstadoSnapshot.DESACTUALIZADO)));

        assertThat(servicio.ultimoActualizadoDespuesDelLimite(repositorio, HITO, limite)).isEmpty();
        assertThat(servicio.ultimoActualizadoDespuesDelLimite(repositorio, HITO, limite)).isEmpty();
    }

    @Test
    @DisplayName("RN-28: si falla un servicio interno, degrada el último snapshot de cada sprint conocido")
    void deberiaDegradarUnSnapshotPorSprint_cuandoFallaUnServicioInterno() {
        Long otroHito = 8L;
        SnapshotActividad primero = snapshotPrevio(EstadoSnapshot.ACTUALIZADO);
        SnapshotActividad segundo = SnapshotActividad.builder().id(6L).repositorio(repositorio).hitoId(otroHito)
                .fechaConsulta(ANTES).estado(EstadoSnapshot.ACTUALIZADO).commitsTotales(4).commitsSinAtribuir(0).build();
        when(snapshots.findAllByRepositorioIdOrderByFechaConsultaDescIdDesc(1L))
                .thenReturn(List.of(segundo, primero));
        when(snapshots.findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(1L, otroHito))
                .thenReturn(Optional.of(segundo));
        when(snapshots.findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(1L, HITO))
                .thenReturn(Optional.of(primero));

        List<SnapshotActividad> degradados = servicio.registrarFallaDeServicio(
                repositorio, MotivoFalla.NO_RESPONDE, AHORA);

        assertThat(degradados).hasSize(2)
                .allSatisfy(snapshot -> assertThat(snapshot.getEstado()).isEqualTo(EstadoSnapshot.DESACTUALIZADO));
        verify(snapshots).save(primero);
        verify(snapshots).save(segundo);
    }
}
