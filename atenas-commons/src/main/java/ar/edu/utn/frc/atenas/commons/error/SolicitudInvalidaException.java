package ar.edu.utn.frc.atenas.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Error para solicitudes con datos inválidos o de formato incorrecto (400).
 */
public class SolicitudInvalidaException extends ErrorDominioException {

    public SolicitudInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "Solicitud inválida", mensaje);
    }

    public SolicitudInvalidaException(String mensaje, String regla) {
        super(HttpStatus.BAD_REQUEST, "Solicitud inválida", mensaje, regla);
    }
}
