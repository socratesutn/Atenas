package ar.edu.utn.frc.atenas.commons.error;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de los errores de dominio. Lleva el código HTTP que debe
 * devolver la API y, opcionalmente, el identificador de la regla de negocio
 * incumplida (por ejemplo, {@code RN-18}).
 */
public abstract class ErrorDominioException extends RuntimeException {

    private final HttpStatus status;
    private final String titulo;
    private final String regla;

    protected ErrorDominioException(HttpStatus status, String titulo, String mensaje) {
        this(status, titulo, mensaje, null);
    }

    protected ErrorDominioException(HttpStatus status, String titulo, String mensaje, String regla) {
        super(mensaje);
        this.status = status;
        this.titulo = titulo;
        this.regla = regla;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getRegla() {
        return regla;
    }
}
