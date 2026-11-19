package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.AsignacionDocente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a las asignaciones docente-comisión. Las consultas que usa C06 traen
 * la comisión lectiva para poder filtrar sin consultas adicionales.
 */
public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocente, Long> {

    boolean existsByDocenteIdAndComisionLectivaId(Long docenteId, Long comisionLectivaId);

    @EntityGraph(attributePaths = {"comisionLectiva"})
    List<AsignacionDocente> findByDocenteIdAndComisionLectivaCursoLectivoId(
            Long docenteId, Long cursoLectivoId);
}
