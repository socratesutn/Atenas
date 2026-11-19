package ar.edu.utn.frc.atenas.seguimiento.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parte de la respuesta de {@code GET /repos/{propietario}/{nombre}} que usamos.
 * Se ignoran las demás propiedades para no romper si GitHub agrega campos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositorioDto(
        String name,
        @JsonProperty("private") Boolean esPrivado,
        @JsonProperty("default_branch") String ramaPorDefecto,
        Propietario owner) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Propietario(String login) {
    }
}
