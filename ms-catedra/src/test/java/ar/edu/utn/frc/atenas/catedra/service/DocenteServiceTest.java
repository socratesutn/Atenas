package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.client.KeycloakAdminClient;
import ar.edu.utn.frc.atenas.catedra.config.KeycloakProperties;
import ar.edu.utn.frc.atenas.catedra.dto.CrearDocenteRequest;
import ar.edu.utn.frc.atenas.catedra.dto.DocenteResponse;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocenteServiceTest {

    private static final String MAIL = "docente@atenas.local";

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private KeycloakProperties keycloakProperties;

    @InjectMocks
    private DocenteService docenteService;

    @Test
    @DisplayName("RN-03: crea el docente, la cuenta y le asigna el rol DOCENTE")
    void deberiaCrearDocente_cuandoKeycloakResponde() {
        when(docenteRepository.existsByMail(MAIL)).thenReturn(false);
        when(keycloakProperties.getClaveTemporal()).thenReturn("clave-temporal");
        when(keycloakAdminClient.crearUsuario(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("sub-1");
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(invocacion -> {
            Docente docente = invocacion.getArgument(0);
            if (docente.getId() == null) {
                docente.setId(1L);
            }
            return docente;
        });

        DocenteResponse respuesta = docenteService.crear(
                new CrearDocenteRequest("Docente", "Prueba", MAIL));

        assertEquals(1L, respuesta.id());
        assertEquals(MAIL, respuesta.mail());
        verify(keycloakAdminClient).asignarRolRealm("sub-1", Roles.DOCENTE);
    }

    @Test
    @DisplayName("RN-03: rechaza un mail duplicado sin llamar a Keycloak")
    void deberiaRechazarDocente_cuandoElMailYaExiste() {
        when(docenteRepository.existsByMail(MAIL)).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> docenteService.crear(new CrearDocenteRequest("Docente", "Prueba", MAIL)));
        verify(keycloakAdminClient, never()).crearUsuario(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("RN-03: propaga el 503 cuando Keycloak falla")
    void deberiaPropagarServicioNoDisponible_cuandoKeycloakFalla() {
        when(docenteRepository.existsByMail(MAIL)).thenReturn(false);
        when(keycloakProperties.getClaveTemporal()).thenReturn("clave-temporal");
        when(keycloakAdminClient.crearUsuario(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("Keycloak caído"));
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(invocacion -> {
            Docente docente = invocacion.getArgument(0);
            docente.setId(1L);
            return docente;
        });

        assertThrows(ServicioNoDisponibleException.class,
                () -> docenteService.crear(new CrearDocenteRequest("Docente", "Prueba", MAIL)));
    }

    @Test
    @DisplayName("Lista todos los docentes")
    void deberiaListarDocentes() {
        when(docenteRepository.findAll()).thenReturn(List.of(
                new Docente(1L, "Docente", "Prueba", MAIL, "sub-1", true)));

        List<DocenteResponse> respuesta = docenteService.listar();

        assertEquals(1, respuesta.size());
        assertEquals(MAIL, respuesta.getFirst().mail());
    }

    @Test
    @DisplayName("RN-05: responde 404 cuando el docente no existe")
    void deberiaLanzarNoEncontrado_cuandoDocenteNoExiste() {
        when(docenteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> docenteService.darDeBaja(99L));
    }

    @Test
    @DisplayName("RN-05: da de baja al docente y deshabilita su cuenta")
    void deberiaDarDeBaja_cuandoKeycloakResponde() {
        Docente docente = new Docente(1L, "Docente", "Prueba", MAIL, "sub-1", true);
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        DocenteResponse respuesta = docenteService.darDeBaja(1L);

        assertFalse(respuesta.activo());
        verify(keycloakAdminClient).deshabilitarUsuario("sub-1");
    }

    @Test
    @DisplayName("RN-05: propaga el 503 cuando Keycloak falla en la baja")
    void deberiaPropagarServicioNoDisponible_cuandoKeycloakFallaEnLaBaja() {
        Docente docente = new Docente(1L, "Docente", "Prueba", MAIL, "sub-1", true);
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
        when(docenteRepository.saveAndFlush(any(Docente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        doThrow(new ServicioNoDisponibleException("Keycloak caído"))
                .when(keycloakAdminClient).deshabilitarUsuario("sub-1");

        assertThrows(ServicioNoDisponibleException.class, () -> docenteService.darDeBaja(1L));
    }
}
