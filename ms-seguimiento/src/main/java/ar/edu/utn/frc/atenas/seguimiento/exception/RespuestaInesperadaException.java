package ar.edu.utn.frc.atenas.seguimiento.exception;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;

/**
 * GitHub respondió, pero con algo que no se puede interpretar (JSON distinto
 * del esperado, campos obligatorios ausentes, fecha inválida, etc.).
 *
 * <p>Extiende {@link ServicioNoDisponibleException} a propósito: si algún
 * flujo no la trata de forma especial (por ejemplo S07, vincular repositorio),
 * igual termina en 503. En la actualización de actividad (RN28) el service
 * debe capturarla <b>antes</b> que a su clase padre, porque no es lo mismo
 * "no responde" (último snapshot pasa a DESACTUALIZADO) que "respondió algo
 * raro" (la evidencia queda NO_DISPONIBLE).</p>
 */
public class RespuestaInesperadaException extends ServicioNoDisponibleException {

    public RespuestaInesperadaException(String mensaje) {
        super(mensaje, "RN-28");
    }
}
