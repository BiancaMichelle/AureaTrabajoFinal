package com.example.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Pago;
import com.example.demo.model.Usuario;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByUsuarioId(java.util.UUID usuarioId);

    List<Pago> findByEstadoPago(String estadoPago);

    List<Pago> findByMetodoPago(String metodoPago);

    @Query("SELECT p FROM Pago p WHERE p.fechaPago BETWEEN :fechaInicio AND :fechaFin")
    List<Pago> findPagosEnRangoFecha(@Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT SUM(p.monto) FROM Pago p WHERE p.estadoPago = 'COMPLETADO'")
    BigDecimal obtenerTotalPagosCompletados();

    @Query("SELECT p FROM Pago p WHERE p.numeroTransaccion = :numeroTransaccion")
    List<Pago> findByNumeroTransaccion(@Param("numeroTransaccion") String numeroTransaccion);

    // Métodos para Mercado Pago
    Optional<Pago> findByPreferenceId(String preferenceId);

    Optional<Pago> findByPaymentId(Long paymentId);

    Optional<Pago> findByExternalReference(String externalReference);

    List<Pago> findByUsuarioDniAndEsCuotaMensualTrue(String dni);

    boolean existsByUsuarioAndOfertaAndEstadoPago(Usuario usuario, com.example.demo.model.OfertaAcademica oferta,
            com.example.demo.enums.EstadoPago estadoPago);
}