package ar.edu.utn.frc.atenas.commons.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Timeouts de las llamadas HTTP salientes, configurables con
 * {@code atenas.http.timeout-conexion} y {@code atenas.http.timeout-lectura}.
 */
@ConfigurationProperties(prefix = "atenas.http")
public class AtenasHttpProperties {

    private Duration timeoutConexion = Duration.ofSeconds(2);
    private Duration timeoutLectura = Duration.ofSeconds(5);

    public Duration getTimeoutConexion() {
        return timeoutConexion;
    }

    public void setTimeoutConexion(Duration timeoutConexion) {
        this.timeoutConexion = timeoutConexion;
    }

    public Duration getTimeoutLectura() {
        return timeoutLectura;
    }

    public void setTimeoutLectura(Duration timeoutLectura) {
        this.timeoutLectura = timeoutLectura;
    }
}
