package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.example.demo.enums.*;

public class OfertaAcademica {
    private UUID idOferta = UUID.randomUUID();
    private String nombre;
    private String descripcion;
    private String duracion;
    private double costoInscripcion;
    private Modalidad modalidad;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String objetivo;
    private boolean certificado;
    private EstadoOferta estado;
    private Integer cupos;
    private boolean visibilidad;
    private List<Categoria> categorias;

}
