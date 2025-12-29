package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Table(name = "cities")
public class Ciudad {
    @Id
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String nombre;
    
    @ManyToOne
    @JoinColumn(name = "provincia_id")
    private Provincia provincia;
}
