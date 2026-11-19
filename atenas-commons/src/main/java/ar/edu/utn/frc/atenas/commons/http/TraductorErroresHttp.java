package ar.edu.utn.frc.atenas.commons.http;

import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Traduce las respuestas de error de un servicio consultado:
 * {@code 404} a {@link NoEncontradoException} y cualquier otro error
 * (5xx u otro 4xx inesperado) a {@link ServicioNoDisponibleException}.
 */
public final class TraductorErroresHttp {

    private TraductorErroresHttp() {
    }

    public static void evaluar(ClientHttpResponse respuesta) throws IOException {
        int codigo = respuesta.getStatusCode().value();
        if (codigo == 404) {
            throw new NoEncontradoException("El recurso solicitado no existe en el servicio consultado");
        }
        throw new ServicioNoDisponibleException("El servicio consultado no está disponible");
    }
}
