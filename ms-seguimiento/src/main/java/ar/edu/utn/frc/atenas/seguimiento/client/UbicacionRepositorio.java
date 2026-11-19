package ar.edu.utn.frc.atenas.seguimiento.client;

/**
 * Propietario y nombre de un repositorio, tal como salen de su URL
 * ({@code https://github.com/{propietario}/{nombre}}).
 */
public record UbicacionRepositorio(String propietario, String nombre) {
}
