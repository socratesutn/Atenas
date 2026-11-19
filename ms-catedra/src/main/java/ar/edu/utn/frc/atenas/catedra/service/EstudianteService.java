package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.client.KeycloakAdminClient;
import ar.edu.utn.frc.atenas.catedra.config.KeycloakProperties;
import ar.edu.utn.frc.atenas.catedra.dto.ActualizarUsuarioGithubRequest;
import ar.edu.utn.frc.atenas.catedra.dto.CrearEstudianteRequest;
import ar.edu.utn.frc.atenas.catedra.dto.EstudianteInternoResponse;
import ar.edu.utn.frc.atenas.catedra.dto.EstudianteResponse;
import ar.edu.utn.frc.atenas.catedra.dto.MiPerfilResponse;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.catedra.entity.Estudiante;
import ar.edu.utn.frc.atenas.catedra.mapper.EstudianteMapper;
import ar.edu.utn.frc.atenas.catedra.repository.AsignacionDocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionLectivaRepository;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.EquipoRepository;
import ar.edu.utn.frc.atenas.catedra.repository.EstudianteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.IntegranteRepository;
import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Alta, baja, consulta y perfil de estudiantes. El alta sigue la misma
 * secuencia que la de docentes: insertar, crear la cuenta en Keycloak, asignar
 * rol, guardar el sub y confirmar. Si Keycloak falla, la transacción revierte.
 */
@Service
public class EstudianteService {

    private static final Logger log = LoggerFactory.getLogger(EstudianteService.class);
    private static final String DOMINIO_MAIL = "@sistemas.frc.utn.edu.ar";
    private static final String PATRON_LEGAJO = "\\d+";
    private static final String PATRON_GITHUB = "^[A-Za-z0-9-]{1,39}$";

    private final EstudianteRepository estudianteRepository;
    private final ComisionLectivaRepository comisionLectivaRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final DocenteRepository docenteRepository;
    private final IntegranteRepository integranteRepository;
    private final EquipoRepository equipoRepository;
    private final EquipoService equipoService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakProperties keycloakProperties;
    private final UsuarioActual usuarioActual;

