package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.seguimiento.client.CommitGitHub;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.CommitsDeIntegrante;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.IntegranteEquipo;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.Marcas;
import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas del cálculo puro de actividad: sin Spring, sin base de datos y sin red. */
class CalculadorActividadTest {

    private static final Instant INICIO = Instant.parse("2026-11-02T03:00:00Z");
    private static final Instant LIMITE = Instant.parse("2026-11-09T02:59:59Z");
    private static final Instant DENTRO = Instant.parse("2026-11-05T12:00:00Z");
    private static final double UMBRAL = 0.5;

    private final CalculadorActividad calculador = new CalculadorActividad();

    private static final List<IntegranteEquipo> EQUIPO = List.of(
            new IntegranteEquipo("sub-a", "usuarioa"),
            new IntegranteEquipo("sub-b", "usuariob"));

    private static CommitGitHub commit(String sha, Instant fecha, String login, boolean merge) {
        return new CommitGitHub(sha, fecha, login, merge);
    }

    private static int de(Resultado resultado, String sub) {
        return resultado.porIntegrante().stream()
                .filter(i -> i.sub().equals(sub)).findFirst().orElseThrow().commits();
    }

    private static void verificarInvariante(Resultado resultado) {
        int atribuidos = resultado.porIntegrante().stream().mapToInt(CommitsDeIntegrante::commits).sum();
        assertThat(resultado.commitsTotales()).isEqualTo(atribuidos + resultado.commitsSinAtribuir());
    }

    // ---------------------------------------------------------------- calcular (RN-26, RN-27)

    @Test
    @DisplayName("RN-26: atribuye cada commit al integrante con ese usuario de GitHub")
    void deberiaAtribuirCadaCommit_segunElUsuarioDeGitHub() {
        Resultado r = calculador.calcular(List.of(
                commit("1", DENTRO, "usuarioa", false),
                commit("2", DENTRO, "usuarioa", false),
                commit("3", DENTRO, "usuariob", false)), INICIO, LIMITE, EQUIPO);

        assertThat(de(r, "sub-a")).isEqualTo(2);
        assertThat(de(r, "sub-b")).isEqualTo(1);
        assertThat(r.commitsSinAtribuir()).isZero();
        assertThat(r.commitsTotales()).isEqualTo(3);
        verificarInvariante(r);
    }

    @Test
    @DisplayName("RN-26: la coincidencia del usuario no distingue mayúsculas, en ninguno de los dos lados")
    void deberiaIgnorarMayusculas_alAtribuir() {
        List<IntegranteEquipo> equipo = List.of(new IntegranteEquipo("sub-a", "UsuarioA"));

        Resultado r = calculador.calcular(List.of(
                commit("1", DENTRO, "usuarioa", false),
                commit("2", DENTRO, "USUARIOA", false)), INICIO, LIMITE, equipo);

        assertThat(de(r, "sub-a")).isEqualTo(2);
        assertThat(r.commitsSinAtribuir()).isZero();
    }

    @Test
    @DisplayName("RN-26: un commit de un usuario desconocido queda sin atribuir pero cuenta en el total")
    void deberiaDejarSinAtribuir_cuandoElUsuarioNoCoincide() {
        Resultado r = calculador.calcular(List.of(
                commit("1", DENTRO, "usuarioa", false),
                commit("2", DENTRO, "otra-persona", false)), INICIO, LIMITE, EQUIPO);

        assertThat(r.commitsSinAtribuir()).isEqualTo(1);
        assertThat(r.commitsTotales()).isEqualTo(2);
        verificarInvariante(r);
    }

    @Test
    @DisplayName("RN-26: un commit sin cuenta de GitHub asociada queda sin atribuir")
    void deberiaDejarSinAtribuir_cuandoElCommitNoTieneCuenta() {
        Resultado r = calculador.calcular(List.of(commit("1", DENTRO, null, false)), INICIO, LIMITE, EQUIPO);

        assertThat(r.commitsSinAtribuir()).isEqualTo(1);
        assertThat(r.commitsTotales()).isEqualTo(1);
        verificarInvariante(r);
    }

