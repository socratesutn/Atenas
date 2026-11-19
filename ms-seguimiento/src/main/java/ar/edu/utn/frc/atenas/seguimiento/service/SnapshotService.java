package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.seguimiento.entity.ActividadIntegrante;
import ar.edu.utn.frc.atenas.seguimiento.entity.EstadoSnapshot;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import ar.edu.utn.frc.atenas.seguimiento.repository.ActividadIntegranteRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.SnapshotActividadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda los snapshots de actividad y administra sus estados (RN28).
 *
 * <p>No habla con GitHub ni con otros servicios: esas llamadas se hacen <b>antes</b>, fuera
 * de la transacción (regla de arquitectura del proyecto), y a este servicio solo llega el
 * resultado ya calculado o el motivo de la falla. Los métodos reciben la hora como
 * parámetro para poder probarlos sin depender del reloj.</p>
 *
 * <h3>Transiciones</h3>
 * <ul>
 *   <li>Consulta exitosa: se crea un snapshot {@code ACTUALIZADO}.</li>
 *   <li>GitHub o un servicio interno no responde: el último snapshot pasa a
 *       {@code DESACTUALIZADO} conservando sus datos; si no hay ninguno, se crea uno
 *       {@code NO_DISPONIBLE}.</li>
 *   <li>Respuesta inesperada de GitHub: se crea un snapshot {@code NO_DISPONIBLE}, aunque
 *       hubiera datos previos (RN28: la evidencia queda no disponible).</li>
 *   <li>Si el último snapshot ya es {@code NO_DISPONIBLE} no se agrega otro igual: así una
 *       falla sostenida no llena la tabla con una fila por noche.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final SnapshotActividadRepository snapshots;
    private final ActividadIntegranteRepository actividades;

    /** Por qué falló la consulta de actividad. */
    public enum MotivoFalla {
        /** Timeout, conexión caída, 5xx, token inválido o cuota agotada. */
        NO_RESPONDE,
        /** GitHub respondió, pero con un contenido que no se pudo interpretar. */
        RESPUESTA_INESPERADA
    }

    /**
     * Guarda un snapshot {@code ACTUALIZADO} con una fila por integrante.
     *
     * @throws IllegalStateException si el resultado rompe el invariante de los contadores
     *                               (es un error de programación, no del usuario)
     */
    @Transactional
    public SnapshotActividad guardarActualizado(RepositorioVinculado repositorio, Long hitoId,
                                                CalculadorActividad.Resultado resultado, Instant ahora) {
        int atribuidos = resultado.porIntegrante().stream()
                .mapToInt(CalculadorActividad.CommitsDeIntegrante::commits).sum();
        if (resultado.commitsTotales() != atribuidos + resultado.commitsSinAtribuir()) {
            throw new IllegalStateException("Los contadores del snapshot no cierran: totales="
                    + resultado.commitsTotales() + ", atribuidos=" + atribuidos
                    + ", sin atribuir=" + resultado.commitsSinAtribuir());
        }

        SnapshotActividad snapshot = snapshots.save(SnapshotActividad.builder()
                .repositorio(repositorio)
                .hitoId(hitoId)
                .fechaConsulta(ahora)
                .estado(EstadoSnapshot.ACTUALIZADO)
                .commitsTotales(resultado.commitsTotales())
                .commitsSinAtribuir(resultado.commitsSinAtribuir())
                .build());

        List<ActividadIntegrante> filas = resultado.porIntegrante().stream()
                .map(integrante -> ActividadIntegrante.builder()
                        .snapshot(snapshot)
                        .estudianteSub(integrante.sub())
                        .usuarioGithub(integrante.usuarioGithub())
                        .commits(integrante.commits())
                        .build())
                .toList();
        actividades.saveAll(filas);

        log.info("Snapshot ACTUALIZADO guardado para el repositorio {} y el sprint {}: {} commits",
                repositorio.getId(), hitoId, resultado.commitsTotales());
        return snapshot;
    }

    /**
     * Registra una falla de consulta aplicando las transiciones de RN28. Nunca lanza
     * por la falla en sí: que GitHub no responda no debe bloquear nada.
     */
    @Transactional
    public SnapshotActividad registrarFalla(RepositorioVinculado repositorio, Long hitoId,
                                            MotivoFalla motivo, Instant ahora) {
        Optional<SnapshotActividad> ultimo = snapshots
                .findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(repositorio.getId(), hitoId);

        // Ya no hay datos que conservar ni nada nuevo que informar.
        if (ultimo.isPresent() && ultimo.get().getEstado() == EstadoSnapshot.NO_DISPONIBLE) {
            return ultimo.get();
        }

        // No respondió: se conservan los datos del último snapshot, marcados como desactualizados.
        // Se deja la fecha de consulta original para saber de cuándo son esos datos.
        if (motivo == MotivoFalla.NO_RESPONDE && ultimo.isPresent()) {
            SnapshotActividad anterior = ultimo.get();
            if (anterior.getEstado() != EstadoSnapshot.DESACTUALIZADO) {
                anterior.setEstado(EstadoSnapshot.DESACTUALIZADO);
                snapshots.save(anterior);
            }
            log.warn("Snapshot {} marcado DESACTUALIZADO (repositorio {}, sprint {})",
                    anterior.getId(), repositorio.getId(), hitoId);
            return anterior;
        }

        // Nunca hubo datos, o la respuesta fue inesperada: la evidencia queda no disponible.
        SnapshotActividad noDisponible = snapshots.save(SnapshotActividad.builder()
                .repositorio(repositorio)
                .hitoId(hitoId)
                .fechaConsulta(ahora)
                .estado(EstadoSnapshot.NO_DISPONIBLE)
                .build());
        log.warn("Snapshot NO_DISPONIBLE creado para el repositorio {} y el sprint {} ({})",
                repositorio.getId(), hitoId, motivo);
        return noDisponible;
    }

    /**
     * Devuelve el último snapshot del sprint solo si ya se calculó después de la fecha límite.
     * Ese sprint está cerrado: los commits posteriores no cuentan (RN27) y volver a consultar
     * GitHub no cambiaría el resultado, por lo que la actualización lo omite.
     */
    public Optional<SnapshotActividad> ultimoActualizadoDespuesDelLimite(RepositorioVinculado repositorio,
                                                                        Long hitoId, Instant limite) {
        Optional<SnapshotActividad> ultimo = snapshots
                .findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(repositorio.getId(), hitoId);
        return ultimo.filter(snapshot -> snapshot.getEstado() == EstadoSnapshot.ACTUALIZADO
                && snapshot.getFechaConsulta().isAfter(limite));
    }

    /**
     * Degrada el último snapshot de cada sprint conocido del repositorio cuando falla I01 o I06.
     * Si no se pudo obtener la lista de sprints, al menos se conserva la evidencia de los que
     * ya se habían calculado. No crea snapshots nuevos porque no se conoce el id del hito.
     */
    @Transactional
    public List<SnapshotActividad> registrarFallaDeServicio(RepositorioVinculado repositorio, MotivoFalla motivo,
                                                            Instant ahora) {
        Map<Long, SnapshotActividad> ultimosPorSprint = new LinkedHashMap<>();
        for (SnapshotActividad snapshot : snapshots
                .findAllByRepositorioIdOrderByFechaConsultaDescIdDesc(repositorio.getId())) {
            ultimosPorSprint.putIfAbsent(snapshot.getHitoId(), snapshot);
        }
        return ultimosPorSprint.values().stream()
                .map(snapshot -> registrarFalla(repositorio, snapshot.getHitoId(), motivo, ahora))
                .toList();
    }
}
