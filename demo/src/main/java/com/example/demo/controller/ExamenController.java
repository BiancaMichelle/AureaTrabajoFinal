package com.example.demo.controller;

import com.example.demo.service.EmailService;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.enums.EstadoCuota;
import com.example.demo.enums.EstadoIntento;
import com.example.demo.enums.TipoPregunta;
import com.example.demo.model.Alumno;
import com.example.demo.model.Cuota;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Examen;
import com.example.demo.model.Formacion;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Intento;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Opcion;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.model.RespuestaIntento;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CuotaRepository;
import com.example.demo.repository.ExamenRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.IntentoRepository;
import com.example.demo.repository.PoolRepository;
import com.example.demo.repository.PreguntaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.InstitutoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Controller
@RequestMapping("/examen")
public class ExamenController {

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private ExamenRepository examenRepository;
    
    @Autowired
    private AlumnoRepository alumnoRepository;
    
    @Autowired
    private IntentoRepository intentoRepository;
    
    @Autowired
    private PreguntaRepository preguntaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private InstitutoService institutoService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Muestra la vista del examen
     */
    @GetMapping("/realizar/{examenId}")
    public String realizarExamen(@PathVariable Long examenId, Principal principal, Model model, Authentication authentication) {
        try {
            Examen examen = examenRepository.findById(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen no encontrado"));

            boolean esDocenteOAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("DOCENTE") || a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));

            if (!esDocenteOAdmin) {
                // 0. Validar si ya lo rindió y está corregido
                try {
                     com.example.demo.model.Usuario usuario = usuarioRepository.findByDni(principal.getName()).orElse(null);
                     // Fix: Find Alumno ID properly (Assuming Usuario extends or finding Alumno by DNI)
                     // Using AlumnoRepository directly if ID needed, or just casting if Usuario instance of Alumno
                     Alumno alumno = alumnoRepository.findByDni(principal.getName()).orElse(null);
                     if (alumno != null) {
                        List<Intento> intentosPrevios = intentoRepository.findByAlumno_IdAndExamen_IdActividad(alumno.getId(), examenId);
                        Intento intentoFinalizado = intentosPrevios.stream()
                            .filter(i -> i.getEstado() == EstadoIntento.FINALIZADO && i.getCalificacion() != null)
                            .findFirst().orElse(null);
                        
                        if (intentoFinalizado != null) {
                            return "redirect:/examen/revision/" + examenId;
                        }
                     }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // 1. Validar Inscripción Activa
                List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDniAndOfertaId(
                    principal.getName(), examen.getModulo().getCurso().getIdOferta());
                
                Inscripciones inscripcionActiva = inscripciones.stream()
                    .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                    .findFirst()
                    .orElse(null);
                    
                if (inscripcionActiva == null) {
                    return "redirect:/alumno/mis-ofertas?error=No+tienes+una+inscripcion+activa+para+este+curso";
                }
                
                // 2. Validar Mora (Con lógica de baja)
                com.example.demo.model.Instituto instituto = institutoService.obtenerInstituto();
                Integer diasMoraPermitidos = instituto.getDiasMoraBloqueoExamen();
                if (diasMoraPermitidos == null) diasMoraPermitidos = 0;

                // Usamos findByInscripcion y filtramos en memoria porque findCuotasVencidas no filtra por ID
                List<Cuota> cuotas = cuotaRepository.findByInscripcionIdInscripcion(inscripcionActiva.getIdInscripcion());
                List<Cuota> cuotasVencidas = cuotas.stream()
                    .filter(c -> c.getEstado() == EstadoCuota.PENDIENTE && 
                                 c.getFechaVencimiento().isBefore(LocalDate.now()))
                    .collect(Collectors.toList());
                
                if (!cuotasVencidas.isEmpty()) {
                    long maxDiasMora = cuotasVencidas.stream()
                        .mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getFechaVencimiento(), LocalDate.now()))
                        .max().orElse(0);
                        
                    if (maxDiasMora > diasMoraPermitidos) {
                         // Baja de la inscripción (Oferta perdida)
                         inscripcionActiva.setEstadoInscripcion(false);
                         inscripcionRepository.save(inscripcionActiva);

                         try {
                            com.example.demo.model.Usuario alumno = usuarioRepository.findByDni(principal.getName()).orElse(null);
                            if (alumno != null && alumno.getCorreo() != null) {
                                String asunto = "Baja de inscripción - " + instituto.getNombreInstituto();
                                String cuerpo = "Estimado/a " + alumno.getNombre() + ",<br><br>" +
                                                "Lamentamos informarle que su inscripción al curso <b>" + 
                                                examen.getModulo().getCurso().getNombre() + 
                                                "</b> ha sido dada de baja debido a la falta de regularización de sus pagos (Mora superior a " + diasMoraPermitidos + " días).<br>" +
                                                "Por favor, contacte con administración para más detalles.";
                                emailService.sendEmail(alumno.getCorreo(), asunto, cuerpo);
                            }
                         } catch (Exception e) {
                             e.printStackTrace();
                         }

                         return "redirect:/alumno/mis-ofertas?error=Tu+inscripción+ha+sido+cancelada+por+deuda+excesiva";
                    } else {
                        // Deuda existente pero dentro del margen de tolerancia -> Solo Bloqueo
                        return "aula-bloqueada"; 
                    }
                }

                // 3. Validar Fecha y Hora del Examen
                LocalDateTime ahora = LocalDateTime.now();
                if (examen.getFechaApertura() != null && ahora.isBefore(examen.getFechaApertura())) {
                    return "redirect:/alumno/aula/" + examen.getModulo().getCurso().getIdOferta() + "?error=El+examen+aun+no+esta+disponible";
                }
                if (examen.getFechaCierre() != null && ahora.isAfter(examen.getFechaCierre())) {
                    return "redirect:/alumno/aula/" + examen.getModulo().getCurso().getIdOferta() + "?error=El+plazo+para+el+examen+ha+finalizado";
                }
            }

