package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.http.AtenasHttpProperties;
import ar.edu.utn.frc.atenas.seguimiento.config.GitHubProperties;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.GitHubCommitDto;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.GitHubRepositorioDto;
import ar.edu.utn.frc.atenas.seguimiento.exception.RepositorioNoAccesibleException;
import ar.edu.utn.frc.atenas.seguimiento.exception.RespuestaInesperadaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementación de {@link GitHubClient} sobre {@link RestClient} (patrón Adapter).
 *
 * <h3>Decisiones que conviene poder explicar</h3>
 * <ul>
 *   <li><b>Cliente HTTP propio.</b> No se usa el {@code RestClient.Builder} de
 *       {@code atenas-commons}: su interceptor copia el JWT de Keycloak del usuario
 *       en {@code Authorization}, y a GitHub tiene que viajar el token de GitHub.
 *       Mandarle a un tercero el token de la sesión sería una fuga de credenciales.
 *       Se reutilizan igual los timeouts de commons.</li>
 *   <li><b>Errores manejados a mano</b> con {@code exchange}: acá 404 no siempre es
 *       un error (una rama inexistente es un "no"), así que no sirve el manejador
 *       de estados genérico.</li>
 *   <li><b>Fecha del commit = fecha del committer</b> (con la del autor como respaldo).
 *       Es la misma que usa GitHub para filtrar con {@code since} y {@code until}.</li>
 * </ul>
 */
@Slf4j
@Component
public class GitHubRestClient implements GitHubClient {

    /** GitHub devuelve como máximo 100 elementos por página. */
    static final int TAMANIO_PAGINA = 100;

    /** Tope de seguridad para no entrar en un bucle infinito (50 páginas = 5000 commits). */
    static final int MAX_PAGINAS = 50;

    private static final Pattern PATRON_URL = Pattern.compile(
            "^https://github\\.com/([A-Za-z0-9][A-Za-z0-9-]{0,38})/([A-Za-z0-9._-]+?)(?:\\.git)?/?$");

    private final RestClient restClient;

    /** Constructor que usa Spring: arma el cliente con las propiedades de la aplicación. */
    @Autowired
    public GitHubRestClient(GitHubProperties propiedades, AtenasHttpProperties http) {
        this(construirBuilder(propiedades, http).build());
    }

