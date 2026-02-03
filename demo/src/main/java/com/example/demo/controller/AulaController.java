package com.example.demo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.enums.EstadoAsistencia;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.AsistenciaService;

@Controller
@RequestMapping("/aula")
public class AulaController {

    private final OfertaAcademicaRepository ofertaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsistenciaService asistenciaService;
    private final ClaseRepository claseRepository;
    private final HorarioRepository horarioRepository;
    private final TareaRepository tareaRepository;
    private final ExamenRepository examenRepository;
    private final IntentoRepository intentoRepository;
    private final EntregaRepository entregaRepository;

    public AulaController(OfertaAcademicaRepository ofertaRepository,
                          InscripcionRepository inscripcionRepository,
                          UsuarioRepository usuarioRepository,
                          AsistenciaService asistenciaService,
                          ClaseRepository claseRepository,
                          HorarioRepository horarioRepository,
                          TareaRepository tareaRepository,
                          ExamenRepository examenRepository,
                          IntentoRepository intentoRepository,
                          EntregaRepository entregaRepository) {
        this.ofertaRepository = ofertaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.asistenciaService = asistenciaService;
        this.claseRepository = claseRepository;
        this.horarioRepository = horarioRepository;
        this.tareaRepository = tareaRepository;
        this.examenRepository = examenRepository;
        this.intentoRepository = intentoRepository;
        this.entregaRepository = entregaRepository;
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

    @GetMapping("/oferta/{id}/asistencia")
    @Transactional(readOnly = true)
    public String verAsistencia(@PathVariable Long id, Model model, Authentication auth) {
        Long ofertaIdSeguro = Objects.requireNonNull(id, "El id de la oferta es requerido");
        OfertaAcademica oferta = ofertaRepository.findById(ofertaIdSeguro)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        
        String currentUserDni = auth.getName();
        Usuario currentUser = usuarioRepository.findByDni(currentUserDni)
                .or(() -> usuarioRepository.findByCorreo(currentUserDni))
                .orElseThrow(() -> new RuntimeException("Usuario actual no encontrado"));

        model.addAttribute("oferta", oferta);
        model.addAttribute("currentUser", currentUser);
        
        // Use repo method
        List<AsistenciaRepository> asistenciaRepositoryList; // Dummy to avoid unused import error if I don't import it? No, repo is injected.
        
        boolean isDocenteOrAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getNombre().equalsIgnoreCase("DOCENTE") || r.getNombre().equalsIgnoreCase("ADMIN"));
        
        if (isDocenteOrAdmin) {
            // Generar lista de clases calculadas (Reales + Teóricas según horario)
            List<Map<String, Object>> clasesCalculadas = new ArrayList<>();
            List<Clase> clasesReales = claseRepository.findByModuloCursoIdOferta(id);
            Map<LocalDate, Clase> mapaClases = new HashMap<>();
            Set<LocalDate> fechasAgregadas = new HashSet<>();
            
            if (clasesReales != null) {
                for (Clase c : clasesReales) {
                    if (c.getInicio() != null) {
                        mapaClases.put(c.getInicio().toLocalDate(), c);
                    }
                }
            }
            
            // Obtener días válidos desde Horarios
            Set<java.time.DayOfWeek> diasValidos = new HashSet<>();
            if (oferta.getHorarios() != null) {
                for (Horario h : oferta.getHorarios()) {
                    if (h.getDia() != null) {
                        switch (h.getDia()) {
                            case LUNES -> diasValidos.add(java.time.DayOfWeek.MONDAY);
                            case MARTES -> diasValidos.add(java.time.DayOfWeek.TUESDAY);
                            case MIERCOLES -> diasValidos.add(java.time.DayOfWeek.WEDNESDAY);
                            case JUEVES -> diasValidos.add(java.time.DayOfWeek.THURSDAY);
                            case VIERNES -> diasValidos.add(java.time.DayOfWeek.FRIDAY);
                            case SABADO -> diasValidos.add(java.time.DayOfWeek.SATURDAY);
                            case DOMINGO -> diasValidos.add(java.time.DayOfWeek.SUNDAY);
                        }
                    }
                }
            }
            
            // Iterar desde Inicio hasta Hoy (o Fin)
            LocalDate inicio = oferta.getFechaInicio();
            LocalDate fin = LocalDate.now();
            if (oferta.getFechaFin() != null && fin.isAfter(oferta.getFechaFin())) {
                fin = oferta.getFechaFin();
            } else if (oferta.getEstado() == com.example.demo.enums.EstadoOferta.FINALIZADA && oferta.getFechaFin() != null) {
                 fin = oferta.getFechaFin();
            }
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            // Si no hay horarios definidos, mostramos solo las clases reales o nada
            if (!diasValidos.isEmpty() && inicio != null) {
                for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
                    if (diasValidos.contains(fecha.getDayOfWeek())) {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("fechaIso", fecha.toString()); // yyyy-MM-dd
                        
                        // Formato amigable: "01/02/2026 - Lunes"
                        String diaSemana = switch(fecha.getDayOfWeek()) {
                            case MONDAY -> "Lunes";
                            case TUESDAY -> "Martes";
                            case WEDNESDAY -> "Miércoles";
                            case THURSDAY -> "Jueves";
                            case FRIDAY -> "Viernes";
                            case SATURDAY -> "Sábado";
                            case SUNDAY -> "Domingo";
                        };
                        
                        if (mapaClases.containsKey(fecha)) {
                            Clase c = mapaClases.get(fecha);
                            dto.put("id", c.getIdClase());
                            dto.put("titulo", c.getTitulo() + " (" + fecha.format(dtf) + ")");
                            dto.put("display", fecha.format(dtf) + " (" + diaSemana + ") - " + c.getTitulo());
                        } else {
                            dto.put("id", "");
                            dto.put("titulo", "Clase del " + fecha.format(dtf));
                            dto.put("display", fecha.format(dtf) + " (" + diaSemana + ")");
                        }
                        clasesCalculadas.add(dto);
                        fechasAgregadas.add(fecha);
                    }
                }
            } 
            
            // AGREGAR CLASES EXTRA (Fuera de horario programado)
            if (clasesReales != null) {
                 for(Clase c : clasesReales) {
                     if(c.getInicio() != null) {
                         LocalDate fechaClase = c.getInicio().toLocalDate();
                         if (!fechasAgregadas.contains(fechaClase)) {
                             // Es una clase extra fuera del horario estándar
                             Map<String, Object> dto = new HashMap<>();
                             dto.put("fechaIso", fechaClase.toString());
                             dto.put("id", c.getIdClase());
                             
                             String diaSemana = switch(fechaClase.getDayOfWeek()) {
                                case MONDAY -> "Lunes";
                                case TUESDAY -> "Martes";
                                case WEDNESDAY -> "Miércoles";
                                case THURSDAY -> "Jueves";
                                case FRIDAY -> "Viernes";
                                case SATURDAY -> "Sábado";
                                case SUNDAY -> "Domingo";
                            };
                             
                             dto.put("titulo", c.getTitulo() + " (" + fechaClase.format(dtf) + ")");
                             dto.put("display", fechaClase.format(dtf) + " (" + diaSemana + ") - " + c.getTitulo() + " (Extra)");
                             clasesCalculadas.add(dto);
                             fechasAgregadas.add(fechaClase);
                         }
                     }
                 }
            }
            
            // Si no hubo horarios y se agregaron solo reales arriba, esto ya cubre el fallback anterior.
            // (El bloque `fallback` original se vuelve redundante o necesita ajuste. 
            //  Pero como mantengo la estructura if/else del original para minimizar cambios drásticos, revisaré el else).
            
            if (diasValidos.isEmpty() && clasesCalculadas.isEmpty()) {
                 // Fallback original: Si no hay horario, mostrar solo las reales
                 // Pero mi bloque 'AGREGAR CLASES EXTRA' ya lo hace si clasesCalculadas estaba vacío.
                 // Entonces el else original se puede quitar o dejar vacío.
            }
            
            // Ordenar por fecha descendente (más reciente primero)
            clasesCalculadas.sort((a, b) -> ((String)b.get("fechaIso")).compareTo((String)a.get("fechaIso")));
            
            model.addAttribute("clasesCalculadas", clasesCalculadas);
            // model.addAttribute("clases", clases); // Ya no lo usamos directamente en el select
            
            List<Inscripciones> inscripciones = inscripcionRepository.findByOfertaIdOferta(id);
            
            // Construir mapa de fechas de inscripción (DNI -> Fecha Inscripción)
            Map<String, String> mapInscripcion = new HashMap<>();
            
            List<Usuario> alumnos = inscripciones.stream()
                    .filter(i -> i.getEstadoInscripcion() != null && i.getEstadoInscripcion()) // Solo inscripciones activas
                    .peek(i -> {
                        if (i.getAlumno() != null && i.getFechaInscripcion() != null) {
                            mapInscripcion.put(i.getAlumno().getDni(), i.getFechaInscripcion().toString());
                        }
                    })
                    .map(Inscripciones::getAlumno)
                    .filter(u -> {
                        return u.getRoles().stream().noneMatch(r -> r.getNombre().equalsIgnoreCase("DOCENTE") || r.getNombre().equalsIgnoreCase("ADMIN"));
                    })
                    .distinct() // Evitar duplicados por si acaso
                    .collect(Collectors.toList());
            alumnos.sort(Comparator.comparing(Usuario::getApellido).thenComparing(Usuario::getNombre));
            
            model.addAttribute("alumnos", alumnos);
            model.addAttribute("mapInscripcion", mapInscripcion);
            model.addAttribute("estados", EstadoAsistencia.values());
            
            // Cargar todas las asistencias existentes para esta oferta
            // Para pintar la tabla sin N+1 requests
            Map<String, String> asistenciaMap = new HashMap<>(); // Key: fechaIso_dni -> Estado
            List<Asistencia> allAsistencias = asistenciaService.getAsistenciaRepository().findByOfertaIdOferta(id);
            for(Asistencia a : allAsistencias) {
                if(a.getFecha() != null && a.getAlumno() != null) {
                     String key = a.getFecha().toString() + "_" + a.getAlumno().getDni();
                     asistenciaMap.put(key, a.getEstado().name());
                }
            }
            model.addAttribute("asistenciaMap", asistenciaMap);
            
        } else {
            List<Asistencia> misAsistencias = asistenciaService.getAsistenciasPorAlumnoYOferta(id, currentUser.getDni());
            if(misAsistencias == null) misAsistencias = new ArrayList<>();
            misAsistencias.sort(Comparator.comparing(Asistencia::getFecha).reversed());
            
            model.addAttribute("misAsistencias", misAsistencias);
            
            long total = misAsistencias.size();
            long presentes = misAsistencias.stream().filter(a -> a.getEstado() == EstadoAsistencia.PRESENTE).count();
            double porcentaje = total > 0 ? (double) presentes / total * 100 : 0;
            
            model.addAttribute("statsTotal", total);
            model.addAttribute("statsPresentes", presentes);
            model.addAttribute("statsPorcentaje", Math.round(porcentaje));
        }

