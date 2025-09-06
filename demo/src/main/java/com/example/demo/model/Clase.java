package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
public class Clase {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_clase", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID idClase;
    private String titulo;
    private String descripcion;
    private LocalDateTime inicio;
    //temporalmente
    private String permisos;
    
    private Boolean asistenciaAutomatica;
    private Boolean preguntasAleatorias;
    private Integer cantidadPreguntas;
    
    @ManyToOne
    @JoinColumn(name = "curso_id")
    private Curso curso;
    
    @ManyToOne
    @JoinColumn(name = "docente_dni")
    private Docente docente;
    
    @ManyToOne
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;
}
