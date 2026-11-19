package ar.edu.utn.frc.atenas.catedra.client;

import ar.edu.utn.frc.atenas.catedra.config.KeycloakProperties;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminClientTest {

    private static final String TOKEN_URI = "http://localhost:8081/realms/atenas/protocol/openid-connect/token";
    private static final String BASE_URL = "http://localhost:8081/admin/realms/atenas";

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private KeycloakAdminClientImpl cliente;

    @BeforeEach
    void preparar() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("atenas-catedra-admin")
                .clientSecret("secreto-test")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(TOKEN_URI)
                .build();
        when(clientRegistrationRepository.findByRegistrationId("keycloak")).thenReturn(registration);

        KeycloakProperties propiedades = new KeycloakProperties();
        propiedades.setClaveTemporal("clave-temporal-test");
        propiedades.getAdmin().setBaseUrl(BASE_URL);

        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        cliente = new KeycloakAdminClientImpl(builder, clientRegistrationRepository, propiedades);
    }

    @Test
    @DisplayName("Crea el usuario y devuelve el sub del Location")
    void deberiaCrearUsuarioYDevolverSub_cuandoKeycloakResponde201() {
        esperarToken();
        server.expect(once(), requestTo(BASE_URL + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-servicio"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .header("Location", BASE_URL + "/users/sub-123"));

        String sub = cliente.crearUsuario("docente@atenas.local", "Docente", "Prueba", "clave");

        assertEquals("sub-123", sub);
        server.verify();
    }

    @Test
    @DisplayName("Traduce el 409 de Keycloak a conflicto")
    void deberiaLanzarConflicto_cuandoElUsuarioYaExiste() {
        esperarToken();
        server.expect(once(), requestTo(BASE_URL + "/users"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(ConflictoException.class,
                () -> cliente.crearUsuario("docente@atenas.local", "Docente", "Prueba", "clave"));
        server.verify();
    }

    @Test
    @DisplayName("Traduce un 5xx de Keycloak a servicio no disponible")
    void deberiaLanzarServicioNoDisponible_cuandoKeycloakResponde5xx() {
        esperarToken();
        server.expect(once(), requestTo(BASE_URL + "/users"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class,
                () -> cliente.crearUsuario("docente@atenas.local", "Docente", "Prueba", "clave"));
        server.verify();
    }

    @Test
    @DisplayName("Traduce un timeout a servicio no disponible")
    void deberiaLanzarServicioNoDisponible_cuandoHayTimeout() {
        esperarToken();
        server.expect(once(), requestTo(BASE_URL + "/users"))
                .andRespond(withException(new SocketTimeoutException("timeout")));

        assertThrows(ServicioNoDisponibleException.class,
                () -> cliente.crearUsuario("docente@atenas.local", "Docente", "Prueba", "clave"));
        server.verify();
    }

    @Test
    @DisplayName("Asigna el rol de realm después de obtenerlo")
    void deberiaAsignarRolRealm_cuandoKeycloakResponde() {
        esperarToken();
        server.expect(once(), requestTo(BASE_URL + "/roles/DOCENTE"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":\"rol-id\",\"name\":\"DOCENTE\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE_URL + "/users/sub-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        cliente.asignarRolRealm("sub-123", "DOCENTE");

        server.verify();
    }

    @Test
    @DisplayName("Deshabilita el usuario en Keycloak")
    void deberiaDeshabilitarUsuario_cuandoKeycloakResponde204() {
        esperarToken();
        server.expect(once(), requestTo(BASE_URL + "/users/sub-123"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        cliente.deshabilitarUsuario("sub-123");

        server.verify();
    }

    private void esperarToken() {
        server.expect(once(), requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"token-servicio\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON));
    }
}
