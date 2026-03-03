package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.enums.EstadoPago;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Pago;
import com.example.demo.repository.PagoRepository;
import com.example.demo.service.MercadoPagoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PagoResultadoController {

    private final PagoRepository pagoRepository;
    private final MercadoPagoService mercadoPagoService;

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping("/pago/success")
    public String pagoSuccess(
            @RequestParam(required = false) String collection_status,
            @RequestParam(required = false) String external_reference,
            @RequestParam(required = false) Long payment_id,
            @RequestParam(required = false) Long collection_id,
            Model model) {
        
        // MercadoPago a veces envía 'collection_id' en lugar de 'payment_id'
        if (payment_id == null && collection_id != null) {
            payment_id = collection_id;
        }

        log.info("✅ Pago exitoso recibido - Status: {}, Ref: {}, PaymentID: {}", collection_status, external_reference,
                payment_id);

        // Forzar la verificación del pago y creación de inscripción inmediatamente
        if (payment_id != null) {
            log.info("🔄 Forzando verificación de pago inmediata...");
            mercadoPagoService.procesarNotificacionPago(payment_id);
        }

        // REDIRECCIÓN INTELIGENTE AL AULA O MIS-PAGOS
        try {
            java.util.Optional<Pago> pagoOpt = java.util.Optional.empty();

            if (payment_id != null) {
                pagoOpt = pagoRepository.findByPaymentId(payment_id);
            }

            if (pagoOpt.isEmpty() && external_reference != null) {
                pagoOpt = pagoRepository.findByExternalReference(external_reference);
            }

            if (pagoOpt.isPresent()) {
                Pago pago = pagoOpt.get();
                
                // Si es pago de cuota mensual, redirigir a mis-pagos
                if (Boolean.TRUE.equals(pago.getEsCuotaMensual())) {
                    log.info("💳 Pago de cuota completado, redirigiendo a mis-pagos");
                    return "redirect:/alumno/mis-pagos?pagoExitoso=true";
                }
                
                // Si es un curso/oferta (inscripción), redirigir directo al aula
                if (pago.getOferta() != null) {
                    Long ofertaId = pago.getOferta().getIdOferta();
                    log.info("🎓 Redirigiendo al aula de la oferta ID: {}", ofertaId);
                    return "redirect:/alumno/aula/" + ofertaId;
                }
            }
        } catch (Exception e) {
            log.error("⚠️ Error intentando redirigir al aula, usando fallback", e);
        }

        return "redirect:/alumno/mis-ofertas";
    }

    @GetMapping("/pago/failure")
    public String pagoFailure(
            @RequestParam(required = false) String collection_status,
            @RequestParam(required = false) String external_reference,
            Model model) {
        log.info("❌ Pago fallido recibido - Status: {}, Ref: {}", collection_status, external_reference);

        if (external_reference != null) {
            pagoRepository.findByExternalReference(external_reference).ifPresent(pago -> {
                // Solo actualizamos si estaba pendiente o en proceso
                if (pago.getEstadoPago() == EstadoPago.PENDIENTE || pago.getEstadoPago() == EstadoPago.EN_PROCESO) {
                     if ("rejected".equalsIgnoreCase(collection_status)) {
                         pago.setEstadoPago(EstadoPago.FALLIDO);
                     } else {
                         pago.setEstadoPago(EstadoPago.CANCELADO);
                     }
                     pagoRepository.save(pago);
                     log.info("🔄 Estado de pago actualizado a {} para ref {}", pago.getEstadoPago(), external_reference);
                }
            });
        }

        return "redirect:/pago-resultado?status=failure&external_reference=" + external_reference;
    }

    @GetMapping("/pago/pending")
    public String pagoPending(
            @RequestParam(required = false) String collection_status,
            @RequestParam(required = false) String external_reference,
            Model model) {
        log.info("⏳ Pago pendiente recibido - Status: {}, Ref: {}", collection_status, external_reference);
        return "redirect:/pago-resultado?status=pending&external_reference=" + external_reference;
    }

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

            // Determinar estado a mostrar tomando en cuenta el registro local
            String estadoParaMostrar = status;
            if ((estadoParaMostrar == null || estadoParaMostrar.isBlank()) && pago != null
                    && pago.getEstadoPago() != null) {
                estadoParaMostrar = pago.getEstadoPago().name().toLowerCase();
            }

            String tipo;
            String mensaje;
            switch (estadoParaMostrar != null ? estadoParaMostrar : "") {
                case "success":
                case "approved":
                case "completado":
                    tipo = "success";
                    mensaje = "¡Pago realizado con éxito!";
                    break;
                case "failure":
                case "rejected":
                case "cancelado":
                case "fallido":
                    tipo = "error";
                    mensaje = "El pago no pudo ser procesado";
                    break;
                case "pending":
                case "pendiente":
                case "en_proceso":
                    tipo = "warning";
                    mensaje = "Tu pago está pendiente de aprobación";
                    break;
                default:
                    tipo = "info";
                    mensaje = "Tu pago está siendo procesado";
                    break;
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
                    pago.getEstadoPago(),
                    pago.getOferta() != null ? pago.getOferta().getNombre() : "-");
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
