package com.example.demo.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Inscripciones {
    @Id
    private UUID idInscripcion = UUID.randomUUID();
    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;
    private LocalDate fechaInscripcion;
    private boolean estadoInscripcion;
    private OfertaAcademica oferta;
    private Pago pagoInscripcion;
}
