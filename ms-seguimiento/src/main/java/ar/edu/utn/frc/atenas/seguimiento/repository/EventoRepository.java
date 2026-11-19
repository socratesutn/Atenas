package ar.edu.utn.frc.atenas.seguimiento.repository;

import ar.edu.utn.frc.atenas.seguimiento.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    /** Calendario de una comisión, del evento más antiguo al más reciente. */
    List<Evento> findByComisionLectivaIdOrderByFechaAsc(Long comisionLectivaId);
}
