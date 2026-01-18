package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Auditable;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.CursoService;

@Controller
public class OfertaAcademicaController {

    @Value("${app.base-url}")
    private String baseUrl;

    private final CursoService cursoService;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final ModuloRepository moduloRepository;
    private final UsuarioRepository usuarioRepository;

    public OfertaAcademicaController(CursoService cursoService,
            OfertaAcademicaRepository ofertaAcademicaRepository,
            ModuloRepository moduloRepository,
            UsuarioRepository usuarioRepository) {
        this.cursoService = cursoService;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.moduloRepository = moduloRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/ofertaAcademica/{cursoId}")
    public String cargarOferta(@PathVariable Long cursoId, Model model, Authentication authentication) {
        // ✅ Buscar en OfertaAcademicaRepository para soportar Cursos Y Formaciones
        Long cursoIdSeguro = Objects.requireNonNull(cursoId, "El identificador de la oferta no puede ser nulo");

        OfertaAcademica oferta = ofertaAcademicaRepository.findById(cursoIdSeguro)
                .orElseThrow(() -> new RuntimeException("Oferta académica no encontrada"));

        System.out.println(
                "📚 Cargando oferta: " + oferta.getNombre() + " (Tipo: " + oferta.getClass().getSimpleName() + ")");

        boolean puedeModificar = puedeModificarCurso(authentication);

        // Cargar módulos: todos para docentes/admin, solo visibles para alumnos
        List<Modulo> modulos;
        if (puedeModificar) {
            System.out.println("🔓 Usuario con permisos de modificación - cargando TODOS los módulos");
            modulos = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(oferta);
        } else {
            System.out.println("👤 Usuario alumno - cargando SOLO módulos visibles");
            modulos = moduloRepository.findByCursoAndVisibilidadTrueOrderByFechaInicioModuloAsc(oferta);
        }
        
        System.out.println("📋 Total de módulos cargados: " + modulos.size());
        for (Modulo m : modulos) {
            System.out.println("  - " + m.getNombre() + " (visible: " + m.getVisibilidad() + ")");
        }

        model.addAttribute("curso", oferta); // Mantener nombre "curso" para compatibilidad con la vista
        model.addAttribute("puedeModificar", puedeModificar);
        model.addAttribute("modulos", modulos);

        return "aula";
    }

    @PostMapping("/crearModulo")
    @Auditable(action = "CREAR_MODULO", entity = "Modulo")
    public String crearModulo(@RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam(required = false) String objetivos,
            @RequestParam(required = false) String temario,
            @RequestParam(required = false) String bibliografia,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "false") Boolean visibilidad,
            @RequestParam Long cursoId,
            Authentication auth) {
        Long cursoIdSeguro = Objects.requireNonNull(cursoId, "El identificador de la oferta no puede ser nulo");
        
        // Validación de permisos antes de crear (Precondición CU-23)
        validarPermisoDocenteOferta(auth, cursoIdSeguro);

        cursoService.crearModulo(nombre, descripcion, objetivos, temario, bibliografia, fechaInicio, fechaFin, visibilidad, cursoIdSeguro);
        // ✅ Redirigir de vuelta al curso específico
        return "redirect:/ofertaAcademica/" + cursoIdSeguro;
    }

