package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.seguimiento.client.CommitGitHub;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Cálculo puro de la actividad de un equipo en un sprint (RN26, RN27).
 *
 * <p>Es un componente sin acceso a base de datos ni a la red: recibe los commits ya
 * descargados y devuelve números. Eso lo hace fácil de probar y de explicar. Quien
 * orquesta (la actualización de actividad) se ocupa de pedirle los commits a GitHub y
 * de guardar el resultado.</p>
 *
 * <h3>Reglas que aplica</h3>
 * <ol>
 *   <li>Los merges (más de un padre) no se cuentan.</li>
 *   <li>Solo cuentan los commits dentro de {@code [inicio, limite]}, ambos extremos incluidos.
 *       Los posteriores al límite no cuentan (docs/14, A-07).</li>
 *   <li>Cada commit se atribuye al integrante cuyo usuario de GitHub coincida con el autor,
 *       sin distinguir mayúsculas (GitHub tampoco las distingue). Si no coincide, o el
 *       commit no tiene cuenta asociada, queda "sin atribuir".</li>
 *   <li>Invariante: {@code totales = Σ commits por integrante + sin atribuir}.</li>
 *   <li>Hay una fila por integrante aunque tenga 0 commits.</li>
 * </ol>
 */
public final class CalculadorActividad {

    /** Integrante del equipo tal como lo informa ms-catedra (solo lo que necesita el cálculo). */
    public record IntegranteEquipo(String sub, String usuarioGithub) {
    }

    /** Commits atribuidos a un integrante (puede ser 0). */
    public record CommitsDeIntegrante(String sub, String usuarioGithub, int commits) {
    }

    /** Resultado del cálculo de un sprint. */
    public record Resultado(int commitsTotales, int commitsSinAtribuir, List<CommitsDeIntegrante> porIntegrante) {
    }

    /**
     * Marcas informativas. No se guardan: se calculan cada vez que se consulta.
     *
     * @param sinActividad                el equipo no tiene ningún commit en el sprint
     * @param subsConBajaContribucion     integrantes por debajo del umbral
     */
    public record Marcas(boolean sinActividad, Set<String> subsConBajaContribucion) {
    }

    /**
     * Calcula los commits del sprint.
     *
     * @param commits     commits de la rama elegida, tal como los devuelve GitHub (con merges y fuera de ventana)
     * @param inicio      fecha de inicio del sprint (incluida)
     * @param limite      fecha límite del sprint (incluida)
     * @param integrantes integrantes vigentes del equipo
     */
    public Resultado calcular(List<CommitGitHub> commits, Instant inicio, Instant limite,
                              List<IntegranteEquipo> integrantes) {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(limite, "limite");

        // Contadores en el orden de los integrantes, todos en 0: así sale una fila por cada uno.
        Map<String, Integer> commitsPorSub = new LinkedHashMap<>();
        Map<String, String> usuarioPorSub = new HashMap<>();
        // usuario de GitHub en minúsculas -> sub del integrante
        Map<String, String> subPorUsuario = new HashMap<>();
        for (IntegranteEquipo integrante : integrantes) {
            commitsPorSub.putIfAbsent(integrante.sub(), 0);
            usuarioPorSub.putIfAbsent(integrante.sub(), integrante.usuarioGithub());
            if (integrante.usuarioGithub() != null && !integrante.usuarioGithub().isBlank()) {
                subPorUsuario.putIfAbsent(minusculas(integrante.usuarioGithub()), integrante.sub());
            }
        }

        int totales = 0;
        int sinAtribuir = 0;
        for (CommitGitHub commit : commits) {
            if (commit.esMerge()) {
                continue;
            }
            if (commit.fecha().isBefore(inicio) || commit.fecha().isAfter(limite)) {
                continue;
            }
            totales++;
            String sub = commit.autorLogin() == null ? null : subPorUsuario.get(minusculas(commit.autorLogin()));
            if (sub == null) {
                sinAtribuir++;
            } else {
                commitsPorSub.merge(sub, 1, Integer::sum);
            }
        }

        List<CommitsDeIntegrante> filas = commitsPorSub.entrySet().stream()
                .map(entrada -> new CommitsDeIntegrante(
                        entrada.getKey(), usuarioPorSub.get(entrada.getKey()), entrada.getValue()))
                .toList();
        return new Resultado(totales, sinAtribuir, filas);
    }

    /**
     * Calcula las marcas "sin actividad" y "baja contribución" (RN27), solo informativas.
     *
     * <p>Baja contribución: el integrante tiene menos commits que {@code umbral × promedio},
     * donde el promedio es (commits atribuidos) / (cantidad de integrantes). El umbral lo pasa
     * quien consulta, leyéndolo de la configuración: así esta clase sigue siendo pura. Si no hay
     * commits atribuidos el promedio es 0 y nadie queda marcado: no tiene sentido decir que todos
     * aportan poco respecto de nada.</p>
     *
     * @param umbralBajaContribucion fracción del promedio por debajo de la cual se marca la baja contribución
     */
    public Marcas calcularMarcas(int commitsTotales, List<CommitsDeIntegrante> porIntegrante,
                                 double umbralBajaContribucion) {
        boolean sinActividad = commitsTotales == 0;
        if (porIntegrante.isEmpty()) {
            return new Marcas(sinActividad, Set.of());
        }

        long atribuidos = porIntegrante.stream().mapToLong(CommitsDeIntegrante::commits).sum();
        double promedio = (double) atribuidos / porIntegrante.size();
        double minimoEsperado = umbralBajaContribucion * promedio;

        Set<String> bajaContribucion = new LinkedHashSet<>();
        for (CommitsDeIntegrante integrante : porIntegrante) {
            if (integrante.commits() < minimoEsperado) {
                bajaContribucion.add(integrante.sub());
            }
        }
        return new Marcas(sinActividad, bajaContribucion);
    }

    private String minusculas(String texto) {
        return texto.toLowerCase(Locale.ROOT);
    }
}
