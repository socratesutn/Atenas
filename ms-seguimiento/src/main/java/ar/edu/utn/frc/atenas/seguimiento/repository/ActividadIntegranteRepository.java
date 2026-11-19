package ar.edu.utn.frc.atenas.seguimiento.repository;

import ar.edu.utn.frc.atenas.seguimiento.entity.ActividadIntegrante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ActividadIntegranteRepository extends JpaRepository<ActividadIntegrante, Long> {

    List<ActividadIntegrante> findBySnapshotId(Long snapshotId);

    /** Filas de varios snapshots en una sola consulta, para calcular las marcas del dashboard. */
    @Query("select a from ActividadIntegrante a where a.snapshot.id in :ids")
    List<ActividadIntegrante> findPorSnapshots(@Param("ids") Collection<Long> ids);
}
