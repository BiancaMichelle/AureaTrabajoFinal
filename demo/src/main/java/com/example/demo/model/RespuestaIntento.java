package com.example.demo.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
public class RespuestaIntento {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID idRespuestaIntento;

    @ManyToOne
    private Intento intento;

    @ManyToOne
    private Pregunta pregunta;

    @Column(columnDefinition = "TEXT")
    private String respuestaUsuario;

    private Float puntajeObtenido;
    private Boolean esCorrecta;
    private Boolean requiereRevisionManual;

    @Override
    public String toString() {
        return "RespuestaIntento [id=" + idRespuestaIntento + "]";
    }
}
