package ar.edu.utn.frc.atenas.gateway.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import static ar.edu.utn.frc.atenas.gateway.seguridad.Roles.ALUMNO;
import static ar.edu.utn.frc.atenas.gateway.seguridad.Roles.DOCENTE;
import static ar.edu.utn.frc.atenas.gateway.seguridad.Roles.JEFE_CATEDRA;

@Configuration
@EnableWebFluxSecurity
public class SeguridadConfig {

    @Bean
    public SecurityWebFilterChain cadenaSeguridad(
            ServerHttpSecurity http,
            ConversorJwtKeycloakReactivo conversor,
            ManejadorNoAutenticado manejadorNoAutenticado,
            ManejadorAccesoDenegado manejadorAccesoDenegado) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(autorizacion -> autorizacion
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/api/*/v3/api-docs",
                                "/api/*/v3/api-docs/**",
                                "/api/*/swagger-ui.html",
                                "/api/*/swagger-ui/**")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST, "/api/catedra/cursos-lectivos").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.GET, "/api/catedra/cursos-lectivos").hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .pathMatchers(HttpMethod.POST, "/api/catedra/comisiones").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.GET, "/api/catedra/comisiones").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.POST, "/api/catedra/cursos-lectivos/*/comisiones-lectivas").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.GET, "/api/catedra/cursos-lectivos/*/comisiones-lectivas").hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .pathMatchers(HttpMethod.POST, "/api/catedra/docentes").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.GET, "/api/catedra/docentes").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.PATCH, "/api/catedra/docentes/*/baja").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.POST, "/api/catedra/comisiones-lectivas/*/docentes").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.POST, "/api/catedra/estudiantes").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.GET, "/api/catedra/comisiones-lectivas/*/estudiantes").hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .pathMatchers(HttpMethod.PATCH, "/api/catedra/estudiantes/*/baja").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.GET, "/api/catedra/mi-perfil").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.PUT, "/api/catedra/mi-perfil/usuario-github").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.POST, "/api/catedra/comisiones-lectivas/*/equipos").hasRole(DOCENTE)
                        .pathMatchers(HttpMethod.GET, "/api/catedra/comisiones-lectivas/*/equipos").hasAnyRole(JEFE_CATEDRA, DOCENTE, ALUMNO)
                        .pathMatchers(HttpMethod.GET, "/api/catedra/equipos/*").hasAnyRole(JEFE_CATEDRA, DOCENTE, ALUMNO)
                        .pathMatchers(HttpMethod.POST, "/api/catedra/equipos/*/integrantes").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.DELETE, "/api/catedra/equipos/*/integrantes/*").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.PATCH, "/api/catedra/equipos/*/aseguramiento").hasAnyRole(ALUMNO, DOCENTE)
                        .pathMatchers(HttpMethod.PATCH, "/api/catedra/equipos/*/reapertura").hasRole(DOCENTE)

                        .pathMatchers(HttpMethod.POST, "/api/documentacion/hitos").hasRole(JEFE_CATEDRA)
                        .pathMatchers(HttpMethod.GET, "/api/documentacion/hitos").hasAnyRole(JEFE_CATEDRA, DOCENTE, ALUMNO)
                        .pathMatchers(HttpMethod.POST, "/api/documentacion/hitos/*/entregas").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.GET, "/api/documentacion/entregas").hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .pathMatchers(HttpMethod.GET, "/api/documentacion/mi-equipo/entregas").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.GET, "/api/documentacion/entregas/*").hasRole(DOCENTE)
                        .pathMatchers(HttpMethod.PATCH, "/api/documentacion/entregas/*/correccion").hasRole(DOCENTE)

                        .pathMatchers(HttpMethod.POST, "/api/seguimiento/comisiones-lectivas/*/eventos").hasRole(DOCENTE)
                        .pathMatchers(HttpMethod.GET, "/api/seguimiento/comisiones-lectivas/*/eventos").hasAnyRole(JEFE_CATEDRA, DOCENTE, ALUMNO)
                        .pathMatchers(HttpMethod.PUT, "/api/seguimiento/eventos/*/asistencias/*").hasRole(DOCENTE)
                        .pathMatchers(HttpMethod.PUT, "/api/seguimiento/eventos/*/equipos/*/asistencias").hasRole(DOCENTE)
                        .pathMatchers(HttpMethod.GET, "/api/seguimiento/eventos/*/asistencias").hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .pathMatchers(HttpMethod.GET, "/api/seguimiento/mi-asistencia").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.PUT, "/api/seguimiento/equipos/*/repositorio").hasRole(ALUMNO)
                        .pathMatchers(HttpMethod.POST, "/api/seguimiento/equipos/*/actividad/actualizacion").hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .pathMatchers(HttpMethod.GET, "/api/seguimiento/dashboard/equipos").hasAnyRole(JEFE_CATEDRA, DOCENTE)
                        .pathMatchers(HttpMethod.GET, "/api/seguimiento/dashboard/equipos/*").hasAnyRole(JEFE_CATEDRA, DOCENTE)

                        .anyExchange().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor)))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(manejadorNoAutenticado)
                        .accessDeniedHandler(manejadorAccesoDenegado))
                .build();
    }
}
