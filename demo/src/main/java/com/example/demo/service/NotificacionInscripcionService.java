package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.NotificacionInscripcion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacionInscripcionRepository;
import com.example.demo.repository.OfertaAcademicaRepository;

@Service
@Transactional
public class NotificacionInscripcionService {
    
    @Autowired
    private NotificacionInscripcionRepository notificacionRepository;
    
    @Autowired
    private OfertaAcademicaRepository ofertaRepository;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Solicita notificación cuando se abran las inscripciones
     */
    public NotificacionInscripcion solicitarNotificacion(Usuario usuario, OfertaAcademica oferta) {
        // Verificar si ya existe una solicitud pendiente
        var existente = notificacionRepository.findByUsuarioAndOfertaAndNotificadoFalse(usuario, oferta);
        if (existente.isPresent()) {
            return existente.get();
        }
        
        NotificacionInscripcion notificacion = new NotificacionInscripcion();
        notificacion.setUsuario(usuario);
        notificacion.setOferta(oferta);
        notificacion.setEmail(usuario.getCorreo()); // Usar getCorreo() en lugar de getEmail()
        notificacion.setFechaSolicitud(LocalDateTime.now());
        notificacion.setNotificado(false);
        
        return notificacionRepository.save(notificacion);
    }
    
    /**
     * Tarea programada que se ejecuta cada hora para verificar si se abrieron inscripciones
     */
    @Scheduled(cron = "0 0 * * * *") // Cada hora en punto
    public void verificarYEnviarNotificaciones() {
        System.out.println("🔔 Verificando inscripciones abiertas para enviar notificaciones...");
        
        LocalDate hoy = LocalDate.now();
        
        // Obtener todas las notificaciones pendientes
        List<NotificacionInscripcion> pendientes = notificacionRepository.findByNotificadoFalse();
        
        for (NotificacionInscripcion notif : pendientes) {
            OfertaAcademica oferta = notif.getOferta();
            
            // Verificar si las inscripciones ya están abiertas
            if (oferta.getFechaInicioInscripcion() != null && 
                !hoy.isBefore(oferta.getFechaInicioInscripcion()) &&
                oferta.getFechaFinInscripcion() != null &&
                !hoy.isAfter(oferta.getFechaFinInscripcion())) {
                
                // Las inscripciones están abiertas, enviar notificación
                enviarNotificacion(notif);
            }
        }
    }
    
    /**
     * Envía la notificación por email
     */
    private void enviarNotificacion(NotificacionInscripcion notif) {
        try {
            OfertaAcademica oferta = notif.getOferta();
            Usuario usuario = notif.getUsuario();
            
            String asunto = "¡Las inscripciones están abiertas! - " + oferta.getNombre();
            
            StringBuilder mensaje = new StringBuilder();
            mensaje.append("<html><body>");
            mensaje.append("<h2>¡Buenas noticias, ").append(usuario.getNombre()).append("!</h2>");
            mensaje.append("<p>Las inscripciones para la oferta académica que te interesaba ya están <strong>ABIERTAS</strong>:</p>");
            mensaje.append("<div style='background-color: #f0f8ff; padding: 20px; border-left: 4px solid #007bff; margin: 20px 0;'>");
            mensaje.append("<h3 style='color: #007bff; margin-top: 0;'>").append(oferta.getNombre()).append("</h3>");
            mensaje.append("<p><strong>Tipo:</strong> ").append(oferta.getTipoOferta()).append("</p>");
            mensaje.append("<p><strong>Modalidad:</strong> ").append(oferta.getModalidad()).append("</p>");
            mensaje.append("<p><strong>Descripción:</strong> ").append(oferta.getDescripcion()).append("</p>");
            
            if (oferta.getFechaInicio() != null) {
                mensaje.append("<p><strong>Fecha de inicio:</strong> ").append(oferta.getFechaInicio()).append("</p>");
            }
            
            if (oferta.getFechaFinInscripcion() != null) {
                mensaje.append("<p style='color: #dc3545;'><strong>⏰ Cierre de inscripciones:</strong> ")
                       .append(oferta.getFechaFinInscripcion()).append("</p>");
            }
            
            mensaje.append("</div>");
            mensaje.append("<p>No pierdas esta oportunidad. <strong>¡Inscríbete ahora!</strong></p>");
            mensaje.append("<p style='text-align: center; margin-top: 30px;'>");
            mensaje.append("<a href='http://localhost:8080/publico/oferta/").append(oferta.getIdOferta())
                   .append("' style='background-color: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Ver Oferta e Inscribirme</a>");
            mensaje.append("</p>");
            mensaje.append("<hr style='margin-top: 30px;'>");
            mensaje.append("<p style='font-size: 12px; color: #666;'>Recibiste este email porque solicitaste ser notificado cuando se abrieran las inscripciones.</p>");
            mensaje.append("</body></html>");
            
            emailService.sendEmail(notif.getEmail(), asunto, mensaje.toString()); // Usar sendEmail() en lugar de enviarCorreo()
            
            // Marcar como notificado
            notif.setNotificado(true);
            notif.setFechaNotificacion(LocalDateTime.now());
            notificacionRepository.save(notif);
            
            System.out.println("✅ Notificación enviada a: " + notif.getEmail() + " para oferta: " + oferta.getNombre());
            
        } catch (Exception e) {
            System.err.println("❌ Error al enviar notificación: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene el conteo de personas esperando notificación para una oferta
     */
    public long contarSolicitudesPendientes(OfertaAcademica oferta) {
        return notificacionRepository.findByOfertaAndNotificadoFalse(oferta).size();
    }
}
