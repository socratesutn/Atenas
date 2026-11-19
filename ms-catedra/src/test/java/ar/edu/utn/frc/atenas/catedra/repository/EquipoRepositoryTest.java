package ar.edu.utn.frc.atenas.catedra.repository;

import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import ar.edu.utn.frc.atenas.catedra.entity.ComisionLectiva;
import ar.edu.utn.frc.atenas.catedra.entity.CursoLectivo;
import ar.edu.utn.frc.atenas.catedra.entity.Equipo;
import ar.edu.utn.frc.atenas.catedra.estado.EstadoEquipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class EquipoRepositoryTest {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private ComisionLectivaRepository comisionLectivaRepository;

    @Autowired
    private ComisionRepository comisionRepository;

    @Autowired
    private CursoLectivoRepository cursoLectivoRepository;

    @Test
    @DisplayName("La consulta de bloqueo encuentra el equipo existente")
    void deberiaEncontrarEquipo_cuandoSeBuscaParaActualizar() {
        CursoLectivo curso = cursoLectivoRepository.saveAndFlush(new CursoLectivo(
                2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 15)));
        Comision comision = comisionRepository.saveAndFlush(new Comision("Comisión 1"));
        ComisionLectiva comisionLectiva = comisionLectivaRepository.saveAndFlush(
                new ComisionLectiva(comision, curso));
        Equipo equipo = equipoRepository.saveAndFlush(
                new Equipo("Equipo 1", 2, 3, comisionLectiva));

        assertTrue(equipoRepository.findByIdForUpdate(equipo.getId()).isPresent());
        assertTrue(equipoRepository.buscarPorId(equipo.getId())
                .map(encontrado -> encontrado.getEstado() == EstadoEquipo.ABIERTO)
                .orElse(false));
    }
}
