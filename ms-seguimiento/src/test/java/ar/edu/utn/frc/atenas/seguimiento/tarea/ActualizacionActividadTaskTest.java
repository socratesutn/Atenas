package ar.edu.utn.frc.atenas.seguimiento.tarea;

import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import ar.edu.utn.frc.atenas.commons.seguridad.Roles;
import ar.edu.utn.frc.atenas.seguimiento.config.ProveedorTokenServicio;
import ar.edu.utn.frc.atenas.seguimiento.service.ActualizacionActividadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RN31: tarea nocturna con token de servicio y aislamiento de fallas. */
@ExtendWith(MockitoExtension.class)
class ActualizacionActividadTaskTest {

    private static final String TOKEN = "token-de-servicio";

    @Mock
    private ActualizacionActividadService servicio;
    @Mock
    private ProveedorTokenServicio proveedorToken;
    @Mock
    private JwtDecoder decodificador;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jwtDeServicio() {
        return Jwt.withTokenValue(TOKEN)
                .header("alg", "none")
                .claim("sub", "service-account-atenas-seguimiento-job")
                .build();
    }

    @Test
    @DisplayName("RN31: la tarea llama al servicio con el rol SERVICIO en el contexto y restaura el contexto previo")
    void deberiaActualizarConRolServicio_cuandoLaTareaSeDispara() {
        Authentication anterior = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "anterior", null, List.of(new SimpleGrantedAuthority("ROLE_ALUMNO")));
        SecurityContextHolder.getContext().setAuthentication(anterior);
        when(proveedorToken.obtener()).thenReturn(TOKEN);
        when(decodificador.decode(TOKEN)).thenReturn(jwtDeServicio());
        when(servicio.actualizarTodos()).thenAnswer(invocacion -> {
            Authentication actual = SecurityContextHolder.getContext().getAuthentication();
            assertThat(actual.getAuthorities())
                    .extracting("authority")
                    .contains(Roles.PREFIJO_ROL + Roles.SERVICIO);
            return List.of();
        });

        new ActualizacionActividadTask(servicio, proveedorToken, decodificador).actualizarActividadNocturna();

        verify(servicio).actualizarTodos();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(anterior);
    }

    @Test
    @DisplayName("RN28: si no se obtiene el token de servicio, la tarea no llama al actualizador ni lanza")
    void deberiaNoLlamarAlServicio_cuandoFallaElTokenDeServicio() {
        when(proveedorToken.obtener())
                .thenThrow(new ServicioNoDisponibleException("No se pudo obtener el token de servicio"));

        new ActualizacionActividadTask(servicio, proveedorToken, decodificador).actualizarActividadNocturna();

        verifyNoInteractions(servicio, decodificador);
    }

    @Test
    @DisplayName("RN28: una falla del actualizador no se propaga desde la tarea")
    void deberiaNoPropagarLaFalla_cuandoElActualizadorFallla() {
        when(proveedorToken.obtener()).thenReturn(TOKEN);
        when(decodificador.decode(TOKEN)).thenReturn(jwtDeServicio());
        doThrow(new ServicioNoDisponibleException("Falla del actualizador")).when(servicio).actualizarTodos();

        new ActualizacionActividadTask(servicio, proveedorToken, decodificador).actualizarActividadNocturna();

        verify(servicio).actualizarTodos();
    }

    @Test
    @DisplayName("RN28: si el token de servicio no es válido, no se intenta actualizar")
    void deberiaNoActualizar_cuandoElTokenNoEsValido() {
        when(proveedorToken.obtener()).thenReturn(TOKEN);
        when(decodificador.decode(TOKEN)).thenThrow(new JwtException("Token inválido"));

        new ActualizacionActividadTask(servicio, proveedorToken, decodificador).actualizarActividadNocturna();

        verify(servicio, never()).actualizarTodos();
    }
}
