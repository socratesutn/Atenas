package ar.edu.utn.frc.atenas.seguimiento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Presencia o ausencia de un alumno en un evento (F15, RN13).
 *
 * <p>Una sola fila por alumno y evento ({@code UNIQUE(evento_id, estudiante_sub)}): repetir
 * la carga actualiza la fila. La carga por equipo se guarda como una fila por integrante.
 * Si no hay fila, la asistencia todavía no se registró; {@code presente = false} es una
 * ausencia registrada. {@code equipoId} es el equipo del alumno al registrar ({@code null}
 * si no tenía), y es una referencia lógica a ms-catedra.</p>
 */
@Entity
@Table(name = "asistencia",
        uniqueConstraints = @UniqueConstraint(name = "uk_asistencia_evento_estudiante",
                columnNames = {"evento_id", "estudiante_sub"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(name = "estudiante_sub", nullable = false)
    private String estudianteSub;

    @Column(name = "equipo_id")
    private Long equipoId;

    @Column(nullable = false)
    private boolean presente;

    @Column(name = "registrada_por_sub", nullable = false)
    private String registradaPorSub;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;
}
