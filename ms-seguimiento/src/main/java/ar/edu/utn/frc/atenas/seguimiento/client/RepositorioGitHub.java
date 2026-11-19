package ar.edu.utn.frc.atenas.seguimiento.client;

/**
 * Modelo propio de un repositorio público ya validado. El service no ve los
 * DTO de GitHub: solo este modelo (patrón Adapter).
 *
 * @param propietario     nombre del dueño, con las mayúsculas canónicas de GitHub
 * @param nombre          nombre del repositorio, con las mayúsculas canónicas de GitHub
 * @param ramaPorDefecto  rama principal informada por GitHub
 */
public record RepositorioGitHub(String propietario, String nombre, String ramaPorDefecto) {
}
