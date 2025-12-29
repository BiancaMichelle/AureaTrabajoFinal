package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Actividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idActividad;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private Boolean visibilidad;
    
    @ManyToOne
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;
}
