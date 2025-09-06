package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
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
public class Formacion extends OfertaAcademica {
    private String plan;
    
    @ManyToMany(mappedBy = "formaciones")
    private List<Docente> docentes = new ArrayList<>();
    
    private Double costoCuota;
    private Double costoMora;
    private Integer nrCuotas;
    private LocalDate vencimientoCuota;
}
