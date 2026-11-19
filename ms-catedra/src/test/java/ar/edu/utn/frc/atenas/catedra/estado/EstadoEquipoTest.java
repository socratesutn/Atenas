package ar.edu.utn.frc.atenas.catedra.estado;

import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstadoEquipoTest {

    @Test
    @DisplayName("RN-08/RN-09: ABIERTO permite ingresar y salir")
    void deberiaPermitirIngresoYSalida_cuandoEstaAbierto() {
        Equipo equipo = equipo(EstadoEquipo.ABIERTO);

        assertDoesNotThrow(equipo::ingresar);
        assertDoesNotThrow(equipo::salir);
    }

    @Test
    @DisplayName("RN-10: ABIERTO permite asegurar y cambia de estado")
    void deberiaAsegurar_cuandoEstaAbierto() {
        Equipo equipo = equipo(EstadoEquipo.ABIERTO);

        equipo.asegurar();

        assertEquals(EstadoEquipo.ASEGURADO, equipo.getEstado());
    }

    @Test
    @DisplayName("RN-11: ABIERTO rechaza reabrir con 409")
    void deberiaRechazarReapertura_cuandoYaEstaAbierto() {
        Equipo equipo = equipo(EstadoEquipo.ABIERTO);

        ConflictoException excepcion = assertThrows(ConflictoException.class, equipo::reabrir);
        assertEquals("RN-11", excepcion.getRegla());
    }

    @Test
    @DisplayName("RN-08: ASEGURADO rechaza ingresar con 409")
    void deberiaRechazarIngreso_cuandoEstaAsegurado() {
        Equipo equipo = equipo(EstadoEquipo.ASEGURADO);

        ConflictoException excepcion = assertThrows(ConflictoException.class, equipo::ingresar);
        assertEquals("RN-08", excepcion.getRegla());
    }

    @Test
    @DisplayName("RN-09: ASEGURADO rechaza salir con 409")
    void deberiaRechazarSalida_cuandoEstaAsegurado() {
        Equipo equipo = equipo(EstadoEquipo.ASEGURADO);

        ConflictoException excepcion = assertThrows(ConflictoException.class, equipo::salir);
        assertEquals("RN-09", excepcion.getRegla());
    }

    @Test
    @DisplayName("RN-10: ASEGURADO rechaza asegurar nuevamente con 409")
    void deberiaRechazarAseguramiento_cuandoYaEstaAsegurado() {
        Equipo equipo = equipo(EstadoEquipo.ASEGURADO);

        ConflictoException excepcion = assertThrows(ConflictoException.class, equipo::asegurar);
        assertEquals("RN-10", excepcion.getRegla());
    }

    @Test
    @DisplayName("RN-11: ASEGURADO permite reabrir y cambia de estado")
    void deberiaReabrir_cuandoEstaAsegurado() {
        Equipo equipo = equipo(EstadoEquipo.ASEGURADO);

        equipo.reabrir();

        assertEquals(EstadoEquipo.ABIERTO, equipo.getEstado());
    }

    private Equipo equipo(EstadoEquipo estado) {
        Equipo equipo = new Equipo();
        equipo.setEstado(estado);
        return equipo;
    }
}
