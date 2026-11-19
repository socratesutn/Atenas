package ar.edu.utn.frc.atenas.seguimiento.repository;

import ar.edu.utn.frc.atenas.seguimiento.entity.SnapshotActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SnapshotActividadRepository extends JpaRepository<SnapshotActividad, Long> {

    /**
     * El snapshot más reciente de un repositorio para un sprint. Si dos tienen la
     * misma fecha de consulta, gana el de mayor id (el creado después).
     */
    Optional<SnapshotActividad> findFirstByRepositorioIdAndHitoIdOrderByFechaConsultaDescIdDesc(
            Long repositorioId, Long hitoId);

    /** Todos los snapshots del repositorio, de más reciente a más viejo, para degradar por sprint. */
    List<SnapshotActividad> findAllByRepositorioIdOrderByFechaConsultaDescIdDesc(Long repositorioId);

    /**
     * Historial completo de un equipo ordenado por sprint. El dashboard agrupa por
     * {@code hitoId} y se queda con el primero de cada grupo: es el último snapshot conocido.
     */
    @Query("select s from SnapshotActividad s where s.repositorio.equipoId = :equipoId "
            + "order by s.hitoId asc, s.fechaConsulta desc, s.id desc")
    List<SnapshotActividad> findHistorialPorEquipo(@Param("equipoId") Long equipoId);
}
