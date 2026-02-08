package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Examen;
import com.example.demo.model.ExamenFeedbackPendiente;
import com.example.demo.model.Notificacion;
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

    @Transactional
    public void programarFeedback(Usuario alumno, Examen examen, java.util.UUID intentoId, String mensaje, LocalDateTime fechaProgramada) {
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
        notif.setMensaje(mensaje);
        notif.setTipo("CHAT_IA");
        notif.setLeida(false);
        notificacionRepository.save(notif);
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
                notif.setTitulo("📌 Resultado del examen: " + (pendiente.getExamen() != null && pendiente.getExamen().getTitulo() != null
                        ? pendiente.getExamen().getTitulo() : "Examen"));
                notif.setMensaje(pendiente.getMensaje());
                notif.setTipo("CHAT_IA");
                notif.setLeida(false);
                notificacionRepository.save(notif);

                pendiente.setEnviado(true);
                pendienteRepository.save(pendiente);
            } catch (Exception e) {
                log.error("Error enviando feedback pendiente {}: {}", pendiente.getId(), e.getMessage());
            }
        }
    }
}
