package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "instituto_logo")
public class InstitutoLogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreArchivo;

    @Column(nullable = false)
    private String tipoMime;

    @Column(nullable = false)
    private byte[] datos;

    @Column(nullable = false)
    private Long tamanio;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_instituto", unique = true)
    private Instituto instituto;
}
