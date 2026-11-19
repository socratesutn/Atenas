package ar.edu.utn.frc.atenas.documentacion.exception;

import ar.edu.utn.frc.atenas.commons.error.ErrorDominioException;

/**
 * La respuesta de un servicio externo no pudo interpretarse.
 */
import org.springframework.http.HttpStatus;

public class RespuestaInesperadaException extends ErrorDominioException {
    public RespuestaInesperadaException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Respuesta inesperada", mensaje);
    }
}