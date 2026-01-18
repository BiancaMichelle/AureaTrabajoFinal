package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEntrega;

    private LocalDateTime fechaEntrega;
    
    @Column(columnDefinition = "TEXT")
    private String contenido; // Puede ser texto o ruta de archivo
    
    private String nombreArchivo; // Nombre original del archivo si es subida
    
    private String archivoNombreGuardado; // Nombre del archivo físico en el servidor
    
    private Double calificacion;
    
    @Column(columnDefinition = "TEXT")
    private String comentarios;
    
    private boolean esTardia;

    @ManyToOne
    @JoinColumn(name = "id_tarea")
    private Tarea tarea;

    @ManyToOne
    @JoinColumn(name = "id_usuario") // Estudiante
    private Usuario estudiante;
}
