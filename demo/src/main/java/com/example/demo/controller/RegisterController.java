package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.model.Alumno;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.InstitucionRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;

@Controller
public class RegisterController {

    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;
    private final InstitucionRepository institucionAlumnoRepository;
    private final AlumnoRepository alumnoRepository;

    // Inyección de dependencias
    public RegisterController(PaisRepository paisRepository,
                              ProvinciaRepository provinciaRepository,
                              CiudadRepository ciudadRepository,
                              InstitucionRepository institucionAlumnoRepository,
                              AlumnoRepository alumnoRepository) {
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
        this.institucionAlumnoRepository = institucionAlumnoRepository;
        this.alumnoRepository = alumnoRepository;
    }

    // 👉 Mostrar formulario
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Objeto vacío de Alumno para el form
        model.addAttribute("alumno", new Alumno());

        // Listas para los select
        model.addAttribute("paises", paisRepository.findAll());
        model.addAttribute("provincias", provinciaRepository.findAll());
        model.addAttribute("ciudades", ciudadRepository.findAll());
        model.addAttribute("instituciones", institucionAlumnoRepository.findAll());

        return "screens/register"; // tu template
    }

    // 👉 Procesar registro
    @PostMapping("/alumnos/registrar")
    public String registerAlumno(@ModelAttribute("alumno") Alumno alumno) {
        alumnoRepository.save(alumno);
        return "redirect:/register?success"; // redirige con mensaje de éxito
    }
}
