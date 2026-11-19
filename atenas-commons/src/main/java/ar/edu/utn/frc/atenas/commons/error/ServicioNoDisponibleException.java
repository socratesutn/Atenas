package ar.edu.utn.frc.atenas.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Error para una dependencia que no responde y no permite continuar (503).
 */
public class ServicioNoDisponibleException extends ErrorDominioException {

    public ServicioNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible", mensaje);
    }

    public ServicioNoDisponibleException(String mensaje, String regla) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible", mensaje, regla);
    }
}
