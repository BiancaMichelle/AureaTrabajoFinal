package com.example.demo.enums;

public enum TipoGenero {
    MASCULINO("Masculino"),
    FEMENINO("Femenino"),
    OTRO("Otro"),
    PREFIERO_NO_DECIR("Prefiero no decir");
    
    private final String descripcion;
    
    TipoGenero(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}