package ar.edu.utn.frc.atenas.documentacion.client;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

/**
 * Centraliza la lectura de respuestas de servicios internos.
 *
 * Los códigos HTTP de error ya los traduce atenas-commons. Acá se cubre lo que
 * ese manejador no ve: un cuerpo vacío o un JSON que no se puede interpretar.
 * Ambos casos se consideran una falla del servicio consultado (503).
 */
final class LectorRespuestaInterna {

    private LectorRespuestaInterna() {
    }

    static <T> T leer(Supplier<T> lectura, String servicio) {
        T cuerpo;
        try {
            cuerpo = lectura.get();
        } catch (RestClientException excepcion) {
            throw new ServicioNoDisponibleException(
                    "El servicio " + servicio + " devolvió una respuesta que no se pudo interpretar");
        }

        if (cuerpo == null) {
            throw new ServicioNoDisponibleException(
                    "El servicio " + servicio + " respondió sin contenido");
        }

        return cuerpo;
    }
}
