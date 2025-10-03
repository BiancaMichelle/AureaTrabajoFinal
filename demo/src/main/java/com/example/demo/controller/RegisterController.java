package com.example.demo.controller;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.model.Alumno;
import com.example.demo.model.Ciudad;
import com.example.demo.model.InstitucionAlumno;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.InstitucionRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;
import com.example.demo.service.InstitucionService;
import com.example.demo.service.LocacionAPIService;
import com.example.demo.service.RegistroService;

import jakarta.validation.Valid;
@Controller
public class RegisterController {

    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;
    private final InstitucionRepository institucionAlumnoRepository;
    private final RegistroService registroService;
    private final InstitucionService institucionService; // ✅ Agregar
    private final LocacionAPIService locacionApiService; // ✅ Agregar
    private final AlumnoRepository alumnoRepository;

    public RegisterController(PaisRepository paisRepository,
                                AlumnoRepository alumnoRepository,
                              ProvinciaRepository provinciaRepository,
                              CiudadRepository ciudadRepository,
                              InstitucionRepository institucionAlumnoRepository,
                              RegistroService registroService,
                              InstitucionService institucionService, // ✅ Inyectar
                              LocacionAPIService locacionApiService) { // ✅ Inyectar
        this.paisRepository = paisRepository;
        this.alumnoRepository = alumnoRepository;
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


    @PostMapping("/register")
    public String registerAlumno(@Valid @ModelAttribute("alumno") Alumno alumno,
                                BindingResult result,
                                @RequestParam(value = "paisCodigo", required = false) String paisCodigo,
                                @RequestParam(value = "provinciaCodigo", required = false) String provinciaCodigo,
                                @RequestParam(value = "ciudadId", required = false) Long ciudadId,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Model model) {
        
        System.out.println("✅ FORMULARIO RECIBIDO - Iniciando validaciones");
        System.out.println("📝 Datos recibidos:");
        System.out.println("   - DNI: " + alumno.getDni());
        System.out.println("   - Nombre: " + alumno.getNombre());
        System.out.println("   - Email: " + alumno.getCorreo());
        System.out.println("   - País código: " + paisCodigo);
        System.out.println("   - Provincia código: " + provinciaCodigo);
        System.out.println("   - Ciudad ID: " + ciudadId);

        // Validar que las contraseñas coincidan
        if (!alumno.getContraseña().equals(confirmPassword)) {
            result.rejectValue("contraseña", "error.alumno", "Las contraseñas no coinciden");
        }

        // Validar edad mínima
        if (alumno.getFechaNacimiento() != null) {
            Period edad = Period.between(alumno.getFechaNacimiento(), LocalDate.now());
            if (edad.getYears() < 16) {
                result.rejectValue("fechaNacimiento", "error.alumno", "Debes tener al menos 16 años");
            }
        }

        if (result.hasErrors()) {
            System.out.println("❌ Errores de validación encontrados:");
            result.getAllErrors().forEach(error -> 
                System.out.println(" - " + error.getDefaultMessage())
            );
            
            recargarDatosFormulario(model);
            return "screens/register";
        }

        try {
            // DEBUG: Verificar el estado del alumno antes del registro
            System.out.println("🔍 Estado del alumno antes del registro:");
            System.out.println("   - País: " + (alumno.getPais() != null ? alumno.getPais().getCodigo() : "null"));
            System.out.println("   - Provincia: " + (alumno.getProvincia() != null ? alumno.getProvincia().getCodigo() : "null"));
            System.out.println("   - Ciudad: " + (alumno.getCiudad() != null ? alumno.getCiudad().getId() : "null"));

            // Pasar los códigos/IDs al servicio para que busque las entidades completas
            registroService.registrarUsuario(alumno, paisCodigo, provinciaCodigo, ciudadId);
            
            System.out.println("🎉 Registro exitoso, redirigiendo a login...");
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

    // Método temporal para verificar la conexión a la base de datos
@GetMapping("/test-db")
@ResponseBody
public String testDatabase() {
    try {
        long countPaises = paisRepository.count();
        long countProvincias = provinciaRepository.count();
        long countCiudades = ciudadRepository.count();
        long countAlumnos = alumnoRepository.count();
        
        return String.format(
            "✅ Base de datos conectada:<br>" +
            " - Países: %d<br>" +
            " - Provincias: %d<br>" +
            " - Ciudades: %d<br>" +
            " - Alumnos: %d", 
            countPaises, countProvincias, countCiudades, countAlumnos
        );
    } catch (Exception e) {
        return "❌ Error conectando a la base de datos: " + e.getMessage();
    }
}
}