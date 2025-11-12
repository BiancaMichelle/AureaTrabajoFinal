package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.enums.EstadoExamen;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Examen extends Actividad {
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private Integer tiempoRealizacion;
    private Integer cantidadIntentos;
    private Boolean calificacionAutomatica;
    private Boolean publicarNota;
    private Boolean generarPreExamen;
    
    @Enumerated(EnumType.STRING)
    private EstadoExamen estado;
    
    @OneToMany(mappedBy = "examen", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Pool> poolPreguntas;
}
