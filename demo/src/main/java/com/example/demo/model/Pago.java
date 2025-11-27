package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.enums.EstadoPago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_oferta")
    private OfertaAcademica oferta;

    @OneToOne
    @JoinColumn(name = "id_inscripcion")
    private Inscripciones inscripcion;

    private BigDecimal monto;
    private LocalDateTime fechaPago;
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    private String descripcion;
    private String numeroTransaccion;
    private String comprobante;

    @OneToMany(mappedBy = "pago")
    private List<Cuota> cuotas;

    // Campos específicos de Mercado Pago
    @Column(name = "preference_id")
    private String preferenceId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "merchant_order_id")
    private Long merchantOrderId;

    @Column(name = "tipo_pago")
    private String tipoPago; // credit_card, debit_card, account_money

    @Column(name = "email_pagador")
    private String emailPagador;

    @Column(name = "nombre_pagador")
    private String nombrePagador;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "comprobante_enviado")
    private Boolean comprobanteEnviado = false;

    @Column(name = "es_cuota_mensual")
    private Boolean esCuotaMensual = false;

    @Column(name = "numero_cuota")
    private Integer numeroCuota;

    // Detalles adicionales del pago
    @Column(name = "status_detail")
    private String statusDetail;

    @Column(name = "installments")
    private Integer installments;

    @Column(name = "card_last_four_digits")
    private String cardLastFourDigits;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "issuer_id")
    private String issuerId;

    @Column(name = "transaction_amount")
    private BigDecimal transactionAmount;

    @PrePersist
    protected void onCreate() {
        if (fechaPago == null) {
            fechaPago = LocalDateTime.now();
        }
        if (comprobanteEnviado == null) {
            comprobanteEnviado = false;
        }
        if (esCuotaMensual == null) {
            esCuotaMensual = false;
        }
    }
}
