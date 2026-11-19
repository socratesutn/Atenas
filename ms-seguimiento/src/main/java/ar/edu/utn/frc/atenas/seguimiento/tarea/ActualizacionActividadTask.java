package ar.edu.utn.frc.atenas.seguimiento.tarea;

import ar.edu.utn.frc.atenas.commons.error.ErrorDominioException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.config.ProveedorTokenServicio;
import ar.edu.utn.frc.atenas.seguimiento.service.ActualizacionActividadService;
import ar.edu.utn.frc.atenas.seguimiento.service.ResultadoActualizacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tarea nocturna de actualización de actividad (F12, RN31).
 *
 * <p>Se dispara con el cron configurable {@code atenas.actividad.cron} en zona
 * {@code America/Argentina/Cordoba} (02:00 por defecto). Obtiene un token de servicio por
 * Client Credentials y lo coloca en el {@code SecurityContext} durante la corrida para que
 * los clientes de I01 e I06 lo propaguen como {@code Authorization: Bearer}. Al terminar
 * restaura el contexto anterior: el hilo del planificador se reutiliza.</p>
 *
 * <p>Nunca propaga una falla de GitHub o de un servicio interno: se registra en el log y se
 * continúa con los demás equipos (RN28).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActualizacionActividadTask {

    private final ActualizacionActividadService servicio;
    private final ProveedorTokenServicio proveedorToken;
    private final JwtDecoder decodificador;

    @Scheduled(cron = "${atenas.actividad.cron:0 0 2 * * *}", zone = ZonaHoraria.ZONA_POR_DEFECTO)
    public void actualizarActividadNocturna() {
        try {
            Jwt jwt = decodificador.decode(proveedorToken.obtener());
            Authentication anterior = SecurityContextHolder.getContext().getAuthentication();
            try {
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                        jwt, List.of(new SimpleGrantedAuthority(Roles.PREFIJO_ROL + Roles.SERVICIO))));
                List<ResultadoActualizacion> resultados = servicio.actualizarTodos();
                log.info("Tarea nocturna de actividad: {} repositorios procesados", resultados.size());
            } finally {
                SecurityContextHolder.getContext().setAuthentication(anterior);
            }
        } catch (ErrorDominioException | JwtException falla) {
            log.warn("La tarea nocturna de actividad no pudo completarse: {}", falla.getMessage());
        }
    }
}
