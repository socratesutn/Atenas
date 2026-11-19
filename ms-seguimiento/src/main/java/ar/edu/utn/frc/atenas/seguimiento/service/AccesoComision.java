package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EstudianteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Segunda capa de autorización por comisión (RN01): el Gateway ya filtró por rol, acá se
 * comprueba que el docente esté asignado a <i>esa</i> comisión o que el alumno pertenezca a
 * ella, consultando a ms-catedra (fuente de verdad de personas y pertenencia).
 *
 * <p>Si ms-catedra no responde, la excepción {@code ServicioNoDisponibleException} se
 * propaga y la operación termina en 503: ante la duda no se concede el permiso.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccesoComision {

    private final CatedraClient catedraClient;
    private final UsuarioActual usuarioActual;

    /** Exige que quien pide sea un docente asignado a la comisión (I04). */
    public void exigirDocenteAsignado(Long comisionLectivaId, String regla) {
        String sub = usuarioActual.sub();
        if (sub == null || !catedraClient.docenteAsignado(comisionLectivaId, sub)) {
            log.warn("Acceso denegado: el usuario {} no es docente de la comisión {}", sub, comisionLectivaId);
            throw new AccesoDenegadoException(
                    "Solo un docente asignado a la comisión puede realizar esta operación", regla);
        }
    }

    /**
     * Informa si el solicitante es jefe. Se expone para los casos en que falta la comisión
     * y hay que decidir si se puede degradar sin haber comprobado RN01: el jefe accede a
     * cualquier comisión, un docente no.
     */
    public boolean esJefe() {
        return usuarioActual.tieneRol(Roles.JEFE_CATEDRA);
    }

    /** El jefe accede a cualquier comisión; el docente, solo a las suyas. */
    public void exigirJefeODocente(Long comisionLectivaId) {
        if (esJefe()) {
            return;
        }
        exigirDocenteAsignado(comisionLectivaId, "RN-01");
    }

    /** El jefe accede a todo; el docente, a sus comisiones; el alumno, a la suya (I03). */
    public void exigirJefeDocenteOAlumno(Long comisionLectivaId) {
        if (esJefe()) {
            return;
        }
        if (usuarioActual.tieneRol(Roles.DOCENTE)) {
            exigirDocenteAsignado(comisionLectivaId, "RN-01");
            return;
        }
        String sub = usuarioActual.sub();
        if (sub == null) {
            throw new AccesoDenegadoException("No se pudo identificar al usuario", "RN-01");
        }
        EstudianteDto alumno = catedraClient.obtenerEstudiantePorSub(sub);
        if (!comisionLectivaId.equals(alumno.comisionLectivaId())) {
            log.warn("Acceso denegado: el alumno {} no pertenece a la comisión {}", sub, comisionLectivaId);
            throw new AccesoDenegadoException("El alumno no pertenece a la comisión", "RN-01");
        }
    }
}
