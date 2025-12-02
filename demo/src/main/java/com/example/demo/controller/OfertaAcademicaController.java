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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.service.CursoService;

@Controller
public class OfertaAcademicaController {

    @Value("${app.base-url}")
    private String baseUrl;

    private final CursoService cursoService;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final ModuloRepository moduloRepository;

    public OfertaAcademicaController(CursoService cursoService,
            OfertaAcademicaRepository ofertaAcademicaRepository,
            ModuloRepository moduloRepository) {
        this.cursoService = cursoService;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.moduloRepository = moduloRepository;
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
    public String crearModulo(@RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam(required = false) String objetivos,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "false") Boolean visibilidad,
            @RequestParam Long cursoId) {
        Long cursoIdSeguro = Objects.requireNonNull(cursoId, "El identificador de la oferta no puede ser nulo");

        cursoService.crearModulo(nombre, descripcion, objetivos, fechaInicio, fechaFin, visibilidad, cursoIdSeguro);
        // ✅ Redirigir de vuelta al curso específico
        return "redirect:/ofertaAcademica/" + cursoIdSeguro;
    }

    @PostMapping("/modulo/actualizar")
    public String actualizarModulo(@RequestParam UUID moduloId,
            @RequestParam Long cursoId,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam(required = false) String objetivos,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "false") Boolean visibilidad) {
        Long cursoIdSeguro = Objects.requireNonNull(cursoId, "El identificador de la oferta no puede ser nulo");

        cursoService.actualizarModulo(moduloId, nombre, descripcion, objetivos, fechaInicio, fechaFin, visibilidad);
        return "redirect:/ofertaAcademica/" + cursoIdSeguro;
    }

    @PostMapping("/modulo/{moduloId}/visibilidad")
    public ResponseEntity<Map<String, Object>> actualizarVisibilidadModulo(@PathVariable UUID moduloId,
            @RequestParam(defaultValue = "false") Boolean visibilidad) {
        Modulo moduloActualizado = cursoService.actualizarVisibilidadModulo(moduloId, visibilidad);

        Map<String, Object> body = Map.of(
                "success", Boolean.TRUE,
                "visibilidad", moduloActualizado.getVisibilidad());

        return ResponseEntity.ok(body);
    }

    private boolean puedeModificarCurso(Authentication authentication) {
        if (authentication == null)
            return false;

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") ||
                        auth.getAuthority().equals("DOCENTE"));
    }
}