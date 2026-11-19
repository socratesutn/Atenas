package ar.edu.utn.frc.atenas.seguimiento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Clase o presentación del calendario de una comisión lectiva (F14, RN12).
 *
 * <p>{@code comisionLectivaId} es una referencia lógica a ms-catedra: no hay clave foránea
 * entre esquemas, por eso la existencia de la comisión se valida por API al escribir.</p>
 */
@Entity
@Table(name = "evento",
        indexes = @Index(name = "ix_evento_comision", columnList = "comision_lectiva_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comision_lectiva_id", nullable = false)
    private Long comisionLectivaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEvento tipo;

    @Column(nullable = false)
    private Instant fecha;

    /** Opcional. */
    private String descripcion;
}
