package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.client.KeycloakAdminClient;
import ar.edu.utn.frc.atenas.catedra.dto.CrearDocenteRequest;
import ar.edu.utn.frc.atenas.catedra.entity.Docente;
import ar.edu.utn.frc.atenas.catedra.repository.DocenteRepository;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DocenteServiceIntegrationTest {

    private static final String MAIL = "integracion@atenas.local";

    @Autowired
    private DocenteService docenteService;

    @Autowired
    private DocenteRepository docenteRepository;

    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void limpiar() {
        docenteRepository.deleteAll();
    }

    @Test
    @DisplayName("RN-03: si Keycloak falla, la transacción no deja el docente guardado")
    void deberiaRevertirAlta_cuandoKeycloakFalla() {
        when(keycloakAdminClient.crearUsuario(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("Keycloak caído"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> docenteService.crear(new CrearDocenteRequest("Docente", "Integración", MAIL)));

        assertEquals(0, docenteRepository.count());
    }

    @Test
    @DisplayName("RN-05: si Keycloak falla en la baja, el docente sigue activo")
    void deberiaRevertirBaja_cuandoKeycloakFalla() {
        Docente nuevo = new Docente("Docente", "Integración", MAIL);
        nuevo.setSub("sub-integracion");
        Docente docente = docenteRepository.saveAndFlush(nuevo);
        doThrow(new ServicioNoDisponibleException("Keycloak caído"))
                .when(keycloakAdminClient).deshabilitarUsuario("sub-integracion");

        assertThrows(ServicioNoDisponibleException.class, () -> docenteService.darDeBaja(docente.getId()));

        Docente guardado = docenteRepository.findById(docente.getId()).orElseThrow();
        assertTrue(guardado.isActivo());
    }
}
