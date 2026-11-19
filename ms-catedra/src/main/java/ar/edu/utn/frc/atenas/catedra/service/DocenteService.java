package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.client.KeycloakAdminClient;
import ar.edu.utn.frc.atenas.catedra.config.KeycloakProperties;
import ar.edu.utn.frc.atenas.catedra.dto.CrearDocenteRequest;
import ar.edu.utn.frc.atenas.catedra.dto.DocenteResponse;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.mapper.DocenteMapper;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Alta y baja de docentes. El alta inserta primero la fila local y luego crea
 * la cuenta en Keycloak; si Keycloak falla, la transacción revierte la fila.
 * La clave temporal nunca se loguea.
 */
@Service
public class DocenteService {

    private static final Logger log = LoggerFactory.getLogger(DocenteService.class);

    private final DocenteRepository docenteRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakProperties keycloakProperties;

    public DocenteService(DocenteRepository docenteRepository,
                          KeycloakAdminClient keycloakAdminClient,
                          KeycloakProperties keycloakProperties) {
        this.docenteRepository = docenteRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakProperties = keycloakProperties;
    }

    @Transactional
    public DocenteResponse crear(CrearDocenteRequest request) {
        if (docenteRepository.existsByMail(request.mail())) {
            throw new ConflictoException("Ya existe un docente con el mail " + request.mail());
        }

        Docente docente = new Docente(request.nombre(), request.apellido(), request.mail());

        try {
            // El flush detecta el mail duplicado antes de llamar a Keycloak.
            docente = docenteRepository.saveAndFlush(docente);
        } catch (DataIntegrityViolationException excepcion) {
            throw new ConflictoException("Ya existe un docente con el mail " + request.mail());
        }

        // La llamada a Keycloak ocurre dentro de la transacción por la excepción
        // documentada en ms-catedra/AGENTS.md (RN03).
        String sub = keycloakAdminClient.crearUsuario(
                request.mail(),
                request.nombre(),
                request.apellido(),
                keycloakProperties.getClaveTemporal());
        keycloakAdminClient.asignarRolRealm(sub, Roles.DOCENTE);

        docente.setSub(sub);
        docente = docenteRepository.saveAndFlush(docente);

        log.info("Docente creado id={} mail={}", docente.getId(), docente.getMail());
        return DocenteMapper.aResponse(docente);
    }

    @Transactional(readOnly = true)
    public List<DocenteResponse> listar() {
        return docenteRepository.findAll().stream()
                .map(DocenteMapper::aResponse)
                .toList();
    }

    @Transactional
    public DocenteResponse darDeBaja(Long id) {
        Docente docente = docenteRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el docente " + id));

        if (docente.getSub() == null || docente.getSub().isBlank()) {
            throw new ServicioNoDisponibleException("El docente no tiene una cuenta de Keycloak asociada");
        }

        docente.setActivo(false);
        docente = docenteRepository.saveAndFlush(docente);
        keycloakAdminClient.deshabilitarUsuario(docente.getSub());

        log.info("Docente dado de baja id={} mail={}", docente.getId(), docente.getMail());
        return DocenteMapper.aResponse(docente);
    }
}
