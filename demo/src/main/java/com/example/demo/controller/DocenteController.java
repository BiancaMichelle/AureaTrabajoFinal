package com.example.demo.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Formacion;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.FormacionRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;

@Controller
@RequestMapping("/docente")
public class DocenteController {

    @Value("${app.base-url}")
    private String baseUrl;

    private final CursoRepository cursoRepository;
    private final FormacionRepository formacionRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloRepository moduloRepository;
    private final InscripcionRepository inscripcionRepository;

    public DocenteController(CursoRepository cursoRepository,
                           FormacionRepository formacionRepository,
                           OfertaAcademicaRepository ofertaAcademicaRepository,
                           UsuarioRepository usuarioRepository,
                           ModuloRepository moduloRepository,
                           InscripcionRepository inscripcionRepository) {
        this.cursoRepository = cursoRepository;
        this.formacionRepository = formacionRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.usuarioRepository = usuarioRepository;
        this.moduloRepository = moduloRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    // Mi Espacio - Dashboard del docente con calendario
    @GetMapping("/mi-espacio")
    public String miEspacio(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario docente = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            model.addAttribute("docente", docente);
            model.addAttribute("esDocente", true);
            
            return "docente/mi-espacio";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mi-espacio (docente): " + e.getMessage());
            model.addAttribute("error", "Error al cargar tu espacio");
            return "redirect:/";
        }
    }
    
