package com.example.demo.enums;

public enum EstadoCuota {
    PENDIENTE("Pendiente"),
    PAGADA("Pagada"),
    VENCIDA("Vencida"),
    PARCIALMENTE_PAGADA("Parcialmente Pagada"),
    CANCELADA("Cancelada");
    
    private final String descripcion;
    
    EstadoCuota(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}