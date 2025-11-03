package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Curso;
import com.example.demo.service.CursoService;

@Controller
public class OfertaAcademica {
    
    private final CursoService cursoService;
    
    public OfertaAcademica(CursoService cursoService) {
        this.cursoService = cursoService;
    }
    
    @GetMapping("/ofertaAcademica/{cursoId}")
    public String cargarOferta(@PathVariable Long cursoId, Model model, Authentication authentication) {
        Curso curso = cursoService.obtenerCursoPorId(cursoId);
        
        boolean puedeModificar = puedeModificarCurso(authentication);
        
        model.addAttribute("curso", curso);
        model.addAttribute("puedeModificar", puedeModificar);
        model.addAttribute("modulos", curso.getModulos());
        
        return "screens/aula"; 
    }
    
    @PostMapping("/crearModulo")
    public String crearModulo(@RequestParam String nombre,
                            @RequestParam String descripcion,
                            @RequestParam Long cursoId) {
        cursoService.crearModulo(nombre, descripcion, cursoId);
        return "redirect:/ofertaAcademica";
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