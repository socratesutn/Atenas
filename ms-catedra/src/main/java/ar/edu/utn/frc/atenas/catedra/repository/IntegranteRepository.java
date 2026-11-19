package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.Integrante;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a integrantes vigentes. La fecha de baja nula identifica una
 * participación actual; el índice único parcial de PostgreSQL refuerza RN07.
 */
public interface IntegranteRepository extends JpaRepository<Integrante, Long> {

    boolean existsByEstudianteIdAndFechaBajaIsNull(Long estudianteId);

    long countByEquipoIdAndFechaBajaIsNull(Long equipoId);

    @EntityGraph(attributePaths = {"estudiante"})
    List<Integrante> findByEquipoIdAndFechaBajaIsNull(Long equipoId);

    @EntityGraph(attributePaths = {"estudiante"})
    Optional<Integrante> findByEquipoIdAndEstudianteIdAndFechaBajaIsNull(
            Long equipoId, Long estudianteId);

    @EntityGraph(attributePaths = {"estudiante"})
    List<Integrante> findByEquipoIdAndFechaBajaIsNullOrderByFechaIngresoAscIdAsc(Long equipoId);

    @EntityGraph(attributePaths = {"estudiante", "equipo"})
    List<Integrante> findByEquipoIdInAndFechaBajaIsNull(Collection<Long> equipoIds);

    @EntityGraph(attributePaths = {"equipo"})
    Optional<Integrante> findByEstudianteIdAndFechaBajaIsNull(Long estudianteId);
}
