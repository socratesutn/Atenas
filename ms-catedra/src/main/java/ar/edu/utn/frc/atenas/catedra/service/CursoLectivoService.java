package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.CrearCursoLectivoRequest;
import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CursoLectivoResponse;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import ar.edu.utn.frc.atenas.catedra.mapper.CursoLectivoMapper;
import ar.edu.utn.frc.atenas.catedra.repository.CursoLectivoRepository;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de los cursos lectivos: fechas coherentes, año único y consulta del
 * contrato interno que usa ms-documentacion.
 */
@Service
public class CursoLectivoService {

    private static final Logger log = LoggerFactory.getLogger(CursoLectivoService.class);

    private final CursoLectivoRepository cursoLectivoRepository;

    public CursoLectivoService(CursoLectivoRepository cursoLectivoRepository) {
        this.cursoLectivoRepository = cursoLectivoRepository;
    }

    /**
     * Crea un curso lectivo. Primero valida la regla de fechas y luego el
     * duplicado; la restricción única de la base se captura por si dos pedidos
     * simultáneos pasan el prechequeo.
     */
    @Transactional
    public CursoLectivoResponse crear(CrearCursoLectivoRequest request) {
        validarFechas(request);

        if (cursoLectivoRepository.existsByAnio(request.anio())) {
            throw new ConflictoException("Ya existe un curso lectivo para el año " + request.anio());
        }

        CursoLectivo curso = new CursoLectivo(request.anio(), request.fechaInicio(), request.fechaFin());

        try {
            curso = cursoLectivoRepository.saveAndFlush(curso);
        } catch (DataIntegrityViolationException excepcion) {
            // Puede ocurrir si otro pedido insertó el mismo año entre el
            // prechequeo y el save. Se traduce a 409, no a un 500.
            throw new ConflictoException("Ya existe un curso lectivo para el año " + request.anio());
        }

        log.info("Curso lectivo creado id={} anio={}", curso.getId(), curso.getAnio());
        return CursoLectivoMapper.aResponse(curso);
    }

    @Transactional(readOnly = true)
    public List<CursoLectivoResponse> listar() {
        return cursoLectivoRepository.findAll().stream()
                .map(CursoLectivoMapper::aResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CursoLectivoInternoResponse obtenerInterno(Long id) {
        CursoLectivo curso = cursoLectivoRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el curso lectivo " + id));
        return CursoLectivoMapper.aInternoResponse(curso);
    }

    private void validarFechas(CrearCursoLectivoRequest request) {
        if (request.fechaInicio() == null || request.fechaFin() == null) {
            throw new SolicitudInvalidaException("Las fechas de inicio y fin son obligatorias");
        }
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new SolicitudInvalidaException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }
}
