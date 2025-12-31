package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Clase;
import com.example.demo.model.Modulo;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ClaseService;
import com.example.demo.service.JitsiClaseService;

@Controller
@RequestMapping("/clase")
public class ClaseController {

    @Value("${app.base-url}")
    private String baseUrl;

    private final ClaseService claseService;
    private final JitsiClaseService jitsiClaseService;
    private final ModuloRepository moduloRepository;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.example.demo.ia.service.ChatServiceSimple chatServiceSimple;
    private final com.example.demo.repository.MaterialRepository materialRepository;
    private final com.example.demo.repository.ArchivoRepository archivoRepository;
    private final com.example.demo.repository.DocenteRepository docenteRepository;

    public ClaseController(ClaseService claseService,
            JitsiClaseService jitsiClaseService,
            ModuloRepository moduloRepository,
            CursoRepository cursoRepository,
            UsuarioRepository usuarioRepository,
            com.example.demo.ia.service.ChatServiceSimple chatServiceSimple,
            com.example.demo.repository.MaterialRepository materialRepository,
            com.example.demo.repository.ArchivoRepository archivoRepository,
            com.example.demo.repository.DocenteRepository docenteRepository) {
        this.claseService = claseService;
        this.jitsiClaseService = jitsiClaseService;
        this.moduloRepository = moduloRepository;
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.chatServiceSimple = chatServiceSimple;
        this.materialRepository = materialRepository;
        this.archivoRepository = archivoRepository;
        this.docenteRepository = docenteRepository;
    }

    @PostMapping("/crear")
    public String crearClase(@RequestParam String titulo,
            @RequestParam String descripcion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam UUID moduloId,
            @RequestParam Integer duracion,
            @RequestParam(required = false, defaultValue = "false") Boolean asistenciaAutomatica,
            @RequestParam(required = false, defaultValue = "false") Boolean transcripcionHabilitada,
            Principal principal) {
        try {
            System.out.println("🎯 Creando clase con Jitsi:");
            System.out.println("   - Título: " + titulo);
            System.out.println("   - Módulo ID: " + moduloId);

            Clase clase = new Clase();
            clase.setTitulo(titulo);
            clase.setDescripcion(descripcion);
            clase.setInicio(inicio);
            clase.setFin(inicio.plusHours(duracion));
            clase.setAsistenciaAutomatica(asistenciaAutomatica);
            clase.setTranscripcionHabilitada(transcripcionHabilitada);

            Clase claseCreada = claseService.crearClase(clase, moduloId, principal.getName());

            Modulo modulo = moduloRepository.findById(moduloId)
                    .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));
            com.example.demo.model.OfertaAcademica curso = modulo.getCurso();

            System.out.println("✅ Clase Jitsi creada exitosamente");

            return "redirect:/docente/aula/" + curso.getIdOferta();

        } catch (Exception e) {
            System.out.println("❌ Error al crear clase Jitsi: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/docente/mis-ofertas?error=" + e.getMessage();
        }
    }

    @GetMapping("/unirse/{claseId}")
    public String unirseAClase(@PathVariable UUID claseId, Principal principal, Model model) {
        try {
            System.out.println("🎯 Uniéndose a clase Jitsi ID: " + claseId);

            String meetingUrl = claseService.unirseAClase(claseId, principal.getName());
            Clase clase = claseService.findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));

            Usuario usuario = usuarioRepository.findByDni(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean esDocente = usuario.getRoles().stream()
                    .anyMatch(r -> r.getNombre().equals("DOCENTE") || r.getNombre().equals("ADMIN"));

            model.addAttribute("meetingUrl", meetingUrl);
            model.addAttribute("clase", clase);
            model.addAttribute("codigoMeet", clase.getRoomName());
            model.addAttribute("proveedor", "Jitsi Meet");
            model.addAttribute("reunionActiva", true);
            model.addAttribute("usuario", usuario);
            model.addAttribute("esDocente", esDocente);

            return "clase-VideoConferencia";

        } catch (Exception e) {
            System.out.println("❌ Error en unirseAClase Jitsi: " + e.getMessage());
            model.addAttribute("error", "No se pudo cargar la sala de Jitsi: " + e.getMessage());
            return "clase-VideoConferencia";
        }
    }

    @GetMapping("/detalle/{claseId}")
    public String detalleClase(@PathVariable UUID claseId, Model model) {
        Optional<Clase> claseOpt = claseService.findById(claseId);

        if (claseOpt.isEmpty()) {
            model.addAttribute("error", "Clase no encontrada");
            return "clase-VideoConferencia";
        }

        model.addAttribute("clase", claseOpt.get());
        return "clase-detalle";
    }

    @DeleteMapping("/{claseId}/eliminar")
    public ResponseEntity<Map<String, Object>> eliminarClase(@PathVariable UUID claseId, Authentication authentication) {
        if (!puedeModificar(authentication)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", "No autorizado"));
        }

        try {
            claseService.eliminarClase(claseId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private boolean puedeModificar(Authentication authentication) {
        if (authentication == null)
            return false;

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") ||
                        auth.getAuthority().equals("DOCENTE"));
    }

    @PostMapping("/finalizar-con-resumen/{claseId}")
    public ResponseEntity<?> finalizarConResumen(@PathVariable UUID claseId, @org.springframework.web.bind.annotation.RequestBody String transcripcion, Principal principal) {
        try {
            Clase clase = claseService.findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));

            // 1. Generar resumen con IA
            String resumenHtml = chatServiceSimple.generarResumenClase(transcripcion);

            // 2. Crear Material
            com.example.demo.model.Material material = new com.example.demo.model.Material();
            material.setTitulo("Resumen IA: " + clase.getTitulo());
            material.setDescripcion("Resumen generado automáticamente por IA para la clase del " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            material.setFechaCreacion(LocalDateTime.now());
            material.setVisibilidad(true);
            material.setModulo(clase.getModulo());
            
            // Asignar docente
            com.example.demo.model.Docente docente = docenteRepository.findByDni(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            material.setDocente(docente);
            
            material = materialRepository.save(material);

            // 3. Crear Archivo HTML
            com.example.demo.model.Archivo archivo = new com.example.demo.model.Archivo();
            archivo.setNombre("resumen-" + claseId + ".html");
            archivo.setTipoMime("text/html");
            archivo.setContenido(resumenHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            archivo.setTamano((long) archivo.getContenido().length);
            archivo.setFechaSubida(LocalDateTime.now());
            archivo.setMaterial(material);
            
            archivoRepository.save(archivo);

            return ResponseEntity.ok().body(Map.of("message", "Resumen generado y guardado exitosamente", "materialId", material.getIdActividad()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

}