package ar.edu.utn.frc.atenas.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Error para un dato sintácticamente válido pero rechazado por el dominio (422).
 */
public class NoProcesableException extends ErrorDominioException {

    public NoProcesableException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Entidad no procesable", mensaje);
    }

    public NoProcesableException(String mensaje, String regla) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Entidad no procesable", mensaje, regla);
    }
}
