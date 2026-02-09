package com.example.demo.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.enums.EstadoCuota;
import com.example.demo.enums.EstadoIntento;
import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.EstadoPago;
import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Auditable;
import com.example.demo.model.Curso;
import com.example.demo.model.Cuota;
import com.example.demo.model.Examen;
import com.example.demo.model.Intento;
import com.example.demo.model.Formacion;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Instituto;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pago;
import com.example.demo.model.Usuario;
import com.example.demo.model.Clase;
import com.example.demo.model.Tarea;
import com.example.demo.model.Entrega;
import com.example.demo.repository.*;
import com.example.demo.service.MercadoPagoService;
import com.example.demo.service.UsuarioImagenService;
import com.example.demo.service.InstitutoService;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.example.demo.model.CustomUsuarioDetails;
import com.example.demo.service.ReporteService;
import com.example.demo.service.EmailService;
import com.example.demo.service.OfertaAcademicaService;
import com.example.demo.service.AnalisisRendimientoService;
import com.example.demo.service.RecuperarContraseñaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;


@Controller
@RequestMapping("/alumno")
public class AlumnoController {

    @Value("${app.base-url}")
    private String baseUrl;

    private static final Locale LOCALE_ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FORMATO_FECHA_RESUMEN = DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE_ES_AR);

    private final TareaRepository tareaRepository;
    private final ExamenRepository examenRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClaseRepository claseRepository;
    private final ObjectMapper objectMapper;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final ModuloRepository moduloRepository;
    private final IntentoRepository intentoRepository;
    private final MercadoPagoService mercadoPagoService;
    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;
    private final InstitutoService institutoService;
    private final ReporteService reporteService;
    private final EmailService emailService;
    private final EntregaRepository entregaRepository;
    private final RecuperarContraseñaService passwordRecoveryService;
    
    @Autowired
    private OfertaAcademicaService ofertaAcademicaService;

    @Autowired
    private AnalisisRendimientoService analisisRendimientoService;
    
    @Autowired
    private com.example.demo.service.NotificacionInscripcionService notificacionInscripcionService;
    
    @Autowired
    private UsuarioImagenService usuarioImagenService;

    public AlumnoController(TareaRepository tareaRepository,
                          ExamenRepository examenRepository,
                          InscripcionRepository inscripcionRepository,
                          UsuarioRepository usuarioRepository,
                          ClaseRepository claseRepository,
                          ObjectMapper objectMapper,
                          OfertaAcademicaRepository ofertaAcademicaRepository,
                          ModuloRepository moduloRepository,
                          IntentoRepository intentoRepository,
                          MercadoPagoService mercadoPagoService,
                          PagoRepository pagoRepository,
                          CuotaRepository cuotaRepository,
                          InstitutoService institutoService,
                          ReporteService reporteService,
                          EmailService emailService,
                          EntregaRepository entregaRepository,
                          RecuperarContraseñaService passwordRecoveryService) {
        this.tareaRepository = tareaRepository;
        this.examenRepository = examenRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.claseRepository = claseRepository;
        this.objectMapper = objectMapper;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.moduloRepository = moduloRepository;
        this.intentoRepository = intentoRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.pagoRepository = pagoRepository;
        this.cuotaRepository = cuotaRepository;
        this.institutoService = institutoService;
        this.reporteService = reporteService;
        this.emailService = emailService;
        this.entregaRepository = entregaRepository;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @GetMapping("/alumno")
    public String alumnoDashboard() {
        return "publico";
    }
    
    // Mi Espacio - Dashboard del alumno con calendario
    @GetMapping("/mi-espacio")
    public String miEspacio(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            
            model.addAttribute("alumno", alumno);

            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoId(alumno.getId());
            model.addAttribute("inscripciones", inscripciones); // Agregado para usar en la vista

            List<OfertaAcademica> ofertasActivas = inscripciones.stream()
                .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())) // Filtrar inscripciones canceladas
                .map(Inscripciones::getOferta)
                .filter(oa -> oa.getEstado() == EstadoOferta.ACTIVA || oa.getEstado() == EstadoOferta.ENCURSO)
                .collect(Collectors.toList());

            long totalCursosActivos = ofertasActivas.size();

            // Tareas y Exámenes de TODAS las ofertas activas (Cursos y Formaciones)
            List<Tarea> todasLasTareas = tareaRepository.findByModuloCursoIn(ofertasActivas);
            List<Entrega> todasLasEntregas = entregaRepository.findByEstudiante(alumno);
            
            // Exámenes
            List<Examen> todosLosExamenes = examenRepository.findByModulo_CursoIn(ofertasActivas);
            List<Intento> intentosAlumno = intentoRepository.findByAlumno(alumno);

            // IDs de tareas y examenes activos para asegurar consistencia
            java.util.Set<Long> tareasActivasIds = todasLasTareas.stream()
                .map(Tarea::getIdActividad)
                .collect(Collectors.toSet());
            
            java.util.Set<Long> examenesActivosIds = todosLosExamenes.stream()
                .map(Examen::getIdActividad)
                .collect(Collectors.toSet());

            long tareasCompletadas = todasLasEntregas.stream()
                .map(e -> e.getTarea().getIdActividad())
                .filter(tareasActivasIds::contains)
                .distinct()
                .count();

            // Exámenes: Contar cuántos exámanes ÚNICOS de las ofertas activas tienen intentos
            long examenesCompletados = intentosAlumno.stream()
                .filter(i -> i.getExamen() != null && examenesActivosIds.contains(i.getExamen().getIdActividad()))
                .map(i -> i.getExamen().getIdActividad())
                .distinct()
                .count();

            // Cursos Finalizados
            long cursosCompletados = inscripciones.stream()
                .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                .map(Inscripciones::getOferta)
                .filter(o -> o.getEstado() == EstadoOferta.FINALIZADA || 
                        (o.getFechaFin() != null && o.getFechaFin().isBefore(LocalDate.now())))
                .count();

            // Tareas: Mostrar TODAS (Pendientes, Entregadas, Futuras) ordenadas por fecha
            LocalDateTime ahora = LocalDateTime.now();

            // Mapeamos ID Tarea -> Entrega (para saber si está entregada y cuándo)
            Map<Long, Entrega> entregasMap = todasLasEntregas.stream()
                .collect(Collectors.toMap(e -> e.getTarea().getIdActividad(), e -> e, (e1, e2) -> e1));
            
            // Mapeamos ID Examen -> (Tiene Intento?)
            Set<Long> examenesHechosSet = intentosAlumno.stream()
                .map(i -> i.getExamen().getIdActividad())
                .collect(Collectors.toSet());

            List<Map<String, Object>> listaTodasLasTareas = new ArrayList<>();
            
            // 1. Agregar Tareas
            todasLasTareas.forEach(t -> {
                 Map<String, Object> map = new HashMap<>();
                 map.put("tarea", t);
                 map.put("tipo", "TAREA");
                 Entrega entrega = entregasMap.get(t.getIdActividad());
                 boolean entregada = entrega != null;
                 map.put("entregada", entregada);
                 
                 String estado = "PENDIENTE";
                 if (entregada) {
                     estado = "COMPLETADA";
                 } else if (t.getLimiteEntrega() != null && t.getLimiteEntrega().isBefore(ahora)) {
                     estado = "ATRASADA";
                 }
                 map.put("estado", estado);
                 map.put("calificacion", entregada ? entrega.getCalificacion() : null);
                 map.put("fecha", t.getLimiteEntrega());
                 
                 listaTodasLasTareas.add(map);
            });
            
            // 2. Agregar Exámenes (Tratarlos como tareas para la lista unificada)
            todosLosExamenes.forEach(ex -> {
                Map<String, Object> map = new HashMap<>();
                // Adaptar Examen a estructura similar para la vista si es necesario, o usar campos distintos
                // Usaremos la misma estructura 'tarea' pero con el objeto examen dentro, y un flag 'isExamen'
                // Pero la vista espera `tarea.titulo`, `tarea.modulo`. Examen tiene lo mismo.
                map.put("tarea", ex); // Polimorfismo en vista: ambos son Actividad
                map.put("tipo", "EXAMEN");
                boolean realizado = examenesHechosSet.contains(ex.getIdActividad());
                map.put("entregada", realizado);
                
                String estado = "PENDIENTE";
                if (realizado) {
                    estado = "COMPLETADA";
                } else if (ex.getFechaApertura() != null && ex.getFechaApertura().isBefore(ahora.minusDays(1))) { // Asumimos vencido si pasó 1 día del inicio y no se hizo? O usar fecha fin si existe
                    // Examenes suelen tener ventana. Usaremos fechaApertura como referencia
                    estado = "PENDIENTE"; // O atrasado si hay fecha fin
                }
                map.put("estado", estado);
                
                // Buscar calificación del mejor intento
                Float notal = null;
                if(realizado) {
                     notal = intentosAlumno.stream()
                         .filter(i -> i.getExamen().getIdActividad().equals(ex.getIdActividad()))
                         .map(Intento::getCalificacion)
                         .filter(Objects::nonNull)
                         .max(Float::compare)
                         .orElse(null);
                }
                map.put("calificacion", notal);
                map.put("fecha", ex.getFechaApertura());
                
                listaTodasLasTareas.add(map);
            });
            
            // Ordenar lista unificada por fecha
            listaTodasLasTareas.sort((m1, m2) -> {
                LocalDateTime d1 = (LocalDateTime) m1.get("fecha");
                LocalDateTime d2 = (LocalDateTime) m2.get("fecha");
                if(d1 == null) return 1;
                if(d2 == null) return -1;
                return d2.compareTo(d1); // Descendente (más nuevos primero)
            });

            // Definir proximasEntregas y proximosExamenes para evitar errores de compilacion
            List<Tarea> proximasEntregas = todasLasTareas.stream()
                .filter(t -> t.getLimiteEntrega() != null && t.getLimiteEntrega().isAfter(ahora))
                .sorted(Comparator.comparing(Tarea::getLimiteEntrega))
                .collect(Collectors.toList());

            List<Examen> proximosExamenes = todosLosExamenes.stream()
                .filter(e -> e.getFechaApertura() != null && e.getFechaApertura().isAfter(ahora))
                .sorted(Comparator.comparing(Examen::getFechaApertura))
                .limit(5)
                .collect(Collectors.toList());
            
            // Pasamos la lista completa de tareas al modelo
            model.addAttribute("listaTareas", listaTodasLasTareas);
            model.addAttribute("tareasPendientes", proximasEntregas); // For compatibility if view uses it
            
            // Mantener compatibilidad con contadores
            model.addAttribute("cursosActivos", totalCursosActivos);
            model.addAttribute("tareasCompletadas", tareasCompletadas + examenesCompletados);
            model.addAttribute("totalTareas", todasLasTareas.size() + todosLosExamenes.size());
            model.addAttribute("cursosCompletados", cursosCompletados);
            model.addAttribute("proximasEntregas", proximasEntregas);
            model.addAttribute("proximosExamenes", proximosExamenes);
            model.addAttribute("tareasPendientes", proximasEntregas); // Usar la misma lista para ambas secciones

            // Alerta de próximos pagos
            LocalDate hoy = LocalDate.now();
            // Filtrar cuotas del alumno que vencen en los proximos 5 dias
            java.util.List<Cuota> cuotasProximas = cuotaRepository.findByUsuarioId(alumno.getId()).stream()
                .filter(c -> c.getEstado() == EstadoCuota.PENDIENTE)
                .filter(c -> c.getFechaVencimiento() != null)
                .filter(c -> !c.getFechaVencimiento().isBefore(hoy)) 
                .filter(c -> c.getFechaVencimiento().isBefore(hoy.plusDays(6))) // < hoy+6 means <= hoy+5
                .collect(Collectors.toList());
            
            if (!cuotasProximas.isEmpty()) {
                model.addAttribute("alertaPagoProximo", true);
                model.addAttribute("cuotasProximasVencer", cuotasProximas);
            }

            // Eventos para el calendario
            List<Map<String, String>> events = new java.util.ArrayList<>();
            
            LocalDateTime fechaInicioCal = ahora.minusMonths(6);
            LocalDateTime fechaFinCal = ahora.plusMonths(12);
            
            // Agregamos todas las tareas al calendario
            todasLasTareas.stream()
                .filter(t -> t.getLimiteEntrega() != null && 
                             t.getLimiteEntrega().isAfter(fechaInicioCal) && 
                             t.getLimiteEntrega().isBefore(fechaFinCal))
                .forEach(t -> {
                    Map<String, String> event = new HashMap<>();
                    boolean entregada = entregasMap.containsKey(t.getIdActividad());
                    String titulo = "Entrega: " + t.getTitulo();
                    if (entregada) {
                         titulo += " (Entregada)";
                    }
                    
                    event.put("title", titulo);
                    event.put("start", t.getLimiteEntrega().toLocalDate().toString());
                    event.put("className", entregada ? "event-entrega completed" : "event-entrega");
                    events.add(event);
                });

            // Agregamos Exámenes del Calendario (Pasados y Futuros)
            // Usamos la lista ya cargada 'todosLosExamenes'
            todosLosExamenes.stream()
                .filter(ex -> ex.getFechaApertura() != null && 
                              ex.getFechaApertura().isAfter(fechaInicioCal) && 
                              ex.getFechaApertura().isBefore(fechaFinCal))
                .forEach(ex -> {
                    Map<String, String> event = new HashMap<>();
                    event.put("title", "Examen: " + ex.getTitulo());
                    event.put("start", ex.getFechaApertura().toLocalDate().toString());
                    event.put("className", "event-examen");
                    events.add(event);
                });

            // Clases (Pasadas y Futuras)
            List<Long> cursosIds = ofertasActivas.stream().map(OfertaAcademica::getIdOferta).collect(Collectors.toList());
            List<Clase> clasesCalendario = new java.util.ArrayList<>();
            if (!cursosIds.isEmpty()) {
                 clasesCalendario = claseRepository.findClasesCalendario(cursosIds, fechaInicioCal, fechaFinCal);
            }
            
            clasesCalendario.forEach(c -> {
                Map<String, String> event = new HashMap<>();
                String nombreCurso = "Clase";
                if(c.getModulo() != null && c.getModulo().getCurso() != null) {
                    nombreCurso = c.getModulo().getCurso().getNombre();
                } else if(c.getCurso() != null) {
                    nombreCurso = c.getCurso().getNombre();
                }

                event.put("title", "Clase: " + c.getTitulo() + " (" + nombreCurso + ")");
                event.put("start", c.getInicio().toLocalDate().toString());
                event.put("className", "event-clase");
                events.add(event);
            });
            
            // Limitamos a 5 para el panel (Solo las futuras)
            List<Clase> proximasClasesPanel = clasesCalendario.stream()
                    .filter(c -> c.getInicio().isAfter(ahora))
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("proximasClases", proximasClasesPanel);

            model.addAttribute("eventsJson", objectMapper.writeValueAsString(events));
            
            return "alumno/mi-espacio";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mi-espacio: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar tu espacio");
            return "redirect:/";
        }
    }
    
    @PostMapping("/mis-pagos/cuotas/{cuotaId}/pagar")
    public String pagarCuota(@PathVariable Long cuotaId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String dni = principal.getName();
            System.out.println("💳 Iniciando pago de cuota ID: " + cuotaId + " para usuario: " + dni);

            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

                Long cuotaIdSeguro = Objects.requireNonNull(cuotaId, "cuotaId no puede ser nulo");

                Cuota cuota = cuotaRepository.findById(cuotaIdSeguro)
                    .orElseThrow(() -> new RuntimeException("Cuota no encontrada"));
            
            System.out.println("📋 Cuota encontrada: Número " + cuota.getNumeroCuota() + 
                             ", Estado: " + cuota.getEstado() + 
                             ", Monto: $" + cuota.getMonto());

            if (cuota.getInscripcion() == null || cuota.getInscripcion().getAlumno() == null
                    || !cuota.getInscripcion().getAlumno().getId().equals(alumno.getId())) {
                System.err.println("❌ Validación fallida: No tienes acceso a esta cuota");
                redirectAttributes.addFlashAttribute("error", "No tienes acceso a esta cuota");
                return "redirect:/alumno/mis-pagos";
            }

            if (cuota.getInscripcion().getOferta() == null) {
                System.err.println("❌ Validación fallida: La cuota no tiene oferta asociada");
                redirectAttributes.addFlashAttribute("error", "Error: La cuota no tiene oferta asociada");
                return "redirect:/alumno/mis-pagos";
            }

            EstadoOferta estadoOferta = cuota.getInscripcion().getOferta().getEstado();
            if (estadoOferta != EstadoOferta.ACTIVA && estadoOferta != EstadoOferta.ENCURSO) {
                System.err.println("❌ Validación fallida: La oferta no está activa ni en curso. Estado: " + estadoOferta);
                redirectAttributes.addFlashAttribute("error", "La oferta académica asociada no está disponible para pagos");
                return "redirect:/alumno/mis-pagos";
            }

            BigDecimal saldoPendiente = calcularSaldoPendiente(cuota);
            System.out.println("💰 Saldo pendiente calculado: $" + saldoPendiente);
            
            if (saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("⚠️ Saldo pendiente es 0 o negativo");
                redirectAttributes.addFlashAttribute("success", "Esta cuota ya está al día");
                return "redirect:/alumno/mis-pagos";
            }

            System.out.println("🔨 Construyendo request para MercadoPago...");
            ReferenceRequest request = construirReferenciaCuota(cuota, alumno, saldoPendiente);

            System.out.println("📤 Llamando a createPreferenceForCuota...");
            ResponseDTO response = mercadoPagoService.createPreferenceForCuota(
                    request,
                    alumno,
                    cuota.getInscripcion().getOferta(),
                    cuota);

            System.out.println("✅ Preferencia creada para cuota: " + response.preferenceId());
            System.out.println("🔗 URL de redirección: " + response.redirectUrl());
            return "redirect:" + response.redirectUrl();

        } catch (Exception e) {
            System.err.println("❌ Error al iniciar pago de cuota: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "No pudimos iniciar el pago. Intenta nuevamente.");
            return "redirect:/alumno/mis-pagos";
        }
    }

    // Mis Pagos
    @GetMapping("/mis-pagos")
    public String misPagos(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

            List<Inscripciones> inscripcionesAlumno = inscripcionRepository.findByAlumnoDni(dni);
            for (Inscripciones inscripcion : inscripcionesAlumno) {
                if (inscripcion.getIdInscripcion() != null) {
                    // Generar la primera cuota si no existe
                    if (cuotaRepository.findByInscripcionIdInscripcion(inscripcion.getIdInscripcion()).isEmpty()) {
                        mercadoPagoService.generarCuotasParaInscripcion(inscripcion);
                    } else {
                        // Verificar si corresponde generar la siguiente cuota
                        mercadoPagoService.generarSiguienteCuotaSiCorresponde(inscripcion);
                    }
                }
            }
            
            // Obtener SOLO pagos COMPLETADOS (inscripciones y cuotas pagadas)
            List<Pago> pagosRealizados = pagoRepository.findByUsuarioId(alumno.getId()).stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.COMPLETADO)
                .sorted(Comparator.comparing(this::obtenerFechaReferencia,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

            BigDecimal totalPagado = pagosRealizados.stream()
                .map(Pago::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            LocalDate hoy = LocalDate.now();

            // Obtener todas las cuotas del alumno
            List<Cuota> todasLasCuotas = cuotaRepository.findByUsuarioId(alumno.getId()).stream()
                .sorted(Comparator.comparing(Cuota::getFechaVencimiento,
                    Comparator.nullsLast(LocalDate::compareTo)))
                .collect(Collectors.toList());

            // Actualizar estados de cuotas vencidas y aplicar MORA
            todasLasCuotas.forEach(cuota -> {
                if (cuota.getEstado() == EstadoCuota.PENDIENTE && 
                    cuota.getFechaVencimiento() != null && 
                    cuota.getFechaVencimiento().isBefore(hoy)) {
                    
                    cuota.setEstado(EstadoCuota.VENCIDA);
                    
                    // Aplicar recargo por mora si existe en la oferta
                    if (cuota.getInscripcion() != null && cuota.getInscripcion().getOferta() != null) {
                        Double recargo = cuota.getInscripcion().getOferta().getRecargoMora();
                        if (recargo != null && recargo > 0) {
                            BigDecimal recargoBD = BigDecimal.valueOf(recargo);
                            // Sumar recargo al monto actual
                            cuota.setMonto(cuota.getMonto().add(recargoBD));
                        }
                    }
                    
                    cuotaRepository.save(cuota);
                }
            });

            // Filtrar cuotas visibles según lógica de fechas
            // Regla: Mostrar cuota 1. Mostrar cuota N solo si hoy > fechaVencimiento(N-1).
            List<Cuota> cuotasVisibles = new java.util.ArrayList<>();
            if (!todasLasCuotas.isEmpty()) {
                // Agrupar cuotas por inscripción para aplicar la lógica por curso
                Map<Long, List<Cuota>> cuotasPorInscripcion = todasLasCuotas.stream()
                    .filter(c -> c.getInscripcion() != null)
                    .collect(Collectors.groupingBy(c -> c.getInscripcion().getIdInscripcion()));

                for (List<Cuota> cuotasCurso : cuotasPorInscripcion.values()) {
                    // Ordenar por fecha de vencimiento
                    cuotasCurso.sort(Comparator.comparing(Cuota::getFechaVencimiento));
                    
                    for (int i = 0; i < cuotasCurso.size(); i++) {
                        Cuota actual = cuotasCurso.get(i);
                        if (i == 0) {
                            // La primera cuota siempre es visible
                            cuotasVisibles.add(actual);
                        } else {
                            // Cuotas subsiguientes: visibles solo si la anterior ya venció Y está pagada
                            Cuota anterior = cuotasCurso.get(i - 1);
                            boolean anteriorPagada = anterior.getEstado() == EstadoCuota.PAGADA;
                            boolean fechaVencida = anterior.getFechaVencimiento() != null && hoy.isAfter(anterior.getFechaVencimiento());
                            
                            if (anteriorPagada && fechaVencida) {
                                cuotasVisibles.add(actual);
                            }
                        }
                    }
                }
            }
            
            // Ordenar cuotas visibles para la vista
            cuotasVisibles.sort(Comparator.comparing(Cuota::getFechaVencimiento, Comparator.nullsLast(LocalDate::compareTo)));

            // Filtrar solo cuotas NO pagadas de las visibles
            List<Cuota> cuotasActivas = cuotasVisibles.stream()
                .filter(c -> c.getEstado() != EstadoCuota.PAGADA && c.getEstado() != EstadoCuota.CANCELADA)
                .collect(Collectors.toList());

            // Encontrar la primera cuota pendiente POR INSCRIPCIÓN para mostrar el botón de pagar
            Map<Long, Cuota> cuotasPendientesPorInscripcion = new java.util.HashMap<>();
            for (Cuota cuota : cuotasActivas) {
                if ((cuota.getEstado() == EstadoCuota.PENDIENTE || cuota.getEstado() == EstadoCuota.VENCIDA) 
                    && cuota.getInscripcion() != null) {
                    Long inscripcionId = cuota.getInscripcion().getIdInscripcion();
                    // Si no hay cuota para esta inscripción o es anterior, guardar
                    if (!cuotasPendientesPorInscripcion.containsKey(inscripcionId) ||
                        cuota.getFechaVencimiento().isBefore(cuotasPendientesPorInscripcion.get(inscripcionId).getFechaVencimiento())) {
                        cuotasPendientesPorInscripcion.put(inscripcionId, cuota);
                    }
                }
            }

            // Para retrocompatibilidad, obtener la primera cuota pendiente global
            Cuota cuotaPendiente = cuotasActivas.stream()
                .filter(c -> c.getEstado() == EstadoCuota.PENDIENTE || c.getEstado() == EstadoCuota.VENCIDA)
                .min(Comparator.comparing(Cuota::getFechaVencimiento, Comparator.nullsLast(LocalDate::compareTo)))
                .orElse(null);

            BigDecimal montoCuotaPendiente = cuotaPendiente != null
                ? calcularSaldoPendiente(cuotaPendiente)
                : BigDecimal.ZERO;

            BigDecimal totalPendiente = cuotasActivas.stream()
                .map(this::calcularSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            String proximoPagoTexto = cuotaPendiente != null 
                ? String.format("%s - %s",
                    formatearFecha(cuotaPendiente.getFechaVencimiento()),
                    formatearMoneda(calcularSaldoPendiente(cuotaPendiente)))
                : "Sin cuotas pendientes";

            model.addAttribute("alumno", alumno);
            model.addAttribute("pagos", pagosRealizados);
            model.addAttribute("cuotas", cuotasVisibles); // Usar cuotasVisibles en lugar de todasLasCuotas
            model.addAttribute("cuotaPendiente", cuotaPendiente);
            model.addAttribute("cuotasPendientesPorInscripcion", cuotasPendientesPorInscripcion);
            model.addAttribute("montoCuotaPendienteTexto", formatearMoneda(montoCuotaPendiente));
            model.addAttribute("totalPagadoTexto", formatearMoneda(totalPagado));
            model.addAttribute("totalPendienteTexto", formatearMoneda(totalPendiente));
            model.addAttribute("proximoPagoTexto", proximoPagoTexto);

            return "alumno/mis-pagos";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mis-pagos: " + e.getMessage());
            model.addAttribute("error", "Error al cargar tus pagos");
            return "redirect:/";
        }
    }

    @GetMapping("/mis-pagos/comprobante/{pagoId}")
    public ResponseEntity<ByteArrayResource> descargarComprobante(@PathVariable Long pagoId, Principal principal) {
        try {
            String dni = principal.getName();
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

            Pago pago = pagoRepository.findById(pagoId)
                    .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

            if (!pago.getUsuario().getId().equals(alumno.getId())) {
                return ResponseEntity.status(403).build();
            }

            java.io.ByteArrayInputStream pdfStream = reporteService.generarComprobantePagoPDF(pago);
            byte[] data = pdfStream.readAllBytes();
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=Comprobante_" + pago.getIdPago() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(data.length)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private BigDecimal calcularSaldoPendiente(Cuota cuota) {
        BigDecimal monto = Optional.ofNullable(cuota.getMonto()).orElse(BigDecimal.ZERO);
        BigDecimal pagado = Optional.ofNullable(cuota.getMontoPagado()).orElse(BigDecimal.ZERO);
        BigDecimal saldo = monto.subtract(pagado);
        return saldo.compareTo(BigDecimal.ZERO) > 0 ? saldo : BigDecimal.ZERO;
    }

    private LocalDateTime obtenerFechaReferencia(Pago pago) {
        if (pago == null) {
            return null;
        }
        return Optional.ofNullable(pago.getFechaAprobacion())
                .orElse(pago.getFechaPago());
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(LOCALE_ES_AR);
        return formatter.format(monto);
    }

    private String formatearFecha(LocalDate fecha) {
        if (fecha == null) {
            return "Sin fecha";
        }
        return FORMATO_FECHA_RESUMEN.format(fecha);
    }

    private int obtenerMaxDiasMora(Inscripciones inscripcion) {
        try {
            List<Cuota> cuotas = cuotaRepository.findByInscripcionIdInscripcion(inscripcion.getIdInscripcion());
            if (cuotas == null || cuotas.isEmpty()) {
                return 0;
            }
            LocalDate hoy = LocalDate.now();
            return cuotas.stream()
                    .filter(c -> c.getFechaVencimiento() != null)
                    .filter(c -> c.getEstado() != EstadoCuota.PAGADA)
                    .mapToInt(c -> (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), hoy))
                    .filter(dias -> dias > 0)
                    .max()
                    .orElse(0);
        } catch (Exception e) {
            System.out.println("⚠️ No se pudo calcular días de mora: " + e.getMessage());
            return 0;
        }
    }

    private ReferenceRequest construirReferenciaCuota(Cuota cuota, Usuario alumno, BigDecimal montoPendiente) {
        ReferenceRequest.PayerDTO payer = new ReferenceRequest.PayerDTO(
                alumno.getNombre() + " " + alumno.getApellido(),
                alumno.getCorreo());

        ReferenceRequest.BackUrlsDTO backUrls = new ReferenceRequest.BackUrlsDTO(
                baseUrl + "/pago/success",
                baseUrl + "/pago/failure",
                baseUrl + "/pago/pending");

        String titulo = String.format("Cuota %s - %s",
                cuota.getNumeroCuota() != null ? cuota.getNumeroCuota() : "",
                cuota.getInscripcion().getOferta().getNombre()).trim();

        ReferenceRequest.ItemDTO item = new ReferenceRequest.ItemDTO(
                "CUOTA-" + cuota.getIdCuota(),
                titulo,
                montoPendiente,
                1);

        return new ReferenceRequest(
                alumno.getId(),
                montoPendiente,
                payer,
                backUrls,
                List.of(item));
    }

    // Inscribirse a una oferta académica - Inscripción directa (sin pago)
    @PostMapping("/inscribirse/{ofertaId}")
    public String inscribirseAOferta(@PathVariable Long ofertaId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String dni = authentication.getName();
            Long ofertaIdSeguro = Objects.requireNonNull(ofertaId, "ofertaId no puede ser nulo");
            System.out.println("📝 Iniciando proceso de inscripción directa para oferta: " + ofertaIdSeguro);
            
            // Buscar el usuario (alumno)
            Usuario usuario = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Buscar la oferta
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaIdSeguro)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            System.out.println("📚 Oferta encontrada: " + oferta.getNombre() + " (Tipo: " + oferta.getClass().getSimpleName() + ")");

            // Verificar si ya está inscrito
            List<Inscripciones> inscripcionesExistentes = inscripcionRepository.findByAlumnoDni(dni);
            boolean yaInscritoActivo = inscripcionesExistentes.stream()
                    .filter(ins -> ins.getOferta().getIdOferta().equals(ofertaIdSeguro))
                    .anyMatch(ins -> Boolean.TRUE.equals(ins.getEstadoInscripcion()));
            
            if (yaInscritoActivo) {
                redirectAttributes.addFlashAttribute("error", "Ya estás inscrito en esta oferta");
                return "redirect:/publico";
            }
            
            // ✅ Verificar si las inscripciones están abiertas
            if (!oferta.getInscripcionesAbiertas()) {
                String mensaje;
                String estadoInscripcion = oferta.getEstadoInscripcion();
                
                // Si son próximas, mostrar modal de notificación
                if ("Próximamente".equals(estadoInscripcion)) {
                    redirectAttributes.addFlashAttribute("inscripcionProxima", true);
                    redirectAttributes.addFlashAttribute("ofertaProxima", oferta);
                    return "redirect:/publico";
                } else {
                    // Si ya cerraron, mostrar mensaje de error
                    mensaje = "Las inscripciones para '" + oferta.getNombre() + "' ya han cerrado. " +
                             "Finalizaron el " + oferta.getFechaFinInscripcion().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    redirectAttributes.addFlashAttribute("error", mensaje);
                    return "redirect:/publico";
                }
            }
            
            // ✅ Verificar cupos disponibles (Validación consistente con Frontend)
            // Usamos los datos de la entidad cargada para asegurar consistencia con lo que ve el usuario
            long inscritosReales = oferta.getCantidadAlumnosActivos();
            int cuposTotales = oferta.getCupos() != null ? oferta.getCupos() : Integer.MAX_VALUE;
            
            System.out.println("📊 Verificando cupos: " + inscritosReales + " inscritos de " + cuposTotales + " totales.");

            if (inscritosReales >= cuposTotales) {
                 System.out.println("❌ Cupos agotados. Redirigiendo con errorCupo.");
                 // Buscar recomendaciones (IA Simulada): Misma Categoría o Nombre similar, y ACTIVA con cupos
                List<OfertaAcademica> recomendaciones = ofertaAcademicaRepository.findAll().stream()
                    .filter(o -> !o.getIdOferta().equals(ofertaIdSeguro)) // Excluir actual
                    .filter(o -> o.getEstado() == EstadoOferta.ACTIVA) // Solo activas
                    .filter(o -> { // Validar cupos de recomendación
                         long ocupadosRec = inscripcionRepository.countByOfertaAndEstadoInscripcionTrue(o);
                         int cuposRec = o.getCupos() != null ? o.getCupos() : Integer.MAX_VALUE;
                         return ocupadosRec < cuposRec;
                    })
                    .sorted((o1, o2) -> {
                        // Prioridad 1: Misma Categoría
                        boolean cat1 = !Collections.disjoint(o1.getCategorias(), oferta.getCategorias());
                        boolean cat2 = !Collections.disjoint(o2.getCategorias(), oferta.getCategorias());
                        if (cat1 && !cat2) return -1;
                        if (!cat1 && cat2) return 1;
                        
                        // Prioridad 2: Similitud Nombre simple
                         boolean nombre1 = o1.getNombre().contains(oferta.getNombre().split(" ")[0]);
                         boolean nombre2 = o2.getNombre().contains(oferta.getNombre().split(" ")[0]);
                         if (nombre1 && !nombre2) return -1;
                         if (!nombre1 && nombre2) return 1;

                        return 0;
                    })
                    .limit(3)
                    .collect(Collectors.toList());

                // Actualizar atributos flash para el modal y la alerta
                redirectAttributes.addFlashAttribute("errorCupo", true);
                redirectAttributes.addFlashAttribute("ofertaLlena", oferta);
                redirectAttributes.addFlashAttribute("recomendaciones", recomendaciones);
                
                // Mensaje de error explícito para la alerta visual (fallback si falla el modal)
                redirectAttributes.addFlashAttribute("error", "Lo sentimos, el cupo para '" + oferta.getNombre() + "' se ha completado. Hemos seleccionado algunas recomendaciones para ti.");
                
                return "redirect:/publico";
            }

            // ✅ VERIFICAR SI LA OFERTA TIENE COSTO (Integración MercadoPago)
            if (oferta.getCostoInscripcion() != null && oferta.getCostoInscripcion() > 0) {
                System.out.println("💵 Oferta con costo: $" + oferta.getCostoInscripcion() + ", redirigiendo a MercadoPago");
                
                try {
                    // Crear el request para MercadoPago
                    ReferenceRequest.ItemDTO item = new ReferenceRequest.ItemDTO(
                            oferta.getIdOferta().toString(),
                            oferta.getNombre(),
                            BigDecimal.valueOf(oferta.getCostoInscripcion()),
                            1);
    
                    List<ReferenceRequest.ItemDTO> items = new ArrayList<>();
                    items.add(item);
                    
                    // Crear el objeto PayerDTO
                    ReferenceRequest.PayerDTO payer = new ReferenceRequest.PayerDTO(
                            usuario.getNombre() + " " + usuario.getApellido(),
                            usuario.getCorreo());
    
                    // Crear el objeto BackUrlsDTO
                    ReferenceRequest.BackUrlsDTO backUrls = new ReferenceRequest.BackUrlsDTO(
                            baseUrl + "/pago/success",
                            baseUrl + "/pago/failure",
                            baseUrl + "/pago/pending");
    
                    // Crear el objeto ReferenceRequest
                    ReferenceRequest request = new ReferenceRequest(
                            usuario.getId(),
                            BigDecimal.valueOf(oferta.getCostoInscripcion()),
                            payer,
                            backUrls,
                            items);
    
                    // Crear preferencia con el usuario y oferta
                    ResponseDTO response = mercadoPagoService.createPreference(request, usuario, oferta);
    
                    System.out.println("✅ Preferencia creada: " + response.preferenceId());
                    return "redirect:" + response.redirectUrl();
                    
                } catch (Exception e) {
                    System.err.println("❌ Error con MercadoPago: " + e.getMessage());
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("error", "Error al iniciar el pago con MercadoPago.");
                    return "redirect:/publico";
                }
            }

            // ✅ OFERTA GRATUITA: CREAR INSCRIPCIÓN DIRECTA usando Usuario
            Inscripciones nuevaInscripcion = new Inscripciones();
            nuevaInscripcion.setAlumno(usuario); // ✅ Ahora acepta Usuario
            nuevaInscripcion.setOferta(oferta);
            nuevaInscripcion.setEstadoInscripcion(true); // Inscripción activa
            nuevaInscripcion.setFechaInscripcion(LocalDate.now());
            
            inscripcionRepository.save(nuevaInscripcion);
            mercadoPagoService.generarCuotasParaInscripcion(nuevaInscripcion);
            
            System.out.println("✅ Inscripción creada exitosamente para " + usuario.getNombre());
            
            redirectAttributes.addFlashAttribute("success", 
                "¡Te has inscrito exitosamente a " + oferta.getNombre() + "!");
            
            return "redirect:/alumno/aula/" + oferta.getIdOferta();
            
        } catch (Exception e) {
            System.err.println("❌ Error al crear inscripción: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "Error al procesar la inscripción: " + e.getMessage());
            return "redirect:/publico";
        }
    }

    // ✅ CU-08: Darse de baja de una oferta académica
    @PostMapping("/darse-de-baja/{inscripcionId}")
    public String darseDeBaja(@PathVariable Long inscripcionId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            String dni = principal.getName();
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

            Inscripciones inscripcion = inscripcionRepository.findById(inscripcionId)
                    .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

            // 1. Validar pertenencia
            if (!inscripcion.getAlumno().getId().equals(alumno.getId())) {
                redirectAttributes.addFlashAttribute("error", "No tienes permiso para realizar esta acción.");
                return "redirect:/alumno/mis-ofertas";
            }

            // 2. Realizar baja lógica
            inscripcion.setEstadoInscripcion(false);
            inscripcionRepository.save(inscripcion);

            // 3. Notificación por Email (Requisito del CU)
            try {
                if (alumno.getCorreo() != null && !alumno.getCorreo().isEmpty()) {
                    String subject = "Confirmación de baja - Aurea";
                    String body = "Hola " + alumno.getNombre() + ",<br><br>" +
                            "Confirmamos que te has dado de baja de la oferta académica: <strong>" + inscripcion.getOferta().getNombre() + "</strong>.<br>" +
                            "Fecha de baja: " + LocalDate.now() + "<br><br>" +
                            "Si deseas retomar el curso en el futuro, deberás inscribirte nuevamente.<br><br>" +
                            "Saludos,<br>Equipo Aurea.";
                    emailService.sendEmail(alumno.getCorreo(), subject, body);
                }
            } catch (Exception mailEx) {
                System.out.println("⚠️ No se pudo enviar email de baja: " + mailEx.getMessage());
            }

            redirectAttributes.addFlashAttribute("success", "Te has dado de baja correctamente de: " + inscripcion.getOferta().getNombre());

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al procesar la baja: " + e.getMessage());
        }
        return "redirect:/alumno/mis-ofertas";
    }

    // Ver mis ofertas académicas (inscripciones)
    @GetMapping("/mis-ofertas")
    public String misOfertasAcademicas(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            System.out.println("🎓 Alumno accediendo a mis ofertas: " + dni);
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            
            // ✅ Buscar inscripciones del alumno
            List<Inscripciones> todasInscripciones = inscripcionRepository.findByAlumnoDni(dni);

            // ✅ FILTRAR SOLO LAS ACTIVAS (estadoInscripcion = true)
            List<Inscripciones> inscripcionesActivas = todasInscripciones.stream()
                    .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                    .collect(Collectors.toList());

            // Dividir en "En Curso" y "Finalizadas" / "Terminadas"
            List<Inscripciones> inscripcionesEnCurso = new ArrayList<>();
            List<Inscripciones> inscripcionesFinalizadas = new ArrayList<>();

            for (Inscripciones insc : inscripcionesActivas) {
                OfertaAcademica oferta = insc.getOferta();
                if (oferta.getEstado() == EstadoOferta.FINALIZADA || 
                   (oferta.getEstado() == EstadoOferta.DE_BAJA) ||
                   (oferta.getFechaFin() != null && oferta.getFechaFin().isBefore(LocalDate.now()))) {
                     inscripcionesFinalizadas.add(insc);
                } else {
                     inscripcionesEnCurso.add(insc);
                }
            }
            
            // ✅ Extraer TODAS las ofertas académicas (Cursos Y Formaciones)
            List<OfertaAcademica> ofertas = inscripcionesActivas.stream()
                    .map(Inscripciones::getOferta)
                    .collect(Collectors.toList());
            
            System.out.println("📊 Inscripciones activas encontradas: " + inscripcionesActivas.size());
            System.out.println("📊 Ofertas académicas: " + ofertas.size());
            
            // Debug: mostrar tipos de ofertas
            for (OfertaAcademica oferta : ofertas) {
                System.out.println("   - " + oferta.getClass().getSimpleName() + ": " + oferta.getNombre());
            }

            model.addAttribute("alumno", alumno);
            model.addAttribute("cursos", ofertas); // Mantener nombre "cursos" para compatibilidad con vista
            model.addAttribute("inscripciones", inscripcionesActivas); // Mantener compatibilidad
            model.addAttribute("inscripcionesEnCurso", inscripcionesEnCurso);
            model.addAttribute("inscripcionesFinalizadas", inscripcionesFinalizadas);
            return "misOfertasAcademicas";
            
        } catch (Exception e) {
            System.out.println("❌ Error en misOfertasAcademicas (alumno): " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar tus cursos: " + e.getMessage());
            return "misOfertasAcademicas";
        }
    }

    @GetMapping("/aula/{ofertaId}")
    public String accederAlAula(@PathVariable Long ofertaId,
                              Authentication authentication,
                              Model model) {
        try {
            String dni = authentication.getName();
            Long ofertaIdSeguro = Objects.requireNonNull(ofertaId, "ofertaId no puede ser nulo");
            System.out.println("🔍 Accediendo al aula para oferta ID: " + ofertaIdSeguro + ", usuario: " + dni);
            
                // Buscar la inscripción del alumno en esta oferta
                List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDniAndOfertaId(dni, ofertaIdSeguro);
                
                // Buscar una inscripción ACTIVA
                Inscripciones inscripcion = inscripciones.stream()
                    .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                    .findFirst()
                    .orElse(null);

                // Si no hay activa, buscar cualquiera para mostrar error adecuado o redirigir
                if (inscripcion == null) {
                    if (!inscripciones.isEmpty()) {
                         // Hay inscripciones pero ninguna activa
                         System.out.println("❌ Inscripción encontrada pero inactiva (Dada de baja)");
                         model.addAttribute("error", "Esta inscripción no está activa. Debes inscribirte nuevamente.");
                         return "misOfertasAcademicas";
                    }
                    throw new RuntimeException("Inscripción no encontrada");
                }

                Usuario alumno = inscripcion.getAlumno();
                if (alumno == null) {
                throw new RuntimeException("Alumno no asociado a la inscripción");
                }

            System.out.println("✅ Inscripción encontrada ID: " + inscripcion.getIdInscripcion());

            // Validación redundante pero segura
            if (!Boolean.TRUE.equals(inscripcion.getEstadoInscripcion())) {
                System.out.println("❌ Inscripción inactiva");
                model.addAttribute("error", "Esta inscripción no está activa");
                return "misOfertasAcademicas";
            }

            OfertaAcademica oferta = inscripcion.getOferta();
            System.out.println("📚 Oferta encontrada: " + oferta.getNombre() + ", tipo: " + oferta.getClass().getSimpleName());

            // ✅ Verificación de Pago de Inscripción (Bloquear si existe un pago asociado NO completado)
            if (inscripcion.getPagoInscripcion() != null) {
                if (inscripcion.getPagoInscripcion().getEstadoPago() != EstadoPago.COMPLETADO) {
                    System.out.println("❌ Pago de inscripción NO completado/aprobado. Estado: " + inscripcion.getPagoInscripcion().getEstadoPago());
                    model.addAttribute("error", "El pago de la inscripción no se ha completado. Por favor verifica su estado.");
                    return "misOfertasAcademicas";
                }
            }

            // Verificar bloqueo por mora total del aula
            Instituto instituto = institutoService.obtenerInstituto();
            Integer limiteMoraAula = instituto != null ? instituto.getDiasMoraBloqueoAula() : null;
            int diasMora = obtenerMaxDiasMora(inscripcion);
            
            System.out.println("🎯 Validando bloqueo total de aula:");
            System.out.println("   - Días de mora calculados: " + diasMora);
            System.out.println("   - Límite configurado (diasMoraBloqueoAula): " + limiteMoraAula);
            
            // Bloquear si excede el límite (no alcanza)
            boolean bloqueadoPorMora = limiteMoraAula != null && limiteMoraAula > 0 && diasMora > limiteMoraAula;

            System.out.println("   - ¿Bloqueado por mora? " + bloqueadoPorMora);

            if (bloqueadoPorMora) {
                System.out.println("🚫 ACCESO AL AULA BLOQUEADO - Redirigiendo a aula-bloqueada.html");
                model.addAttribute("ofertaId", ofertaId);
                model.addAttribute("diasMora", Integer.valueOf(diasMora));
                model.addAttribute("limiteMora", limiteMoraAula);
                
                // Retornar vista especial de bloqueo total
                return "aula-bloqueada";
            }
            
            System.out.println("✅ Acceso permitido - Mora dentro del límite");
            model.addAttribute("bloqueadoPorMora", Boolean.FALSE);
            
            // Si es un curso o formación, cargar módulos y contenido
            if (oferta instanceof Curso || oferta instanceof Formacion) {
                System.out.println("🎓 Es un curso/formación: " + oferta.getNombre());
                
                // Verificar permisos de modificación (solo admin o docente de la oferta)
                boolean puedeModificar = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                
                // Verificar si es docente según el tipo de oferta
                if (oferta instanceof Curso) {
                    Curso curso = (Curso) oferta;
                    puedeModificar = puedeModificar || (curso.getDocentes() != null && curso.getDocentes().stream()
                            .anyMatch(docente -> docente.getDni().equals(dni)));
                } else if (oferta instanceof Formacion) {
                    Formacion formacion = (Formacion) oferta;
                    puedeModificar = puedeModificar || (formacion.getDocentes() != null && formacion.getDocentes().stream()
                            .anyMatch(docente -> docente.getDni().equals(dni)));
                }
                
                // Cargar módulos: todos para docentes/admin, solo visibles para alumnos
                List<Modulo> modulos;
                if (puedeModificar) {
                    System.out.println("🔓 Usuario con permisos de modificación - cargando TODOS los módulos");
                    modulos = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(oferta);
                } else {
                    System.out.println("👤 Usuario alumno - cargando SOLO módulos visibles");
                    modulos = moduloRepository.findByCursoAndVisibilidadTrueOrderByFechaInicioModuloAsc(oferta);
                }
                
                System.out.println("📦 Módulos encontrados: " + modulos.size());
                for (Modulo m : modulos) {
                    System.out.println("  - " + m.getNombre() + " (visible: " + m.getVisibilidad() + ")");
                }
                
                Map<Long, Map<String, Object>> examenesResumen = null;
                Map<Long, Entrega> tareasEntregas = new HashMap<>(); // Mapa para las entregas

                if (!puedeModificar && modulos != null) {
                    for (Modulo moduloActual : modulos) {
                        if (moduloActual.getActividades() != null) {
                            moduloActual.setActividades(moduloActual.getActividades().stream()
                                    .filter(a -> Boolean.TRUE.equals(a.getVisibilidad()))
                                    .collect(Collectors.toList()));
                        }
                    }

                    examenesResumen = new HashMap<>();
                    for (Modulo moduloActual : modulos) {
                        if (moduloActual.getActividades() == null) {
                            continue;
                        }
                        for (var actividad : moduloActual.getActividades()) {
                            if (actividad instanceof Examen examenActividad) {
                                List<Intento> intentosAlumno = intentoRepository
                                        .findByAlumno_IdAndExamen_IdActividad(alumno.getId(), examenActividad.getIdActividad());

                                intentosAlumno.sort(Comparator
                                        .comparing(Intento::getFechaFin, Comparator.nullsLast(Comparator.naturalOrder()))
                                    .reversed());

                                Intento ultimoIntento = intentosAlumno.isEmpty() ? null : intentosAlumno.get(0);
                                Integer intentosPermitidos = examenActividad.getCantidadIntentos();
                                int intentosRealizados = intentosAlumno.size();
                                boolean puedeReintentar = intentosPermitidos == null || intentosRealizados < intentosPermitidos;

                                Float calificacionValor = ultimoIntento != null ? ultimoIntento.getCalificacion() : null;
                                String calificacionTexto = calificacionValor != null
                                        ? String.format(LOCALE_ES_AR, "%.2f", calificacionValor)
                                        : null;

                                EstadoIntento ultimoEstado = ultimoIntento != null ? ultimoIntento.getEstado() : null;
                                boolean requiereRevision = ultimoEstado == EstadoIntento.PENDIENTE_CORRECCION;

                                Map<String, Object> resumen = new HashMap<>();
                                resumen.put("intentosRealizados", intentosRealizados);
                                resumen.put("intentosPermitidos", intentosPermitidos);
                                resumen.put("puedeReintentar", puedeReintentar);
                                resumen.put("ultimaCalificacion", calificacionValor);
                                resumen.put("calificacionTexto", calificacionTexto);
                                resumen.put("ultimoEstado", ultimoEstado);
                                resumen.put("requiereRevision", requiereRevision);

                                examenesResumen.put(examenActividad.getIdActividad(), resumen);
                            } else if (actividad instanceof Tarea tarea) {
                                // Buscar entrega para la tarea
                                entregaRepository.findByTareaAndEstudiante(tarea, alumno)
                                    .ifPresent(entrega -> tareasEntregas.put(tarea.getIdActividad(), entrega));
                            }
                        }
                    }
                }

                model.addAttribute("curso", oferta); // Mantener nombre "curso" para compatibilidad
                model.addAttribute("modulos", modulos);
                model.addAttribute("examenIntentos", examenesResumen != null ? examenesResumen : Collections.emptyMap());
                model.addAttribute("tareasEntregas", tareasEntregas);
                model.addAttribute("inscripcion", inscripcion);
                model.addAttribute("puedeModificar", puedeModificar);
                System.out.println("👤 Puede modificar: " + puedeModificar);
                
                // Calcular progreso
                try {
                    Double progreso = ofertaAcademicaService.calcularProgreso(oferta.getIdOferta(), alumno);
                    model.addAttribute("progreso", progreso);
                    System.out.println("📊 Progreso calculado: " + progreso + "%");
                } catch (Exception e) {
                    System.out.println("⚠️ Error calculando progreso: " + e.getMessage());
                    model.addAttribute("progreso", 0.0);
                }

                model.addAttribute("avisosRecientes", construirAvisosAlumno(
                    oferta,
                    alumno,
                    modulos,
                    tareasEntregas,
                    examenesResumen
                ));
                
                System.out.println("✅ Redirigiendo a template: aula");
                return "aula"; 
                
            } else {
                // Para otros tipos de ofertas académicas
                System.out.println("ℹ️  No es un curso, redirigiendo a aula general");
                model.addAttribute("oferta", oferta);
                model.addAttribute("inscripcion", inscripcion);
                return "aula/general"; // Esto buscaría templates/aula/general.html
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error al acceder al aula: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al acceder al aula: " + e.getMessage());
            return "misOfertasAcademicas";
        }
    }
    
    private List<Map<String, Object>> construirAvisosAlumno(OfertaAcademica oferta,
                                                            Usuario alumno,
                                                            List<Modulo> modulos,
                                                            Map<Long, Entrega> tareasEntregas,
                                                            Map<Long, Map<String, Object>> examenesResumen) {
        List<Map<String, Object>> avisos = new ArrayList<>();
        if (oferta == null || oferta.getIdOferta() == null) {
            return avisos;
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime haceUnaSemana = ahora.minusDays(7);
        LocalDateTime enTresDias = ahora.plusDays(3);

        List<Tarea> tareas = new ArrayList<>();
        List<Examen> examenes = new ArrayList<>();

        if (modulos != null) {
            for (Modulo modulo : modulos) {
                if (modulo.getActividades() == null) {
                    continue;
                }
                for (var actividad : modulo.getActividades()) {
                    if (actividad instanceof Tarea tarea) {
                        tareas.add(tarea);
                    } else if (actividad instanceof Examen examen) {
                        examenes.add(examen);
                    }
                }
            }
        }

        for (Tarea tarea : tareas) {
            LocalDateTime creada = tarea.getFechaCreacion();
            if (creada != null && creada.isAfter(haceUnaSemana)) {
                avisos.add(crearAviso("Nueva tarea: " + tarea.getTitulo(), creada, "info"));
            }

            LocalDateTime limite = tarea.getLimiteEntrega();
            if (limite != null && limite.isAfter(ahora) && limite.isBefore(enTresDias)) {
                boolean entregada = tareasEntregas != null && tareasEntregas.containsKey(tarea.getIdActividad());
                if (!entregada) {
                    avisos.add(crearAviso("Entrega próxima: " + tarea.getTitulo(), limite, "warning"));
                }
            }
        }

        for (Examen examen : examenes) {
            LocalDateTime apertura = examen.getFechaApertura();
            if (apertura != null && apertura.isAfter(haceUnaSemana)) {
                avisos.add(crearAviso("Nuevo examen: " + examen.getTitulo(), apertura, "info"));
            }

            if (apertura != null && !apertura.isAfter(ahora)) {
                LocalDateTime cierre = examen.getFechaCierre();
                boolean abierto = cierre == null || cierre.isAfter(ahora);
                if (abierto) {
                    int intentosRealizados = 0;
                    boolean puedeReintentar = true;
                    if (examenesResumen != null) {
                        Map<String, Object> resumen = examenesResumen.get(examen.getIdActividad());
                        if (resumen != null) {
                            Object intentosVal = resumen.get("intentosRealizados");
                            if (intentosVal instanceof Integer) {
                                intentosRealizados = (Integer) intentosVal;
                            }
                            Object puedeVal = resumen.get("puedeReintentar");
                            if (puedeVal instanceof Boolean) {
                                puedeReintentar = (Boolean) puedeVal;
                            }
                        }
                    }
                    if (intentosRealizados == 0) {
                        avisos.add(crearAviso("Examen abierto: " + examen.getTitulo(), apertura, "success"));
                    } else if (puedeReintentar) {
                        avisos.add(crearAviso("Reintento disponible: " + examen.getTitulo(), apertura, "success"));
                    }
                }
            }
        }

        List<Clase> clases = claseRepository.findByModuloCursoIdOferta(oferta.getIdOferta());
        for (Clase clase : clases) {
            LocalDateTime inicio = clase.getInicio();
            if (inicio != null && inicio.isAfter(ahora) && inicio.isBefore(enTresDias)) {
                avisos.add(crearAviso("Clase próxima: " + clase.getTitulo(), inicio, "info"));
            }
        }

        avisos.sort(Comparator.comparing(
                (Map<String, Object> a) -> (LocalDateTime) a.get("fecha"),
                Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed());

        if (avisos.size() > 5) {
            return new ArrayList<>(avisos.subList(0, 5));
        }
        return avisos;
    }

    private Map<String, Object> crearAviso(String titulo, LocalDateTime fecha, String tipo) {
        Map<String, Object> aviso = new HashMap<>();
        aviso.put("titulo", titulo);
        aviso.put("fecha", fecha);
        aviso.put("tipo", tipo);
        return aviso;
    }

    @GetMapping("/perfil")
    public String perfilUsuario(Authentication authentication, Model model) {
        String username = authentication.getName();
        
        // Buscar usuario por DNI (que es el username en el login)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(username);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            
            // Si es alumno, cargar datos académicos
            if (usuario instanceof Alumno) {
                model.addAttribute("alumno", (Alumno) usuario);
            }
        }
        
        return "perfilAlumno";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(
            Authentication authentication,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
            @RequestParam(required = false) String genero,
            @RequestParam String correo,
            @RequestParam("numTelefono") String numTelefono,
            @RequestParam(required = false) String ultimosEstudios,
            @RequestParam(required = false) String colegioEgreso,
            @RequestParam(required = false) Integer añoEgreso,
            @RequestParam(required = false) MultipartFile foto,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            System.out.println("[PERFIL] Solicitud de actualización recibida");
            String username = authentication.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(username);
            if (usuarioOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("mensaje", "Usuario no encontrado");
                redirectAttributes.addFlashAttribute("tipo", "error");
                return "redirect:/alumno/perfil"; // Corregido redirección
            }

            Usuario usuario = usuarioOpt.get();

            // Actualizar campos permitidos (DNI NO se modifica)
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setFechaNacimiento(fechaNacimiento);
            if (genero != null && !genero.isBlank()) {
                try {
                    usuario.setGenero(TipoGenero.valueOf(genero.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    redirectAttributes.addFlashAttribute("mensaje", "Género inválido");
                    redirectAttributes.addFlashAttribute("tipo", "error");
                    return "redirect:/alumno/perfil";
                }
            }
            usuario.setCorreo(correo);
            usuario.setNumTelefono(numTelefono);

            // Actualizar foto de perfil
            if (foto != null && !foto.isEmpty()) {
                try {
                    eliminarFotoAnteriorSiExiste(usuario.getFoto());
                    String urlFoto = guardarFotoPerfil(foto);
                    usuario.setFoto(urlFoto);
                } catch (IOException e) {
                    System.err.println("Error al guardar la foto de perfil: " + e.getMessage());
                    redirectAttributes.addFlashAttribute("mensaje", "Error al guardar la imagen de perfil");
                    redirectAttributes.addFlashAttribute("tipo", "warning");
                }
            }

            // Actualizar datos académicos si es Alumno
            if (usuario instanceof Alumno) {
                Alumno alumno = (Alumno) usuario;
                if (ultimosEstudios != null) alumno.setUltimosEstudios(ultimosEstudios);
                if (colegioEgreso != null) alumno.setColegioEgreso(colegioEgreso);
                if (añoEgreso != null) alumno.setAñoEgreso(añoEgreso);
            }

            usuarioRepository.save(usuario);

            // Actualizar la sesión de seguridad con los nuevos datos del usuario
            CustomUsuarioDetails userDetails = new CustomUsuarioDetails(usuario);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userDetails,
                authentication.getCredentials(),
                userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente");
            redirectAttributes.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al actualizar el perfil: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "error");
        }
        return "redirect:/alumno/perfil"; // Corregido redirección
    }

    private String guardarFotoPerfil(MultipartFile foto) throws IOException {
        return "/api/usuarios/imagen/" + usuarioImagenService.guardarImagenUsuario(foto).getId();
    }
    
    private void eliminarFotoAnteriorSiExiste(String fotoUrl) {
        if (fotoUrl == null || fotoUrl.isBlank()) return;
        Long id = extraerIdImagenUsuario(fotoUrl);
        if (id != null) {
            try {
                usuarioImagenService.eliminarImagenUsuario(id);
            } catch (Exception e) {
                System.err.println("Advertencia: no se pudo eliminar foto previa: " + e.getMessage());
            }
        }
    }
    
    private Long extraerIdImagenUsuario(String fotoUrl) {
        String prefix = "/api/usuarios/imagen/";
        if (fotoUrl == null) return null;
        int idx = fotoUrl.indexOf(prefix);
        if (idx == -1) return null;
        String idPart = fotoUrl.substring(idx + prefix.length());
        try {
            return Long.parseLong(idPart.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/perfil/seguridad/cambiar-password")
    public String solicitarCambioPassword(
            Authentication authentication,
            @RequestParam String passwordActual,
            @RequestParam String passwordNueva,
            RedirectAttributes redirectAttributes) {
        try {
            String dni = authentication.getName();
            Usuario usuario = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            passwordRecoveryService.solicitarCambioContraseña(usuario, passwordActual, passwordNueva);

            redirectAttributes.addFlashAttribute("mensaje", "Solicitud iniciada. Revisa tu correo electrónico y haz clic en el enlace para CONFIRMAR el cambio.");
            redirectAttributes.addFlashAttribute("tipo", "success");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensaje", e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "error");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al procesar la solicitud: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "error");
        }
        return "redirect:/alumno/perfil?tab=seguridad";
    }

    // Evitar 404 si el navegador hace GET a /perfil/actualizar
    @GetMapping("/perfil/actualizar")
    public String redirigirPerfilActualizar() {
        return "redirect:/alumno/perfil";
    }

    // Endpoint para IA - Análisis Personal del Alumno
    @Auditable(action = "ANALISIS_PERSONAL_IA", entity = "IA")
    @GetMapping("/ia/analisis-personal")
    public ResponseEntity<String> analizarRendimientoPersonal(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("No autorizado");
        
        Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(principal.getName());
        if (usuarioOpt.isEmpty()) return ResponseEntity.notFound().build();

        String reporteHtml = analisisRendimientoService.analizarAlumnoCompleto(usuarioOpt.get());
        return ResponseEntity.ok(reporteHtml);
    }
    
    @Auditable(action = "SOLICITUD_TUTORIA_IA", entity = "IA")
    @PostMapping("/ia/solicitar-tutoria")
    public ResponseEntity<String> solicitarTutoria(@RequestParam Long ofertaId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("No autorizado");
        
        Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(principal.getName());
        if (usuarioOpt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            analisisRendimientoService.solicitarTutoria(usuarioOpt.get(), ofertaId);
            return ResponseEntity.ok("Solicitud enviada correctamente a los docentes.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al procesar solicitud.");
        }
    }

    @GetMapping("/ia/ofertas")
    @ResponseBody
    public ResponseEntity<List<Map<String,Object>>> obtenerOfertasParaIA(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findByEstadoIn(
                Arrays.asList(EstadoOferta.ACTIVA, EstadoOferta.ENCURSO));

        List<Map<String,Object>> resultado = ofertas.stream().map(oferta -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", oferta.getIdOferta());
            m.put("nombre", oferta.getNombre());
            m.put("descripcion", oferta.getDescripcion());
            m.put("tipo", oferta.getTipo());
            m.put("costoInscripcion", oferta.getCostoInscripcion());
            m.put("recargoMora", oferta.getRecargoMora());
            m.put("fechaInicio", oferta.getFechaInicio());
            m.put("fechaFin", oferta.getFechaFin());
            m.put("cupos", oferta.getCupos());
            m.put("visibilidad", oferta.getVisibilidad());
            // Campos específicos de subclases
            if (oferta instanceof Curso) {
                Curso c = (Curso) oferta;
                m.put("costoCuota", c.getCostoCuota());
                m.put("nrCuotas", c.getNrCuotas());
                m.put("diaVencimiento", c.getDiaVencimiento());
            } else if (oferta instanceof Formacion) {
                Formacion f = (Formacion) oferta;
                m.put("costoCuota", f.getCostoCuota());
                m.put("nrCuotas", f.getNrCuotas());
            }
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }
    
    /**
     * Solicitar notificación cuando se abran las inscripciones
     */
    @PostMapping("/notificar-inscripcion/{ofertaId}")
    @ResponseBody
    public ResponseEntity<?> solicitarNotificacionInscripcion(@PathVariable Long ofertaId, Authentication authentication) {
        try {
            String dni = authentication.getName();
            
            Usuario usuario = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
            
            notificacionInscripcionService.solicitarNotificacion(usuario, oferta);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Te notificaremos por email cuando se abran las inscripciones");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al procesar tu solicitud: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

