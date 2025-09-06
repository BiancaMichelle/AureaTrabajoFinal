package com.example.demo.model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Instituto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idInstituto;
    private String nombreInstituto;
    private String descripcion;
    
    @ElementCollection
    private List<String> colores;
    
    private Boolean permisoBajaAutomatica;
    private Integer minimoAlumnoBaja;
    private Integer inactividadBaja;
    private String moneda;
    private String cuentaBancaria;
    private String politicaPagos;
    private Boolean habilitarIA;
    private Boolean reportesAutomaticos;
    private String direccion;
    private String telefono;
    private String email;
    private String facebook;
    private String x;
    private String instagram;
    
    @OneToMany(mappedBy = "instituto")
    private List<OfertaAcademica> ofertas;
}
