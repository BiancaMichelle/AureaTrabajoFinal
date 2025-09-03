package com.example.demo.model;

import java.time.*;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Asistencia {
    @Id
    private UUID idAsistencia = UUID.randomUUID();
    private LocalDate fecha;
    private LocalTime hora;
    private Integer tiempoClase;
    private int cantidadRespuestas;
    private Clase clase;

}
