package ar.edu.utn.frc.atenas.seguimiento.config;

import ar.edu.utn.frc.atenas.seguimiento.service.CalculadorActividad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone el cálculo de actividad como bean sin ensuciar la clase con anotaciones de Spring.
 *
 * <p>{@link CalculadorActividad} es puro: solo recibe datos y devuelve números. Armarlo acá
 * permite que los {@code service} lo reciban por inyección y que se pueda reemplazar por un
 * mock en los tests, sin que el cálculo dependa del contenedor.</p>
 */
@Configuration
public class ConfiguracionCalculoActividad {

    @Bean
    public CalculadorActividad calculadorActividad() {
        return new CalculadorActividad();
    }
}