    public EstudianteService(EstudianteRepository estudianteRepository,
                             ComisionLectivaRepository comisionLectivaRepository,
                             AsignacionDocenteRepository asignacionDocenteRepository,
                             DocenteRepository docenteRepository,
                             IntegranteRepository integranteRepository,
                             EquipoRepository equipoRepository,
                             EquipoService equipoService,
                             KeycloakAdminClient keycloakAdminClient,
                             KeycloakProperties keycloakProperties,
                             UsuarioActual usuarioActual) {
        this.estudianteRepository = estudianteRepository;
        this.comisionLectivaRepository = comisionLectivaRepository;
        this.asignacionDocenteRepository = asignacionDocenteRepository;
        this.docenteRepository = docenteRepository;
        this.integranteRepository = integranteRepository;
        this.equipoRepository = equipoRepository;
        this.equipoService = equipoService;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakProperties = keycloakProperties;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public EstudianteResponse crear(CrearEstudianteRequest request) {
        validarLegajo(request.legajo());

        if (estudianteRepository.existsByLegajo(request.legajo())) {
            throw new ConflictoException("Ya existe un estudiante con el legajo " + request.legajo());
        }

        ComisionLectiva comisionLectiva = comisionLectivaRepository.findById(request.comisionLectivaId())
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe la comisión lectiva " + request.comisionLectivaId()));

        String mail = request.legajo() + DOMINIO_MAIL;
        Estudiante estudiante = new Estudiante(
                request.legajo(), request.nombre(), request.apellido(), mail, comisionLectiva);

        try {
            // El flush detecta legajo o mail duplicado antes de llamar a Keycloak.
            estudiante = estudianteRepository.saveAndFlush(estudiante);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ConflictoException("Ya existe un estudiante con el legajo " + request.legajo());
        }

        // La llamada a Keycloak ocurre dentro de la transacción por RN03.
        String sub = keycloakAdminClient.crearUsuario(
                mail,
                request.nombre(),
                request.apellido(),
                keycloakProperties.getClaveTemporal());
        keycloakAdminClient.asignarRolRealm(sub, Roles.ALUMNO);

        estudiante.setSub(sub);
        estudiante = estudianteRepository.saveAndFlush(estudiante);

        log.info("Estudiante creado id={} legajo={}", estudiante.getId(), estudiante.getLegajo());
        return EstudianteMapper.aResponse(estudiante);
    }

    @Transactional(readOnly = true)
    public List<EstudianteResponse> listarPorComision(Long comisionLectivaId) {
        if (!comisionLectivaRepository.existsById(comisionLectivaId)) {
            throw new NoEncontradoException("No existe la comisión lectiva " + comisionLectivaId);
        }

        if (usuarioActual.tieneRol(Roles.JEFE_CATEDRA)) {
            return estudianteRepository.findByComisionLectivaId(comisionLectivaId).stream()
                    .map(EstudianteMapper::aResponse)
                    .toList();
        }

        if (!usuarioActual.tieneRol(Roles.DOCENTE)) {
            throw new AccesoDenegadoException(
                    "Solo el jefe o un docente asignado pueden consultar los estudiantes", "RN-01");
        }

        String sub = usuarioActual.sub();
        Docente docente = (sub == null || sub.isBlank())
                ? null
                : docenteRepository.findBySub(sub).orElse(null);

        if (docente == null || !asignacionDocenteRepository
                .existsByDocenteIdAndComisionLectivaId(docente.getId(), comisionLectivaId)) {
            throw new AccesoDenegadoException("El docente no está asignado a la comisión", "RN-01");
        }

        return estudianteRepository.findByComisionLectivaId(comisionLectivaId).stream()
                .map(EstudianteMapper::aResponse)
                .toList();
    }

    @Transactional
    public EstudianteResponse darDeBaja(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el estudiante " + id));

        if (estudiante.getSub() == null || estudiante.getSub().isBlank()) {
            throw new ServicioNoDisponibleException("El estudiante no tiene una cuenta de Keycloak asociada");
        }

        integranteRepository.findByEstudianteIdAndFechaBajaIsNull(id).ifPresent(integrante -> {
            Equipo equipo = equipoRepository.findByIdForUpdate(integrante.getEquipo().getId())
                    .orElseThrow(() -> new NoEncontradoException(
                            "No existe el equipo " + integrante.getEquipo().getId()));
            equipoService.cerrarParticipacion(equipo, integrante);
        });

        estudiante.setActivo(false);
        estudiante = estudianteRepository.saveAndFlush(estudiante);
        keycloakAdminClient.deshabilitarUsuario(estudiante.getSub());

        log.info("Estudiante dado de baja id={} legajo={}", estudiante.getId(), estudiante.getLegajo());
        return EstudianteMapper.aResponse(estudiante);
    }

    @Transactional(readOnly = true)
    public MiPerfilResponse miPerfil() {
        Estudiante estudiante = obtenerPorSub(usuarioActual.sub());
        return EstudianteMapper.aMiPerfil(estudiante, equipoIdDe(estudiante.getId()));
    }

    @Transactional
    public MiPerfilResponse actualizarUsuarioGithub(ActualizarUsuarioGithubRequest request) {
        validarUsuarioGithub(request.usuarioGithub());

        Estudiante estudiante = obtenerPorSub(usuarioActual.sub());
        String normalizado = request.usuarioGithub().toLowerCase(Locale.ROOT);

        boolean cambia = estudiante.getUsuarioGithub() == null
                || !estudiante.getUsuarioGithub().equals(normalizado);

        if (cambia && estudianteRepository.existsByUsuarioGithub(normalizado)) {
            throw new ConflictoException("El usuario de GitHub " + normalizado + " ya está en uso");
        }

        estudiante.setUsuarioGithub(normalizado);
        try {
            estudiante = estudianteRepository.saveAndFlush(estudiante);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ConflictoException("El usuario de GitHub " + normalizado + " ya está en uso");
        }

        log.info("Usuario de GitHub actualizado estudianteId={}", estudiante.getId());
        return EstudianteMapper.aMiPerfil(estudiante, equipoIdDe(estudiante.getId()));
    }

    @Transactional(readOnly = true)
    public EstudianteInternoResponse obtenerInternoPorSub(String sub) {
        Estudiante estudiante = obtenerPorSub(sub);
        return EstudianteMapper.aInterno(estudiante, equipoIdDe(estudiante.getId()));
    }

    private Long equipoIdDe(Long estudianteId) {
        return integranteRepository.findByEstudianteIdAndFechaBajaIsNull(estudianteId)
                .map(integrante -> integrante.getEquipo().getId())
                .orElse(null);
    }

    private Estudiante obtenerPorSub(String sub) {
        if (sub == null || sub.isBlank()) {
            throw new NoEncontradoException("No se encontró el estudiante del token");
        }
        return estudianteRepository.findBySub(sub)
                .orElseThrow(() -> new NoEncontradoException("No existe un estudiante con ese sub"));
    }

    private void validarLegajo(String legajo) {
        if (legajo == null || !legajo.matches(PATRON_LEGAJO)) {
            throw new SolicitudInvalidaException("El legajo debe contener solo dígitos");
        }
    }

    private void validarUsuarioGithub(String usuarioGithub) {
        if (usuarioGithub == null || !usuarioGithub.matches(PATRON_GITHUB)) {
            throw new SolicitudInvalidaException(
                    "El usuario de GitHub solo admite letras, dígitos y guiones, hasta 39 caracteres");
        }
    }
}
