package com.example.demo.event;

import org.springframework.context.ApplicationEvent;

import com.example.demo.model.OfertaAcademica;

/**
 * Evento que se dispara cuando una oferta académica es finalizada
 */
public class OfertaFinalizadaEvent extends ApplicationEvent {
    
    private final OfertaAcademica oferta;
    
    public OfertaFinalizadaEvent(Object source, OfertaAcademica oferta) {
        super(source);
        this.oferta = oferta;
    }
    
    public OfertaAcademica getOferta() {
        return oferta;
    }
}
