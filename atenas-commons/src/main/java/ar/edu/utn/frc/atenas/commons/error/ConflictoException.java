package ar.edu.utn.frc.atenas.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Error para un conflicto de estado o un duplicado (409).
 */
public class ConflictoException extends ErrorDominioException {

    public ConflictoException(String mensaje) {
        super(HttpStatus.CONFLICT, "Conflicto", mensaje);
    }

    public ConflictoException(String mensaje, String regla) {
        super(HttpStatus.CONFLICT, "Conflicto", mensaje, regla);
    }
}
