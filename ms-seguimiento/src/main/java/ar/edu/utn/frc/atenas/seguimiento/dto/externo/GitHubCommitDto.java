package ar.edu.utn.frc.atenas.seguimiento.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Un elemento de la respuesta de {@code GET /repos/{propietario}/{nombre}/commits}.
 *
 * <p>Ojo con los dos "autores": {@code commit.author} es el nombre, correo y fecha
 * escritos dentro del commit; {@code author} (primer nivel) es la cuenta de
 * GitHub asociada, y puede ser {@code null}. El usuario que atribuimos sale del
 * segundo.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommitDto(
        String sha,
        Detalle commit,
        Cuenta author,
        List<Padre> parents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Detalle(Persona author, Persona committer) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Persona(String date) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cuenta(String login) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Padre(String sha) {
    }
}
