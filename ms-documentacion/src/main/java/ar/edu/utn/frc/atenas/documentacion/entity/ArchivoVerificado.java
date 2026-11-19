package ar.edu.utn.frc.atenas.documentacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Archivo de Drive asociado a una entrega.
 *
 * Guarda el enlace y los metadatos verificados al registrar la entrega. Para
 * D06 se usan el enlace y la fecha de modificación original del archivo.
 */
@Entity
@Table(name = "archivo_verificado")
public class ArchivoVerificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false, unique = true)
    private Entrega entrega;

    @Column(nullable = false)
    private String enlace;

    @Column(name = "drive_file_id", nullable = false)
    private String driveFileId;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "modified_time_entrega", nullable = false)
    private Instant modifiedTimeEntrega;

    public ArchivoVerificado() {
    }

    public ArchivoVerificado(Long id, Entrega entrega, String enlace, String driveFileId,
                             String nombre, String mimeType, Instant modifiedTimeEntrega) {
        this.id = id;
        this.entrega = entrega;
        this.enlace = enlace;
        this.driveFileId = driveFileId;
        this.nombre = nombre;
        this.mimeType = mimeType;
        this.modifiedTimeEntrega = modifiedTimeEntrega;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Entrega getEntrega() {
        return entrega;
    }

    public void setEntrega(Entrega entrega) {
        this.entrega = entrega;
    }

    public String getEnlace() {
        return enlace;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    public String getDriveFileId() {
        return driveFileId;
    }

    public void setDriveFileId(String driveFileId) {
        this.driveFileId = driveFileId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Instant getModifiedTimeEntrega() {
        return modifiedTimeEntrega;
    }

    public void setModifiedTimeEntrega(Instant modifiedTimeEntrega) {
        this.modifiedTimeEntrega = modifiedTimeEntrega;
    }
}
