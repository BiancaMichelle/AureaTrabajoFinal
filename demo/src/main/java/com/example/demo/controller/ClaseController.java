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
            @RequestParam(required = false, defaultValue = "false") Boolean preguntasAleatorias,
            @RequestParam(required = false) Integer cantidadPreguntas,
            @RequestParam(required = false, defaultValue = "false") Boolean permisoMicrofono,
            @RequestParam(required = false, defaultValue = "false") Boolean permisoCamara,
            @RequestParam(required = false, defaultValue = "false") Boolean permisoCompartirPantalla,
            @RequestParam(required = false, defaultValue = "true") Boolean permisoChat,
            @RequestParam(required = false, defaultValue = "false") Boolean transcripcionHabilitada,
            @RequestParam(required = false, defaultValue = "false") Boolean generarResumenAutomatico,
            @RequestParam(required = false, defaultValue = "false") Boolean publicarResumenAutomaticamente,
            Principal principal) {
        try {
            // Validar que existe el módulo (Precondición CU-26)
            Modulo modulo = moduloRepository.findById(moduloId)
                    .orElseThrow(() -> new IllegalArgumentException("Módulo no encontrado"));
            
            // Validar estado del curso (Precondición CU-26)
            com.example.demo.enums.EstadoOferta estado = modulo.getCurso().getEstado();
            if (estado != com.example.demo.enums.EstadoOferta.ACTIVA && 
                estado != com.example.demo.enums.EstadoOferta.ENCURSO) {
                throw new IllegalStateException("El curso debe estar en estado ACTIVA o ENCURSO para crear clases");
            }
            
            // Validar campos obligatorios (RF-06)
            if (titulo == null || titulo.trim().isEmpty()) {
                throw new IllegalArgumentException("El título de la clase es obligatorio");
            }
            
            if (inicio == null) {
                throw new IllegalArgumentException("La fecha y hora de inicio son obligatorias");
            }
            
            // Validar preguntas aleatorias (Paso 5 CU-26)
            if (Boolean.TRUE.equals(preguntasAleatorias)) {
                if (cantidadPreguntas == null || cantidadPreguntas < 1) {
                    throw new IllegalArgumentException("Debe especificar la cantidad de preguntas (mínimo 1)");
                }
            }

            System.out.println("🎯 Creando clase con configuración completa:");
            System.out.println("   - Título: " + titulo);
            System.out.println("   - Módulo ID: " + moduloId);
            System.out.println("   - Permisos: Mic=" + permisoMicrofono + ", Cam=" + permisoCamara + 
                             ", Pantalla=" + permisoCompartirPantalla + ", Chat=" + permisoChat);
            System.out.println("   - Asistencia: Auto=" + asistenciaAutomatica + ", Preguntas=" + preguntasAleatorias +
                             (Boolean.TRUE.equals(preguntasAleatorias) ? " (Cantidad: " + cantidadPreguntas + ")" : ""));

            Clase clase = new Clase();
            clase.setTitulo(titulo.trim());
            clase.setDescripcion(descripcion != null ? descripcion.trim() : "");
            clase.setInicio(inicio);
            clase.setFin(inicio.plusHours(duracion));
            clase.setAsistenciaAutomatica(asistenciaAutomatica);
            clase.setPreguntasAleatorias(preguntasAleatorias);
            clase.setCantidadPreguntas(Boolean.TRUE.equals(preguntasAleatorias) ? cantidadPreguntas : null);
            clase.setPermisoMicrofono(permisoMicrofono);
            clase.setPermisoCamara(permisoCamara);
            clase.setPermisoCompartirPantalla(permisoCompartirPantalla);
            clase.setPermisoChat(permisoChat);
            clase.setTranscripcionHabilitada(transcripcionHabilitada);
            clase.setGenerarResumenAutomatico(generarResumenAutomatico);
            clase.setPublicarResumenAutomaticamente(publicarResumenAutomaticamente);

            Clase claseCreada = claseService.crearClase(clase, moduloId, principal.getName());

            System.out.println("✅ Clase creada exitosamente con ID: " + claseCreada.getIdClase());
            
            // Notificar a los alumnos del curso (Postcondición CU-26)
            notificarNuevaClase(claseCreada);

            return "redirect:/docente/aula/" + modulo.getCurso().getIdOferta();

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("❌ Error de validación: " + e.getMessage());
            return "redirect:/docente/mis-ofertas?error=" + e.getMessage();
        } catch (Exception e) {
            System.out.println("❌ Error al crear clase: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/docente/mis-ofertas?error=Error+interno+del+servidor";
        }
    }

    private void notificarNuevaClase(Clase clase) {
        // TODO: Integrar con sistema de notificaciones
        System.out.println("📧 Stub Notificación: Nueva clase '" + clase.getTitulo() + 
                          "' creada para el " + clase.getInicio());
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
            // Paso 1: Verificar que la clase existe (Precondición CU-27)
            Clase clase = claseService.findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));

            // Paso 2: Verificar si está habilitada la generación de resumen (CU-27)
            if (!Boolean.TRUE.equals(clase.getGenerarResumenAutomatico())) {
                // Secuencia Alternativa Paso 2: Preguntar al docente
                return ResponseEntity.ok().body(Map.of(
                    "preguntarDocente", true,
                    "message", "¿Desea generar un resumen de esta clase?"
                ));
            }
            
            // Paso 3: Verificar que hay transcripción disponible
            if (transcripcion == null || transcripcion.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "error", "No hay transcripción disponible para generar el resumen"
                ));
            }

            System.out.println("🎯 Generando resumen de clase: " + clase.getTitulo());

            // Paso 4: Generar resumen con IA
            String resumenHtml = chatServiceSimple.generarResumenClase(transcripcion);

            // Paso 5: Verificar si debe publicarse automáticamente
            boolean publicarAutomaticamente = Boolean.TRUE.equals(clase.getPublicarResumenAutomaticamente());
            
            // Paso 6: Crear Material (siempre se crea, pero la visibilidad depende de la configuración)
            com.example.demo.model.Material material = new com.example.demo.model.Material();
            material.setTitulo("Resumen IA: " + clase.getTitulo());
            material.setDescripcion("Resumen generado automáticamente por IA para la clase del " + 
                                   LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            material.setFechaCreacion(LocalDateTime.now());
            material.setVisibilidad(publicarAutomaticamente); // Secuencia Alternativa Paso 5
            material.setModulo(clase.getModulo());
            
            // Asignar docente
            com.example.demo.model.Docente docente = docenteRepository.findByDni(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            material.setDocente(docente);
            
            material = materialRepository.save(material);

            // Crear Archivo HTML con el resumen
            com.example.demo.model.Archivo archivo = new com.example.demo.model.Archivo();
            archivo.setNombre("resumen-" + claseId + ".html");
            archivo.setTipoMime("text/html");
            archivo.setContenido(resumenHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            archivo.setTamano((long) archivo.getContenido().length);
            archivo.setFechaSubida(LocalDateTime.now());
            archivo.setMaterial(material);
            
            archivoRepository.save(archivo);

            System.out.println("✅ Resumen generado y guardado");
            System.out.println("   - Material ID: " + material.getIdActividad());
            System.out.println("   - Visible para alumnos: " + publicarAutomaticamente);

            // Paso 7: Notificar (Postcondición CU-27)
            notificarResumenGenerado(clase, material, publicarAutomaticamente);

            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", publicarAutomaticamente 
                    ? "Resumen generado y publicado en el módulo exitosamente" 
                    : "Resumen generado y guardado (solo visible para el docente)",
                "materialId", material.getIdActividad(),
                "publicado", publicarAutomaticamente
            ));

        } catch (Exception e) {
            System.err.println("❌ Error al generar resumen: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error al generar resumen: " + e.getMessage()));
        }
    }
    
    private void notificarResumenGenerado(Clase clase, com.example.demo.model.Material material, boolean publicado) {
        // TODO: Integrar con sistema de notificaciones
        System.out.println("📧 Stub Notificación: Resumen generado para clase '" + clase.getTitulo() + "'");
        System.out.println("   - Docente: " + clase.getDocente().getNombre() + " " + clase.getDocente().getApellido());
        if (publicado) {
            System.out.println("   - Alumnos del curso: Resumen disponible en el módulo");
        } else {
            System.out.println("   - Solo visible para el docente");
        }
    }

}