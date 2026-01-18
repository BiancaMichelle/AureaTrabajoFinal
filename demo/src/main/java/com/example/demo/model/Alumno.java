package com.example.demo.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Alumno extends Usuario {
    
    @NotBlank(message = "El colegio de egreso es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-\\.\\(\\)]+$", message = "El nombre del colegio solo puede contener letras, números y espacios")
    private String colegioEgreso;

    @NotNull(message = "El año de egreso es obligatorio")
    @Min(value = 1980, message = "El año de egreso debe ser como mínimo 1980")
    @Max(value = 2025, message = "El año de egreso debe ser como máximo 2025")
    private Integer añoEgreso;

    @NotBlank(message = "Los últimos estudios son obligatorios")
    private String ultimosEstudios;

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    private List<Calificacion> calificaciones;

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    private List<Asistencia> asistencias;

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    private List<Inscripciones> inscripciones;

    // Opt-in para recibir promociones e información relevante del instituto
    private boolean aceptaPromociones = false;
}
