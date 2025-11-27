package com.example.demo.DTOMP;

import java.math.BigDecimal;
import java.util.List;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;

public record ReferenceRequest(
    
    Long userId,
    @DecimalMin(value = "0.01", message = "El monto total debe ser mayor que cero")
    BigDecimal totalAmount,
    @NotNull
    PayerDTO payer,
    @NotNull
    BackUrlsDTO backUrls,
    @NotNull
    DeliveryAddressDTO deliveryAddress,
    @NotNull
    String autoReturn,
    @NotNull
    @Valid
    List<ItemDTO> items
){

    public record PayerDTO(
        String name,
        String email
    ){}
    public record BackUrlsDTO(
        String success,
        String failure,
        String pending
    ){}

    public record DeliveryAddressDTO(
        String number,
        String street,
        String zipCode,
        String complement,
        String neighborhood,
        String city,
        String country
    ){}

    public record ItemDTO(
        String id,
        String title,
        BigDecimal unitPrice,
        Integer quantity
    ){}
}
