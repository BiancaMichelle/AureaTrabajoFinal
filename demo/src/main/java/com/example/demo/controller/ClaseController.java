package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
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
import com.example.demo.service.JitsiClaseService;

@Controller
@RequestMapping("/clase")
public class ClaseController {
    
    private final ClaseService claseService;
    private final JitsiClaseService jitsiClaseService; // Cambiar esto
    private final ModuloRepository moduloRepository;
    private final CursoRepository cursoRepository;
    
    public ClaseController(ClaseService claseService, 
                          JitsiClaseService jitsiClaseService, // Cambiar esto
                          ModuloRepository moduloRepository,
                          CursoRepository cursoRepository) {
        this.claseService = claseService;
        this.jitsiClaseService = jitsiClaseService; // Cambiar esto
        this.moduloRepository = moduloRepository;
        this.cursoRepository = cursoRepository;
    }
    
    @PostMapping("/crear")
    public String crearClase(@RequestParam String titulo,
                           @RequestParam String descripcion,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                           @RequestParam UUID moduloId,
                           @RequestParam Integer duracion,
                           @RequestParam(required = false, defaultValue = "false") Boolean asistenciaAutomatica,
                           Principal principal) {
        try {
            System.out.println("🎯 Creando clase con Jitsi:");
            System.out.println("   - Título: " + titulo);
            System.out.println("   - Módulo ID: " + moduloId);
            
            Clase clase = new Clase();
            clase.setTitulo(titulo);
            clase.setDescripcion(descripcion);
            clase.setInicio(inicio);
            clase.setFin(inicio.plusHours(duracion));
            clase.setAsistenciaAutomatica(asistenciaAutomatica);
            
            Clase claseCreada = claseService.crearClase(clase, moduloId, principal.getName());
            
            Modulo modulo = moduloRepository.findById(moduloId)
                    .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));
            Curso curso = modulo.getCurso();
            
            System.out.println("✅ Clase Jitsi creada exitosamente");
            
            return "redirect:/docente/aula/" + curso.getIdOferta();
            
        } catch (Exception e) {
            System.out.println("❌ Error al crear clase Jitsi: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/docente/mis-ofertas?error=" + e.getMessage();
        }
    }
    
    @GetMapping("/unirse/{claseId}")
    public String unirseAClase(@PathVariable UUID claseId, Principal principal, Model model) {
        try {
            System.out.println("🎯 Uniéndose a clase Jitsi ID: " + claseId);
            System.out.println("🎯 Usuario: " + principal.getName());
            
            String meetingUrl = claseService.unirseAClase(claseId, principal.getName());
            
            Clase clase = claseService.findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));
            
            // Con Jitsi, la reunión siempre está disponible
            boolean reunionActiva = true;
            
            System.out.println("🎯 Meeting URL Jitsi: " + meetingUrl);
            System.out.println("🎯 Clase: " + clase.getTitulo());
            
            model.addAttribute("meetingUrl", meetingUrl);
            model.addAttribute("clase", clase);
            model.addAttribute("codigoMeet", clase.getRoomName());
            model.addAttribute("proveedor", "Jitsi Meet"); // Cambiar esto
            model.addAttribute("reunionActiva", reunionActiva);
            
            return "clase-VideoConferencia";
            
        } catch (Exception e) {
            System.out.println("❌ Error en unirseAClase Jitsi: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "No se pudo cargar la sala de Jitsi: " + e.getMessage());
            return "clase-VideoConferencia";
        }
    }
    
    @GetMapping("/detalle/{claseId}")
    public String detalleClase(@PathVariable UUID claseId, Model model) {
        Optional<Clase> claseOpt = claseService.findById(claseId);
        
        if (claseOpt.isEmpty()) {
            model.addAttribute("error", "Clase no encontrada");
            return "clase-VideoConferencia";
        }
        
        model.addAttribute("clase", claseOpt.get());
        return "clase-detalle";
    }
}