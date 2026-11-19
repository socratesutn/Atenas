package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.client.GitHubClient;
import ar.edu.utn.frc.atenas.seguimiento.client.RepositorioGitHub;
import ar.edu.utn.frc.atenas.seguimiento.client.UbicacionRepositorio;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.exception.RepositorioNoAccesibleException;
import ar.edu.utn.frc.atenas.seguimiento.repository.RepositorioVinculadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

/**
 * Vinculación del repositorio de GitHub de un equipo (F11, S07, RN25).
 *
 * <p>Orden de las comprobaciones, de la más barata y más sensible a la más costosa:</p>
 * <ol>
 *   <li>El equipo existe y quien pide es su representante (consulta a ms-catedra, I01).
 *       Va primero para no revelar información sobre repositorios a quien no tiene permiso.</li>
 *   <li>La URL tiene formato válido (sin red).</li>
 *   <li>El repositorio existe y es público (GitHub).</li>
 *   <li>La rama existe (GitHub).</li>
 *   <li>Recién entonces se guarda o se reemplaza.</li>
 * </ol>
 *
 * <p>Las llamadas a ms-catedra y a GitHub se hacen <b>antes</b> de abrir la transacción;
 * este método no es transaccional y usa {@link TransactionTemplate} solo para persistir.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepositorioVinculadoService {

    private final CatedraClient catedraClient;
    private final GitHubClient gitHubClient;
    private final RepositorioVinculadoRepository repositorios;
    private final UsuarioActual usuarioActual;
    private final TransactionTemplate transaccion;

    /**
     * Valida y guarda el repositorio y la rama de seguimiento del equipo. Si el equipo ya
     * tenía uno, lo reemplaza ({@code UNIQUE(equipo_id)}).
     *
     * @throws AccesoDenegadoException  si quien pide no es el representante (403)
     * @throws NoProcesableException    si el repositorio no es público o la rama no existe (422)
     */
    public RepositorioVinculado vincular(Long equipoId, String url, String rama) {
        EquipoDto equipo = catedraClient.obtenerEquipo(equipoId);
        verificarRepresentante(equipo);

        String ramaElegida = rama.trim();
        UbicacionRepositorio ubicacion = gitHubClient.parsearUrl(url);
        RepositorioGitHub repositorio;
        try {
            repositorio = gitHubClient.repositorioPublico(ubicacion.propietario(), ubicacion.nombre());
        } catch (NoEncontradoException noExiste) {
            // El adapter informa el 404 de GitHub; para S07 tanto un repo inexistente como uno
            // privado son el mismo rechazo de dominio (422, RN25).
            throw new RepositorioNoAccesibleException(
                    "El repositorio " + ubicacion.propietario() + "/" + ubicacion.nombre()
                            + " no existe o no es público");
        }
        if (!gitHubClient.existeRama(repositorio.propietario(), repositorio.nombre(), ramaElegida)) {
            throw new NoProcesableException(
                    "La rama " + ramaElegida + " no existe en el repositorio "
                            + repositorio.propietario() + "/" + repositorio.nombre(), "RN-25");
        }

        RepositorioVinculado guardado = transaccion.execute(estado -> {
            RepositorioVinculado entidad = repositorios.findByEquipoId(equipoId)
                    .orElseGet(() -> RepositorioVinculado.builder().equipoId(equipoId).build());
            // Se guardan los nombres con las mayúsculas canónicas que informa GitHub.
            entidad.setUrl("https://github.com/" + repositorio.propietario() + "/" + repositorio.nombre());
            entidad.setPropietario(repositorio.propietario());
            entidad.setNombre(repositorio.nombre());
            entidad.setRamaSeguimiento(ramaElegida);
            entidad.setFechaValidacion(Instant.now());
            return repositorios.save(entidad);
        });

        log.info("Repositorio {}/{} (rama {}) vinculado al equipo {}",
                repositorio.propietario(), repositorio.nombre(), ramaElegida, equipoId);
        return guardado;
    }

    /** RN25: solo el representante del equipo puede vincular el repositorio. */
    private void verificarRepresentante(EquipoDto equipo) {
        String sub = usuarioActual.sub();
        if (sub == null || !sub.equals(equipo.representanteSub())) {
            log.warn("El usuario {} intentó vincular el repositorio del equipo {} sin ser su representante",
                    sub, equipo.id());
            throw new AccesoDenegadoException(
                    "Solo el representante del equipo puede vincular el repositorio", "RN-25");
        }
    }
}
