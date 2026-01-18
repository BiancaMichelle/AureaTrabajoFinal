package com.example.demo.model;



import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
    
    @Column(columnDefinition = "TEXT")
    private String objetivos;
    
    @Column(columnDefinition = "TEXT")
    private String temario;
    
    @Column(columnDefinition = "TEXT")
    private String bibliografia;

    private LocalDate fechaInicioModulo;
    private LocalDate fechaFinModulo;
    
    @OneToMany(mappedBy = "modulo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Clase> clases;
    
    private Boolean visibilidad;
    
    @OneToMany(mappedBy = "modulo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Actividad> actividades;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private OfertaAcademica curso; // Ahora puede ser Curso, Formación, etc.
}
