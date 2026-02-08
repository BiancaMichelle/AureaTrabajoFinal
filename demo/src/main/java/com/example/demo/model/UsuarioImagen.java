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
@Table(name = "usuario_imagenes")
public class UsuarioImagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreArchivo;

    @Column(nullable = false)
    private String tipoMime;

    @Column(nullable = false)
    private byte[] datos;

    @Column(nullable = false)
    private Long tamano;

    private LocalDateTime fechaSubida;

    public UsuarioImagen(String nombreArchivo, String tipoMime, byte[] datos, Long tamano) {
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.datos = datos;
        this.tamano = tamano;
        this.fechaSubida = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }
}
