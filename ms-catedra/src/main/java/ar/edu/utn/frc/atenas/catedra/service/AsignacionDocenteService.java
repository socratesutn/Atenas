package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.AsignacionDocenteResponse;
import ar.edu.utn.frc.atenas.catedra.dto.AsignarDocenteRequest;
import ar.edu.utn.frc.atenas.catedra.dto.DocenteAsignadoResponse;
import ar.edu.utn.frc.atenas.catedra.entity.AsignacionDocente;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.mapper.AsignacionDocenteMapper;
import ar.edu.utn.frc.atenas.catedra.repository.AsignacionDocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionLectivaRepository;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asignación de docentes a comisiones lectivas y consulta del contrato I04.
 */
@Service
public class AsignacionDocenteService {

    private static final Logger log = LoggerFactory.getLogger(AsignacionDocenteService.class);

    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final DocenteRepository docenteRepository;
    private final ComisionLectivaRepository comisionLectivaRepository;

    public AsignacionDocenteService(AsignacionDocenteRepository asignacionDocenteRepository,
                                    DocenteRepository docenteRepository,
                                    ComisionLectivaRepository comisionLectivaRepository) {
        this.asignacionDocenteRepository = asignacionDocenteRepository;
        this.docenteRepository = docenteRepository;
        this.comisionLectivaRepository = comisionLectivaRepository;
    }

    @Transactional
    public AsignacionDocenteResponse asignar(Long comisionLectivaId, AsignarDocenteRequest request) {
        ComisionLectiva comisionLectiva = comisionLectivaRepository.findById(comisionLectivaId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe la comisión lectiva " + comisionLectivaId));

        Docente docente = docenteRepository.findById(request.docenteId())
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe el docente " + request.docenteId()));

        if (asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(
                docente.getId(), comisionLectiva.getId())) {
            throw new ConflictoException("El docente " + docente.getId()
                    + " ya está asignado a la comisión lectiva " + comisionLectiva.getId());
        }

        AsignacionDocente asignacion = new AsignacionDocente(docente, comisionLectiva);

        try {
            asignacion = asignacionDocenteRepository.saveAndFlush(asignacion);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ConflictoException("El docente " + docente.getId()
                    + " ya está asignado a la comisión lectiva " + comisionLectiva.getId());
        }

        log.info("Docente asignado docenteId={} comisionLectivaId={}",
                docente.getId(), comisionLectiva.getId());
        return AsignacionDocenteMapper.aResponse(asignacion);
    }

    @Transactional(readOnly = true)
    public DocenteAsignadoResponse estaAsignado(Long comisionLectivaId, String sub) {
        if (!comisionLectivaRepository.existsById(comisionLectivaId)) {
            throw new NoEncontradoException("No existe la comisión lectiva " + comisionLectivaId);
        }

        boolean asignado = docenteRepository.findBySub(sub)
                .map(docente -> asignacionDocenteRepository.existsByDocenteIdAndComisionLectivaId(
                        docente.getId(), comisionLectivaId))
                .orElse(false);

        return new DocenteAsignadoResponse(asignado);
    }
}
