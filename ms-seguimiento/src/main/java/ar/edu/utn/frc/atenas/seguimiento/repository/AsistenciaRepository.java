package ar.edu.utn.frc.atenas.seguimiento.repository;

import ar.edu.utn.frc.atenas.seguimiento.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findByEventoIdOrderByEstudianteSubAsc(Long eventoId);

    Optional<Asistencia> findByEventoIdAndEstudianteSub(Long eventoId, String estudianteSub);

    /** Filas ya existentes de un grupo de alumnos en un evento (para la carga por equipo). */
    List<Asistencia> findByEventoIdAndEstudianteSubIn(Long eventoId, Collection<String> estudianteSubs);

    /** Asistencia de un alumno con los datos de cada evento, del más reciente al más antiguo. */
    @Query("select a from Asistencia a join fetch a.evento e where a.estudianteSub = :sub order by e.fecha desc")
    List<Asistencia> findDeEstudiante(@Param("sub") String sub);

    /**
     * Asistencia registrada por un equipo en los eventos de su comisión. El {@code join fetch}
     * evita recorrer cada evento por separado al armar el dashboard (S10).
     */
    @Query("select a from Asistencia a join fetch a.evento e "
            + "where a.equipoId = :equipoId and e.comisionLectivaId = :comisionLectivaId "
            + "order by e.fecha desc, a.estudianteSub asc")
    List<Asistencia> findDelEquipoEnComision(@Param("equipoId") Long equipoId,
                                             @Param("comisionLectivaId") Long comisionLectivaId);
}
