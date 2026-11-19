package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a las comisiones habilitadas en cada curso lectivo. Las consultas
 * traen la comisión y el curso en la misma consulta para no caer en N+1 al
 * armar las respuestas.
 */
public interface ComisionLectivaRepository extends JpaRepository<ComisionLectiva, Long> {

    boolean existsByComisionIdAndCursoLectivoId(Long comisionId, Long cursoLectivoId);

    @EntityGraph(attributePaths = {"comision", "cursoLectivo"})
    List<ComisionLectiva> findByCursoLectivoId(Long cursoLectivoId);
}
