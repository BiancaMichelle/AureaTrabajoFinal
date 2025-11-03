package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
public class Inscripciones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInscripcion;

    @ManyToOne
    @JoinColumn(name = "usuario_dni")
    private Usuario alumno;
    
    @ManyToOne
    @JoinColumn(name = "oferta_id")
    private OfertaAcademica oferta;
    
    private LocalDate fechaInscripcion;
    private Boolean estadoInscripcion;
    private String observaciones;
    
    @ManyToOne
    @JoinColumn(name = "pago_id")
    private Pago pagoInscripcion;
    
    // Relaciones según el diagrama
    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL)
    private List<Cuota> cuotas;
}
