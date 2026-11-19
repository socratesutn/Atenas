package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.ComisionResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CrearComisionRequest;
import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import ar.edu.utn.frc.atenas.catedra.mapper.ComisionMapper;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionRepository;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de las comisiones permanentes. Una comisión se identifica por su
 * nombre y puede habilitarse en varios cursos lectivos.
 */
@Service
public class ComisionService {

    private static final Logger log = LoggerFactory.getLogger(ComisionService.class);

    private final ComisionRepository comisionRepository;

    public ComisionService(ComisionRepository comisionRepository) {
        this.comisionRepository = comisionRepository;
    }

    @Transactional
    public ComisionResponse crear(CrearComisionRequest request) {
        if (comisionRepository.existsByNombre(request.nombre())) {
            throw new ConflictoException("Ya existe una comisión con el nombre " + request.nombre());
        }

        Comision comision = new Comision(request.nombre());

        try {
            comision = comisionRepository.saveAndFlush(comision);
        } catch (DataIntegrityViolationException excepcion) {
            // Protege la carrera entre dos altas simultáneas con el mismo nombre.
            throw new ConflictoException("Ya existe una comisión con el nombre " + request.nombre());
        }

        log.info("Comisión creada id={} nombre={}", comision.getId(), comision.getNombre());
        return ComisionMapper.aResponse(comision);
    }

    @Transactional(readOnly = true)
    public List<ComisionResponse> listar() {
        return comisionRepository.findAll().stream()
                .map(ComisionMapper::aResponse)
                .toList();
    }
}
