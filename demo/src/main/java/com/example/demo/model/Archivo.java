package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Archivo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idArchivo;
    
    private String nombre;
    private String tipoMime;
    private Long tamano; // en bytes
    private LocalDateTime fechaSubida;
    
    @Lob
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "contenido", columnDefinition = "bytea")
    private byte[] contenido;
    
    @ManyToOne
    @JoinColumn(name = "carpeta_id")
    private Carpeta carpeta;
    
    @ManyToOne
    @JoinColumn(name = "material_id")
    private Material material;
}
