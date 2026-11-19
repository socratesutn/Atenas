package ar.edu.utn.frc.atenas.catedra.client;

import ar.edu.utn.frc.atenas.catedra.config.KeycloakProperties;
import ar.edu.utn.frc.atenas.catedra.dto.externo.CredencialKeycloak;
import ar.edu.utn.frc.atenas.catedra.dto.externo.RolKeycloak;
import ar.edu.utn.frc.atenas.catedra.dto.externo.UsuarioKeycloakRequest;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Implementación del Adapter de Keycloak. Obtiene un token de servicio con
 * Client Credentials y lo reutiliza hasta su vencimiento. Traduce los errores
 * de Keycloak a las excepciones de dominio de atenas-commons.
 */
@Component
public class KeycloakAdminClientImpl implements KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClientImpl.class);
    private static final String REGISTRATION_ID = "keycloak";
    private static final long MARGEN_TOKEN_SEGUNDOS = 30;

    private final RestClient restClient;
    private final ClientRegistration registration;
    private final String baseUrl;

    private volatile TokenKeycloak token;

    public KeycloakAdminClientImpl(
            @Qualifier("keycloakRestClientBuilder") RestClient.Builder restClientBuilder,
            ClientRegistrationRepository clientRegistrationRepository,
            KeycloakProperties propiedades) {
        this.restClient = restClientBuilder.build();
        this.registration = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
        if (this.registration == null) {
            throw new IllegalStateException("Falta la configuración del cliente Keycloak " + REGISTRATION_ID);
        }

        String url = propiedades.getAdmin().getBaseUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Falta atenas.keycloak.admin.base-url");
        }
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public String crearUsuario(String mail, String nombre, String apellido, String claveTemporal) {
        UsuarioKeycloakRequest request = new UsuarioKeycloakRequest(
                mail,
                mail,
                nombre,
                apellido,
                true,
                true,
                List.of(new CredencialKeycloak("password", claveTemporal, true)),
                List.of("UPDATE_PASSWORD"));

        try {
            var respuesta = restClient.post()
                    .uri(baseUrl + "/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            String ubicacion = respuesta.getHeaders().getFirst(HttpHeaders.LOCATION);
            if (ubicacion == null || ubicacion.isBlank()) {
                throw new ServicioNoDisponibleException("Keycloak no devolvió el identificador del usuario creado");
            }

            log.info("Usuario creado en Keycloak mail={}", mail);
            return ubicacion.substring(ubicacion.lastIndexOf('/') + 1);
        } catch (RestClientResponseException excepcion) {
            if (excepcion.getStatusCode().value() == 409) {
                throw new ConflictoException("Ya existe un usuario con el mail " + mail);
            }
            log.warn("Keycloak rechazó el alta del usuario {} con estado {}", mail,
                    excepcion.getStatusCode().value());
            throw new ServicioNoDisponibleException("No se pudo crear el usuario en Keycloak");
        } catch (RestClientException excepcion) {
            throw new ServicioNoDisponibleException("No se pudo conectar con Keycloak");
        }
    }

    @Override
    public void asignarRolRealm(String sub, String rol) {
        try {
            RolKeycloak rolKeycloak = restClient.get()
                    .uri(baseUrl + "/roles/" + rol)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerToken())
                    .retrieve()
                    .body(RolKeycloak.class);

            if (rolKeycloak == null || rolKeycloak.id() == null) {
                throw new ServicioNoDisponibleException("Keycloak no devolvió el rol " + rol);
            }

            restClient.post()
                    .uri(baseUrl + "/users/" + sub + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(rolKeycloak))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Rol {} asignado en Keycloak sub={}", rol, sub);
        } catch (RestClientResponseException excepcion) {
            log.warn("Keycloak rechazó la asignación de rol {} a {} con estado {}", rol, sub,
                    excepcion.getStatusCode().value());
            throw new ServicioNoDisponibleException("No se pudo asignar el rol en Keycloak");
        } catch (RestClientException excepcion) {
            throw new ServicioNoDisponibleException("No se pudo conectar con Keycloak");
        }
    }

    @Override
    public void deshabilitarUsuario(String sub) {
        try {
            restClient.put()
                    .uri(baseUrl + "/users/" + sub)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", false))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Usuario deshabilitado en Keycloak sub={}", sub);
        } catch (RestClientResponseException excepcion) {
            log.warn("Keycloak rechazó la baja de {} con estado {}", sub, excepcion.getStatusCode().value());
            throw new ServicioNoDisponibleException("No se pudo deshabilitar el usuario en Keycloak");
        } catch (RestClientException excepcion) {
            throw new ServicioNoDisponibleException("No se pudo conectar con Keycloak");
        }
    }

    private synchronized String obtenerToken() {
        TokenKeycloak actual = token;
        if (actual != null && Instant.now().isBefore(actual.expiraEn())) {
            return actual.accessToken();
        }

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "client_credentials");
        formulario.add("client_id", registration.getClientId());
        formulario.add("client_secret", registration.getClientSecret());

        try {
            RespuestaTokenKeycloak respuesta = restClient.post()
                    .uri(registration.getProviderDetails().getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(RespuestaTokenKeycloak.class);

            if (respuesta == null || respuesta.accessToken() == null || respuesta.accessToken().isBlank()) {
                throw new ServicioNoDisponibleException("Keycloak no devolvió un token de servicio");
            }

            long segundos = respuesta.expiraEn() != null ? respuesta.expiraEn() : 60;
            Instant expiracion = Instant.now().plusSeconds(Math.max(segundos - MARGEN_TOKEN_SEGUNDOS, 1));
            token = new TokenKeycloak(respuesta.accessToken(), expiracion);
            return respuesta.accessToken();
        } catch (RestClientException excepcion) {
            throw new ServicioNoDisponibleException("No se pudo obtener el token de servicio de Keycloak");
        }
    }

    private record TokenKeycloak(String accessToken, Instant expiraEn) {
    }

    private record RespuestaTokenKeycloak(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiraEn) {
    }
}
