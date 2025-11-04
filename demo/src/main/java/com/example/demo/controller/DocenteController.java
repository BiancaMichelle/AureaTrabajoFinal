package com.example.demo.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Modulo;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.UsuarioRepository;

@Controller
@RequestMapping("/docente")
public class DocenteController {

    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloRepository moduloRepository;

    public DocenteController(CursoRepository cursoRepository, 
                           UsuarioRepository usuarioRepository,
                           ModuloRepository moduloRepository) {
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.moduloRepository = moduloRepository;
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
            
            // Buscar cursos donde el docente esté asignado
            List<Curso> cursosDelDocente = cursoRepository.findByDocentesId(docente.getId());
            System.out.println("📚 Cursos encontrados: " + cursosDelDocente.size());
            
            // Debug: mostrar info de cada curso
            for (Curso curso : cursosDelDocente) {
                System.out.println("   - Curso: " + curso.getNombre() + " (ID: " + curso.getIdOferta() + ")");
            }
            
            model.addAttribute("cursos", cursosDelDocente);
            model.addAttribute("docente", docente);
            model.addAttribute("esDocente", true); // Para el template
            
            return "misOfertasAcademicas";
            
        } catch (Exception e) {
            System.err.println("❌ Error en misOfertas: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar tus cursos: " + e.getMessage());
            return "misOfertasAcademicas";
        }
    }

    // Acceder al aula de un curso como DOCENTE
    @GetMapping("/aula/{cursoId}")
    public String accederAlAula(@PathVariable Long cursoId,
                            Principal principal,
                            Model model) {
        try {
            String dni = principal.getName();
            System.out.println("🎓 Docente accediendo al aula para curso ID: " + cursoId + ", usuario: " + dni);
            
            Curso curso = cursoRepository.findById(cursoId)
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

            System.out.println("📋 Docentes del curso:");
            for (Docente docente : curso.getDocentes()) {
                System.out.println("   - " + docente.getDni() + " | " + docente.getNombre());
            }
            
            boolean esDocenteDelCurso = curso.getDocentes().stream()
                    .anyMatch(docente -> docente.getDni().equals(dni));
            
            System.out.println("🔍 Es docente del curso: " + esDocenteDelCurso);
            System.out.println("🔍 DNI buscado: " + dni);
            
            if (!esDocenteDelCurso) {
                System.out.println("❌ Acceso denegado: el docente no está asignado a este curso");
                return "redirect:/docente/mis-ofertas";
            }

            List<Modulo> modulos = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(curso);
            Usuario docente = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            model.addAttribute("curso", curso);
            model.addAttribute("modulos", modulos);
            model.addAttribute("docente", docente);
            model.addAttribute("puedeModificar", true);
            
            System.out.println("✅ Model attributes:");
            System.out.println("   - puedeModificar: " + true);
            System.out.println("   - docente: " + docente.getNombre());
            System.out.println("   - curso: " + curso.getNombre());
            System.out.println("   - modulos: " + modulos.size());
            
            return "aula";
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/docente/mis-ofertas";
        }
    }
}