    @PostMapping("/modulo/actualizar")
    @Auditable(action = "ACTUALIZAR_MODULO", entity = "Modulo")
    public String actualizarModulo(@RequestParam UUID moduloId,
            @RequestParam Long cursoId,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam(required = false) String objetivos,
            @RequestParam(required = false) String temario,
            @RequestParam(required = false) String bibliografia,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "false") Boolean visibilidad,
            Authentication auth) {
        Long cursoIdSeguro = Objects.requireNonNull(cursoId, "El identificador de la oferta no puede ser nulo");
        
        validarPermisoDocenteOferta(auth, cursoIdSeguro);

        cursoService.actualizarModulo(moduloId, nombre, descripcion, objetivos, temario, bibliografia, fechaInicio, fechaFin, visibilidad, cursoIdSeguro);
        return "redirect:/ofertaAcademica/" + cursoIdSeguro;
    }

    @PostMapping("/modulo/{moduloId}/visibilidad")
    @Auditable(action = "CAMBIAR_VISIBILIDAD_MODULO", entity = "Modulo")
    public ResponseEntity<Map<String, Object>> actualizarVisibilidadModulo(@PathVariable UUID moduloId,
            @RequestParam(defaultValue = "false") Boolean visibilidad) {
        Modulo moduloActualizado = cursoService.actualizarVisibilidadModulo(moduloId, visibilidad);

        Map<String, Object> body = Map.of(
                "success", Boolean.TRUE,
                "visibilidad", moduloActualizado.getVisibilidad());

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/modulo/{moduloId}/eliminar")
    @Auditable(action = "ELIMINAR_MODULO", entity = "Modulo")
    public ResponseEntity<Map<String, Object>> eliminarModulo(@PathVariable UUID moduloId, Authentication authentication) {
        
        // 1. Obtener el módulo para identificar el curso (Precondición de Seguridad)
        // Nota: Idealmente esto debería hacerse en el servicio, pero necesitamos el ID del curso para validar permisos antes de la operación.
        // Simulamos una búsqueda rápida o delegamos. Aquí optamos por capturar la excepción si falla en el servicio, 
        // pero para permisos correctos necesitamos el cursoId.
        // Dado que CursoService.eliminarModulo busca el módulo, podemos confiar en el servicio SI pasamos el usuario, 
        // o validamos aquí recuperando el módulo (costo de doble query pero seguro).
        
        try {
           // Validación Manual previa para asegurar propiedad (Esto requiere un método de lectura en servicio o repo expuesto)
           // Para simplificar y no romper encapsulamiento excesivo, confiamos en la validación de estado del servicio,
           // pero la validación de USUARIO-CURSO (Propiedad) es crítica.
           // Solución: Buscamos el módulo usando el repositorio (inyectado) o servicio.
           // Como moduloRepository es private en Service, usamos una estrategia de try-catch optimista o 
           // agregamos un método 'obtenerCursoIdDeModulo' en servicio.
           // Por consistencia con actualizaciones anteriores, validaremos permiso recuperando el módulo.
           
           Modulo modulo = cursoService.obtenerModuloPorId(moduloId); // Necesitamos exponer este método o usar repositorio
           validarPermisoDocenteOferta(authentication, modulo.getCurso().getIdOferta());

            cursoService.eliminarModulo(moduloId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalStateException e) {
             // Errores de lógica de negocio (Contenido activo, Estado incorrecto)
             return ResponseEntity.status(409).body(Map.of("success", false, "error", e.getMessage()));
        } catch (RuntimeException e) {
             // Errores de permisos o no encontrado
             return ResponseEntity.status(403).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Error interno del servidor"));
        }
    }

    private boolean puedeModificarCurso(Authentication authentication) {
        if (authentication == null)
            return false;

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") ||
                        auth.getAuthority().equals("DOCENTE"));
    }

    private void validarPermisoDocenteOferta(Authentication auth, Long cursoId) {
        if (auth == null) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        // Si es Admin, tiene permiso total
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (isAdmin) return;

        String dni = auth.getName();
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

        boolean esDocenteAsignado = false;
        if (oferta instanceof Curso curso) {
            esDocenteAsignado = curso.getDocentes().stream()
                    .anyMatch(d -> d.getDni().equals(dni));
        } else if (oferta instanceof Formacion formacion) {
            esDocenteAsignado = formacion.getDocentes().stream()
                    .anyMatch(d -> d.getDni().equals(dni));
        }

        if (!esDocenteAsignado) {
             throw new RuntimeException("Acceso denegado: El docente no está asignado a este curso.");
        }
    }
}