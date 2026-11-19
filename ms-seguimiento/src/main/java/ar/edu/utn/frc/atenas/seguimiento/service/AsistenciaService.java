package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EstudianteDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;
import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.repository.AsistenciaRepository;
import ar.edu.utn.frc.atenas.seguimiento.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registro y consulta de asistencia (F15, F16; S03 a S06; RN13 y RN15).
 *
 * <p>Orden de las comprobaciones en los registros: el evento existe (404), quien pide es
 * docente de su comisión (403), el evento no es futuro (409) y recién entonces se consulta
 * por el alumno o el equipo. Las llamadas a ms-catedra se hacen <b>antes</b> de abrir la
 * transacción; el método usa {@link TransactionTemplate} solo para persistir.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final EventoRepository eventos;
    private final AsistenciaRepository asistencias;
    private final CatedraClient catedraClient;
    private final AccesoComision acceso;
    private final UsuarioActual usuarioActual;
    private final TransactionTemplate transaccion;

    /**
     * S03: registra o corrige la asistencia de un alumno. Si ya existía una fila para ese
     * alumno y evento, la actualiza en lugar de duplicarla.
     */
    public Asistencia registrarDeAlumno(Long eventoId, String estudianteSub, boolean presente) {
        Evento evento = buscarEvento(eventoId);
        acceso.exigirDocenteAsignado(evento.getComisionLectivaId(), "RN-13");
        exigirEventoNoFuturo(evento);

        EstudianteDto alumno = catedraClient.obtenerEstudiantePorSub(estudianteSub);
        if (!evento.getComisionLectivaId().equals(alumno.comisionLectivaId())) {
            throw new AccesoDenegadoException(
                    "El alumno no pertenece a la comisión del evento", "RN-13");
        }

        String registradaPor = usuarioActual.sub();
        Instant ahora = Instant.now();
        Asistencia guardada = transaccion.execute(estado -> {
            Asistencia fila = asistencias.findByEventoIdAndEstudianteSub(eventoId, estudianteSub)
                    .orElseGet(() -> nueva(evento, estudianteSub));
            completar(fila, alumno.equipoId(), presente, registradaPor, ahora);
            return asistencias.save(fila);
        });
        log.info("Asistencia del alumno {} en el evento {}: presente={}", estudianteSub, eventoId, presente);
        return guardada;
    }

    /**
     * S04: registra o corrige la asistencia de todos los integrantes vigentes de un equipo.
     * Se guarda una fila por integrante; las que ya existían se actualizan.
     */
    public List<Asistencia> registrarDeEquipo(Long eventoId, Long equipoId, boolean presente) {
        Evento evento = buscarEvento(eventoId);
        acceso.exigirDocenteAsignado(evento.getComisionLectivaId(), "RN-13");
        exigirEventoNoFuturo(evento);

        EquipoDto equipo = catedraClient.obtenerEquipo(equipoId);
        if (!evento.getComisionLectivaId().equals(equipo.comisionLectivaId())) {
            throw new AccesoDenegadoException(
                    "El equipo no pertenece a la comisión del evento", "RN-13");
        }
        List<String> subs = equipo.integrantes() == null ? List.<String>of() : equipo.integrantes().stream()
                .map(EquipoDto.IntegranteDto::sub)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (subs.isEmpty()) {
            throw new ConflictoException("El equipo no tiene integrantes vigentes", "RN-13");
        }

        String registradaPor = usuarioActual.sub();
        Instant ahora = Instant.now();
        List<Asistencia> guardadas = transaccion.execute(estado -> {
            Map<String, Asistencia> existentes = new HashMap<>();
            asistencias.findByEventoIdAndEstudianteSubIn(eventoId, subs)
                    .forEach(fila -> existentes.put(fila.getEstudianteSub(), fila));

            List<Asistencia> filas = new ArrayList<>();
            for (String sub : subs) {
                Asistencia fila = existentes.getOrDefault(sub, nueva(evento, sub));
                completar(fila, equipoId, presente, registradaPor, ahora);
                filas.add(fila);
            }
            return asistencias.saveAll(filas);
        });
        log.info("Asistencia del equipo {} en el evento {}: {} integrantes, presente={}",
                equipoId, eventoId, subs.size(), presente);
        return guardadas.stream().sorted(Comparator.comparing(Asistencia::getEstudianteSub)).toList();
    }

    /** S05: el jefe o un docente de la comisión consultan la asistencia de un evento. */
    public List<Asistencia> listarDeEvento(Long eventoId) {
        Evento evento = buscarEvento(eventoId);
        acceso.exigirJefeODocente(evento.getComisionLectivaId());
        return asistencias.findByEventoIdOrderByEstudianteSubAsc(eventoId);
    }

    /**
     * S06 (RN15): un alumno ve solo su propia asistencia. La búsqueda se hace por el
     * {@code sub} del token, nunca por un dato que mande el cliente.
     */
    public List<Asistencia> miAsistencia() {
        String sub = usuarioActual.sub();
        if (sub == null) {
            throw new AccesoDenegadoException("No se pudo identificar al usuario", "RN-15");
        }
        return asistencias.findDeEstudiante(sub);
    }

    private Evento buscarEvento(Long eventoId) {
        return eventos.findById(eventoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el evento " + eventoId));
    }

    /** RN13: no se registra asistencia de un evento que todavía no ocurrió. */
    private void exigirEventoNoFuturo(Evento evento) {
        if (evento.getFecha().isAfter(Instant.now())) {
            throw new ConflictoException(
                    "No se puede registrar asistencia de un evento futuro", "RN-13");
        }
    }

    private Asistencia nueva(Evento evento, String estudianteSub) {
        return Asistencia.builder().evento(evento).estudianteSub(estudianteSub).build();
    }

    private void completar(Asistencia fila, Long equipoId, boolean presente, String registradaPor, Instant ahora) {
        fila.setEquipoId(equipoId);
        fila.setPresente(presente);
        fila.setRegistradaPorSub(registradaPor);
        fila.setFechaRegistro(ahora);
    }
}
