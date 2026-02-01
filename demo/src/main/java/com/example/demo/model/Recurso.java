package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Recurso extends Actividad {
    
    private String url;
    
    @Enumerated(EnumType.STRING)
    private TipoRecurso tipo; // ENLACE, TEXTO
    
    @Column(columnDefinition = "TEXT")
    private String contenidoTexto; // Para texto enriquecido
}
