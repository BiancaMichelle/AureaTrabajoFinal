package com.example.demo.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.demo.service.CertificacionService;

/**
 * Listener que escucha cuando una oferta es finalizada
 * y automáticamente calcula las certificaciones
 */
@Component
public class OfertaFinalizadaListener {
    
    @Autowired
    private CertificacionService certificacionService;
    
    /**
     * Cuando una oferta es finalizada, calcular automáticamente
     * qué alumnos califican para certificación
     */
    @EventListener
    @Async
    public void onOfertaFinalizada(OfertaFinalizadaEvent event) {
        try {
            System.out.println("🎓 [EVENTO] Oferta finalizada: " + event.getOferta().getNombre());
            System.out.println("🔄 Calculando certificaciones automáticamente...");
            
            certificacionService.calcularCertificacionesAutomaticas(event.getOferta());
            
            System.out.println("✅ Certificaciones calculadas exitosamente");
        } catch (Exception e) {
            System.err.println("❌ Error al calcular certificaciones automáticas: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
