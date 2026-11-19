package ar.edu.utn.frc.atenas.gateway.seguridad;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ConversorJwtKeycloakReactivo implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        return Mono.just(new JwtAuthenticationToken(jwt, autoridades(jwt)));
    }

    private Collection<GrantedAuthority> autoridades(Jwt jwt) {
        Map<String, Object> accesoAlRealm = jwt.getClaim("realm_access");
        if (accesoAlRealm == null) {
            return List.of();
        }

        Object roles = accesoAlRealm.get("roles");
        if (!(roles instanceof Collection<?> coleccion)) {
            return List.of();
        }

        return coleccion.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .distinct()
                .<GrantedAuthority>map(rol -> new SimpleGrantedAuthority(Roles.PREFIJO + rol))
                .toList();
    }
}
