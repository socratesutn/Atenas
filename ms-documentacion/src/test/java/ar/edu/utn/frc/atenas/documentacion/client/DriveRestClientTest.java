package ar.edu.utn.frc.atenas.documentacion.client;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.documentacion.config.DriveProperties;
import ar.edu.utn.frc.atenas.documentacion.exception.ArchivoNoAccesibleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Pruebas del adaptador de Google Drive con un servidor simulado. */
class DriveRestClientTest {

    private MockRestServiceServer servidor;
    private DriveRestClient cliente;

    @BeforeEach
    void preparar() {
        DriveProperties propiedades = new DriveProperties();
        propiedades.setApiKey("api-key");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://www.googleapis.com/drive/v3")
                .defaultStatusHandler(HttpStatusCode::isError, (solicitud, respuesta) -> {
                    if (respuesta.getStatusCode().value() == 404) {
                        throw new ArchivoNoAccesibleException("no accesible");
                    }
                    throw new ServicioNoDisponibleException("Drive no disponible");
                });
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new DriveRestClient(builder.build(), propiedades);
    }

    @AfterEach
    void limpiar() {
        servidor.verify();
    }

    @Test
    @DisplayName("Drive devuelve los metadatos y no recibe el JWT del usuario")
    void deberiaDevolverElArchivo_cuandoDriveResponde200() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/drive/v3/files/file-1", "key=api-key"))
                .andExpect(solicitud -> assertThat(
                        solicitud.getHeaders().containsKey("Authorization")).isFalse())
                .andRespond(withSuccess("""
                        {"id":"file-1","name":"informe.pdf",
                         "mimeType":"application/pdf","modifiedTime":"2026-04-01T12:00:00Z"}
                        """, MediaType.APPLICATION_JSON));

        ArchivoDrive archivo = cliente.obtenerArchivo("file-1");

        assertThat(archivo.id()).isEqualTo("file-1");
        assertThat(archivo.nombre()).isEqualTo("informe.pdf");
        assertThat(archivo.mimeType()).isEqualTo("application/pdf");
        assertThat(archivo.modifiedTime()).isNotNull();
    }

    @Test
    @DisplayName("Drive 404 se traduce a ArchivoNoAccesibleException")
    void deberiaLanzarArchivoNoAccesible_cuandoDriveResponde404() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ArchivoNoAccesibleException.class,
                () -> cliente.obtenerArchivo("file-1"));
    }

    @Test
    @DisplayName("Drive 5xx se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoDriveRespondeError() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class,
                () -> cliente.obtenerArchivo("file-1"));
    }

    @Test
    @DisplayName("Un timeout de Drive se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoDriveNoResponde() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(ServicioNoDisponibleException.class,
                () -> cliente.obtenerArchivo("file-1"));
    }

    private static RequestMatcher pedidoA(String ruta, String fragmentoQuery) {
        return solicitud -> {
            URI uri = solicitud.getURI();
            assertThat(uri.getRawPath()).isEqualTo(ruta);
            assertThat(uri.getQuery()).contains(fragmentoQuery);
        };
    }
}
