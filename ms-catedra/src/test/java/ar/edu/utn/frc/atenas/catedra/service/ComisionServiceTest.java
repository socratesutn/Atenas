package ar.edu.utn.frc.atenas.catedra.service;

import ar.edu.utn.frc.atenas.catedra.dto.ComisionResponse;
import ar.edu.utn.frc.atenas.catedra.dto.CrearComisionRequest;
import ar.edu.utn.frc.atenas.catedra.entity.Comision;
import ar.edu.utn.frc.atenas.catedra.repository.ComisionRepository;
import ar.edu.utn.frc.atenas.commons.error.ConflictoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComisionServiceTest {

    private static final String NOMBRE = "Comisión 1";

    @Mock
    private ComisionRepository comisionRepository;

    @InjectMocks
    private ComisionService comisionService;

    @Test
    @DisplayName("Crea una comisión cuando el nombre es nuevo")
    void deberiaCrearComision_cuandoNombreEsNuevo() {
        CrearComisionRequest request = new CrearComisionRequest(NOMBRE);
        when(comisionRepository.existsByNombre(NOMBRE)).thenReturn(false);
        when(comisionRepository.saveAndFlush(any(Comision.class)))
                .thenReturn(new Comision(1L, NOMBRE));

        ComisionResponse respuesta = comisionService.crear(request);

        assertEquals(1L, respuesta.id());
        assertEquals(NOMBRE, respuesta.nombre());
    }

    @Test
    @DisplayName("Rechaza una comisión duplicada por nombre con 409")
    void deberiaRechazarComision_cuandoNombreDuplicado() {
        CrearComisionRequest request = new CrearComisionRequest(NOMBRE);
        when(comisionRepository.existsByNombre(NOMBRE)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> comisionService.crear(request));
        verify(comisionRepository, never()).saveAndFlush(any(Comision.class));
    }

    @Test
    @DisplayName("Traduce a 409 la violación de unicidad detectada por la base")
    void deberiaTraducirViolacionDeUnicidad_cuandoSaveFalla() {
        CrearComisionRequest request = new CrearComisionRequest(NOMBRE);
        when(comisionRepository.existsByNombre(NOMBRE)).thenReturn(false);
        when(comisionRepository.saveAndFlush(any(Comision.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoException.class, () -> comisionService.crear(request));
    }

    @Test
    @DisplayName("Lista las comisiones existentes")
    void deberiaListarComisiones() {
        when(comisionRepository.findAll()).thenReturn(List.of(new Comision(1L, NOMBRE)));

        List<ComisionResponse> respuesta = comisionService.listar();

        assertEquals(1, respuesta.size());
        assertEquals(NOMBRE, respuesta.getFirst().nombre());
    }
}
