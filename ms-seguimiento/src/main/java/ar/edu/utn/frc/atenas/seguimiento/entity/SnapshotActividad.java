package ar.edu.utn.frc.atenas.seguimiento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Resultado de consultar la actividad de un repositorio en un sprint.
 *
 * <p>Cada consulta exitosa crea un snapshot nuevo (queda el historial) y el dashboard
 * usa el más reciente por repositorio y sprint. {@code hitoId} identifica el sprint
 * (referencia lógica a ms-documentacion). Los contadores son {@code null} solo cuando
 * el estado es {@link EstadoSnapshot#NO_DISPONIBLE}.</p>
 *
 * <p>Invariante (RN27): {@code commitsTotales = Σ commits de los integrantes + commitsSinAtribuir}.</p>
 */
@Entity
@Table(name = "snapshot_actividad",
        indexes = @Index(name = "ix_snapshot_repositorio_hito", columnList = "repositorio_id, hito_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repositorio_id", nullable = false)
    private RepositorioVinculado repositorio;

    @Column(name = "hito_id", nullable = false)
    private Long hitoId;

    @Column(name = "fecha_consulta", nullable = false)
    private Instant fechaConsulta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSnapshot estado;

    @Column(name = "commits_totales")
    private Integer commitsTotales;

    @Column(name = "commits_sin_atribuir")
    private Integer commitsSinAtribuir;
}
