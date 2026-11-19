package ar.edu.utn.frc.atenas.commons.seguridad;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Convierte los roles del realm de Keycloak ({@code realm_access.roles}) en
 * autoridades de Spring Security con el prefijo {@code ROLE_}.
 */
@Component
public class ConversorJwtKeycloak implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLAIM_ACCESO_REALM = "realm_access";
    private static final String CLAIM_ROLES = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, autoridades(jwt));
    }

    private Collection<GrantedAuthority> autoridades(Jwt jwt) {
        Map<String, Object> accesoRealm = jwt.getClaimAsMap(CLAIM_ACCESO_REALM);
        if (accesoRealm == null) {
            return List.of();
        }

        Object roles = accesoRealm.get(CLAIM_ROLES);
        if (!(roles instanceof Collection<?> coleccion)) {
            return List.of();
        }

        return coleccion.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(rol -> !rol.isBlank())
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(Roles.PREFIJO_ROL + rol))
                .toList();
    }
}
