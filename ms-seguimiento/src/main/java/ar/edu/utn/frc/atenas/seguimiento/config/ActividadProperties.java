package ar.edu.utn.frc.atenas.seguimiento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades del cálculo de actividad (prefijo {@code atenas.actividad}).
 *
 * <p>{@code umbralBajaContribucion} es la fracción del promedio de commits por
 * integrante por debajo de la cual se marca "baja contribución" (RN27). Por
 * defecto 0.5, es decir 1/2 del promedio. Es solo informativo.</p>
 */
@Component
@ConfigurationProperties(prefix = "atenas.actividad")
public class ActividadProperties {

    private double umbralBajaContribucion = 0.5;

    public double getUmbralBajaContribucion() {
        return umbralBajaContribucion;
    }

    public void setUmbralBajaContribucion(double umbralBajaContribucion) {
        this.umbralBajaContribucion = umbralBajaContribucion;
    }
}
