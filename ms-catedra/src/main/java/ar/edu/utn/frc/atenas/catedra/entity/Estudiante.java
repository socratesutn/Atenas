package ar.edu.utn.frc.atenas.catedra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Estudiante de una comisión lectiva. El mail se deriva del legajo y el sub se
 * completa cuando se crea la cuenta en Keycloak.
 */
@Entity
@Table(name = "estudiante")
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legajo", nullable = false, unique = true)
    private String legajo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido", nullable = false)
    private String apellido;

    @Column(name = "mail", nullable = false, unique = true)
    private String mail;

    @Column(name = "sub", unique = true)
    private String sub;

    @Column(name = "usuario_github", unique = true)
    private String usuarioGithub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comision_lectiva_id", nullable = false)
    private ComisionLectiva comisionLectiva;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    public Estudiante() {
        // Constructor requerido por JPA.
    }

    public Estudiante(String legajo, String nombre, String apellido, String mail,
                      ComisionLectiva comisionLectiva) {
        this.legajo = legajo;
        this.nombre = nombre;
        this.apellido = apellido;
        this.mail = mail;
        this.comisionLectiva = comisionLectiva;
        this.activo = true;
    }

    public Estudiante(Long id, String legajo, String nombre, String apellido, String mail,
                      String sub, String usuarioGithub, ComisionLectiva comisionLectiva,
                      boolean activo) {
        this.id = id;
        this.legajo = legajo;
        this.nombre = nombre;
        this.apellido = apellido;
        this.mail = mail;
        this.sub = sub;
        this.usuarioGithub = usuarioGithub;
        this.comisionLectiva = comisionLectiva;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLegajo() {
        return legajo;
    }

    public void setLegajo(String legajo) {
        this.legajo = legajo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getUsuarioGithub() {
        return usuarioGithub;
    }

    public void setUsuarioGithub(String usuarioGithub) {
        this.usuarioGithub = usuarioGithub;
    }

    public ComisionLectiva getComisionLectiva() {
        return comisionLectiva;
    }

    public void setComisionLectiva(ComisionLectiva comisionLectiva) {
        this.comisionLectiva = comisionLectiva;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
