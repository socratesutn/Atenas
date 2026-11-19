package ar.edu.utn.frc.atenas.documentacion.service;

import ar.edu.utn.frc.atenas.commons.error.AccesoDenegadoException;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.error.SolicitudInvalidaException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.commons.seguridad.UsuarioActual;
import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.documentacion.client.CatedraClient;
import ar.edu.utn.frc.atenas.documentacion.dto.CrearHitoRequest;
import ar.edu.utn.frc.atenas.documentacion.dto.externo.CursoLectivoDto;
import ar.edu.utn.frc.atenas.documentacion.entity.Hito;
import ar.edu.utn.frc.atenas.documentacion.entity.TipoHito;
import ar.edu.utn.frc.atenas.documentacion.repository.HitoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HitoServiceTest {

    @Mock
    private HitoRepository hitoRepository;

    @Mock
    private CatedraClient catedraClient;

    @Mock
    private UsuarioActual usuarioActual;

    private HitoService service;

    @BeforeEach
    void preparar() {
        service = new HitoService(hitoRepository, catedraClient, usuarioActual,
                new ZonaHoraria("America/Argentina/Cordoba"));
    }

    @Test
    @DisplayName("D01: el jefe crea un hito válido")
    void deberiaCrearHito_cuandoElJefeYLasFechasSonValidas() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.obtenerCurso(2L)).thenReturn(curso());
        when(hitoRepository.save(any(Hito.class))).thenAnswer(inv -> {
            Hito hito = inv.getArgument(0);
            hito.setId(10L);
            return hito;
        });

        var response = service.crear(request("2026-04-01T00:00:00Z",
                "2026-04-30T00:00:00Z"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.tipo()).isEqualTo(TipoHito.SPRINT);
    }

    @Test
    @DisplayName("D01: un usuario que no es jefe recibe 403")
    void deberiaRechazar_cuandoNoEsJefe() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(false);

        assertThrows(AccesoDenegadoException.class,
                () -> service.crear(request("2026-04-01T00:00:00Z",
                        "2026-04-30T00:00:00Z")));
    }

    @Test
    @DisplayName("D01: fechas invertidas responden 400")
    void deberiaRechazarFechasInvertidas() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.obtenerCurso(2L)).thenReturn(curso());

        assertThrows(SolicitudInvalidaException.class,
                () -> service.crear(request("2026-05-01T00:00:00Z",
                        "2026-04-30T00:00:00Z")));
    }

    @Test
    @DisplayName("D01: fechas fuera del curso responden 400")
    void deberiaRechazarFechasFueraDelCurso() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.obtenerCurso(2L)).thenReturn(curso());

        assertThrows(SolicitudInvalidaException.class,
                () -> service.crear(request("2025-01-01T00:00:00Z",
                        "2025-01-10T00:00:00Z")));
    }

    @Test
    @DisplayName("D01: ms-catedra caído responde 503")
    void deberiaPropagarServicioNoDisponible_cuandoFallaCatedra() {
        when(usuarioActual.tieneRol(Roles.JEFE_CATEDRA)).thenReturn(true);
        when(catedraClient.obtenerCurso(2L))
                .thenThrow(new ServicioNoDisponibleException("caído"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> service.crear(request("2026-04-01T00:00:00Z",
                        "2026-04-30T00:00:00Z")));
    }

    @Test
    @DisplayName("I06: un tipo inválido responde 400")
    void deberiaRechazarTipoInvalido() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.internos(2L, "NO_EXISTE"));
    }

    @Test
    @DisplayName("D02: lista los hitos ordenados")
    void deberiaListarLosHitos() {
        Hito hito = new Hito(10L, 2L, "Sprint 1", TipoHito.SPRINT, 1,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T00:00:00Z"));
        when(hitoRepository.findByCursoLectivoIdOrderByOrdenAscIdAsc(2L))
                .thenReturn(List.of(hito));

        var response = service.listar(2L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(10L);
    }

    private CrearHitoRequest request(String inicio, String limite) {
        return new CrearHitoRequest(2L, "Sprint 1", TipoHito.SPRINT, 1,
                Instant.parse(inicio), Instant.parse(limite));
    }

    private CursoLectivoDto curso() {
        return new CursoLectivoDto(2L, 2026, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 15));
    }
}
