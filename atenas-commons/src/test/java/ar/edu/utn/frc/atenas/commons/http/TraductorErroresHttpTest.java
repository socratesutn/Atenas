package ar.edu.utn.frc.atenas.commons.http;

import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TraductorErroresHttpTest {

    private MockRestServiceServer servidor;
    private RestClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = new ConfiguracionHttp()
                .restClientBuilder(new AtenasHttpProperties());
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = constructor.build();
    }

    @Test
    @DisplayName("Traduce un 404 del servicio consultado a NoEncontradoException")
    void deberiaTraducir404_cuandoElRecursoNoExiste() {
        servidor.expect(requestTo("http://localhost/recurso"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> cliente.get().uri("http://localhost/recurso")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Traduce un 500 del servicio consultado a ServicioNoDisponibleException")
    void deberiaTraducir500_cuandoElServicioFalla() {
        servidor.expect(requestTo("http://localhost/recurso"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> cliente.get().uri("http://localhost/recurso")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Traduce otro 4xx inesperado a ServicioNoDisponibleException")
    void deberiaTraducir4xxInesperado_cuandoNoEs404() {
        servidor.expect(requestTo("http://localhost/recurso"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> cliente.get().uri("http://localhost/recurso")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Traduce un timeout de lectura a ServicioNoDisponibleException")
    void deberiaTraducirTimeout_cuandoElServicioNoResponde() {
        servidor.expect(requestTo("http://localhost/recurso"))
                .andRespond(withException(new SocketTimeoutException("timeout")));

        assertThatThrownBy(() -> cliente.get().uri("http://localhost/recurso")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
