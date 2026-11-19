package ar.edu.utn.frc.atenas.seguimiento.config;

import ar.edu.utn.frc.atenas.commons.seguridad.ConversorJwtKeycloak;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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
                                "/api/seguimiento/v3/api-docs",
                                "/api/seguimiento/v3/api-docs/**",
                                "/api/seguimiento/swagger-ui.html",
                                "/api/seguimiento/swagger-ui/**",
                                "/webjars/**")
                        .permitAll()
                        // S07: el Gateway ya filtra por rol; se vuelve a validar acá (defensa en profundidad).
                        .requestMatchers(HttpMethod.PUT, "/api/seguimiento/equipos/*/repositorio")
                        .hasRole(Roles.ALUMNO)
                        // S01 a S06: calendario y asistencia (mismas reglas de rol que el Gateway).
                        .requestMatchers(HttpMethod.POST, "/api/seguimiento/comisiones-lectivas/*/eventos")
                        .hasRole(Roles.DOCENTE)
                        .requestMatchers(HttpMethod.GET, "/api/seguimiento/comisiones-lectivas/*/eventos")
                        .hasAnyRole(Roles.JEFE_CATEDRA, Roles.DOCENTE, Roles.ALUMNO)
                        .requestMatchers(HttpMethod.PUT, "/api/seguimiento/eventos/*/asistencias/*",
                                "/api/seguimiento/eventos/*/equipos/*/asistencias")
                        .hasRole(Roles.DOCENTE)
                        .requestMatchers(HttpMethod.GET, "/api/seguimiento/eventos/*/asistencias")
                        .hasAnyRole(Roles.JEFE_CATEDRA, Roles.DOCENTE)
                        .requestMatchers(HttpMethod.GET, "/api/seguimiento/mi-asistencia")
                        .hasRole(Roles.ALUMNO)
                        // S08: el servicio además valida que el docente pertenezca a la comisión (I04).
                        .requestMatchers(HttpMethod.POST, "/api/seguimiento/equipos/*/actividad/actualizacion")
                        .hasAnyRole(Roles.JEFE_CATEDRA, Roles.DOCENTE)
                        // S09 y S10: el servicio valida la comisión y reserva el filtro por curso al jefe.
                        .requestMatchers(HttpMethod.GET, "/api/seguimiento/dashboard/equipos",
                                "/api/seguimiento/dashboard/equipos/*")
                        .hasAnyRole(Roles.JEFE_CATEDRA, Roles.DOCENTE)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwtKeycloak)));

        return http.build();
    }
}
