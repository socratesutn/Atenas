package ar.edu.utn.frc.atenas.catedra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatedraApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("El contexto de ms-catedra levanta con el perfil test")
    void deberiaCargarElContexto_cuandoSeUsaElPerfilTest() {
    }

    @Test
    @DisplayName("GET /actuator/health responde 200 sin token")
    void deberiaResponderOk_cuandoSeConsultaHealthSinToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/catedra/prueba responde 401 sin token")
    void deberiaResponderNoAutorizado_cuandoSeConsultaApiSinToken() throws Exception {
        mockMvc.perform(get("/api/catedra/prueba"))
                .andExpect(status().isUnauthorized());
    }
}
