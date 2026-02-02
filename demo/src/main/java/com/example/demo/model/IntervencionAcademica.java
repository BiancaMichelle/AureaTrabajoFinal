package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "intervencion_academica")
public class IntervencionAcademica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario alumno;

    @ManyToOne
    @JoinColumn(name = "oferta_id")
    private OfertaAcademica oferta; // Puede ser null si es alerta general

    @Column(columnDefinition = "TEXT")
    private String motivoDetectado;

    @Column(columnDefinition = "TEXT")
    private String sugerenciaIA;

    private String tipoIntervencion; // "BAJO_RENDIMIENTO", "INASISTENCIA", "DESMOTIVACION"

    private LocalDateTime fechaCreacion;
    
    private boolean enviadaAlumno;
    
    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
