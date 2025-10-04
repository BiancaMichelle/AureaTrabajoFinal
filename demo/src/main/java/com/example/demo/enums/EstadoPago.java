package com.example.demo.enums;

public enum EstadoPago {
    PENDIENTE("Pendiente"),
    COMPLETADO("Completado"),
    FALLIDO("Fallido"),
    CANCELADO("Cancelado"),
    REEMBOLSADO("Reembolsado"),
    EN_PROCESO("En Proceso");
    
    private final String descripcion;
    
    EstadoPago(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}