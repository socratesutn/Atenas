package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.client.KeycloakAdminClient;
import ar.edu.utn.frc.atenas.catedra.dto.CrearEstudianteRequest;
import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.catedra.entity.Estudiante;
import ar.edu.utn.frc.atenas.catedra.entity.Integrante;
import ar.edu.utn.frc.atenas.catedra.estado.EstadoEquipo;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionLectivaRepository;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionRepository;
import ar.edu.utn.frc.atenas.catedra.repository.CursoLectivoRepository;
import ar.edu.utn.frc.atenas.catedra.repository.EquipoRepository;
import ar.edu.utn.frc.atenas.catedra.repository.EstudianteRepository;
import ar.edu.utn.frc.atenas.catedra.repository.IntegranteRepository;
import ar.edu.utn.frc.atenas.commons.error.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class EstudianteServiceIntegrationTest {

    private static final String LEGAJO = "900001";
    private static final String MAIL = LEGAJO + "@sistemas.frc.utn.edu.ar";

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private IntegranteRepository integranteRepository;

    @Autowired
    private ComisionLectivaRepository comisionLectivaRepository;

    @Autowired
    private ComisionRepository comisionRepository;

    @Autowired
    private CursoLectivoRepository cursoLectivoRepository;

    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void limpiar() {
        integranteRepository.deleteAll();
        equipoRepository.deleteAll();
        estudianteRepository.deleteAll();
        comisionLectivaRepository.deleteAll();
        comisionRepository.deleteAll();
        cursoLectivoRepository.deleteAll();
    }

    @Test
    @DisplayName("RN-03: si Keycloak falla, la transacción no deja el estudiante guardado")
    void deberiaRevertirAlta_cuandoKeycloakFalla() {
        ComisionLectiva comisionLectiva = crearComisionLectiva();
        when(keycloakAdminClient.crearUsuario(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("Keycloak caído"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> estudianteService.crear(
                        new CrearEstudianteRequest(LEGAJO, "Alumno", "Prueba", comisionLectiva.getId())));

        assertEquals(0, estudianteRepository.count());
    }

    @Test
    @DisplayName("RN-05: si Keycloak falla en la baja, el estudiante sigue activo")
    void deberiaRevertirBaja_cuandoKeycloakFalla() {
        ComisionLectiva comisionLectiva = crearComisionLectiva();
        Estudiante estudiante = new Estudiante(LEGAJO, "Alumno", "Prueba", MAIL, comisionLectiva);
        estudiante.setSub("sub-integracion");
        estudianteRepository.saveAndFlush(estudiante);
        Long estudianteId = estudiante.getId();

        doThrow(new ServicioNoDisponibleException("Keycloak caído"))
                .when(keycloakAdminClient).deshabilitarUsuario("sub-integracion");

        assertThrows(ServicioNoDisponibleException.class,
                () -> estudianteService.darDeBaja(estudianteId));

        Estudiante guardado = estudianteRepository.findById(estudianteId).orElseThrow();
        assertTrue(guardado.isActivo());
    }

    @Test
    @DisplayName("RN-05: la baja cierra la participación y reasigna al representante")
    void deberiaCerrarParticipacionYReasignar_cuandoTieneEquipoAbierto() {
        ComisionLectiva comisionLectiva = crearComisionLectiva();
        Estudiante representante = crearEstudiante("900001", "sub-representante", comisionLectiva);
        Estudiante segundo = crearEstudiante("900002", "sub-segundo", comisionLectiva);

        Equipo equipo = new Equipo("Equipo integración", 2, 3, comisionLectiva);
        equipo.setRepresentante(representante);
        equipo = equipoRepository.saveAndFlush(equipo);
        Integrante participacionRepresentante = integranteRepository.saveAndFlush(
                new Integrante(equipo, representante, Instant.now().minusSeconds(10)));
        integranteRepository.saveAndFlush(new Integrante(equipo, segundo, Instant.now()));

        estudianteService.darDeBaja(representante.getId());

        Estudiante representanteGuardado = estudianteRepository.findById(representante.getId()).orElseThrow();
        Integrante participacionGuardada = integranteRepository
                .findById(participacionRepresentante.getId()).orElseThrow();
        Equipo equipoGuardado = equipoRepository.findById(equipo.getId()).orElseThrow();

        assertFalse(representanteGuardado.isActivo());
        assertNotNull(participacionGuardada.getFechaBaja());
        assertEquals(segundo.getId(), equipoGuardado.getRepresentante().getId());
    }

    private Estudiante crearEstudiante(String legajo, String sub, ComisionLectiva comisionLectiva) {
        Estudiante estudiante = new Estudiante(
                legajo,
                "Alumno",
                "Prueba",
                legajo + "@sistemas.frc.utn.edu.ar",
                comisionLectiva);
        estudiante.setSub(sub);
        return estudianteRepository.saveAndFlush(estudiante);
    }

    private ComisionLectiva crearComisionLectiva() {
        CursoLectivo curso = cursoLectivoRepository.saveAndFlush(new CursoLectivo(
                2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15)));
        Comision comision = comisionRepository.saveAndFlush(new Comision("Comisión integración"));
        return comisionLectivaRepository.saveAndFlush(new ComisionLectiva(comision, curso));
    }
}