            model.addAttribute("examen", examen);
            model.addAttribute("tiempoTotal",
                    examen.getTiempoRealizacion() != null ? examen.getTiempoRealizacion() : 60);
            model.addAttribute("vistaPrevia", esDocenteOAdmin);

            return "examen";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    /**
     * Vista de revisión de examen corregido
     */
    @GetMapping("/revision/{examenId}")
    public String verRevisionExamen(@PathVariable Long examenId, Principal principal, Model model) {
        try {
            Alumno alumno = alumnoRepository.findByDni(principal.getName())
                  .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
                  
            // Buscar intento
            List<Intento> intentos = intentoRepository.findByAlumno_IdAndExamen_IdActividad(alumno.getId(), examenId);
            
            // Tomar el último finalizado
            Intento intento = intentos.stream()
                .filter(i -> i.getEstado() == EstadoIntento.FINALIZADO && i.getCalificacion() != null)
                .reduce((first, second) -> second)
                .orElse(null);
                
            if (intento == null) {
                // Si no hay intento corregido, intentar realizarlo
                return "redirect:/examen/realizar/" + examenId;
            }
            
            Examen examen = intento.getExamen();
            // Asegurar carga de Respuestas (Lazy)
            intento.getRespuestas().size(); 
            
            model.addAttribute("examen", examen);
            model.addAttribute("intento", intento);
            model.addAttribute("respuestas", intento.getRespuestas());
            
            return "examen-revision";
        } catch (Exception e) {
             e.printStackTrace();
             return "redirect:/alumno/mis-ofertas";
        }
    }

    /**
     * API REST para obtener los datos del examen en formato JSON
     */
    @GetMapping("/api/datos/{examenId}")
    public ResponseEntity<?> obtenerDatosExamen(@PathVariable Long examenId, Principal principal, Authentication authentication) {
        try {
            Examen examen = examenRepository.findById(examenId)
                    .orElseThrow(() -> new RuntimeException("Examen no encontrado"));

            boolean esDocenteOAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("DOCENTE") || a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));

