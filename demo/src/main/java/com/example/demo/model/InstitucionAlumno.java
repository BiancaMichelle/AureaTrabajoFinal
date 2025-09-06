package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InstitucionAlumno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInstitucion;
    private String nombre;
    private String direccion;
    private String localidad;
    private String correoInstitucional;
    private String telefonoContacto;
    public Long getId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getId'");
    }
    

}
