package ar.edu.utn.frc.atenas.seguimiento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * URLs base de los otros microservicios (prefijo {@code atenas.clientes}).
 *
 * <p>Los valores por defecto sirven para correr desde el IDE; el perfil {@code docker}
 * los reemplaza por los nombres de servicio de Docker Compose.</p>
 */
@Component
@ConfigurationProperties(prefix = "atenas.clientes")
public class ClientesProperties {

    private Servicio catedra = new Servicio("http://localhost:8082");
    private Servicio documentacion = new Servicio("http://localhost:8083");

    public Servicio getCatedra() {
        return catedra;
    }

    public void setCatedra(Servicio catedra) {
        this.catedra = catedra;
    }

    public Servicio getDocumentacion() {
        return documentacion;
    }

    public void setDocumentacion(Servicio documentacion) {
        this.documentacion = documentacion;
    }

    /** Datos de conexión de un servicio. */
    public static class Servicio {

        private String url;

        public Servicio() {
        }

        public Servicio(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
