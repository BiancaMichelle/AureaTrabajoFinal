package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.MercadoPagoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class MPController {

    private final MercadoPagoService mercadoPagoService;

    // Este endpoint ya no se usa - el flujo ahora pasa por InscripcionController
    // que crea la preferencia junto con el registro de pago pendiente
    /*
     * @PostMapping
     * public ResponseEntity<ResponseDTO> createPreferecen(@Valid ReferenceRequest
     * request){
     * try {
     * ResponseDTO response = mercadoPagoService.createPreference(request);
     * return ResponseEntity.ok(new ResponseDTO(
     * response.preferenceId(),
     * response.redirectUrl()
     * ));
     * } catch(Exception e) {
     * log.error("Error al crear la preferencia de pago", e);
     * return ResponseEntity.internalServerError().build();
     * }
     * }
     * package com.example.demo.controller;
     * 
     * import java.time.LocalDate;
     * import java.time.LocalDateTime;
     * import java.util.Map;
     * 
     * import org.springframework.http.ResponseEntity;
     * import org.springframework.web.bind.annotation.PostMapping;
     * import org.springframework.web.bind.annotation.RequestBody;
     * import org.springframework.web.bind.annotation.RequestMapping;
     * import org.springframework.web.bind.annotation.RequestParam;
     * import org.springframework.web.bind.annotation.RestController;
     * 
     * import com.example.demo.enums.EstadoPago;
     * import com.example.demo.model.Inscripciones;
     * import com.example.demo.model.Pago;
     * import com.example.demo.repository.InscripcionRepository;
     * import com.example.demo.repository.PagoRepository;
     * import com.example.demo.service.MercadoPagoService;
     * import com.mercadopago.client.payment.PaymentClient;
     * import com.mercadopago.resources.payment.Payment;
     * 
     * import lombok.RequiredArgsConstructor;
     * import lombok.extern.slf4j.Slf4j;
     * 
     * @RestController
     * 
     * @RequestMapping("/api/v1/payments")
     * 
     * @RequiredArgsConstructor
     * 
     * @Slf4j
     * public class MPController {
     * 
     * private final MercadoPagoService mercadoPagoService;
     * private final PagoRepository pagoRepository;
     * private final InscripcionRepository inscripcionRepository;
     * 
     * // Este endpoint ya no se usa - el flujo ahora pasa por InscripcionController
     * // que crea la preferencia junto con el registro de pago pendiente
     * /*
     * 
     * @PostMapping
     * public ResponseEntity<ResponseDTO> createPreferecen(@Valid ReferenceRequest
     * request){
     * try {
     * ResponseDTO response = mercadoPagoService.createPreference(request);
     * return ResponseEntity.ok(new ResponseDTO(
     * response.preferenceId(),
     * response.redirectUrl()
     * ));
     * } catch(Exception e) {
     * log.error("Error al crear la preferencia de pago", e);
     * return ResponseEntity.internalServerError().build();
     * }
     * }
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
                mercadoPagoService.procesarNotificacionPago(id);
            }

            // Siempre devolver 200 OK para que MercadoPago no reintente
            return ResponseEntity.ok("Notificación procesada");

        } catch (Exception e) {
            log.error("❌ Error al procesar notificación de MercadoPago", e);
            // Aún así devolver 200 para evitar reintentos
            return ResponseEntity.ok("Error procesado");
        }
    }
}
