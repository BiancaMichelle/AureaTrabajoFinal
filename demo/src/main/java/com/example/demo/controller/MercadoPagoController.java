package com.example.demo.controller;

import com.example.demo.model.Pago;
import com.example.demo.service.MercadoPagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/pago")
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    public MercadoPagoController(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
    }

    /**
     * Webhook para recibir notificaciones de Mercado Pago
     * URL: http://localhost:8080/pago/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("🔔 Webhook recibido: " + payload);

            String type = (String) payload.get("type");
            
            if ("payment".equals(type)) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Long paymentId = Long.parseLong(data.get("id").toString());
                
                System.out.println("💳 Procesando pago: " + paymentId);
                mercadoPagoService.procesarNotificacionPago(paymentId);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            System.err.println("❌ Error procesando webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }

    /**
     * Página de éxito después del pago
     */
    @GetMapping("/success")
    public String pagoExitoso(
            @RequestParam(value = "collection_id", required = false) String collectionId,
            @RequestParam(value = "collection_status", required = false) String collectionStatus,
            @RequestParam(value = "payment_id", required = false) String paymentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "external_reference", required = false) String externalReference,
            @RequestParam(value = "payment_type", required = false) String paymentType,
            @RequestParam(value = "merchant_order_id", required = false) String merchantOrderId,
            @RequestParam(value = "preference_id", required = false) String preferenceId,
            @RequestParam(value = "site_id", required = false) String siteId,
            @RequestParam(value = "processing_mode", required = false) String processingMode,
            @RequestParam(value = "merchant_account_id", required = false) String merchantAccountId,
            Model model) {
        
        try {
            System.out.println("✅ Pago exitoso recibido");
            System.out.println("Payment ID: " + paymentId);
            System.out.println("Status: " + status);
            System.out.println("External Reference: " + externalReference);

            // Si viene el paymentId, procesamos la notificación
            if (paymentId != null && !paymentId.isEmpty()) {
                try {
                    mercadoPagoService.procesarNotificacionPago(Long.parseLong(paymentId));
                } catch (MPException | MPApiException e) {
                    System.err.println("Error procesando pago en success: " + e.getMessage());
                }
            }

            // Buscar el pago por external reference
            if (externalReference != null) {
                Pago pago = mercadoPagoService.obtenerPagoPorExternalReference(externalReference);
                if (pago != null) {
                    model.addAttribute("pago", pago);
                    model.addAttribute("oferta", pago.getOferta());
                    model.addAttribute("inscripcion", pago.getInscripcion());
                }
            }

            model.addAttribute("paymentId", paymentId);
            model.addAttribute("status", status);
            model.addAttribute("mensaje", "¡Pago procesado exitosamente!");
            model.addAttribute("tipo", "success");
            
            return "pago-resultado";
            
        } catch (Exception e) {
            System.err.println("❌ Error en página de éxito: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("mensaje", "Hubo un error al procesar tu pago. Por favor, contacta con soporte.");
            model.addAttribute("tipo", "error");
            return "pago-resultado";
        }
    }

    /**
     * Página de pago pendiente
     */
    @GetMapping("/pending")
    public String pagoPendiente(
            @RequestParam(value = "collection_id", required = false) String collectionId,
            @RequestParam(value = "collection_status", required = false) String collectionStatus,
            @RequestParam(value = "payment_id", required = false) String paymentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "external_reference", required = false) String externalReference,
            Model model) {
        
        System.out.println("⏳ Pago pendiente");
        System.out.println("Payment ID: " + paymentId);
        System.out.println("Status: " + status);

        model.addAttribute("paymentId", paymentId);
        model.addAttribute("status", status);
        model.addAttribute("mensaje", "Tu pago está pendiente de aprobación. Te notificaremos cuando se confirme.");
        model.addAttribute("tipo", "warning");
        
        return "pago-resultado";
    }

    /**
     * Página de pago fallido
     */
    @GetMapping("/failure")
    public String pagoFallido(
            @RequestParam(value = "collection_id", required = false) String collectionId,
            @RequestParam(value = "collection_status", required = false) String collectionStatus,
            @RequestParam(value = "payment_id", required = false) String paymentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "external_reference", required = false) String externalReference,
            Model model) {
        
        System.out.println("❌ Pago fallido");
        System.out.println("Payment ID: " + paymentId);
        System.out.println("Status: " + status);

        model.addAttribute("paymentId", paymentId);
        model.addAttribute("status", status);
        model.addAttribute("mensaje", "Hubo un problema con tu pago. Por favor, intenta nuevamente.");
        model.addAttribute("tipo", "error");
        
        return "pago-resultado";
    }
}