            if (!esDocenteOAdmin) {
                if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

                // 1. Validar Inscripción
                List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDniAndOfertaId(
                    principal.getName(), examen.getModulo().getCurso().getIdOferta());
                
                Inscripciones inscripcionActiva = inscripciones.stream()
                    .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                    .findFirst()
                    .orElse(null);
                    
                if (inscripcionActiva == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No inscrito o inscripción inactiva"));
                }
                
                // 2. Validar Mora
                com.example.demo.model.Instituto instituto = institutoService.obtenerInstituto();
                Integer diasMoraPermitidos = instituto.getDiasMoraBloqueoExamen();
                if (diasMoraPermitidos == null) diasMoraPermitidos = 0;

                // Usamos findByInscripcion y filtramos en memoria
                List<Cuota> cuotas = cuotaRepository.findByInscripcionIdInscripcion(inscripcionActiva.getIdInscripcion());
                List<Cuota> cuotasVencidas = cuotas.stream()
                    .filter(c -> c.getEstado() == EstadoCuota.PENDIENTE && 
                                 c.getFechaVencimiento().isBefore(LocalDate.now()))
                    .collect(Collectors.toList());
                
                if (!cuotasVencidas.isEmpty()) {
                    long maxDiasMora = cuotasVencidas.stream()
                        .mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getFechaVencimiento(), LocalDate.now()))
                        .max().orElse(0);
                        