    @Test
    @DisplayName("RN-27: los merges no se cuentan")
    void deberiaExcluirLosMerges() {
        Resultado r = calculador.calcular(List.of(
                commit("1", DENTRO, "usuarioa", false),
                commit("2", DENTRO, "usuarioa", true),
                commit("3", DENTRO, null, true)), INICIO, LIMITE, EQUIPO);

        assertThat(r.commitsTotales()).isEqualTo(1);
        assertThat(r.commitsSinAtribuir()).isZero();
        assertThat(de(r, "sub-a")).isEqualTo(1);
    }

    @Test
    @DisplayName("RN-27: no cuentan los commits anteriores al inicio ni posteriores al límite")
    void deberiaExcluirLosCommitsFueraDeLaVentana() {
        Resultado r = calculador.calcular(List.of(
                commit("antes", INICIO.minusSeconds(1), "usuarioa", false),
                commit("despues", LIMITE.plusSeconds(1), "usuarioa", false),
                commit("dentro", DENTRO, "usuarioa", false)), INICIO, LIMITE, EQUIPO);

        assertThat(r.commitsTotales()).isEqualTo(1);
        assertThat(de(r, "sub-a")).isEqualTo(1);
    }

    @Test
    @DisplayName("RN-27: los extremos de la ventana están incluidos")
    void deberiaIncluirLosExtremosDeLaVentana() {
        Resultado r = calculador.calcular(List.of(
                commit("inicio", INICIO, "usuarioa", false),
                commit("limite", LIMITE, "usuariob", false)), INICIO, LIMITE, EQUIPO);

        assertThat(r.commitsTotales()).isEqualTo(2);
        assertThat(de(r, "sub-a")).isEqualTo(1);
        assertThat(de(r, "sub-b")).isEqualTo(1);
    }

    @Test
    @DisplayName("Hay una fila por integrante aunque tenga 0 commits, en el orden recibido")
    void deberiaDevolverUnaFilaPorIntegrante_aunqueTengaCero() {
        Resultado r = calculador.calcular(List.of(commit("1", DENTRO, "usuarioa", false)), INICIO, LIMITE, EQUIPO);

        assertThat(r.porIntegrante()).extracting(CommitsDeIntegrante::sub).containsExactly("sub-a", "sub-b");
        assertThat(de(r, "sub-b")).isZero();
    }

    @Test
    @DisplayName("Un equipo sin commits tiene total 0 y todos los integrantes en 0")
    void deberiaDevolverCeros_cuandoNoHayCommits() {
        Resultado r = calculador.calcular(List.of(), INICIO, LIMITE, EQUIPO);

        assertThat(r.commitsTotales()).isZero();
        assertThat(r.commitsSinAtribuir()).isZero();
        assertThat(r.porIntegrante()).hasSize(2).allMatch(i -> i.commits() == 0);
    }

    @Test
    @DisplayName("RN-26: un integrante sin usuario de GitHub cargado no recibe commits y conserva su fila")
    void deberiaNoAtribuir_aUnIntegranteSinUsuarioDeGitHub() {
        List<IntegranteEquipo> equipo = List.of(
                new IntegranteEquipo("sub-a", "usuarioa"),
                new IntegranteEquipo("sub-c", null));

        Resultado r = calculador.calcular(List.of(commit("1", DENTRO, "usuarioa", false)), INICIO, LIMITE, equipo);

        assertThat(de(r, "sub-c")).isZero();
        assertThat(r.porIntegrante()).extracting(CommitsDeIntegrante::usuarioGithub).containsExactly("usuarioa", null);
    }

    @Test
    @DisplayName("El usuario informado en cada fila es el que tenía el integrante al consultar")
    void deberiaConservarElUsuarioUsadoEnLaConsulta() {
        Resultado r = calculador.calcular(List.of(), INICIO, LIMITE,
                List.of(new IntegranteEquipo("sub-a", "UsuarioA")));

        assertThat(r.porIntegrante().get(0).usuarioGithub()).isEqualTo("UsuarioA");
    }

    // ---------------------------------------------------------------- calcularMarcas (RN-27)

