package ar.edu.utn.frc.atenas.documentacion.config;

import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static ar.edu.utn.frc.atenas.commons.seguridad.Roles.ALUMNO;
import static ar.edu.utn.frc.atenas.commons.seguridad.Roles.DOCENTE;
import static ar.edu.utn.frc.atenas.commons.seguridad.Roles.JEFE_CATEDRA;
import static ar.edu.utn.frc.atenas.commons.seguridad.Roles.SERVICIO;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ConversorJwtKeycloak conversorJwtKeycloak;

    public SecurityConfig(ConversorJwtKeycloak conversorJwtKeycloak) {
        this.conversorJwtKeycloak = conversorJwtKeycloak;
    }

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/api/documentacion/v3/api-docs",
                                "/api/documentacion/v3/api-docs/**",
                                "/api/documentacion/swagger-ui.html",
                                "/api/documentacion/swagger-ui/**",
                                "/webjars/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/documentacion/hitos")
                        .hasRole(JEFE_CATEDRA)
                        .requestMatchers(HttpMethod.GET, "/api/documentacion/hitos")
                        .hasAnyRole(JEFE_CATEDRA, DOCENTE, ALUMNO)
                        .requestMatchers(HttpMethod.POST, "/api/documentacion/hitos/*/entregas")
                        .hasRole(ALUMNO)
                        .requestMatchers(HttpMethod.GET, "/internal/hitos")
                        .hasAnyRole(JEFE_CATEDRA, DOCENTE, SERVICIO)
                        .requestMatchers(HttpMethod.GET, "/api/documentacion/entregas")
                        .hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .requestMatchers(HttpMethod.GET, "/api/documentacion/mi-equipo/entregas")
                        .hasRole(ALUMNO)
                        .requestMatchers(HttpMethod.GET, "/api/documentacion/entregas/*")
                        .hasRole(DOCENTE)
                        .requestMatchers(HttpMethod.PATCH, "/api/documentacion/entregas/*/correccion")
                        .hasRole(DOCENTE)
                        .requestMatchers(HttpMethod.GET, "/internal/entregas")
                        .hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwtKeycloak)));

        return http.build();
    }
}
