package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
public class Docente extends Usuario{
    private String matricula;
    private Integer añosExperiencia;
    
    @OneToMany(mappedBy = "docente")
    private List<Horario> horario = new ArrayList<>();
    
    @OneToMany(mappedBy = "docente", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<DisponibilidadDocente> disponibilidades = new ArrayList<>();
    
    @ManyToMany
    @JoinTable(
        name = "docente_formacion", 
        joinColumns = @JoinColumn(name = "docente_id"), 
        inverseJoinColumns = @JoinColumn(name = "formacion_id") 
    )
    private List<Formacion> formaciones = new ArrayList<>();
    
    @OneToMany(mappedBy = "docente")
    private List<Clase> clases;

    public void addHorario(Horario h) {
        if (this.horario == null) {
            this.horario = new ArrayList<>();
        }
        this.horario.add(h);
        h.setDocente(this);
    }
    
    public void addDisponibilidad(DisponibilidadDocente disponibilidad) {
        if (this.disponibilidades == null) {
            this.disponibilidades = new ArrayList<>();
        }
        this.disponibilidades.add(disponibilidad);
        disponibilidad.setDocente(this);
    }
    
    public void removeDisponibilidad(DisponibilidadDocente disponibilidad) {
        if (this.disponibilidades != null) {
            this.disponibilidades.remove(disponibilidad);
            disponibilidad.setDocente(null);
        }
    }
    
    public void clearDisponibilidades() {
        if (this.disponibilidades != null) {
            this.disponibilidades.clear();
        }
    }
}