    private static List<CommitsDeIntegrante> filas(int... commits) {
        return java.util.stream.IntStream.range(0, commits.length)
                .mapToObj(i -> new CommitsDeIntegrante("sub-" + i, "u" + i, commits[i])).toList();
    }

    @Test
    @DisplayName("RN-27: sin commits en el sprint, el equipo se marca sin actividad y nadie con baja contribución")
    void deberiaMarcarSinActividad_cuandoNoHayCommits() {
        Marcas marcas = calculador.calcularMarcas(0, filas(0, 0, 0), UMBRAL);

        assertThat(marcas.sinActividad()).isTrue();
        assertThat(marcas.subsConBajaContribucion()).isEmpty();
    }

    @Test
    @DisplayName("RN-27: un integrante por debajo de 1/2 del promedio se marca con baja contribución")
    void deberiaMarcarBajaContribucion_cuandoEstaPorDebajoDelUmbral() {
        // Atribuidos 22 entre 3 integrantes: promedio 22/3 ≈ 7,33; mínimo esperado 11/3 ≈ 3,67.
        Marcas marcas = calculador.calcularMarcas(22, filas(10, 10, 2), UMBRAL);

        assertThat(marcas.sinActividad()).isFalse();
        assertThat(marcas.subsConBajaContribucion()).containsExactly("sub-2");
    }

    @Test
    @DisplayName("RN-27: un aporte parejo no marca a nadie")
    void deberiaNoMarcarNadie_cuandoElAporteEsParejo() {
        assertThat(calculador.calcularMarcas(12, filas(4, 4, 4), UMBRAL).subsConBajaContribucion()).isEmpty();
    }

    @Test
    @DisplayName("RN-27: estar exactamente en el umbral no es baja contribución")
    void deberiaNoMarcar_cuandoEstaExactamenteEnElUmbral() {
        // Atribuidos 6 entre 3: promedio 2; mínimo esperado 1; el integrante con 1 no está por debajo.
        assertThat(calculador.calcularMarcas(6, filas(4, 1, 1), UMBRAL).subsConBajaContribucion()).isEmpty();
    }

    @Test
    @DisplayName("RN-27: quien no aportó nada se marca cuando el resto sí aportó")
    void deberiaMarcarAlQueTieneCero_cuandoElRestoAporto() {
        assertThat(calculador.calcularMarcas(8, filas(4, 4, 0), UMBRAL).subsConBajaContribucion())
                .containsExactly("sub-2");
    }

    @Test
    @DisplayName("RN-27: si todos los commits quedaron sin atribuir, hay actividad pero nadie queda marcado")
    void deberiaNoMarcarNadie_cuandoTodoQuedoSinAtribuir() {
        Marcas marcas = calculador.calcularMarcas(5, filas(0, 0), UMBRAL);

        assertThat(marcas.sinActividad()).isFalse();
        assertThat(marcas.subsConBajaContribucion()).isEmpty();
    }

    @Test
    @DisplayName("RN-27: el umbral es configurable")
    void deberiaUsarElUmbralConfigurado() {
        // Promedio 4; con umbral 1 el mínimo esperado es 4: quien tiene 3 queda marcado.
        assertThat(calculador.calcularMarcas(12, filas(5, 4, 3), 1.0).subsConBajaContribucion())
                .containsExactly("sub-2");
    }

    @Test
    @DisplayName("Un equipo sin integrantes no produce marcas de baja contribución")
    void deberiaNoFallar_cuandoNoHayIntegrantes() {
        Marcas marcas = calculador.calcularMarcas(0, List.of(), UMBRAL);

        assertThat(marcas.sinActividad()).isTrue();
        assertThat(marcas.subsConBajaContribucion()).isEmpty();
    }

    @Test
    @DisplayName("RN-27: el cálculo es puro: no tiene anotaciones de Spring ni estado inyectado")
    void deberiaSerPuro_cuandoSeUsaSinSpring() {
        assertThat(CalculadorActividad.class.getAnnotations()).isEmpty();
        assertThat(CalculadorActividad.class.getDeclaredFields()).isEmpty();
    }
}
