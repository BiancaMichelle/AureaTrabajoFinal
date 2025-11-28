package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.service.CursoService;

@Controller
public class OfertaAcademicaController {
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    private final CursoService cursoService;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final ModuloRepository moduloRepository;
    
    public OfertaAcademicaController(CursoService cursoService,
                                    OfertaAcademicaRepository ofertaAcademicaRepository,
                                    ModuloRepository moduloRepository) {
        this.cursoService = cursoService;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.moduloRepository = moduloRepository;
    }
    
    @GetMapping("/ofertaAcademica/{cursoId}")
    public String cargarOferta(@PathVariable Long cursoId, Model model, Authentication authentication) {
        // ✅ Buscar en OfertaAcademicaRepository para soportar Cursos Y Formaciones
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Oferta académica no encontrada"));
        
        System.out.println("📚 Cargando oferta: " + oferta.getNombre() + " (Tipo: " + oferta.getClass().getSimpleName() + ")");
        
        boolean puedeModificar = puedeModificarCurso(authentication);
        
        // Cargar módulos de la oferta
        List<Modulo> modulos = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(oferta);
        
        model.addAttribute("curso", oferta); // Mantener nombre "curso" para compatibilidad con la vista
        model.addAttribute("puedeModificar", puedeModificar);
        model.addAttribute("modulos", modulos);
        
        return "aula"; 
    }
    
    @PostMapping("/crearModulo")
    public String crearModulo(@RequestParam String nombre,
                            @RequestParam String descripcion,
                            @RequestParam(required = false) String objetivos,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                            @RequestParam(required = false, defaultValue = "false") Boolean visibilidad,
                            @RequestParam Long cursoId) {
        cursoService.crearModulo(nombre, descripcion, objetivos, fechaInicio, fechaFin, visibilidad, cursoId);
        // ✅ Redirigir de vuelta al curso específico
        return "redirect:" + baseUrl + "/ofertaAcademica/" + cursoId;
    }
    
    private boolean puedeModificarCurso(Authentication authentication) {
        if (authentication == null) return false;
        
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> 
                    auth.getAuthority().equals("ADMIN") || 
                    auth.getAuthority().equals("DOCENTE")
                );
    }
}