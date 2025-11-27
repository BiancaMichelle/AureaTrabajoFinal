package com.example.demo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.enums.EstadoPago;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Pago;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.service.MercadoPagoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class MPController {
    
    private final MercadoPagoService mercadoPagoService;
    private final PagoRepository pagoRepository;
    private final InscripcionRepository inscripcionRepository;

    // Este endpoint ya no se usa - el flujo ahora pasa por InscripcionController
    // que crea la preferencia junto con el registro de pago pendiente
    /*
    @PostMapping
    public ResponseEntity<ResponseDTO> createPreferecen(@Valid ReferenceRequest request){
        try {
            ResponseDTO response = mercadoPagoService.createPreference(request);
            return ResponseEntity.ok(new ResponseDTO(
                response.preferenceId(),
                response.redirectUrl()
            ));
        } catch(Exception e) {
            log.error("Error al crear la preferencia de pago", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    */
    
    /**
     * Webhook para recibir notificaciones de MercadoPago
     * Se activa cuando hay cambios en el estado de un pago
     */
    @PostMapping("/mercadopago/notification")
    public ResponseEntity<String> handleMercadoPagoNotification(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        
        try {
            log.info("🔔 Notificación recibida de MercadoPago - Topic: {}, ID: {}", topic, id);
            log.info("📋 Payload: {}", payload);
            
            // MercadoPago envía notificaciones con topic="payment" o "merchant_order"
            if ("payment".equals(topic) && id != null) {
                procesarNotificacionPago(id);
            }
            
            // Siempre devolver 200 OK para que MercadoPago no reintente
            return ResponseEntity.ok("Notificación procesada");
            
        } catch (Exception e) {
            log.error("❌ Error al procesar notificación de MercadoPago", e);
            // Aún así devolver 200 para evitar reintentos
            return ResponseEntity.ok("Error procesado");
        }
    }
    
    /**
     * Procesa la notificación de pago de MercadoPago
     * Consulta el estado del pago y actualiza la base de datos
     */
    private void procesarNotificacionPago(Long paymentId) {
        try {
            log.info("💳 Procesando pago con ID: {}", paymentId);
            
            // Consultar el pago en MercadoPago
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(paymentId);
            
            log.info("📊 Estado del pago: {}", payment.getStatus());
            log.info("📝 External Reference: {}", payment.getExternalReference());
            
            // Buscar el pago en nuestra base de datos por external reference
            Pago pagoLocal = pagoRepository.findByExternalReference(payment.getExternalReference())
                    .orElse(null);
            
            if (pagoLocal == null) {
                log.warn("⚠️ No se encontró el pago en la base de datos con external reference: {}", 
                    payment.getExternalReference());
                return;
            }
            
            // Actualizar información del pago
            pagoLocal.setPaymentId(payment.getId());
            pagoLocal.setTipoPago(payment.getPaymentTypeId());
            
            // Procesar según el estado del pago
            switch (payment.getStatus()) {
                case "approved":
                    procesarPagoAprobado(pagoLocal, payment);
                    break;
                    
                case "rejected":
                case "cancelled":
                    pagoLocal.setEstadoPago(EstadoPago.FALLIDO);
                    pagoRepository.save(pagoLocal);
                    log.info("❌ Pago rechazado/cancelado: {}", paymentId);
                    break;
                    
                case "in_process":
                case "pending":
                    pagoLocal.setEstadoPago(EstadoPago.PENDIENTE);
                    pagoRepository.save(pagoLocal);
                    log.info("⏳ Pago en proceso/pendiente: {}", paymentId);
                    break;
                    
                default:
                    log.warn("⚠️ Estado desconocido: {}", payment.getStatus());
            }
            
        } catch (Exception e) {
            log.error("❌ Error al procesar notificación de pago", e);
        }
    }
    
    /**
     * Procesa un pago aprobado: actualiza el estado y crea la inscripción
     */
    private void procesarPagoAprobado(Pago pago, Payment payment) {
        try {
            log.info("✅ Procesando pago aprobado - ID: {}", payment.getId());
            
            // Actualizar estado del pago
            pago.setEstadoPago(EstadoPago.COMPLETADO);
            pago.setFechaAprobacion(LocalDateTime.now());
            pago.setNumeroTransaccion(payment.getId().toString());
            pagoRepository.save(pago);
            
            log.info("💾 Pago actualizado a COMPLETADO");
            
            // Verificar si ya existe una inscripción para este pago
            if (pago.getInscripcion() != null) {
                log.info("ℹ️ Ya existe una inscripción para este pago");
                return;
            }
            
            // Crear la inscripción ahora que el pago fue exitoso
            Inscripciones nuevaInscripcion = new Inscripciones();
            nuevaInscripcion.setAlumno(pago.getUsuario());
            nuevaInscripcion.setOferta(pago.getOferta());
            nuevaInscripcion.setEstadoInscripcion(true);
            nuevaInscripcion.setFechaInscripcion(LocalDate.now());
            nuevaInscripcion.setPagoInscripcion(pago);
            nuevaInscripcion.setObservaciones("Inscripción automática por pago aprobado");
            
            inscripcionRepository.save(nuevaInscripcion);
            
            // Actualizar la relación en el pago
            pago.setInscripcion(nuevaInscripcion);
            pagoRepository.save(pago);
            
            log.info("✅ Inscripción creada exitosamente - ID: {}", nuevaInscripcion.getIdInscripcion());
            log.info("🎓 Usuario {} inscrito en {}", 
                pago.getUsuario().getNombre(), 
                pago.getOferta().getNombre());
            
            // Aquí podrías enviar un email de confirmación al usuario
            
        } catch (Exception e) {
            log.error("❌ Error al crear inscripción para pago aprobado", e);
        }
    }
}
