package ar.edu.utn.frc.atenas.commons.tiempo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Utilidad de zona horaria configurable mediante {@code atenas.zona-horaria}.
 * Internamente las fechas se manejan en UTC; al responder se convierten a la
 * zona de Argentina para conservar siempre el mismo offset.
 */
@Component
public class ZonaHoraria {

    public static final String ZONA_POR_DEFECTO = "America/Argentina/Cordoba";

    private final ZoneId zona;

    public ZonaHoraria(@Value("${atenas.zona-horaria:" + ZONA_POR_DEFECTO + "}") String zona) {
        this.zona = ZoneId.of(zona);
    }

    public ZoneId zona() {
        return zona;
    }

    public OffsetDateTime aOffsetDateTime(Instant instante) {
        return instante.atZone(zona).toOffsetDateTime();
    }

    public OffsetDateTime ahora() {
        return aOffsetDateTime(Instant.now());
    }
}
