package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Clase;
import com.example.demo.model.Curso;
import com.example.demo.model.Modulo;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.service.ClaseService;

@Controller
@RequestMapping("/clase")
public class ClaseController {
    
    private final ClaseService claseService;
    private final ModuloRepository moduloRepository;
    private final CursoRepository cursoRepository; // Agrega esto
    
    public ClaseController(ClaseService claseService, ModuloRepository moduloRepository, 
                          CursoRepository cursoRepository) {
        this.claseService = claseService;
        this.moduloRepository = moduloRepository;
        this.cursoRepository = cursoRepository;
    }
    
    // Procesar creación de clase
    @PostMapping("/crear")
    public String crearClase(@RequestParam String titulo,
                           @RequestParam String descripcion,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                           @RequestParam UUID moduloId,
                           @RequestParam Integer duracion,
                           @RequestParam(required = false, defaultValue = "false") Boolean asistenciaAutomatica,
                           Principal principal) {
        try {
            Clase clase = new Clase();
            clase.setTitulo(titulo);
            clase.setDescripcion(descripcion);
            clase.setInicio(inicio);
            clase.setFin(inicio.plusHours(duracion));
            clase.setAsistenciaAutomatica(asistenciaAutomatica);
            
            Clase claseCreada = claseService.crearClase(clase, moduloId, principal.getName());
            
            // Obtener el curso para saber el ID
            Modulo modulo = moduloRepository.findById(moduloId)
                    .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));
            Curso curso = modulo.getCurso();
            
            // Redirigir correctamente al endpoint del docente
            return "redirect:/docente/aula/" + curso.getIdOferta();
            
        } catch (Exception e) {
            return "redirect:/docente/mis-ofertas?error=" + e.getMessage();
        }
    }

    @GetMapping("/unirse/{claseId}")
    public String unirseAClase(@PathVariable UUID claseId, Principal principal, Model model) {
        try {
            System.out.println("🎯 Solicitando unirse a clase ID: " + claseId);
            System.out.println("🎯 Usuario: " + principal.getName());
            
            String meetingUrl = claseService.unirseAClase(claseId, principal.getName());
            
            // Obtener información de la clase para mostrar en la vista
            Clase clase = claseService.findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));
            
            System.out.println("🎯 Meeting URL obtenida: " + meetingUrl);
            System.out.println("🎯 Clase encontrada: " + clase.getTitulo());
            
            model.addAttribute("meetingUrl", meetingUrl);
            model.addAttribute("clase", clase);
            return "clase-VideoConferencia";
            
        } catch (Exception e) {
            System.out.println("❌ Error en unirseAClase: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "No se pudo cargar la sala de videoconferencia: " + e.getMessage());
            return "clase-VideoConferencia";
        }
    }
}