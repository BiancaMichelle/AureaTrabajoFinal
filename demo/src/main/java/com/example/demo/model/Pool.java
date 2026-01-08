package com.example.demo.model;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pool {
    @Id
    private UUID idPool = UUID.randomUUID();
    private String nombre;
    private String descripcion;
    private Integer cantidadPreguntas;
    
    @OneToMany(mappedBy = "pool", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<Pregunta> preguntas;

    @ManyToOne
    @JoinColumn(name = "oferta_id")
    private com.example.demo.model.OfertaAcademica oferta;

    // Campos para generación por IA
    private Boolean generatedByIA = false;
    
    @jakarta.persistence.Column(length = 2000)
    private String iaRequest; // JSON con parámetros
    
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private com.example.demo.enums.IaGenerationStatus iaStatus = com.example.demo.enums.IaGenerationStatus.NONE;
    
    private String iaErrorMessage;
    
    // Un pool puede ser usado por múltiples exámenes (relación inversa de ManyToMany)
    // JsonIgnore para evitar referencia circular al serializar
    @JsonIgnore
    @ManyToMany(mappedBy = "poolPreguntas")
    private List<Examen> examenes;
}
