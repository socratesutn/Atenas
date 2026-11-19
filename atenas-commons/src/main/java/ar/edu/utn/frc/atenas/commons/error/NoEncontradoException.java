package ar.edu.utn.frc.atenas.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Error para un identificador que no existe (404).
 */
public class NoEncontradoException extends ErrorDominioException {

    public NoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, "No encontrado", mensaje);
    }

    public NoEncontradoException(String mensaje, String regla) {
        super(HttpStatus.NOT_FOUND, "No encontrado", mensaje, regla);
    }
}
