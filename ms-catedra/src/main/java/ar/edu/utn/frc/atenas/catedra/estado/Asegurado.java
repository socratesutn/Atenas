package ar.edu.utn.frc.atenas.catedra.estado;

import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;

/**
 * Un equipo asegurado bloquea ingresos, salidas y nuevas aserciones, pero puede
 * reabrirse.
 */
public final class Asegurado implements ComportamientoEquipo {

    @Override
    public void ingresar(Equipo equipo) {
        throw new ConflictoException("El equipo está asegurado y no permite nuevos ingresos", "RN-08");
    }

    @Override
    public void salir(Equipo equipo) {
        throw new ConflictoException("El equipo está asegurado y no permite salidas", "RN-09");
    }

    @Override
    public void asegurar(Equipo equipo) {
        throw new ConflictoException("El equipo ya está asegurado", "RN-10");
    }

    @Override
    public void reabrir(Equipo equipo) {
        equipo.setEstado(EstadoEquipo.ABIERTO);
    }
}
