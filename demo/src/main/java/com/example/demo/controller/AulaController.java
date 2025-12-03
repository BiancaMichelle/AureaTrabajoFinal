package com.example.demo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.enums.EstadoAsistencia;
import com.example.demo.model.Asistencia;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ClaseRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.AsistenciaService;

@Controller
@RequestMapping("/aula")
public class AulaController {

    private final OfertaAcademicaRepository ofertaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsistenciaService asistenciaService;
    private final ClaseRepository claseRepository;

    public AulaController(OfertaAcademicaRepository ofertaRepository,
                          InscripcionRepository inscripcionRepository,
                          UsuarioRepository usuarioRepository,
                          AsistenciaService asistenciaService,
                          ClaseRepository claseRepository) {
        this.ofertaRepository = ofertaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.asistenciaService = asistenciaService;
        this.claseRepository = claseRepository;
    }

    @GetMapping("/oferta/{id}/participantes")
    public String verParticipantes(@PathVariable Long id, Model model, Authentication auth) {
        Long ofertaIdSeguro = Objects.requireNonNull(id, "El id de la oferta es requerido");
        OfertaAcademica oferta = ofertaRepository.findById(ofertaIdSeguro)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        
        String currentUserDni = auth.getName(); // Assuming username is DNI or email, need to check
        // Actually, in Spring Security, getName() usually returns the username.
        // We need to find the user to get their DNI if username is not DNI.
        // Assuming username is DNI for now based on other controllers, or we fetch user.
        
        Usuario currentUser = usuarioRepository.findByDni(currentUserDni)
             .or(() -> usuarioRepository.findByCorreo(currentUserDni))
             .orElseThrow(() -> new RuntimeException("Usuario actual no encontrado"));

        model.addAttribute("oferta", oferta);
        model.addAttribute("currentUser", currentUser);
        
        return "aula/participantes";
    }

    // API Endpoints for the frontend logic

    @GetMapping("/api/oferta/{id}/usuarios")
    @ResponseBody
    public List<Map<String, Object>> getUsuariosOferta(@PathVariable Long id, Authentication auth) {
        String currentUserDni = auth.getName();
        Usuario currentUser = usuarioRepository.findByDni(currentUserDni)
             .or(() -> usuarioRepository.findByCorreo(currentUserDni))
             .orElse(null);
             
        List<Inscripciones> inscripciones = inscripcionRepository.findByOfertaIdOferta(id);
        
        return inscripciones.stream()
                .map(Inscripciones::getAlumno)
                .filter(u -> {
                    // Filter out the current user
                    if (currentUser != null && u.getDni().equals(currentUser.getDni())) return false;
                    
                    // Filter out teachers and admins
                    boolean isTeacherOrAdmin = u.getRoles().stream()
                        .anyMatch(r -> r.getNombre().equalsIgnoreCase("DOCENTE") || 
                                       r.getNombre().equalsIgnoreCase("ADMIN"));
                    return !isTeacherOrAdmin;
                })
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("dni", u.getDni());
                    map.put("name", u.getNombre() + " " + u.getApellido());
                    map.put("email", u.getCorreo());
                    String role = u.getRoles().isEmpty() ? "ESTUDIANTE" : u.getRoles().iterator().next().getNombre();
                    map.put("role", role);
                    map.put("avatar", (u.getNombre().substring(0, 1) + u.getApellido().substring(0, 1)).toUpperCase());
                    map.put("color", "bg-blue-100 text-blue-600");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/api/oferta/{id}/calendario")
    @ResponseBody
    public Map<String, Object> getCalendarioOferta(@PathVariable Long id) {
        Long ofertaIdSeguro = Objects.requireNonNull(id, "El id de la oferta es requerido");
        OfertaAcademica oferta = ofertaRepository.findById(ofertaIdSeguro)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        
        Map<String, Object> data = new HashMap<>();
        data.put("fechaInicio", oferta.getFechaInicio());
        data.put("fechaFin", oferta.getFechaFin());
        
        List<String> diasClase = new ArrayList<>();
        if (oferta.getHorarios() != null) {
            diasClase = oferta.getHorarios().stream()
                    .map(h -> h.getDia().name()) // LUNES, MARTES, etc.
                    .collect(Collectors.toList());
        }
        data.put("diasClase", diasClase);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        Map<String, List<Map<String, Object>>> clasesPorFecha = new HashMap<>();
        claseRepository.findByModuloCursoIdOferta(ofertaIdSeguro).stream()
                .filter(clase -> clase.getInicio() != null)
                .forEach(clase -> {
                    LocalDateTime inicio = clase.getInicio();
                    LocalDateTime fin = clase.getFin();
                    String fechaKey = inicio.toLocalDate().toString();

                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", clase.getIdClase().toString());
                    dto.put("titulo", clase.getTitulo());
                    dto.put("horaInicio", timeFormatter.format(inicio.toLocalTime()));
                    dto.put("horaFin", fin != null ? timeFormatter.format(fin.toLocalTime()) : null);

                    clasesPorFecha.computeIfAbsent(fechaKey, k -> new ArrayList<>()).add(dto);
                });
        data.put("clasesPorFecha", clasesPorFecha);
        
        return data;
    }

    @GetMapping("/api/oferta/{id}/asistencia/{dni}")
    @ResponseBody
    public Map<String, Map<String, String>> getAsistenciaAlumno(@PathVariable Long id, @PathVariable String dni) {
        List<Asistencia> asistencias = asistenciaService.getAsistenciasPorAlumnoYOferta(id, dni);
        
        Map<String, Map<String, String>> records = new HashMap<>();
        for (Asistencia a : asistencias) {
            String status = "absent";
            if (a.getEstado() == EstadoAsistencia.PRESENTE) status = "present";
            else if (a.getEstado() == EstadoAsistencia.TARDANZA) status = "late";

            String dateKey = a.getFecha().toString();
            String classKey = a.getClase() != null ? a.getClase().getIdClase().toString() : "__default__";

            records.computeIfAbsent(dateKey, k -> new HashMap<>()).put(classKey, status);
        }
        return records;
    }

    @PostMapping("/api/asistencia/registrar")
    @ResponseBody
    public ResponseEntity<?> registrarAsistencia(@RequestBody Map<String, Object> payload) {
        try {
            Long ofertaId = Long.valueOf(payload.get("ofertaId").toString());
            String dni = payload.get("dni").toString();
            LocalDate fecha = LocalDate.parse(payload.get("fecha").toString());
            String estadoStr = payload.get("estado").toString();
            UUID claseId = null;
            if (payload.containsKey("claseId") && payload.get("claseId") != null) {
                String claseIdStr = payload.get("claseId").toString();
                if (!claseIdStr.isBlank()) {
                    claseId = UUID.fromString(claseIdStr);
                }
            }
            
            EstadoAsistencia estado = EstadoAsistencia.AUSENTE;
            if ("present".equals(estadoStr)) estado = EstadoAsistencia.PRESENTE;
            else if ("late".equals(estadoStr)) estado = EstadoAsistencia.TARDANZA;
            
            asistenciaService.registrarAsistencia(ofertaId, dni, fecha, estado, claseId);
            
            return ResponseEntity.ok().body(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
