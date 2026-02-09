package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Time;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.enums.Dias;
import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.EstadoProcesoCertificacion;
import com.example.demo.enums.Modalidad;
import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Auditable;
import com.example.demo.model.CarruselImagen;
import com.example.demo.model.Categoria;
import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.DisponibilidadDocente;
import com.example.demo.model.Docente;
import com.example.demo.model.Formacion;
import com.example.demo.model.Horario;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Instituto;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pais;
import com.example.demo.model.Rol;
import com.example.demo.model.Seminario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.CarruselImagenRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CharlaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.FormacionRepository;
import com.example.demo.repository.HorarioRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.SeminarioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.AnalisisRendimientoService;
import com.example.demo.service.CertificacionService;
import com.example.demo.service.DisponibilidadDocenteService;
import com.example.demo.service.GeneradorHorariosService;
import com.example.demo.service.ImagenService;
import com.example.demo.service.InstitutoService;
import com.example.demo.service.InstitutoLogoService;
import com.example.demo.service.LocacionAPIService;
import com.example.demo.service.OfertaAcademicaService;
import com.example.demo.service.OfertaImagenService;
import com.example.demo.service.RegistroService;
import com.example.demo.service.UsuarioImagenService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@SuppressWarnings("unused") // Los repositorios se usan en métodos privados
public class AdminController {

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private AnalisisRendimientoService analisisRendimientoService;

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private FormacionRepository formacionRepository;

    @Autowired
    private CharlaRepository charlaRepository;

    @Autowired
    private SeminarioRepository seminarioRepository;

    @Autowired
    private OfertaAcademicaService ofertaAcademicaService;

    @Autowired
    private final LocacionAPIService locacionApiService;

    @Autowired
    private RegistroService registroService;

    @Autowired
    private InstitutoService institutoService;
    
    @Autowired
    private com.example.demo.scheduler.ReporteSemanalScheduler reporteSemanalScheduler;

    @Autowired
    private InstitutoLogoService institutoLogoService;
    
    @Autowired
    private CarruselImagenRepository carruselImagenRepository;

    @Autowired
    private ImagenService imagenService;

    @Autowired
    private OfertaImagenService ofertaImagenService;

    @Autowired
    private UsuarioImagenService usuarioImagenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private DisponibilidadDocenteService disponibilidadDocenteService;

    @Autowired
    private GeneradorHorariosService generadorHorariosService;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CertificacionService certificacionService;

    public AdminController(LocacionAPIService locacionApiService,
            RegistroService registroService) {
        this.locacionApiService = locacionApiService;
        this.registroService = registroService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        // 1. Estadísticas de Ofertas
        long totalOfertas = ofertaAcademicaRepository.count();
        long ofertasActivas = ofertaAcademicaRepository.countByEstado(EstadoOferta.ACTIVA) +
                ofertaAcademicaRepository.countByEstado(EstadoOferta.ENCURSO);

        long totalCursos = cursoRepository.count();
        long totalFormaciones = formacionRepository.count();
        long totalSeminarios = seminarioRepository.count();
        long totalCharlas = charlaRepository.count();

        // 2. Estadísticas de Usuarios
        List<Usuario> alumnos = usuarioRepository.findByRolesNombre("ALUMNO");
        long totalAlumnos = alumnos.size();

        List<Usuario> docentes = usuarioRepository.findByRolesNombre("DOCENTE");
        long totalDocentes = docentes.size();

        String ratio = totalDocentes > 0 ? String.format("%.1f:1", (double) totalAlumnos / totalDocentes) : "0:1";

        // 3. Inscripciones del mes actual
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        long inscripcionesMes = inscripcionRepository.countByFechaInscripcionBetween(startOfMonth, endOfMonth);

        // 4. Tasa de Finalización (Alumnos que terminaron / Total inscripciones en
        // ofertas finalizadas)
        long inscripcionesFinalizadas = inscripcionRepository
                .countByOfertaEstadoAndEstadoInscripcionTrue(EstadoOferta.FINALIZADA);
        long totalInscripcionesEnFinalizadas = inscripcionRepository.countByOfertaEstado(EstadoOferta.FINALIZADA);

        long tasaFinalizacion = totalInscripcionesEnFinalizadas > 0
                ? (inscripcionesFinalizadas * 100 / totalInscripcionesEnFinalizadas)
                : 0;

        // 5. Actividad Mensual (Últimos 6 meses para gráfica)
        List<Map<String, Object>> actividadMensual = new ArrayList<>();
        String[] nombresMeses = { "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic" };

        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            LocalDate start = date.withDayOfMonth(1);
            LocalDate end = date.withDayOfMonth(date.lengthOfMonth());
            long count = inscripcionRepository.countByFechaInscripcionBetween(start, end);

            Map<String, Object> mesData = new HashMap<>();
            mesData.put("mes", nombresMeses[date.getMonthValue() - 1]);
            mesData.put("inscripciones", count);
            mesData.put("completados", 0); // Dato simulado por falta de fecha fin inscripción
            actividadMensual.add(mesData);
        }

        // 6. Actividad Reciente (solo logins de admin y CRUD del ABM)
        List<com.example.demo.model.AuditLog> logs = auditLogRepository
                .findDashboardRelevantLogs(org.springframework.data.domain.PageRequest.of(0, 5)).getContent();
        List<Map<String, Object>> actividadesRecientes = logs.stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            String accion = log.getAccion() != null ? log.getAccion().toLowerCase() : "";

            String tipo = "otro";
            String descripcion = "";

            // Clasificar por tipo de acción
            if (accion.contains("inicio_sesion") || accion.contains("login")) {
                tipo = "login";
                descripcion = log.getDetalles() != null ? log.getDetalles() : "Inicio de sesión de administrador";
            } else if (accion.contains("alta") || accion.contains("crear")) {
                tipo = "registro";
                descripcion = log.getDetalles() != null ? log.getDetalles() : log.getAccion();
            } else if (accion.contains("modificar") || accion.contains("modificacion")) {
                tipo = "modificacion";
                descripcion = log.getDetalles() != null ? log.getDetalles() : log.getAccion();
            } else if (accion.contains("baja") || accion.contains("eliminar")) {
                tipo = "eliminacion";
                descripcion = log.getDetalles() != null ? log.getDetalles() : log.getAccion();
            } else if (accion.contains("reactivar") || accion.contains("cambiar_estado")) {
                tipo = "modificacion";
                descripcion = log.getDetalles() != null ? log.getDetalles() : log.getAccion();
            } else {
                descripcion = log.getDetalles() != null ? log.getDetalles() : log.getAccion();
            }

