package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EstudianteDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RN01 y RN12: segunda capa de autorización por comisión. */
@ExtendWith(MockitoExtension.class)
class AccesoComisionTest {

    private static final Long COMISION = 10L;

    @Mock
    private CatedraClient catedraClient;
    @Mock
    private UsuarioActual usuarioActual;

    private AccesoComision acceso;

    @BeforeEach
    void preparar() {
        acceso = new AccesoComision(catedraClient, usuarioActual);
    }

    private static EstudianteDto alumnoDeComision(Long comision) {
        return new EstudianteDto(5L, "sub-alumno", true, comision, 2L, null);
    }

    // ---------------------------------------------------------------- exigirDocenteAsignado

    @Test
    @DisplayName("RN-12: un docente asignado a la comisión pasa la verificación")
    void deberiaPermitir_cuandoElDocenteEstaAsignado() {
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(COMISION, "sub-doc")).thenReturn(true);

        assertDoesNotThrow(() -> acceso.exigirDocenteAsignado(COMISION, "RN-12"));
    }

    @Test
    @DisplayName("RN-12: un docente no asignado a la comisión recibe 403 con la regla indicada")
    void deberiaLanzarAccesoDenegado_cuandoElDocenteNoEstaAsignado() {
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(COMISION, "sub-doc")).thenReturn(false);

        AccesoDenegadoException excepcion =
                assertThrows(AccesoDenegadoException.class, () -> acceso.exigirDocenteAsignado(COMISION, "RN-12"));

        assertThat(excepcion.getRegla()).isEqualTo("RN-12");
    }

    @Test
    @DisplayName("Sin un usuario identificable en el token se rechaza sin consultar a ms-catedra")
    void deberiaLanzarAccesoDenegado_cuandoElTokenNoTieneSub() {
        when(usuarioActual.sub()).thenReturn(null);

        assertThrows(AccesoDenegadoException.class, () -> acceso.exigirDocenteAsignado(COMISION, "RN-12"));

        verifyNoInteractions(catedraClient);
    }

    @Test
    @DisplayName("RN-12: si ms-catedra no responde no se concede el permiso: 503")
    void deberiaPropagarServicioNoDisponible_cuandoMsCatedraNoResponde() {
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(COMISION, "sub-doc"))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra caído"));

        assertThrows(ServicioNoDisponibleException.class, () -> acceso.exigirDocenteAsignado(COMISION, "RN-12"));
    }

    // ---------------------------------------------------------------- exigirJefeODocente

    @Test
    @DisplayName("RN-01: esJefe refleja el rol del token para decidir si se puede degradar")
    void deberiaInformarSiElSolicitanteEsJefe() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);

        assertThat(acceso.esJefe()).isTrue();
    }

    @Test
    @DisplayName("RN-01: el jefe accede a cualquier comisión sin consultar a ms-catedra")
    void deberiaPermitirAlJefe_sinConsultarACatedra() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);

        assertDoesNotThrow(() -> acceso.exigirJefeODocente(COMISION));

        verifyNoInteractions(catedraClient);
    }

    @Test
    @DisplayName("RN-01: un docente de otra comisión no accede")
    void deberiaLanzarAccesoDenegado_cuandoElDocenteEsDeOtraComision() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(COMISION, "sub-doc")).thenReturn(false);

        AccesoDenegadoException excepcion =
                assertThrows(AccesoDenegadoException.class, () -> acceso.exigirJefeODocente(COMISION));

        assertThat(excepcion.getRegla()).isEqualTo("RN-01");
    }

    // ---------------------------------------------------------------- exigirJefeDocenteOAlumno

    @Test
    @DisplayName("RN-01: el jefe consulta el calendario de cualquier comisión")
    void deberiaPermitirAlJefe_alConsultarElCalendario() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);

        assertDoesNotThrow(() -> acceso.exigirJefeDocenteOAlumno(COMISION));

        verifyNoInteractions(catedraClient);
    }

    @Test
    @DisplayName("RN-01: un docente asignado consulta el calendario de su comisión")
    void deberiaPermitirAlDocenteAsignado_alConsultarElCalendario() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(true);
        when(usuarioActual.sub()).thenReturn("sub-doc");
        when(catedraClient.docenteAsignado(COMISION, "sub-doc")).thenReturn(true);

        assertDoesNotThrow(() -> acceso.exigirJefeDocenteOAlumno(COMISION));
    }

    @Test
    @DisplayName("RN-01: un alumno de la comisión consulta el calendario (I03)")
    void deberiaPermitirAlAlumno_deLaComision() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(false);
        when(usuarioActual.sub()).thenReturn("sub-alumno");
        when(catedraClient.obtenerEstudiantePorSub("sub-alumno")).thenReturn(alumnoDeComision(COMISION));

        assertDoesNotThrow(() -> acceso.exigirJefeDocenteOAlumno(COMISION));
    }

    @Test
    @DisplayName("RN-01: un alumno de otra comisión no consulta el calendario")
    void deberiaLanzarAccesoDenegado_cuandoElAlumnoEsDeOtraComision() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(false);
        when(usuarioActual.sub()).thenReturn("sub-alumno");
        when(catedraClient.obtenerEstudiantePorSub("sub-alumno")).thenReturn(alumnoDeComision(99L));

        assertThrows(AccesoDenegadoException.class, () -> acceso.exigirJefeDocenteOAlumno(COMISION));
    }

    @Test
    @DisplayName("RN-01: si ms-catedra no responde al validar al alumno, 503")
    void deberiaPropagarServicioNoDisponible_cuandoFallaLaValidacionDelAlumno() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);
        when(usuarioActual.tieneRol(Roles.DOCENTE)).thenReturn(false);
        when(usuarioActual.sub()).thenReturn("sub-alumno");
        when(catedraClient.obtenerEstudiantePorSub("sub-alumno"))
                .thenThrow(new ServicioNoDisponibleException("ms-catedra caído"));

        assertThrows(ServicioNoDisponibleException.class, () -> acceso.exigirJefeDocenteOAlumno(COMISION));
    }
}
