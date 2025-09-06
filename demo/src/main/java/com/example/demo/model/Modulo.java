package com.example.demo.model;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Modulo {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_modulo", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID idModulo;
    private String nombre;
    private String descripcion;
    private Date fechaInicioModulo;
    private Date fechaFinModulo;
    private String objetivos;
    
    @OneToMany(mappedBy = "modulo")
    private List<Clase> clases;
    
    private Boolean visibilidad;
    
    @OneToMany(mappedBy = "modulo")
    private List<Actividad> actividades;
    
    @ManyToOne
    @JoinColumn(name = "curso_id")
    private Curso curso;
}
