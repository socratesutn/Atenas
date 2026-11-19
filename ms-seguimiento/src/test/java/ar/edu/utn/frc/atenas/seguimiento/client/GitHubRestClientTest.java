package ar.edu.utn.frc.atenas.seguimiento.client;

import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.http.AtenasHttpProperties;
import ar.edu.utn.frc.atenas.seguimiento.config.GitHubProperties;
import ar.edu.utn.frc.atenas.seguimiento.exception.RepositorioNoAccesibleException;
import ar.edu.utn.frc.atenas.seguimiento.exception.RespuestaInesperadaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pruebas del adaptador de GitHub. No usan red: {@link MockRestServiceServer} simula las
 * respuestas de la API y permite comprobar qué pide el cliente.
 */
class GitHubRestClientTest {

    private static final Instant DESDE = Instant.parse("2026-11-02T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-11-09T02:59:59Z");

    private MockRestServiceServer servidor;
    private GitHubRestClient cliente;

    @BeforeEach
    void preparar() {
        GitHubProperties propiedades = new GitHubProperties();
        propiedades.setToken("token-de-prueba");
        // Se usa el mismo builder que en producción para probar también las cabeceras reales.
        RestClient.Builder builder = GitHubRestClient.construirBuilder(propiedades, new AtenasHttpProperties());
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new GitHubRestClient(builder.build());
    }

    // ---------------------------------------------------------------- utilidades

    /** Verifica el camino y que la consulta contenga cada fragmento (la consulta llega decodificada). */
    private static RequestMatcher pedidoA(String ruta, String... fragmentosDeConsulta) {
        return solicitud -> {
            URI uri = solicitud.getURI();
            assertThat(uri.getRawPath()).isEqualTo(ruta);
            for (String fragmento : fragmentosDeConsulta) {
                assertThat(uri.getQuery()).contains(fragmento);
            }
        };
    }

    private static String commitJson(String sha, String fecha, String login, int padres) {
        String cuenta = login == null ? "null" : "{\"login\":\"" + login + "\",\"id\":1}";
        String listaPadres = IntStream.range(0, padres)
                .mapToObj(i -> "{\"sha\":\"padre" + i + "\"}")
                .collect(Collectors.joining(","));
        // Incluye propiedades que el cliente no usa, para comprobar que se ignoran.
        return "{\"sha\":\"" + sha + "\",\"node_id\":\"x\",\"commit\":{\"message\":\"m\","
                + "\"author\":{\"name\":\"n\",\"date\":\"" + fecha + "\"},"
                + "\"committer\":{\"name\":\"n\",\"date\":\"" + fecha + "\"}},"
                + "\"author\":" + cuenta + ",\"parents\":[" + listaPadres + "],\"html_url\":\"u\"}";
    }

    private static String pagina(String... commits) {
        return "[" + String.join(",", commits) + "]";
    }

