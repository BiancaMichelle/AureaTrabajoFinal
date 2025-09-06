package com.example.demo.model;

import java.time.LocalDate;
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
public class Curso extends OfertaAcademica {
    
    @OneToMany(mappedBy = "curso")
    private List<Horario> horarios;
    
    private String temario;
    
    @ManyToMany
    @JoinTable(
        name = "curso_docente",
        joinColumns = @JoinColumn(name = "curso_id"),
        inverseJoinColumns = @JoinColumn(name = "docente_id")
    )
    private List<Docente> docentes;
    
    @ManyToMany
    @JoinTable(
        name = "curso_requisitos",
        joinColumns = @JoinColumn(name = "curso_id"),
        inverseJoinColumns = @JoinColumn(name = "requisito_id")
    )
    private List<OfertaAcademica> requisitos;
    
    private Double costoCuota;
    private Double costoMora;
    private Integer nrCuotas;
    private LocalDate vencimientoCuota;
    
    @OneToMany(mappedBy = "curso")
    private List<Modulo> modulos;
    
    @OneToMany(mappedBy = "curso")
    private List<Clase> clases;
}
