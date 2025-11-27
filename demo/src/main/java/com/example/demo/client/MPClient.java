package com.example.demo.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MPClient {

    @Value("${api.v1.mercadopago-access-token}")
    private String acessToken;

    @Value("${api.v1.mercadopago-notification-url}")
    private String notificationUrl;

    @PostConstruct
    public void init() {
        if (acessToken == null || acessToken.trim().isEmpty()) {
            log.error("❌ Access Token de MercadoPago no configurado!");
            throw new RuntimeException("MercadoPago Access Token no encontrado en application.properties");
        }

        MercadoPagoConfig.setAccessToken(acessToken);

        log.info("✅ MercadoPago inicializado correctamente");
        log.info("🔑 Access Token configurado (primeros 10 caracteres): {}",
                acessToken.substring(0, Math.min(10, acessToken.length())) + "...");

        // Validar si es token TEST o PROD
        if (acessToken.startsWith("TEST-")) {
            log.warn("⚠️  Usando Access Token de PRUEBA (TEST)");
        } else if (acessToken.startsWith("APP_USR-")) {
            log.info("🔒 Usando Access Token de PRODUCCIÓN");
        } else {
            log.warn("⚠️  Formato de Access Token no reconocido");
        }
    }

    public ResponseDTO createPreference(ReferenceRequest inputData, String orderNumber)
            throws MPException, MPApiException {

        log.info("creando preferencia de mercado pago con los datos: {}", inputData);
        log.info("📦 Order Number: {}", orderNumber);

        try {
            PreferenceClient preferenceClient = new PreferenceClient();
            List<PreferenceItemRequest> items = inputData.items().stream().map(item -> PreferenceItemRequest.builder()
                    .id(item.id())
                    .title(item.title())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .build()).toList();

            log.info("📋 Items construidos: {} items", items.size());
            items.forEach(item -> log.info("   - Item: id={}, title={}, quantity={}, unitPrice={}",
                    item.getId(), item.getTitle(), item.getQuantity(), item.getUnitPrice()));

            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .name(inputData.payer().name())
                    .email(inputData.payer().email())
                    .build();

            log.info("👤 Payer: name={}, email={}", payer.getName(), payer.getEmail());

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(inputData.backUrls().success())
                    .failure(inputData.backUrls().failure())
                    .pending(inputData.backUrls().pending())
                    .build();

            log.info("🔗 Back URLs configuradas");
            log.info("   - Success: {}", backUrls.getSuccess());
            log.info("   - Failure: {}", backUrls.getFailure());
            log.info("   - Pending: {}", backUrls.getPending());

            // Verificar que las URLs no sean null
            if (backUrls.getSuccess() == null || backUrls.getSuccess().isEmpty()) {
                log.error("❌ Success URL está vacía!");
            }
            if (backUrls.getFailure() == null || backUrls.getFailure().isEmpty()) {
                log.error("❌ Failure URL está vacía!");
            }

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference(orderNumber)
                    // Configuración adicional para modo TEST
                    .statementDescriptor("AUREA-INSCRIPCION") // Nombre que aparece en el resumen
                    .binaryMode(false) // Permite pagos pendientes
                    .autoReturn("approved")
                    .build();

            log.info("🔔 Notification URL: {}", notificationUrl);
            log.info("🔖 External Reference: {}", orderNumber);
            log.info("🚀 Enviando request a MercadoPago...");

            Preference preference = preferenceClient.create(preferenceRequest);

            log.info("🆔 Preference ID generado: {}", preference.getId());
            log.info("🔗 Init Point generado: {}", preference.getInitPoint());

            log.info("preferencia de mercado pago creada con id: {}", preference.getId());
            return new ResponseDTO(
                    preference.getId(),
                    preference.getInitPoint());

        } catch (MPApiException e) {
            log.error("❌ Error de API de MercadoPago: {}", e.getMessage());
            log.error("📋 Detalles de la respuesta de MercadoPago:");
            log.error("   - Status Code: {}", e.getStatusCode());
            if (e.getApiResponse() != null) {
                log.error("   - Response: {}", e.getApiResponse().getContent());
            }
            log.error("   - Causa: {}", e.getCause());
            throw new MPApiException("Error en la API de MercadoPago: " + e.getMessage(), e.getApiResponse());

        } catch (MPException e) {
            log.error("❌ Error de MercadoPago SDK: {}", e.getMessage());
            log.error("   - Causa: {}", e.getCause());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error general al crear preferencia de MercadoPago: {}", e.getMessage());
            e.printStackTrace();
            throw new MPException("ERROR inesperado al crear preferencia", e);
        }
    }
}