package com.example.demo.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
<<<<<<< Updated upstream:demo/src/main/java/com/example/demo/model/InstitucionAlumno.java
public class InstitucionAlumno {
=======
@Getter
public class Institucion {
>>>>>>> Stashed changes:demo/src/main/java/com/example/demo/model/Institucion.java
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInstitucion;
    private String nombre;
    private String direccion;
    private String localidad;
    private String correoInstitucional;
    private String telefonoContacto;
    

}
