package ar.edu.utn.frc.atenas.seguimiento.client;

import java.time.Instant;
import java.util.List;

/**
 * Adaptador de salida hacia la API REST de GitHub (patrón Adapter).
 *
 * <p>Los {@code service} dependen de esta interfaz y no de {@code RestClient}:
 * así las reglas de negocio no conocen la paginación, el token ni el formato
 * JSON de GitHub, y se pueden probar con Mockito sin red.</p>
 *
 * <p>Traducción de errores que hacen todas las implementaciones:</p>
 * <ul>
 *   <li>404 de un recurso que debía existir: {@code NoEncontradoException}.</li>
 *   <li>401, 403 (token inválido o cuota), 429, 5xx, timeout o falla de conexión:
 *       {@code ServicioNoDisponibleException}.</li>
 *   <li>Respuesta que no se puede interpretar: {@code RespuestaInesperadaException}.</li>
 * </ul>
 */
public interface GitHubClient {

    /**
     * Extrae propietario y nombre de una URL {@code https://github.com/{propietario}/{nombre}[.git]}.
     * No consulta la red.
     *
     * @throws ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException si la URL no se reconoce (400)
     */
    UbicacionRepositorio parsearUrl(String url);

    /**
     * Devuelve los datos básicos de un repositorio público.
     *
     * @throws ar.edu.utn.frc.atenas.commons.error.NoEncontradoException si GitHub responde 404
     * @throws ar.edu.utn.frc.atenas.seguimiento.exception.RepositorioNoAccesibleException
     *         si el repositorio existe pero es privado (RN25)
     */
    RepositorioGitHub repositorioPublico(String propietario, String nombre);

    /**
     * Indica si la rama existe en el repositorio. Un 404 no es un error: devuelve {@code false}.
     */
    boolean existeRama(String propietario, String nombre, String rama);

    /**
     * Lista los commits de una rama con fecha entre {@code desde} y {@code hasta}.
     * La paginación de GitHub queda oculta: se recorren todas las páginas.
     * Incluye los merges (se identifican con {@link CommitGitHub#esMerge()}):
     * decidir cuáles cuentan es regla de negocio y no de este cliente.
     */
    List<CommitGitHub> listarCommits(String propietario, String nombre, String rama,
                                     Instant desde, Instant hasta);
}
