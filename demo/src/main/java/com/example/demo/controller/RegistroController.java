package com.example.demo.controller;

import com.example.demo.model.Alumno;
import com.example.demo.model.Ciudad;
import com.example.demo.model.InstitucionAlumno;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.InstitucionRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;
import com.example.demo.service.InstitucionService;
import com.example.demo.service.RegistroService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/register")
public class RegistroController {

    private final RegistroService registroService;
    private final InstitucionRepository institucionRepository;

    public RegistroController(RegistroService registroService, InstitucionRepository institucionRepository) {
        this.institucionRepository = institucionRepository;
        this.registroService = registroService;
    }

    /**
     * Mostrar formulario de registro
     */
    @GetMapping
    public String mostrarFormulario(Model model) {
    Alumno alumno = new Alumno();
    model.addAttribute("alumno", alumno);

    List<InstitucionAlumno> instituciones = institucionRepository.findAll();
    model.addAttribute("instituciones", instituciones);

    return "register";
    }

    /**
     * Procesar formulario de registro
     */
    @PostMapping
    public String registrarAlumno(@ModelAttribute("alumno") Alumno alumno, Model model) {
        try {
            registroService.registrarUsuario(alumno);
            model.addAttribute("mensaje", "Alumno registrado con éxito");
            model.addAttribute("alumno", new Alumno()); // reinicia el form
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar: " + e.getMessage());
        }
        return "register";
    }
}