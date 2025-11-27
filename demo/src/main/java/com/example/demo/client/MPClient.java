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
    public void init(){
        MercadoPagoConfig.setAccessToken(acessToken);
        log.info("Iniciando Mercado Pago");
    }

    public ResponseDTO createPreference(ReferenceRequest inputData, String orderNumber) throws MPException, MPApiException{

        log.info("creando preferencia de mercado pago con los datos: {}", inputData);
        try{
            PreferenceClient preferenceClient = new PreferenceClient();
        List<PreferenceItemRequest> items = inputData.items().stream().map(item -> 
            PreferenceItemRequest.builder()
                .id(item.id())
                .title(item.title())
                .quantity(item.quantity())
                .unitPrice(item.unitPrice())
                .build()
        ).toList();

        PreferencePayerRequest payer = PreferencePayerRequest.builder()
            .name(inputData.payer().name())
            .email(inputData.payer().email())
            .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
            .success(inputData.backUrls().success())
            .failure(inputData.backUrls().failure())
            .pending(inputData.backUrls().pending())
            .build(); 


        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
            .items(items)
            .payer(payer)
            .backUrls(backUrls)
            .notificationUrl(notificationUrl)
            .externalReference(orderNumber)
            .autoReturn("approved")
            .build();

        Preference preference = preferenceClient.create(preferenceRequest);

        log.info("preferencia de mercado pago creada con id: {}", preference.getId());
        return new ResponseDTO(
            preference.getId(),
            preference.getInitPoint()
        );

        }catch (MPApiException e){
            log.error("error en crear la preferencia de mercado pago en api: {}", e.getMessage());
            throw new MPApiException("ERROR inesperado",e.getApiResponse());

        }catch (MPException e){
            log.error("error en crear la preferencia de mercado pago", e.getMessage());
            throw e;
        }catch (Exception e){
            log.error("error general en crear la preferencia de mercado pago", e.getMessage());
            throw new MPException("ERROR inesperado",e);
        }
    } 
}