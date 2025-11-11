package com.example.demo.service;

// ===============================================
// 🔒 SERVICIO DE MERCADO PAGO - COMENTADO TEMPORALMENTE
// ===============================================
// Este servicio está desactivado hasta que se active la funcionalidad de pago
// Para activarlo:
// 1. Descomentar la anotación @Service abajo
// 2. Descomentar las configuraciones en application.properties
// 3. Descomentar las líneas relacionadas en AlumnoController
// ===============================================

import com.example.demo.enums.EstadoPago;
import com.example.demo.model.Alumno;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pago;
import com.example.demo.model.Usuario;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.PagoRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.common.PhoneRequest;
import com.mercadopago.client.common.AddressRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

// ✅ COMENTADO TEMPORALMENTE - Descomentar cuando se active la funcionalidad de pago
@Service
public class MercadoPagoService {

    @Value("${mercadopago.access.token:}")
    private String accessToken;

    private final PagoRepository pagoRepository;
    private final InscripcionRepository inscripcionRepository;

    public MercadoPagoService(PagoRepository pagoRepository, InscripcionRepository inscripcionRepository) {
        this.pagoRepository = pagoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    /**
     * Inicializa la configuración de Mercado Pago
     */
    private void initMercadoPago() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    /**
     * Crea una preferencia de pago para la inscripción a un curso
     * @param usuario Usuario que se inscribe
     * @param oferta Oferta académica
     * @return URL de pago de Mercado Pago
     */
    @Transactional
    public String crearPreferenciaPago(Usuario usuario, OfertaAcademica oferta) throws MPException, MPApiException {
        try {
            initMercadoPago();

            System.out.println("📝 Creando preferencia de pago:");
            System.out.println("   Usuario: " + usuario.getNombre() + " " + usuario.getApellido());
            System.out.println("   Email: " + usuario.getCorreo());
            System.out.println("   Oferta: " + oferta.getNombre());
            System.out.println("   Costo: $" + oferta.getCostoInscripcion());

            // Validar que el costo no sea null o 0
            Double costo = oferta.getCostoInscripcion();
            if (costo == null || costo <= 0) {
                throw new RuntimeException("El costo de inscripción debe ser mayor a 0. Valor actual: " + costo);
            }

            // Crear el item de pago
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .id(oferta.getIdOferta().toString())
                    .title("Inscripción - " + oferta.getNombre())
                    .description(oferta.getDescripcion() != null && !oferta.getDescripcion().isEmpty() 
                        ? oferta.getDescripcion() 
                        : "Inscripción a curso")
                    .categoryId("education")
                    .quantity(1)
                    .currencyId("ARS")
                    .unitPrice(BigDecimal.valueOf(costo))
                    .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(itemRequest);

            // Configurar URLs de retorno
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("http://localhost:8080/pago/success")
                    .failure("http://localhost:8080/pago/failure")
                    .pending("http://localhost:8080/pago/pending")
                    .build();

            // Crear la referencia externa única
            String externalReference = "INSCRIPCION_" + usuario.getDni() + "_" + oferta.getIdOferta() + "_" + System.currentTimeMillis();

            System.out.println("🔗 URLs configuradas:");
            System.out.println("   Success: http://localhost:8080/pago/success");
            System.out.println("   Failure: http://localhost:8080/pago/failure");
            System.out.println("   Pending: http://localhost:8080/pago/pending");

            // Configurar información del pagador (opcional pero ayuda en sandbox)
            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .name(usuario.getNombre())
                    .surname(usuario.getApellido())
                    .email(usuario.getCorreo())
                    .phone(PhoneRequest.builder()
                            .areaCode("11")
                            .number(usuario.getNumTelefono())
                            .build())
                    .identification(IdentificationRequest.builder()
                            .type("DNI")
                            .number(usuario.getDni())
                            .build())
                    .build();

            // Crear la preferencia CON configuración para checkout como invitado
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .externalReference(externalReference)
                    .statementDescriptor("AUREA")
                    .binaryMode(true) // Simplifica el proceso: solo approved o rejected
                    .build();

            System.out.println("📤 Enviando request a Mercado Pago...");
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            // Guardar el registro del pago pendiente en la base de datos
            Pago pago = new Pago();
            pago.setUsuario(usuario);
            pago.setOferta(oferta);
            pago.setPreferenceId(preference.getId());
            pago.setMonto(BigDecimal.valueOf(costo));
            pago.setEstadoPago(EstadoPago.PENDIENTE);
            pago.setDescripcion("Inscripción a " + oferta.getNombre());
            pago.setExternalReference(externalReference);
            pago.setEmailPagador(usuario.getCorreo());
            pago.setNombrePagador(usuario.getNombre() + " " + usuario.getApellido());
            pago.setEsCuotaMensual(false);
            pago.setNumeroCuota(1);
            
            pagoRepository.save(pago);

            System.out.println("💳 Preferencia creada exitosamente: " + preference.getId());
            System.out.println("🔗 Init Point: " + preference.getInitPoint());
            
            return preference.getInitPoint();
            
        } catch (MPApiException e) {
            System.err.println("❌ Error de API de Mercado Pago:");
            System.err.println("   Status Code: " + e.getStatusCode());
            System.err.println("   Message: " + e.getMessage());
            if (e.getApiResponse() != null) {
                System.err.println("   Response: " + e.getApiResponse().getContent());
            }
            throw e;
        } catch (MPException e) {
            System.err.println("❌ Error de Mercado Pago:");
            System.err.println("   Message: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error inesperado:");
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al crear preferencia de pago: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa la notificación de webhook de Mercado Pago
     * @param paymentId ID del pago
     */
    @Transactional
    public void procesarNotificacionPago(Long paymentId) throws MPException, MPApiException {
        initMercadoPago();

        PaymentClient paymentClient = new PaymentClient();
        Payment payment = paymentClient.get(paymentId);

        System.out.println("📬 Notificación recibida para pago: " + paymentId);
        System.out.println("📊 Estado: " + payment.getStatus());
        System.out.println("📝 External Reference: " + payment.getExternalReference());

        // Buscar el pago en la base de datos
        Pago pago = pagoRepository.findByExternalReference(payment.getExternalReference())
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con external reference: " + payment.getExternalReference()));

        // Actualizar información del pago
        pago.setPaymentId(payment.getId());
        pago.setTipoPago(payment.getPaymentTypeId());
        pago.setMetodoPago(payment.getPaymentMethodId());
        
        if (payment.getOrder() != null) {
            pago.setMerchantOrderId(payment.getOrder().getId());
        }

        // Actualizar estado según el status de Mercado Pago
        switch (payment.getStatus()) {
            case "approved":
                pago.setEstadoPago(EstadoPago.COMPLETADO);
                if (payment.getDateApproved() != null) {
                    pago.setFechaAprobacion(LocalDateTime.ofInstant(
                        payment.getDateApproved().toInstant(), 
                        ZoneId.systemDefault()
                    ));
                }
                // Crear o activar la inscripción
                crearInscripcion(pago);
                System.out.println("✅ Pago aprobado - Inscripción creada");
                break;
                
            case "pending":
            case "in_process":
                pago.setEstadoPago(EstadoPago.PENDIENTE);
                System.out.println("⏳ Pago pendiente");
                break;
                
            case "rejected":
            case "cancelled":
                pago.setEstadoPago(EstadoPago.FALLIDO);
                System.out.println("❌ Pago rechazado/cancelado");
                break;
                
            case "refunded":
                pago.setEstadoPago(EstadoPago.REEMBOLSADO);
                System.out.println("💸 Pago reembolsado");
                break;
        }

        pagoRepository.save(pago);
    }

    /**
     * Crea la inscripción después de un pago exitoso
     * @param pago Pago aprobado
     */
    private void crearInscripcion(Pago pago) {
        // Verificar si ya existe una inscripción
        if (pago.getInscripcion() != null) {
            System.out.println("⚠️ La inscripción ya existe para este pago");
            return;
        }

        // Verificar que el usuario sea un alumno
        if (!(pago.getUsuario() instanceof Alumno)) {
            System.err.println("❌ Error: El usuario no es un alumno");
            return;
        }

        Alumno alumno = (Alumno) pago.getUsuario();

        // Crear nueva inscripción
        Inscripciones inscripcion = new Inscripciones();
        inscripcion.setAlumno(alumno);
        inscripcion.setOferta(pago.getOferta());
        inscripcion.setEstadoInscripcion(true); // Activa
        inscripcion.setFechaInscripcion(LocalDate.now());

        Inscripciones inscripcionGuardada = inscripcionRepository.save(inscripcion);
        
        // Asociar la inscripción al pago
        pago.setInscripcion(inscripcionGuardada);
        pagoRepository.save(pago);

        System.out.println("📝 Inscripción creada con ID: " + inscripcionGuardada.getIdInscripcion());
    }

    /**
     * Obtiene información de un pago por su ID
     */
    public Payment obtenerPago(Long paymentId) throws MPException, MPApiException {
        initMercadoPago();
        PaymentClient paymentClient = new PaymentClient();
        return paymentClient.get(paymentId);
    }

    /**
     * Verifica el estado de un pago por su external reference
     */
    public Pago obtenerPagoPorExternalReference(String externalReference) {
        return pagoRepository.findByExternalReference(externalReference)
                .orElse(null);
    }
}
