package ar.edu.utn.frc.atenas.commons.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Acceso al usuario autenticado en el request actual: {@code sub}, roles y
 * token. Los roles se devuelven sin el prefijo {@code ROLE_} para compararlos
 * con las constantes de {@link Roles}.
 */
@Component
public class UsuarioActual {

    public String sub() {
        Jwt jwt = jwt();
        return jwt != null ? jwt.getSubject() : null;
    }

    public Set<String> roles() {
        Set<String> roles = new LinkedHashSet<>();
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            return roles;
        }
        for (GrantedAuthority autoridad : autenticacion.getAuthorities()) {
            String nombre = autoridad.getAuthority();
            if (nombre != null && nombre.startsWith(Roles.PREFIJO_ROL)) {
                roles.add(nombre.substring(Roles.PREFIJO_ROL.length()));
            }
        }
        return roles;
    }

    public String token() {
        Jwt jwt = jwt();
        return jwt != null ? jwt.getTokenValue() : null;
    }

    public boolean autenticado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.isAuthenticated();
    }

    private Jwt jwt() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken tokenJwt) {
            return tokenJwt.getToken();
        }
        return null;
    }

    public boolean tieneRol(String rol) {
        return roles().contains(rol);
    }
}
