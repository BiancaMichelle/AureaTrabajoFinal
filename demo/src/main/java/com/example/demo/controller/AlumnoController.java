package com.example.demo.controller;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.Alumno;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Controller
public class AlumnoController {
    @GetMapping("/alumno")
    public String alumnoDashboard() {
        return "publico";
    }

    private final UsuarioRepository usuarioRepository;
    
    public AlumnoController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
    
    @GetMapping("/perfil")
    public String perfilUsuario(Authentication authentication, Model model) {
        String username = authentication.getName();
        
        // Buscar usuario por DNI (que es el username en el login)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(username);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            
            // Si es alumno, cargar datos académicos
            if (usuario instanceof Alumno) {
                model.addAttribute("alumno", (Alumno) usuario);
            }
        }
        
        return "perfilAlumno";
    }


}