    // Mis Pagos
    @GetMapping("/mis-pagos")
    public String misPagos(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario docente = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            model.addAttribute("docente", docente);
            model.addAttribute("esDocente", true);
            
            return "docente/mis-pagos";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mis-pagos (docente): " + e.getMessage());
            model.addAttribute("error", "Error al cargar tus pagos");
            return "redirect:/";
        }
    }

    @GetMapping("/mis-ofertas")
    public String misOfertas(Model model, Principal principal) {
        try {
            String username = principal.getName();
            System.out.println("🔍 Username del Principal: " + username);
            
            // Buscar por DNI
            Optional<Usuario> docenteOpt = usuarioRepository.findByDni(username);
            
            if (docenteOpt.isEmpty()) {
                System.out.println("❌ No se encontró docente con DNI: " + username);
                model.addAttribute("error", "No se pudo encontrar docente con DNI: " + username);
                return "misOfertasAcademicas";
            }
            
            Usuario docente = docenteOpt.get();
            System.out.println("✅ Docente encontrado: " + docente.getNombre() + " " + docente.getApellido());
            
            // ========================================
            // 1. OFERTAS DONDE ES DOCENTE
            // ========================================
            List<Curso> cursosDelDocente = cursoRepository.findByDocentesId(docente.getId());
            System.out.println("�‍🏫 Cursos como docente: " + cursosDelDocente.size());
            
            List<Formacion> formacionesDelDocente = formacionRepository.findByDocentesId(docente.getId());
            System.out.println("👨‍🏫 Formaciones como docente: " + formacionesDelDocente.size());
            
            List<OfertaAcademica> ofertasComoDocente = new ArrayList<>();
            ofertasComoDocente.addAll(cursosDelDocente);
            ofertasComoDocente.addAll(formacionesDelDocente);
            
            // ========================================
            // 2. OFERTAS DONDE ESTÁ INSCRITO COMO ALUMNO
            // ========================================
            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDni(username);
            System.out.println("🎓 Inscripciones como alumno: " + inscripciones.size());
            
            List<OfertaAcademica> ofertasComoAlumno = inscripciones.stream()
                    .filter(ins -> ins.getEstadoInscripcion())
                    .map(Inscripciones::getOferta)
                    .collect(Collectors.toList());
            
            // ========================================
            // 3. COMBINAR TODAS (sin duplicados)
            // ========================================
            List<OfertaAcademica> todasLasOfertas = new ArrayList<>(ofertasComoDocente);
            
            // Agregar ofertas como alumno que NO estén ya en la lista (evitar duplicados)
            for (OfertaAcademica ofertaAlumno : ofertasComoAlumno) {
                boolean yaExiste = todasLasOfertas.stream()
                        .anyMatch(o -> o.getIdOferta().equals(ofertaAlumno.getIdOferta()));
                if (!yaExiste) {
                    todasLasOfertas.add(ofertaAlumno);
                }
            }
            
            System.out.println("📋 Total de ofertas académicas: " + todasLasOfertas.size());
            
            // Debug: mostrar info de cada oferta
            for (OfertaAcademica oferta : todasLasOfertas) {
                String tipo = oferta instanceof Curso ? "Curso" : "Formación";
                boolean esDocente = ofertasComoDocente.stream()
                        .anyMatch(o -> o.getIdOferta().equals(oferta.getIdOferta()));
                boolean esAlumno = ofertasComoAlumno.stream()
                        .anyMatch(o -> o.getIdOferta().equals(oferta.getIdOferta()));
                
                String roles = esDocente && esAlumno ? "DOCENTE + ALUMNO" : (esDocente ? "DOCENTE" : "ALUMNO");
                System.out.println("   - " + tipo + ": " + oferta.getNombre() + " (" + roles + ")");
            }
            
            model.addAttribute("cursos", todasLasOfertas);
            model.addAttribute("ofertasComoDocente", ofertasComoDocente); // Para diferenciar en la vista
            model.addAttribute("ofertasComoAlumno", ofertasComoAlumno);
            model.addAttribute("docente", docente);
            model.addAttribute("esDocente", true);
            
            return "misOfertasAcademicas";
            
        } catch (Exception e) {
            System.err.println("❌ Error en misOfertas: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar tus ofertas académicas: " + e.getMessage());
            return "misOfertasAcademicas";
        }
    }

    // Acceder al aula de una oferta académica (Curso o Formación) como DOCENTE
    @GetMapping("/aula/{cursoId}")
    public String accederAlAula(@PathVariable Long cursoId,
                            Principal principal,
                            Model model) {
        try {
            String dni = principal.getName();
            System.out.println("🎓 Docente accediendo al aula para oferta ID: " + cursoId + ", usuario: " + dni);
            
            // Buscar la oferta académica (puede ser Curso o Formación)
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(cursoId)
                    .orElseThrow(() -> new RuntimeException("Oferta académica no encontrada"));

            System.out.println("📋 Tipo de oferta: " + oferta.getClass().getSimpleName());
            
            // Verificar si el docente está asignado a esta oferta
            boolean esDocenteDeLaOferta = false;
            List<Docente> docentesAsignados = new ArrayList<>();
            
            if (oferta instanceof Curso) {
                Curso curso = (Curso) oferta;
                docentesAsignados = curso.getDocentes();
                esDocenteDeLaOferta = curso.getDocentes().stream()
                        .anyMatch(docente -> docente.getDni().equals(dni));
            } else if (oferta instanceof Formacion) {
                Formacion formacion = (Formacion) oferta;
                docentesAsignados = formacion.getDocentes();
                esDocenteDeLaOferta = formacion.getDocentes().stream()
                        .anyMatch(docente -> docente.getDni().equals(dni));
            }
            
            System.out.println("� Docentes asignados:");
            for (Docente docente : docentesAsignados) {
                System.out.println("   - " + docente.getDni() + " | " + docente.getNombre());
            }
            
            System.out.println("🔍 Es docente de la oferta: " + esDocenteDeLaOferta);
            System.out.println("🔍 DNI buscado: " + dni);
            
            if (!esDocenteDeLaOferta) {
                System.out.println("❌ Acceso denegado: el docente no está asignado a esta oferta");
                return "redirect:/docente/mis-ofertas";
            }

            // Buscar módulos de esta oferta
            List<Modulo> modulos = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(oferta);
            Usuario docente = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            model.addAttribute("curso", oferta); // Mantener nombre "curso" para compatibilidad con la vista
            model.addAttribute("modulos", modulos);
            model.addAttribute("docente", docente);
            model.addAttribute("puedeModificar", true);
            
            System.out.println("✅ Model attributes:");
            System.out.println("   - puedeModificar: " + true);
            System.out.println("   - docente: " + docente.getNombre());
            System.out.println("   - oferta: " + oferta.getNombre());
            System.out.println("   - modulos: " + modulos.size());
            
            return "aula";
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/docente/mis-ofertas";
        }
    }
}