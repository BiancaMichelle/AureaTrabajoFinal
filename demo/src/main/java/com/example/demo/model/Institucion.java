package com.example.demo.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Institucion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID idInstitucion;
    private String nombre;
    private String direccion;
    private String localidad;
    private String correoInstitucional;
    private String telefonoContacto;
    

}
