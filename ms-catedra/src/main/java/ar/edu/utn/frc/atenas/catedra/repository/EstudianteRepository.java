package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.Estudiante;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a estudiantes. Las consultas por sub traen la comisión lectiva, la
 * comisión permanente y el curso para armar el perfil y el contrato I03 sin
 * consultas adicionales.
 */
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    boolean existsByLegajo(String legajo);

    boolean existsByUsuarioGithub(String usuarioGithub);

    @EntityGraph(attributePaths = {
            "comisionLectiva",
            "comisionLectiva.comision",
            "comisionLectiva.cursoLectivo"
    })
    Optional<Estudiante> findBySub(String sub);

    List<Estudiante> findByComisionLectivaId(Long comisionLectivaId);
}