        return "aula/asistencia";
    }

    @GetMapping("/oferta/{id}/calificaciones")
    public String verCalificaciones(@PathVariable Long id, Model model, Authentication auth) {
        Long ofertaIdSeguro = Objects.requireNonNull(id, "El id de la oferta es requerido");
        OfertaAcademica oferta = ofertaRepository.findById(ofertaIdSeguro)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        
        String currentUserDni = auth.getName();
        Usuario currentUser = usuarioRepository.findByDni(currentUserDni)
                .or(() -> usuarioRepository.findByCorreo(currentUserDni))
                .orElseThrow(() -> new RuntimeException("Usuario actual no encontrado"));

        model.addAttribute("oferta", oferta);
        model.addAttribute("currentUser", currentUser);
        
        boolean isDocenteOrAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getNombre().equalsIgnoreCase("DOCENTE") || r.getNombre().equalsIgnoreCase("ADMIN"));
        model.addAttribute("isDocenteOrAdmin", isDocenteOrAdmin);

        // Alumnos
        List<Inscripciones> inscripciones = inscripcionRepository.findByOfertaIdOferta(id);
        List<Usuario> alumnos = inscripciones.stream()
                .filter(i -> i.getEstadoInscripcion() != null && i.getEstadoInscripcion())
                .map(Inscripciones::getAlumno)
                .filter(u -> u.getRoles().stream().noneMatch(r -> r.getNombre().equalsIgnoreCase("DOCENTE") || r.getNombre().equalsIgnoreCase("ADMIN")))
                .distinct()
                .sorted(Comparator.comparing(Usuario::getApellido).thenComparing(Usuario::getNombre))
                .collect(Collectors.toList());
        model.addAttribute("alumnos", alumnos);

        // Actividades
        List<Tarea> tareas = tareaRepository.findByCursoId(id);
        List<Examen> examenes = examenRepository.findByModulo_Curso_IdOferta(id);
        
        model.addAttribute("tareas", tareas);
        model.addAttribute("examenes", examenes);

        // Notas Mapeadas: Map<UUID_Alumno, Map<ID_Actividad, Nota>>
        Map<UUID, Map<Long, Double>> notasTareas = new HashMap<>(); 
        Map<UUID, Map<Long, Float>> notasExamenes = new HashMap<>(); 

        for(Tarea t : tareas) {
             List<Entrega> entregas = entregaRepository.findByTarea(t); 
             for(Entrega e : entregas) {
                 if(e.getEstudiante() != null && e.getCalificacion() != null) {
                      notasTareas.computeIfAbsent(e.getEstudiante().getId(), k -> new HashMap<>())
                                 .put(t.getIdActividad(), e.getCalificacion());
                 }
             }
        }
        
        for(Examen e : examenes) {
             List<Intento> intentos = intentoRepository.findByExamen_IdActividad(e.getIdActividad());
             for(Intento i : intentos) {
                 if((i.getEstado().name().equals("FINALIZADO") || i.getEstado().name().equals("CALIFICADO")) && i.getCalificacion() != null) {
                      notasExamenes.computeIfAbsent(i.getAlumno().getId(), k -> new HashMap<>())
                                   .merge(e.getIdActividad(), i.getCalificacion(), Math::max);
                 }
             }
        }
        
        model.addAttribute("notasTareas", notasTareas);
        model.addAttribute("notasExamenes", notasExamenes);

        // Sección Corrección (Solo Docente)
        if(isDocenteOrAdmin) {
            List<Map<String, Object>> correccionExamenes = new ArrayList<>();
            for(Examen e : examenes) {
                // Fetch all attempts for the exam
                List<Intento> intentos = intentoRepository.findByExamen_IdActividad(e.getIdActividad());
                
                // Filtrar intentos sin alumno (huérfanos) para evitar NPE en la vista
                intentos = intentos.stream()
                        .filter(i -> i.getAlumno() != null)
                        .collect(Collectors.toList());

                // Sort attempts by date descending (newest first)
                intentos.sort(Comparator.comparing(Intento::getFechaFin, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                
                long pendientes = intentos.stream().filter(i -> i.getEstado() == com.example.demo.enums.EstadoIntento.PENDIENTE_CORRECCION).count();
                long totalIntentos = intentos.size();
                long finalizados = intentos.stream().filter(i -> i.getEstado() == com.example.demo.enums.EstadoIntento.FINALIZADO).count();

                Map<String, Object> dato = new HashMap<>();
                // Flatten data for view safety
                dato.put("idActividad", e.getIdActividad());
                dato.put("titulo", e.getTitulo());
                dato.put("fechaCierreStr", e.getFechaCierre() != null ? e.getFechaCierre().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null);

                dato.put("examen", e);
                dato.put("pendientes", pendientes);
                dato.put("finalizados", finalizados);
                dato.put("total", totalIntentos);
                dato.put("intentos", intentos); // Pass attempts to view
                correccionExamenes.add(dato);
            }
            model.addAttribute("correccionExamenes", correccionExamenes);

            // Sección Corrección de Tareas (Solo Docente)
            List<Map<String, Object>> correccionTareas = new ArrayList<>();
            for(Tarea t : tareas) {
                List<Entrega> entregas = entregaRepository.findByTarea(t);
                
                // Filtrar entregas sin estudiante para evitar NPE en la vista
                entregas = entregas.stream()
                        .filter(ent -> ent.getEstudiante() != null)
                        .collect(Collectors.toList());

                // Sort by date descending
                entregas.sort(Comparator.comparing(Entrega::getFechaEntrega, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

                long pendientes = entregas.stream().filter(i -> i.getCalificacion() == null).count();
                long total = entregas.size();
                long calificados = entregas.stream().filter(i -> i.getCalificacion() != null).count();

                Map<String, Object> dato = new HashMap<>();
                // Flatten data for view safety
                dato.put("idActividad", t.getIdActividad());
                dato.put("titulo", t.getTitulo());
                dato.put("fechaEntregaStr", t.getLimiteEntrega() != null ? t.getLimiteEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null);
                dato.put("entregasTardias", t.getEntregasTardias());

                dato.put("tarea", t);
                dato.put("pendientes", pendientes);
                dato.put("calificados", calificados);
                dato.put("total", total);
                dato.put("entregas", entregas);
                correccionTareas.add(dato);
            }
            model.addAttribute("correccionTareas", correccionTareas);
        }

        return "aula/calificaciones";
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

        // Construir salida incluyendo la fecha de inscripción del alumno
        return inscripciones.stream()
                .filter(i -> i.getEstadoInscripcion() != null && i.getEstadoInscripcion())
                .map(i -> {
                    Usuario u = i.getAlumno();
                    // Filtrar usuario actual y roles no alumno
                    if (currentUser != null && u.getDni().equals(currentUser.getDni())) return null;
                    boolean isTeacherOrAdmin = u.getRoles().stream()
                        .anyMatch(r -> r.getNombre().equalsIgnoreCase("DOCENTE") || r.getNombre().equalsIgnoreCase("ADMIN"));
                    if (isTeacherOrAdmin) return null;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("dni", u.getDni());
                    map.put("name", u.getNombre() + " " + u.getApellido());
                    map.put("email", u.getCorreo());
                    String role = u.getRoles().isEmpty() ? "ESTUDIANTE" : u.getRoles().iterator().next().getNombre();
                    map.put("role", role);
                    map.put("avatar", (u.getNombre().substring(0, 1) + u.getApellido().substring(0, 1)).toUpperCase());
                    map.put("color", "bg-blue-100 text-blue-600");
                    map.put("fechaInscripcion", i.getFechaInscripcion());
                    return map;
                })
                .filter(Objects::nonNull)
                // Evitar duplicados por DNI
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(m -> m.get("dni"), m -> m, (a,b) -> a, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())));
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

    @GetMapping("/api/oferta/{id}/asistencia/fecha/{fecha}")
    @ResponseBody
    public List<Map<String, Object>> getAsistenciaPorFecha(@PathVariable Long id, @PathVariable String fecha) {
        LocalDate fechaParsed = LocalDate.parse(fecha);
        List<Asistencia> asistencias = asistenciaService.getAsistenciasPorFecha(id, fechaParsed);
        
        return asistencias.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("dni", a.getAlumno().getDni()); // Changed from id to dni to match UI
            map.put("estado", a.getEstado().name());
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/api/asistencia/registrar")
    @ResponseBody
    @Transactional // Ensure lazy collections (horarios) are initialized
    public ResponseEntity<?> registrarAsistencia(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            // Validar autenticación
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "No autenticado"));
            }
            
            // Fix: Look up by DNI or Email to support both login types
            String currentUserKey = principal.getName();
            Usuario usuarioLogueado = usuarioRepository.findByDni(currentUserKey)
                 .or(() -> usuarioRepository.findByCorreo(currentUserKey))
                 .orElseThrow(() -> new RuntimeException("Usuario actual no encontrado"));

            Long ofertaId = Long.valueOf(payload.get("ofertaId").toString());
            String dni = payload.get("dni").toString();

            // Validar autorización (Solo Docente del curso)
            // Usamos el repositorio directamente para evitar problemas de Lazy Loading y Proxies de Hibernate
            // Check based on roles
            boolean isDocente = usuarioLogueado.getRoles().stream()
                .anyMatch(r -> "DOCENTE".equalsIgnoreCase(r.getNombre()) || "ROLE_DOCENTE".equalsIgnoreCase(r.getNombre()));

            boolean esDocenteDeLaOferta = false;
        
            if (isDocente) {
                // 1. Check if connected via Horario
                esDocenteDeLaOferta = horarioRepository.existsByOfertaAcademica_IdOfertaAndDocente_Id(ofertaId, usuarioLogueado.getId());
                
                // 2. If not found in Horario, check direct relationship (Curso/Formacion)
                if (!esDocenteDeLaOferta) {
                    OfertaAcademica ofertaCheck = ofertaRepository.findById(ofertaId).orElse(null);
                    if (ofertaCheck instanceof Curso) {
                         esDocenteDeLaOferta = ((Curso) ofertaCheck).getDocentes().stream()
                             .anyMatch(d -> d.getId().equals(usuarioLogueado.getId()));
                    } else if (ofertaCheck instanceof Formacion) {
                         esDocenteDeLaOferta = ((Formacion) ofertaCheck).getDocentes().stream()
                             .anyMatch(d -> d.getId().equals(usuarioLogueado.getId()));
                    }
                }
            }
            
            // REQUERIMIENTO: Solo el docente de la oferta puede modificar la asistencia. Admin NO.
            if (!esDocenteDeLaOferta) {
               // DEBUG: Imprimir por qué falló si es necesario, o devolver más info temporalmente si sigue fallando
               // return ResponseEntity.status(403).body(Map.of("success", false, "message", "User " + usuarioLogueado.getId() + " is not teacher for offer " + ofertaId));
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "No autorizado para modificar asistencia en esta oferta (Solo el docente a cargo)"));
            }

            // Parse de parámetros y delegación en servicio (el servicio valida con el modelo)
            LocalDate fecha = LocalDate.parse(payload.get("fecha").toString());

            String estadoStr = payload.get("estado").toString();
            UUID claseId = null;
            if (payload.containsKey("claseId") && payload.get("claseId") != null) {
                String claseIdStr = payload.get("claseId").toString();
                if (!claseIdStr.isBlank()) {
                    claseId = UUID.fromString(claseIdStr);
                }
            }
            
            // Use Enum directly
            EstadoAsistencia estado;
            try {
                estado = EstadoAsistencia.valueOf(estadoStr);
            } catch (IllegalArgumentException e) {
                 // Fallback or error? Let's try to map common lowercase values 
                 if ("present".equalsIgnoreCase(estadoStr)) estado = EstadoAsistencia.PRESENTE;
                 else if ("late".equalsIgnoreCase(estadoStr)) estado = EstadoAsistencia.TARDANZA;
                 else if ("absent".equalsIgnoreCase(estadoStr)) estado = EstadoAsistencia.AUSENTE;
                 else throw new IllegalArgumentException("Estado desconocido: " + estadoStr);
            }
            
            asistenciaService.registrarAsistencia(ofertaId, dni, fecha, estado, claseId);
            
            return ResponseEntity.ok().body(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage() != null ? e.getMessage() : "Error desconocido"));
        }
    }
}
