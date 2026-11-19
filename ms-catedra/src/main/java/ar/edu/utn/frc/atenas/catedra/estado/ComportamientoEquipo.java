package ar.edu.utn.frc.atenas.catedra.estado;

import ar.edu.utn.frc.atenas.catedra.entity.Equipo;

/**
 * Comportamiento asociado a un estado del equipo. El service conserva las
 * reglas de pertenencia, cupo y GitHub; el estado solo decide si la operación
 * es válida.
 */
public interface ComportamientoEquipo {

    void ingresar(Equipo equipo);

    void salir(Equipo equipo);

    void asegurar(Equipo equipo);

    void reabrir(Equipo equipo);
}
