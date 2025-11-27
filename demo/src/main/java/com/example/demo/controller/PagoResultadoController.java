package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Inscripciones;
import com.example.demo.model.Pago;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.PagoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PagoResultadoController {

    private final PagoRepository pagoRepository;
    private final InscripcionRepository inscripcionRepository;

    @GetMapping("/pago-resultado")
    public String mostrarResultadoPago(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long payment_id,
            @RequestParam(required = false) String external_reference,
            @RequestParam(required = false) Long preference_id,
            Model model) {
        
        try {
            log.info("📄 Mostrando resultado de pago - Status: {}, Payment ID: {}, External Ref: {}", 
                status, payment_id, external_reference);
            
            // Buscar el pago en la base de datos
            Pago pago = null;
            if (external_reference != null) {
                pago = pagoRepository.findByExternalReference(external_reference).orElse(null);
            } else if (payment_id != null) {
                pago = pagoRepository.findByPaymentId(payment_id).orElse(null);
            }
            
            // Determinar el tipo de mensaje según el status
            String tipo = "warning";
            String mensaje = "Tu pago está siendo procesado";
            
            if ("success".equals(status) || "approved".equals(status)) {
                tipo = "success";
                mensaje = "¡Pago realizado con éxito!";
            } else if ("failure".equals(status) || "rejected".equals(status)) {
                tipo = "error";
                mensaje = "El pago no pudo ser procesado";
            } else if ("pending".equals(status)) {
                tipo = "warning";
                mensaje = "Tu pago está pendiente de aprobación";
            }
            
            model.addAttribute("tipo", tipo);
            model.addAttribute("mensaje", mensaje);
            
            // Si encontramos el pago, agregarlo al modelo
            if (pago != null) {
                model.addAttribute("pago", pago);
                model.addAttribute("oferta", pago.getOferta());
                model.addAttribute("paymentId", payment_id);
                
                // Buscar la inscripción si existe
                Inscripciones inscripcion = pago.getInscripcion();
                if (inscripcion != null) {
                    model.addAttribute("inscripcion", inscripcion);
                }
                
                log.info("✅ Pago encontrado - Estado: {}, Oferta: {}", 
                    pago.getEstadoPago(), pago.getOferta().getNombre());
            } else {
                log.warn("⚠️ No se encontró el pago en la base de datos");
            }
            
        } catch (Exception e) {
            log.error("❌ Error al mostrar resultado de pago", e);
            model.addAttribute("tipo", "error");
            model.addAttribute("mensaje", "Error al obtener información del pago");
        }
        
        return "pago-resultado";
    }
}
