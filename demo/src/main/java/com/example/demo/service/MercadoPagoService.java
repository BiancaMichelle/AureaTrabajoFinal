package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.client.MPClient;
import com.example.demo.enums.EstadoPago;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pago;
import com.example.demo.model.Usuario;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.PagoRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MercadoPagoService {

    private final MPClient mpClient;
    private final PagoRepository pagoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EmailService emailService;

    public MercadoPagoService(MPClient mpClient, PagoRepository pagoRepository,
            InscripcionRepository inscripcionRepository, EmailService emailService) {
        this.mpClient = mpClient;
        this.pagoRepository = pagoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.emailService = emailService;
    }

    public ResponseDTO createPreference(ReferenceRequest inputData, Usuario usuario, OfertaAcademica oferta)
            throws MPException, MPApiException {
        log.info("creando preferencia de pago con respuesta: {}", inputData);

        // Generar número de orden único
        String orderNumber = generarNumeroOrden(usuario, oferta);
        log.info("📝 Número de orden generado: {}", orderNumber);

        try {
            // Crear preferencia en MercadoPago
            ResponseDTO response = mpClient.createPreference(inputData, orderNumber);

            // Crear registro de pago pendiente en la base de datos
            Pago pago = new Pago();
            pago.setUsuario(usuario);
            pago.setOferta(oferta);
            pago.setMonto(inputData.totalAmount());
            pago.setEstadoPago(EstadoPago.PENDIENTE);
            pago.setMetodoPago("MERCADOPAGO");
            pago.setDescripcion("Inscripción a " + oferta.getNombre());
            pago.setPreferenceId(response.preferenceId());
            pago.setExternalReference(orderNumber);
            pago.setNombrePagador(inputData.payer().name());
            pago.setEmailPagador(inputData.payer().email());
            pago.setFechaPago(LocalDateTime.now());

            pagoRepository.save(pago);
            log.info("✅ Pago pendiente creado con ID: {}", pago.getIdPago());

            return response;
        } catch (MPException e) {
            log.error("Error en crear la preferencia de pago", e);
            throw new RuntimeException("Error al crear preferencia de pago: " + e.getMessage());
        } catch (MPApiException e) {
            log.error("Error en crear el pago", e);
            throw new RuntimeException("Error en la API de MercadoPago: " + e.getMessage());
        }
    }

    /**
     * Genera un número de orden único basado en timestamp y datos del usuario
     */
    private String generarNumeroOrden(Usuario usuario, OfertaAcademica oferta) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String userId = usuario.getId() != null ? usuario.getId().toString().substring(0, 8) : "00000000";
        String ofertaId = oferta.getIdOferta().toString();
        String random = UUID.randomUUID().toString().substring(0, 4);

        return String.format("ORD-%s-%s-%s-%s", timestamp, userId, ofertaId, random);
    }

    /**
     * Procesa la notificación de pago de MercadoPago
     * Consulta el estado del pago y actualiza la base de datos
     */
    public void procesarNotificacionPago(Long paymentId) {
        try {
            log.info("💳 Procesando pago con ID: {}", paymentId);

            // Consultar el pago en MercadoPago
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(paymentId);

            log.info("📊 Estado del pago: {}", payment.getStatus());
            log.info("📝 External Reference: {}", payment.getExternalReference());

            // Buscar el pago en nuestra base de datos por external reference
            String externalRef = payment.getExternalReference();
            log.info("🔍 Buscando pago con external reference: '{}'", externalRef);

            Pago pagoLocal = pagoRepository.findByExternalReference(externalRef)
                    .orElse(null);

            if (pagoLocal == null) {
                log.error("❌ CRÍTICO: No se encontró el pago en la base de datos con external reference: '{}'",
                        externalRef);
                return;
            }

            log.info("✅ Pago encontrado en BD: ID={}, EstadoActual={}", pagoLocal.getIdPago(),
                    pagoLocal.getEstadoPago());

            // Actualizar información del pago
            pagoLocal.setPaymentId(payment.getId());
            pagoLocal.setTipoPago(payment.getPaymentTypeId());
            pagoLocal.setStatusDetail(payment.getStatusDetail());
            pagoLocal.setTransactionAmount(payment.getTransactionAmount());

            // Installments is primitive int, so no null check needed/possible
            pagoLocal.setInstallments(payment.getInstallments());

            if (payment.getCard() != null) {
                pagoLocal.setCardLastFourDigits(payment.getCard().getLastFourDigits());
                if (payment.getCard().getCardholder() != null) {
                    pagoLocal.setCardHolderName(payment.getCard().getCardholder().getName());
                }
            }

            if (payment.getIssuerId() != null) {
                pagoLocal.setIssuerId(payment.getIssuerId());
            }

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

            // Enviar comprobante por email
            enviarComprobantePago(pago);

        } catch (Exception e) {
            log.error("❌ Error al crear inscripción para pago aprobado", e);
        }
    }

    private void enviarComprobantePago(Pago pago) {
        try {
            String to = pago.getEmailPagador();
            if (to == null || to.isEmpty()) {
                to = pago.getUsuario().getCorreo();
            }

            String subject = "Comprobante de Pago - Inscripción Exitosa";
            String body = String.format("""
                    <html>
                    <body>
                        <h2>¡Pago Recibido!</h2>
                        <p>Hola %s,</p>
                        <p>Tu pago para la inscripción a <strong>%s</strong> ha sido procesado exitosamente.</p>
                        <hr>
                        <h3>Detalles de la Transacción</h3>
                        <ul>
                            <li><strong>Referencia:</strong> %s</li>
                            <li><strong>Monto:</strong> $%s</li>
                            <li><strong>Fecha:</strong> %s</li>
                            <li><strong>Método de Pago:</strong> %s</li>
                        </ul>
                        <p>Ya puedes acceder a tu curso desde la plataforma.</p>
                        <p>Gracias por confiar en nosotros.</p>
                    </body>
                    </html>
                    """,
                    pago.getNombrePagador() != null ? pago.getNombrePagador() : "Alumno",
                    pago.getOferta().getNombre(),
                    pago.getExternalReference(),
                    pago.getMonto(),
                    pago.getFechaAprobacion(),
                    pago.getTipoPago());

            emailService.sendEmail(to, subject, body);
            pago.setComprobanteEnviado(true);
            pagoRepository.save(pago);
            log.info("📧 Comprobante enviado a {}", to);

        } catch (Exception e) {
            log.error("❌ Error al enviar comprobante de pago", e);
        }
    }
}