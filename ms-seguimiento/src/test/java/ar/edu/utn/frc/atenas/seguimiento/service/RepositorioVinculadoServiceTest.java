package ar.edu.utn.frc.atenas.seguimiento.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.NoEncontradoException;
import ar.edu.utn.frc.atenas.commons.error.NoProcesableException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.seguimiento.client.CatedraClient;
import ar.edu.utn.frc.atenas.seguimiento.client.GitHubClient;
import ar.edu.utn.frc.atenas.seguimiento.client.RepositorioGitHub;
import ar.edu.utn.frc.atenas.seguimiento.client.UbicacionRepositorio;
import ar.edu.utn.frc.atenas.seguimiento.dto.externo.EquipoDto;
import ar.edu.utn.frc.atenas.seguimiento.entity.RepositorioVinculado;
import ar.edu.utn.frc.atenas.seguimiento.exception.RepositorioNoAccesibleException;
import ar.edu.utn.frc.atenas.seguimiento.repository.RepositorioVinculadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositorioVinculadoServiceTest {

    private static final Long EQUIPO_ID = 7L;
    private static final String URL = "https://github.com/usuario/repo";

    @Mock
    private CatedraClient catedraClient;
    @Mock
    private GitHubClient gitHubClient;
    @Mock
    private RepositorioVinculadoRepository repositorios;
    @Mock
    private UsuarioActual usuarioActual;

    private RepositorioVinculadoService servicio;

    @BeforeEach
    void preparar() {
        // Plantilla real sobre un administrador simulado: ejecuta el bloque sin base de datos.
        TransactionTemplate transaccion = new TransactionTemplate(mock(PlatformTransactionManager.class));
        servicio = new RepositorioVinculadoService(catedraClient, gitHubClient, repositorios, usuarioActual, transaccion);
    }

    private static EquipoDto equipo(String representanteSub) {
        return new EquipoDto(EQUIPO_ID, "Equipo 1", 10L, 2L, "ASEGURADO", true, representanteSub, List.of());
    }

    private void usuarioRepresentanteDelEquipo() {
        when(usuarioActual.sub()).thenReturn("sub-rep");
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo("sub-rep"));
    }

    private void repositorioPublicoConRama(String rama, boolean existe) {
        when(gitHubClient.parsearUrl(URL)).thenReturn(new UbicacionRepositorio("usuario", "repo"));
        when(gitHubClient.repositorioPublico("usuario", "repo"))
                .thenReturn(new RepositorioGitHub("Usuario", "Repo", "main"));
        when(gitHubClient.existeRama("Usuario", "Repo", rama)).thenReturn(existe);
    }

    private void guardarDevuelveLoRecibido() {
        when(repositorios.save(any(RepositorioVinculado.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------- caso feliz y reemplazo

    @Test
    @DisplayName("RN-25: el representante vincula un repositorio público con una rama existente")
    void deberiaGuardarElRepositorio_cuandoEsElRepresentanteYTodoEsValido() {
        usuarioRepresentanteDelEquipo();
        repositorioPublicoConRama("main", true);
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.empty());
        guardarDevuelveLoRecibido();
        Instant antes = Instant.now();

        RepositorioVinculado guardado = servicio.vincular(EQUIPO_ID, URL, "main");

        assertThat(guardado.getEquipoId()).isEqualTo(EQUIPO_ID);
        // Se guardan las mayúsculas canónicas que informa GitHub, no lo que escribió el usuario.
        assertThat(guardado.getPropietario()).isEqualTo("Usuario");
        assertThat(guardado.getNombre()).isEqualTo("Repo");
        assertThat(guardado.getUrl()).isEqualTo("https://github.com/Usuario/Repo");
        assertThat(guardado.getRamaSeguimiento()).isEqualTo("main");
        assertThat(guardado.getFechaValidacion()).isBetween(antes, Instant.now());
    }

    @Test
    @DisplayName("RN-25: vincular de nuevo reemplaza el repositorio del equipo y conserva su fila")
    void deberiaReemplazarElRepositorio_cuandoElEquipoYaTeniaUno() {
        usuarioRepresentanteDelEquipo();
        repositorioPublicoConRama("main", true);
        RepositorioVinculado existente = RepositorioVinculado.builder().id(99L).equipoId(EQUIPO_ID)
                .url("https://github.com/viejo/viejo").propietario("viejo").nombre("viejo")
                .ramaSeguimiento("develop").fechaValidacion(Instant.parse("2026-10-01T00:00:00Z")).build();
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.of(existente));
        guardarDevuelveLoRecibido();

        RepositorioVinculado guardado = servicio.vincular(EQUIPO_ID, URL, "main");

        assertThat(guardado).isSameAs(existente);
        assertThat(guardado.getId()).isEqualTo(99L);
        assertThat(guardado.getPropietario()).isEqualTo("Usuario");
        assertThat(guardado.getNombre()).isEqualTo("Repo");
        assertThat(guardado.getRamaSeguimiento()).isEqualTo("main");
        assertThat(guardado.getFechaValidacion()).isAfter(Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    @DisplayName("RN-25: la rama se valida y se guarda sin espacios sobrantes")
    void deberiaQuitarLosEspacios_deLaRama() {
        usuarioRepresentanteDelEquipo();
        repositorioPublicoConRama("main", true);
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.empty());
        guardarDevuelveLoRecibido();

        RepositorioVinculado guardado = servicio.vincular(EQUIPO_ID, URL, "  main ");

        assertThat(guardado.getRamaSeguimiento()).isEqualTo("main");
    }

    @Test
    @DisplayName("RN-25: primero se verifica al representante y recién después se consulta a GitHub")
    void deberiaConsultarACatedraAntesQueAGitHub() {
        usuarioRepresentanteDelEquipo();
        repositorioPublicoConRama("main", true);
        when(repositorios.findByEquipoId(EQUIPO_ID)).thenReturn(Optional.empty());
        guardarDevuelveLoRecibido();

        servicio.vincular(EQUIPO_ID, URL, "main");

        InOrder orden = inOrder(catedraClient, gitHubClient, repositorios);
        orden.verify(catedraClient).obtenerEquipo(EQUIPO_ID);
        orden.verify(gitHubClient).repositorioPublico("usuario", "repo");
        orden.verify(gitHubClient).existeRama("Usuario", "Repo", "main");
        orden.verify(repositorios).save(any(RepositorioVinculado.class));
    }

    // ---------------------------------------------------------------- permisos (403)

    @Test
    @DisplayName("RN-25: un integrante que no es el representante recibe 403 y no se consulta a GitHub")
    void deberiaLanzarAccesoDenegado_cuandoNoEsElRepresentante() {
        when(usuarioActual.sub()).thenReturn("sub-otro");
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo("sub-rep"));

        AccesoDenegadoException excepcion =
                assertThrows(AccesoDenegadoException.class, () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        assertThat(excepcion.getRegla()).isEqualTo("RN-25");
        verifyNoInteractions(gitHubClient, repositorios);
    }

    @Test
    @DisplayName("RN-25: un equipo sin representante no permite vincular a nadie")
    void deberiaLanzarAccesoDenegado_cuandoElEquipoNoTieneRepresentante() {
        when(usuarioActual.sub()).thenReturn("sub-rep");
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo(null));

        assertThrows(AccesoDenegadoException.class, () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        verifyNoInteractions(gitHubClient, repositorios);
    }

    @Test
    @DisplayName("RN-25: sin un usuario identificable en el token se rechaza con 403")
    void deberiaLanzarAccesoDenegado_cuandoElTokenNoTieneSub() {
        when(usuarioActual.sub()).thenReturn(null);
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenReturn(equipo("sub-rep"));

        assertThrows(AccesoDenegadoException.class, () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        verifyNoInteractions(gitHubClient, repositorios);
    }

    // ---------------------------------------------------------------- ms-catedra

    @Test
    @DisplayName("Un equipo inexistente (404 de ms-catedra) se propaga y no se consulta a GitHub")
    void deberiaPropagarNoEncontrado_cuandoElEquipoNoExiste() {
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenThrow(new NoEncontradoException("No existe el equipo"));

        assertThrows(NoEncontradoException.class, () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        verifyNoInteractions(gitHubClient, repositorios);
    }

    @Test
    @DisplayName("RN-25: si ms-catedra no responde, 503 y no se vincula nada")
    void deberiaPropagarServicioNoDisponible_cuandoMsCatedraNoResponde() {
        when(catedraClient.obtenerEquipo(EQUIPO_ID)).thenThrow(new ServicioNoDisponibleException("ms-catedra caído"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        verifyNoInteractions(gitHubClient, repositorios);
    }

    // ---------------------------------------------------------------- GitHub

    @Test
    @DisplayName("Una URL mal formada se rechaza con 400 antes de consultar a GitHub")
    void deberiaPropagarSolicitudInvalida_cuandoLaUrlNoSeReconoce() {
        usuarioRepresentanteDelEquipo();
        when(gitHubClient.parsearUrl("basura")).thenThrow(new SolicitudInvalidaException("URL inválida"));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.vincular(EQUIPO_ID, "basura", "main"));

        verify(gitHubClient, never()).repositorioPublico(anyString(), anyString());
        verifyNoInteractions(repositorios);
    }

    @Test
    @DisplayName("RN-25: un repositorio privado o inexistente se rechaza con 422 y no se guarda")
    void deberiaPropagarRepositorioNoAccesible_cuandoElRepositorioEsPrivado() {
        usuarioRepresentanteDelEquipo();
        when(gitHubClient.parsearUrl(URL)).thenReturn(new UbicacionRepositorio("usuario", "repo"));
        when(gitHubClient.repositorioPublico("usuario", "repo"))
                .thenThrow(new RepositorioNoAccesibleException("El repositorio no es público"));

        assertThrows(RepositorioNoAccesibleException.class, () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        verify(gitHubClient, never()).existeRama(anyString(), anyString(), anyString());
        verifyNoInteractions(repositorios);
    }

    @Test
    @DisplayName("RN-25: el 404 de GitHub se convierte en el rechazo 422 de S07 y no se guarda")
    void deberiaRechazarComoNoAccesible_cuandoGitHubNoEncuentraElRepositorio() {
        usuarioRepresentanteDelEquipo();
        when(gitHubClient.parsearUrl(URL)).thenReturn(new UbicacionRepositorio("usuario", "repo"));
        when(gitHubClient.repositorioPublico("usuario", "repo"))
                .thenThrow(new NoEncontradoException("El repositorio no existe en GitHub"));

        RepositorioNoAccesibleException excepcion = assertThrows(RepositorioNoAccesibleException.class,
                () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        assertThat(excepcion.getRegla()).isEqualTo("RN-25");
        verify(gitHubClient, never()).existeRama(anyString(), anyString(), anyString());
        verifyNoInteractions(repositorios);
    }

    @Test
    @DisplayName("RN-25: una rama inexistente se rechaza con 422 y no se guarda")
    void deberiaLanzarNoProcesable_cuandoLaRamaNoExiste() {
        usuarioRepresentanteDelEquipo();
        repositorioPublicoConRama("inexistente", false);

        NoProcesableException excepcion = assertThrows(NoProcesableException.class,
                () -> servicio.vincular(EQUIPO_ID, URL, "inexistente"));

        assertThat(excepcion.getRegla()).isEqualTo("RN-25");
        assertThat(excepcion.getMessage()).contains("inexistente");
        verifyNoInteractions(repositorios);
    }

    @Test
    @DisplayName("RN-28: si GitHub no responde al validar el repositorio, 503 y no se guarda")
    void deberiaPropagarServicioNoDisponible_cuandoGitHubNoRespondeAlValidarElRepositorio() {
        usuarioRepresentanteDelEquipo();
        when(gitHubClient.parsearUrl(URL)).thenReturn(new UbicacionRepositorio("usuario", "repo"));
        when(gitHubClient.repositorioPublico("usuario", "repo"))
                .thenThrow(new ServicioNoDisponibleException("GitHub no respondió"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        verifyNoInteractions(repositorios);
    }

    @Test
    @DisplayName("RN-28: si GitHub no responde al buscar la rama, 503 y no se guarda")
    void deberiaPropagarServicioNoDisponible_cuandoGitHubNoRespondeAlBuscarLaRama() {
        usuarioRepresentanteDelEquipo();
        when(gitHubClient.parsearUrl(URL)).thenReturn(new UbicacionRepositorio("usuario", "repo"));
        when(gitHubClient.repositorioPublico("usuario", "repo"))
                .thenReturn(new RepositorioGitHub("Usuario", "Repo", "main"));
        when(gitHubClient.existeRama("Usuario", "Repo", "main"))
                .thenThrow(new ServicioNoDisponibleException("GitHub no respondió"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.vincular(EQUIPO_ID, URL, "main"));

        verifyNoInteractions(repositorios);
    }
}
