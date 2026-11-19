package ar.edu.utn.frc.atenas.documentacion.repository;

import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Consultas de hitos propias de ms-documentacion. */
public interface HitoRepository extends JpaRepository<Hito, Long> {

    List<Hito> findByCursoLectivoIdOrderByOrdenAscIdAsc(Long cursoLectivoId);

    Optional<Hito> findByIdAndCursoLectivoId(Long id, Long cursoLectivoId);
}
