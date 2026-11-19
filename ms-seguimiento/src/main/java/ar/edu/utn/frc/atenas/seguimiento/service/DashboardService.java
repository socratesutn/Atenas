package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.client.DocumentacionClient;
import ar.edu.utn.frc.atenas.seguimiento.config.ActividadProperties;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EntregaEquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.ActividadIntegrante;
import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import ar.edu.utn.frc.atenas.seguimiento.repository.ActividadIntegranteRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.AsistenciaRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.EventoRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.RepositorioVinculadoRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.SnapshotActividadRepository;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.CommitsDeIntegrante;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.Marcas;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.ActividadEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.AsistenciaEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.ContribucionEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.DetalleEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.EntregasEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.ResumenEquipos;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadosDashboard.SnapshotEquipo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compone los dashboards S09 y S10 (F13, RN27, RN28, RN33).
 *
 * <p>Es el orquestador de la historia: primero solicita el equipo y valida el permiso con
 * ms-catedra (I01 e I04), después lee asistencia y snapshots de las tablas propias y por
 * último pide las entregas (I07). Ese orden respeta el flujo de docs/09 y permite degradar
 * solo la sección de entregas si ms-documentacion no responde.</p>
 *
 * <p>No abre transacciones: todas las operaciones son consultas y cada repositorio maneja
 * la suya. Las marcas de actividad se calculan acá, al consultar, y nunca se persisten.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CatedraClient catedraClient;
    private final DocumentacionClient documentacionClient;
    private final AccesoComision acceso;
    private final UsuarioActual usuarioActual;
    private final EventoRepository eventos;
    private final AsistenciaRepository asistencias;
    private final RepositorioVinculadoRepository repositorios;
    private final SnapshotActividadRepository snapshots;
    private final ActividadIntegranteRepository actividades;
    private final CalculadorActividad calculador;
    private final ActividadProperties propiedadesActividad;

    /** S10: dashboard de un equipo para el jefe o un docente de su comisión. */
    public DetalleEquipo dashboardDeEquipo(Long equipoId) {
        EquipoDto equipo = verificarEquipo(catedraClient.obtenerEquipo(equipoId));
        acceso.exigirJefeODocente(equipo.comisionLectivaId());

        AsistenciaEquipo asistencia = armarAsistencia(equipoId, equipo.comisionLectivaId());
        ActividadEquipo actividad = armarActividad(equipoId);
        EntregasEquipo entregas = armarEntregas(equipo.cursoLectivoId(), List.of(equipoId));
        return new DetalleEquipo(equipo, asistencia, entregas, actividad);
    }

    /** S09: resumen de los equipos de una comisión, o de un curso si lo pide el jefe. */
    public ResumenEquipos dashboardDeEquipos(Long comisionLectivaId, Long cursoLectivoId) {
        exigirUnSoloFiltro(comisionLectivaId, cursoLectivoId);

        List<EquipoDto> equipos;
        if (cursoLectivoId != null) {
            exigirJefe();
            equipos = catedraClient.listarEquiposPorCurso(cursoLectivoId);
        } else {
            // I04 se llama antes que I02: si el docente no es de la comisión, no hace falta listar.
            acceso.exigirJefeODocente(comisionLectivaId);
            equipos = catedraClient.listarEquiposPorComision(comisionLectivaId);
        }
        if (equipos == null) {
            throw new ServicioNoDisponibleException("ms-catedra respondió sin contenido");
        }

        List<EquipoDto> verificados = new ArrayList<>();
        for (EquipoDto equipo : equipos) {
            verificados.add(verificarEquipo(equipo));
        }

        Long curso = cursoLectivoId;
        if (curso == null && !verificados.isEmpty()) {
            curso = verificados.stream().map(EquipoDto::cursoLectivoId).filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new ServicioNoDisponibleException(
                            "ms-catedra devolvió equipos sin curso lectivo"));
        }

        EntregasEquipo entregas = curso == null || verificados.isEmpty()
                ? new EntregasEquipo(true, List.of())
                : armarEntregas(curso, verificados.stream().map(EquipoDto::id).toList());
        Map<Long, List<EntregaEquipoDto>> entregasPorEquipo = entregas.entregas().stream()
                .filter(entrega -> entrega != null && entrega.equipoId() != null)
                .collect(Collectors.groupingBy(EntregaEquipoDto::equipoId));

        List<DetalleEquipo> detalles = verificados.stream()
                .map(equipo -> new DetalleEquipo(
                        equipo,
                        armarAsistencia(equipo.id(), equipo.comisionLectivaId()),
                        new EntregasEquipo(entregas.disponible(),
                                entregasPorEquipo.getOrDefault(equipo.id(), List.of())),
                        armarActividad(equipo.id())))
                .toList();
        return new ResumenEquipos(comisionLectivaId, cursoLectivoId, entregas, detalles);
    }

    /** RN01: el filtro por curso es una consulta global reservada al jefe. */
    private void exigirJefe() {
        if (!usuarioActual.tieneRol(Roles.JEFE_CATEDRA)) {
            throw new AccesoDenegadoException(
                    "Solo el jefe puede consultar el dashboard por curso lectivo", "RN-01");
        }
    }

    private void exigirUnSoloFiltro(Long comisionLectivaId, Long cursoLectivoId) {
        if (comisionLectivaId == null && cursoLectivoId == null) {
            throw new SolicitudInvalidaException("Debe indicar comisionLectivaId o cursoLectivoId");
        }
        if (comisionLectivaId != null && cursoLectivoId != null) {
            throw new SolicitudInvalidaException("Debe indicar solo uno de los dos filtros");
        }
    }

    private EquipoDto verificarEquipo(EquipoDto equipo) {
        if (equipo == null || equipo.id() == null || equipo.comisionLectivaId() == null) {
            throw new ServicioNoDisponibleException("ms-catedra devolvió un equipo incompleto");
        }
        return equipo;
    }

    /** Asistencia del equipo: total de eventos de la comisión y filas efectivamente cargadas. */
    private AsistenciaEquipo armarAsistencia(Long equipoId, Long comisionLectivaId) {
        List<Evento> calendario = eventos.findByComisionLectivaIdOrderByFechaAsc(comisionLectivaId);
        List<Asistencia> registros = asistencias.findDelEquipoEnComision(equipoId, comisionLectivaId);
        int presentes = (int) registros.stream().filter(Asistencia::isPresente).count();
        return new AsistenciaEquipo(calendario.size(), registros.size(), presentes, registros);
    }

    /**
     * Último snapshot por sprint. Si el equipo no tiene repositorio, la sección queda marcada
     * como no disponible; si tiene repositorio pero nunca se calculó, simplemente no hay filas.
     */
    private ActividadEquipo armarActividad(Long equipoId) {
        Optional<RepositorioVinculado> repositorio = repositorios.findByEquipoId(equipoId);
        if (repositorio.isEmpty()) {
            return new ActividadEquipo(false, List.of());
        }

        Map<Long, SnapshotActividad> ultimosPorSprint = new LinkedHashMap<>();
        for (SnapshotActividad snapshot : snapshots.findHistorialPorEquipo(equipoId)) {
            ultimosPorSprint.putIfAbsent(snapshot.getHitoId(), snapshot);
        }
        if (ultimosPorSprint.isEmpty()) {
            return new ActividadEquipo(true, List.of());
        }

        List<Long> ids = ultimosPorSprint.values().stream().map(SnapshotActividad::getId).toList();
        Map<Long, List<ActividadIntegrante>> filasPorSnapshot = actividades.findPorSnapshots(ids).stream()
                .collect(Collectors.groupingBy(fila -> fila.getSnapshot().getId()));

        List<SnapshotEquipo> resumen = ultimosPorSprint.values().stream()
                .map(snapshot -> aSnapshotEquipo(snapshot, filasPorSnapshot))
                .toList();
        return new ActividadEquipo(true, resumen);
    }

    /**
     * RN27: las marcas se recalculan con los contadores guardados. Un snapshot NO_DISPONIBLE
     * no tiene evidencia (contadores en null), por lo que no se le asignan marcas.
     */
    private SnapshotEquipo aSnapshotEquipo(SnapshotActividad snapshot,
                                           Map<Long, List<ActividadIntegrante>> filasPorSnapshot) {
        List<CommitsDeIntegrante> contribuciones = filasPorSnapshot
                .getOrDefault(snapshot.getId(), List.of()).stream()
                .map(fila -> new CommitsDeIntegrante(
                        fila.getEstudianteSub(), fila.getUsuarioGithub(), fila.getCommits()))
                .toList();

        Integer totales = snapshot.getCommitsTotales();
        Marcas marcas = totales == null
                ? new Marcas(false, Set.of())
                : calculador.calcularMarcas(totales, contribuciones,
                        propiedadesActividad.getUmbralBajaContribucion());

        List<ContribucionEquipo> integrantes = contribuciones.stream()
                .map(contribucion -> new ContribucionEquipo(
                        contribucion.sub(), contribucion.usuarioGithub(), contribucion.commits(),
                        marcas.subsConBajaContribucion().contains(contribucion.sub())))
                .toList();
        return new SnapshotEquipo(snapshot.getHitoId(), snapshot.getEstado(), snapshot.getFechaConsulta(),
                totales, snapshot.getCommitsSinAtribuir(), marcas.sinActividad(), integrantes);
    }

    /** RN33: una falla de ms-documentacion solo deja sin entregas; el resto del dashboard sigue. */
    private EntregasEquipo armarEntregas(Long cursoLectivoId, Collection<Long> equipoIds) {
        if (cursoLectivoId == null) {
            throw new ServicioNoDisponibleException("ms-catedra devolvió un equipo sin curso lectivo");
        }
        try {
            List<EntregaEquipoDto> entregas = documentacionClient.listarEntregas(cursoLectivoId, List.copyOf(equipoIds));
            if (entregas == null) {
                throw new ServicioNoDisponibleException("ms-documentacion respondió sin contenido");
            }
            return new EntregasEquipo(true, entregas);
        } catch (ServicioNoDisponibleException falla) {
            log.warn("RN-33: ms-documentacion no respondió; se arma el dashboard sin entregas");
            return new EntregasEquipo(false, List.of());
        }
    }
}
