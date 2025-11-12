package com.example.demo.model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
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
public class Pool {
    @Id
    private UUID idPool = UUID.randomUUID();
    private String nombre;
    private String descripcion;
    private Integer cantidadPreguntas;
    
    @OneToMany(mappedBy = "pool", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Pregunta> preguntas;
    
    @ManyToOne
    @JoinColumn(name = "examen_id")
    private Examen examen;
}
