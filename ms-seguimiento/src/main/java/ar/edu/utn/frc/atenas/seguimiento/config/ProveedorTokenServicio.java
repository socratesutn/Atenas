package ar.edu.utn.frc.atenas.seguimiento.config;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Obtiene el token de servicio del cliente {@code atenas-seguimiento-job} (Client Credentials).
 *
 * <p>La tarea nocturna no tiene un usuario en el {@code SecurityContext}, así que no puede
 * apoyarse en el interceptor que propaga el JWT del request. Se pide el token directamente al
 * proveedor de Client Credentials con el registro {@code keycloak}, configurado en los perfiles
 * dev y docker. No se guarda el token: se solicita una vez por corrida y se usa de inmediato.</p>
 */
@Component
public class ProveedorTokenServicio {

    private static final String REGISTRO_KEYCLOAK = "keycloak";

    private final ClientRegistrationRepository registros;

    public ProveedorTokenServicio(ClientRegistrationRepository registros) {
        this.registros = registros;
    }

    /** Devuelve el access token de la cuenta de servicio con rol SERVICIO. */
    public String obtener() {
        ClientRegistration registro = registros.findByRegistrationId(REGISTRO_KEYCLOAK);
        if (registro == null) {
            throw new ServicioNoDisponibleException("No está configurado el cliente de servicio keycloak");
        }
        Authentication principal = new UsernamePasswordAuthenticationToken(
                "atenas-seguimiento-job", null, List.of());
        try {
            OAuth2AuthorizedClient autorizado = new ClientCredentialsOAuth2AuthorizedClientProvider()
                    .authorize(OAuth2AuthorizationContext.withClientRegistration(registro)
                            .principal(principal)
                            .build());
            if (autorizado == null || autorizado.getAccessToken() == null) {
                throw new ServicioNoDisponibleException("No se pudo obtener el token de servicio");
            }
            return autorizado.getAccessToken().getTokenValue();
        } catch (OAuth2AuthorizationException falla) {
            throw new ServicioNoDisponibleException("No se pudo obtener el token de servicio");
        }
    }
}