            map.put("tipo", tipo);
            map.put("descripcion", descripcion);
            map.put("fechaHora", log.getFecha() + " " + log.getHora());
            return map;
        }).collect(Collectors.toList());

        // Pasar al modelo
        model.addAttribute("totalOfertas", totalOfertas);
        model.addAttribute("ofertasActivas", ofertasActivas);

        model.addAttribute("totalCursos", totalCursos);
        model.addAttribute("totalFormaciones", totalFormaciones);
        model.addAttribute("totalSeminarios", totalSeminarios);
        model.addAttribute("totalCharlas", totalCharlas);

        model.addAttribute("totalAlumnos", totalAlumnos);
        model.addAttribute("totalDocentes", totalDocentes);
        model.addAttribute("ratioAlumnosDocentes", ratio);

        model.addAttribute("inscripcionesMes", inscripcionesMes);
        model.addAttribute("tasaFinalizacion", tasaFinalizacion);

        model.addAttribute("actividadMensual", actividadMensual);
        model.addAttribute("actividadesRecientes", actividadesRecientes);

        return "admin/panelAdmin";
    }

    @GetMapping("/admin/certificaciones")
    public String adminCertificaciones(Model model) {
        List<OfertaAcademica> ofertasCerradas = ofertaAcademicaRepository.findByEstado(EstadoOferta.CERRADA);
        ofertasCerradas.forEach(o -> {
            if (o.getEstadoProcesoCertificacion() == null) {
                o.setEstadoProcesoCertificacion(EstadoProcesoCertificacion.EN_GESTION_CERTIFICACION);
            }
        });

        model.addAttribute("ofertas", ofertasCerradas);
        model.addAttribute("estadosCert", EstadoProcesoCertificacion.values());
        return "admin/certificaciones";
    }

    @PostMapping("/admin/certificaciones/{id}/estado")
    public String actualizarEstadoCertificacion(
            @PathVariable Long id,
            @RequestParam EstadoProcesoCertificacion estado,
            RedirectAttributes redirectAttributes) {
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

        oferta.setEstadoProcesoCertificacion(estado);
        ofertaAcademicaRepository.save(oferta);

        redirectAttributes.addFlashAttribute("success", "Estado de certificación actualizado");
        return "redirect:/admin/certificaciones";
    }

    @PostMapping("/admin/certificaciones/{id}/cerrar")
    public String cerrarNotasYEmitirActaAdmin(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            Docente docenteReferencia = obtenerDocenteReferencia(oferta);
            certificacionService.cerrarNotasYEmitirCertificados(id, docenteReferencia);

            redirectAttributes.addFlashAttribute("success",
                    "Notas cerradas y acta emitida para la oferta " + oferta.getNombre());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cerrar notas: " + e.getMessage());
        }
        return "redirect:/admin/certificaciones";
    }

    private Docente obtenerDocenteReferencia(OfertaAcademica oferta) {
        if (oferta instanceof Curso curso && curso.getDocentes() != null && !curso.getDocentes().isEmpty()) {
            return curso.getDocentes().get(0);
        }
        if (oferta instanceof Formacion formacion && formacion.getDocentes() != null
                && !formacion.getDocentes().isEmpty()) {
            return formacion.getDocentes().get(0);
        }
        throw new RuntimeException("La oferta no tiene docente asignado para registrar el cierre");
    }

    @GetMapping("/admin/gestion-ofertas")
    public String gestionOfertas(Model model) {
        try {
            System.out.println("🔍 Iniciando gestionOfertas...");

            // Asegurar estados actualizados por fecha (finalizadas/encurso)
            ofertaAcademicaService.actualizarEstadosPorFecha();

            // RESTAURADO: Cargar ofertas desde la base de datos con validación de nulos
            List<OfertaAcademica> ofertas = ofertaAcademicaService.obtenerTodas();

            // Validación defensiva: eliminar ofertas nulas
            if (ofertas != null) {
                ofertas.removeIf(Objects::isNull);
                System.out.println("📊 Ofertas válidas encontradas: " + ofertas.size());
            } else {
                ofertas = new ArrayList<>();
                System.out.println("⚠️ Lista de ofertas era null, inicializando vacía");
            }

            model.addAttribute("ofertas", ofertas);
            model.addAttribute("modalidades", Modalidad.values());
            model.addAttribute("estados", EstadoOferta.values());

            // Objeto vacío para formulario de edición
            OfertaAcademica ofertaEditar = new OfertaAcademica();
            model.addAttribute("ofertaEditar", ofertaEditar);

            System.out.println("✅ gestionOfertas completado exitosamente");
            return "admin/gestionOfertas";
        } catch (Exception e) {
            System.err.println("❌ Error en gestionOfertas: " + e.getMessage());
            e.printStackTrace();

            // En caso de error, pasamos datos mínimos
            model.addAttribute("ofertas", new ArrayList<>());
            model.addAttribute("modalidades", Modalidad.values());
            model.addAttribute("estados", EstadoOferta.values());
            model.addAttribute("ofertaEditar", new OfertaAcademica());
            model.addAttribute("error", "Error al cargar ofertas académicas: " + e.getMessage());

            return "admin/gestionOfertas";
        }
    }

    // =================== ENDPOINTS PARA GESTIÓN DE OFERTAS ===================

    /**
     * Endpoint para obtener los detalles de una oferta específica (para el modal)
     */
    @GetMapping("/admin/ofertas/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDetalleOferta(@PathVariable Long id) {
        try {
            System.out.println("🔍 Buscando oferta con ID: " + id);
            Optional<OfertaAcademica> ofertaOpt = ofertaAcademicaRepository.findById(id);

            if (ofertaOpt.isEmpty()) {
                System.out.println("❌ Oferta no encontrada con ID: " + id);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Oferta no encontrada");
                return ResponseEntity.notFound().build();
            }

            OfertaAcademica oferta = ofertaOpt.get();
            EstadoOferta estadoAnterior = oferta.getEstado();
            System.out.println("✅ Oferta encontrada: " + oferta.getNombre() + " (Tipo: "
                    + oferta.getClass().getSimpleName() + ")");

            Map<String, Object> detalleOferta = obtenerDetalleOfertaCompleto(oferta);
            System.out.println("📋 Detalle obtenido con " + detalleOferta.size() + " campos");
            System.out.println("🔑 Campos disponibles: " + detalleOferta.keySet());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("oferta", detalleOferta);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener los detalles de la oferta: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para el formulario de edición (carga los datos en el formulario)
     */
    @GetMapping("/admin/ofertas/editar/{id}")
    public String editarOferta(@PathVariable Long id, Model model) {
        try {
            Optional<OfertaAcademica> ofertaOpt = ofertaAcademicaRepository.findById(id);

            if (ofertaOpt.isEmpty()) {
                model.addAttribute("error", "Oferta no encontrada");
                return "redirect:/admin/gestion-ofertas";
            }

            OfertaAcademica oferta = ofertaOpt.get();
            EstadoOferta estadoAnterior = oferta.getEstado();

            // Agregar la oferta al modelo para pre-poblar el formulario
            model.addAttribute("ofertaEditar", oferta);
            model.addAttribute("esEdicion", true);

            // Obtener todas las ofertas para la tabla
            List<OfertaAcademica> ofertas = ofertaAcademicaService.obtenerTodas();
            model.addAttribute("ofertas", ofertas);

            model.addAttribute("modalidades", Modalidad.values());
            model.addAttribute("estados", EstadoOferta.values());

            return "admin/gestionOfertas";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la oferta para edición: " + e.getMessage());
            return "redirect:/admin/gestion-ofertas";
        }
    }

    @PostMapping("/admin/ia/trigger-analysis")
    @ResponseBody
    public ResponseEntity<String> triggerAnalisisIA() {
        try {
            analisisRendimientoService.ejecutarAnalisisDiario();
            return ResponseEntity.ok("Análisis de IA ejecutado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al ejecutar análisis: " + e.getMessage());
        }
    }

    /**
     * Endpoint para eliminar una oferta (Eliminación lógica: cambia estado a
     * DE_BAJA)
     */
    @PostMapping("/admin/ofertas/eliminar/{id}")
    @Auditable(action = "ELIMINAR_OFERTA", entity = "OfertaAcademica")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarOferta(@PathVariable Long id) {
        try {
            Optional<OfertaAcademica> ofertaOpt = ofertaAcademicaRepository.findById(id);

            if (ofertaOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Oferta no encontrada");
                response.put("auditDetails", "No se pudo eliminar: oferta no encontrada (ID " + id + ").");
                return ResponseEntity.notFound().build();
            }

            OfertaAcademica oferta = ofertaOpt.get();
            EstadoOferta estadoAnterior = oferta.getEstado();

            // Verificar si puede ser eliminada (lógica de negocio: sin inscripciones, no
            // finalizada)
            if (!oferta.puedeSerEliminada()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                String motivo = "No se puede eliminar esta oferta porque tiene inscripciones o ya finalizó";
                response.put("message", motivo);
                response.put("auditDetails", motivo + " (ID " + oferta.getIdOferta() + ").");
                return ResponseEntity.badRequest().body(response);
            }

            // Eliminación Lógica a través del servicio
            ofertaAcademicaService.eliminar(id, oferta.getTipoOferta());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta eliminada correctamente (Baja lógica)");
            response.put("auditDetails",
                    "Eliminacion de oferta " + oferta.getTipoOferta() + ": " + trunc(oferta.getNombre(), 80) + " (ID "
                            + oferta.getIdOferta() + ")");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            String motivo = "Error al eliminar la oferta: " + e.getMessage();
            response.put("message", motivo);
            response.put("auditDetails", motivo);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para cambiar el estado de una oferta (activar/desactivar)
     */
    @PostMapping("/admin/ofertas/cambiar-estado/{id}")
    @Auditable(action = "CAMBIAR_ESTADO_OFERTA", entity = "OfertaAcademica")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstadoOferta(@PathVariable Long id) {
        try {
            System.out.println("🔄 Cambiando estado de oferta con ID: " + id);
            Optional<OfertaAcademica> ofertaOpt = ofertaAcademicaRepository.findById(id);

            if (ofertaOpt.isEmpty()) {
                System.out.println("❌ Oferta no encontrada con ID: " + id);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Oferta no encontrada");
                response.put("motivo", "OFERTA_NO_ENCONTRADA");
                response.put("auditDetails", "No se pudo cambiar estado: oferta no encontrada (ID " + id + ").");
                return ResponseEntity.ok(response);
            }

            OfertaAcademica oferta = ofertaOpt.get();
            EstadoOferta estadoAnterior = oferta.getEstado();
            System.out.println("📋 Estado actual: " + oferta.getEstado());

            // Validar si se puede cambiar el estado (no FINALIZADA)
            if (!oferta.puedeCambiarEstado()) {
                String motivo = "No se puede cambiar el estado de una oferta finalizada";
                System.out.println("❌ No se puede cambiar estado: " + motivo);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", motivo);
                response.put("motivo", "ESTADO_FINAL");
                response.put("auditDetails", motivo + " (ID " + oferta.getIdOferta() + ").");
                return ResponseEntity.ok(response);
            }

            boolean exito = false;
            String mensajeError = null;
            String motivoCodigo = "VALIDACION_FALLIDA";

            // Lógica de Toggle: Si está DE_BAJA -> Alta, Si está ACTIVA/ENCURSO -> Baja
            if (oferta.getEstado() == EstadoOferta.DE_BAJA) {
                // Intentar dar de alta
                exito = oferta.darDeAlta();
                if (!exito) {
                    mensajeError = "No se puede activar: La fecha de inicio no debe ser anterior a hoy y la fecha fin debe ser futura.";
                    motivoCodigo = "VALIDACION_ALTA";
                } else {
                    System.out.println("🟢 Cambiando a " + oferta.getEstado());
                }
            } else {
                // Intentar dar de baja
                if (!oferta.puedeDarseDeBaja()) {
                    mensajeError = obtenerMotivoRechazoBaja(oferta);
                    motivoCodigo = "VALIDACION_BAJA";
                    exito = false;
                } else {
                    exito = oferta.darDeBaja();
                    System.out.println("🔴 Cambiando a DE_BAJA");
                }
            }

            if (!exito) {
                System.out.println("❌ Operación rechazada: " + mensajeError);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", mensajeError);
                response.put("motivo", motivoCodigo);
                response.put("auditDetails", mensajeError + " (ID " + oferta.getIdOferta() + ").");
                return ResponseEntity.ok(response);
            }

            // Guardar cambios
            ofertaAcademicaRepository.save(oferta);
            System.out.println("✅ Estado cambiado exitosamente a: " + oferta.getEstado());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("nuevoEstado", oferta.getEstado().toString());
            response.put("message", "Estado de la oferta cambiado exitosamente");
            response.put("auditDetails",
                    "Cambio de estado oferta " + oferta.getTipoOferta() + ": " + trunc(oferta.getNombre(), 80) + " (ID "
                            + oferta.getIdOferta() + ") " + estadoAnterior + " -> " + oferta.getEstado());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error cambiando estado de oferta " + id + ": " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            String motivo = "Error interno del servidor: " + e.getMessage();
            response.put("message", motivo);
            response.put("motivo", "ERROR_SERVIDOR");
            response.put("auditDetails", motivo + " (ID " + id + ").");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Genera el mensaje de error explicando por qué no se puede dar de baja
     */
    private String obtenerMotivoRechazoBaja(OfertaAcademica oferta) {
        java.time.LocalDate ahora = java.time.LocalDate.now();

        // Contar inscripciones activas
        int inscripcionesActivas = 0;
        if (oferta.getInscripciones() != null) {
            inscripcionesActivas = (int) oferta.getInscripciones().stream()
                    .filter(inscripcion -> inscripcion.getEstadoInscripcion() != null &&
                            inscripcion.getEstadoInscripcion() == true)
                    .count();
        }

        // Si ya terminó la oferta, siempre se debería poder dar de baja (aunque el
        // estado sería FINALIZADA)
        if (oferta.getFechaFin() != null && oferta.getFechaFin().isBefore(ahora)) {
            return null;
        }

        // Si ya comenzó y tiene inscripciones activas
        if (oferta.getFechaInicio() != null && !oferta.getFechaInicio().isAfter(ahora) && inscripcionesActivas > 0) {
            return "No se puede dar de baja esta oferta porque ya comenzó y tiene " +
                    inscripcionesActivas + " inscripcion" + (inscripcionesActivas > 1 ? "es" : "") + " activa" +
                    (inscripcionesActivas > 1 ? "s" : "") + ". Las inscripciones deben ser canceladas primero.";
        }

        return "No se puede dar de baja la oferta debido a restricciones de negocio.";
    }

    /**
     * Método auxiliar para mapear una oferta a un objeto de respuesta con
     * validaciones defensivas
     */
    private Map<String, Object> mapearOfertaAResponse(OfertaAcademica oferta) {
        Map<String, Object> map = new HashMap<>();

        // Validación defensiva: verificar que la oferta no sea null
        if (oferta == null) {
            map.put("error", "Oferta nula");
            return map;
        }

        // Mapear campos básicos con validaciones defensivas
        map.put("id", oferta.getIdOferta() != null ? oferta.getIdOferta() : 0L);
        map.put("nombre", oferta.getNombre() != null ? oferta.getNombre() : "");
        map.put("descripcion", oferta.getDescripcion() != null ? oferta.getDescripcion() : "");
        map.put("tipo", oferta.getTipoOferta() != null ? oferta.getTipoOferta() : "");
        map.put("modalidad", oferta.getModalidad() != null ? oferta.getModalidad() : "");
        map.put("cupos", oferta.getCupos() != null ? oferta.getCupos() : 0);
        map.put("infoCupos", oferta.getInfoCupos() != null ? oferta.getInfoCupos() : "");
        map.put("fechaInicio", oferta.getFechaInicio());
        map.put("fechaFin", oferta.getFechaFin());
        map.put("estado", oferta.getEstado() != null ? oferta.getEstado() : "");
        map.put("duracion", oferta.getDuracion() != null ? oferta.getDuracion() : "");
        map.put("certificado", oferta.getCertificado() != null ? oferta.getCertificado() : "");
        map.put("costoFormateado", oferta.getCostoFormateado() != null ? oferta.getCostoFormateado() : "$0");
        map.put("costoInscripcion", oferta.getCostoInscripcion() != null ? oferta.getCostoInscripcion() : 0.0);
        map.put("categoriasTexto", oferta.getCategoriasTexto() != null ? oferta.getCategoriasTexto() : "");
        map.put("duracionTexto", oferta.getDuracionTexto() != null ? oferta.getDuracionTexto() : "");
        map.put("lugar", oferta.getLugar() != null ? oferta.getLugar() : "");
        map.put("enlace", oferta.getEnlace() != null ? oferta.getEnlace() : "");
        map.put("imagenUrl", oferta.getImagenUrl() != null ? oferta.getImagenUrl() : "");

        // Validaciones defensivas para métodos que pueden fallar
        try {
            map.put("puedeSerEditada", oferta.puedeSerEditada() != null ? oferta.puedeSerEditada() : false);
        } catch (Exception e) {
            map.put("puedeSerEditada", false);
        }

        try {
            map.put("puedeSerEliminada", oferta.puedeSerEliminada() != null ? oferta.puedeSerEliminada() : false);
        } catch (Exception e) {
            map.put("puedeSerEliminada", false);
        }

        map.put("visibilidad", oferta.getVisibilidad() != null ? oferta.getVisibilidad() : "");
        return map;
    }

    /**
     * Obtiene el detalle completo de una oferta usando los métodos específicos del
     * modelo
     */
    private Map<String, Object> obtenerDetalleOfertaCompleto(OfertaAcademica oferta) {
        Map<String, Object> detalle = new HashMap<>();

        try {
            System.out.println("🔄 Obteniendo detalle para oferta tipo: " + oferta.getClass().getSimpleName());

            // Determinar el tipo específico y obtener el detalle correspondiente
            if (oferta instanceof com.example.demo.model.Curso) {
                System.out.println("📚 Procesando como Curso...");
                com.example.demo.model.Curso curso = (com.example.demo.model.Curso) oferta;
                com.example.demo.model.Curso.CursoDetalle cursoDetalle = curso.obtenerDetalleCompleto();
                detalle = convertirDetalleAMap(cursoDetalle);
                System.out.println("✅ Curso procesado, campos obtenidos: " + detalle.size());
            } else if (oferta instanceof com.example.demo.model.Formacion) {
                System.out.println("🎓 Procesando como Formación...");
                com.example.demo.model.Formacion formacion = (com.example.demo.model.Formacion) oferta;
                com.example.demo.model.Formacion.FormacionDetalle formacionDetalle = formacion.obtenerDetalleCompleto();
                detalle = convertirDetalleAMap(formacionDetalle);
                System.out.println("✅ Formación procesada, campos obtenidos: " + detalle.size());
            } else if (oferta instanceof com.example.demo.model.Charla) {
                System.out.println("🎤 Procesando como Charla...");
                com.example.demo.model.Charla charla = (com.example.demo.model.Charla) oferta;
                com.example.demo.model.Charla.CharlaDetalle charlaDetalle = charla.obtenerDetalleCompleto();
                detalle = convertirDetalleAMap(charlaDetalle);
                System.out.println("✅ Charla procesada, campos obtenidos: " + detalle.size());
            } else if (oferta instanceof com.example.demo.model.Seminario) {
                System.out.println("🏛️ Procesando como Seminario...");
                com.example.demo.model.Seminario seminario = (com.example.demo.model.Seminario) oferta;
                com.example.demo.model.Seminario.SeminarioDetalle seminarioDetalle = seminario.obtenerDetalleCompleto();
                detalle = convertirDetalleAMap(seminarioDetalle);
                System.out.println("✅ Seminario procesado, campos obtenidos: " + detalle.size());
            } else {
                System.out.println("⚠️ Tipo no reconocido, usando fallback...");
                // Fallback para tipos no reconocidos
                detalle = mapearOfertaAResponse(oferta);
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo detalle de oferta " + oferta.getIdOferta() + ": " + e.getMessage());
            e.printStackTrace();
            // Fallback en caso de error
            detalle = mapearOfertaAResponse(oferta);
        }

        // Agregar horarios si existen
        if (oferta.getHorarios() != null && !oferta.getHorarios().isEmpty()) {
            List<Map<String, Object>> horariosList = new ArrayList<>();
            for (com.example.demo.model.Horario h : oferta.getHorarios()) {
                Map<String, Object> hMap = new HashMap<>();
                hMap.put("dia", h.getDia() != null ? h.getDia().toString() : "-");
                hMap.put("horaInicio", h.getHoraInicio() != null ? h.getHoraInicio().toString() : "-");
                hMap.put("horaFin", h.getHoraFin() != null ? h.getHoraFin().toString() : "-");
                horariosList.add(hMap);
            }
            detalle.put("horarios", horariosList);
            System.out.println("✅ Horarios agregados al detalle: " + horariosList.size());
        }

        return detalle;
    }

    /**
     * Convierte cualquier objeto detalle a Map para la respuesta JSON
     */
    private Map<String, Object> convertirDetalleAMap(Object detalle) {
        Map<String, Object> map = new HashMap<>();

        try {
            System.out.println("🔄 Convirtiendo objeto detalle a Map: " + detalle.getClass().getSimpleName());

            // Usar reflection para convertir el objeto a Map
            java.lang.reflect.Field[] fields = detalle.getClass().getDeclaredFields();
            System.out.println("📊 Campos encontrados: " + fields.length);

            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(detalle);
                String fieldName = field.getName();
                map.put(fieldName, value);

                System.out.println("🔑 Campo: " + fieldName + " = " + (value != null ? value.toString() : "null"));
            }

            System.out.println("✅ Conversión completada. Total campos: " + map.size());
        } catch (Exception e) {
            System.err.println("❌ Error convirtiendo detalle a Map: " + e.getMessage());
            e.printStackTrace();
        }

        return map;
    }

    private String trunc(Object value, int max) {
        if (value == null)
            return "";
        String text = value.toString();
        if (text.length() <= max)
            return text;
        if (max <= 3)
            return text.substring(0, max);
        return text.substring(0, max - 3) + "...";
    }

    private void addCambio(List<String> cambios, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            String antes = anterior == null ? "-" : trunc(anterior, 60);
            String despues = nuevo == null ? "-" : trunc(nuevo, 60);
            cambios.add(campo + ": '" + antes + "' -> '" + despues + "'");
        }
    }

    // =================== ENDPOINTS PARA DOCENTES ===================

    @GetMapping("/admin/docentes/buscar")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> buscarDocentes(
            @RequestParam(value = "q", defaultValue = "") String query) {
        try {
            // Si la query está vacía, devolver todos los docentes
            List<Docente> todosDocentes = query.trim().isEmpty() ? docenteRepository.findAllDocentes()
                    : docenteRepository.buscarPorNombreApellidoOMatricula(query);

            // FILTRAR SOLO DOCENTES ACTIVOS
            List<Docente> docentes = todosDocentes.stream()
                    .filter(d -> Boolean.TRUE.equals(d.isEstado()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> resultado = new ArrayList<>();

            for (Docente docente : docentes) {
                Map<String, Object> docenteMap = new HashMap<>();
                docenteMap.put("id", docente.getId());
                docenteMap.put("nombre", docente.getNombre() + " " + docente.getApellido());

                resultado.add(docenteMap);
            }

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    // =================== ENDPOINTS PARA CATEGORÍAS ===================

    @GetMapping("/admin/gestion-usuarios")
    public String gestionUsuarios(Model model) {

        try {
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            model.addAttribute("paises", paises);
        } catch (Exception e) {
            model.addAttribute("paises", List.of());
        }

        return "admin/gestionUsuarios";
    }

    @PostMapping("/admin/ofertas/crear")
    @Auditable(action = "CREAR_OFERTA", entity = "OfertaAcademica")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearOferta(@RequestBody Map<String, Object> datos) {
        try {
            // Crear una oferta académica básica para demostrar funcionalidad
            OfertaAcademica oferta = new OfertaAcademica();
            oferta.setNombre((String) datos.get("nombre"));
            oferta.setDescripcion((String) datos.get("descripcion"));

            if (datos.get("cupos") != null) {
                oferta.setCupos(Integer.valueOf(datos.get("cupos").toString()));
            }

            if (datos.get("costo") != null) {
                oferta.setCostoInscripcion(Double.valueOf(datos.get("costo").toString()));
            }

            if (datos.get("fechaInicio") != null) {
                oferta.setFechaInicio(LocalDate.parse((String) datos.get("fechaInicio")));
            }

            if (datos.get("fechaFin") != null) {
                oferta.setFechaFin(LocalDate.parse((String) datos.get("fechaFin")));
            }

            // Configurar modalidad
            String modalidadStr = (String) datos.get("modalidad");
            if (modalidadStr != null) {
                oferta.setModalidad(Modalidad.valueOf(modalidadStr.toUpperCase()));
            }

            // Configurar estado por defecto
            oferta.setEstado(EstadoOferta.ACTIVA);
            oferta.setVisibilidad(true);

            OfertaAcademica nuevaOferta = ofertaAcademicaRepository.save(oferta);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta creada exitosamente");
            response.put("auditDetails",
                    "Creacion de oferta: " + trunc(oferta.getNombre(), 80) + " (ID " + nuevaOferta.getIdOferta() + ")" +
                            (oferta.getDescripcion() != null ? " | descripcion: " + trunc(oferta.getDescripcion(), 120)
                                    : "")
                            +
                            (oferta.getCupos() != null ? " | cupos: " + oferta.getCupos() : "") +
                            (oferta.getCostoInscripcion() != null ? " | costo: " + oferta.getCostoInscripcion() : "") +
                            (oferta.getFechaInicio() != null ? " | inicio: " + oferta.getFechaInicio() : "") +
                            (oferta.getFechaFin() != null ? " | fin: " + oferta.getFechaFin() : "") +
                            (oferta.getModalidad() != null ? " | modalidad: " + oferta.getModalidad() : ""));
            response.put("id", nuevaOferta.getIdOferta());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al crear oferta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/admin/ofertas/registrar")
    @Auditable(action = "CREAR_OFERTA", entity = "OfertaAcademica")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarOferta(
            @RequestParam String tipoOferta,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) Integer cupos,
            @RequestParam(required = false) Double costoInscripcion,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String fechaInicioInscripcion,
            @RequestParam(required = false) String fechaFinInscripcion,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String otorgaCertificado,
            @RequestParam(required = false) MultipartFile imagen,
            @RequestParam(required = false) String categorias, // IDs de categorías separados por coma
            @RequestParam(required = false) String horarios, // JSON con datos de horarios
            // Campos específicos para CURSO
            @RequestParam(required = false) String temario,
            @RequestParam(required = false) String docentesCurso, // IDs de docentes separados por coma
            @RequestParam(required = false) Double costoCuota,
            @RequestParam(required = false) Double costoMora,
            @RequestParam(required = false) Integer nrCuotas,
            @RequestParam(required = false) Integer diaVencimiento,
            // Campos específicos para FORMACION
            @RequestParam(required = false) String planFormacion,
            @RequestParam(required = false) String docentesFormacion, // IDs de docentes separados por coma
            @RequestParam(required = false) Double costoCuotaFormacion,
            @RequestParam(required = false) Double costoMoraFormacion,
            @RequestParam(required = false) Integer nrCuotasFormacion,
            @RequestParam(required = false) Integer diaVencimientoFormacion,
            // Campos específicos para CHARLA
            @RequestParam(required = false) String lugarCharla,
            @RequestParam(required = false) String enlaceCharla,
            @RequestParam(required = false) String horaCharla,
            @RequestParam(required = false) Integer duracionEstimada,
            @RequestParam(required = false) String disertantesCharla,
            @RequestParam(required = false) String publicoObjetivoCharla,
            // Campos específicos para SEMINARIO
            @RequestParam(required = false) String lugarSeminario,
            @RequestParam(required = false) String enlaceSeminario,
            @RequestParam(required = false) String horaSeminario,
            @RequestParam(required = false) Integer duracionMinutos,
            @RequestParam(required = false) String disertantesSeminario,
            @RequestParam(required = false) String publicoObjetivoSeminario,
            // Campos genéricos para lugar y enlace (para todos los tipos)
            @RequestParam(required = false) String lugar,
            @RequestParam(required = false) String enlace) {

        try {
            System.out.println("🔥 REGISTRO DE OFERTA INICIADO");
            System.out.println("Tipo: " + tipoOferta);

            // Unificar lugar y enlace si vienen en campos específicos
            if (lugar == null || lugar.trim().isEmpty()) {
                if ("CHARLA".equalsIgnoreCase(tipoOferta))
                    lugar = lugarCharla;
                else if ("SEMINARIO".equalsIgnoreCase(tipoOferta))
                    lugar = lugarSeminario;
            }
            if (enlace == null || enlace.trim().isEmpty()) {
                if ("CHARLA".equalsIgnoreCase(tipoOferta))
                    enlace = enlaceCharla;
                else if ("SEMINARIO".equalsIgnoreCase(tipoOferta))
                    enlace = enlaceSeminario;
            }

            System.out.println("Lugar: " + lugar);
            System.out.println("Enlace: " + enlace);

            // Validación obligatoria de fechas
            if (fechaInicio == null || fechaInicio.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "La fecha de inicio es obligatoria");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (fechaFin == null || fechaFin.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "La fecha de fin es obligatoria");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validar formato de fechas
            LocalDate fechaInicioDate, fechaFinDate, fechaInicioInscripcionDate, fechaFinInscripcionDate;
            try {
                fechaInicioDate = LocalDate.parse(fechaInicio);
                fechaFinDate = LocalDate.parse(fechaFin);

                // Validar que fecha de inicio no sea posterior a fecha de fin
                if (fechaInicioDate.isAfter(fechaFinDate)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "La fecha de inicio no puede ser posterior a la fecha de fin");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                // Validar fechas de inscripción
                if (fechaInicioInscripcion == null || fechaInicioInscripcion.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "La fecha de inicio de inscripción es obligatoria");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                if (fechaFinInscripcion == null || fechaFinInscripcion.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "La fecha de fin de inscripción es obligatoria");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                fechaInicioInscripcionDate = LocalDate.parse(fechaInicioInscripcion);
                fechaFinInscripcionDate = LocalDate.parse(fechaFinInscripcion);

                // Validar que fecha de inicio de inscripción no sea posterior a fecha de fin de
                // inscripción
                if (fechaInicioInscripcionDate.isAfter(fechaFinInscripcionDate)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message",
                            "La fecha de inicio de inscripción no puede ser posterior a la fecha de fin de inscripción");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                // REMOVIDO: Validación que las inscripciones deben cerrar antes del inicio
                // Las inscripciones pueden continuar incluso después del inicio de la oferta
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Formato de fecha inválido. Use el formato YYYY-MM-DD");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validar lugar y enlace según modalidad
            // IMPORTANTE: Enlace obligatorio SOLO para CHARLA y SEMINARIO en modalidad
            // virtual/híbrida
            boolean esVirtual = "VIRTUAL".equalsIgnoreCase(modalidad) || "HIBRIDA".equalsIgnoreCase(modalidad);
            boolean esPresencial = "PRESENCIAL".equalsIgnoreCase(modalidad) || "HIBRIDA".equalsIgnoreCase(modalidad);
            boolean esCharlaOSeminario = "CHARLA".equalsIgnoreCase(tipoOferta)
                    || "SEMINARIO".equalsIgnoreCase(tipoOferta);

            // Validar enlace si es virtual/híbrida SOLO para Charla y Seminario
            if (esVirtual && esCharlaOSeminario) {
                if (enlace == null || enlace.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message",
                            "Para modalidad Virtual o Híbrida en Charlas y Seminarios, el enlace es obligatorio");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Validar URL si se proporciona enlace
            if (enlace != null && !enlace.trim().isEmpty()) {
                try {
                    new java.net.URL(enlace);
                } catch (java.net.MalformedURLException e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "El enlace proporcionado no es una URL válida");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Validar lugar si es presencial/híbrida
            if (esPresencial) {
                if (lugar == null || lugar.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Para modalidad Presencial o Híbrida, el lugar es obligatorio");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Validaciones específicas por tipo
            if ("CURSO".equalsIgnoreCase(tipoOferta) || "FORMACION".equalsIgnoreCase(tipoOferta)) {
                String docentes = "CURSO".equalsIgnoreCase(tipoOferta) ? docentesCurso : docentesFormacion;
                if (docentes == null || docentes.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Debe asignar al menos un docente para Cursos y Formaciones");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                if (horarios == null || horarios.trim().isEmpty() || "[]".equals(horarios.trim())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Debe agregar al menos un horario para Cursos y Formaciones");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            } else if ("CHARLA".equalsIgnoreCase(tipoOferta) || "SEMINARIO".equalsIgnoreCase(tipoOferta)) {
                String disertantes = "CHARLA".equalsIgnoreCase(tipoOferta) ? disertantesCharla : disertantesSeminario;

                // Validar disertantes (siempre requerido al menos uno)
                boolean tieneDisertantes = disertantes != null && !disertantes.trim().isEmpty()
                        && !"[]".equals(disertantes.trim());

                if (!tieneDisertantes) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Debe agregar al menos un disertante");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Normalizar costos: convertir null a 0
            if (costoInscripcion == null)
                costoInscripcion = 0.0;
            if (costoCuota == null)
                costoCuota = 0.0;
            if (costoMora == null)
                costoMora = 0.0;
            if (costoCuotaFormacion == null)
                costoCuotaFormacion = 0.0;
            if (costoMoraFormacion == null)
                costoMoraFormacion = 0.0;

            OfertaAcademica oferta;

            // Crear la instancia específica según el tipo
            switch (tipoOferta.toUpperCase()) {
                case "CURSO":
                    oferta = crearCurso(nombre, descripcion, cupos, costoInscripcion, fechaInicio, fechaFin,
                            fechaInicioInscripcion, fechaFinInscripcion, modalidad, lugar, enlace,
                            temario, docentesCurso, costoCuota, costoMora, nrCuotas, diaVencimiento);
                    break;
                case "FORMACION":
                    oferta = crearFormacion(nombre, descripcion, cupos, costoInscripcion, fechaInicio, fechaFin,
                            fechaInicioInscripcion, fechaFinInscripcion, modalidad, lugar, enlace,
                            planFormacion, docentesFormacion,
                            costoCuotaFormacion, costoMoraFormacion, nrCuotasFormacion, diaVencimientoFormacion);
                    break;
                case "CHARLA":
                    oferta = crearCharla(nombre, descripcion, cupos, costoInscripcion, fechaInicio, fechaFin,
                            fechaInicioInscripcion, fechaFinInscripcion, modalidad,
                            lugar, enlace, horaCharla, duracionEstimada, disertantesCharla,
                            publicoObjetivoCharla);
                    break;
                case "SEMINARIO":
                    oferta = crearSeminario(nombre, descripcion, cupos, costoInscripcion, fechaInicio, fechaFin,
                            fechaInicioInscripcion, fechaFinInscripcion, modalidad,
                            lugar, enlace, horaSeminario, duracionMinutos, disertantesSeminario,
                            publicoObjetivoSeminario);
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de oferta no válido: " + tipoOferta);
            }

            // Calcular duración en meses antes de guardar
            oferta.calcularDuracionMeses();

            // Establecer valor del certificado
            if (otorgaCertificado != null && !otorgaCertificado.trim().isEmpty()) {
                boolean certificado = "true".equalsIgnoreCase(otorgaCertificado.trim());
                oferta.setCertificado(certificado);
                System.out.println(
                        "Certificado establecido: " + certificado + " (desde string: '" + otorgaCertificado + "')");
            } else {
                oferta.setCertificado(false);
                System.out.println("Certificado establecido por defecto: false");
            }

            // Manejar imagen
            if (imagen != null && !imagen.isEmpty()) {
                try {
                    String rutaRelativa = guardarImagenOferta(imagen);
                    oferta.setImagenUrl(rutaRelativa);
                } catch (Exception e) {
                    System.err.println("Error al guardar imagen: " + e.getMessage());
                    // Fallback a imagen por defecto
                    oferta.setImagenUrl("/img/predeterminado.jpg");
                }
            } else {
                // Si no se carga imagen nueva Y no tiene imagen previa (es create o no edit),
                // usar default.
                // Nota: para editar, normalmente se valida si imagen es null para mantener la
                // anterior.
                // Aquí, como es registrarOferta (POST), es creación nueva (o sobreescritura si
                // lógica lo permite).
                // Revisar si es edición -> El método registrarOferta parece ser solo para CREAR
                // o registrar nueva.
                // Para editar suele haber otro método 'actualizarOferta'.
                oferta.setImagenUrl("/img/predeterminado.jpg");
            }

            // Asociar horarios si se proporcionaron (ANTES DE GUARDAR)
            if (horarios != null && !horarios.trim().isEmpty()) {
                asociarHorarios(oferta, horarios);
            }

            // Validación de duplicados usando lógica del modelo
            java.util.List<String> erroresDuplicado = oferta.validarDuplicado(ofertaAcademicaRepository);
            if (!erroresDuplicado.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", String.join("; ", erroresDuplicado));
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Guardar en la base de datos
            OfertaAcademica nuevaOferta = ofertaAcademicaRepository.save(oferta);

            // ✅ Asociar categorías si se proporcionaron
            if (categorias != null && !categorias.trim().isEmpty()) {
                System.out.println("🏷️ Procesando categorías: " + categorias);
                asociarCategoriasAOferta(nuevaOferta, categorias);
            }

            // ✅ Si es una Formación con docentes, guardar los docentes para persistir la
            // relación ManyToMany
            if (nuevaOferta instanceof Formacion) {
                Formacion formacion = (Formacion) nuevaOferta;
                if (formacion.getDocentes() != null && !formacion.getDocentes().isEmpty()) {
                    System.out.println("💾 Guardando docentes para persistir relación ManyToMany...");
                    for (Docente docente : formacion.getDocentes()) {
                        docenteRepository.save(docente);
                    }
                    System.out.println("   ✅ Relación docente-formación guardada");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta académica registrada exitosamente");
            response.put("auditDetails",
                    "Creacion de oferta " + tipoOferta + ": " + trunc(nombre, 80) + " (ID " + oferta.getIdOferta() + ")"
                            +
                            (descripcion != null ? " | descripcion: " + trunc(descripcion, 120) : "") +
                            (cupos != null ? " | cupos: " + cupos : "") +
                            (costoInscripcion != null ? " | costo: " + costoInscripcion : "") +
                            (fechaInicio != null ? " | inicio: " + fechaInicio : "") +
                            (fechaFin != null ? " | fin: " + fechaFin : "") +
                            (modalidad != null ? " | modalidad: " + modalidad : ""));
            response.put("id", nuevaOferta.getIdOferta());
            response.put("tipo", tipoOferta);

            return ResponseEntity.ok(response);

        } catch (DataIntegrityViolationException e) {
            // Capturar error de duplicado (nombre único)
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);

            String mensaje = e.getMessage();
            if (mensaje != null && (mensaje.toLowerCase().contains("uk_oferta_nombre") ||
                    mensaje.toLowerCase().contains("duplicate key"))) {
                response.put("message",
                        "Ya existe una oferta académica con este nombre. Por favor, elija un nombre diferente.");
            } else {
                response.put("message",
                        "Error: La oferta no pudo registrarse debido a una restricción de datos. Verifique que no exista una oferta duplicada.");
            }

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (jakarta.validation.ConstraintViolationException e) {
            // Capturar errores de validación y convertirlos a mensajes amigables
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);

            String mensajeError = e.getConstraintViolations().stream()
                    .map(violation -> {
                        String campo = violation.getPropertyPath().toString();
                        String mensaje = violation.getMessage();

                        // Personalizar mensajes según el campo
                        if (campo.equals("cupos")) {
                            return "La cantidad mínima de cupos es de 1";
                        } else if (campo.equals("costoInscripcion")) {
                            return "El costo de inscripción " + mensaje;
                        } else if (campo.equals("nombre")) {
                            return "El nombre de la oferta es obligatorio";
                        } else if (campo.equals("descripcion")) {
                            return "La descripción de la oferta es obligatoria";
                        } else {
                            return "El campo " + campo + " " + mensaje;
                        }
                    })
                    .findFirst()
                    .orElse("Error de validación en los datos ingresados");

            response.put("message", mensajeError);
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al registrar oferta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Métodos auxiliares para crear cada tipo de oferta específica

    private Curso crearCurso(String nombre, String descripcion, Integer cupos, Double costo,
            String fechaInicio, String fechaFin, String fechaInicioInscripcion, String fechaFinInscripcion,
            String modalidad, String lugar, String enlace,
            String temario, String docentesIds, Double costoCuota, Double costoMora,
            Integer nrCuotas, Integer diaVencimiento) {
        Curso curso = new Curso();
        configurarOfertaBase(curso, nombre, descripcion, cupos, costo, fechaInicio, fechaFin,
                fechaInicioInscripcion, fechaFinInscripcion, modalidad, lugar, enlace);

        // Campos específicos del curso
        curso.setTemario(temario);
        if (costoCuota != null)
            curso.setCostoCuota(costoCuota);
        if (costoMora != null) {
            curso.setCostoMora(costoMora);
            curso.setRecargoMora(costoMora);
        }
        if (nrCuotas != null)
            curso.setNrCuotas(nrCuotas);
        if (diaVencimiento != null)
            curso.setDiaVencimiento(diaVencimiento);

        // Asociar docentes si se proporcionaron
        if (docentesIds != null && !docentesIds.trim().isEmpty()) {
            List<Docente> docentes = obtenerDocentesPorIds(docentesIds);
            curso.setDocentes(docentes);
        }

        return curso;
    }

    private Formacion crearFormacion(String nombre, String descripcion, Integer cupos, Double costo,
            String fechaInicio, String fechaFin, String fechaInicioInscripcion, String fechaFinInscripcion,
            String modalidad, String lugar, String enlace,
            String plan, String docentesIds, Double costoCuota, Double costoMora,
            Integer nrCuotas, Integer diaVencimiento) {
        Formacion formacion = new Formacion();
        configurarOfertaBase(formacion, nombre, descripcion, cupos, costo, fechaInicio, fechaFin,
                fechaInicioInscripcion, fechaFinInscripcion, modalidad, lugar, enlace);

        // Campos específicos de la formación
        formacion.setPlan(plan);

        // ✅ Asegurar que los valores no sean null
        formacion.setCostoCuota(costoCuota != null ? costoCuota : 0.0);
        formacion.setCostoMora(costoMora != null ? costoMora : 0.0);
        if (costoMora != null) {
            formacion.setRecargoMora(costoMora);
        }
        formacion.setNrCuotas(nrCuotas != null ? nrCuotas : 0);
        formacion.setDiaVencimiento(diaVencimiento != null ? diaVencimiento : 0);

        System.out.println("🔍 Formación creada con:");
        System.out.println("   - Costo Cuota: " + formacion.getCostoCuota());
        System.out.println("   - Costo Mora: " + formacion.getCostoMora());
        System.out.println("   - Nr Cuotas: " + formacion.getNrCuotas());
        System.out.println("   - Día Vencimiento: " + formacion.getDiaVencimiento());

        // Asociar docentes si se proporcionaron
        if (docentesIds != null && !docentesIds.trim().isEmpty()) {
            System.out.println("🎓 Asociando docentes: " + docentesIds);
            List<Docente> docentes = obtenerDocentesPorIds(docentesIds);

            if (!docentes.isEmpty()) {
                // ✅ Configurar relación bidireccional ManyToMany
                for (Docente docente : docentes) {
                    // Agregar la formación a la lista del docente (lado propietario)
                    if (!docente.getFormaciones().contains(formacion)) {
                        docente.getFormaciones().add(formacion);
                    }
                    // Agregar el docente a la lista de la formación (lado inverso)
                    if (!formacion.getDocentes().contains(docente)) {
                        formacion.getDocentes().add(docente);
                    }
                }
                System.out.println("   ✅ " + docentes.size() + " docente(s) asociado(s)");
            } else {
                System.out.println("   ⚠️ No se encontraron docentes con los IDs proporcionados");
            }
        } else {
            System.out.println("   ℹ️ No se proporcionaron docentes");
        }

        return formacion;
    }

    private Charla crearCharla(String nombre, String descripcion, Integer cupos, Double costo,
            String fechaInicio, String fechaFin, String fechaInicioInscripcion, String fechaFinInscripcion,
            String modalidad,
            String lugar, String enlace, String horaCharla, Integer duracionEstimada,
            String disertantesStr, String publicoObjetivo) {
        Charla charla = new Charla();
        configurarOfertaBase(charla, nombre, descripcion, cupos, costo, fechaInicio, fechaFin,
                fechaInicioInscripcion, fechaFinInscripcion, modalidad, lugar, enlace);

        // Convertir y asignar hora de inicio
        if (horaCharla != null && !horaCharla.trim().isEmpty()) {
            try {
                charla.setHoraInicio(java.sql.Time.valueOf(horaCharla + ":00"));
            } catch (Exception e) {
                System.err.println("Error al convertir hora de charla: " + e.getMessage());
            }
        }

        // Campos específicos de la charla
        if (duracionEstimada != null)
            charla.setDuracionEstimada(duracionEstimada);
        charla.setPublicoObjetivo(publicoObjetivo);

        // Procesar disertantes (separados por coma)
        if (disertantesStr != null && !disertantesStr.trim().isEmpty()) {
            List<String> disertantes = new ArrayList<>();
            String[] partes = disertantesStr.split(",");
            for (String parte : partes) {
                String disertante = parte.trim();
                if (!disertante.isEmpty()) {
                    disertantes.add(disertante);
                }
            }
            charla.setDisertantes(disertantes);
        }

        return charla;
    }

    private Seminario crearSeminario(String nombre, String descripcion, Integer cupos, Double costo,
            String fechaInicio, String fechaFin, String fechaInicioInscripcion, String fechaFinInscripcion,
            String modalidad,
            String lugar, String enlace, String horaSeminario, Integer duracionMinutos,
            String disertantesStr, String publicoObjetivo) {
        Seminario seminario = new Seminario();
        configurarOfertaBase(seminario, nombre, descripcion, cupos, costo, fechaInicio, fechaFin,
                fechaInicioInscripcion, fechaFinInscripcion, modalidad, lugar, enlace);

        // Convertir y asignar hora de inicio
        if (horaSeminario != null && !horaSeminario.trim().isEmpty()) {
            try {
                seminario.setHoraInicio(java.sql.Time.valueOf(horaSeminario + ":00"));
            } catch (Exception e) {
                System.err.println("Error al convertir hora de seminario: " + e.getMessage());
            }
        }

        // Campos específicos del seminario
        if (duracionMinutos != null)
            seminario.setDuracionMinutos(duracionMinutos);
        seminario.setPublicoObjetivo(publicoObjetivo);

        // Procesar disertantes (separados por coma)
        if (disertantesStr != null && !disertantesStr.trim().isEmpty()) {
            List<String> disertantes = new ArrayList<>();
            String[] partes = disertantesStr.split(",");
            for (String parte : partes) {
                String disertante = parte.trim();
                if (!disertante.isEmpty()) {
                    disertantes.add(disertante);
                }
            }
            seminario.setDisertantes(disertantes);
        }

        return seminario;
    }

    private void configurarOfertaBase(OfertaAcademica oferta, String nombre, String descripcion,
            Integer cupos, Double costo, String fechaInicio,
            String fechaFin, String fechaInicioInscripcion, String fechaFinInscripcion,
            String modalidad, String lugar, String enlace) {
        oferta.setNombre(nombre);
        oferta.setDescripcion(descripcion);
        oferta.setLugar(lugar);
        oferta.setEnlace(enlace);

        if (cupos != null)
            oferta.setCupos(cupos);
        if (costo != null)
            oferta.setCostoInscripcion(costo);

        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            oferta.setFechaInicio(LocalDate.parse(fechaInicio));
        }

        if (fechaFin != null && !fechaFin.isEmpty()) {
            oferta.setFechaFin(LocalDate.parse(fechaFin));
        }

        if (fechaInicioInscripcion != null && !fechaInicioInscripcion.isEmpty()) {
            oferta.setFechaInicioInscripcion(LocalDate.parse(fechaInicioInscripcion));
        }

        if (fechaFinInscripcion != null && !fechaFinInscripcion.isEmpty()) {
            oferta.setFechaFinInscripcion(LocalDate.parse(fechaFinInscripcion));
        }

        // Configurar modalidad
        if (modalidad != null && !modalidad.isEmpty()) {
            oferta.setModalidad(Modalidad.valueOf(modalidad.toUpperCase()));
        }

        // Configurar estado por defecto
        oferta.setEstado(EstadoOferta.ACTIVA);
        oferta.setVisibilidad(true);
    }

    /**
     * Endpoint para modificar una oferta académica existente
     */
    @PostMapping("/admin/ofertas/modificar")
    @Auditable(action = "MODIFICAR_OFERTA", entity = "OfertaAcademica")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> modificarOferta(
            @RequestParam Long idOferta, // ID de la oferta a modificar
            @RequestParam String tipoOferta,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) Integer cupos,
            @RequestParam(required = false) Double costoInscripcion,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String fechaInicioInscripcion,
            @RequestParam(required = false) String fechaFinInscripcion,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String otorgaCertificado,
            @RequestParam(required = false) MultipartFile imagen,
            @RequestParam(required = false) String categorias,
            @RequestParam(required = false) String horarios,
            // Campos específicos para CURSO
            @RequestParam(required = false) String temario,
            @RequestParam(required = false) String docentesCurso,
            @RequestParam(required = false) Double costoCuota,
            @RequestParam(required = false) Double costoMora,
            @RequestParam(required = false) Integer nrCuotas,
            @RequestParam(required = false) Integer diaVencimiento,
            // Campos específicos para FORMACION
            @RequestParam(required = false) String planFormacion,
            @RequestParam(required = false) String docentesFormacion,
            @RequestParam(required = false) Double costoCuotaFormacion,
            @RequestParam(required = false) Double costoMoraFormacion,
            @RequestParam(required = false) Integer nrCuotasFormacion,
            @RequestParam(required = false) Integer diaVencimientoFormacion,
            // Campos específicos para CHARLA
            @RequestParam(required = false) String lugarCharla,
            @RequestParam(required = false) String enlaceCharla,
            @RequestParam(required = false) String horaCharla,
            @RequestParam(required = false) Integer duracionEstimada,
            @RequestParam(required = false) String disertantesCharla,
            @RequestParam(required = false) String publicoObjetivoCharla,
            // Campos específicos para SEMINARIO
            @RequestParam(required = false) String lugarSeminario,
            @RequestParam(required = false) String enlaceSeminario,
            @RequestParam(required = false) String horaSeminario,
            @RequestParam(required = false) Integer duracionMinutos,
            @RequestParam(required = false) String disertantesSeminario,
            @RequestParam(required = false) String publicoObjetivoSeminario,
            // Campos genéricos para lugar y enlace
            @RequestParam(required = false) String lugar,
            @RequestParam(required = false) String enlace) {

        try {
            System.out.println("MODIFICACIÓN DE OFERTA INICIADA");
            System.out.println("ID Oferta: " + idOferta);
            System.out.println("Tipo: " + tipoOferta);
            System.out.println("Nombre: " + nombre);

            // Unificar lugar y enlace si vienen en campos específicos
            if (lugar == null || lugar.trim().isEmpty()) {
                if ("CHARLA".equalsIgnoreCase(tipoOferta))
                    lugar = lugarCharla;
                else if ("SEMINARIO".equalsIgnoreCase(tipoOferta))
                    lugar = lugarSeminario;
            }
            if (enlace == null || enlace.trim().isEmpty()) {
                if ("CHARLA".equalsIgnoreCase(tipoOferta))
                    enlace = enlaceCharla;
                else if ("SEMINARIO".equalsIgnoreCase(tipoOferta))
                    enlace = enlaceSeminario;
            }

            // Buscar la oferta existente
            Optional<OfertaAcademica> ofertaOpt = ofertaAcademicaRepository.findById(idOferta);
            if (ofertaOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Oferta no encontrada");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            OfertaAcademica ofertaExistente = ofertaOpt.get();
            String nombreAnterior = ofertaExistente.getNombre();
            String descripcionAnterior = ofertaExistente.getDescripcion();
            Integer cuposAnterior = ofertaExistente.getCupos();
            Double costoAnterior = ofertaExistente.getCostoInscripcion();
            LocalDate fechaInicioAnterior = ofertaExistente.getFechaInicio();
            LocalDate fechaFinAnterior = ofertaExistente.getFechaFin();
            LocalDate fechaInicioInscripcionAnterior = ofertaExistente.getFechaInicioInscripcion();
            LocalDate fechaFinInscripcionAnterior = ofertaExistente.getFechaFinInscripcion();
            Modalidad modalidadAnterior = ofertaExistente.getModalidad();

            // Verificar si se puede modificar
            if (!ofertaExistente.puedeSerEditada()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No se puede modificar una oferta finalizada");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validaciones de fechas
            if (fechaInicio == null || fechaInicio.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "La fecha de inicio es obligatoria");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (fechaFin == null || fechaFin.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "La fecha de fin es obligatoria");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validar formato de fechas
            LocalDate fechaInicioDate, fechaFinDate;
            try {
                fechaInicioDate = LocalDate.parse(fechaInicio);
                fechaFinDate = LocalDate.parse(fechaFin);

                if (fechaInicioDate.isAfter(fechaFinDate)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "La fecha de inicio no puede ser posterior a la fecha de fin");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Formato de fecha inválido. Use AAAA-MM-DD");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validar fechas de inscripción si se proporcionan
            if ((fechaInicioInscripcion != null && !fechaInicioInscripcion.trim().isEmpty()) ||
                    (fechaFinInscripcion != null && !fechaFinInscripcion.trim().isEmpty())) {
                if (fechaInicioInscripcion == null || fechaInicioInscripcion.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "La fecha de inicio de inscripción es obligatoria");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                if (fechaFinInscripcion == null || fechaFinInscripcion.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "La fecha de fin de inscripción es obligatoria");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                try {
                    LocalDate fechaInicioInscripcionDate = LocalDate.parse(fechaInicioInscripcion);
                    LocalDate fechaFinInscripcionDate = LocalDate.parse(fechaFinInscripcion);
                    if (fechaInicioInscripcionDate.isAfter(fechaFinInscripcionDate)) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message",
                                "La fecha de inicio de inscripción no puede ser posterior a la fecha de fin de inscripción");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                } catch (Exception e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Formato de fecha de inscripción inválido. Use AAAA-MM-DD");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Validar lugar y enlace según modalidad
            // IMPORTANTE: Enlace obligatorio SOLO para CHARLA y SEMINARIO en modalidad
            // virtual/híbrida
            boolean esVirtual = "VIRTUAL".equalsIgnoreCase(modalidad) || "HIBRIDA".equalsIgnoreCase(modalidad);
            boolean esPresencial = "PRESENCIAL".equalsIgnoreCase(modalidad) || "HIBRIDA".equalsIgnoreCase(modalidad);
            boolean esCharlaOSeminario = "CHARLA".equalsIgnoreCase(tipoOferta)
                    || "SEMINARIO".equalsIgnoreCase(tipoOferta);

            // Validar enlace si es virtual/híbrida SOLO para Charla y Seminario
            if (esVirtual && esCharlaOSeminario) {
                if (enlace == null || enlace.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message",
                            "Para modalidad Virtual o Híbrida en Charlas y Seminarios, el enlace es obligatorio");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Validar URL si se proporciona enlace
            if (enlace != null && !enlace.trim().isEmpty()) {
                try {
                    new java.net.URL(enlace);
                } catch (java.net.MalformedURLException e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "El enlace proporcionado no es una URL válida");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Validar lugar si es presencial/híbrida
            if (esPresencial) {
                if (lugar == null || lugar.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Para modalidad Presencial o Híbrida, el lugar es obligatorio");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Validar que el tipo de oferta coincida (Comparación robusta por instancia)
            boolean tipoCoincide = false;
            String tipoOfertaUpper = tipoOferta.toUpperCase();

            switch (tipoOfertaUpper) {
                case "CURSO":
                    tipoCoincide = ofertaExistente instanceof Curso;
                    break;
                case "FORMACION":
                    tipoCoincide = ofertaExistente instanceof Formacion;
                    break;
                case "CHARLA":
                    tipoCoincide = ofertaExistente instanceof Charla;
                    break;
                case "SEMINARIO":
                    tipoCoincide = ofertaExistente instanceof Seminario;
                    break;
                default:
                    tipoCoincide = false;
            }

            if (!tipoCoincide) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No se puede cambiar el tipo de oferta existente ("
                        + ofertaExistente.getTipoOferta() + " vs " + tipoOferta + ")");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Llamar al servicio apropiado según el tipo de oferta
            OfertaAcademica ofertaModificada = null;

            // Lógica de Imagen (Previa a la actualización del modelo)
            String nuevaImagenUrl = null;
            if (imagen != null && !imagen.isEmpty()) {
                try {
                    nuevaImagenUrl = guardarImagenOferta(imagen);
                    System.out.println("✅ Imagen actualizada: " + nuevaImagenUrl);
                } catch (Exception e) {
                    System.err.println("❌ Error al actualizar imagen: " + e.getMessage());
                }
            }

            // Preparar mapa de datos para delegar la actualización al modelo
            // (Refactorización)
            Map<String, Object> datosActualizar = new HashMap<>();

            // Datos comunes
            if (nombre != null)
                datosActualizar.put("nombre", nombre);
            if (descripcion != null)
                datosActualizar.put("descripcion", descripcion);
            datosActualizar.put("fechaInicio", fechaInicio); // Ya validados no nulos como String o LocalDate
            datosActualizar.put("fechaFin", fechaFin);
            if (fechaInicioInscripcion != null && !fechaInicioInscripcion.trim().isEmpty()) {
                datosActualizar.put("fechaInicioInscripcion", fechaInicioInscripcion);
            }
            if (fechaFinInscripcion != null && !fechaFinInscripcion.trim().isEmpty()) {
                datosActualizar.put("fechaFinInscripcion", fechaFinInscripcion);
            }

            // Lugar y Enlace pueden ser seteados a vacío, así que los pasamos directo
            datosActualizar.put("lugar", lugar);
            datosActualizar.put("enlace", enlace);

            if (cupos != null)
                datosActualizar.put("cupos", cupos);
            if (modalidad != null)
                datosActualizar.put("modalidad", modalidad);
            if (otorgaCertificado != null)
                datosActualizar.put("certificado", otorgaCertificado);
            if (costoInscripcion != null)
                datosActualizar.put("costoInscripcion", costoInscripcion);

            if (nuevaImagenUrl != null) {
                datosActualizar.put("imagenUrl", nuevaImagenUrl);
            }

            // Datos específicos por tipo
            if ("CURSO".equals(tipoOfertaUpper)) {
                if (temario != null)
                    datosActualizar.put("temario", temario);
                if (costoCuota != null)
                    datosActualizar.put("costoCuota", costoCuota);
                if (costoMora != null)
                    datosActualizar.put("costoMora", costoMora);
                if (nrCuotas != null)
                    datosActualizar.put("nrCuotas", nrCuotas);
                if (diaVencimiento != null)
                    datosActualizar.put("diaVencimiento", diaVencimiento);

                if (docentesCurso != null && !docentesCurso.trim().isEmpty()) {
                    datosActualizar.put("docentes", obtenerDocentesPorIds(docentesCurso));
                }

            } else if ("FORMACION".equals(tipoOfertaUpper)) {
                if (planFormacion != null)
                    datosActualizar.put("plan", planFormacion);
                if (costoCuotaFormacion != null)
                    datosActualizar.put("costoCuota", costoCuotaFormacion);
                if (costoMoraFormacion != null)
                    datosActualizar.put("costoMora", costoMoraFormacion);
                if (nrCuotasFormacion != null)
                    datosActualizar.put("nrCuotas", nrCuotasFormacion);
                if (diaVencimientoFormacion != null)
                    datosActualizar.put("diaVencimiento", diaVencimientoFormacion);

                // Si hubiera lógica de docentes para formación
                if (docentesFormacion != null && !docentesFormacion.trim().isEmpty()) {
                    // datosActualizar.put("docentes", obtenerDocentesPorIds(docentesFormacion));
                }

            } else if ("CHARLA".equals(tipoOfertaUpper)) {
                if (duracionEstimada != null)
                    datosActualizar.put("duracionEstimada", duracionEstimada);
                if (publicoObjetivoCharla != null)
                    datosActualizar.put("publicoObjetivo", publicoObjetivoCharla);
                if (disertantesCharla != null)
                    datosActualizar.put("disertantes", disertantesCharla);
                if (horaCharla != null && !horaCharla.trim().isEmpty()) {
                    try {
                        datosActualizar.put("horaInicio", java.sql.Time.valueOf(horaCharla + ":00"));
                    } catch (Exception e) {
                        System.err.println("Error al convertir hora de charla: " + e.getMessage());
                    }
                }

            } else if ("SEMINARIO".equals(tipoOfertaUpper)) {
                if (duracionMinutos != null)
                    datosActualizar.put("duracionMinutos", duracionMinutos);
                if (publicoObjetivoSeminario != null)
                    datosActualizar.put("publicoObjetivo", publicoObjetivoSeminario);
                if (disertantesSeminario != null)
                    datosActualizar.put("disertantes", disertantesSeminario);
                if (horaSeminario != null && !horaSeminario.trim().isEmpty()) {
                    try {
                        datosActualizar.put("horaInicio", java.sql.Time.valueOf(horaSeminario + ":00"));
                    } catch (Exception e) {
                        System.err.println("Error al convertir hora de seminario: " + e.getMessage());
                    }
                }
            }

            // Ejecutar actualización en el modelo
            ofertaExistente.actualizarDatos(datosActualizar);

            // Guardar la oferta modificada
            ofertaModificada = ofertaAcademicaRepository.save(ofertaExistente);

            // Asociar categorías si se proporcionaron
            if (categorias != null && !categorias.trim().isEmpty()) {
                asociarCategoriasAOferta(ofertaModificada, categorias);
            }

            if (ofertaModificada != null) {
                System.out.println("Oferta modificada exitosamente: " + ofertaModificada.getNombre());

                List<String> cambios = new ArrayList<>();
                addCambio(cambios, "nombre", nombreAnterior, ofertaModificada.getNombre());
                addCambio(cambios, "descripcion", descripcionAnterior, ofertaModificada.getDescripcion());
                addCambio(cambios, "cupos", cuposAnterior, ofertaModificada.getCupos());
                addCambio(cambios, "costo", costoAnterior, ofertaModificada.getCostoInscripcion());
                addCambio(cambios, "fechaInicio", fechaInicioAnterior, ofertaModificada.getFechaInicio());
                addCambio(cambios, "fechaFin", fechaFinAnterior, ofertaModificada.getFechaFin());
                addCambio(cambios, "fechaInicioInscripcion", fechaInicioInscripcionAnterior,
                        ofertaModificada.getFechaInicioInscripcion());
                addCambio(cambios, "fechaFinInscripcion", fechaFinInscripcionAnterior,
                        ofertaModificada.getFechaFinInscripcion());
                addCambio(cambios, "modalidad", modalidadAnterior, ofertaModificada.getModalidad());

                String detalleCambios = "Modificacion de oferta " + ofertaModificada.getTipoOferta() + ": " +
                        trunc(ofertaModificada.getNombre(), 80) + " (ID " + ofertaModificada.getIdOferta() + ")" +
                        (cambios.isEmpty() ? " | sin cambios detectados" : " | " + String.join("; ", cambios));

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Oferta modificada exitosamente");
                response.put("auditDetails", detalleCambios);
                response.put("oferta", mapearOfertaAResponse(ofertaModificada));

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Error al modificar la oferta");
                return ResponseEntity.badRequest().body(errorResponse);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al modificar oferta: " + e.toString());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al modificar oferta: " + e.toString()); // Usamos toString para ver el
                                                                                        // tipo de excepción si mensaje
                                                                                        // es null
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Endpoint para generar propuestas automáticas de horarios para una oferta
     * académica
     */
    @PostMapping("/admin/ofertas/generar-horarios-automaticos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generarHorariosAutomaticos(
            @RequestParam(required = false) Long idOferta,
            @RequestParam String idDocente,
            @RequestParam(required = false) String docentesIds,
            @RequestParam Double horasSemanales,
            @RequestParam(defaultValue = "4") int maxHorasDiarias,
            @RequestParam(required = false) String horariosFijadosJson,
            @RequestParam(defaultValue = "false") boolean buscarAlternativas) {

        try {
            System.out.println("📅 Generando propuestas automáticas de horarios...");
            System.out.println("   - Oferta ID: " + idOferta);
            System.out.println("   - Docente ID: " + idDocente);
            System.out.println("   - Horas semanales: " + horasSemanales);
            System.out.println("   - Max Horas/Día: " + maxHorasDiarias);
            System.out.println("   - Buscar Alternativas: " + buscarAlternativas);

            // Validaciones
            if (idDocente == null || horasSemanales == null || horasSemanales <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Parámetros inválidos: docente y horas semanales son requeridos");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Buscar docente
            Docente docente = docenteRepository.findById(java.util.UUID.fromString(idDocente))
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

            List<Docente> docentes = new ArrayList<>();
            if (docentesIds != null && !docentesIds.isBlank()) {
                String[] ids = docentesIds.split(",");
                for (String s : ids) {
                    try {
                        UUID did = UUID.fromString(s.trim());
                        docenteRepository.findById(did).ifPresent(docentes::add);
                    } catch (Exception e) {
                        // ignore invalid id
                    }
                }
            }
            if (docentes.isEmpty()) {
                docentes.add(docente);
            }

            // Buscar oferta (solo si existe - para ofertas existentes)
            OfertaAcademica oferta = null;
            if (idOferta != null && idOferta > 0) {
                oferta = ofertaAcademicaRepository.findById(idOferta).orElse(null);
            }

            // Procesar horarios fijados (pinned)
            List<GeneradorHorariosService.HorarioAsignado> pinned = new ArrayList<>();
            if (horariosFijadosJson != null && !horariosFijadosJson.isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, String>> rawList = mapper.readValue(horariosFijadosJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {
                            });

                    for (Map<String, String> m : rawList) {
                        try {
                            String horaInicioStr = m.get("horaInicio");
                            String horaFinStr = m.get("horaFin");

                            // Unificar formato a HH:mm:ss para Time.valueOf
                            if (horaInicioStr.length() == 5)
                                horaInicioStr += ":00"; // HH:mm -> HH:mm:ss
                            if (horaFinStr.length() == 5)
                                horaFinStr += ":00";

                            GeneradorHorariosService.HorarioAsignado ha = new GeneradorHorariosService.HorarioAsignado(
                                    Dias.valueOf(m.get("dia")),
                                    Time.valueOf(horaInicioStr),
                                    Time.valueOf(horaFinStr));
                            if (m.get("docenteId") != null) {
                                ha.setDocenteId(m.get("docenteId"));
                            }
                            if (m.get("docentesIds") != null) {
                                ha.setDocenteId(null);
                                ha.setDocentesIds(m.get("docentesIds"));
                            }
                            pinned.add(ha);
                        } catch (Exception e) {
                            System.err.println(
                                    "   ! Error al procesar horario fijado individual: " + m + " - " + e.getMessage());
                        }
                    }
                    System.out.println("   - Horarios fijados procesados correctamente: " + pinned.size());
                } catch (Exception e) {
                    System.err.println("Error parseando JSON de horarios fijados: " + e.getMessage());
                }
            }

            // Generar propuestas
            List<GeneradorHorariosService.PropuestaHorario> propuestas = (docentes.size() > 1)
                    ? generadorHorariosService.generarPropuestasMulti(oferta, docentes, horasSemanales, maxHorasDiarias,
                            pinned, buscarAlternativas)
                    : generadorHorariosService.generarPropuestas(oferta, docente, horasSemanales, maxHorasDiarias,
                            pinned, buscarAlternativas);

            if (propuestas.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                // Mensaje especifico si no hay opciones
                String msg = pinned.isEmpty()
                        ? "No se pudieron generar propuestas. El docente no tiene suficiente disponibilidad."
                        : "No se encontraron otras alternativas con las restricciones y horarios fijados seleccionados.";
                errorResponse.put("message", msg);

                // Info diagnostico de disponibilidad
                double cargaActual = disponibilidadDocenteService.calcularCargaHorariaSemanal(docente);
                double disponibilidadTotal = disponibilidadDocenteService.calcularDisponibilidadTotalSemanal(docente);
                double disponibilidadLibre = disponibilidadDocenteService.calcularDisponibilidadLibreSemanal(docente);

                Map<String, Object> debug = new HashMap<>();
                debug.put("horasRequeridas", horasSemanales);
                debug.put("maxHorasDiarias", maxHorasDiarias);
                debug.put("cargaActual", Math.round(cargaActual * 100.0) / 100.0);
                debug.put("disponibilidadTotal", Math.round(disponibilidadTotal * 100.0) / 100.0);
                debug.put("disponibilidadLibre", Math.round(disponibilidadLibre * 100.0) / 100.0);
                debug.put("disponibilidadLibrePorDia",
                        disponibilidadDocenteService.calcularDisponibilidadLibrePorDia(docente));
                errorResponse.put("debug", debug);

                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Convertir propuestas a JSON
            List<Map<String, Object>> propuestasJSON = new ArrayList<>();
            for (GeneradorHorariosService.PropuestaHorario propuesta : propuestas) {
                Map<String, Object> propuestaMap = new HashMap<>();
                propuestaMap.put("nombre", propuesta.getNombre());
                propuestaMap.put("descripcion", propuesta.getDescripcion());
                propuestaMap.put("horarios", propuesta.toJSON());
                propuestaMap.put("totalHorasSemana", Math.round(propuesta.getTotalHorasSemana() * 100.0) / 100.0);
                propuestaMap.put("cantidadDias", propuesta.getCantidadDias());
                propuestaMap.put("promedioHorasPorDia", Math.round(propuesta.getPromedioHorasPorDia() * 100.0) / 100.0);
                propuestaMap.put("cargaAdicionalDocente",
                        Math.round(propuesta.getCargaAdicionalDocente() * 100.0) / 100.0);
                propuestaMap.put("porcentajeCargaTotal",
                        Math.round(propuesta.getPorcentajeCargaTotal() * 100.0) / 100.0);
                propuestaMap.put("score", Math.round(propuesta.getScore() * 100.0) / 100.0);
                propuestasJSON.add(propuestaMap);
            }

            // Información adicional del docente
            double cargaActual = disponibilidadDocenteService.calcularCargaHorariaSemanal(docente);
            double disponibilidadTotal = disponibilidadDocenteService.calcularDisponibilidadTotalSemanal(docente);
            double porcentajeOcupacion = disponibilidadDocenteService.calcularPorcentajeOcupacion(docente);

            Map<String, Object> infoDocente = new HashMap<>();
            infoDocente.put("nombre", docente.getNombre() + " " + docente.getApellido());
            infoDocente.put("cargaActual", Math.round(cargaActual * 100.0) / 100.0);
            infoDocente.put("disponibilidadTotal", Math.round(disponibilidadTotal * 100.0) / 100.0);
            infoDocente.put("porcentajeOcupacion", Math.round(porcentajeOcupacion * 100.0) / 100.0);

            // Agregar detalle de disponibilidad para validación en frontend
            try {
                // Mapa DIA -> Lista de rangos [{start: "08:00", end: "12:00"}]
                Map<String, List<Map<String, String>>> disponibilidadDetallada = new HashMap<>();
                List<DisponibilidadDocente> dispoList = disponibilidadDocenteService.obtenerDisponibilidades(docente);

                for (DisponibilidadDocente d : dispoList) {
                    String dia = d.getDia().toString();
                    if (!disponibilidadDetallada.containsKey(dia)) {
                        disponibilidadDetallada.put(dia, new ArrayList<>());
                    }
                    Map<String, String> rango = new HashMap<>();
                    String inicio = d.getHoraInicio().toString();
                    String fin = d.getHoraFin().toString();
                    if (inicio.length() == 8)
                        inicio = inicio.substring(0, 5); // 08:00:00 -> 08:00
                    if (fin.length() == 8)
                        fin = fin.substring(0, 5);
                    rango.put("inicio", inicio);
                    rango.put("fin", fin);
                    disponibilidadDetallada.get(dia).add(rango);
                }
                infoDocente.put("disponibilidadDetallada", disponibilidadDetallada);
            } catch (Exception e) {
                System.err.println("Error procesando disponibilidad detallada: " + e.getMessage());
            }

            List<Map<String, Object>> docentesInfo = new ArrayList<>();
            for (Docente dref : docentes) {
                try {
                    double carga = disponibilidadDocenteService.calcularCargaHorariaSemanal(dref);
                    double dispoTotal = disponibilidadDocenteService.calcularDisponibilidadTotalSemanal(dref);
                    double porc = disponibilidadDocenteService.calcularPorcentajeOcupacion(dref);

                    Map<String, Object> dMap = new HashMap<>();
                    dMap.put("id", dref.getId().toString());
                    dMap.put("nombre", dref.getNombre() + " " + dref.getApellido());
                    dMap.put("cargaActual", Math.round(carga * 100.0) / 100.0);
                    dMap.put("disponibilidadTotal", Math.round(dispoTotal * 100.0) / 100.0);
                    dMap.put("porcentajeOcupacion", Math.round(porc * 100.0) / 100.0);

                    Map<String, List<Map<String, String>>> disponibilidadDetallada = new HashMap<>();
                    List<DisponibilidadDocente> dispoList = disponibilidadDocenteService.obtenerDisponibilidades(dref);

                    for (DisponibilidadDocente dd : dispoList) {
                        String dia = dd.getDia().toString();
                        if (!disponibilidadDetallada.containsKey(dia)) {
                            disponibilidadDetallada.put(dia, new ArrayList<>());
                        }
                        Map<String, String> rango = new HashMap<>();
                        String inicio = dd.getHoraInicio().toString();
                        String fin = dd.getHoraFin().toString();
                        if (inicio.length() == 8)
                            inicio = inicio.substring(0, 5); // 08:00:00 -> 08:00
                        if (fin.length() == 8)
                            fin = fin.substring(0, 5);
                        rango.put("inicio", inicio);
                        rango.put("fin", fin);
                        disponibilidadDetallada.get(dia).add(rango);
                    }
                    dMap.put("disponibilidadDetallada", disponibilidadDetallada);

                    docentesInfo.add(dMap);
                } catch (Exception e) {
                    System.err.println("Error procesando disponibilidad detallada docente: " + e.getMessage());
                }
            }
            infoDocente.put("docentes", docentesInfo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Propuestas generadas exitosamente");
            response.put("propuestas", propuestasJSON);
            response.put("infoDocente", infoDocente);

            System.out.println("✅ " + propuestas.size() + " propuestas generadas exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error al generar propuestas: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al generar propuestas: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/admin/ofertas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarOfertas() {
        try {
            List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findAll();

            // Validación defensiva: eliminar ofertas nulas
            if (ofertas != null) {
                ofertas.removeIf(Objects::isNull);
            }

            List<Map<String, Object>> ofertasResponse = new ArrayList<>();
            for (OfertaAcademica oferta : ofertas) {
                if (oferta != null) { // Validación adicional
                    ofertasResponse.add(mapearOfertaAResponse(oferta));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ofertasResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener ofertas: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/ofertas/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarOfertasAjax() {
        try {
            List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findAll();

            // Validación defensiva: eliminar ofertas nulas
            if (ofertas != null) {
                ofertas.removeIf(Objects::isNull);
            }

            List<Map<String, Object>> ofertasResponse = new ArrayList<>();
            for (OfertaAcademica oferta : ofertas) {
                if (oferta != null) { // Validación adicional
                    ofertasResponse.add(mapearOfertaAResponse(oferta));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ofertasResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener ofertas: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // =================== ENDPOINTS PARA USUARIOS ===================

    @PostMapping("/admin/usuarios/registrar")
    @Auditable(action = "ALTA_USUARIO", entity = "Usuario")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarUsuario(
            @RequestParam String dni,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam LocalDate fechaNacimiento,
            @RequestParam TipoGenero genero,
            @RequestParam String paisCodigo,
            @RequestParam String provinciaCodigo,
            @RequestParam String ciudadId,
            @RequestParam String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam String rol,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) Integer experiencia,
            @RequestParam(required = false) String horariosDisponibilidad, // ✅ Recibimos los horarios como JSON
            @RequestParam(required = false) String estado,
            @RequestParam(required = false, defaultValue = "true") Boolean notificacionesEmail,
            @RequestParam(required = false, defaultValue = "false") Boolean cambiarPasswordPrimerAcceso,
            @RequestParam(required = false) String colegioEgreso,
            @RequestParam(required = false) Integer añoEgreso,
            @RequestParam(required = false) String ultimosEstudios) {

        try {
            System.out.println("Registrando usuario desde admin:");
            System.out.println("   - DNI: " + dni);
            System.out.println("   - Nombre: " + nombre);
            System.out.println("   - Apellido: " + apellido);
            System.out.println("   - Fecha Nacimiento: " + fechaNacimiento);
            System.out.println("   - Rol: " + rol);
            System.out.println("   - Horarios: " + horariosDisponibilidad); // ✅ Debug horarios

            // Validar fecha de nacimiento
            if (fechaNacimiento == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "La fecha de nacimiento es obligatoria");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar edad mínima (16 años)
            Period edad = Period.between(fechaNacimiento, LocalDate.now());
            if (edad.getYears() < 16) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "El usuario debe tener al menos 16 años");
                return ResponseEntity.badRequest().body(response);
            }

            // Convertir ciudadId a Long (opcional)
            Long ciudadIdLong = (ciudadId != null && !ciudadId.isBlank()) ? Long.valueOf(ciudadId) : null;

            // ✅ Procesar horarios si es docente y hay horarios
            List<Map<String, String>> horariosList = new ArrayList<>();
            if ("DOCENTE".equals(rol) && horariosDisponibilidad != null && !horariosDisponibilidad.isEmpty()) {
                try {
                    // Parsear el JSON de horarios
                    ObjectMapper objectMapper = new ObjectMapper();
                    horariosList = objectMapper.readValue(horariosDisponibilidad,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

                    System.out.println("Horarios procesados: " + horariosList.size());
                    for (Map<String, String> horario : horariosList) {
                        System.out.println("   - " + horario.get("diaSemana") + ": " +
                                horario.get("horaInicio") + " - " + horario.get("horaFin"));
                    }
                } catch (Exception e) {
                    System.out.println("Error parseando horarios: " + e.getMessage());
                }
            }

            // Usar el servicio unificado - MODIFICAR EL SERVICIO PARA ACEPTAR HORARIOS
            Usuario nuevoUsuario = registroService.registrarUsuarioAdministrativo(
                    dni, nombre, apellido, fechaNacimiento, genero,
                    correo, telefono, paisCodigo, provinciaCodigo,
                    ciudadIdLong, rol, matricula,
                    experiencia, colegioEgreso, añoEgreso, ultimosEstudios,
                    horariosList // ✅ Pasar los horarios al servicio
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message",
                    "Usuario registrado exitosamente. Las credenciales han sido enviadas al correo electrónico.");
            response.put("auditDetails",
                    "Creacion de usuario: " + nombre + " " + apellido + " (DNI " + dni + ")" +
                            (correo != null ? " | correo: " + correo : "") +
                            (telefono != null ? " | telefono: " + telefono : "") +
                            (rol != null ? " | rol: " + rol : "") +
                            (estado != null ? " | estado: " + estado : ""));
            response.put("id", nuevoUsuario.getId());
            response.put("dni", nuevoUsuario.getDni());
            response.put("nombre", nuevoUsuario.getNombre() + " " + nuevoUsuario.getApellido());
            response.put("rol", rol);

            System.out.println("✅ Usuario registrado exitosamente desde admin: " + nuevoUsuario.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Error al registrar usuario desde admin: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al registrar usuario: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/usuarios/{identificador}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> obtenerUsuarioPorIdentificador(@PathVariable String identificador) {
        Map<String, Object> response = new HashMap<>();

        Optional<Usuario> usuarioOpt = buscarUsuarioPorIdentificador(identificador);
        if (usuarioOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Usuario no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        response.put("success", true);
        response.put("data", mapearDetalleUsuario(usuarioOpt.get()));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/admin/usuarios/{identificador}")
    @Auditable(action = "MODIFICACION_USUARIO", entity = "Usuario")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarUsuario(
            @PathVariable String identificador,
            @RequestParam String dni,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam LocalDate fechaNacimiento,
            @RequestParam TipoGenero genero,
            @RequestParam String paisCodigo,
            @RequestParam String provinciaCodigo,
            @RequestParam String ciudadId,
            @RequestParam String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam String rol,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) Integer experiencia,
            @RequestParam(required = false) String horariosDisponibilidad,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false, defaultValue = "false") Boolean notificacionesEmail,
            @RequestParam(required = false) String colegioEgreso,
            @RequestParam(required = false) Integer añoEgreso,
            @RequestParam(required = false) String ultimosEstudios) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("📝 Actualizando usuario: " + identificador);
            System.out.println("📍 Datos de ubicación recibidos: paisCodigo=" + paisCodigo + ", provinciaCodigo="
                    + provinciaCodigo + ", ciudadId=" + ciudadId);
            if ("DOCENTE".equalsIgnoreCase(rol)) {
                System.out.println("👨‍🏫 Datos de docente recibidos: matricula=" + matricula + ", experiencia="
                        + experiencia + ", horarios="
                        + (horariosDisponibilidad != null
                                ? horariosDisponibilidad.substring(0, Math.min(100, horariosDisponibilidad.length()))
                                : "null"));
            }

            Optional<Usuario> usuarioOpt = buscarUsuarioPorIdentificador(identificador);
            if (usuarioOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                response.put("auditDetails", "No se pudo dar de baja: usuario no encontrado (ident " + identificador + ").");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Usuario usuarioPrevio = usuarioOpt.get();
            String rolAnterior = usuarioPrevio.getRoles() != null && !usuarioPrevio.getRoles().isEmpty()
                    ? usuarioPrevio.getRoles().iterator().next().getNombre()
                    : "";
            String estadoAnterior = usuarioPrevio.getEstado();

            Long ciudadIdLong = (ciudadId != null && !ciudadId.isBlank()) ? Long.valueOf(ciudadId) : null;

            Usuario actualizado = registroService.actualizarUsuarioAdministrativo(
                    usuarioPrevio,
                    dni,
                    nombre,
                    apellido,
                    fechaNacimiento,
                    genero,
                    correo,
                    telefono,
                    paisCodigo,
                    provinciaCodigo,
                    ciudadIdLong,
                    rol,
                    matricula,
                    experiencia,
                    colegioEgreso,
                    añoEgreso,
                    ultimosEstudios,
                    horariosDisponibilidad,
                    estado);

            List<String> cambios = new ArrayList<>();
            addCambio(cambios, "dni", usuarioPrevio.getDni(), dni);
            addCambio(cambios, "nombre", usuarioPrevio.getNombre(), nombre);
            addCambio(cambios, "apellido", usuarioPrevio.getApellido(), apellido);
            addCambio(cambios, "fechaNacimiento", usuarioPrevio.getFechaNacimiento(), fechaNacimiento);
            addCambio(cambios, "genero", usuarioPrevio.getGenero(), genero);
            addCambio(cambios, "correo", usuarioPrevio.getCorreo(), correo);
            addCambio(cambios, "telefono", usuarioPrevio.getNumTelefono(), telefono);
            addCambio(cambios, "rol", rolAnterior, rol);
            if (estado != null) {
                addCambio(cambios, "estado", estadoAnterior, estado);
            }

            String detalleCambios = "Modificacion de usuario: " + nombre + " " + apellido +
                    " (DNI " + dni + ", ident " + identificador + ")" +
                    (cambios.isEmpty() ? " | sin cambios detectados" : " | " + String.join("; ", cambios));

            response.put("success", true);
            response.put("message", "Usuario actualizado correctamente");
            response.put("auditDetails", detalleCambios);
            response.put("data", mapearDetalleUsuario(actualizado));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al actualizar usuario: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/admin/usuarios/{identificador}/foto")
    @Auditable(action = "ELIMINAR_FOTO_USUARIO", entity = "Usuario")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarFotoUsuario(@PathVariable String identificador) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Usuario> usuarioOpt = buscarUsuarioPorIdentificador(identificador);
            if (usuarioOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Usuario usuario = usuarioOpt.get();
            String fotoUrl = usuario.getFoto();
            if (fotoUrl == null || fotoUrl.isBlank()) {
                response.put("success", false);
                response.put("message", "El usuario no tiene foto");
                return ResponseEntity.badRequest().body(response);
            }

            Long idImagen = extraerIdImagenUsuario(fotoUrl);
            if (idImagen != null) {
                try {
                    usuarioImagenService.eliminarImagenUsuario(idImagen);
                } catch (Exception e) {
                    System.err.println("⚠️ No se pudo eliminar imagen en BD: " + e.getMessage());
                }
            }

            usuario.setFoto(null);
            usuarioRepository.save(usuario);

            response.put("success", true);
            response.put("message", "Foto eliminada correctamente");
            response.put("data", mapearDetalleUsuario(usuario));
            response.put("auditDetails",
                    "Eliminacion de foto de usuario: " + usuario.getNombre() + " " + usuario.getApellido() +
                            " (DNI " + usuario.getDni() + ", ident " + identificador + ")");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar foto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/admin/alumnos/{idAlumno}/toggle-documentacion")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleDocumentacionAlumno(@PathVariable UUID idAlumno) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(idAlumno);
            if (usuarioOpt.isEmpty() || !(usuarioOpt.get() instanceof Alumno)) {
                response.put("success", false);
                response.put("message", "Alumno no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Alumno alumno = (Alumno) usuarioOpt.get();
            boolean nuevoEstado = Boolean.FALSE.equals(alumno.getDocumentacionEntregada());
            alumno.setDocumentacionEntregada(nuevoEstado);
            usuarioRepository.save(alumno);

            response.put("success", true);
            response.put("nuevoEstado", nuevoEstado);
            response.put("message", "Estado de documentación del alumno actualizado");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar documentación: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/admin/inscripciones/{idInscripcion}/cancelar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelarInscripcion(@PathVariable Long idInscripcion) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Inscripciones> inscripcionOpt = inscripcionRepository.findById(idInscripcion);
            if (inscripcionOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Inscripción no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Inscripciones inscripcion = inscripcionOpt.get();
            inscripcion.setEstadoInscripcion(false);
            inscripcionRepository.save(inscripcion);

            response.put("success", true);
            response.put("message", "Inscripción cancelada correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al cancelar inscripción: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/admin/usuarios/{identificador}")
    @Auditable(action = "BAJA_USUARIO", entity = "Usuario")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarUsuario(@PathVariable String identificador) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Usuario> usuarioOpt = buscarUsuarioPorIdentificador(identificador);
            if (usuarioOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Validar que no se elimine a sí mismo
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            // currentUsername es el DNI según CustomUsuarioDetails
            if (usuarioOpt.get().getDni().equals(currentUsername)) {
                response.put("success", false);
                String motivo = "No puedes dar de baja a tu propia cuenta de usuario.";
                response.put("message", motivo);
                response.put("auditDetails", motivo + " (DNI " + usuarioOpt.get().getDni() + ").");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            registroService.eliminarUsuarioAdministrativo(usuarioOpt.get());

            response.put("success", true);
            response.put("message", "Usuario dado de baja correctamente");
            response.put("auditDetails",
                    "Baja de usuario: " + usuarioOpt.get().getNombre() + " " + usuarioOpt.get().getApellido() +
                            " (DNI " + usuarioOpt.get().getDni() + ", ident " + identificador + ")");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            response.put("success", false);
            String motivo = ex.getMessage();
            response.put("message", motivo);
            response.put("auditDetails", motivo);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (DataIntegrityViolationException ex) {
            response.put("success", false);
            String motivo = "El usuario no puede eliminarse porque tiene registros asociados";
            response.put("message", motivo);
            response.put("auditDetails", motivo);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.put("success", false);
            String motivo = "Error al eliminar usuario: " + e.getMessage();
            response.put("message", motivo);
            response.put("auditDetails", motivo);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/admin/usuarios/{identificador}/reactivar")
    @Auditable(action = "REACTIVAR_USUARIO", entity = "Usuario")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reactivarUsuario(@PathVariable String identificador) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Usuario> usuarioOpt = buscarUsuarioPorIdentificador(identificador);
            if (usuarioOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            registroService.reactivarUsuarioAdministrativo(usuarioOpt.get());

            response.put("success", true);
            response.put("message", "Usuario reactivado correctamente");
            response.put("auditDetails",
                    "Reactivacion de usuario: " + usuarioOpt.get().getNombre() + " " + usuarioOpt.get().getApellido() +
                            " (DNI " + usuarioOpt.get().getDni() + ", ident " + identificador + ")");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al reactivar usuario: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/usuarios/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarUsuarios(
            @RequestParam(defaultValue = "0") int page, // ✅ CAMBIAR A 0 POR DEFECTO
            @RequestParam(defaultValue = "10") int size) {

        try {
            System.out.println("📋 Solicitando usuarios REALES - página: " + page + ", tamaño: " + size);

            // OBTENER USUARIOS REALES DE LA BASE DE DATOS
            List<Usuario> todosUsuarios = usuarioRepository.findAll();
            System.out.println("👥 Usuarios encontrados en BD: " + todosUsuarios.size());

            // ✅ CORREGIR PAGINACIÓN - VERIFICAR LÍMITES
            int start = page * size;
            int end = Math.min(start + size, todosUsuarios.size());

            // ✅ EVITAR ERROR CUANDO START ES MAYOR QUE EL TAMAÑO DE LA LISTA
            if (start >= todosUsuarios.size()) {
                start = 0; // Volver a la primera página
                page = 0;
            }

            List<Usuario> usuariosPagina = todosUsuarios.subList(start, end);

            System.out.println("📄 Usuarios en esta página: " + usuariosPagina.size() + " (start: " + start + ", end: "
                    + end + ")");

            // Convertir usuarios reales a formato para frontend
            List<Map<String, Object>> usuariosResponse = new ArrayList<>();

            for (Usuario usuario : usuariosPagina) {
                Map<String, Object> usuarioMap = new HashMap<>();
                usuarioMap.put("dni", usuario.getDni());
                usuarioMap.put("nombreCompleto", usuario.getNombre() + " " + usuario.getApellido());
                usuarioMap.put("correo", usuario.getCorreo());
                usuarioMap.put("foto", usuario.getFoto());
                usuarioMap.put("estado", usuario.isEstado() ? "ACTIVO" : "INACTIVO");
                if (usuario.getFechaRegistro() != null) {
                    usuarioMap.put("fechaRegistro", usuario.getFechaRegistro().toString());
                } else {
                    // Si no existe, usar fecha por defecto o campo alternativo
                    usuarioMap.put("fechaRegistro", "Fecha no disponible");
                }

                // Obtener roles reales
                List<String> roles = new ArrayList<>();
                if (usuario.getRoles() != null) {
                    for (Rol rol : usuario.getRoles()) {
                        if (rol != null && rol.getNombre() != null) {
                            String nombreRol = convertirRolALegible(rol.getNombre());
                            roles.add(nombreRol);
                        }
                    }
                }
                usuarioMap.put("roles", roles);

                usuariosResponse.add(usuarioMap);

                System.out.println("✅ Usuario real: " + usuario.getNombre() + " " + usuario.getApellido() +
                        " - DNI: " + usuario.getDni() + " - Roles: " + roles);
            }

            // Crear respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            Map<String, Object> data = new HashMap<>();
            data.put("content", usuariosResponse);
            data.put("totalElements", todosUsuarios.size());
            data.put("totalPages", Math.max(1, (int) Math.ceil((double) todosUsuarios.size() / size))); // ✅ Mínimo 1
                                                                                                        // página
            data.put("size", size);
            data.put("number", page);

            response.put("data", data);

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("currentPage", page);
            pagination.put("totalPages", Math.max(1, (int) Math.ceil((double) todosUsuarios.size() / size)));
            pagination.put("totalElements", todosUsuarios.size());
            pagination.put("pageSize", size);

            response.put("pagination", pagination);

            System.out.println("✅ Respuesta enviada - " + usuariosResponse.size() + " usuarios reales");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ Error obteniendo usuarios reales: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener usuarios: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Método auxiliar para convertir roles a nombres legibles
    private String convertirRolALegible(String rol) {
        if (rol == null)
            return "Sin rol";

        switch (rol.toUpperCase()) {
            case "ALUMNO":
                return "Alumno";
            case "DOCENTE":
                return "Docente";
            case "ADMIN":
                return "Administrador";
            case "COORDINADOR":
                return "Coordinador";
            default:
                return rol;
        }
    }

    private Optional<Usuario> buscarUsuarioPorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }

        Optional<Usuario> usuarioPorDni = usuarioRepository.findByDni(identificador);
        if (usuarioPorDni.isPresent()) {
            return usuarioPorDni;
        }

        Optional<Usuario> usuarioPorCorreo = usuarioRepository.findByCorreo(identificador);
        if (usuarioPorCorreo.isPresent()) {
            return usuarioPorCorreo;
        }

        try {
            UUID uuid = UUID.fromString(identificador);
            return usuarioRepository.findById(uuid);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Object> mapearDetalleUsuario(Usuario usuario) {
        Map<String, Object> data = new HashMap<>();

        data.put("id", usuario.getId());
        data.put("dni", usuario.getDni());
        data.put("nombre", usuario.getNombre());
        data.put("apellido", usuario.getApellido());
        data.put("nombreCompleto", usuario.getNombre() + " " + usuario.getApellido());
        data.put("correo", usuario.getCorreo());
        data.put("foto", usuario.getFoto());
        data.put("telefono", usuario.getNumTelefono());
        data.put("fechaNacimiento", usuario.getFechaNacimiento());
        data.put("genero", usuario.getGenero());
        data.put("estado", usuario.getEstado());
        data.put("estadoBoolean", usuario.isEstado());
        data.put("fechaRegistro", usuario.getFechaRegistro());

        // Lógica de validación para baja
        boolean canBeDeleted = true;
        List<String> warnings = new ArrayList<>();
        String blockingReason = null;

        // Validar si el usuario es el mismo que está logueado
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        // currentUsername es el DNI
        if (usuario.getDni().equals(currentUsername)) {
            canBeDeleted = false;
            blockingReason = "No puedes dar de baja a tu propia cuenta de usuario.";
        }

        // Validar si es Admin
        boolean esAdmin = usuario.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getNombre()));
        if (esAdmin) {
            warnings.add("Este usuario es ADMINISTRADOR. Tenga cuidado al darlo de baja.");
        }

        // ===== DATOS ESPECÍFICOS DE ALUMNO =====
        if (usuario instanceof Alumno) {
            Alumno alumno = (Alumno) usuario;
            data.put("colegioEgreso", alumno.getColegioEgreso());
            data.put("añoEgreso", alumno.getAñoEgreso());
            data.put("ultimosEstudios", alumno.getUltimosEstudios());
            data.put("documentacionEntregada", alumno.getDocumentacionEntregada());

            // Validar inscripciones activas
            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumno(alumno);
            long activas = inscripciones.stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count();
            if (activas > 0) {
                warnings.add("El alumno tiene " + activas
                        + " inscripción(es) activa(s). Se recomienda verificar antes de dar de baja.");
            }

            // Mapear detalle de inscripciones para el panel de administración
            List<Map<String, Object>> inscripcionesDetalle = inscripciones.stream().map(insc -> {
                Map<String, Object> map = new HashMap<>();
                map.put("idInscripcion", insc.getIdInscripcion());
                map.put("ofertaTitulo", insc.getOferta().getNombre()); // Nombre de la oferta
                map.put("fechaInscripcion", insc.getFechaInscripcion());
                map.put("estadoInscripcion", insc.getEstadoInscripcion());
                map.put("documentacionEntregada",
                        insc.getDocumentacionEntregada() != null ? insc.getDocumentacionEntregada() : false);
                return map;
            }).collect(Collectors.toList());
            data.put("inscripciones", inscripcionesDetalle);
        }

        // ===== DATOS ESPECÍFICOS DE DOCENTE =====
        if (usuario instanceof Docente) {
            Docente docente = (Docente) usuario;
            data.put("matricula", docente.getMatricula());
            data.put("experiencia", docente.getExperienciaActualizada());
            data.put("experienciaBase", docente.getAñosExperiencia());

            // Validar cursos activos
            List<Curso> cursos = cursoRepository.findByDocentesId(docente.getId());
            for (Curso curso : cursos) {
                if (curso.getEstado() == EstadoOferta.ACTIVA || curso.getEstado() == EstadoOferta.ENCURSO) {
                    // REGLA ESTRICTA: Si está en un curso activo, BLOQUEAR.
                    canBeDeleted = false;
                    blockingReason = "No se puede dar de baja: El docente está asignado al curso activo '"
                            + curso.getNombre() + "'.";
                    break;
                }
            }

            // Obtener las disponibilidades del docente (no los horarios de clases)
            List<DisponibilidadDocente> disponibilidades = disponibilidadDocenteService
                    .obtenerDisponibilidades(docente);
            if (disponibilidades != null && !disponibilidades.isEmpty()) {
                List<Map<String, Object>> horarios = disponibilidades.stream().map(disponibilidad -> {
                    Map<String, Object> horarioMap = new HashMap<>();
                    horarioMap.put("diaSemana",
                            disponibilidad.getDia() != null ? disponibilidad.getDia().name() : null);
                    horarioMap.put("horaInicio",
                            disponibilidad.getHoraInicio() != null
                                    ? disponibilidad.getHoraInicio().toString().substring(0, 5)
                                    : null);
                    horarioMap.put("horaFin",
                            disponibilidad.getHoraFin() != null ? disponibilidad.getHoraFin().toString().substring(0, 5)
                                    : null);
                    return horarioMap;
                }).collect(Collectors.toList());
                data.put("horariosDisponibilidad", horarios);
            }
        }

        // ===== DATOS DE VALIDACIÓN =====
        data.put("canBeDeleted", canBeDeleted);
        data.put("warnings", warnings);
        data.put("blockingReason", blockingReason);

        // ===== ROLES =====
        List<String> rolesRaw = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toList());
        data.put("roles", rolesRaw.stream().map(this::convertirRolALegible).collect(Collectors.toList()));
        data.put("rolesRaw", rolesRaw);
        data.put("rolPrincipal", rolesRaw.isEmpty() ? null : rolesRaw.get(0));

        // ===== DATOS DE UBICACIÓN =====
        if (usuario.getPais() != null) {
            Map<String, Object> pais = new HashMap<>();
            pais.put("codigo", usuario.getPais().getCodigo());
            pais.put("nombre", usuario.getPais().getNombre());
            data.put("pais", pais);
        }

        if (usuario.getProvincia() != null) {
            Map<String, Object> provincia = new HashMap<>();
            provincia.put("codigo", usuario.getProvincia().getCodigo());
            provincia.put("nombre", usuario.getProvincia().getNombre());
            data.put("provincia", provincia);
        }

        if (usuario.getCiudad() != null) {
            Map<String, Object> ciudad = new HashMap<>();
            ciudad.put("id", usuario.getCiudad().getId());
            ciudad.put("nombre", usuario.getCiudad().getNombre());
            data.put("ciudad", ciudad);
        }

        return data;
    }

    // ================= CONFIGURACIONES INSTITUCIONALES =================

    @GetMapping("/admin/configuracion")
    public String configuracionInstitucional(Model model) {
        // Obtener configuración actual del instituto
        Instituto instituto = institutoService.obtenerInstituto();

        // Obtener imágenes del carrusel
        List<CarruselImagen> imagenesCarrusel = carruselImagenRepository
                .findByInstitutoAndActivaTrueOrderByOrden(instituto);

        model.addAttribute("instituto", instituto);
        model.addAttribute("imagenesCarrusel", imagenesCarrusel);
        return "admin/configuraciones";
    }

    @PostMapping("/admin/configuracion/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarConfiguracion(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "logo", required = false) MultipartFile logo) {

        try {
            System.out.println("=== GUARDANDO CONFIGURACIÓN ===");
            System.out.println("Parámetros recibidos: " + params.keySet());

            Map<String, Object> response = new HashMap<>();

            // Validar campos requeridos
            if (params.get("nombreInstituto") == null || params.get("nombreInstituto").trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El nombre del instituto es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }

            // Obtener instituto actual
            Instituto instituto = institutoService.obtenerInstituto();
            System.out.println("Instituto actual ID: " + instituto.getIdInstituto());

            // Actualizar campos básicos
            instituto.setNombreInstituto(params.get("nombreInstituto"));
            instituto.setDescripcion(params.get("descripcion"));
            instituto.setMision(params.get("mision"));
            instituto.setVision(params.get("vision"));
            instituto.setSobreNosotros(params.get("sobreNosotros")); // Campo nuevo
            instituto.setDireccion(params.get("direccion"));
            instituto.setTelefono(params.get("telefono"));
            instituto.setEmail(params.get("email"));
            instituto.setFacebook(params.get("facebook"));
            instituto.setInstagram(params.get("instagram"));
            instituto.setX(params.get("x"));
            instituto.setMoneda(params.get("moneda"));
            instituto.setCuentaBancaria(params.get("cuentaBancaria"));
            instituto.setPoliticaPagos(params.get("politicaPagos"));
            instituto.setRazonSocial(params.get("razonSocial"));
            instituto.setCuil(params.get("cuil"));
            if (params.get("inicioActividad") != null && !params.get("inicioActividad").trim().isEmpty()) {
                try {
                    instituto.setInicioActividad(java.time.LocalDateTime.parse(params.get("inicioActividad").trim()));
                } catch (Exception e) {
                    System.out.println("Error parseando inicioActividad: " + params.get("inicioActividad"));
                }
            } else {
                instituto.setInicioActividad(null);
            }

            // Configuraciones automáticas - Los checkboxes solo envían valor si están
            // marcados
            instituto.setPermisoBajaAutomatica("on".equals(params.get("permisoBajaAutomatica")));
            System.out.println("Baja automática: " + instituto.getPermisoBajaAutomatica());

            if (params.get("minimoAlumnoBaja") != null && !params.get("minimoAlumnoBaja").trim().isEmpty()) {
                try {
                    instituto.setMinimoAlumnoBaja(Integer.parseInt(params.get("minimoAlumnoBaja").trim()));
                } catch (NumberFormatException e) {
                    System.out.println("Error parseando minimoAlumnoBaja: " + params.get("minimoAlumnoBaja"));
                }
            }
            if (params.get("inactividadBaja") != null && !params.get("inactividadBaja").trim().isEmpty()) {
                try {
                    instituto.setInactividadBaja(Integer.parseInt(params.get("inactividadBaja").trim()));
                } catch (NumberFormatException e) {
                    System.out.println("Error parseando inactividadBaja: " + params.get("inactividadBaja"));
                }
            }

            // Configuración de bloqueos por mora
            System.out.println("=== BLOQUEOS POR MORA ===");
            if (params.get("diasMoraBloqueoExamen") != null && !params.get("diasMoraBloqueoExamen").trim().isEmpty()) {
                try {
                    int dias = Integer.parseInt(params.get("diasMoraBloqueoExamen").trim());
                    instituto.setDiasMoraBloqueoExamen(dias);
                    System.out.println("Días mora examen: " + dias);
                } catch (NumberFormatException e) {
                    System.out.println("Error parseando diasMoraBloqueoExamen: " + params.get("diasMoraBloqueoExamen"));
                }
            } else {
                instituto.setDiasMoraBloqueoExamen(null);
                System.out.println("Días mora examen: null (campo vacío)");
            }

            if (params.get("diasMoraBloqueoMaterial") != null
                    && !params.get("diasMoraBloqueoMaterial").trim().isEmpty()) {
                try {
                    int dias = Integer.parseInt(params.get("diasMoraBloqueoMaterial").trim());
                    instituto.setDiasMoraBloqueoMaterial(dias);
                    System.out.println("Días mora material: " + dias);
                } catch (NumberFormatException e) {
                    System.out.println(
                            "Error parseando diasMoraBloqueoMaterial: " + params.get("diasMoraBloqueoMaterial"));
                }
            } else {
                instituto.setDiasMoraBloqueoMaterial(null);
                System.out.println("Días mora material: null (campo vacío)");
            }

            if (params.get("diasMoraBloqueoActividad") != null
                    && !params.get("diasMoraBloqueoActividad").trim().isEmpty()) {
                try {
                    int dias = Integer.parseInt(params.get("diasMoraBloqueoActividad").trim());
                    instituto.setDiasMoraBloqueoActividad(dias);
                    System.out.println("Días mora actividad: " + dias);
                } catch (NumberFormatException e) {
                    System.out.println(
                            "Error parseando diasMoraBloqueoActividad: " + params.get("diasMoraBloqueoActividad"));
                }
            } else {
                instituto.setDiasMoraBloqueoActividad(null);
                System.out.println("Días mora actividad: null (campo vacío)");
            }

            if (params.get("diasMoraBloqueoAula") != null && !params.get("diasMoraBloqueoAula").trim().isEmpty()) {
                try {
                    int dias = Integer.parseInt(params.get("diasMoraBloqueoAula").trim());
                    instituto.setDiasMoraBloqueoAula(dias);
                    System.out.println("Días mora aula: " + dias);
                } catch (NumberFormatException e) {
                    System.out.println("Error parseando diasMoraBloqueoAula: " + params.get("diasMoraBloqueoAula"));
                }
            } else {
                instituto.setDiasMoraBloqueoAula(null);
                System.out.println("Días mora aula: null (campo vacío)");
            }

            instituto.setHabilitarIA("on".equals(params.get("habilitarIA")));
            instituto.setReportesAutomaticos("on".equals(params.get("reportesAutomaticos")));

            System.out.println("Habilitar IA: " + instituto.getHabilitarIA());
            System.out.println("Reportes automáticos: " + instituto.getReportesAutomaticos());

            // Guardar colores institucionales
            List<String> colores = new ArrayList<>(List.of(
                    params.get("colorPrimario") != null ? params.get("colorPrimario") : "#1f2937",
                    params.get("colorSecundario") != null ? params.get("colorSecundario") : "#f8fafc",
                    params.get("colorTexto") != null ? params.get("colorTexto") : "#374151"));
            instituto.setColores(colores);
            System.out.println("Colores: " + colores);

            // Procesar logo si se subió
            if (logo != null && !logo.isEmpty()) {
                String logoPath = guardarLogo(logo);
                instituto.setLogoPath(logoPath);
                System.out.println("Logo guardado: " + logoPath);
            }

            // Guardar instituto
            Instituto institutoGuardado = institutoService.guardarInstituto(instituto);
            System.out.println("=== INSTITUTO GUARDADO EXITOSAMENTE ===");
            System.out.println("ID: " + institutoGuardado.getIdInstituto());
            System.out.println("Días mora examen guardado: " + institutoGuardado.getDiasMoraBloqueoExamen());
            System.out.println("Días mora material guardado: " + institutoGuardado.getDiasMoraBloqueoMaterial());
            System.out.println("Días mora actividad guardado: " + institutoGuardado.getDiasMoraBloqueoActividad());

            response.put("success", true);
            response.put("message", "Configuración guardada exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ ERROR AL GUARDAR CONFIGURACIÓN: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al guardar la configuración: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ================= GESTIÓN WEB DEL CARRUSEL =================

    @PostMapping("/admin/configuracion/carrusel/subir")
    public String subirImagenesCarruselWeb(
            @RequestParam("imagenes") MultipartFile[] imagenes,
            Model model) {

        System.out.println("=== INICIO SUBIDA DE IMÁGENES ===");
        System.out.println("Número de imágenes recibidas: " + (imagenes != null ? imagenes.length : "null"));

        try {
            // Validaciones
            if (imagenes == null || imagenes.length == 0) {
                System.out.println("ERROR: No se han seleccionado imágenes");
                model.addAttribute("error", "No se han seleccionado imágenes");
                return "redirect:/admin/configuracion?error=nofiles";
            }

            for (int i = 0; i < imagenes.length; i++) {
                MultipartFile imagen = imagenes[i];
                System.out.println("Imagen " + i + ": " + imagen.getOriginalFilename() +
                        " - Tamaño: " + imagen.getSize() + " bytes");
            }

            Instituto instituto = institutoService.obtenerInstituto();
            System.out.println("Instituto obtenido: " + (instituto != null ? "OK" : "null"));

            List<CarruselImagen> imagenesGuardadas = imagenService.guardarMultiplesImagenesCarrusel(imagenes,
                    instituto);
            System.out.println("Imágenes guardadas exitosamente: " + imagenesGuardadas.size());

            model.addAttribute("mensaje", "Imágenes subidas exitosamente: " + imagenesGuardadas.size());
            return "redirect:/admin/configuracion?success=upload";

        } catch (IOException e) {
            System.out.println("ERROR IOException: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al procesar las imágenes: " + e.getMessage());
            return "redirect:/admin/configuracion?error=processing";
        } catch (Exception e) {
            System.out.println("ERROR Exception: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error interno del servidor: " + e.getMessage());
            return "redirect:/admin/configuracion?error=server";
        }
    }

    @PostMapping("/admin/configuracion/carrusel/eliminar/{id}")
    public String eliminarImagenCarruselWeb(@PathVariable Long id, Model model) {
        try {
            Optional<CarruselImagen> imagen = imagenService.obtenerImagenCarrusel(id);
            if (!imagen.isPresent()) {
                return "redirect:/admin/configuracion?error=notfound";
            }

            imagenService.eliminarImagenCarrusel(id);
            return "redirect:/admin/configuracion?success=delete";

        } catch (Exception e) {
            return "redirect:/admin/configuracion?error=deletefail";
        }
    }

    // ================= MÉTODOS AUXILIARES =================

    private String guardarLogo(MultipartFile logo) throws IOException {
        Instituto instituto = institutoService.obtenerInstituto();
        if (instituto == null) {
            throw new IOException("Instituto no encontrado");
        }

        // Eliminar logo anterior si existía en BD
        Long oldId = extraerIdLogoInstituto(instituto.getLogoPath());
        if (oldId != null) {
            try {
                institutoLogoService.eliminar(oldId);
            } catch (Exception e) {
                // Continuar si no se pudo borrar el anterior
            }
        }

        var logoGuardado = institutoLogoService.guardarLogo(logo, instituto);
        return "/api/instituto/logo/" + logoGuardado.getId();
    }

    @PostMapping("/admin/reportes/semanal/ejecutar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ejecutarReportesSemanalesManual() {
        Map<String, Object> response = new HashMap<>();
        try {
            String resumen = reporteSemanalScheduler.ejecutarReportesSemanalesManual();
            response.put("success", true);
            response.put("message", "Reportes semanales generados correctamente.");
            response.put("resumen", resumen);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al generar reportes semanales: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private Long extraerIdLogoInstituto(String logoUrl) {
        String prefix = "/api/instituto/logo/";
        if (logoUrl == null) return null;
        int idx = logoUrl.indexOf(prefix);
        if (idx == -1) return null;
        String idPart = logoUrl.substring(idx + prefix.length());
        try {
            return Long.parseLong(idPart.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String guardarImagenOferta(MultipartFile imagen) throws IOException {
        // Guardar en BD y exponer vía endpoint (similar al carrusel)
        return "/api/ofertas/imagen/" + ofertaImagenService.guardarImagenOferta(imagen).getId();
    }

    private Long extraerIdImagenUsuario(String fotoUrl) {
        String prefix = "/api/usuarios/imagen/";
        if (fotoUrl == null)
            return null;
        int idx = fotoUrl.indexOf(prefix);
        if (idx == -1)
            return null;
        String idPart = fotoUrl.substring(idx + prefix.length());
        try {
            return Long.parseLong(idPart.trim());
        } catch (Exception e) {
            return null;
        }
    }

    // ================= MÉTODOS AUXILIARES PARA OFERTAS =================

    /**
     * Asocia categorías a una oferta académica
     */
    private void asociarCategorias(OfertaAcademica oferta, String categoriasIds) {
        if (categoriasIds == null || categoriasIds.trim().isEmpty()) {
            return;
        }

        List<Categoria> categorias = new ArrayList<>();
        String[] ids = categoriasIds.split(",");

        for (String idStr : ids) {
            try {
                Long id = Long.parseLong(idStr.trim());
                Optional<Categoria> categoria = categoriaRepository.findById(id);
                if (categoria.isPresent()) {
                    categorias.add(categoria.get());
                }
            } catch (NumberFormatException e) {
                System.err.println("Error al parsear ID de categoría: " + idStr);
            }
        }

        oferta.setCategorias(categorias);
    }

    /**
     * Obtiene lista de docentes por IDs separados por coma
     */
    private List<Docente> obtenerDocentesPorIds(String docentesIds) {
        List<Docente> docentes = new ArrayList<>();

        if (docentesIds == null || docentesIds.trim().isEmpty()) {
            return docentes;
        }

        String[] ids = docentesIds.split(",");

        for (String idStr : ids) {
            try {
                UUID id = UUID.fromString(idStr.trim());
                Optional<Docente> docente = docenteRepository.findById(id);
                if (docente.isPresent()) {
                    docentes.add(docente.get());
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Error al parsear UUID de docente: " + idStr);
            }
        }

        return docentes;
    }

    /**
     * Asocia horarios a una oferta académica
     * 
     * @param oferta       La oferta académica guardada
     * @param horariosJson JSON con los horarios en formato:
     *                     [{"dia":"LUNES","horaInicio":"09:00","horaFin":"11:00","docenteId":"1"}]
     */

    /**
     * Asocia horarios a una oferta academica
     * 
     * @param oferta       La oferta academica guardada
     * @param horariosJson JSON con los horarios en formato:
     *                     [{"dia":"LUNES","horaInicio":"09:00","horaFin":"11:00","docenteId":"1"}]
     */

    /**
     * Asocia horarios a una oferta academica
     * 
     * @param oferta       La oferta academica guardada
     * @param horariosJson JSON con los horarios en formato:
     *                     [{"dia":"LUNES","horaInicio":"09:00","horaFin":"11:00","docenteId":"1"}]
     */
    private void asociarHorarios(OfertaAcademica oferta, String horariosJson) {
        try {
            System.out.println("?? ASOCIANDO HORARIOS - JSON recibido: " + horariosJson);
            List<Map<String, String>> horariosData = parseHorariosJson(horariosJson);

            for (Map<String, String> horarioData : horariosData) {
                // 1. Configurar día
                String diaStr = horarioData.get("dia");
                Dias dia = null;
                try {
                    if (diaStr != null && !diaStr.isEmpty()) {
                        dia = Dias.valueOf(diaStr.trim().toUpperCase());
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Día inválido: " + diaStr);
                }

                // 2. Configurar horas
                Time horaInicio = null;
                Time horaFin = null;
                String hIStr = horarioData.get("horaInicio");
                String hFStr = horarioData.get("horaFin");

                if (hIStr != null)
                    horaInicio = parseFlexibleTime(hIStr);
                if (hFStr != null)
                    horaFin = parseFlexibleTime(hFStr);

                // 3. Procesar Docentes (Maneja múltiples o uno solo)
                List<UUID> docentesIdsList = obtenerListaDeIds(horarioData);

                if (docentesIdsList.isEmpty()) {
                    // Crear horario sin docente
                    crearYAñadirHorario(oferta, dia, horaInicio, horaFin, null);
                } else {
                    // Crear un horario por cada docente
                    for (UUID docenteId : docentesIdsList) {
                        Docente docente = docenteRepository.findById(docenteId).orElse(null);
                        crearYAñadirHorario(oferta, dia, horaInicio, horaFin, docente);
                    }
                }
            }
            System.out.println("? Proceso completado. Los horarios se guardarán en cascada con la oferta.");

        } catch (Exception e) {
            System.err.println("Error crítico al procesar horarios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<UUID> obtenerListaDeIds(Map<String, String> horarioData) {
        List<UUID> listaIds = new ArrayList<>();

        // 1. Intentar obtener múltiples IDs (separados por coma)
        String docentesIdsStr = horarioData.get("docentesIds");
        if (docentesIdsStr != null && !docentesIdsStr.isEmpty()) {
            for (String s : docentesIdsStr.split(",")) {
                try {
                    listaIds.add(UUID.fromString(s.trim()));
                } catch (IllegalArgumentException e) {
                    System.err.println("UUID inválido en lista: " + s);
                }
            }
        }

        // 2. Si no hubo múltiples, intentar obtener el ID único
        String docenteIdStr = horarioData.get("docenteId");
        if (listaIds.isEmpty() && docenteIdStr != null && !docenteIdStr.isEmpty()) {
            try {
                listaIds.add(UUID.fromString(docenteIdStr.trim()));
            } catch (IllegalArgumentException e) {
                System.err.println("UUID único inválido: " + docenteIdStr);
            }
        }

        return listaIds;
    }

    // Método auxiliar para evitar repetir código
    private void crearYAñadirHorario(OfertaAcademica oferta, Dias dia, Time inicio, Time fin, Docente docente) {
        Horario h = new Horario();
        h.setDia(dia);
        h.setHoraInicio(inicio);
        h.setHoraFin(fin);
        h.setDocente(docente);
        oferta.addHorario(h); // Asumo que este método también hace h.setOfertaAcademica(this)
    }

    private Time parseFlexibleTime(String value) {
        String v = value.trim();
        if (v.length() > 8) {
            v = v.substring(0, 8);
        }
        java.time.format.DateTimeFormatter fmt = new java.time.format.DateTimeFormatterBuilder()
                .appendPattern("H:mm")
                .optionalStart()
                .appendPattern(":ss")
                .optionalEnd()
                .toFormatter();
        java.time.LocalTime lt = java.time.LocalTime.parse(v, fmt);
        return java.sql.Time.valueOf(lt);
    }

    private List<Map<String, String>> parseHorariosJson(String json) {
        List<Map<String, String>> horarios = new ArrayList<>();

        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return horarios;
        }

        try {
            System.out.println("📋 Parseando JSON: " + json);

            // Remover corchetes
            json = json.trim();
            if (json.startsWith("["))
                json = json.substring(1);
            if (json.endsWith("]"))
                json = json.substring(0, json.length() - 1);

            if (json.trim().isEmpty()) {
                return horarios;
            }

            // Dividir por objetos (asumiendo formato simple)
            String[] objetos = json.split("\\},\\s*\\{");

            for (String objeto : objetos) {
                objeto = objeto.trim();
                if (objeto.startsWith("{"))
                    objeto = objeto.substring(1);
                if (objeto.endsWith("}"))
                    objeto = objeto.substring(0, objeto.length() - 1);

                System.out.println("  📝 Procesando objeto: " + objeto);

                Map<String, String> horario = new HashMap<>();

                // Dividir por comas que NO estén dentro de comillas
                String[] pares = objeto.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                for (String par : pares) {
                    par = par.trim();
                    System.out.println("    🔍 Par: " + par);

                    // Encontrar la primera ocurrencia de : que separa clave de valor
                    int colonIndex = par.indexOf(":");
                    if (colonIndex > 0) {
                        String key = par.substring(0, colonIndex).trim().replaceAll("\"", "");
                        String value = par.substring(colonIndex + 1).trim().replaceAll("\"", "");

                        System.out.println("      ✅ Key: " + key + ", Value: " + value);
                        horario.put(key, value);
                    }
                }

                if (!horario.isEmpty()) {
                    System.out.println("  ✅ Horario parseado: " + horario);
                    horarios.add(horario);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error al parsear JSON de horarios: " + e.getMessage());
            e.printStackTrace();
        }

        return horarios;
    }

    /**
     * Asocia categorías a una oferta académica
     */
    private void asociarCategoriasAOferta(OfertaAcademica oferta, String categoriasIds) {
        if (categoriasIds == null || categoriasIds.trim().isEmpty()) {
            System.out.println("⚠️ No se proporcionaron categorías");
            return;
        }

        try {
            System.out.println("🏷️ Asociando categorías a oferta: " + oferta.getNombre());
            System.out.println("   IDs recibidos: " + categoriasIds);

            // Dividir por comas
            String[] ids = categoriasIds.split(",");
            List<Categoria> categorias = new ArrayList<>();

            for (String idStr : ids) {
                idStr = idStr.trim();
                if (!idStr.isEmpty()) {
                    try {
                        Long id = Long.parseLong(idStr);
                        Optional<Categoria> categoriaOpt = categoriaRepository.findById(id);

                        if (categoriaOpt.isPresent()) {
                            Categoria categoria = categoriaOpt.get();
                            categorias.add(categoria);
                            System.out.println("   ✅ Categoría agregada: " + categoria.getNombre());
                        } else {
                            System.out.println("   ⚠️ Categoría no encontrada con ID: " + id);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("   ❌ ID inválido: " + idStr);
                    }
                }
            }

            // Asignar categorías a la oferta
            if (!categorias.isEmpty()) {
                oferta.setCategorias(categorias);
                ofertaAcademicaRepository.save(oferta);
                System.out.println("   💾 " + categorias.size() + " categoría(s) asociada(s) y guardada(s)");
            } else {
                System.out.println("   ⚠️ No se encontraron categorías válidas para asociar");
            }

        } catch (Exception e) {
            System.err.println("❌ Error al asociar categorías: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
