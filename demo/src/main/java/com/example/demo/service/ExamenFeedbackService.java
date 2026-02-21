package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ia.service.ChatServiceSimple;
import com.example.demo.model.Examen;
import com.example.demo.model.ExamenFeedbackPendiente;
import com.example.demo.model.Modulo;
import com.example.demo.model.Notificacion;
import com.example.demo.model.RespuestaIntento;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ExamenFeedbackPendienteRepository;
import com.example.demo.repository.NotificacionRepository;

@Service
public class ExamenFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(ExamenFeedbackService.class);

    @Autowired
    private ExamenFeedbackPendienteRepository pendienteRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ChatServiceSimple chatServiceSimple;

    @Autowired
    private EmailService emailService;

    @Async
    @Transactional
    public void generarYProgramarFeedback(Usuario alumno, Examen examen, com.example.demo.model.Intento intento,
            List<RespuestaIntento> respuestasIntento) {
        System.out.println("🚀 [ExamenFeedbackService] Iniciando generación de feedback...");
        try {
            if (alumno == null || examen == null || respuestasIntento == null) {
                System.out.println("❌ [ExamenFeedbackService] Datos nulos recibidos. Abortando.");
                return;
            }

            List<RespuestaIntento> incorrectas = respuestasIntento.stream()
                    .filter(r -> Boolean.FALSE.equals(r.getEsCorrecta()))
                    .limit(3)
                    .toList();

            System.out.println("📊 [ExamenFeedbackService] Respuestas incorrectas encontradas: " + incorrectas.size());

            if (incorrectas.isEmpty()) {
                System.out.println("ℹ️ [ExamenFeedbackService] No hay respuestas incorrectas. No se genera feedback.");
                return;
            }

            List<Modulo> modulosRelacionados = examen.getModulosRelacionados();
            if (modulosRelacionados == null || modulosRelacionados.isEmpty()) {
                modulosRelacionados = examen.getModulo() != null ? List.of(examen.getModulo()) : List.of();
            }

            System.out.println("🤖 [ExamenFeedbackService] Llamando a chatServiceSimple.generarFeedbackExamen...");
            String feedback = chatServiceSimple.generarFeedbackExamen(alumno, examen, incorrectas, modulosRelacionados);

            if (feedback == null || feedback.isBlank()) {
                System.out.println("⚠️ [ExamenFeedbackService] IA devolvió feedback vacío o nulo.");
                return;
            }

            System.out.println(
                    "✅ [ExamenFeedbackService] Feedback generado correctamente. Longitud: " + feedback.length());

            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime cierre = examen.getFechaCierre();
            if (cierre == null || !ahora.isBefore(cierre)) {
                enviarFeedbackInmediato(alumno, examen, feedback);
            } else {
                programarFeedback(alumno, examen,
                        intento != null ? intento.getIdIntento() : null,
                        feedback,
                        cierre);
            }
        } catch (Exception e) {
            log.error("Error generando feedback IA: {}", e.getMessage());
        }
    }

    @Transactional
    public void programarFeedback(Usuario alumno, Examen examen, java.util.UUID intentoId, String mensaje,
            LocalDateTime fechaProgramada) {
        if (alumno == null || examen == null || mensaje == null || mensaje.isBlank()) {
            return;
        }

        ExamenFeedbackPendiente pendiente = new ExamenFeedbackPendiente();
        pendiente.setAlumno(alumno);
        pendiente.setExamen(examen);
        pendiente.setIntentoId(intentoId);
        pendiente.setMensaje(mensaje);
        pendiente.setFechaProgramada(fechaProgramada != null ? fechaProgramada : LocalDateTime.now());
        pendiente.setEnviado(false);

        pendienteRepository.save(pendiente);
    }

    @Transactional
    public void enviarFeedbackInmediato(Usuario alumno, Examen examen, String mensaje) {
        if (alumno == null || examen == null || mensaje == null || mensaje.isBlank()) {
            return;
        }

        Notificacion notif = new Notificacion();
        notif.setUsuario(alumno);
        notif.setTitulo("📌 Resultado del examen: " + (examen.getTitulo() != null ? examen.getTitulo() : "Examen"));
        notif.setLeida(false);
        notificacionRepository.save(notif);
        System.out.println("🔔 [ExamenFeedbackService] Notificación guardada en plataforma");

        // 2. BACKUP: Enviar correo electrónico
        try {
            if (alumno.getCorreo() != null && !alumno.getCorreo().isBlank()) {
                String subject = "Feedback de tu examen: "
                        + (examen.getTitulo() != null ? examen.getTitulo() : "Examen");
                // Convertir markdown básico a HTML simple para el correo
                String bodyHtml = "<html><body>" +
                        "<h2>Hola " + alumno.getNombre() + ",</h2>" +
                        "<p>Aquí tienes el feedback generado automáticamente sobre tu examen:</p>" +
                        "<hr/>" +
                        mensaje.replace("\n", "<br/>") +
                        "<hr/>" +
                        "<p>Recuerda que también puedes ver este mensaje en el chat de la plataforma.</p>" +
                        "<p>Saludos,<br>Equipo Aurea</p>" +
                        "</body></html>";

                emailService.sendEmail(alumno.getCorreo(), subject, bodyHtml);
                System.out.println("📧 [ExamenFeedbackService] Email de respaldo enviado a: " + alumno.getCorreo());
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR al enviar email de feedback: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void procesarPendientes() {
        LocalDateTime ahora = LocalDateTime.now();
        List<ExamenFeedbackPendiente> pendientes = pendienteRepository
                .findByEnviadoFalseAndFechaProgramadaLessThanEqual(ahora);

        for (ExamenFeedbackPendiente pendiente : pendientes) {
            try {
                Notificacion notif = new Notificacion();
                notif.setUsuario(pendiente.getAlumno());
                notif.setTitulo("📌 Resultado del examen: "
                        + (pendiente.getExamen() != null && pendiente.getExamen().getTitulo() != null
                                ? pendiente.getExamen().getTitulo()
                                : "Examen"));
                notif.setMensaje(pendiente.getMensaje());
                notif.setTipo("CHAT_IA");
                notif.setLeida(false);
                notificacionRepository.save(notif);

                // 2. BACKUP: Enviar correo electrónico
                try {
                    Usuario alumno = pendiente.getAlumno();
                    if (alumno != null && alumno.getCorreo() != null && !alumno.getCorreo().isBlank()) {
                        String tituloExamen = pendiente.getExamen() != null && pendiente.getExamen().getTitulo() != null
                                ? pendiente.getExamen().getTitulo()
                                : "Examen";

                        String subject = "Feedback disponible: " + tituloExamen;

                        String bodyHtml = "<html><body>" +
                                "<h2>Hola " + alumno.getNombre() + ",</h2>" +
                                "<p>Ya está disponible el feedback de tu examen <strong>" + tituloExamen
                                + "</strong>.</p>" +
                                "<hr/>" +
                                pendiente.getMensaje().replace("\n", "<br/>") +
                                "<hr/>" +
                                "<p>Saludos,<br>Equipo Aurea</p>" +
                                "</body></html>";

                        emailService.sendEmail(alumno.getCorreo(), subject, bodyHtml);
                        System.out.println(
                                "📧 [ExamenFeedbackService] Email programado enviado a: " + alumno.getCorreo());
                    }
                } catch (Exception e) {
                    System.err.println("❌ ERROR al enviar email de feedback pendiente: " + e.getMessage());
                }

                pendiente.setEnviado(true);
                pendienteRepository.save(pendiente);
            } catch (Exception e) {
                log.error("Error enviando feedback pendiente {}: {}", pendiente.getId(), e.getMessage());
            }
        }
    }
}
