package com.example.demo.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Opcion {
    @Id
    private UUID idOpcion = UUID.randomUUID();
    private String descripcion;
    private Boolean esCorrecta;
    
    @ManyToOne
    @JoinColumn(name = "pregunta_id")
    private Pregunta pregunta;
}
