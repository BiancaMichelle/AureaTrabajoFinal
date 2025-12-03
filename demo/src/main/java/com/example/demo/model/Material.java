package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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
public class Material extends Actividad {
    
    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Archivo> archivos = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "docente_dni")
    private Docente docente;
    
    @ManyToOne
    @JoinColumn(name = "carpeta_id")
    private Carpeta carpeta;
}
