package com.example.demo.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.enums.EstadoCuota;
import com.example.demo.model.Cuota;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CuotaRepository;
import com.example.demo.service.EmailService;

@Component
public class RecordatorioPagoScheduler {

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private EmailService emailService;

    // Se ejecuta todos los días a las 09:00 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void enviarRecordatoriosPago() {
        System.out.println("⏰ Iniciando tarea programada: Recordatorio de Pagos...");
        
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(5); // Avisar con 5 días de anticipación

        // Buscar cuotas pendientes que vencen entre hoy y dentro de 5 días
        List<Cuota> cuotasProximas = cuotaRepository.findByEstadoAndFechaVencimientoBetween(
                EstadoCuota.PENDIENTE, hoy, fechaLimite);

        int enviados = 0;
        for (Cuota cuota : cuotasProximas) {
            try {
                Usuario alumno = cuota.getInscripcion().getAlumno();
                if (alumno != null && alumno.getCorreo() != null) {
                    String asunto = "Recordatorio de Pago Próximo - Aurea";
                    String mensaje = String.format("""
                        <h2>Hola %s!</h2>
                        <p>Te recordamos que la <strong>cuota n° %d</strong> de tu formación vence pronto.</p>
                        <ul>
                            <li><strong>Fecha de vencimiento:</strong> %s</li>
                            <li><strong>Monto:</strong> $%s</li>
                        </ul>
                        <p>Por favor, ingresa a tu <a href="http://localhost:8080/alumno/mi-espacio">Espacio de Alumno</a> para realizar el pago.</p>
                        <br>
                        <p>Atentamente,<br>Equipo Aurea</p>
                        """, 
                        alumno.getNombre(), 
                        cuota.getNumeroCuota(), 
                        cuota.getFechaVencimiento(), 
                        cuota.getMonto());
    
                    emailService.sendEmail(alumno.getCorreo(), asunto, mensaje);
                    enviados++;
                }
            } catch (Exception e) {
                System.err.println("❌ Error enviando recordatorio para cuota ID " + cuota.getIdCuota() + ": " + e.getMessage());
            }
        }
        System.out.println("✅ Tarea finalizada. Recordatorios enviados: " + enviados);
    }
}
