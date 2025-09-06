package com.example.demo.model;

import java.util.List;

import org.stringtemplate.v4.compiler.CodeGenerator.listElement_return;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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
public class Alumno extends Usuario {
    
    @ManyToOne
    @JoinColumn(name = "institucion_id")
    private InstitucionAlumno colegioEgreso;
    private Integer añoEgreso;
    private String ultimosEstudios;
    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    private List<Calificacion> calificaciones;
    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    private List<Asistencia> asistencias;
    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    private List<Inscripciones> inscripciones;
}
