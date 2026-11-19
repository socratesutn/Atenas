package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.CrearEquipoRequest;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EquipoResumenResponse;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.catedra.entity.Estudiante;
import ar.edu.utn.frc.atenas.catedra.entity.Integrante;
import ar.edu.utn.frc.atenas.catedra.mapper.EquipoMapper;
import ar.edu.utn.frc.atenas.catedra.repository.AsignacionDocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionLectivaRepository;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.EquipoRepository;
import ar.edu.utn.frc.atenas.catedra.repository.EstudianteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.IntegranteRepository;
import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creación, consulta e ingreso a equipos. El service aplica RN06-RN09 y los
 * permisos de dominio; el patrón State decide únicamente si el estado permite
 * la operación.
 */
@Service
public class EquipoService {

    private static final Logger log = LoggerFactory.getLogger(EquipoService.class);

    private final EquipoRepository equipoRepository;
    private final IntegranteRepository integranteRepository;
    private final EstudianteRepository estudianteRepository;
    private final ComisionLectivaRepository comisionLectivaRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioActual usuarioActual;

    public EquipoService(EquipoRepository equipoRepository,
                         IntegranteRepository integranteRepository,
                         EstudianteRepository estudianteRepository,
                         ComisionLectivaRepository comisionLectivaRepository,
                         AsignacionDocenteRepository asignacionDocenteRepository,
                         DocenteRepository docenteRepository,
                         UsuarioActual usuarioActual) {
        this.equipoRepository = equipoRepository;
        this.integranteRepository = integranteRepository;
        this.estudianteRepository = estudianteRepository;
        this.comisionLectivaRepository = comisionLectivaRepository;
        this.asignacionDocenteRepository = asignacionDocenteRepository;
        this.docenteRepository = docenteRepository;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public EquipoResponse crear(Long comisionLectivaId, CrearEquipoRequest request) {
        ComisionLectiva comisionLectiva = comisionLectivaRepository.findById(comisionLectivaId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe la comisión lectiva " + comisionLectivaId));

        validarDocenteAsignado(comisionLectivaId);
        validarCupos(request);

        if (equipoRepository.existsByComisionLectivaIdAndNombre(comisionLectivaId, request.nombre())) {
            throw new ConflictoException("Ya existe un equipo con ese nombre en la comisión");
        }

        Equipo equipo = new Equipo(
                request.nombre(), request.cupoMinimo(), request.cupoMaximo(), comisionLectiva);

        try {
            equipo = equipoRepository.saveAndFlush(equipo);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ConflictoException("Ya existe un equipo con ese nombre en la comisión");
        }

        log.info("Equipo creado id={} comisionLectivaId={}", equipo.getId(), comisionLectivaId);
        return EquipoMapper.aResponse(equipo, List.of());
    }

    @Transactional(readOnly = true)
    public List<EquipoResumenResponse> listarPorComision(Long comisionLectivaId) {
        if (!comisionLectivaRepository.existsById(comisionLectivaId)) {
            throw new NoEncontradoException("No existe la comisión lectiva " + comisionLectivaId);
        }

        validarAccesoComision(comisionLectivaId);

        List<Equipo> equipos = equipoRepository.findByComisionLectivaId(comisionLectivaId);
        if (equipos.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> cantidades = cantidadesPorEquipo(equipos);
        return equipos.stream()
                .map(equipo -> EquipoMapper.aResumen(
                        equipo, cantidades.getOrDefault(equipo.getId(), 0L).intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(Long equipoId) {
        Equipo equipo = equipoRepository.buscarPorId(equipoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el equipo " + equipoId));

        validarAccesoComision(equipo.getComisionLectiva().getId());
        return EquipoMapper.aResponse(equipo, integrantesVigentes(equipoId));
    }

    @Transactional
    public EquipoResponse ingresar(Long equipoId) {
        // El bloqueo serializa la validación de cupo y el primer ingreso.
        Equipo equipo = equipoRepository.findByIdForUpdate(equipoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el equipo " + equipoId));

        String sub = usuarioActual.sub();
        Estudiante estudiante = (sub == null || sub.isBlank())
                ? null
                : estudianteRepository.findBySub(sub).orElse(null);

        if (estudiante == null) {
            throw new NoEncontradoException("No existe un estudiante asociado al token");
        }
        if (!estudiante.isActivo()) {
            throw new AccesoDenegadoException("El estudiante no está activo", "RN-01");
        }
        if (!estudiante.getComisionLectiva().getId()
                .equals(equipo.getComisionLectiva().getId())) {
            throw new AccesoDenegadoException(
                    "El estudiante no pertenece a la comisión del equipo", "RN-01");
        }
        if (integranteRepository.existsByEstudianteIdAndFechaBajaIsNull(estudiante.getId())) {
            throw new ConflictoException(
                    "El estudiante ya integra un equipo vigente", "RN-07");
        }

        // El estado decide si el ingreso es válido.
        equipo.ingresar();

        long cantidadActual = integranteRepository.countByEquipoIdAndFechaBajaIsNull(equipoId);
        if (cantidadActual >= equipo.getCupoMaximo()) {
            throw new ConflictoException("El equipo no tiene cupo disponible", "RN-08");
        }

        Integrante integrante = new Integrante(equipo, estudiante, Instant.now());
        try {
            integranteRepository.saveAndFlush(integrante);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ConflictoException(
                    "El estudiante ya integra un equipo vigente", "RN-07");
        }

        if (cantidadActual == 0) {
            equipo.setRepresentante(estudiante);
            equipoRepository.saveAndFlush(equipo);
        }

        log.info("Estudiante ingresó al equipo equipoId={} estudianteId={}",
                equipoId, estudiante.getId());
        return EquipoMapper.aResponse(equipo, integrantesVigentes(equipoId));
    }

    @Transactional
    public void salir(Long equipoId, Long estudianteId) {
        Equipo equipo = equipoRepository.findByIdForUpdate(equipoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el equipo " + equipoId));

        Integrante integrante = integranteRepository
                .findByEquipoIdAndEstudianteIdAndFechaBajaIsNull(equipoId, estudianteId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe una participación vigente del estudiante " + estudianteId
                                + " en el equipo " + equipoId));

        String sub = usuarioActual.sub();
        boolean propio = sub != null && sub.equals(integrante.getEstudiante().getSub());
        boolean representante = sub != null && equipo.getRepresentante() != null
                && sub.equals(equipo.getRepresentante().getSub());

        if (!propio && !representante) {
            throw new AccesoDenegadoException(
                    "Solo el propio integrante o el representante pueden realizar esta salida", "RN-09");
        }

        cerrarParticipacion(equipo, integrante);
        log.info("Estudiante salió del equipo equipoId={} estudianteId={}", equipoId, estudianteId);
    }

    @Transactional
    public EquipoResponse asegurar(Long equipoId) {
        Equipo equipo = equipoRepository.findByIdForUpdate(equipoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el equipo " + equipoId));

        // El estado decide primero si el equipo puede asegurarse.
        equipo.asegurar();

        if (usuarioActual.tieneRol(Roles.DOCENTE)) {
            validarDocenteAsignado(equipo.getComisionLectiva().getId());
        } else if (usuarioActual.tieneRol(Roles.ALUMNO)) {
            validarRepresentanteYCupo(equipo);
        } else {
            throw new AccesoDenegadoException(
                    "Solo un docente asignado o el representante pueden asegurar el equipo", "RN-10");
        }

        equipo = equipoRepository.saveAndFlush(equipo);
        log.info("Equipo asegurado id={}", equipoId);
        return EquipoMapper.aResponse(equipo, integrantesVigentes(equipoId));
    }

    @Transactional
    public EquipoResponse reabrir(Long equipoId) {
        Equipo equipo = equipoRepository.findByIdForUpdate(equipoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el equipo " + equipoId));

        validarDocenteAsignado(equipo.getComisionLectiva().getId());
        equipo.reabrir();
        equipo = equipoRepository.saveAndFlush(equipo);

        log.info("Equipo reabierto id={}", equipoId);
        return EquipoMapper.aResponse(equipo, integrantesVigentes(equipoId));
    }

    /**
     * Cierra una participación y actualiza el representante si era necesario.
     * Se reutiliza desde C20 y desde la baja de estudiante C13.
     */
    void cerrarParticipacion(Equipo equipo, Integrante integrante) {
        equipo.salir();
        integrante.setFechaBaja(Instant.now());
        integranteRepository.saveAndFlush(integrante);

        boolean eraRepresentante = equipo.getRepresentante() != null
                && equipo.getRepresentante().getId().equals(integrante.getEstudiante().getId());

        if (eraRepresentante) {
            List<Integrante> restantes = integranteRepository
                    .findByEquipoIdAndFechaBajaIsNullOrderByFechaIngresoAscIdAsc(equipo.getId());

            equipo.setRepresentante(restantes.isEmpty() ? null : restantes.getFirst().getEstudiante());
            equipoRepository.saveAndFlush(equipo);
        }
    }

    @Transactional(readOnly = true)
    public EquipoInternoResponse obtenerInterno(Long equipoId) {
        Equipo equipo = equipoRepository.buscarPorId(equipoId)
                .orElseThrow(() -> new NoEncontradoException("No existe el equipo " + equipoId));
        return EquipoMapper.aInterno(equipo, integrantesVigentes(equipoId));
    }

    @Transactional(readOnly = true)
    public List<EquipoInternoResponse> listarInterno(Long comisionLectivaId, Long cursoLectivoId) {
        if ((comisionLectivaId == null) == (cursoLectivoId == null)) {
            throw new SolicitudInvalidaException(
                    "Se debe indicar exactamente comisionLectivaId o cursoLectivoId");
        }

        List<Equipo> equipos = comisionLectivaId != null
                ? equipoRepository.findByComisionLectivaId(comisionLectivaId)
                : equipoRepository.findByComisionLectivaCursoLectivoId(cursoLectivoId);

        if (equipos.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Integrante>> integrantesPorEquipo = integranteRepository
                .findByEquipoIdInAndFechaBajaIsNull(equipos.stream().map(Equipo::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(integrante -> integrante.getEquipo().getId()));

        return equipos.stream()
                .map(equipo -> EquipoMapper.aInterno(
                        equipo, integrantesPorEquipo.getOrDefault(equipo.getId(), List.of())))
                .toList();
    }

    private List<Integrante> integrantesVigentes(Long equipoId) {
        return integranteRepository.findByEquipoIdAndFechaBajaIsNull(equipoId);
    }

    private Map<Long, Long> cantidadesPorEquipo(Collection<Equipo> equipos) {
        return integranteRepository
                .findByEquipoIdInAndFechaBajaIsNull(equipos.stream().map(Equipo::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        integrante -> integrante.getEquipo().getId(),
                        Collectors.counting()));
    }

    private void validarRepresentanteYCupo(Equipo equipo) {
        String sub = usuarioActual.sub();
        if (sub == null || equipo.getRepresentante() == null
                || !sub.equals(equipo.getRepresentante().getSub())) {
            throw new AccesoDenegadoException(
                    "Solo el representante puede asegurar el equipo", "RN-10");
        }

        List<Integrante> integrantes = integrantesVigentes(equipo.getId());
        if (integrantes.size() < equipo.getCupoMinimo()
                || integrantes.size() > equipo.getCupoMaximo()) {
            throw new ConflictoException(
                    "La cantidad de integrantes no está dentro del rango de cupo", "RN-10");
        }

        boolean todosTienenGithub = integrantes.stream()
                .allMatch(integrante -> integrante.getEstudiante().getUsuarioGithub() != null
                        && !integrante.getEstudiante().getUsuarioGithub().isBlank());

        if (!todosTienenGithub) {
            throw new ConflictoException(
                    "Todos los integrantes deben tener un usuario de GitHub cargado", "RN-10");
        }
    }

    private void validarCupos(CrearEquipoRequest request) {
        if (request.cupoMinimo() == null || request.cupoMaximo() == null
                || request.cupoMinimo() < 1
                || request.cupoMinimo() > request.cupoMaximo()) {
            throw new SolicitudInvalidaException(
                    "El cupo mínimo debe ser al menos 1 y no puede superar al máximo");
        }
    }

    private void validarDocenteAsignado(Long comisionLectivaId) {
        String sub = usuarioActual.sub();
        Docente docente = (sub == null || sub.isBlank())
                ? null
                : docenteRepository.findBySub(sub).orElse(null);

        if (docente == null || !asignacionDocenteRepository
                .existsByDocenteIdAndComisionLectivaId(docente.getId(), comisionLectivaId)) {
            throw new AccesoDenegadoException(
                    "El docente no está asignado a la comisión", "RN-01");
        }
    }

    private void validarAccesoComision(Long comisionLectivaId) {
        if (usuarioActual.tieneRol(Roles.JEFE_CATEDRA)) {
            return;
        }

        String sub = usuarioActual.sub();
        if (usuarioActual.tieneRol(Roles.DOCENTE)) {
            Docente docente = (sub == null || sub.isBlank())
                    ? null
                    : docenteRepository.findBySub(sub).orElse(null);
            if (docente != null && asignacionDocenteRepository
                    .existsByDocenteIdAndComisionLectivaId(docente.getId(), comisionLectivaId)) {
                return;
            }
        }

        if (usuarioActual.tieneRol(Roles.ALUMNO)) {
            Estudiante estudiante = (sub == null || sub.isBlank())
                    ? null
                    : estudianteRepository.findBySub(sub).orElse(null);
            if (estudiante != null && estudiante.getComisionLectiva().getId()
                    .equals(comisionLectivaId)) {
                return;
            }
        }

        throw new AccesoDenegadoException(
                "No tiene acceso a los equipos de esa comisión", "RN-01");
    }
}
