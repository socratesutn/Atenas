package ar.edu.utn.frc.atenas.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Error para un rol o una regla de dominio no cumplida (403).
 */
public class AccesoDenegadoException extends ErrorDominioException {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, "Acceso denegado", mensaje);
    }

    public AccesoDenegadoException(String mensaje, String regla) {
        super(HttpStatus.FORBIDDEN, "Acceso denegado", mensaje, regla);
    }
}
