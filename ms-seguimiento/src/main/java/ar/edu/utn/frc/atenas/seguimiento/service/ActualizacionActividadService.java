package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.ErrorDominioException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.client.CommitGitHub;
import ar.edu.utn.frc.atenas.seguimiento.client.DocumentacionClient;
import ar.edu.utn.frc.atenas.seguimiento.client.GitHubClient;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.HitoDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import ar.edu.utn.frc.atenas.seguimiento.exception.RespuestaInesperadaException;
import ar.edu.utn.frc.atenas.seguimiento.repository.RepositorioVinculadoRepository;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadoActualizacion.DetalleSprint;
import ar.edu.utn.frc.atenas.seguimiento.service.SnapshotService.MotivoFalla;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta la actualización de actividad de GitHub (F12, S08, RN26–RN28, RN31).
 *
 * <p>Responsabilidad: pedir el equipo (I01) y los sprints de su curso (I06), pedir los commits
 * de cada sprint a {@link GitHubClient}, delegar el conteo a {@link CalculadorActividad} y
 * guardar el resultado con {@link SnapshotService}. No conoce HTTP ni formatea fechas.</p>
 *
 * <p>Las llamadas externas ocurren antes de abrir cualquier transacción: este service no es
 * transaccional y {@link SnapshotService} abre la suya solo para persistir. Las fallas se
 * convierten en estados de snapshot (RN28) y nunca se lanzan hacia afuera, salvo la ausencia
 * de repositorio vinculado (404), la negación de acceso de S08 (403) y la imposibilidad de
 * verificar la comisión del solicitante (503). En la tarea nocturna una falla por equipo se
 * aísla para que no corte el barrido de los demás.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActualizacionActividadService {

    private final CatedraClient catedraClient;
    private final DocumentacionClient documentacionClient;
    private final GitHubClient gitHubClient;
    private final RepositorioVinculadoRepository repositorios;
    private final CalculadorActividad calculador;
    private final SnapshotService snapshots;
    private final AccesoComision accesoComision;

    /**
     * S08: actualiza los sprints de un equipo a pedido de un jefe o de un docente de su comisión.
     * La autorización de dominio se hace con el equipo devuelto por I01 (I04 para docentes).
     */
    public ResultadoActualizacion actualizarEquipo(Long equipoId) {
        return actualizarEquipo(equipoId, Instant.now());
    }

    /** Sobrecarga con reloj inyectable para los tests unitarios. */
    ResultadoActualizacion actualizarEquipo(Long equipoId, Instant ahora) {
        RepositorioVinculado repositorio = repositorios.findByEquipoId(equipoId)
                .orElseThrow(() -> new NoEncontradoException(
                        "El equipo " + equipoId + " no tiene un repositorio vinculado", "RN-31"));

        EquipoDto equipo;
        try {
            equipo = catedraClient.obtenerEquipo(equipoId);
        } catch (ErrorDominioException falla) {
            /*
             * S08 necesita la comisión antes de escribir. Un docente no puede degradar
             * snapshots sin comprobar RN01: se propaga el 404/503 de I01 para no modificar
             * evidencia de un equipo ajeno. El jefe accede a cualquier comisión, así que
             * para él sí se aplica la degradación prevista en RN28.
             */
            if (falla instanceof NoEncontradoException || !accesoComision.esJefe()) {
                throw falla;
            }
            log.warn("No se pudo obtener el equipo {} para verificar el acceso; se degradan sus snapshots", equipoId);
            return fallaDeServicio(repositorio, ahora, motivoDe(falla));
        }
        if (equipo == null || equipo.comisionLectivaId() == null) {
            if (!accesoComision.esJefe()) {
                throw new ServicioNoDisponibleException(
                        "ms-catedra devolvió un equipo incompleto", "RN-01");
            }
            return fallaDeServicio(repositorio, ahora, MotivoFalla.RESPUESTA_INESPERADA);
        }

        accesoComision.exigirJefeODocente(equipo.comisionLectivaId());
        return actualizarConEquipo(repositorio, equipo, ahora);
    }

    /** Barrido nocturno: recorre todos los repositorios vinculados y aísla la falla de cada equipo. */
    public List<ResultadoActualizacion> actualizarTodos() {
        return actualizarTodos(Instant.now());
    }

    /** Sobrecarga con reloj inyectable para los tests unitarios. */
    List<ResultadoActualizacion> actualizarTodos(Instant ahora) {
        List<ResultadoActualizacion> resultados = new ArrayList<>();
        for (RepositorioVinculado repositorio : repositorios.findAll()) {
            try {
                resultados.add(actualizarRepositorio(repositorio, ahora));
            } catch (ErrorDominioException | IllegalStateException falla) {
                log.warn("Falla al actualizar la actividad del equipo {}: {}",
                        repositorio.getEquipoId(), falla.getMessage());
                resultados.add(fallaDeServicio(repositorio, ahora, motivoDe(falla)));
            }
        }
        return resultados;
    }

    /** Actualiza un repositorio del barrido, sin la comprobación de acceso de S08. */
    private ResultadoActualizacion actualizarRepositorio(RepositorioVinculado repositorio, Instant ahora) {
        EquipoDto equipo;
        try {
            equipo = catedraClient.obtenerEquipo(repositorio.getEquipoId());
        } catch (ErrorDominioException falla) {
            log.warn("No se pudo obtener el equipo {} del barrido nocturno", repositorio.getEquipoId());
            return fallaDeServicio(repositorio, ahora, motivoDe(falla));
        }
        if (equipo == null || equipo.cursoLectivoId() == null) {
            return fallaDeServicio(repositorio, ahora, MotivoFalla.RESPUESTA_INESPERADA);
        }
        return actualizarConEquipo(repositorio, equipo, ahora);
    }

    private ResultadoActualizacion actualizarConEquipo(RepositorioVinculado repositorio, EquipoDto equipo,
                                                       Instant ahora) {
        List<HitoDto> hitos;
        try {
            hitos = documentacionClient.listarHitosSprint(equipo.cursoLectivoId());
        } catch (ErrorDominioException falla) {
            log.warn("No se pudieron obtener los sprints del curso {}", equipo.cursoLectivoId());
            return fallaDeServicio(repositorio, ahora, motivoDe(falla));
        }
        if (hitos == null) {
            return fallaDeServicio(repositorio, ahora, MotivoFalla.RESPUESTA_INESPERADA);
        }

        List<CalculadorActividad.IntegranteEquipo> integrantes = equipo.integrantes() == null
                ? List.of()
                : equipo.integrantes().stream()
                        .filter(integrante -> integrante != null && integrante.sub() != null)
                        .map(integrante -> new CalculadorActividad.IntegranteEquipo(
                                integrante.sub(), integrante.usuarioGithub()))
                        .toList();

        List<DetalleSprint> detalles = new ArrayList<>();
        for (HitoDto hito : hitos) {
            detalles.add(actualizarSprint(repositorio, hito, integrantes, ahora));
        }
        return new ResultadoActualizacion(repositorio.getEquipoId(), ahora, detalles);
    }

    private DetalleSprint actualizarSprint(RepositorioVinculado repositorio, HitoDto hito,
                                           List<CalculadorActividad.IntegranteEquipo> integrantes, Instant ahora) {
        if (hito == null || hito.id() == null || hito.fechaInicio() == null || hito.fechaLimite() == null) {
            SnapshotActividad noDisponible = snapshots.registrarFalla(
                    repositorio, hito == null ? null : hito.id(), MotivoFalla.RESPUESTA_INESPERADA, ahora);
            return new DetalleSprint(noDisponible.getHitoId(), null, noDisponible, false);
        }

        Instant inicio = hito.fechaInicio().toInstant();
        Instant limite = hito.fechaLimite().toInstant();

        // Un sprint cerrado ya no puede cambiar: se informa el snapshot que se calculó después del límite.
        Optional<SnapshotActividad> cerrado = snapshots.ultimoActualizadoDespuesDelLimite(repositorio, hito.id(), limite);
        if (cerrado.isPresent()) {
            log.debug("Se omite el sprint {} porque ya se actualizó después de su fecha límite", hito.id());
            return new DetalleSprint(hito.id(), hito.nombre(), cerrado.get(), true);
        }

        try {
            List<CommitGitHub> commits = gitHubClient.listarCommits(
                    repositorio.getPropietario(), repositorio.getNombre(), repositorio.getRamaSeguimiento(),
                    inicio, limite);
            CalculadorActividad.Resultado resultado = calculador.calcular(commits, inicio, limite, integrantes);
            SnapshotActividad actualizado = snapshots.guardarActualizado(repositorio, hito.id(), resultado, ahora);
            return new DetalleSprint(hito.id(), hito.nombre(), actualizado, false);
        } catch (RespuestaInesperadaException | IllegalStateException inesperada) {
            SnapshotActividad noDisponible = snapshots.registrarFalla(
                    repositorio, hito.id(), MotivoFalla.RESPUESTA_INESPERADA, ahora);
            return new DetalleSprint(hito.id(), hito.nombre(), noDisponible, false);
        } catch (ServicioNoDisponibleException | NoEncontradoException | NoProcesableException noResponde) {
            SnapshotActividad desactualizado = snapshots.registrarFalla(
                    repositorio, hito.id(), MotivoFalla.NO_RESPONDE, ahora);
            return new DetalleSprint(hito.id(), hito.nombre(), desactualizado, false);
        }
    }

    /** Degrada los snapshots que se conocen del repositorio cuando falla un servicio interno. */
    private ResultadoActualizacion fallaDeServicio(RepositorioVinculado repositorio, Instant ahora, MotivoFalla motivo) {
        List<DetalleSprint> detalles = snapshots.registrarFallaDeServicio(repositorio, motivo, ahora).stream()
                .map(snapshot -> new DetalleSprint(snapshot.getHitoId(), null, snapshot, false))
                .toList();
        return new ResultadoActualizacion(repositorio.getEquipoId(), ahora, detalles);
    }

    /** Respuesta inesperada se distingue de "no responde" antes de degradar (RN28). */
    private MotivoFalla motivoDe(RuntimeException falla) {
        return falla instanceof RespuestaInesperadaException
                ? MotivoFalla.RESPUESTA_INESPERADA
                : MotivoFalla.NO_RESPONDE;
    }
}
