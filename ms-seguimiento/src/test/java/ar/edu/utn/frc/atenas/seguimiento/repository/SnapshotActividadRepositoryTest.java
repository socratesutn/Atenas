package ar.edu.utn.frc.atenas.seguimiento.repository;

import ar.edu.utn.frc.atenas.seguimiento.entity.ActividadIntegrante;
import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prueba las consultas propias y las restricciones de las tablas de actividad contra H2
 * (el esquema se genera a partir de las entidades).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SnapshotActividadRepositoryTest {

    private static final Instant T1 = Instant.parse("2026-11-10T03:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-11T03:00:00Z");

    @Autowired
    private RepositorioVinculadoRepository repositorios;
    @Autowired
    private SnapshotActividadRepository snapshots;
    @Autowired
    private ActividadIntegranteRepository actividades;

    private RepositorioVinculado repositorio;

    @BeforeEach
    void preparar() {
        repositorio = repositorios.saveAndFlush(nuevoRepositorio(10L));
    }

    private static RepositorioVinculado nuevoRepositorio(Long equipoId) {
        return RepositorioVinculado.builder().equipoId(equipoId).url("https://github.com/u/r")
                .propietario("u").nombre("r").ramaSeguimiento("main").fechaValidacion(T1).build();
    }

    private SnapshotActividad snapshot(RepositorioVinculado repo, Long hito, Instant fecha, EstadoSnapshot estado) {
        return snapshots.saveAndFlush(SnapshotActividad.builder().repositorio(repo).hitoId(hito)
                .fechaConsulta(fecha).estado(estado).commitsTotales(1).commitsSinAtribuir(0).build());
    }

    @Test
    @DisplayName("Devuelve el snapshot más reciente del repositorio para el sprint pedido")
    void deberiaDevolverElMasReciente_delRepositorioYElSprint() {
        snapshot(repositorio, 7L, T1, EstadoSnapshot.ACTUALIZADO);
        SnapshotActividad reciente = snapshot(repositorio, 7L, T2, EstadoSnapshot.ACTUALIZADO);
        snapshot(repositorio, 8L, T2.plusSeconds(60), EstadoSnapshot.ACTUALIZADO);

        var ultimo = snapshots.findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(
                repositorio.getId(), 7L);

        assertThat(ultimo).isPresent();
        assertThat(ultimo.get().getId()).isEqualTo(reciente.getId());
    }

    @Test
    @DisplayName("Con la misma fecha de consulta gana el snapshot creado después")
    void deberiaDesempatarPorId_cuandoLaFechaEsIgual() {
        snapshot(repositorio, 7L, T1, EstadoSnapshot.ACTUALIZADO);
        SnapshotActividad segundo = snapshot(repositorio, 7L, T1, EstadoSnapshot.ACTUALIZADO);

        var ultimo = snapshots.findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(
                repositorio.getId(), 7L);

        assertThat(ultimo.orElseThrow().getId()).isEqualTo(segundo.getId());
    }

    @Test
    @DisplayName("No mezcla snapshots de otro repositorio ni de otro sprint")
    void deberiaDevolverVacio_cuandoNoHaySnapshotsDeEseRepositorioYSprint() {
        RepositorioVinculado otro = repositorios.saveAndFlush(nuevoRepositorio(11L));
        snapshot(otro, 7L, T1, EstadoSnapshot.ACTUALIZADO);
        snapshot(repositorio, 8L, T1, EstadoSnapshot.ACTUALIZADO);

        assertThat(snapshots.findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(
                repositorio.getId(), 7L)).isEmpty();
    }

    @Test
    @DisplayName("Un equipo tiene un solo repositorio vinculado")
    void deberiaRechazarUnSegundoRepositorio_paraElMismoEquipo() {
        assertThrows(DataIntegrityViolationException.class,
                () -> repositorios.saveAndFlush(nuevoRepositorio(10L)));
    }

    @Test
    @DisplayName("Se encuentra el repositorio vinculado por equipo")
    void deberiaEncontrarElRepositorio_porEquipo() {
        assertThat(repositorios.findByEquipoId(10L)).isPresent();
        assertThat(repositorios.findByEquipoId(99L)).isEmpty();
    }

    @Test
    @DisplayName("Un integrante tiene una sola fila de actividad por snapshot")
    void deberiaRechazarDosFilasDelMismoIntegrante_enUnSnapshot() {
        SnapshotActividad snap = snapshot(repositorio, 7L, T1, EstadoSnapshot.ACTUALIZADO);
        actividades.saveAndFlush(fila(snap, "sub-a", 2));

        assertThrows(DataIntegrityViolationException.class,
                () -> actividades.saveAndFlush(fila(snap, "sub-a", 3)));
    }

    @Test
    @DisplayName("Los commits de un integrante no pueden ser negativos")
    void deberiaRechazarCommitsNegativos() {
        SnapshotActividad snap = snapshot(repositorio, 7L, T1, EstadoSnapshot.ACTUALIZADO);

        assertThrows(DataIntegrityViolationException.class,
                () -> actividades.saveAndFlush(fila(snap, "sub-a", -1)));
    }

    @Test
    @DisplayName("Se listan las filas de actividad de un snapshot, incluida la de 0 commits")
    void deberiaListarLasFilas_deUnSnapshot() {
        SnapshotActividad snap = snapshot(repositorio, 7L, T1, EstadoSnapshot.ACTUALIZADO);
        actividades.saveAndFlush(fila(snap, "sub-a", 2));
        actividades.saveAndFlush(fila(snap, "sub-b", 0));

        assertThat(actividades.findBySnapshotId(snap.getId()))
                .extracting(ActividadIntegrante::getEstudianteSub, ActividadIntegrante::getCommits)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("sub-a", 2),
                        org.assertj.core.groups.Tuple.tuple("sub-b", 0));
    }

    @Test
    @DisplayName("F13: el historial del dashboard se ordena por sprint y, dentro de cada uno, por fecha descendente")
    void deberiaListarElHistorialDeUnEquipo_ordenadoPorSprintYFecha() {
        SnapshotActividad viejo = snapshot(repositorio, 7L, T1, EstadoSnapshot.ACTUALIZADO);
        SnapshotActividad reciente = snapshot(repositorio, 7L, T2, EstadoSnapshot.DESACTUALIZADO);
        SnapshotActividad otroSprint = snapshot(repositorio, 8L, T1, EstadoSnapshot.NO_DISPONIBLE);
        RepositorioVinculado otroEquipo = repositorios.saveAndFlush(nuevoRepositorio(11L));
        snapshot(otroEquipo, 9L, T1, EstadoSnapshot.ACTUALIZADO);

        List<SnapshotActividad> historial = snapshots.findHistorialPorEquipo(10L);

        assertThat(historial).extracting(SnapshotActividad::getId)
                .containsExactly(reciente.getId(), viejo.getId(), otroSprint.getId());
    }

    @Test
    @DisplayName("F13: las filas de actividad de varios snapshots se traen en una sola consulta")
    void deberiaListarLasFilas_deVariosSnapshots() {
        SnapshotActividad primero = snapshot(repositorio, 7L, T1, EstadoSnapshot.ACTUALIZADO);
        SnapshotActividad segundo = snapshot(repositorio, 8L, T1, EstadoSnapshot.ACTUALIZADO);
        actividades.saveAndFlush(fila(primero, "sub-a", 4));
        actividades.saveAndFlush(fila(segundo, "sub-b", 0));

        List<ActividadIntegrante> encontradas =
                actividades.findPorSnapshots(List.of(primero.getId(), segundo.getId()));

        assertThat(encontradas)
                .extracting(ActividadIntegrante::getEstudianteSub, ActividadIntegrante::getCommits)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("sub-a", 4),
                        org.assertj.core.groups.Tuple.tuple("sub-b", 0));
    }

    private static ActividadIntegrante fila(SnapshotActividad snap, String sub, int commits) {
        return ActividadIntegrante.builder().snapshot(snap).estudianteSub(sub).usuarioGithub("u").commits(commits).build();
    }
}
