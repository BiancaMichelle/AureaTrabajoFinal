package com.example.demo.model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class Pool {
    @Id
    private UUID idPool = UUID.randomUUID();
    private String nombre;
    private String descripcion;
    private Integer cantidadPreguntas;
    
    @OneToMany(mappedBy = "pool", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<Pregunta> preguntas;
    
    // Un pool puede ser usado por múltiples exámenes (relación inversa de ManyToMany)
    @ManyToMany(mappedBy = "poolPreguntas")
    private List<Examen> examenes;
}