                    if (maxDiasMora > diasMoraPermitidos) {
                         // Baja de la inscripción (Oferta perdida)
                         inscripcionActiva.setEstadoInscripcion(false);
                         inscripcionRepository.save(inscripcionActiva);

                         try {
                            com.example.demo.model.Usuario alumno = usuarioRepository.findByDni(principal.getName()).orElse(null);
                            if (alumno != null && alumno.getCorreo() != null) {
                                String asunto = "Baja de inscripción - " + instituto.getNombreInstituto();
                                String cuerpo = "Estimado/a " + alumno.getNombre() + ",<br><br>" +
                                                "Lamentamos informarle que su inscripción al curso <b>" + 
                                                examen.getModulo().getCurso().getNombre() + 
                                                "</b> ha sido dada de baja debido a la falta de regularización de sus pagos.<br>";
                                emailService.sendEmail(alumno.getCorreo(), asunto, cuerpo);
                            }
                         } catch (Exception e) {}

                         return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Oferta perdida por deuda excesiva"));
                    } else {
                         return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Acceso bloqueado. Debe regularizar sus pagos."));
                    }
                }

                // 3. Validar Fecha
                LocalDateTime ahora = LocalDateTime.now();
                if (examen.getFechaApertura() != null && ahora.isBefore(examen.getFechaApertura())) {
                     return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "El examen aún no ha comenzado"));
                }
                if (examen.getFechaCierre() != null && ahora.isAfter(examen.getFechaCierre())) {
                     return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "El examen ha finalizado"));
                }
            }

            // Construir el objeto de respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("titulo", examen.getTitulo());
            response.put("descripcion", examen.getDescripcion());
            response.put("tiempoRealizacion",
                    examen.getTiempoRealizacion() != null ? examen.getTiempoRealizacion() : 60);

            // Construir preguntas desde todos los pools (con selección aleatoria y límite)
            List<Map<String, Object>> preguntasDTO = examen.getPoolPreguntas().stream()
                    .flatMap(pool -> {
                        List<Pregunta> preguntasDelPool = new ArrayList<>(pool.getPreguntas());
                        Collections.shuffle(preguntasDelPool); // Mezclar preguntas
                        
                        // Limitar a la cantidad configurada en el pool
                        int cantidad = pool.getCantidadPreguntas() != null ? pool.getCantidadPreguntas() : preguntasDelPool.size();
                        List<Pregunta> preguntasSeleccionadas = preguntasDelPool.stream()
                            .limit(cantidad)
                            .collect(Collectors.toList());
                            
                        return preguntasSeleccionadas.stream().map(pregunta -> {
                            Map<String, Object> preguntaMap = new HashMap<>();
                            preguntaMap.put("id", pregunta.getIdPregunta().toString());
                            preguntaMap.put("tipo", pregunta.getTipoPregunta().name());
                            preguntaMap.put("texto", pregunta.getEnunciado());
                            preguntaMap.put("puntaje", pregunta.getPuntaje());

                            // Si tiene opciones, agregarlas
                            if (pregunta.getOpciones() != null && !pregunta.getOpciones().isEmpty()) {
                                List<Map<String, Object>> opciones = pregunta.getOpciones().stream()
                                        .map(opcion -> {
                                            Map<String, Object> opcionMap = new HashMap<>();
                                            opcionMap.put("id", opcion.getIdOpcion().toString());
                                            opcionMap.put("texto", opcion.getDescripcion());
                                            return opcionMap;
                                        })
                                        .collect(Collectors.toList());
                                preguntaMap.put("opciones", opciones);
                            } else {
                                preguntaMap.put("opciones", List.of());
                            }

                            return preguntaMap;
                        });
                    })
                    .collect(Collectors.toList());
            
            // Mezclar el orden final de las preguntas
            Collections.shuffle(preguntasDTO);

            response.put("preguntas", preguntasDTO);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error al cargar el examen: " + e.getMessage()));
        }
    }
    
    @PostMapping("/entregar")
    public ResponseEntity<?> entregarExamen(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            Long examenId = Long.valueOf(payload.get("examenId").toString());
            List<Map<String, Object>> respuestas = (List<Map<String, Object>>) payload.get("respuestas");
            
            Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen no encontrado"));
                
            Alumno alumno = alumnoRepository.findByDni(principal.getName())
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
                
            Intento intento = new Intento();
            intento.setExamen(examen);
            intento.setAlumno(alumno);
            intento.setFechaInicio(LocalDateTime.now().minusMinutes(60)); // TODO: Mejorar esto con fecha real de inicio
            intento.setFechaFin(LocalDateTime.now());
            intento.setEstado(EstadoIntento.FINALIZADO);
            
            List<RespuestaIntento> respuestasIntento = new ArrayList<>();
            float puntajeTotal = 0;
            boolean requiereRevision = false;
            
            for (Map<String, Object> resp : respuestas) {
                String preguntaId = (String) resp.get("preguntaId");
                Object respuestaUsuarioObj = resp.get("respuesta");
                String respuestaUsuario = "";
                
                if (respuestaUsuarioObj instanceof List) {
                    try {
                        respuestaUsuario = objectMapper.writeValueAsString(respuestaUsuarioObj);
                    } catch (Exception e) {
                        respuestaUsuario = respuestaUsuarioObj.toString();
                    }
                } else if (respuestaUsuarioObj != null) {
                    respuestaUsuario = respuestaUsuarioObj.toString();
                }
                
                Pregunta pregunta = preguntaRepository.findById(UUID.fromString(preguntaId))
                    .orElseThrow(() -> new RuntimeException("Pregunta no encontrada"));
                    
                RespuestaIntento respuestaIntento = new RespuestaIntento();
                respuestaIntento.setIntento(intento);
                respuestaIntento.setPregunta(pregunta);
                respuestaIntento.setRespuestaUsuario(respuestaUsuario);
                
                // Lógica de corrección
                if (Boolean.TRUE.equals(examen.getCalificacionAutomatica())) {
                    if (esTipoManual(pregunta.getTipoPregunta())) {
                        respuestaIntento.setRequiereRevisionManual(true);
                        respuestaIntento.setPuntajeObtenido(0f);
                        requiereRevision = true;
                    } else {
                        boolean esCorrecta = corregirAutomatica(pregunta, respuestaUsuarioObj);
                        respuestaIntento.setEsCorrecta(esCorrecta);
                        respuestaIntento.setPuntajeObtenido(esCorrecta ? pregunta.getPuntaje() : 0f);
                        respuestaIntento.setRequiereRevisionManual(false);
                        puntajeTotal += respuestaIntento.getPuntajeObtenido();
                    }
                } else {
                    respuestaIntento.setRequiereRevisionManual(true);
                    respuestaIntento.setPuntajeObtenido(0f);
                    requiereRevision = true;
                }
                
                respuestasIntento.add(respuestaIntento);
            }
            
            intento.setRespuestas(respuestasIntento);
            intento.setCalificacion(puntajeTotal);
            
            if (requiereRevision) {
                intento.setEstado(EstadoIntento.PENDIENTE_CORRECCION);
            }
            
            intentoRepository.save(intento);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            // Redirigir al aula del alumno asociado a la oferta del módulo
            Long ofertaId = null;
            if (examen.getModulo() != null && examen.getModulo().getCurso() != null) {
                ofertaId = examen.getModulo().getCurso().getIdOferta();
            }
            String redirectPath = ofertaId != null ? "/alumno/aula/" + ofertaId : "/alumno/aula";
            response.put("redirectUrl", redirectPath);
            response.put("calificacion", puntajeTotal);
            response.put("estado", intento.getEstado());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    private boolean esTipoManual(TipoPregunta tipo) {
        return tipo == TipoPregunta.RESPUESTA_CORTA || 
               tipo == TipoPregunta.DESCRIPCION_LARGA || 
               tipo == TipoPregunta.AUTOCOMPLETADO;
    }
    
    private boolean corregirAutomatica(Pregunta pregunta, Object respuestaUsuarioObj) {
        if (respuestaUsuarioObj == null) return false;
        
        TipoPregunta tipo = pregunta.getTipoPregunta();
        List<Opcion> opcionesCorrectas = pregunta.getOpciones().stream()
            .filter(Opcion::getEsCorrecta)
            .collect(Collectors.toList());
            
        if (tipo == TipoPregunta.VERDADERO_FALSO || tipo == TipoPregunta.UNICA_RESPUESTA) {
            String respuesta = respuestaUsuarioObj.toString();
            return opcionesCorrectas.stream()
                .anyMatch(op -> op.getDescripcion().equalsIgnoreCase(respuesta));
        } else if (tipo == TipoPregunta.MULTIPLE_CHOICE) {
            if (respuestaUsuarioObj instanceof List) {
                List<String> respuestas = (List<String>) respuestaUsuarioObj;
                // Verificar que todas las respuestas seleccionadas sean correctas y que no falte ninguna
                // (Criterio estricto)
                List<String> textosCorrectos = opcionesCorrectas.stream()
                    .map(Opcion::getDescripcion)
                    .collect(Collectors.toList());
                    
                if (respuestas.size() != textosCorrectos.size()) return false;
                
                return respuestas.containsAll(textosCorrectos);
            }
        }
        
        return false;
    }

    @GetMapping("/{examenId}/intentos")
    @PreAuthorize("hasAnyAuthority('DOCENTE','ADMIN')")
    public String verIntentosExamen(@PathVariable Long examenId,
                                    Authentication authentication,
                                    Model model) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen no encontrado"));

        Long ofertaId = examen.getModulo().getCurso().getIdOferta();
        return "redirect:/aula/oferta/" + ofertaId + "/calificaciones#examen-" + examenId;
    }

    @GetMapping("/{examenId}/intentos/{intentoId}/fragment-correccion")
    @PreAuthorize("hasAnyAuthority('DOCENTE','ADMIN')")
    public String obtenerFragmentCorreccion(@PathVariable Long examenId,
                                            @PathVariable UUID intentoId,
                                            Authentication authentication,
                                            Model model) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen no encontrado"));

        var usuario = usuarioRepository.findByDni(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!puedeGestionarExamen(authentication, usuario, examen)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Intento intento = intentoRepository.findByIdIntentoAndExamen_IdActividad(intentoId, examenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));

        List<Map<String, Object>> respuestasDetalle = new ArrayList<>();
        if (intento.getRespuestas() != null) {
            for (RespuestaIntento respuesta : intento.getRespuestas()) {
                Map<String, Object> detalle = new HashMap<>();
                detalle.put("pregunta", respuesta.getPregunta());
                detalle.put("respuestaTexto", formatearRespuestaUsuario(respuesta.getRespuestaUsuario()));
                detalle.put("respuestaLista", obtenerRespuestaComoLista(respuesta.getRespuestaUsuario()));
                detalle.put("puntaje", respuesta.getPuntajeObtenido());
                detalle.put("esCorrecta", respuesta.getEsCorrecta());
                detalle.put("requiereRevision", respuesta.getRequiereRevisionManual());
                respuestasDetalle.add(detalle);
            }
        }
        
        // Ordenar por orden de pregunta si es necesario (asumimos orden de inserción o UUID por ahora)

        model.addAttribute("examen", examen);
        model.addAttribute("intento", intento);
        model.addAttribute("respuestas", respuestasDetalle);

        return "docente/fragments/examen-correccion-panel :: panel-correccion";
    }

    @PostMapping("/intentos/{intentoId}/calificar")
    @PreAuthorize("hasAnyAuthority('DOCENTE','ADMIN')")
    public String calificarIntento(@PathVariable UUID intentoId,
                                   @RequestParam Map<String, String> allParams,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        
        Intento intento = intentoRepository.findById(intentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));
                
        float puntajeTotal = 0;
        
        if (intento.getRespuestas() != null) {
            for (RespuestaIntento respuesta : intento.getRespuestas()) {
                String paramName = "puntaje_" + respuesta.getPregunta().getIdPregunta();
                if (allParams.containsKey(paramName)) {
                    try {
                        float puntaje = Float.parseFloat(allParams.get(paramName));
                        // Validar que no exceda el puntaje de la pregunta
                        if (puntaje > respuesta.getPregunta().getPuntaje()) {
                            puntaje = respuesta.getPregunta().getPuntaje();
                        }
                        if (puntaje < 0) {
                            puntaje = 0;
                        }
                        
                        respuesta.setPuntajeObtenido(puntaje);
                        // Se considera correcta si obtuvo el puntaje máximo, o parcial. 
                        // Para simplificar: >= puntaje máximo es FULL correct.
                        respuesta.setEsCorrecta(puntaje >= respuesta.getPregunta().getPuntaje());
                        respuesta.setRequiereRevisionManual(false);
                        
                        puntajeTotal += puntaje;
                    } catch (NumberFormatException e) {
                        // Ignorar valores no numéricos
                    }
                }
            }
        }
        
        intento.setCalificacion(puntajeTotal);
        intento.setEstado(com.example.demo.enums.EstadoIntento.FINALIZADO);
        intentoRepository.save(intento);
        
        redirectAttributes.addFlashAttribute("mensaje", "Calificación guardada correctamente");
        
        Long ofertaId = intento.getExamen().getModulo().getCurso().getIdOferta();
        return "redirect:/aula/oferta/" + ofertaId + "/calificaciones#examen-" + intento.getExamen().getIdActividad();
    }

    @GetMapping("/detalle/{id}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    public String verDetalleExamen(@PathVariable Long id, Model model) {
        Examen examen = examenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examen no encontrado"));
        
        boolean haComenzado = examen.getFechaApertura() != null && 
                              examen.getFechaApertura().isBefore(LocalDateTime.now());
        
        long intentos = intentoRepository.findByExamen_IdActividad(id).size();
        boolean tieneIntentos = intentos > 0;
        
        boolean soloFechas = haComenzado || tieneIntentos;
        
        // Obtener pools disponibles para la oferta académica
        List<Pool> poolsDisponibles = new ArrayList<>();
        if (examen.getModulo() != null && examen.getModulo().getCurso() != null) {
            poolsDisponibles = poolRepository.findByOferta_IdOferta(examen.getModulo().getCurso().getIdOferta());
        }
        
        // Preparar datos de preguntas para el frontend (JSON)
        Map<String, Object> preguntasData = new HashMap<>();
        try {
            for (Pool pool : examen.getPoolPreguntas()) {
                if (pool.getPreguntas() != null) {
                    for (Pregunta p : pool.getPreguntas()) {
                        Map<String, Object> pData = new HashMap<>();
                        pData.put("enunciado", p.getEnunciado());
                        pData.put("tipo", p.getTipoPregunta());
                        pData.put("puntaje", p.getPuntaje());
                        
                        List<Map<String, Object>> opcionesData = new ArrayList<>();
                        if (p.getOpciones() != null) {
                            for (Opcion op : p.getOpciones()) {
                                Map<String, Object> oData = new HashMap<>();
                                oData.put("descripcion", op.getDescripcion());
                                oData.put("esCorrecta", op.getEsCorrecta());
                                opcionesData.add(oData);
                            }
                        }
                        pData.put("opciones", opcionesData);
                        
                        preguntasData.put(p.getIdPregunta().toString(), pData);
                    }
                }
            }
            model.addAttribute("preguntasDataJson", objectMapper.writeValueAsString(preguntasData));
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("preguntasDataJson", "{}");
        }
        
        model.addAttribute("examen", examen);
        model.addAttribute("poolsDisponibles", poolsDisponibles);
        model.addAttribute("soloFechas", soloFechas);
        model.addAttribute("haComenzado", haComenzado);
        model.addAttribute("tieneIntentos", tieneIntentos);
        
        return "docente/examen-detalle";
    }
    
    @PostMapping("/guardar-detalle")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    public String guardarDetalleExamen(@ModelAttribute Examen examenForm, 
                                       @RequestParam Long id, 
                                       @RequestParam(required = false) List<UUID> pools,
                                       RedirectAttributes redirectAttributes) {
        Examen examen = examenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examen no encontrado"));
        
        // Validar fechas
        if (examenForm.getFechaApertura() != null && examenForm.getFechaCierre() != null && 
            examenForm.getFechaCierre().isBefore(examenForm.getFechaApertura())) {
            redirectAttributes.addFlashAttribute("error", "La fecha de cierre no puede ser anterior a la fecha de apertura");
            return "redirect:/examen/detalle/" + id;
        }
        
        boolean haComenzado = examen.getFechaApertura() != null && 
                              examen.getFechaApertura().isBefore(LocalDateTime.now());
        long intentos = intentoRepository.findByExamen_IdActividad(id).size();
        boolean tieneIntentos = intentos > 0;
        boolean soloFechas = haComenzado || tieneIntentos;
        
        if (soloFechas) {
            // Solo actualizar fechas
            examen.setFechaApertura(examenForm.getFechaApertura());
            examen.setFechaCierre(examenForm.getFechaCierre());
        } else {
            // Actualizar todo
            examen.setTitulo(examenForm.getTitulo());
            examen.setDescripcion(examenForm.getDescripcion());
            examen.setFechaApertura(examenForm.getFechaApertura());
            examen.setFechaCierre(examenForm.getFechaCierre());
            examen.setTiempoRealizacion(examenForm.getTiempoRealizacion());
            examen.setCantidadIntentos(examenForm.getCantidadIntentos());
            
            // Actualizar pools
            if (pools != null) {
                List<Pool> poolsSeleccionados = poolRepository.findAllById(pools);
                examen.setPoolPreguntas(poolsSeleccionados);
            } else {
                examen.setPoolPreguntas(new ArrayList<>());
            }
        }
        
        examenRepository.save(examen);
        redirectAttributes.addFlashAttribute("mensaje", "Examen actualizado correctamente");
        return "redirect:/examen/detalle/" + id;
    }

    private boolean puedeGestionarExamen(Authentication authentication, com.example.demo.model.Usuario usuario, Examen examen) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> {
                    String nombre = auth.getAuthority();
                    return "ADMIN".equalsIgnoreCase(nombre) || "ROLE_ADMIN".equalsIgnoreCase(nombre);
                });
        if (esAdmin) {
            return true;
        }

        if (!(usuario instanceof Docente docente)) {
            return false;
        }

        OfertaAcademica oferta = examen.getModulo() != null ? examen.getModulo().getCurso() : null;
        if (oferta instanceof Curso curso) {
            return curso.getDocentes() != null && curso.getDocentes().stream()
                    .anyMatch(d -> d.getId().equals(docente.getId()));
        }
        if (oferta instanceof Formacion formacion) {
            return formacion.getDocentes() != null && formacion.getDocentes().stream()
                    .anyMatch(d -> d.getId().equals(docente.getId()));
        }
        return false;
    }

    private List<String> obtenerRespuestaComoLista(String respuestaUsuario) {
        if (respuestaUsuario == null || respuestaUsuario.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode node = objectMapper.readTree(respuestaUsuario.trim());
            if (node.isArray()) {
                List<String> valores = new ArrayList<>();
                node.forEach(n -> valores.add(n.isTextual() ? n.asText() : n.toString()));
                return valores;
            }
            // Si es un simple JSON String (ej: "Respuesta")
            if (node.isTextual()) {
                return Collections.singletonList(node.asText());
            }
        } catch (Exception ignored) {
            // No es JSON válido, retornar como único elemento
        }
        return Collections.singletonList(respuestaUsuario.trim());
    }

    private String formatearRespuestaUsuario(String respuestaUsuario) {
        if (respuestaUsuario == null || respuestaUsuario.isBlank()) {
            return "Sin respuesta";
        }

        String valor = respuestaUsuario.trim();
        try {
            JsonNode node = objectMapper.readTree(valor);
            if (node.isArray()) {
                List<String> valores = new ArrayList<>();
                node.forEach(n -> valores.add(n.isTextual() ? n.asText() : n.toString()));
                return String.join(", ", valores);
            }
            if (node.isObject()) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            }
        } catch (Exception ignored) {
            // Si no es JSON, devolvemos el texto original.
        }

        return valor;
    }
}
