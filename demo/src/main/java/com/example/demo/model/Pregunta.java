package com.example.demo.model;

import java.util.List;
import java.util.UUID;

import com.example.demo.enums.TipoPregunta;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Pregunta {
    @Id
    private UUID idPregunta = UUID.randomUUID();
    private String enunciado;
    
    @Enumerated(EnumType.STRING)
    private TipoPregunta tipoPregunta;
    
    @OneToMany(mappedBy = "pregunta")
    private List<Opcion> opciones;
    
    private Float puntaje;
    
    @ManyToOne
    @JoinColumn(name = "pool_id")
    private Pool pool;
}
