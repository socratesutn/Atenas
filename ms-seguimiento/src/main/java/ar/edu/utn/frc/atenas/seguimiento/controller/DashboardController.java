package ar.edu.utn.frc.atenas.seguimiento.controller;

import ar.edu.utn.frc.atenas.commons.tiempo.ZonaHoraria;
import ar.edu.utn.frc.atenas.seguimiento.dto.DashboardEquipoResponse;
import ar.edu.utn.frc.atenas.seguimiento.dto.ResumenDashboardResponse;
import ar.edu.utn.frc.atenas.seguimiento.mapper.DashboardMapper;
import ar.edu.utn.frc.atenas.seguimiento.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dashboards por equipo y por comisión/curso (S09 y S10, F13). */
@Slf4j
@RestController
@RequestMapping("/api/seguimiento/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Asistencia, entregas y actividad de los equipos")
public class DashboardController {

    private final DashboardService servicio;
    private final ZonaHoraria zonaHoraria;

    @GetMapping("/equipos")
    @Operation(summary = "Resumen de equipos",
            description = "Jefe o docente de la comisión. El jefe también puede filtrar por curso lectivo (RN01).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen por equipo; entregas.disponible=false si falla ms-documentacion (RN33)"),
            @ApiResponse(responseCode = "400", description = "Falta el filtro o se indicaron los dos a la vez"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido o docente de otra comisión"),
            @ApiResponse(responseCode = "503", description = "ms-catedra no responde")
    })
    public ResumenDashboardResponse resumen(@RequestParam(required = false) Long comisionLectivaId,
                                            @RequestParam(required = false) Long cursoLectivoId) {
        log.info("S09: dashboard de equipos, comision={}, curso={}", comisionLectivaId, cursoLectivoId);
        return DashboardMapper.aResumen(
                servicio.dashboardDeEquipos(comisionLectivaId, cursoLectivoId), zonaHoraria);
    }

    @GetMapping("/equipos/{equipoId}")
    @Operation(summary = "Dashboard de un equipo",
            description = "Asistencia y actividad propias más entregas de ms-documentacion; ante su falla se degrada solo esa sección (RN33).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asistencia, entregas y actividad del equipo"),
            @ApiResponse(responseCode = "401", description = "Sin token válido"),
            @ApiResponse(responseCode = "403", description = "Rol no permitido o docente de otra comisión"),
            @ApiResponse(responseCode = "404", description = "El equipo no existe"),
            @ApiResponse(responseCode = "503", description = "ms-catedra no responde")
    })
    public DashboardEquipoResponse detalle(@PathVariable Long equipoId) {
        log.info("S10: dashboard del equipo {}", equipoId);
        return DashboardMapper.aDetalle(servicio.dashboardDeEquipo(equipoId), zonaHoraria);
    }
}
