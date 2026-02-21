package com.example.demo.model;

import java.time.LocalDateTime;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Actividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idActividad;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private Boolean visibilidad;
    
    @ManyToOne
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;

    @Transient
    public String getTipoActividad() {
        if (this instanceof Examen) {
            return "EXAMEN";
        }
        if (this instanceof Tarea) {
            return "TAREA";
        }
        if (this instanceof Carpeta) {
            return "CARPETA";
        }
        if (this instanceof Material) {
            return "MATERIAL";
        }
        if (this instanceof Recurso recurso) {
            return recurso.getTipo() != null ? recurso.getTipo().name() : "RECURSO";
        }
        return "ACTIVIDAD";
    }

    @Transient
    public String getContenidoTextoActividad() {
        if (this instanceof Recurso recurso) {
            if (recurso.getTipo() == TipoRecurso.TEXTO) {
                return recurso.getContenidoTexto();
            }
        }
        return "";
    }

    @Transient
    public String getUrlActividad() {
        if (this instanceof Recurso recurso) {
            if (recurso.getTipo() == TipoRecurso.ENLACE) {
                return recurso.getUrl();
            }
        }
        return "";
    }

    @Transient
    public LocalDateTime getFechaAperturaActividad() {
        if (this instanceof Examen examen) {
            return examen.getFechaApertura();
        }
        return null;
    }
}
