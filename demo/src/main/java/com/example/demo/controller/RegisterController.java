package com.example.demo.controller;

import java.util.List;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Alumno;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.model.Ciudad;
import com.example.demo.model.InstitucionAlumno;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.InstitucionRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;
import com.example.demo.service.InstitucionService;
import com.example.demo.service.LocacionAPIService;
import com.example.demo.service.RegistroService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Controller
public class RegisterController {

    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;
    private final InstitucionRepository institucionAlumnoRepository;
    private final RegistroService registroService;
    private final InstitucionService institucionService; // ✅ Agregar
    private final LocacionAPIService locacionApiService; // ✅ Agregar

    public RegisterController(PaisRepository paisRepository,
                              ProvinciaRepository provinciaRepository,
                              CiudadRepository ciudadRepository,
                              InstitucionRepository institucionAlumnoRepository,
                              RegistroService registroService,
                              InstitucionService institucionService, // ✅ Inyectar
                              LocacionAPIService locacionApiService) { // ✅ Inyectar
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
        this.institucionAlumnoRepository = institucionAlumnoRepository;
        this.registroService = registroService;
        this.institucionService = institucionService;
        this.locacionApiService = locacionApiService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // ✅ Usar la misma lógica que en PublicoController
        Alumno alumno = new Alumno();
        alumno.setPais(new Pais());
        alumno.setProvincia(new Provincia());
        alumno.setCiudad(new Ciudad());
        model.addAttribute("alumno", alumno);

        // ✅ Usar institucionService en lugar de repository directo
        List<InstitucionAlumno> instituciones = institucionService.obtenerTodasLasInstituciones();
        model.addAttribute("instituciones", instituciones);

        try {
            // ✅ Usar locacionApiService para obtener países
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            model.addAttribute("paises", paises);
        } catch (Exception e) {
            // Manejar error, cargar lista vacía
            model.addAttribute("paises", List.of());
        }

        return "screens/register";
    }

    // ✅ Mantener endpoints AJAX para el formulario
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

    // ✅ CAMBIAR EL ENDPOINT POST para que coincida con PublicoController
    @PostMapping("/register")
    public String registerAlumno(@ModelAttribute("alumno") Alumno alumno,
                                @RequestParam(value = "paisCodigo", required = false) String paisCodigo,
                                @RequestParam(value = "provinciaCodigo", required = false) String provinciaCodigo,
                                @RequestParam(value = "ciudadId", required = false) Long ciudadId,
                                BindingResult result, 
                                Model model) {
        
        System.out.println("✅ FORMULARIO RECIBIDO:");
        System.out.println("País código recibido: " + paisCodigo);
        System.out.println("Provincia código recibido: " + provinciaCodigo);
        System.out.println("Ciudad ID recibido: " + ciudadId);
        
        // Establecer objetos si se recibieron
        if (paisCodigo != null && !paisCodigo.isEmpty()) {
            alumno.setPais(new Pais());
            alumno.getPais().setCodigo(paisCodigo);
        }
        
        if (provinciaCodigo != null && !provinciaCodigo.isEmpty()) {
            alumno.setProvincia(new Provincia());
            alumno.getProvincia().setCodigo(provinciaCodigo);
        }
        
        if (ciudadId != null) {
            alumno.setCiudad(new Ciudad());
            alumno.getCiudad().setId(ciudadId);
        }
        
        try {
            registroService.registrarUsuario(alumno);
            return "redirect:/login?success";
        } catch (Exception e) {
            System.out.println("❌ Error al registrar: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al registrar: " + e.getMessage());
            recargarDatosFormulario(model);
            return "screens/register";
        }
    }

    // ✅ Método auxiliar para recargar datos del formulario
    private void recargarDatosFormulario(Model model) {
        try {
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            model.addAttribute("paises", paises);
        } catch (Exception e) {
            model.addAttribute("paises", List.of());
        }
        
        List<InstitucionAlumno> instituciones = institucionService.obtenerTodasLasInstituciones();
        model.addAttribute("instituciones", instituciones);
    }
}