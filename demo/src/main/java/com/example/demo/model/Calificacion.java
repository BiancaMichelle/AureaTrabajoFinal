package com.example.demo.model;

import java.time.LocalDate;
import java.util.UUID;

import com.example.demo.enums.TipoCalificacion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Calificacion {
    @Id
    private UUID idCalificacion = UUID.randomUUID();
    private Double nota;
    private String observaciones;
    private LocalDate fecha;
    @ManyToOne
    @JoinColumn(name = "docente_dni")
    private Docente docente;
    private TipoCalificacion tipoCalificacion;
    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

}