    private void esperarCommits(int numeroPagina, org.springframework.test.web.client.ResponseCreator respuesta) {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/repos/usuario/repo/commits",
                        "sha=main", "since=2026-11-02T03:00:00Z", "until=2026-11-09T02:59:59Z",
                        "per_page=100", "page=" + numeroPagina))
                .andRespond(respuesta);
    }

    private List<CommitGitHub> listar() {
        return cliente.listarCommits("usuario", "repo", "main", DESDE, HASTA);
    }

    // ---------------------------------------------------------------- listarCommits

    @Test
    @DisplayName("RN-27: lista los commits de una página con autor, sin autor y merge, y manda las cabeceras de GitHub")
    void deberiaTraducirLosCommits_cuandoHayUnaSolaPagina() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(solicitud -> {
                    assertThat(solicitud.getHeaders().getFirst("Authorization")).isEqualTo("Bearer token-de-prueba");
                    assertThat(solicitud.getHeaders().getFirst("Accept")).contains("application/vnd.github+json");
                    assertThat(solicitud.getHeaders().getFirst("X-Api-Version")).isNull();
                    assertThat(solicitud.getHeaders().getFirst("X-GitHub-Api-Version")).isEqualTo("2022-11-28");
                })
                .andRespond(withSuccess(pagina(
                        commitJson("a1", "2026-11-03T12:00:00Z", "UsuarioA", 1),
                        commitJson("a2", "2026-11-04T12:00:00Z", null, 1),
                        commitJson("a3", "2026-11-05T12:00:00Z", "usuariob", 2)),
                        MediaType.APPLICATION_JSON));

        List<CommitGitHub> commits = listar();

        assertThat(commits).containsExactly(
                new CommitGitHub("a1", Instant.parse("2026-11-03T12:00:00Z"), "UsuarioA", false),
                new CommitGitHub("a2", Instant.parse("2026-11-04T12:00:00Z"), null, false),
                new CommitGitHub("a3", Instant.parse("2026-11-05T12:00:00Z"), "usuariob", true));
        servidor.verify();
    }

    @Test
    @DisplayName("RN-27: recorre todas las páginas cuando hay más de 100 commits")
    void deberiaPedirLaSiguientePagina_cuandoLaPrimeraVieneCompleta() {
        String[] primera = IntStream.range(0, 100)
                .mapToObj(i -> commitJson("c" + i, "2026-11-03T12:00:00Z", "usuarioa", 1))
                .toArray(String[]::new);
        esperarCommits(1, withSuccess(pagina(primera), MediaType.APPLICATION_JSON));
        esperarCommits(2, withSuccess(pagina(commitJson("ultimo", "2026-11-04T12:00:00Z", "usuarioa", 1)),
                MediaType.APPLICATION_JSON));

        List<CommitGitHub> commits = listar();

        assertThat(commits).hasSize(101);
        assertThat(commits.get(100).sha()).isEqualTo("ultimo");
        servidor.verify();
    }

    @Test
    @DisplayName("RN-27: una página vacía después de una completa cierra la lista")
    void deberiaTerminar_cuandoLaPaginaSiguienteVieneVacia() {
        String[] primera = IntStream.range(0, 100)
                .mapToObj(i -> commitJson("c" + i, "2026-11-03T12:00:00Z", "usuarioa", 1))
                .toArray(String[]::new);
        esperarCommits(1, withSuccess(pagina(primera), MediaType.APPLICATION_JSON));
        esperarCommits(2, withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(listar()).hasSize(100);
        servidor.verify();
    }

    @Test
    @DisplayName("Una rama sin commits devuelve lista vacía")
    void deberiaDevolverListaVacia_cuandoNoHayCommits() {
        esperarCommits(1, withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(listar()).isEmpty();
    }

    @Test
    @DisplayName("404 al listar commits: NoEncontradoException")
    void deberiaLanzarNoEncontrado_cuandoGitHubResponde404() {
        esperarCommits(1, withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, this::listar);
    }

    @ParameterizedTest(name = "RN-28: el estado {0} se traduce a ServicioNoDisponibleException")
    @ValueSource(ints = {401, 403, 429, 500, 502, 503})
    @DisplayName("RN-28: token inválido, cuota agotada o falla de GitHub: servicio no disponible")
    void deberiaLanzarServicioNoDisponible_cuandoGitHubRespondeError(int estado) {
        esperarCommits(1, withStatus(HttpStatus.valueOf(estado)));

        ServicioNoDisponibleException excepcion = assertThrows(ServicioNoDisponibleException.class, this::listar);
        // No debe ser la subclase de "respuesta inesperada": no responder es otra situación.
        assertThat(excepcion).isNotInstanceOf(RespuestaInesperadaException.class);
    }

    @Test
    @DisplayName("RN-28: un timeout o una falla de conexión se traduce a ServicioNoDisponibleException")
    void deberiaLanzarServicioNoDisponible_cuandoVenceElTiempo() {
        esperarCommits(1, withException(new SocketTimeoutException("Read timed out")));

        ServicioNoDisponibleException excepcion = assertThrows(ServicioNoDisponibleException.class, this::listar);
        assertThat(excepcion).isNotInstanceOf(RespuestaInesperadaException.class);
    }

    @Test
    @DisplayName("RN-28: un JSON que no es una lista de commits es una respuesta inesperada")
    void deberiaLanzarRespuestaInesperada_cuandoElJsonNoEsUnaLista() {
        esperarCommits(1, withSuccess("{\"mensaje\":\"otra cosa\"}", MediaType.APPLICATION_JSON));

        assertThrows(RespuestaInesperadaException.class, this::listar);
    }

    @Test
    @DisplayName("RN-28: una respuesta 200 que no es JSON es una respuesta inesperada")
    void deberiaLanzarRespuestaInesperada_cuandoLaRespuestaEsHtml() {
        esperarCommits(1, withSuccess("<html>mantenimiento</html>", MediaType.TEXT_HTML));

        assertThrows(RespuestaInesperadaException.class, this::listar);
    }

    @Test
    @DisplayName("RN-28: un commit sin sha es una respuesta inesperada")
    void deberiaLanzarRespuestaInesperada_cuandoUnCommitNoTieneSha() {
        esperarCommits(1, withSuccess(pagina(commitJson("", "2026-11-03T12:00:00Z", "usuarioa", 1)),
                MediaType.APPLICATION_JSON));

        assertThrows(RespuestaInesperadaException.class, this::listar);
    }

    @Test
    @DisplayName("RN-28: un commit con fecha inválida es una respuesta inesperada")
    void deberiaLanzarRespuestaInesperada_cuandoLaFechaEsInvalida() {
        esperarCommits(1, withSuccess(pagina(commitJson("a1", "ayer a la tarde", "usuarioa", 1)),
                MediaType.APPLICATION_JSON));

        assertThrows(RespuestaInesperadaException.class, this::listar);
    }

    @Test
    @DisplayName("RN-28: un commit sin la lista de padres es una respuesta inesperada")
    void deberiaLanzarRespuestaInesperada_cuandoFaltanLosPadres() {
        String sinPadres = "{\"sha\":\"a1\",\"commit\":{\"committer\":{\"date\":\"2026-11-03T12:00:00Z\"}},"
                + "\"author\":{\"login\":\"usuarioa\"}}";
        esperarCommits(1, withSuccess(pagina(sinPadres), MediaType.APPLICATION_JSON));

        assertThrows(RespuestaInesperadaException.class, this::listar);
    }

    @Test
    @DisplayName("Si no hay fecha del committer se usa la del autor")
    void deberiaUsarLaFechaDelAutor_cuandoNoHayFechaDelCommitter() {
        String soloAutor = "{\"sha\":\"a1\",\"commit\":{\"author\":{\"date\":\"2026-11-03T12:00:00Z\"}},"
                + "\"author\":{\"login\":\"usuarioa\"},\"parents\":[{\"sha\":\"p\"}]}";
        esperarCommits(1, withSuccess(pagina(soloAutor), MediaType.APPLICATION_JSON));

        assertThat(listar()).containsExactly(
                new CommitGitHub("a1", Instant.parse("2026-11-03T12:00:00Z"), "usuarioa", false));
    }

    // ---------------------------------------------------------------- repositorioPublico

    @Test
    @DisplayName("RN-25: devuelve los datos de un repositorio público con las mayúsculas de GitHub")
    void deberiaDevolverElRepositorio_cuandoEsPublico() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/repos/usuario/repo"))
                .andRespond(withSuccess("{\"name\":\"Repo\",\"private\":false,\"default_branch\":\"main\","
                        + "\"owner\":{\"login\":\"Usuario\",\"id\":9},\"stargazers_count\":3}",
                        MediaType.APPLICATION_JSON));

        RepositorioGitHub repositorio = cliente.repositorioPublico("usuario", "repo");

        assertThat(repositorio).isEqualTo(new RepositorioGitHub("Usuario", "Repo", "main"));
    }

    @Test
    @DisplayName("RN-25: un repositorio privado se rechaza aunque el token pueda verlo")
    void deberiaRechazar_cuandoElRepositorioEsPrivado() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("{\"name\":\"repo\",\"private\":true,\"default_branch\":\"main\","
                        + "\"owner\":{\"login\":\"usuario\"}}", MediaType.APPLICATION_JSON));

        assertThrows(RepositorioNoAccesibleException.class, () -> cliente.repositorioPublico("usuario", "repo"));
    }

    @Test
    @DisplayName("RN-25: un 404 al buscar el repositorio se traduce a NoEncontradoException")
    void deberiaLanzarNoEncontrado_cuandoElRepositorioNoExiste() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(NoEncontradoException.class, () -> cliente.repositorioPublico("usuario", "repo"));
    }

    @Test
    @DisplayName("RN-28: 403 al validar el repositorio es servicio no disponible, no repositorio inválido")
    void deberiaLanzarServicioNoDisponible_cuandoGitHubResponde403AlValidar() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.repositorioPublico("usuario", "repo"));
    }

    @Test
    @DisplayName("RN-28: un repositorio sin el campo private es una respuesta inesperada")
    void deberiaLanzarRespuestaInesperada_cuandoFaltaElCampoPrivate() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("{\"name\":\"repo\",\"owner\":{\"login\":\"usuario\"}}",
                        MediaType.APPLICATION_JSON));

        assertThrows(RespuestaInesperadaException.class, () -> cliente.repositorioPublico("usuario", "repo"));
    }

    // ---------------------------------------------------------------- existeRama

    @Test
    @DisplayName("RN-25: la rama existe cuando GitHub responde 200")
    void deberiaDevolverVerdadero_cuandoLaRamaExiste() {
        servidor.expect(once(), method(HttpMethod.GET))
                .andExpect(pedidoA("/repos/usuario/repo/branches/main"))
                .andRespond(withSuccess("{\"name\":\"main\"}", MediaType.APPLICATION_JSON));

        assertThat(cliente.existeRama("usuario", "repo", "main")).isTrue();
    }

    @Test
    @DisplayName("RN-25: la rama no existe cuando GitHub responde 404 (no es un error)")
    void deberiaDevolverFalso_cuandoLaRamaNoExiste() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(cliente.existeRama("usuario", "repo", "inexistente")).isFalse();
    }

    @Test
    @DisplayName("RN-28: una falla de GitHub al buscar la rama es servicio no disponible")
    void deberiaLanzarServicioNoDisponible_cuandoFallaLaBusquedaDeLaRama() {
        servidor.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.existeRama("usuario", "repo", "main"));
    }

    // ---------------------------------------------------------------- parsearUrl

    @ParameterizedTest(name = "reconoce {0}")
    @ValueSource(strings = {
            "https://github.com/usuario/repo",
            "https://github.com/usuario/repo.git",
            "https://github.com/usuario/repo/",
            "  https://github.com/usuario/repo  "})
    @DisplayName("Reconoce las formas habituales de URL de un repositorio")
    void deberiaExtraerPropietarioYNombre_cuandoLaUrlEsValida(String url) {
        assertThat(cliente.parsearUrl(url)).isEqualTo(new UbicacionRepositorio("usuario", "repo"));
    }

    @Test
    @DisplayName("Conserva puntos y guiones del nombre del repositorio")
    void deberiaConservarElNombre_cuandoTienePuntosYGuiones() {
        assertThat(cliente.parsearUrl("https://github.com/mi-grupo/tpi.backend_2026.git"))
                .isEqualTo(new UbicacionRepositorio("mi-grupo", "tpi.backend_2026"));
    }

    @ParameterizedTest(name = "rechaza [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {
            "no es una url",
            "http://github.com/usuario/repo",
            "https://gitlab.com/usuario/repo",
            "https://github.com/usuario",
            "https://github.com/usuario/repo/tree/main",
            "https://github.com/-usuario/repo",
            "https://github.com/usuario/.."})
    @DisplayName("RN-25: una URL mal formada se rechaza con 400")
    void deberiaLanzarSolicitudInvalida_cuandoLaUrlNoSeReconoce(String url) {
        assertThrows(SolicitudInvalidaException.class, () -> cliente.parsearUrl(url));
    }

    @Test
    @DisplayName("Sin token configurado no se manda la cabecera Authorization")
    void deberiaOmitirAuthorization_cuandoNoHayToken() throws IOException {
        RestClient.Builder builder = GitHubRestClient.construirBuilder(new GitHubProperties(), new AtenasHttpProperties());
        MockRestServiceServer otroServidor = MockRestServiceServer.bindTo(builder).build();
        GitHubRestClient sinToken = new GitHubRestClient(builder.build());
        otroServidor.expect(once(), method(HttpMethod.GET))
                .andExpect(solicitud -> assertThat(solicitud.getHeaders().containsKey("Authorization")).isFalse())
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        sinToken.listarCommits("usuario", "repo", "main", DESDE, HASTA);

        otroServidor.verify();
    }
}
