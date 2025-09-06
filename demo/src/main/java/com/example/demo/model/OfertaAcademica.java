package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.example.demo.enums.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Inheritance(strategy = InheritanceType.JOINED)
public class OfertaAcademica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOferta;
    private String nombre;
    private String descripcion;
    private String duracion;
    private Double costoInscripcion;
    
    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;
    
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String objetivo;
    private Boolean certificado;
    
    @Enumerated(EnumType.STRING)
    private EstadoOferta estado;
    
    private Integer cupos;
    private Boolean visibilidad;
    
    @ManyToMany
    @JoinTable(
        name = "oferta_categoria",
        joinColumns = @JoinColumn(name = "oferta_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private List<Categoria> categorias;
    
    @ManyToOne
    @JoinColumn(name = "instituto_id")
    private Instituto instituto;
    
    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL)
    private List<Inscripciones> inscripciones;

        public Boolean getEstaActiva() {
            boolean estaActiva;
            if(this.estado == EstadoOferta.ACTIVA && 
               (this.fechaInicio.isBefore(LocalDate.now()) || this.fechaInicio.isEqual(LocalDate.now())) &&
               (this.fechaFin.isAfter(LocalDate.now()) || this.fechaFin.isEqual(LocalDate.now())) &&
               (this.cupos == null || this.cupos > 0)) {
                estaActiva = true;
            } else {
                estaActiva = false;
            }
        return estaActiva;
    }
}
