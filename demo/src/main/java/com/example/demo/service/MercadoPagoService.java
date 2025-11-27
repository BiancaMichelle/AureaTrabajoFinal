package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.client.MPClient;
import com.example.demo.enums.EstadoPago;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pago;
import com.example.demo.model.Usuario;
import com.example.demo.repository.PagoRepository;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class MercadoPagoService {

    private final MPClient mpClient;
    private final PagoRepository pagoRepository;

    public MercadoPagoService(MPClient mpClient, PagoRepository pagoRepository) {
        this.mpClient = mpClient;
        this.pagoRepository = pagoRepository;
    }

    public ResponseDTO createPreference(ReferenceRequest inputData, Usuario usuario, OfertaAcademica oferta) throws MPException, MPApiException{
        log.info("creando preferencia de pago con respuesta: {}", inputData);

        // Generar número de orden único
        String orderNumber = generarNumeroOrden(usuario, oferta);
        log.info("📝 Número de orden generado: {}", orderNumber);

        try{
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
        }catch(MPException e){
            log.error("Error en crear la preferencia de pago", e);
            throw new RuntimeException("Error al crear preferencia de pago: " + e.getMessage());
        }catch(MPApiException e){
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
}