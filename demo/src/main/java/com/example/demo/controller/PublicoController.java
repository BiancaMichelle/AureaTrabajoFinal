package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Seminario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.OfertaAcademicaRepository;

@Controller
public class PublicoController {

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping({"/", "/inicio"})
    public String home(Model model, @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "timeout", required = false) String timeout) {

        if (error != null) {
                model.addAttribute("error", "Credenciales inválidas. Intenta otra vez.");
            }
            if (logout != null) {
                model.addAttribute("message", "Has cerrado sesión exitosamente.");
            }
            if (timeout != null) {
                model.addAttribute("timeout", "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.");
            }

        List<OfertaAcademica> todasLasOfertas = ofertaAcademicaRepository.findAll();

        model.addAttribute("formaciones", todasLasOfertas.stream()
                .filter(o -> o instanceof Formacion)
                .collect(Collectors.toList()));

        model.addAttribute("seminarios", todasLasOfertas.stream()
                .filter(o -> o instanceof Seminario)
                .collect(Collectors.toList()));

        model.addAttribute("cursos", todasLasOfertas.stream()
                .filter(o -> o instanceof Curso)
                .collect(Collectors.toList()));

        model.addAttribute("charlas", todasLasOfertas.stream()
                .filter(o -> o instanceof Charla)
                .collect(Collectors.toList()));

        model.addAttribute("categorias", categoriaRepository.findAll());

        return "screens/inicio";
    }

}

