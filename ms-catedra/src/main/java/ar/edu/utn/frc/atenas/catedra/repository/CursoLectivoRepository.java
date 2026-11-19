package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a cursos lectivos. La comprobación de año duplicado se hace antes de
 * guardar y la restricción única de la base sigue siendo la última defensa.
 */
public interface CursoLectivoRepository extends JpaRepository<CursoLectivo, Long> {

    boolean existsByAnio(Integer anio);
}
