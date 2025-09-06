package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Docente extends Usuario{
    private String matricula;
    private Integer añosExperiencia; // Cambiar a Integer para permitir null
    
    @OneToOne
    @JoinColumn(name = "horario_id")
    private Horario horario;
    
    @ManyToMany
    @JoinTable(
        name = "docente_formacion", // nombre de la tabla intermedia
        joinColumns = @JoinColumn(name = "docente_id"), // FK a Docente
        inverseJoinColumns = @JoinColumn(name = "formacion_id") // FK a Formacion
    )
    private List<Formacion> formaciones = new ArrayList<>();
    
    @OneToMany(mappedBy = "docente")
    private List<Clase> clases;
}
