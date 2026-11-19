package ar.edu.utn.frc.atenas.catedra.estado;

import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;

/**
 * Un equipo abierto permite ingresar, salir y asegurarse. No puede reabrirse.
 */
public final class Abierto implements ComportamientoEquipo {

    @Override
    public void ingresar(Equipo equipo) {
        // Sin restricciones adicionales.
    }

    @Override
    public void salir(Equipo equipo) {
        // Sin restricciones adicionales.
    }

    @Override
    public void asegurar(Equipo equipo) {
        equipo.setEstado(EstadoEquipo.ASEGURADO);
    }

    @Override
    public void reabrir(Equipo equipo) {
        throw new ConflictoException("El equipo ya está abierto", "RN-11");
    }
}