    /** Constructor para tests: permite inyectar un cliente conectado a un servidor simulado. */
    GitHubRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Arma el builder con URL base, cabeceras fijas de GitHub y timeouts.
     * Es visible en el paquete para que los tests prueben las cabeceras reales.
     */
    static RestClient.Builder construirBuilder(GitHubProperties propiedades, AtenasHttpProperties http) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Math.toIntExact(http.getTimeoutConexion().toMillis()));
        fabrica.setReadTimeout(Math.toIntExact(http.getTimeoutLectura().toMillis()));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(propiedades.getUrlBase())
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", propiedades.getVersionApi())
                .defaultHeader(HttpHeaders.USER_AGENT, "atenas-seguimiento");

        String token = propiedades.getToken();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return builder;
    }

    @Override
    public UbicacionRepositorio parsearUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new SolicitudInvalidaException("La URL del repositorio es obligatoria");
        }
        Matcher coincidencia = PATRON_URL.matcher(url.trim());
        if (!coincidencia.matches() || coincidencia.group(2).matches("\\.{1,2}")) {
            throw new SolicitudInvalidaException(
                    "La URL del repositorio debe tener el formato https://github.com/{propietario}/{repositorio}");
        }
        return new UbicacionRepositorio(coincidencia.group(1), coincidencia.group(2));
    }

    @Override
    public RepositorioGitHub repositorioPublico(String propietario, String nombre) {
        String referencia = propietario + "/" + nombre;
        GitHubRepositorioDto dto = obtener(
                uri -> uri.path("/repos/{propietario}/{nombre}").build(propietario, nombre),
                GitHubRepositorioDto.class)
                .orElseThrow(() -> new NoEncontradoException(
                        "El repositorio " + referencia + " no existe en GitHub"));

        if (dto.esPrivado() == null || esVacio(dto.name())
                || dto.owner() == null || esVacio(dto.owner().login())) {
            throw new RespuestaInesperadaException("GitHub devolvió datos incompletos del repositorio");
        }
        // Con un token que tenga acceso, GitHub también devuelve repos privados: se rechazan igual.
        if (dto.esPrivado()) {
            throw new RepositorioNoAccesibleException(
                    "El repositorio " + referencia + " no existe o no es público");
        }
        return new RepositorioGitHub(dto.owner().login(), dto.name(), dto.ramaPorDefecto());
    }

    @Override
    public boolean existeRama(String propietario, String nombre, String rama) {
        // Si la rama tiene "/" (por ejemplo feature/x) el valor se codifica como %2F.
        return ejecutar(
                uri -> uri.path("/repos/{propietario}/{nombre}/branches/{rama}")
                        .build(propietario, nombre, rama),
                (solicitud, respuesta) -> {
                    HttpStatusCode estado = respuesta.getStatusCode();
                    if (estado.is2xxSuccessful()) {
                        return true;
                    }
                    if (estado.value() == 404) {
                        return false;
                    }
                    throw errorPorEstado(estado);
                });
    }

    @Override
    public List<CommitGitHub> listarCommits(String propietario, String nombre, String rama,
                                            Instant desde, Instant hasta) {
        List<CommitGitHub> commits = new ArrayList<>();

        // GitHub entrega los commits de a 100: se piden páginas hasta recibir una incompleta.
        for (int pagina = 1; pagina <= MAX_PAGINAS; pagina++) {
            final int paginaActual = pagina;
            GitHubCommitDto[] recibidos = obtener(
                    uri -> uri.path("/repos/{propietario}/{nombre}/commits")
                            .queryParam("sha", "{rama}")
                            .queryParam("since", "{desde}")
                            .queryParam("until", "{hasta}")
                            .queryParam("per_page", TAMANIO_PAGINA)
                            .queryParam("page", "{pagina}")
                            .build(propietario, nombre, rama, desde.toString(), hasta.toString(), paginaActual),
                    GitHubCommitDto[].class)
                    .orElseThrow(() -> new NoEncontradoException(
                            "No se encontró el repositorio o la rama en GitHub"));

            for (GitHubCommitDto dto : recibidos) {
                commits.add(traducir(dto));
            }
            if (recibidos.length < TAMANIO_PAGINA) {
                return commits;
            }
        }
        // Si se agotó el tope, devolver una lista parcial daría conteos incorrectos sin avisar.
        throw new RespuestaInesperadaException("El repositorio tiene más commits de los que se pueden procesar");
    }

    /**
     * Traduce el DTO de GitHub al modelo propio. Si falta algo imprescindible
     * la respuesta se considera inesperada (RN28), en vez de inventar valores.
     */
    private CommitGitHub traducir(GitHubCommitDto dto) {
        if (dto == null || esVacio(dto.sha()) || dto.commit() == null || dto.parents() == null) {
            throw new RespuestaInesperadaException("GitHub devolvió un commit con datos incompletos");
        }
        String fechaTexto = fechaDe(dto.commit());
        if (esVacio(fechaTexto)) {
            throw new RespuestaInesperadaException("GitHub devolvió un commit sin fecha");
        }
        Instant fecha;
        try {
            fecha = Instant.parse(fechaTexto);
        } catch (DateTimeParseException excepcion) {
            throw new RespuestaInesperadaException("GitHub devolvió un commit con una fecha inválida");
        }

        String login = dto.author() == null ? null : dto.author().login();
        if (esVacio(login)) {
            login = null; // commit sin cuenta de GitHub asociada: quedará "sin atribuir"
        }
        boolean esMerge = dto.parents().size() > 1;
        return new CommitGitHub(dto.sha(), fecha, login, esMerge);
    }

    private String fechaDe(GitHubCommitDto.Detalle detalle) {
        if (detalle.committer() != null && !esVacio(detalle.committer().date())) {
            return detalle.committer().date();
        }
        return detalle.author() == null ? null : detalle.author().date();
    }

    /**
     * GET que interpreta el cuerpo como {@code tipo}. Devuelve vacío ante un 404;
     * cualquier otro error se traduce a excepción.
     */
    private <T> Optional<T> obtener(Function<UriBuilder, URI> uri, Class<T> tipo) {
        return ejecutar(uri, (solicitud, respuesta) -> {
            HttpStatusCode estado = respuesta.getStatusCode();
            if (estado.is2xxSuccessful()) {
                T cuerpo = respuesta.bodyTo(tipo);
                if (cuerpo == null) {
                    throw new RespuestaInesperadaException("GitHub respondió sin contenido");
                }
                return Optional.of(cuerpo);
            }
            if (estado.value() == 404) {
                return Optional.<T>empty();
            }
            throw errorPorEstado(estado);
        });
    }

    /**
     * Ejecuta el GET y centraliza el tratamiento de fallas de red:
     * timeout o conexión caída = 503; cuerpo ilegible = respuesta inesperada.
     * El orden de los catch importa: {@code ResourceAccessException} es hija de
     * {@code RestClientException}.
     */
    private <R> R ejecutar(Function<UriBuilder, URI> uri,
                           RestClient.RequestHeadersSpec.ExchangeFunction<R> funcion) {
        try {
            return restClient.get().uri(uri).exchange(funcion);
        } catch (ResourceAccessException excepcion) {
            log.warn("No se pudo conectar con GitHub o venció el tiempo de espera", excepcion);
            throw new ServicioNoDisponibleException("GitHub no respondió a tiempo", "RN-28");
        } catch (RestClientException excepcion) {
            log.warn("GitHub devolvió un contenido que no se pudo interpretar", excepcion);
            throw new RespuestaInesperadaException("GitHub devolvió un contenido que no se pudo interpretar");
        }
    }

    /** 401, 403 (token inválido o cuota), 429, 5xx y cualquier otro estado: GitHub no está disponible. */
    private ServicioNoDisponibleException errorPorEstado(HttpStatusCode estado) {
        log.warn("GitHub respondió con el estado {}", estado.value());
        return new ServicioNoDisponibleException(
                "GitHub respondió con el estado " + estado.value(), "RN-28");
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
