package ar.edu.utn.frc.atenas.commons.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversorJwtKeycloakTest {

    private final ConversorJwtKeycloak conversor = new ConversorJwtKeycloak();

    @Test
    @DisplayName("Convierte los roles del realm en autoridades con prefijo ROLE_")
    void deberiaConvertirRolesDelRealm_cuandoElJwtTraeRealmAccess() {
        Jwt jwt = jwtConRoles(List.of("JEFE_CATEDRA", "DOCENTE"));

        AbstractAuthenticationToken autenticacion = conversor.convert(jwt);

        assertThat(autenticacion.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_JEFE_CATEDRA", "ROLE_DOCENTE");
        assertThat(autenticacion.getName()).isEqualTo("usuario-1");
    }

    @Test
    @DisplayName("No genera autoridades cuando el JWT no trae realm_access")
    void deberiaDevolverSinAutoridades_cuandoNoHayRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("usuario-1")
                .build();

        AbstractAuthenticationToken autenticacion = conversor.convert(jwt);

        assertThat(autenticacion.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Ignora roles vacíos y valores que no son texto")
    void deberiaIgnorarRolesInvalidos_cuandoLaColeccionTieneValoresVacios() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("usuario-1")
                .claim("realm_access", Map.of("roles", List.of("", "ALUMNO", 7)))
                .build();

        AbstractAuthenticationToken autenticacion = conversor.convert(jwt);

        assertThat(autenticacion.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ALUMNO");
    }

    private Jwt jwtConRoles(List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("usuario-1")
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }
}
