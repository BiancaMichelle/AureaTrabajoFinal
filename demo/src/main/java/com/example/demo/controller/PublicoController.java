package com.example.demo.controller;

import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Seminario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PublicoController {

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping({"/", "/inicio"})
    public String home(Model model) {
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

