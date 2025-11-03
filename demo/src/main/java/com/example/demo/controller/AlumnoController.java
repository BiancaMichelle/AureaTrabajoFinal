package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Curso;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;

@Controller
@RequestMapping("/alumno")
public class AlumnoController {
    
    @GetMapping("/alumno")
    public String alumnoDashboard() {
        return "publico";
    }

    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final ModuloRepository moduloRepository;

    public AlumnoController(OfertaAcademicaRepository ofertaAcademicaRepository,
                          InscripcionRepository inscripcionRepository,
                          UsuarioRepository usuarioRepository,
                          CursoRepository cursoRepository,
                          ModuloRepository moduloRepository) {
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.moduloRepository = moduloRepository;
    }

    // Inscribirse a una oferta académica
    @PostMapping("/inscribirse/{ofertaId}")
    public String inscribirseAOferta(@PathVariable Long ofertaId,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            String dni = authentication.getName();
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            // Verificar si ya está inscrito
            if (inscripcionRepository.findByAlumnoAndOferta(alumno, oferta).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Ya estás inscrito en esta oferta");
                return "redirect:/";
            }

            // Crear inscripción
            Inscripciones inscripcion = new Inscripciones();
            inscripcion.setAlumno(alumno);
            inscripcion.setOferta(oferta);
            inscripcion.setFechaInscripcion(LocalDate.now());
            inscripcion.setEstadoInscripcion(true);
            inscripcion.setObservaciones("Inscripción realizada desde el portal web");
            
            inscripcionRepository.save(inscripcion);

            redirectAttributes.addFlashAttribute("success", 
                "¡Inscripción exitosa! Ahora puedes acceder al aula virtual desde 'Mis Ofertas Académicas'");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error al realizar la inscripción: " + e.getMessage());
        }
        
        return "redirect:/";
    }

    // Ver mis ofertas académicas (inscripciones)
    @GetMapping("/mis-ofertas")
    public String verMisOfertas(Authentication authentication, Model model) {
        String dni = authentication.getName();
        Usuario alumno = usuarioRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        List<Inscripciones> inscripciones = inscripcionRepository.findByAlumno(alumno);
        model.addAttribute("inscripciones", inscripciones);
        
        return "misOfertasAcademicas"; // Asegúrate de que este template existe
    }

    // Acceder al aula de un curso - VERSIÓN CORREGIDA
    @GetMapping("/aula/{inscripcionId}")
    public String accederAlAula(@PathVariable Long inscripcionId,
                              Authentication authentication,
                              Model model) {
        try {
            String dni = authentication.getName();
            System.out.println("🔍 Accediendo al aula para inscripción ID: " + inscripcionId + ", usuario: " + dni);
            
            Inscripciones inscripcion = inscripcionRepository.findById(inscripcionId)
                    .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

            // Verificar que el alumno es el dueño de la inscripción
            if (!inscripcion.getAlumno().getDni().equals(dni)) {
                System.out.println("❌ Acceso denegado: el alumno no es dueño de la inscripción");
                return "redirect:/acceso-denegado";
            }

            // Verificar que la inscripción esté activa
            if (!inscripcion.getEstadoInscripcion()) {
                System.out.println("❌ Inscripción inactiva");
                model.addAttribute("error", "Esta inscripción no está activa");
                return "misOfertasAcademicas";
            }

            OfertaAcademica oferta = inscripcion.getOferta();
            System.out.println("📚 Oferta encontrada: " + oferta.getNombre() + ", tipo: " + oferta.getClass().getSimpleName());
            
            // Si es un curso, cargar módulos y contenido
            if (oferta instanceof Curso) {
                Curso curso = (Curso) oferta;
                System.out.println("🎓 Es un curso: " + curso.getNombre());
                
                // Cargar módulos del curso
                List<Modulo> modulos = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(curso);
                System.out.println("📦 Módulos encontrados: " + modulos.size());
                
                model.addAttribute("curso", curso);
                model.addAttribute("modulos", modulos);
                model.addAttribute("inscripcion", inscripcion);
                
                // Verificar permisos de modificación (solo admin o docente del curso)
                boolean puedeModificar = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")) ||
                        (curso.getDocentes() != null && curso.getDocentes().stream()
                                .anyMatch(docente -> docente.getDni().equals(dni)));
                
                model.addAttribute("puedeModificar", puedeModificar);
                System.out.println("👤 Puede modificar: " + puedeModificar);
                
                System.out.println("✅ Redirigiendo a template: aula");
                return "aula"; // Esto busca templates/aula.html
                
            } else {
                // Para otros tipos de ofertas académicas
                System.out.println("ℹ️  No es un curso, redirigiendo a aula general");
                model.addAttribute("oferta", oferta);
                model.addAttribute("inscripcion", inscripcion);
                return "aula/general"; // Esto buscaría templates/aula/general.html
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error al acceder al aula: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al acceder al aula: " + e.getMessage());
            return "misOfertasAcademicas";
        }
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

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(
            Authentication authentication,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
            @RequestParam(required = false) String genero,
            @RequestParam String correo,
            @RequestParam("numTelefono") String numTelefono,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            System.out.println("[PERFIL] Solicitud de actualización recibida");
            String username = authentication.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(username);
            if (usuarioOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("mensaje", "Usuario no encontrado");
                redirectAttributes.addFlashAttribute("tipo", "error");
                return "redirect:/perfil";
            }

            Usuario usuario = usuarioOpt.get();

            // Actualizar campos permitidos (DNI NO se modifica)
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setFechaNacimiento(fechaNacimiento);
            if (genero != null && !genero.isBlank()) {
                try {
                    usuario.setGenero(TipoGenero.valueOf(genero.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    redirectAttributes.addFlashAttribute("mensaje", "Género inválido");
                    redirectAttributes.addFlashAttribute("tipo", "error");
                    return "redirect:/perfil";
                }
            }
            usuario.setCorreo(correo);
            usuario.setNumTelefono(numTelefono);

            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente");
            redirectAttributes.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al actualizar el perfil: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "error");
        }
        return "redirect:/perfil";
    }

    // Evitar 404 si el navegador hace GET a /perfil/actualizar (p.ej., refresh después del POST)
    @GetMapping("/perfil/actualizar")
    public String redirigirPerfilActualizar() {
        return "redirect:/perfil";
    }


}
