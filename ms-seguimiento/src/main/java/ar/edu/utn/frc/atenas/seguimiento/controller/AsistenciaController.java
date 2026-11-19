package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.AsistenciaResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.MiAsistenciaResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.RegistrarAsistenciaRequest;
import ar.edu.utn.frc.atenas.seguimiento.mapper.AsistenciaMapper;
import ar.edu.utn.frc.atenas.seguimiento.service.AsistenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Registro y consulta de asistencia (S03 a S06). */
@Slf4j
@RestController
@RequestMapping("/api/seguimiento")
@RequiredArgsConstructor
@Tag(name = "Asistencia", description = "Asistencia de los alumnos a los eventos de la comisión")
public class AsistenciaController {

    private final AsistenciaService servicio;
    private final ZonaHoraria zonaHoraria;

    @PutMapping("/eventos/{eventoId}/asistencias/{estudianteSub}")
    @Operation(summary = "Registrar o corregir la asistencia de un alumno",
            description = "Solo un docente de la comisión del evento. Repetir la carga actualiza el registro existente (RN13).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asistencia registrada o actualizada"),
            @ApiResponse(responseCode = "400", description = "Falta indicar si estuvo presente"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido, docente de otra comisión o alumno de otra comisión"),
            @ApiResponse(responseCode = "404", description = "El evento o el alumno no existen"),
            @ApiResponse(responseCode = "409", description = "El evento todavía no ocurrió"),
            @ApiResponse(responseCode = "503", description = "ms-catedra no responde")
    })
    public AsistenciaResponse registrarDeAlumno(@PathVariable Long eventoId,
                                                @PathVariable String estudianteSub,
                                                @Valid @RequestBody RegistrarAsistenciaRequest request) {
        log.info("S03: asistencia del alumno {} en el evento {}", estudianteSub, eventoId);
        return AsistenciaMapper.aResponse(
                servicio.registrarDeAlumno(eventoId, estudianteSub, request.presente()), zonaHoraria);
    }

    @PutMapping("/eventos/{eventoId}/equipos/{equipoId}/asistencias")
    @Operation(summary = "Registrar o corregir la asistencia de todos los integrantes de un equipo",
            description = "Se guarda una fila por integrante vigente; las existentes se actualizan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asistencia de los integrantes registrada o actualizada"),
            @ApiResponse(responseCode = "400", description = "Falta indicar si estuvieron presentes"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido, docente de otra comisión o equipo de otra comisión"),
            @ApiResponse(responseCode = "404", description = "El evento o el equipo no existen"),
            @ApiResponse(responseCode = "409", description = "El evento todavía no ocurrió o el equipo no tiene integrantes"),
            @ApiResponse(responseCode = "503", description = "ms-catedra no responde")
    })
    public List<AsistenciaResponse> registrarDeEquipo(@PathVariable Long eventoId,
                                                      @PathVariable Long equipoId,
                                                      @Valid @RequestBody RegistrarAsistenciaRequest request) {
        log.info("S04: asistencia del equipo {} en el evento {}", equipoId, eventoId);
        return AsistenciaMapper.aResponses(
                servicio.registrarDeEquipo(eventoId, equipoId, request.presente()), zonaHoraria);
    }

    @GetMapping("/eventos/{eventoId}/asistencias")
    @Operation(summary = "Consultar la asistencia de un evento",
            description = "El jefe o un docente de la comisión del evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asistencia registrada del evento"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido o docente de otra comisión"),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(responseCode = "503", description = "ms-catedra no responde")
    })
    public List<AsistenciaResponse> listarDeEvento(@PathVariable Long eventoId) {
        log.info("S05: consultar la asistencia del evento {}", eventoId);
        return AsistenciaMapper.aResponses(servicio.listarDeEvento(eventoId), zonaHoraria);
    }

    @GetMapping("/mi-asistencia")
    @Operation(summary = "Consultar la asistencia propia",
            description = "Solo un alumno, y siempre la suya: se identifica por el token (RN15).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asistencia propia, del evento más reciente al más antiguo"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido")
    })
    public List<MiAsistenciaResponse> miAsistencia() {
        log.info("S06: consultar la asistencia propia");
        return AsistenciaMapper.aMisAsistencias(servicio.miAsistencia(), zonaHoraria);
    }
}
