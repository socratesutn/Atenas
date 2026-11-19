package ar.edu.utn.frc.atenas.seguimiento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Repositorio público y rama de seguimiento validados para un equipo (F11, RN25).
 *
 * <p>{@code equipoId} es una referencia lógica a ms-catedra (no hay clave foránea
 * entre esquemas). La restricción {@code UNIQUE(equipo_id)} garantiza un solo
 * repositorio por equipo; vincular de nuevo reemplaza la fila existente.</p>
 */
@Entity
@Table(name = "repositorio_vinculado",
        uniqueConstraints = @UniqueConstraint(name = "uk_repositorio_equipo", columnNames = "equipo_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositorioVinculado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipo_id", nullable = false)
    private Long equipoId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String propietario;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "rama_seguimiento", nullable = false)
    private String ramaSeguimiento;

    @Column(name = "fecha_validacion", nullable = false)
    private Instant fechaValidacion;
}
