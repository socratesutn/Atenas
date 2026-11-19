package ar.edu.utn.frc.atenas.documentacion.repository;

import ar.edu.utn.frc.atenas.documentacion.entity.Entrega;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Consultas de entregas para el cálculo de estados y la apertura D06. */
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    List<Entrega> findByHitoIdInAndEquipoIdIn(Collection<Long> hitoIds,
                                               Collection<Long> equipoIds);

    Optional<Entrega> findByHitoIdAndEquipoId(Long hitoId, Long equipoId);

    @Override
    @EntityGraph(attributePaths = "hito")
    Optional<Entrega> findById(Long id);
}
