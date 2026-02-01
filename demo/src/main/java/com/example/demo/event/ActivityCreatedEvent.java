package com.example.demo.event;

import java.time.LocalDateTime;

public class ActivityCreatedEvent {
    private final Long ofertaId;
    private final String actividadId;
    private final String tipo; // "ACTIVIDAD", "EXAMEN", "CLASE"
    private final LocalDateTime deadline;
    private final String titulo;

    public ActivityCreatedEvent(Long ofertaId, Object actividadId, String tipo, LocalDateTime deadline, String titulo) {
        this.ofertaId = ofertaId;
        this.actividadId = actividadId.toString();
        this.tipo = tipo;
        this.deadline = deadline;
        this.titulo = titulo;
    }

    public Long getOfertaId() {
        return ofertaId;
    }

    public String getActividadId() {
        return actividadId;
    }

    public String getTipo() {
        return tipo;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }
    
    public String getTitulo() {
        return titulo;
    }
}
