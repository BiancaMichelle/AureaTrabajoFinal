package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.model.Alumno;
import com.example.demo.model.Charla;
import com.example.demo.model.Ciudad;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.InstitucionAlumno;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.model.Seminario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.service.InstitucionService;
import com.example.demo.service.LocacionAPIService;
import com.example.demo.service.RegistroService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PublicoController {

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private RegistroService registroService;

    @Autowired
    private InstitucionService institucionService;

    @Autowired
    private LocacionAPIService locacionApiService;

    @Autowired
    private UbicacionController ubicacionController;

    @GetMapping({"/", "/inicio"})
    public String home(Model model, 
                      @RequestParam(value = "error", required = false) String error,
                      @RequestParam(value = "logout", required = false) String logout,
                      @RequestParam(value = "timeout", required = false) String timeout,
                      @RequestParam(value = "registroError", required = false) String registroError) {

        // Mensajes de autenticación
        if (error != null) {
            model.addAttribute("error", "Credenciales inválidas. Intenta otra vez.");
        }
        if (logout != null) {
            model.addAttribute("message", "Has cerrado sesión exitosamente.");
        }
        if (timeout != null) {
            model.addAttribute("timeout", "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.");
        }
        if (registroError != null) {
            model.addAttribute("registroError", "Error en el registro. Intenta nuevamente.");
        }

        // Preparar datos para el formulario de registro modal
        Alumno alumno = new Alumno();
        alumno.setPais(new Pais());
        alumno.setProvincia(new Provincia());
        alumno.setCiudad(new Ciudad());
        model.addAttribute("alumno", alumno);

        List<InstitucionAlumno> instituciones = institucionService.obtenerTodasLasInstituciones();
        model.addAttribute("instituciones", instituciones);

        try {
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            model.addAttribute("paises", paises);
        } catch (Exception e) {
            // Manejar error, tal vez cargar una lista vacía o de respaldo
            model.addAttribute("paises", List.of());
        }

        // Datos existentes para la página principal
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

    // Endpoints para AJAX - Mantener estos
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

    // Mostrar página de registro
    @GetMapping("/register")
    public String mostrarRegistro(Model model) {
        model.addAttribute("alumno", new Alumno());
        return "screens/register";
    }

    // Nuevo endpoint para registro desde modal
    @PostMapping("/register")
    public String registrarAlumno(@ModelAttribute("alumno") Alumno alumno, 
                                BindingResult result, 
                                Model model,
                                HttpServletRequest request) {
        
        System.out.println("Datos recibidos desde formulario de registro:");
        System.out.println("Nombre: " + alumno.getNombre());
        System.out.println("Email: " + alumno.getCorreo());
        System.out.println("País código: " + (alumno.getPais() != null ? alumno.getPais().getCodigo() : "null"));
        
        if (result.hasErrors()) {
            System.out.println("Errores de validación: " + result.getAllErrors());
            return "redirect:/register?error=true";
        }
        
        try {
            registroService.registrarUsuario(alumno);
            return "redirect:/login?success=true";
        } catch (Exception e) {
            System.out.println("Error al registrar: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/register?error=true";
        }
    }
}