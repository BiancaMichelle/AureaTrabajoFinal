package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

// OpenPDF imports
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Clase;
import com.example.demo.model.Cuota;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Modulo;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CuotaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ClaseService;
import com.example.demo.service.InstitutoService;
import com.example.demo.service.JitsiClaseService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

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
    private final com.example.demo.repository.InscripcionRepository inscripcionRepository;
    private final com.example.demo.repository.TareaRepository tareaRepository;
    private final CuotaRepository cuotaRepository;
    private final InstitutoService institutoService;
    private final PasswordEncoder passwordEncoder;
    private final com.example.demo.service.AsistenciaEnVivoService asistenciaEnVivoService;

    public ClaseController(ClaseService claseService,
            JitsiClaseService jitsiClaseService,
            ModuloRepository moduloRepository,
            CursoRepository cursoRepository,
            UsuarioRepository usuarioRepository,
            com.example.demo.ia.service.ChatServiceSimple chatServiceSimple,
            com.example.demo.repository.MaterialRepository materialRepository,
            com.example.demo.repository.ArchivoRepository archivoRepository,
            com.example.demo.repository.DocenteRepository docenteRepository,
            com.example.demo.repository.InscripcionRepository inscripcionRepository,
            com.example.demo.repository.TareaRepository tareaRepository,
            CuotaRepository cuotaRepository,
            InstitutoService institutoService,
            PasswordEncoder passwordEncoder,
            com.example.demo.service.AsistenciaEnVivoService asistenciaEnVivoService) {
        this.claseService = claseService;
        this.jitsiClaseService = jitsiClaseService;
        this.moduloRepository = moduloRepository;
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.chatServiceSimple = chatServiceSimple;
        this.materialRepository = materialRepository;
        this.archivoRepository = archivoRepository;
        this.docenteRepository = docenteRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.tareaRepository = tareaRepository;
        this.cuotaRepository = cuotaRepository;
        this.institutoService = institutoService;
        this.passwordEncoder = passwordEncoder;
        this.asistenciaEnVivoService = asistenciaEnVivoService;
    }

    @PostMapping("/crear")
    @ResponseBody
    public ResponseEntity<?> crearClase(@RequestParam String titulo,
            @RequestParam String descripcion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam UUID moduloId,
            @RequestParam Integer duracion,
            @RequestParam(required = false, defaultValue = "false") Boolean asistenciaAutomatica,
            @RequestParam(required = false, defaultValue = "false") Boolean preguntasAleatorias,
            @RequestParam(required = false) Integer cantidadPreguntas,
            @RequestParam(required = false) Integer tiempoPreguntas,
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
                if (tiempoPreguntas == null || tiempoPreguntas < 1) {
                    throw new IllegalArgumentException("Debe especificar el intervalo de tiempo (mínimo 1 minuto)");
                }
                // Tipo de asistencia por preguntas (exclusivo)
                asistenciaAutomatica = false;
            } else if (Boolean.TRUE.equals(asistenciaAutomatica)) {
                // Tipo de asistencia por tiempo (exclusivo)
                preguntasAleatorias = false;
                cantidadPreguntas = null;
                tiempoPreguntas = null;
            }
