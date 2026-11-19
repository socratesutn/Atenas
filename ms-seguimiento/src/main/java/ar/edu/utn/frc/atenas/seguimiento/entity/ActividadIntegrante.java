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
import org.hibernate.annotations.Check;

/**
 * Commits atribuidos a un integrante dentro de un snapshot.
 *
 * <p>Se guarda una fila por integrante <b>aunque tenga 0 commits</b>: esa fila permite
 * distinguir "sin actividad" (hay fila con 0) de "sin datos" (no hay filas).
 * {@code usuarioGithub} guarda el usuario usado en esa consulta, para que el histórico
 * siga mostrando con qué datos se calculó aunque después cambie en ms-catedra.</p>
 */
@Entity
@Table(name = "actividad_integrante",
        uniqueConstraints = @UniqueConstraint(name = "uk_actividad_snapshot_estudiante",
                columnNames = {"snapshot_id", "estudiante_sub"}))
@Check(constraints = "commits >= 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActividadIntegrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private SnapshotActividad snapshot;

    @Column(name = "estudiante_sub", nullable = false)
    private String estudianteSub;

    /** Puede ser {@code null}: un docente puede asegurar un equipo aunque falte cargarlo (docs/14, A-04). */
    @Column(name = "usuario_github")
    private String usuarioGithub;

    @Column(nullable = false)
    private int commits;
}
