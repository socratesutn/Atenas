package ar.edu.utn.frc.atenas.commons.traza;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Toma el {@code X-Correlation-Id} recibido o genera uno nuevo, lo deja
 * disponible en el MDC para los logs y lo devuelve en la respuesta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Correlation-Id";
    public static final String NOMBRE_MDC = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String correlationId = solicitud.getHeader(CABECERA);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(NOMBRE_MDC, correlationId);
        respuesta.setHeader(CABECERA, correlationId);
        try {
            cadena.doFilter(solicitud, respuesta);
        } finally {
            MDC.remove(NOMBRE_MDC);
        }
    }
}
