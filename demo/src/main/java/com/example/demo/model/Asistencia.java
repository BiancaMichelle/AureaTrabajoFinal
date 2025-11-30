package com.example.demo.model;

import java.time.*;
import java.util.UUID;

import com.example.demo.enums.EstadoAsistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
public class Asistencia {
    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID idAsistencia;
    
    private LocalDate fecha;
    private LocalTime hora;
    private Integer tiempoClase;
    private int cantidadRespuestas;
    
    @Enumerated(EnumType.STRING)
    private EstadoAsistencia estado;
    
    @ManyToOne
    @JoinColumn(name = "clase_id", nullable = true)
    private Clase clase;
    
    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Usuario alumno; // Changed to Usuario to match Inscripciones
    
    @ManyToOne
    @JoinColumn(name = "oferta_id")
    private OfertaAcademica oferta;
}
