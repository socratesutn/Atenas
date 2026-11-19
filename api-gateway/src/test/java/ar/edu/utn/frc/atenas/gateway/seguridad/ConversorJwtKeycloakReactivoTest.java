package ar.edu.utn.frc.atenas.gateway.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversorJwtKeycloakReactivoTest {

    private final ConversorJwtKeycloakReactivo conversor = new ConversorJwtKeycloakReactivo();

    @Test
    @DisplayName("Convierte los roles de realm_access a authorities con prefijo ROLE_")
    void deberiaConvertirLosRolesDelRealm_cuandoElJwtLosContiene() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "usuario")
                .claim("realm_access", Map.of("roles", List.of("JEFE_CATEDRA", "DOCENTE")))
                .build();

        AbstractAuthenticationToken autenticacion = conversor.convert(jwt).block();

        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_JEFE_CATEDRA", "ROLE_DOCENTE");
    }

    @Test
    @DisplayName("No asigna authorities cuando el JWT no trae realm_access")
    void deberiaNoAsignarAuthorities_cuandoElJwtNoTraeRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "usuario")
                .build();

        AbstractAuthenticationToken autenticacion = conversor.convert(jwt).block();

        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getAuthorities()).isEmpty();
    }
}
