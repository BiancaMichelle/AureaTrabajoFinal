package com.example.demo.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.enums.EstadoPago;
import com.example.demo.model.Pago;
import com.example.demo.repository.PagoRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PagoCleanupScheduler {

    @Autowired
    private PagoRepository pagoRepository;

    /**
     * Tarea programada para cancelar pagos que quedaron en 'EN_PROCESO'
     * por más de 30 minutos (abandonados por el usuario).
     * Se ejecuta cada 15 minutos.
     */
    @Scheduled(fixedRate = 900000) // 15 minutos
    public void limpiarPagosAbandonados() {
        log.info("🧹 Iniciando limpieza de pagos abandonados...");

        LocalDateTime limite = LocalDateTime.now().minusMinutes(30);
        List<Pago> pagosAbandonados = pagoRepository.findByEstadoPagoAndFechaPagoBefore(EstadoPago.EN_PROCESO, limite);

        if (!pagosAbandonados.isEmpty()) {
            log.info("⚠️ Se encontraron {} pagos abandonados para cancelar.", pagosAbandonados.size());
            
            for (Pago pago : pagosAbandonados) {
                pago.setEstadoPago(EstadoPago.CANCELADO);
                // Opcional: Agregar detalle en descripción
                // pago.setDescripcion(pago.getDescripcion() + " (Cancelado por inactividad)");
            }
            
            pagoRepository.saveAll(pagosAbandonados);
            log.info("✅ {} pagos marcados como CANCELADO.", pagosAbandonados.size());
        } else {
            log.info("✅ No hay pagos abandonados para limpiar.");
        }
    }
}
