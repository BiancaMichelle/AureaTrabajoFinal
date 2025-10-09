package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "carrusel_imagenes")
public class CarruselImagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nombreArchivo;
    
    @Column(nullable = false)
    private String rutaArchivo; // Mantenemos para compatibilidad si se usan archivos
    
    @Column(nullable = false)
    private String tipoMime;
    
    @Lob
    @Column(nullable = false)
    private byte[] datos;
    
    @Column(nullable = false)
    private Long tamaño;
    
    private String altText;
    
    @Column(nullable = false)
    private Integer orden;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_instituto")
    private Instituto instituto;
    
    private Boolean activa = true;
    
    private LocalDateTime fechaSubida;
    
    // Constructor adicional
    public CarruselImagen(String nombreArchivo, String tipoMime, byte[] datos, Long tamaño, Instituto instituto, Integer orden) {
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.datos = datos;
        this.tamaño = tamaño;
        this.instituto = instituto;
        this.orden = orden;
        this.activa = true;
        this.fechaSubida = LocalDateTime.now();
        this.rutaArchivo = "/api/carrusel/imagen/" + System.currentTimeMillis(); // URL virtual
    }
    
    @PrePersist
    protected void onCreate() {
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }
}