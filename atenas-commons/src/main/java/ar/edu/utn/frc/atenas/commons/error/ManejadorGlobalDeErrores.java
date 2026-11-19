package ar.edu.utn.frc.atenas.commons.error;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.commons.traza.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

/**
 * Traduce las excepciones de la aplicación a {@code ProblemDetail} (RFC 7807)
 * con timestamp en la zona de Argentina, identificador de regla opcional,
 * correlation-id y errores de validación.
 */
@RestControllerAdvice
public class ManejadorGlobalDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalDeErrores.class);

    private static final String TITULO_ERROR_INTERNO = "Error interno del servidor";
    private static final String DETALLE_ERROR_INTERNO = "Se produjo un error inesperado";
    private static final String DETALLE_VALIDACION = "La solicitud contiene datos inválidos";
    private static final String DETALLE_ACCESO_DENEGADO = "No tiene permiso para realizar la operación";

    private final ZonaHoraria zonaHoraria;

    public ManejadorGlobalDeErrores(ZonaHoraria zonaHoraria) {
        this.zonaHoraria = zonaHoraria;
    }

    @ExceptionHandler(ErrorDominioException.class)
    public ResponseEntity<ProblemDetail> manejarErrorDeDominio(ErrorDominioException excepcion,
                                                                HttpServletRequest solicitud) {
        ProblemDetail problema = problema(excepcion.getStatus(), excepcion.getTitulo(),
                excepcion.getMessage(), solicitud);
        if (excepcion.getRegla() != null && !excepcion.getRegla().isBlank()) {
            problema.setProperty("regla", excepcion.getRegla());
        }
        return ResponseEntity.status(excepcion.getStatus()).body(problema);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacion(MethodArgumentNotValidException excepcion,
                                                           HttpServletRequest solicitud) {
        List<ErrorValidacion> errores = excepcion.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new ErrorValidacion(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                DETALLE_VALIDACION, solicitud);
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> manejarAccesoDenegado(AccessDeniedException excepcion,
                                                               HttpServletRequest solicitud) {
        String detalle = excepcion.getMessage() != null && !excepcion.getMessage().isBlank()
                ? excepcion.getMessage()
                : DETALLE_ACCESO_DENEGADO;
        ProblemDetail problema = problema(HttpStatus.FORBIDDEN, "Acceso denegado", detalle, solicitud);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarErrorNoPrevisto(Exception excepcion,
                                                                HttpServletRequest solicitud) {
        log.error("Error no previsto al procesar {} {}", solicitud.getMethod(), solicitud.getRequestURI(),
                excepcion);
        ProblemDetail problema = problema(HttpStatus.INTERNAL_SERVER_ERROR, TITULO_ERROR_INTERNO,
                DETALLE_ERROR_INTERNO, solicitud);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema);
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalle,
                                   HttpServletRequest solicitud) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(solicitud.getRequestURI()));
        problema.setProperty("timestamp", zonaHoraria.ahora().toString());

        String correlationId = MDC.get(CorrelationIdFilter.NOMBRE_MDC);
        if (correlationId != null && !correlationId.isBlank()) {
            problema.setProperty("correlationId", correlationId);
        }
        return problema;
    }
}
