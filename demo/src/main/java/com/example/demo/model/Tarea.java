package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.enums.TipoEntrega;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tarea extends Actividad {
    private LocalDateTime limiteEntrega;
    private Boolean entregasTardias;
    private Boolean modificaciones;
    
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<TipoEntrega> tipoEntrega;
    
    private String comentarios;
}
