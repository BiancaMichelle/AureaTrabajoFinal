package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.enums.EstadoIntento;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Intento {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID idIntento;

    @ManyToOne
    private Alumno alumno;

    @ManyToOne
    private Examen examen;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Float calificacion;

    @Enumerated(EnumType.STRING)
    private EstadoIntento estado;

    @OneToMany(mappedBy = "intento", cascade = CascadeType.ALL)
    private List<RespuestaIntento> respuestas;
}
