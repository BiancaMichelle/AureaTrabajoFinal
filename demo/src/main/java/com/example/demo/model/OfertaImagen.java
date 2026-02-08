package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "oferta_imagenes")
public class OfertaImagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreArchivo;

    @Column(nullable = false)
    private String tipoMime;

    // byte[] para mantener consistencia con CarruselImagen
    @Column(nullable = false)
    private byte[] datos;

    @Column(nullable = false)
    private Long tamano;

    private Boolean activa = true;

    private LocalDateTime fechaSubida;

    public OfertaImagen(String nombreArchivo, String tipoMime, byte[] datos, Long tamano) {
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.datos = datos;
        this.tamano = tamano;
        this.activa = true;
        this.fechaSubida = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }
}
