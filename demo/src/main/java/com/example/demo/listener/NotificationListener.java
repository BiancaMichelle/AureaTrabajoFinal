package com.example.demo.listener;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.demo.event.ActivityCreatedEvent;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.service.EmailService;

@Component
public class NotificationListener {

    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final InscripcionRepository inscripcionRepository;
    private final OfertaAcademicaRepository ofertaRepository;

    public NotificationListener(EmailService emailService, TemplateEngine templateEngine, InscripcionRepository inscripcionRepository, OfertaAcademicaRepository ofertaRepository) {
        this.emailService = emailService;
        this.templateEngine = templateEngine;
        this.inscripcionRepository = inscripcionRepository;
        this.ofertaRepository = ofertaRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivityCreated(ActivityCreatedEvent event) {
        OfertaAcademica oferta = ofertaRepository.findById(event.getOfertaId()).orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        List<Inscripciones> inscripciones = inscripcionRepository.findByOfertaIdOferta(event.getOfertaId());

        for (Inscripciones inscripcion : inscripciones) {
            Usuario alumno = inscripcion.getAlumno();
            // Assuming we send to all enrolled students
             if (alumno.getCorreo() != null && !alumno.getCorreo().isEmpty()) {
                sendNotificationEmail(alumno, event, oferta);
            }
        }
    }

    private void sendNotificationEmail(Usuario alumno, ActivityCreatedEvent event, OfertaAcademica oferta) {
        Context context = new Context();
        context.setVariable("alumnoName", alumno.getNombre());
        context.setVariable("ofertaName", oferta.getNombre());
        context.setVariable("tipoActividad", event.getTipo());
        context.setVariable("tituloActividad", event.getTitulo());
        context.setVariable("fechaLimite", event.getDeadline());

        String subject = "Nueva " + event.getTipo() + " en " + oferta.getNombre();
        String body = templateEngine.process("email/activity-created", context);

        emailService.sendEmail(alumno.getCorreo(), subject, body);
    }
}
