package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import ar.edu.utn.frc.atenas.seguimiento.entity.TipoEvento;
import ar.edu.utn.frc.atenas.seguimiento.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Calendario de una comisión lectiva (F14, S01 y S02). */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventos;
    private final AccesoComision acceso;

    /**
     * RN12: solo un docente asignado a la comisión crea eventos. La consulta a ms-catedra
     * (I04) se hace antes de guardar; si falla, no se crea nada.
     */
    public Evento crear(Long comisionLectivaId, TipoEvento tipo, Instant fecha, String descripcion) {
        acceso.exigirDocenteAsignado(comisionLectivaId, "RN-12");

        Evento guardado = eventos.save(Evento.builder()
                .comisionLectivaId(comisionLectivaId)
                .tipo(tipo)
                .fecha(fecha)
                .descripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim())
                .build());
        log.info("Evento {} ({}) creado en la comisión {}", guardado.getId(), tipo, comisionLectivaId);
        return guardado;
    }

    /** RN01: el jefe, un docente de la comisión o un alumno de la comisión consultan el calendario. */
    public List<Evento> listar(Long comisionLectivaId) {
        acceso.exigirJefeDocenteOAlumno(comisionLectivaId);
        return eventos.findByComisionLectivaIdOrderByFechaAsc(comisionLectivaId);
    }
}
