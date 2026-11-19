package ar.edu.utn.frc.atenas.seguimiento.client;

import java.time.Instant;

/**
 * Modelo propio de un commit, con solo lo que necesita el cálculo de actividad.
 *
 * @param sha         identificador del commit
 * @param fecha       fecha del commit (la del committer; ver {@link GitHubRestClient})
 * @param autorLogin  usuario de GitHub del autor; {@code null} si GitHub no pudo
 *                    asociar el correo del commit a ninguna cuenta
 * @param esMerge     {@code true} si el commit tiene más de un padre
 */
public record CommitGitHub(String sha, Instant fecha, String autorLogin, boolean esMerge) {
}
