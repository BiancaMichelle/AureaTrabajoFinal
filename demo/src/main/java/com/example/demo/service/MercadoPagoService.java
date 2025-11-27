package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.client.MPClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class MercadoPagoService {

    private final MPClient mpClient;

    public MercadoPagoService (MPClient mpClient) {
        this.mpClient = mpClient;
    }

    public ResponseDTO createPreference(ReferenceRequest inputData) throws MPException, MPApiException{
        log.info("creando preferencia de pago con respuesta: {}", inputData);

        //validar request
        //validar usuario existe
        //validar si la oferta existe
        //validar 

        String ordenNumber = "123123123123"; //usar alguna logica para generar un numero que sea unico

        try{
            return mpClient.createPreference(inputData, ordenNumber);
        }catch(MPException e){
            log.error("Error en crear la preferencia de pago", e);
            throw new RuntimeException();
        }catch(MPApiException e){
            log.error("Error en crear el pago", e);
            throw new RuntimeException();
        }


    }
}