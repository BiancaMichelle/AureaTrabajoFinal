package com.example.demo.model;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
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
    
    @Column(length = 1000)
    private String descripcion;
    
    @Column(length = 2000)
    private String mision;
    
    @Column(length = 2000)
    private String vision;
    
    @ElementCollection
    private List<String> colores;
    
    // Configuraciones automáticas
    private Boolean permisoBajaAutomatica;
    private Integer minimoAlumnoBaja;
    private Integer inactividadBaja;
    
    // Configuración de pagos
    private String moneda;
    private String cuentaBancaria;
    
    @Column(length = 1000)
    private String politicaPagos;

    // Configuración de bloqueos por mora (días de tolerancia)
    private Integer diasMoraBloqueoExamen;
    private Integer diasMoraBloqueoMaterial;
    private Integer diasMoraBloqueoActividad;
    private Integer diasMoraBloqueoAula;
    
    // Configuraciones del sistema
    private Boolean habilitarIA;
    private Boolean reportesAutomaticos;
    
    // Información de contacto
    private String direccion;
    private String telefono;
    private String email;
    
    // Redes sociales
    private String facebook;
    private String x;
    private String instagram;
    
    // Logo del instituto
    private String logoPath;
    
    // Certificaciones y avales
    @Column(length = 3000)
    private String certificacionesAvales;
    
    @OneToMany(mappedBy = "instituto")
    private List<OfertaAcademica> ofertas;
    
    @OneToMany(mappedBy = "instituto", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CarruselImagen> imagenesCarrusel;
}
