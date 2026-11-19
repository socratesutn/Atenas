package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a comisiones permanentes. El nombre se valida contra la base para
 * devolver un conflicto claro antes de que actúe la restricción única.
 */
public interface ComisionRepository extends JpaRepository<Comision, Long> {

    boolean existsByNombre(String nombre);
}
