package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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
public class Carpeta extends Actividad {
    
    @OneToMany(mappedBy = "carpeta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Archivo> archivos = new ArrayList<>();
    
    @OneToMany(mappedBy = "carpeta", cascade = CascadeType.ALL)
    private List<Material> materiales = new ArrayList<>();
    
    public Carpeta(String titulo, String descripcion, Modulo modulo) {
        this.setTitulo(titulo);
        this.setDescripcion(descripcion);
        this.setModulo(modulo);
        this.setFechaCreacion(LocalDateTime.now());
        this.setVisibilidad(true);
    }
}
