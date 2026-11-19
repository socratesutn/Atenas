package ar.edu.utn.frc.atenas.documentacion.exception;

import ar.edu.utn.frc.atenas.commons.error.ErrorDominioException;
import org.springframework.http.HttpStatus;

/**
 * El archivo de Drive no existe o no es accesible públicamente.
 *
 * Para D03 se traduce a 422; para D06 se degrada a NO_VERIFICADO.
 */
public class ArchivoNoAccesibleException extends ErrorDominioException {

    public ArchivoNoAccesibleException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Archivo no accesible", mensaje);
    }
}
