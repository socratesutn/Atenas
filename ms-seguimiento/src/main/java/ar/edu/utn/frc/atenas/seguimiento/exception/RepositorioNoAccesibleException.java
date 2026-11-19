package ar.edu.utn.frc.atenas.seguimiento.exception;

import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;

/**
 * El repositorio no existe o no es público (RN25). Se responde 422: el dato
 * (la URL) está bien formado, pero el sistema no puede aceptarlo.
 */
public class RepositorioNoAccesibleException extends NoProcesableException {

    public RepositorioNoAccesibleException(String mensaje) {
        super(mensaje, "RN-25");
    }
}
