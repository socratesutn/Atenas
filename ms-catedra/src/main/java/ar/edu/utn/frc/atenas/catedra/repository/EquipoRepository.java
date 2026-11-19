package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a equipos. Las consultas de detalle traen comisión, curso y
 * representante; el ingreso usa una consulta con bloqueo pesimista para
 * serializar la validación de cupo.
 */
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    @EntityGraph(attributePaths = {
            "comisionLectiva",
            "comisionLectiva.cursoLectivo",
            "representante"
    })
    @Query("select e from Equipo e where e.id = :id")
    Optional<Equipo> buscarPorId(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "comisionLectiva",
            "comisionLectiva.cursoLectivo",
            "representante"
    })
    List<Equipo> findByComisionLectivaId(Long comisionLectivaId);

    @EntityGraph(attributePaths = {
            "comisionLectiva",
            "comisionLectiva.cursoLectivo",
            "representante"
    })
    List<Equipo> findByComisionLectivaCursoLectivoId(Long cursoLectivoId);

    boolean existsByComisionLectivaIdAndNombre(Long comisionLectivaId, String nombre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipo e where e.id = :id")
    Optional<Equipo> findByIdForUpdate(@Param("id") Long id);
}
