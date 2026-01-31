package com.example.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.client.MPClient;
import com.example.demo.enums.EstadoCuota;
import com.example.demo.enums.EstadoPago;
import com.example.demo.model.Curso;
import com.example.demo.model.Cuota;
import com.example.demo.model.Formacion;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pago;
import com.example.demo.model.Usuario;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.CuotaRepository;
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
    private final CuotaRepository cuotaRepository;

        public MercadoPagoService(MPClient mpClient, PagoRepository pagoRepository,
            InscripcionRepository inscripcionRepository, EmailService emailService,
            CuotaRepository cuotaRepository) {
        this.mpClient = mpClient;
        this.pagoRepository = pagoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.emailService = emailService;
        this.cuotaRepository = cuotaRepository;
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

    public ResponseDTO createPreferenceForCuota(ReferenceRequest inputData, Usuario usuario, OfertaAcademica oferta,
            Cuota cuota) {
        log.info("💵 Creando preferencia de cuota {} para inscripción {}", cuota.getNumeroCuota(),
                cuota.getInscripcion().getIdInscripcion());

        String orderNumber = generarNumeroOrden(usuario, oferta) + "-CUOTA-"
                + (cuota.getNumeroCuota() != null ? cuota.getNumeroCuota() : "1");

        try {
            ResponseDTO response = mpClient.createPreference(inputData, orderNumber);

            Pago pago = new Pago();
            pago.setUsuario(usuario);
            pago.setOferta(oferta);
            pago.setMonto(inputData.totalAmount());
            pago.setEstadoPago(EstadoPago.PENDIENTE);
            pago.setMetodoPago("MERCADOPAGO");
            pago.setDescripcion(String.format("Cuota %s - %s",
                    cuota.getNumeroCuota() != null ? cuota.getNumeroCuota() : "",
                    oferta.getNombre()));
            pago.setPreferenceId(response.preferenceId());
            pago.setExternalReference(orderNumber);
            pago.setNombrePagador(inputData.payer().name());
            pago.setEmailPagador(inputData.payer().email());
            pago.setFechaPago(LocalDateTime.now());
            pago.setEsCuotaMensual(true);
            pago.setNumeroCuota(cuota.getNumeroCuota());

            pagoRepository.save(pago);

            cuota.setPago(pago);
            cuotaRepository.save(cuota);

            log.info("✅ Pago pendiente para cuota guardado con ID {}", pago.getIdPago());

            return response;
        } catch (MPException e) {
            log.error("Error en crear la preferencia de pago para cuota", e);
            throw new RuntimeException("Error al crear preferencia de cuota: " + e.getMessage());
        } catch (MPApiException e) {
            log.error("Error en crear el pago de cuota", e);
            throw new RuntimeException("Error en la API de MercadoPago al crear cuota: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al crear preferencia de cuota", e);
            throw new RuntimeException("Error al crear preferencia de cuota: " + e.getMessage(), e);
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

            pago.setEstadoPago(EstadoPago.COMPLETADO);
            pago.setFechaAprobacion(LocalDateTime.now());
            pago.setNumeroTransaccion(payment.getId().toString());
            pagoRepository.save(pago);

            log.info("💾 Pago actualizado a COMPLETADO");

            if (Boolean.TRUE.equals(pago.getEsCuotaMensual())) {
                actualizarCuotasDePago(pago);
                enviarComprobantePago(pago);
                return;
            }

            if (pago.getInscripcion() != null) {
                log.info("ℹ️ Ya existe una inscripción para este pago");
                generarCuotasParaInscripcion(pago.getInscripcion());
                return;
            }

            Inscripciones nuevaInscripcion = new Inscripciones();
            nuevaInscripcion.setAlumno(pago.getUsuario());
            nuevaInscripcion.setOferta(pago.getOferta());
            nuevaInscripcion.setEstadoInscripcion(true);
            nuevaInscripcion.setFechaInscripcion(LocalDate.now());
            nuevaInscripcion.setPagoInscripcion(pago);
            nuevaInscripcion.setObservaciones("Inscripción automática por pago aprobado");

            inscripcionRepository.save(nuevaInscripcion);

            pago.setInscripcion(nuevaInscripcion);
            pagoRepository.save(pago);

                generarCuotasParaInscripcion(nuevaInscripcion);

            log.info("✅ Inscripción creada exitosamente - ID: {}", nuevaInscripcion.getIdInscripcion());
            log.info("🎓 Usuario {} inscrito en {}",
                    pago.getUsuario().getNombre(),
                    pago.getOferta().getNombre());

            enviarComprobantePago(pago);

        } catch (Exception e) {
            log.error("❌ Error al crear inscripción para pago aprobado", e);
        }
    }

    private void actualizarCuotasDePago(Pago pago) {
        try {
            List<Cuota> cuotasVinculadas = new ArrayList<>(cuotaRepository.findByPagoIdPago(pago.getIdPago()));

            if (cuotasVinculadas.isEmpty() && pago.getInscripcion() != null) {
                List<Cuota> cuotasInscripcion = cuotaRepository
                        .findByInscripcionIdInscripcion(pago.getInscripcion().getIdInscripcion());
                for (Cuota cuota : cuotasInscripcion) {
                    if (pago.getNumeroCuota() != null && pago.getNumeroCuota().equals(cuota.getNumeroCuota())) {
                        cuotasVinculadas.add(cuota);
                    }
                }
            }

            if (cuotasVinculadas.isEmpty()) {
                log.warn("⚠️ No se encontraron cuotas vinculadas al pago {}", pago.getIdPago());
                return;
            }

            for (Cuota cuota : cuotasVinculadas) {
                BigDecimal montoCuota = Optional.ofNullable(cuota.getMonto()).orElse(BigDecimal.ZERO);
                BigDecimal montoPagadoPrevio = Optional.ofNullable(cuota.getMontoPagado()).orElse(BigDecimal.ZERO);
                BigDecimal montoDelPago = Optional.ofNullable(pago.getMonto()).orElse(BigDecimal.ZERO);

                BigDecimal nuevoTotalPagado = montoPagadoPrevio.add(montoDelPago);
                cuota.setMontoPagado(nuevoTotalPagado);
                cuota.setFechaPago(LocalDate.now());
                cuota.setPago(pago);

                if (nuevoTotalPagado.compareTo(montoCuota) >= 0) {
                    cuota.setEstado(EstadoCuota.PAGADA);
                } else {
                    cuota.setEstado(EstadoCuota.PARCIALMENTE_PAGADA);
                }

                cuotaRepository.save(cuota);
                log.info("💰 Cuota {} actualizada al estado {}", cuota.getIdCuota(), cuota.getEstado());
            }

        } catch (Exception e) {
            log.error("❌ Error al actualizar cuotas para el pago {}", pago.getIdPago(), e);
        }
    }

    public void generarCuotasParaInscripcion(Inscripciones inscripcion) {
        try {
            if (inscripcion == null || inscripcion.getIdInscripcion() == null) {
                log.warn("⚠️ No se generaron cuotas: la inscripción es nula o no tiene ID persistido");
                return;
            }

            OfertaAcademica oferta = inscripcion.getOferta();
            if (oferta == null) {
                log.warn("⚠️ No se generaron cuotas: la inscripción {} no tiene oferta asociada",
                        inscripcion.getIdInscripcion());
                return;
            }

            Double costoCuota = null;
            Integer numeroCuotas = null;
            Integer diaVencimiento = null;

            if (oferta instanceof Curso curso) {
                costoCuota = curso.getCostoCuota();
                numeroCuotas = curso.getNrCuotas();
                diaVencimiento = curso.getDiaVencimiento();
            } else if (oferta instanceof Formacion formacion) {
                costoCuota = formacion.getCostoCuota();
                numeroCuotas = formacion.getNrCuotas();
                diaVencimiento = formacion.getDiaVencimiento();
            } else {
                log.info("ℹ️ La oferta {} no maneja cuotas automáticas", oferta.getNombre());
                return;
            }

            if (numeroCuotas == null || numeroCuotas <= 0 || costoCuota == null || costoCuota <= 0) {
                log.info("ℹ️ La oferta {} no tiene configuración de cuotas válida", oferta.getNombre());
                return;
            }

            List<Cuota> existentes = cuotaRepository
                    .findByInscripcionIdInscripcion(inscripcion.getIdInscripcion());
            
            // Si ya existe alguna cuota, no generar más automáticamente
            if (!existentes.isEmpty()) {
                log.info("ℹ️ La inscripción {} ya posee {} cuotas registradas", inscripcion.getIdInscripcion(),
                        existentes.size());
                return;
            }

            BigDecimal montoCuota = BigDecimal.valueOf(costoCuota).setScale(2, RoundingMode.HALF_UP);

            LocalDate fechaReferencia = Optional.ofNullable(inscripcion.getFechaInscripcion()).orElse(LocalDate.now());
            if (oferta.getFechaInicio() != null && fechaReferencia.isBefore(oferta.getFechaInicio())) {
                fechaReferencia = oferta.getFechaInicio();
            }

            // ⭐ SOLO generar la PRIMERA cuota
            Cuota primeraCuota = new Cuota();
            primeraCuota.setInscripcion(inscripcion);
            primeraCuota.setNumeroCuota(1);
            primeraCuota.setMonto(montoCuota);
            primeraCuota.setMontoPagado(BigDecimal.ZERO);
            primeraCuota.setEstado(EstadoCuota.PENDIENTE);
            primeraCuota.setFechaVencimiento(calcularFechaVencimientoParaCuota(diaVencimiento, fechaReferencia, 1));

            cuotaRepository.save(primeraCuota);

            log.info("✅ Generada PRIMERA cuota para la inscripción {}", inscripcion.getIdInscripcion());
        } catch (Exception e) {
            log.error("❌ Error al generar cuotas para la inscripción {}", inscripcion != null ? inscripcion.getIdInscripcion() : null, e);
        }
    }

    /**
     * Genera la siguiente cuota si corresponde (cuota anterior pagada + fecha vencimiento pasada)
     */
    public void generarSiguienteCuotaSiCorresponde(Inscripciones inscripcion) {
        try {
            if (inscripcion == null || inscripcion.getIdInscripcion() == null) {
                return;
            }

            OfertaAcademica oferta = inscripcion.getOferta();
            if (oferta == null) {
                return;
            }

            Double costoCuota = null;
            Integer numeroCuotasTotal = null;
            Integer diaVencimiento = null;

            if (oferta instanceof Curso curso) {
                costoCuota = curso.getCostoCuota();
                numeroCuotasTotal = curso.getNrCuotas();
                diaVencimiento = curso.getDiaVencimiento();
            } else if (oferta instanceof Formacion formacion) {
                costoCuota = formacion.getCostoCuota();
                numeroCuotasTotal = formacion.getNrCuotas();
                diaVencimiento = formacion.getDiaVencimiento();
            } else {
                return;
            }

            if (numeroCuotasTotal == null || numeroCuotasTotal <= 0 || costoCuota == null || costoCuota <= 0) {
                return;
            }

            // Obtener todas las cuotas existentes de esta inscripción
            List<Cuota> cuotasExistentes = cuotaRepository
                    .findByInscripcionIdInscripcion(inscripcion.getIdInscripcion())
                    .stream()
                    .sorted(Comparator.comparing(Cuota::getNumeroCuota))
                    .collect(Collectors.toList());

            if (cuotasExistentes.isEmpty()) {
                return; // No hay ni la primera cuota, esto no debería pasar
            }

            int ultimoNumeroCuota = cuotasExistentes.get(cuotasExistentes.size() - 1).getNumeroCuota();

            // Si ya se generaron todas las cuotas, no hacer nada
            if (ultimoNumeroCuota >= numeroCuotasTotal) {
                return;
            }

            // Verificar que la última cuota esté PAGADA
            Cuota ultimaCuota = cuotasExistentes.get(cuotasExistentes.size() - 1);
            if (ultimaCuota.getEstado() != EstadoCuota.PAGADA) {
                return; // La última cuota no está pagada, no generar la siguiente
            }

            // Calcular la fecha de vencimiento de la SIGUIENTE cuota
            int siguienteNumero = ultimoNumeroCuota + 1;
            LocalDate fechaReferencia = Optional.ofNullable(inscripcion.getFechaInscripcion()).orElse(LocalDate.now());
            if (oferta.getFechaInicio() != null && fechaReferencia.isBefore(oferta.getFechaInicio())) {
                fechaReferencia = oferta.getFechaInicio();
            }

            LocalDate fechaVencimientoSiguiente = calcularFechaVencimientoParaCuota(diaVencimiento, fechaReferencia, siguienteNumero);

            // Solo generar si ya pasó la fecha de vencimiento de la cuota ANTERIOR
            LocalDate hoy = LocalDate.now();
            if (ultimaCuota.getFechaVencimiento() != null && !hoy.isAfter(ultimaCuota.getFechaVencimiento())) {
                log.info("⏳ Aún no ha vencido la cuota anterior {}, no se genera la siguiente", ultimoNumeroCuota);
                return;
            }

            // Generar la siguiente cuota
            BigDecimal montoCuota = BigDecimal.valueOf(costoCuota).setScale(2, RoundingMode.HALF_UP);
            
            Cuota nuevaCuota = new Cuota();
            nuevaCuota.setInscripcion(inscripcion);
            nuevaCuota.setNumeroCuota(siguienteNumero);
            nuevaCuota.setMonto(montoCuota);
            nuevaCuota.setMontoPagado(BigDecimal.ZERO);
            nuevaCuota.setEstado(EstadoCuota.PENDIENTE);
            nuevaCuota.setFechaVencimiento(fechaVencimientoSiguiente);

            cuotaRepository.save(nuevaCuota);

            log.info("✅ Generada cuota {} para la inscripción {} (vencimiento: {})", 
                siguienteNumero, inscripcion.getIdInscripcion(), fechaVencimientoSiguiente);

        } catch (Exception e) {
            log.error("❌ Error al generar siguiente cuota para la inscripción {}", 
                inscripcion != null ? inscripcion.getIdInscripcion() : null, e);
        }
    }

    private LocalDate calcularFechaVencimientoParaCuota(Integer diaVencimiento, LocalDate fechaReferencia,
            int numeroCuota) {
        LocalDate base = fechaReferencia != null ? fechaReferencia : LocalDate.now();

        if (diaVencimiento == null || diaVencimiento < 1 || diaVencimiento > 31) {
            return base.plusMonths(numeroCuota - 1);
        }

        LocalDate fechaInicioCuota = ajustarAlDiaDeVencimiento(base, diaVencimiento);
        LocalDate fechaObjetivo = fechaInicioCuota.plusMonths(numeroCuota - 1);
        int dia = Math.min(diaVencimiento, fechaObjetivo.lengthOfMonth());
        return fechaObjetivo.withDayOfMonth(dia);
    }

    private LocalDate ajustarAlDiaDeVencimiento(LocalDate base, int diaVencimiento) {
        LocalDate fecha = base != null ? base : LocalDate.now();
        int diaDentroDeMes = Math.min(diaVencimiento, fecha.lengthOfMonth());
        LocalDate ajustada = fecha.withDayOfMonth(diaDentroDeMes);

        if (ajustada.isBefore(fecha)) {
            LocalDate siguienteMes = fecha.plusMonths(1);
            int diaSiguiente = Math.min(diaVencimiento, siguienteMes.lengthOfMonth());
            ajustada = siguienteMes.withDayOfMonth(diaSiguiente);
        }

        return ajustada;
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