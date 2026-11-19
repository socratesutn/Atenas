package ar.edu.utn.frc.atenas.commons.seguridad;

/**
 * Roles de la aplicación. Se usan sin el prefijo {@code ROLE_}: el conversor
 * de JWT es quien lo agrega al construir las autoridades de Spring Security.
 */
public final class Roles {

    public static final String PREFIJO_ROL = "ROLE_";

    public static final String JEFE_CATEDRA = "JEFE_CATEDRA";
    public static final String DOCENTE = "DOCENTE";
    public static final String ALUMNO = "ALUMNO";
    public static final String SERVICIO = "SERVICIO";

    private Roles() {
    }
}
