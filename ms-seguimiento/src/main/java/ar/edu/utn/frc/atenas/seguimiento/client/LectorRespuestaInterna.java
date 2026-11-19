package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

/**
 * Lectura de respuestas de los servicios internos con el tratamiento de fallas común.
 *
 * <p>Los errores HTTP (404, 5xx…) ya los traduce el manejador de estados de
 * {@code atenas-commons}. Acá se cubre lo que ese manejador no ve: un cuerpo que no se
 * puede interpretar o que viene vacío. Ambos casos se tratan como "servicio no
 * disponible" (503), igual que cualquier otra respuesta inesperada de un servicio propio.</p>
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
            throw new ServicioNoDisponibleException("El servicio " + servicio + " respondió sin contenido");
        }
        return cuerpo;
    }
}
