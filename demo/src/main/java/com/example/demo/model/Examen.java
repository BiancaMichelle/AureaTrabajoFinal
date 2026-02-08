package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.enums.EstadoExamen;
import com.example.demo.model.Modulo;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
    
    // Cambio: Examen usa ManyToMany para compartir pools con otros exámenes
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "examen_pool",
        joinColumns = @JoinColumn(name = "examen_id"),
        inverseJoinColumns = @JoinColumn(name = "pool_id")
    )
    private List<Pool> poolPreguntas;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "examen_modulo_rel",
        joinColumns = @JoinColumn(name = "examen_id"),
        inverseJoinColumns = @JoinColumn(name = "modulo_id")
    )
    private List<Modulo> modulosRelacionados;
}
