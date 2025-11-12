package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Formacion;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;

/**
 * Controlador genérico para inscripciones a ofertas académicas.
 * Permite que tanto ALUMNOS como DOCENTES se inscriban como estudiantes.
 * Valida que un docente NO pueda inscribirse a una oferta donde ya enseña.
 */
@Controller
@RequestMapping("/inscribirse")
public class InscripcionController {

    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final InscripcionRepository inscripcionRepository;

    public InscripcionController(
            UsuarioRepository usuarioRepository,
            DocenteRepository docenteRepository,
            OfertaAcademicaRepository ofertaAcademicaRepository,
            InscripcionRepository inscripcionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @PostMapping("/{ofertaId}")
    public String inscribirseAOferta(@PathVariable Long ofertaId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String dni = authentication.getName();
            String rol = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority())
                    .orElse("");

            System.out.println("📝 Iniciando proceso de inscripción para oferta: " + ofertaId + " (Rol: " + rol + ")");
            
            // Buscar el usuario (puede ser Alumno o Docente)
            Usuario usuario = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            System.out.println("👤 Usuario encontrado: " + usuario.getNombre() + " " + usuario.getApellido() + " (Tipo: " + usuario.getClass().getSimpleName() + ")");
            
            // Buscar la oferta
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            System.out.println("📚 Oferta encontrada: " + oferta.getNombre() + " (Tipo: " + oferta.getClass().getSimpleName() + ")");

            // ========================================
            // VALIDACIÓN ESPECIAL PARA DOCENTES
            // ========================================
            if ("DOCENTE".equals(rol)) {
                Docente docente = docenteRepository.findByDni(dni)
                        .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
                
                // Verificar si el docente ya enseña en esta oferta
                boolean yaEsDocenteAqui = false;
                
                if (oferta instanceof Curso) {
                    Curso curso = (Curso) oferta;
                    yaEsDocenteAqui = curso.getDocentes() != null && curso.getDocentes().stream()
                            .anyMatch(d -> d.getId().equals(docente.getId()));
                } else if (oferta instanceof Formacion) {
                    Formacion formacion = (Formacion) oferta;
                    yaEsDocenteAqui = formacion.getDocentes() != null && formacion.getDocentes().stream()
                            .anyMatch(d -> d.getId().equals(docente.getId()));
                }
                
                if (yaEsDocenteAqui) {
                    System.out.println("❌ El docente ya enseña en esta oferta");
                    redirectAttributes.addFlashAttribute("error", 
                        "No puedes inscribirte como alumno a una oferta donde ya eres docente");
                    return "redirect:/publico";
                }
                
                System.out.println("✅ El docente NO enseña en esta oferta, puede inscribirse como alumno");
            }

            // Verificar si ya está inscrito
            List<Inscripciones> inscripcionesExistentes = inscripcionRepository.findByAlumnoDni(dni);
            boolean yaInscrito = inscripcionesExistentes.stream()
                    .anyMatch(ins -> ins.getOferta().getIdOferta().equals(ofertaId));
            
            if (yaInscrito) {
                redirectAttributes.addFlashAttribute("error", "Ya estás inscrito en esta oferta");
                return redirectSegunRol(rol);
            }

            // ========================================
            // CREAR INSCRIPCIÓN usando el Usuario directamente
            // ========================================
            Inscripciones nuevaInscripcion = new Inscripciones();
            nuevaInscripcion.setAlumno(usuario); // ✅ Ahora acepta Usuario (Alumno o Docente)
            nuevaInscripcion.setOferta(oferta);
            nuevaInscripcion.setEstadoInscripcion(true);
            nuevaInscripcion.setFechaInscripcion(LocalDate.now());
            
            inscripcionRepository.save(nuevaInscripcion);
            
            System.out.println("✅ Inscripción creada exitosamente para " + usuario.getNombre() + " " + usuario.getApellido());
            
            redirectAttributes.addFlashAttribute("success", 
                "¡Te has inscrito exitosamente a " + oferta.getNombre() + "!");
            
            return redirectSegunRol(rol);
            
        } catch (Exception e) {
            System.err.println("❌ Error al crear inscripción: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "Hubo un error al procesar tu inscripción. Por favor, intenta nuevamente.");
            return "redirect:/publico";
        }
    }

    /**
     * Redirige al usuario según su rol después de la inscripción
     */
    private String redirectSegunRol(String rol) {
        if ("DOCENTE".equals(rol)) {
            return "redirect:/docente/mis-ofertas";
        } else {
            return "redirect:/alumno/mis-ofertas";
        }
    }
}
