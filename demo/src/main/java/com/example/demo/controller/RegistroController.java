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
import com.example.demo.service.LocacionAPIService;
import com.example.demo.service.RegistroService;

import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/register")
public class RegistroController {

    private final RegistroService registroService;
    private final InstitucionService institucionService;
    private final LocacionAPIService locacionApiService;

    public RegistroController(RegistroService registroService, 
                             InstitucionService institucionService,
                             LocacionAPIService locacionApiService) {
        this.registroService = registroService;
        this.institucionService = institucionService;
        this.locacionApiService = locacionApiService;
    }

    @GetMapping
    public String mostrarFormulario(Model model) {
        Alumno alumno = new Alumno();
        //alumno.setColegioEgreso(new Institucion());
        alumno.setPais(new Pais());
        alumno.setProvincia(new Provincia());
        alumno.setCiudad(new Ciudad());
        //alumno.setColegioEgreso(new Institucion());
        model.addAttribute("alumno", alumno);

        List<InstitucionAlumno> instituciones = institucionService.obtenerTodasLasInstituciones();
        model.addAttribute("instituciones", instituciones);

        // Cargar países
        List<Pais> paises = locacionApiService.obtenerTodosPaises();
        model.addAttribute("paises", paises);

        return "register";
    }

    // Endpoints para AJAX - CON CORS HEADERS
    @GetMapping("/provincias/{paisCode}")
    @ResponseBody
    public ResponseEntity<List<Provincia>> obtenerProvincias(@PathVariable String paisCode, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<Provincia> provincias = locacionApiService.obtenerProvinciasPorPais(paisCode);
        return ResponseEntity.ok(provincias);
    }

    
    @GetMapping("/ciudades/{paisCode}/{provinciaCode}")
    @ResponseBody  
    public ResponseEntity<List<Ciudad>> obtenerCiudades(@PathVariable String paisCode, 
                                                       @PathVariable String provinciaCode,
                                                       HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<Ciudad> ciudades = locacionApiService.obtenerCiudadesPorProvincia(paisCode, provinciaCode);
        return ResponseEntity.ok(ciudades);
    }

    @PostMapping
    public String registrarAlumno(@ModelAttribute("alumno") Alumno alumno, 
                                BindingResult result, 
                                Model model) {
        
        // DEBUG: Ver qué datos llegan
        System.out.println("Datos recibidos:");
        System.out.println("Nombre: " + alumno.getNombre());
        System.out.println("Email: " + alumno.getCorreo());
        System.out.println("País código: " + (alumno.getPais() != null ? alumno.getPais().getCodigo() : "null"));
        //System.out.println("Institución: " + (alumno.getColegioEgreso() != null ? alumno.getColegioEgreso().getId() : "null"));
        
        if (result.hasErrors()) {
            System.out.println("Errores de validación: " + result.getAllErrors());
            recargarDatosFormulario(model);
            return "register";
        }
        
        try {
            registroService.registrarUsuario(alumno);
            return "redirect:/login?registroExitoso=true";
        } catch (Exception e) {
            System.out.println("Error al registrar: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al registrar: " + e.getMessage());
            recargarDatosFormulario(model);
            return "register";
        }
    }
    
    private void recargarDatosFormulario(Model model) {
        model.addAttribute("paises", locacionApiService.obtenerTodosPaises());
        model.addAttribute("instituciones", institucionService.obtenerTodasLasInstituciones());
    }
}