/* 
            System.out.println("Creando clase con configuración completa:");
            System.out.println("   - Título: " + titulo);
            System.out.println("   - Módulo ID: " + moduloId);
            System.out.println("   - Permisos: Mic=" + permisoMicrofono + ", Cam=" + permisoCamara + 
                             ", Pantalla=" + permisoCompartirPantalla + ", Chat=" + permisoChat);
            System.out.println("   - Asistencia: Auto=" + asistenciaAutomatica + ", Preguntas=" + preguntasAleatorias +
                             (Boolean.TRUE.equals(preguntasAleatorias) ? " (Cantidad: " + cantidadPreguntas + ")" : ""));
*/
            Clase clase = new Clase();
            clase.setTitulo(titulo.trim());
            clase.setDescripcion(descripcion != null ? descripcion.trim() : "");
            clase.setInicio(inicio);
            clase.setFin(calcularFinClase(inicio, duracion));
            clase.setAsistenciaAutomatica(asistenciaAutomatica);
            clase.setPreguntasAleatorias(preguntasAleatorias);
            clase.setCantidadPreguntas(Boolean.TRUE.equals(preguntasAleatorias) ? cantidadPreguntas : null);
            clase.setTiempoPreguntas(Boolean.TRUE.equals(preguntasAleatorias) ? tiempoPreguntas : null);
            clase.setPermisoMicrofono(permisoMicrofono);
            clase.setPermisoCamara(permisoCamara);
            clase.setPermisoCompartirPantalla(permisoCompartirPantalla);
            clase.setPermisoChat(permisoChat);
            clase.setTranscripcionHabilitada(transcripcionHabilitada);
            clase.setGenerarResumenAutomatico(generarResumenAutomatico);
            clase.setPublicarResumenAutomaticamente(publicarResumenAutomaticamente);

            Clase claseCreada = claseService.crearClase(clase, moduloId, principal.getName());

            System.out.println("Clase creada exitosamente con ID: " + claseCreada.getIdClase());
            
            // Notificar a los alumnos del curso (Postcondición CU-26)
            notificarNuevaClase(claseCreada);

            return ResponseEntity.ok(Map.of("success", true, "message", "Clase creada exitosamente", "id", claseCreada.getIdClase()));

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            System.out.println("Error al crear clase: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error interno desconocido";
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", errorMsg));
        }
    }

    @PostMapping("/{claseId}/modificar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> modificarClase(@PathVariable UUID claseId,
            @RequestParam String titulo,
            @RequestParam String descripcion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam Integer duracion,
            @RequestParam(required = false, defaultValue = "false") Boolean asistenciaAutomatica,
            @RequestParam(required = false, defaultValue = "false") Boolean preguntasAleatorias,
            @RequestParam(required = false) Integer cantidadPreguntas,
            @RequestParam(required = false) Integer tiempoPreguntas,
            @RequestParam(required = false, defaultValue = "false") Boolean permisoMicrofono,
            @RequestParam(required = false, defaultValue = "false") Boolean permisoCamara,
            @RequestParam(required = false, defaultValue = "false") Boolean permisoCompartirPantalla,
            @RequestParam(required = false, defaultValue = "true") Boolean permisoChat,
            @RequestParam(required = false, defaultValue = "false") Boolean transcripcionHabilitada,
            @RequestParam(required = false, defaultValue = "false") Boolean generarResumenAutomatico,
            @RequestParam(required = false, defaultValue = "false") Boolean publicarResumenAutomaticamente,
            Principal principal) {
        
        try {
            Clase claseDetalles = new Clase();
            claseDetalles.setTitulo(titulo.trim());
            claseDetalles.setDescripcion(descripcion.trim());
            claseDetalles.setInicio(inicio);
            claseDetalles.setFin(calcularFinClase(inicio, duracion));
            
            if (Boolean.TRUE.equals(preguntasAleatorias)) {
                if (cantidadPreguntas == null || cantidadPreguntas < 1) {
                    throw new IllegalArgumentException("Debe especificar la cantidad de preguntas (mínimo 1)");
                }
                if (tiempoPreguntas == null || tiempoPreguntas < 1) {
                    throw new IllegalArgumentException("Debe especificar el intervalo de tiempo (mínimo 1 minuto)");
                }
                // Tipo de asistencia por preguntas (exclusivo)
                asistenciaAutomatica = false;
            } else if (Boolean.TRUE.equals(asistenciaAutomatica)) {
                // Tipo de asistencia por tiempo (exclusivo)
                preguntasAleatorias = false;
                cantidadPreguntas = null;
                tiempoPreguntas = null;
            }

            claseDetalles.setAsistenciaAutomatica(asistenciaAutomatica);
            claseDetalles.setPreguntasAleatorias(preguntasAleatorias);
            claseDetalles.setCantidadPreguntas(Boolean.TRUE.equals(preguntasAleatorias) ? cantidadPreguntas : null);
            claseDetalles.setTiempoPreguntas(Boolean.TRUE.equals(preguntasAleatorias) ? tiempoPreguntas : null);
            
            claseDetalles.setPermisoMicrofono(permisoMicrofono);
            claseDetalles.setPermisoCamara(permisoCamara);
            claseDetalles.setPermisoCompartirPantalla(permisoCompartirPantalla);
            claseDetalles.setPermisoChat(permisoChat);
            claseDetalles.setTranscripcionHabilitada(transcripcionHabilitada);
            claseDetalles.setGenerarResumenAutomatico(generarResumenAutomatico);
            claseDetalles.setPublicarResumenAutomaticamente(publicarResumenAutomaticamente);

            claseService.actualizarClase(claseId, claseDetalles);
            return ResponseEntity.ok(Map.of("success", true, "message", "Clase actualizada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error al actualizar la clase: " + e.getMessage()));
        }
    }

    @GetMapping("/{claseId}/datos")
    @ResponseBody
    public ResponseEntity<?> obtenerDatosClase(@PathVariable UUID claseId) {
        return claseService.findById(claseId)
                .map(clase -> {
                    long duracionMinutos = java.time.Duration.between(clase.getInicio(), clase.getFin()).toMinutes();
                    long duracionHoras = java.time.Duration.between(clase.getInicio(), clase.getFin()).toHours();
                    long duracionValor;
                    if (duracionMinutos > 0 && duracionMinutos <= 3) {
                        duracionValor = 0; // opción de prueba (3 minutos)
                    } else {
                        duracionValor = duracionHoras == 0 ? 1 : duracionHoras;
                    }

                    return ResponseEntity.ok(Map.ofEntries(
                        Map.entry("id", clase.getIdClase()),
                        Map.entry("titulo", clase.getTitulo()),
                        Map.entry("descripcion", clase.getDescripcion()),
                        Map.entry("inicio", clase.getInicio().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                        Map.entry("duracion", duracionValor),
                        Map.entry("asistenciaAutomatica", clase.getAsistenciaAutomatica() != null && clase.getAsistenciaAutomatica()),
                        Map.entry("preguntasAleatorias", clase.getPreguntasAleatorias() != null && clase.getPreguntasAleatorias()),
                        Map.entry("cantidadPreguntas", clase.getCantidadPreguntas() != null ? clase.getCantidadPreguntas() : 3),
                        Map.entry("tiempoPreguntas", clase.getTiempoPreguntas() != null ? clase.getTiempoPreguntas() : 5),
                        Map.entry("permisoMicrofono", clase.getPermisoMicrofono() != null ? clase.getPermisoMicrofono() : true),
                        Map.entry("permisoCamara", clase.getPermisoCamara() != null ? clase.getPermisoCamara() : true),
                        Map.entry("permisoCompartirPantalla", clase.getPermisoCompartirPantalla() != null ? clase.getPermisoCompartirPantalla() : true),
                        Map.entry("permisoChat", clase.getPermisoChat() != null ? clase.getPermisoChat() : true),
                        Map.entry("transcripcionHabilitada", clase.getTranscripcionHabilitada() != null && clase.getTranscripcionHabilitada()),
                        Map.entry("generarResumenAutomatico", clase.getGenerarResumenAutomatico() != null && clase.getGenerarResumenAutomatico()),
                        Map.entry("publicarResumenAutomaticamente", clase.getPublicarResumenAutomaticamente() != null && clase.getPublicarResumenAutomaticamente())
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void notificarNuevaClase(Clase clase) {
        // Notificación pendiente de implementación
        System.out.println("Stub Notificación: Nueva clase '" + clase.getTitulo() + 
                          "' creada para el " + clase.getInicio());
    }

    @GetMapping("/unirse/{claseId}")
    public String unirseAClase(@PathVariable UUID claseId, Principal principal, Model model) {
        try {
            Clase clase = claseService.findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));

            // Validar existencia de usuario
            Usuario usuario = usuarioRepository.findByDni(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean esDocente = usuario.getRoles().stream()
                    .anyMatch(r -> r.getNombre().equals("DOCENTE") || r.getNombre().equals("ADMIN"));

            // 1. Verificación de inscripción (Precondición)
            if (!esDocente) {
                Long ofertaId = clase.getModulo().getCurso().getIdOferta();
                List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDniAndOfertaId(principal.getName(), ofertaId);
                
                Inscripciones inscripcionActiva = inscripciones.stream()
                        .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                        .findFirst()
                        .orElse(null);
                
                if (inscripcionActiva == null) {
                    model.addAttribute("error", "No estás inscrito en este curso o tu inscripción no está activa.");
                    return "redirect:/alumno/mis-ofertas?error=no-inscrito";
                }
                
                // Validar Mora (Nuevo)
                Integer diasMoraPermitidos = institutoService.obtenerInstituto().getDiasMoraBloqueoAula();
                // Importante: usar import com.example.demo.enums.EstadoCuota; si no está, usar FQCN o importarlo
                List<Cuota> cuotas = cuotaRepository.findByInscripcionIdInscripcion(inscripcionActiva.getIdInscripcion());
                List<Cuota> cuotasVencidas = cuotas.stream()
                    .filter(c -> com.example.demo.enums.EstadoCuota.PENDIENTE.equals(c.getEstado()) && 
                                 c.getFechaVencimiento().isBefore(java.time.LocalDate.now()))
                    .toList();
                
                if (!cuotasVencidas.isEmpty()) {
                    long maxDiasMora = cuotasVencidas.stream()
                        .mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getFechaVencimiento(), LocalDate.now()))
                        .max().orElse(0);
                        
                    if (maxDiasMora > diasMoraPermitidos) {
                        model.addAttribute("diasMora", maxDiasMora);
                        model.addAttribute("limiteMora", diasMoraPermitidos);
                        model.addAttribute("ofertaId", ofertaId);
                        return "aula-bloqueada"; 
                    }
                }
            }

            // 2. Verificación de Horario
            LocalDateTime ahora = LocalDateTime.now();
            
            // A) Verificar si ya terminó (CRITICO: Esto debe ser antes de cualquier credencial)
            if (!ahora.isBefore(clase.getFin())) {
                model.addAttribute("clase", clase);
                model.addAttribute("error", "La clase finalizó el " + 
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(clase.getFin()));
                return "clase-espera";
            }

            // B) Verificar si es muy temprano (Solo alumnos)
            // Permitimos entrar a docentes 15 min antes, alumnos solo si ya inició (o 5 min antes)
            if (!esDocente && ahora.isBefore(clase.getInicio().minusMinutes(5))) {
                // Redirigir a sala de espera
                model.addAttribute("clase", clase);
                model.addAttribute("mensajeEspera", "La clase comenzará el: " + 
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(clase.getInicio()));
                return "clase-espera";
            }

            System.out.println("Uniéndose a clase Jitsi ID: " + claseId);

            String meetingUrl = claseService.unirseAClase(claseId, principal.getName());
            
            model.addAttribute("meetingUrl", meetingUrl);
            model.addAttribute("clase", clase);
            model.addAttribute("codigoMeet", clase.getRoomName());
            model.addAttribute("proveedor", "Jitsi Meet");
            model.addAttribute("reunionActiva", true);
            model.addAttribute("usuario", usuario);
            model.addAttribute("esDocente", esDocente);
            long duracionMinutos = clase.getInicio() != null && clase.getFin() != null
                ? java.time.Duration.between(clase.getInicio(), clase.getFin()).toMinutes()
                : 0;
            model.addAttribute("duracionMinutos", duracionMinutos);

            return "clase-VideoConferencia";

        } catch (Exception e) {
            System.out.println("Error en unirseAClase Jitsi: " + e.getMessage());
            
            // INTENTAR RECUPERAR CLASE PARA EL MODELO SI FALLÓ ANTES
            try {
                 if (!model.containsAttribute("clase")) {
                     claseService.findById(claseId).ifPresent(c -> model.addAttribute("clase", c));
                 }
            } catch (Exception ex) { /* Ignorar si falla recuperación */ }

            model.addAttribute("error", "No se pudo cargar la sala: " + e.getMessage());
            return "clase-espera"; 
        }
    }

    @GetMapping("/validar-mora-aula/{claseId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validarMoraAula(@PathVariable UUID claseId, Principal principal) {
        try {
            System.out.println("Validando mora para videoconferencia - Clase ID: " + claseId);
            System.out.println("   - Usuario: " + principal.getName());
            
            // Obtener clase y validar
            Clase clase = claseService.findById(claseId).orElse(null);
            if (clase == null) {
                System.out.println("Clase no encontrada");
                return ResponseEntity.badRequest().body(Map.of("error", "Clase no encontrada"));
            }
            
            // Verificar si es docente o admin (no se les bloquea)
            Usuario usuario = usuarioRepository.findByDni(principal.getName()).orElse(null);
            if (usuario != null) {
                boolean esDocente = usuario.getRoles().stream()
                        .anyMatch(r -> r.getNombre().equals("DOCENTE") || r.getNombre().equals("ADMIN"));
                if (esDocente) {
                    System.out.println("Usuario es docente/admin - Acceso permitido");
                    return ResponseEntity.ok(Map.of("bloqueado", false));
                }
            }
            
            Long ofertaId = clase.getModulo().getCurso().getIdOferta();
            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDniAndOfertaId(principal.getName(), ofertaId);
            
            Inscripciones inscripcionActiva = inscripciones.stream()
                    .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                    .findFirst()
                    .orElse(null);
            
            if (inscripcionActiva == null) {
                System.out.println("No hay inscripción activa");
                return ResponseEntity.status(403).body(Map.of(
                    "bloqueado", true,
                    "error", "No estás inscrito"
                ));
            }
            
            // Validar mora para aulas/videoconferencias (usa el mismo límite que actividades)
            Integer diasMoraPermitidos = institutoService.obtenerInstituto().getDiasMoraBloqueoActividad();
            if (diasMoraPermitidos == null) diasMoraPermitidos = 0;
            
            System.out.println("   - Días mora permitidos (actividad): " + diasMoraPermitidos);
            
            List<Cuota> cuotas = cuotaRepository.findByInscripcionIdInscripcion(inscripcionActiva.getIdInscripcion());
            System.out.println("   - Cuotas encontradas: " + cuotas.size());
            
            List<Cuota> cuotasVencidas = cuotas.stream()
                .filter(c -> (com.example.demo.enums.EstadoCuota.PENDIENTE.equals(c.getEstado()) || 
                             com.example.demo.enums.EstadoCuota.VENCIDA.equals(c.getEstado())) &&
                        c.getFechaVencimiento() != null &&
                        c.getFechaVencimiento().isBefore(LocalDate.now()))
                .toList();
            
            System.out.println("   - Cuotas vencidas: " + cuotasVencidas.size());
            
            if (!cuotasVencidas.isEmpty()) {
                long maxDiasMora = cuotasVencidas.stream()
                    .mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getFechaVencimiento(), LocalDate.now()))
                    .max().orElse(0);
                
                System.out.println("   - Días de mora máximos: " + maxDiasMora);
                System.out.println("   - ¿Bloquear? " + (maxDiasMora > diasMoraPermitidos));
                
                if (maxDiasMora > diasMoraPermitidos) {
                    System.out.println("BLOQUEADO - Mora excede el límite");
                    return ResponseEntity.status(403).body(Map.of(
                        "bloqueado", true,
                        "diasMora", maxDiasMora,
                        "limiteMora", diasMoraPermitidos,
                        "ofertaId", ofertaId,
                        "mensaje", "Tienes pagos pendientes que superan el límite permitido"
                    ));
                }
            }
            
            System.out.println("Validación de mora pasada - Acceso permitido");
            return ResponseEntity.ok(Map.of("bloqueado", false));
            
        } catch (Exception e) {
            System.out.println("Error en validación de mora: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error al validar mora: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/validar-mora/{tareaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validarMoraTarea(@PathVariable Long tareaId, Principal principal) {
        try {
            // Obtener tarea y validar inscripción
            com.example.demo.model.Tarea tarea = tareaRepository.findById(tareaId).orElse(null);
            if (tarea == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tarea no encontrada"));
            }
            
            Long ofertaId = tarea.getModulo().getCurso().getIdOferta();
            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDniAndOfertaId(principal.getName(), ofertaId);
            
            Inscripciones inscripcionActiva = inscripciones.stream()
                    .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                    .findFirst()
                    .orElse(null);
            
            if (inscripcionActiva == null) {
                return ResponseEntity.status(403).body(Map.of(
                    "bloqueado", true,
                    "error", "No estás inscrito"
                ));
            }
            
            // Validar mora para actividades
            Integer diasMoraPermitidos = institutoService.obtenerInstituto().getDiasMoraBloqueoActividad();
            if (diasMoraPermitidos == null) diasMoraPermitidos = 0;
            
            List<Cuota> cuotas = cuotaRepository.findByInscripcionIdInscripcion(inscripcionActiva.getIdInscripcion());
            List<Cuota> cuotasVencidas = cuotas.stream()
                .filter(c -> (com.example.demo.enums.EstadoCuota.PENDIENTE.equals(c.getEstado()) || 
                             com.example.demo.enums.EstadoCuota.VENCIDA.equals(c.getEstado())) &&
                        c.getFechaVencimiento() != null &&
                        c.getFechaVencimiento().isBefore(LocalDate.now()))
                .toList();
            
            if (!cuotasVencidas.isEmpty()) {
                long maxDiasMora = cuotasVencidas.stream()
                    .mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getFechaVencimiento(), LocalDate.now()))
                    .max().orElse(0);
                
                if (maxDiasMora > diasMoraPermitidos) {
                    return ResponseEntity.status(403).body(Map.of(
                        "bloqueado", true,
                        "diasMora", maxDiasMora,
                        "limiteMora", diasMoraPermitidos,
                        "mensaje", "Tienes pagos pendientes que superan el límite permitido"
                    ));
                }
            }
            
            return ResponseEntity.ok(Map.of("bloqueado", false));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error al validar mora: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/validar-ingreso")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validarIngreso(@RequestBody Map<String, String> credenciales, Principal principal) {
        String dni = credenciales.get("dni");
        String password = credenciales.get("password");
        
        // Validar que el DNI coincida con el usuario logueado (seguridad básica)
        if (!principal.getName().equals(dni)) {
             return ResponseEntity.status(403).body(Map.of("valid", false, "message", "El DNI no corresponde al usuario actual."));
        }

        Usuario usuario = usuarioRepository.findByDni(dni).orElse(null);
        if (usuario != null && passwordEncoder.matches(password, usuario.getContraseña())) {
            return ResponseEntity.ok(Map.of("valid", true));
        } else {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Credenciales inválidas."));
        }
    }

    @GetMapping("/estado/{claseId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> estadoClase(@PathVariable UUID claseId) {
        return claseService.findById(claseId)
            .map(clase -> {
                LocalDateTime ahora = LocalDateTime.now();
                LocalDateTime inicio = clase.getInicio();
                LocalDateTime inicioMenos5 = inicio.minusMinutes(5);
                
                boolean iniciada = ahora.isAfter(inicioMenos5);
                boolean finalizada = clase.getFin() != null && ahora.isAfter(clase.getFin());

                // Calculamos datos para el frontend
                boolean esHoy = inicio.toLocalDate().isEqual(ahora.toLocalDate());
                long minutosRestantes = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME != null 
                    ? java.time.temporal.ChronoUnit.MINUTES.between(ahora, inicio) 
                    : 0;

                return ResponseEntity.ok(Map.<String, Object>of(
                    "iniciada", iniciada,
                    "finalizada", finalizada,
                    "esHoy", esHoy,
                    "inicio", inicio, // Serializado por defecto
                    "minutosRestantes", minutosRestantes,
                    "mensaje", !esHoy ? "La clase no es hoy" : (iniciada ? "Clase iniciada" : "Aún no es hora") 
                ));
            })
            .orElse(ResponseEntity.notFound().build());
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

    private LocalDateTime calcularFinClase(LocalDateTime inicio, Integer duracionHoras) {
        if (inicio == null) {
            return null;
        }
        if (duracionHoras != null && duracionHoras == 0) {
            // Opción de prueba: 3 minutos
            return inicio.plusMinutes(3);
        }
        if (duracionHoras == null) {
            return inicio;
        }
        return inicio.plusHours(duracionHoras);
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

            System.out.println("Generando resumen de clase: " + clase.getTitulo());

            // Paso 4: Generar resumen con IA (ahora devuelve texto plano estructurado)
            String resumenTexto = chatServiceSimple.generarResumenClase(transcripcion);

            // Paso 5: Verificar si debe publicarse automáticamente
            boolean publicarAutomaticamente = Boolean.TRUE.equals(clase.getPublicarResumenAutomaticamente());
            
            // Paso 6: Crear Material (siempre se crea, pero la visibilidad depende de la configuración)
            com.example.demo.model.Material material = new com.example.demo.model.Material();
            // Formatear fecha y hora para el título
            String fechaHoraClase = clase.getInicio() != null 
                ? clase.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            
            material.setTitulo("Resumen de la clase " + fechaHoraClase);
            material.setDescripcion("Resumen generado automáticamente por IA para la clase del " + 
                                   (clase.getInicio() != null ? clase.getInicio().toLocalDate() : LocalDate.now()));
            material.setFechaCreacion(LocalDateTime.now());
            material.setVisibilidad(publicarAutomaticamente); // Secuencia Alternativa Paso 5
            material.setModulo(clase.getModulo());
            
            // Asignar docente
            com.example.demo.model.Docente docente = docenteRepository.findByDni(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            material.setDocente(docente);
            
            material = materialRepository.save(material);

            // Generar PDF usando OpenPDF
            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            Document document = new Document();
            try {
                PdfWriter.getInstance(document, pdfOut);
                document.open();

                // Título
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
                Paragraph title = new Paragraph("Resumen de la clase " + fechaHoraClase, titleFont);
                title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                // Contenido
                Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
                String[] paragraphs = resumenTexto.split("\\n");
                for (String para : paragraphs) {
                    if (!para.trim().isEmpty()) {
                        Paragraph p = new Paragraph(para.trim(), bodyFont);
                        p.setSpacingAfter(10);
                        document.add(p);
                    }
                }
            } catch (Exception e) {
                // Fallback si falla PDF: guardar texto plano
                pdfOut.reset();
                pdfOut.write(resumenTexto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } finally {
                if(document.isOpen()) document.close();
            }

            // Crear Archivo PDF con el resumen
            com.example.demo.model.Archivo archivo = new com.example.demo.model.Archivo();
            archivo.setNombre("resumen-" + claseId + ".pdf");
            archivo.setTipoMime("application/pdf");
            archivo.setContenido(pdfOut.toByteArray());
            archivo.setTamano((long) archivo.getContenido().length);
            archivo.setFechaSubida(LocalDateTime.now());
            archivo.setMaterial(material);
            
            archivoRepository.save(archivo);

            System.out.println("Resumen generado y guardado");
            System.out.println("   - Material ID: " + material.getIdActividad());
            System.out.println("   - Visible para alumnos: " + publicarAutomaticamente);

            // Consolidar asistencia de la clase (calcular presentes/ausentes)
            try {
                asistenciaEnVivoService.consolidarAsistenciaClase(claseId);
                System.out.println("Asistencia consolidada para clase: " + claseId);
            } catch (Exception e) {
                System.err.println("Error al consolidar asistencia: " + e.getMessage());
                // No detenemos el flujo, ya que el resumen es lo principal aquí
            }

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
            System.err.println("Error al generar resumen: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error al generar resumen: " + e.getMessage()));
        }
    }
    
    private void notificarResumenGenerado(Clase clase, com.example.demo.model.Material material, boolean publicado) {
        System.out.println("Stub Notificación: Resumen generado para clase '" + clase.getTitulo() + "'");
        System.out.println("   - Docente: " + clase.getDocente().getNombre() + " " + clase.getDocente().getApellido());
        if (publicado) {
            System.out.println("   - Alumnos del curso: Resumen disponible en el módulo");
        } else {
            System.out.println("   - Solo visible para el docente");
        }
    }

    @PostMapping("/{claseId}/finalizar")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<?> finalizarClase(@PathVariable UUID claseId, Principal principal) {
         try {
             // Obtener el bean manualmente ya que no lo inyectamos en el constructor original
             // Esto es un parche rápido para no modificar todo el constructor y romper otros tests
             org.springframework.context.ApplicationContext context = 
                org.springframework.web.context.support.WebApplicationContextUtils
                    .getRequiredWebApplicationContext(
                        ((jakarta.servlet.http.HttpServletRequest) 
                            ((org.springframework.web.context.request.ServletRequestAttributes) 
                                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                                    .getRequest()).getServletContext()
                    );
             
             com.example.demo.service.AsistenciaEnVivoService asistenciaService = context.getBean(com.example.demo.service.AsistenciaEnVivoService.class);
             
             asistenciaService.consolidarAsistenciaClase(claseId);
             
             System.out.println("Clase finalizada y asistencia calculada para: " + claseId);
             
             return ResponseEntity.ok().body(Map.of("success", true, "message", "Clase finalizada y asistencia calculada"));
         } catch (Exception e) {
             System.err.println("Error finalizando clase: " + e.getMessage());
             e.printStackTrace();
             return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
         }
    }

}









