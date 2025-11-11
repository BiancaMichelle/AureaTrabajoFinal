package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.MercadoPagoService;
import com.example.demo.repository.UsuarioRepository;

@Controller
@RequestMapping("/alumno")
public class AlumnoController {
    
    @GetMapping("/alumno")
    public String alumnoDashboard() {
        return "publico";
    }
    
    // Mi Espacio - Dashboard del alumno con calendario
    @GetMapping("/mi-espacio")
    public String miEspacio(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            
            model.addAttribute("alumno", alumno);
            
            return "alumno/mi-espacio";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mi-espacio: " + e.getMessage());
            model.addAttribute("error", "Error al cargar tu espacio");
            return "redirect:/";
        }
    }
    
    // Mis Pagos
    @GetMapping("/mis-pagos")
    public String misPagos(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            
            model.addAttribute("alumno", alumno);
            // Aquí cargarías los pagos del alumno
            
            return "alumno/mis-pagos";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mis-pagos: " + e.getMessage());
            model.addAttribute("error", "Error al cargar tus pagos");
            return "redirect:/";
        }
    }

    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final CursoRepository cursoRepository;
    private final ModuloRepository moduloRepository;
    private final MercadoPagoService mercadoPagoService;

    public AlumnoController(OfertaAcademicaRepository ofertaAcademicaRepository,
                          InscripcionRepository inscripcionRepository,
                          UsuarioRepository usuarioRepository,
                          CursoRepository cursoRepository,
                          ModuloRepository moduloRepository,
                          AlumnoRepository alumnoRepository,
                          MercadoPagoService mercadoPagoService) {
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.moduloRepository = moduloRepository;
        this.alumnoRepository = alumnoRepository;
        this.mercadoPagoService = mercadoPagoService;
    }

    // Inscribirse a una oferta académica - Redirige a Mercado Pago
    @PostMapping("/inscribirse/{ofertaId}")
    public String inscribirseAOferta(@PathVariable Long ofertaId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String dni = authentication.getName();
            System.out.println("💳 Iniciando proceso de inscripción con pago para oferta: " + ofertaId);
            
            // Buscar el usuario
            Usuario usuario = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Buscar la oferta
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            // Verificar si ya está inscrito
            List<Inscripciones> inscripcionesExistentes = inscripcionRepository.findByAlumnoDni(dni);
            boolean yaInscrito = inscripcionesExistentes.stream()
                    .anyMatch(ins -> ins.getOferta().getIdOferta().equals(ofertaId));
            
            if (yaInscrito) {
                redirectAttributes.addFlashAttribute("error", "Ya estás inscrito en esta oferta");
                return "redirect:/publico";
            }

            // Crear preferencia de pago en Mercado Pago
            String urlPago = mercadoPagoService.crearPreferenciaPago(usuario, oferta);
            
            System.out.println("✅ Preferencia creada, redirigiendo a: " + urlPago);
            
            // Redirigir a la URL de pago de Mercado Pago
            return "redirect:" + urlPago;
            
        } catch (Exception e) {
            System.err.println("❌ Error al crear preferencia de pago: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "Error al procesar la inscripción: " + e.getMessage());
            return "redirect:/publico";
        }
    }

    // Ver mis ofertas académicas (inscripciones)
    @GetMapping("/mis-ofertas")
    public String misOfertasAcademicas(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            System.out.println("🎓 Alumno accediendo a mis ofertas: " + dni);
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            
            // ✅ CORREGIDO: Buscar inscripciones del alumno
            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDni(dni);
            
            // Extraer los cursos de las inscripciones
            List<Curso> cursos = inscripciones.stream()
                    .filter(insc -> insc.getOferta() instanceof Curso)
                    .map(insc -> (Curso) insc.getOferta())
                    .collect(Collectors.toList());
            
            System.out.println("📊 Inscripciones encontradas: " + inscripciones.size());
            System.out.println("📊 Cursos encontrados: " + cursos.size());
            
            model.addAttribute("alumno", alumno);
            model.addAttribute("cursos", cursos);
            model.addAttribute("inscripciones", inscripciones); // Para debug
            
            return "misOfertasAcademicas";
            
        } catch (Exception e) {
            System.out.println("❌ Error en misOfertasAcademicas (alumno): " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar tus cursos: " + e.getMessage());
            return "misOfertasAcademicas";
        }
    }

    @GetMapping("/aula/{ofertaId}")
    public String accederAlAula(@PathVariable Long ofertaId,
                              Authentication authentication,
                              Model model) {
        try {
            String dni = authentication.getName();
            System.out.println("🔍 Accediendo al aula para oferta ID: " + ofertaId + ", usuario: " + dni);
            
            // Buscar la inscripción del alumno en esta oferta
            Inscripciones inscripcion = inscripcionRepository.findByAlumnoDniAndOfertaId(dni, ofertaId)
                    .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

            System.out.println("✅ Inscripción encontrada ID: " + inscripcion.getIdInscripcion());

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
                return "aula"; 
                
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
