package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.service.MercadoPagoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class MPController {
    
    private final MercadoPagoService mercadoPagoService;

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
}
