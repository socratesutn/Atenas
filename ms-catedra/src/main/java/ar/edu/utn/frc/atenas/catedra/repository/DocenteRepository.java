package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a docentes. El mail y el sub son únicos; el sub se completa después
 * de crear la cuenta en Keycloak.
 */
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    boolean existsByMail(String mail);

    Optional<Docente> findBySub(String sub);
}
