package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "states")
public class Provincia {
    @Id
    @Column(name = "iso2")
    private String codigo;
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "country_code")
    private Pais pais;
}
