package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.ComisionLectivaResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CrearComisionLectivaRequest;
import ar.edu.utn.frc.atenas.catedra.entity.AsignacionDocente;
import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import ar.edu.utn.frc.atenas.catedra.mapper.ComisionLectivaMapper;
import ar.edu.utn.frc.atenas.catedra.repository.AsignacionDocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionLectivaRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionRepository;
import ar.edu.utn.frc.atenas.catedra.repository.CursoLectivoRepository;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Habilita comisiones dentro de un curso lectivo y las lista por curso.
 * C06 aplica RN01: el jefe ve todas y el docente solo las que tiene asignadas.
 */
@Service
public class ComisionLectivaService {

    private static final Logger log = LoggerFactory.getLogger(ComisionLectivaService.class);

    private final ComisionLectivaRepository comisionLectivaRepository;
    private final CursoLectivoRepository cursoLectivoRepository;
    private final ComisionRepository comisionRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioActual usuarioActual;

    public ComisionLectivaService(ComisionLectivaRepository comisionLectivaRepository,
                                  CursoLectivoRepository cursoLectivoRepository,
                                  ComisionRepository comisionRepository,
                                  AsignacionDocenteRepository asignacionDocenteRepository,
                                  DocenteRepository docenteRepository,
                                  UsuarioActual usuarioActual) {
        this.comisionLectivaRepository = comisionLectivaRepository;
        this.cursoLectivoRepository = cursoLectivoRepository;
        this.comisionRepository = comisionRepository;
        this.asignacionDocenteRepository = asignacionDocenteRepository;
        this.docenteRepository = docenteRepository;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public ComisionLectivaResponse crear(Long cursoLectivoId, CrearComisionLectivaRequest request) {
        CursoLectivo curso = cursoLectivoRepository.findById(cursoLectivoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el curso lectivo " + cursoLectivoId));

        Comision comision = comisionRepository.findById(request.comisionId())
                .orElseThrow(() -> new NoEncontradoException("No existe la comisión " + request.comisionId()));

        if (comisionLectivaRepository.existsByComisionIdAndCursoLectivoId(comision.getId(), curso.getId())) {
            throw new ConflictoException("La comisión " + comision.getId()
                    + " ya está habilitada en el curso lectivo " + curso.getId());
        }

        ComisionLectiva comisionLectiva = new ComisionLectiva(comision, curso);

        try {
            comisionLectiva = comisionLectivaRepository.saveAndFlush(comisionLectiva);
        } catch (DataIntegrityViolationException excepcion) {
            // La restricción única evita duplicados si dos pedidos llegan a la vez.
            throw new ConflictoException("La comisión " + comision.getId()
                    + " ya está habilitada en el curso lectivo " + curso.getId());
        }

        log.info("Comisión lectiva creada id={} cursoLectivoId={} comisionId={}",
                comisionLectiva.getId(), curso.getId(), comision.getId());
        return ComisionLectivaMapper.aResponse(comisionLectiva);
    }

    @Transactional(readOnly = true)
    public List<ComisionLectivaResponse> listarPorCurso(Long cursoLectivoId) {
        if (!cursoLectivoRepository.existsById(cursoLectivoId)) {
            throw new NoEncontradoException("No existe el curso lectivo " + cursoLectivoId);
        }

        List<ComisionLectiva> comisiones = comisionLectivaRepository.findByCursoLectivoId(cursoLectivoId);

        if (usuarioActual.tieneRol(Roles.JEFE_CATEDRA)) {
            return comisiones.stream()
                    .map(ComisionLectivaMapper::aResponse)
                    .toList();
        }

        if (!usuarioActual.tieneRol(Roles.DOCENTE)) {
            throw new AccesoDenegadoException(
                    "Solo el jefe o los docentes pueden consultar las comisiones lectivas", "RN-01");
        }

        String sub = usuarioActual.sub();
        if (sub == null || sub.isBlank()) {
            return List.of();
        }

        return docenteRepository.findBySub(sub)
                .map(docente -> {
                    Set<Long> asignadas = asignacionDocenteRepository
                            .findByDocenteIdAndComisionLectivaCursoLectivoId(docente.getId(), cursoLectivoId)
                            .stream()
                            .map(AsignacionDocente::getComisionLectiva)
                            .map(ComisionLectiva::getId)
                            .collect(Collectors.toSet());

                    return comisiones.stream()
                            .filter(comision -> asignadas.contains(comision.getId()))
                            .map(ComisionLectivaMapper::aResponse)
                            .toList();
                })
                .orElseGet(List::of);
    }
}
