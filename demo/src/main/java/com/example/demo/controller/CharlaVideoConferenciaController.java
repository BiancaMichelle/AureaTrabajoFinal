package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
public class CharlaVideoConferenciaController {

    @GetMapping("/charla/videoconferencia/{roomName}")
    public String mostrarVideoconferencia(
            @PathVariable String roomName,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String hora,
            @RequestParam(required = false) String moderador,
            Model model) {
        
        // Extraer el nombre de la charla del roomName (quitar el prefijo aurea-charla-)
        String nombreCharla = roomName.replace("aurea-charla-", "")
                .replace("-", " ")
                .trim();
        
        // Capitalizar primera letra de cada palabra
        String[] palabras = nombreCharla.split(" ");
        StringBuilder nombreFormateado = new StringBuilder();
        for (String palabra : palabras) {
            if (palabra.length() > 0) {
                nombreFormateado.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1))
                        .append(" ");
            }
        }
        
        // Validar fecha y hora
        if (fecha != null && hora != null) {
            try {
                LocalDate fechaCharla = LocalDate.parse(fecha);
                LocalTime horaCharla = LocalTime.parse(hora);
                LocalDateTime fechaHoraCharla = LocalDateTime.of(fechaCharla, horaCharla);
                LocalDateTime ahora = LocalDateTime.now();
                
                // Permitir acceso 5 minutos antes
                LocalDateTime horaAccesoPermitido = fechaHoraCharla.minusMinutes(5);
                
                System.out.println("🕐 Validación de horario:");
                System.out.println("   - Fecha/Hora charla: " + fechaHoraCharla);
                System.out.println("   - Hora actual: " + ahora);
                System.out.println("   - Acceso permitido desde: " + horaAccesoPermitido);
                
                // Si es demasiado temprano
                if (ahora.isBefore(horaAccesoPermitido)) {
                    model.addAttribute("error", "demasiado-temprano");
                    model.addAttribute("fechaHoraCharla", fechaHoraCharla.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    model.addAttribute("horaAcceso", horaAccesoPermitido.format(DateTimeFormatter.ofPattern("HH:mm")));
                    model.addAttribute("nombreCharla", nombreFormateado.toString().trim());
                    return "charla-acceso-denegado";
                }
                
                // Si ya pasó más de 2 horas de la charla
                LocalDateTime horaFinPermitido = fechaHoraCharla.plusHours(2);
                if (ahora.isAfter(horaFinPermitido)) {
                    model.addAttribute("error", "charla-finalizada");
                    model.addAttribute("fechaHoraCharla", fechaHoraCharla.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    model.addAttribute("nombreCharla", nombreFormateado.toString().trim());
                    return "charla-acceso-denegado";
                }
                
            } catch (DateTimeParseException e) {
                System.err.println("❌ Error al parsear fecha/hora: " + e.getMessage());
            }
        }
        
        model.addAttribute("roomName", roomName);
        model.addAttribute("nombreCharla", nombreFormateado.toString().trim());
        model.addAttribute("fecha", fecha);
        model.addAttribute("hora", hora);
        model.addAttribute("moderador", moderador);
        
        System.out.println("🎥 Accediendo a videoconferencia de charla:");
        System.out.println("   - Room Name: " + roomName);
        System.out.println("   - Nombre Charla: " + nombreFormateado.toString().trim());
        System.out.println("   - Fecha: " + fecha);
        System.out.println("   - Hora: " + hora);
        System.out.println("   - Moderador: " + moderador);
        
        return "charla-videoconferencia";
    }
}
