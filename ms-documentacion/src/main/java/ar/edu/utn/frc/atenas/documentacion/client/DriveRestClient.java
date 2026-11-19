package ar.edu.utn.frc.atenas.documentacion.client;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.http.AtenasHttpProperties;
import ar.edu.utn.frc.atenas.documentacion.config.DriveProperties;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.ArchivoDriveDto;
import ar.edu.utn.frc.atenas.documentacion.exception.ArchivoNoAccesibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementación de {@link DriveClient} sobre Google Drive API v3.
 *
 * Usa los timeouts de commons pero no el interceptor de JWT: el token de
 * Keycloak no debe enviarse a Google. La API key viaja en la query.
 */
@Component
public class DriveRestClient implements DriveClient {

    private static final String SERVICIO = "Google Drive";

    private static final Pattern[] PATRONES = new Pattern[]{
            Pattern.compile("drive\\.google\\.com/file/d/([^/]+)"),
            Pattern.compile("docs\\.google\\.com/document/d/([^/]+)"),
            Pattern.compile("open\\?id=([^&]+)")
    };

    private final RestClient restClient;
    private final DriveProperties propiedades;

    @Autowired
    public DriveRestClient(AtenasHttpProperties httpProperties, DriveProperties propiedades) {
        this(construirCliente(httpProperties, propiedades), propiedades);
    }

    /** Constructor para tests: permite inyectar un cliente contra un servidor simulado. */
    DriveRestClient(RestClient restClient, DriveProperties propiedades) {
        this.restClient = restClient;
        this.propiedades = propiedades;
    }

    @Override
    public String extraerFileId(String enlace) {
        if (enlace == null || enlace.isBlank()) {
            throw new SolicitudInvalidaException("El enlace de Drive es obligatorio");
        }
        for (Pattern patron : PATRONES) {
            Matcher matcher = patron.matcher(enlace);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new SolicitudInvalidaException("No se pudo reconocer el fileId del enlace");
    }

    @Override
    public ArchivoDrive obtenerArchivo(String driveFileId) {
        try {
            ArchivoDriveDto respuesta = restClient.get()
                    .uri(uri -> uri.path("/files/{id}")
                            .queryParam("fields", "id,name,mimeType,modifiedTime")
                            .queryParam("key", propiedades.getApiKey())
                            .build(driveFileId))
                    .retrieve()
                    .body(ArchivoDriveDto.class);

            if (respuesta == null || respuesta.modifiedTime() == null) {
                throw new ServicioNoDisponibleException(
                        SERVICIO + " devolvió una respuesta incompleta");
            }

            return new ArchivoDrive(
                    respuesta.id(),
                    respuesta.name(),
                    respuesta.mimeType(),
                    respuesta.modifiedTime());
        } catch (ArchivoNoAccesibleException | ServicioNoDisponibleException excepcion) {
            throw excepcion;
        } catch (RestClientException excepcion) {
            throw new ServicioNoDisponibleException(
                    "No se pudo consultar el archivo en " + SERVICIO);
        }
    }

    private static RestClient construirCliente(AtenasHttpProperties httpProperties,
                                               DriveProperties propiedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Math.toIntExact(
                httpProperties.getTimeoutConexion().toMillis()));
        fabrica.setReadTimeout(Math.toIntExact(
                httpProperties.getTimeoutLectura().toMillis()));

        return RestClient.builder()
                .baseUrl(propiedades.getUrlBase())
                .requestFactory(fabrica)
                .defaultStatusHandler(HttpStatusCode::isError, (solicitud, respuesta) -> {
                    int codigo = respuesta.getStatusCode().value();
                    String cuerpo = new String(respuesta.getBody().readAllBytes(),
                            StandardCharsets.UTF_8);
                    if (codigo == 404
                            || (codigo == 403
                            && (cuerpo.contains("notFound")
                            || cuerpo.toLowerCase().contains("permission")))) {
                        throw new ArchivoNoAccesibleException(
                                "El archivo de Drive no existe o no es público");
                    }
                    throw new ServicioNoDisponibleException(
                            "Google Drive no está disponible");
                })
                .build();
    }
}
