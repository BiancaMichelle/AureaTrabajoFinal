package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.demo.enums.EstadoCuota;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Cuota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCuota;
    
    @ManyToOne
    @JoinColumn(name = "inscripcion_id")
    private Inscripciones inscripcion;
    
    private Integer numeroCuota;
    private BigDecimal monto;
    private LocalDate fechaVencimiento;
    
    @Enumerated(EnumType.STRING)
    private EstadoCuota estado = EstadoCuota.PENDIENTE;
    
    private BigDecimal montoPagado;
    private LocalDate fechaPago;
    
    @ManyToOne
    @JoinColumn(name = "pago_id")
    private Pago pago;
}