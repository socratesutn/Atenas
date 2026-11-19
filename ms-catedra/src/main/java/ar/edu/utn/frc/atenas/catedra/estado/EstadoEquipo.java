package ar.edu.utn.frc.atenas.catedra.estado;

/**
 * Estado persistido del equipo. Cada valor delega las operaciones en un objeto
 * de comportamiento sin estado, siguiendo el patrón State.
 */
public enum EstadoEquipo {

    ABIERTO(new Abierto()),
    ASEGURADO(new Asegurado());

    private final ComportamientoEquipo comportamiento;

    EstadoEquipo(ComportamientoEquipo comportamiento) {
        this.comportamiento = comportamiento;
    }

    public ComportamientoEquipo comportamiento() {
        return comportamiento;
    }
}
