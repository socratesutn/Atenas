package ar.edu.utn.frc.atenas.documentacion.repository;

import ar.edu.utn.frc.atenas.documentacion.entity.ArchivoVerificado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso al archivo verificado de una entrega. */
public interface ArchivoVerificadoRepository extends JpaRepository<ArchivoVerificado, Long> {

    Optional<ArchivoVerificado> findByEntregaId(Long entregaId);
}
