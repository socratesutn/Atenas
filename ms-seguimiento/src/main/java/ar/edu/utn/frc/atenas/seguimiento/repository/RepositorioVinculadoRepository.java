package ar.edu.utn.frc.atenas.seguimiento.repository;

import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositorioVinculadoRepository extends JpaRepository<RepositorioVinculado, Long> {

    /** Como {@code equipo_id} es único, hay a lo sumo un repositorio por equipo. */
    Optional<RepositorioVinculado> findByEquipoId(Long equipoId);
}
