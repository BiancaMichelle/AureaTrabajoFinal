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
import org.springframework.stereotype.Controller;
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
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;
import com.example.demo.enums.TipoGenero;
import com.example.demo.enums.Dias;
import com.example.demo.model.Alumno;
import com.example.demo.model.CarruselImagen;
import com.example.demo.model.Auditable;
import com.example.demo.model.Categoria;
import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Formacion;
import com.example.demo.model.Horario;
import com.example.demo.model.Instituto;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pais;
import com.example.demo.model.Rol;
import com.example.demo.model.Seminario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CarruselImagenRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CharlaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.FormacionRepository;
import com.example.demo.repository.HorarioRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.SeminarioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.ImagenService;
import com.example.demo.service.InstitutoService;
import com.example.demo.service.LocacionAPIService;
import com.example.demo.service.OfertaAcademicaService;
import com.example.demo.service.RegistroService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@SuppressWarnings("unused") // Los repositorios se usan en métodos privados
public class AdminController {

    @Value("${app.base-url}")
    private String baseUrl;

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
    private CarruselImagenRepository carruselImagenRepository;
    
    
    @Autowired
    private ImagenService imagenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private HorarioRepository horarioRepository;


    public AdminController(LocacionAPIService locacionApiService,
                           RegistroService registroService) {
        this.locacionApiService = locacionApiService;
        this.registroService = registroService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        // Obtener estadísticas básicas
        long totalOfertas = ofertaAcademicaRepository.count();
        
        model.addAttribute("totalOfertas", totalOfertas);
        
        return "admin/panelAdmin";
    }

    @GetMapping("/admin/gestion-ofertas")
    public String gestionOfertas(Model model) {
        try {
            System.out.println("🔍 Iniciando gestionOfertas...");
            
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
            System.out.println("✅ Oferta encontrada: " + oferta.getNombre() + " (Tipo: " + oferta.getClass().getSimpleName() + ")");
            
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
    
    /**
     * Endpoint para eliminar una oferta
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
                return ResponseEntity.notFound().build();
            }
            
            OfertaAcademica oferta = ofertaOpt.get();
            
            // Verificar si puede ser eliminada
            if (!oferta.puedeSerEliminada()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No se puede eliminar esta oferta porque tiene inscripciones o ya finalizó");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Eliminar usando el servicio
            ofertaAcademicaService.eliminar(id, oferta.getTipoOferta());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta eliminada correctamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al eliminar la oferta: " + e.getMessage());
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
                return ResponseEntity.ok(response); // Devolver 200 OK con success: false
            }
            
            OfertaAcademica oferta = ofertaOpt.get();
            System.out.println("📋 Estado actual: " + oferta.getEstado());
            
            // Validar si se puede cambiar el estado
            if (!oferta.puedeCambiarEstado()) {
                String motivo = "No se puede cambiar el estado de una oferta finalizada o cancelada";
                System.out.println("❌ No se puede cambiar estado: " + motivo);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", motivo);
                response.put("motivo", "ESTADO_FINAL");
                return ResponseEntity.ok(response); // Devolver 200 OK con success: false
            }
            
            // Si está activa, validar si se puede dar de baja
            if (oferta.getEstado() == com.example.demo.enums.EstadoOferta.ACTIVA) {
                String motivoRechazo = validarDarDeBaja(oferta);
                if (motivoRechazo != null) {
                    System.out.println("❌ No se puede dar de baja: " + motivoRechazo);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", motivoRechazo);
                    response.put("motivo", "VALIDACION_BAJA");
                    return ResponseEntity.ok(response); // Devolver 200 OK con success: false
                }
                
                // Dar de baja
                oferta.setEstado(com.example.demo.enums.EstadoOferta.INACTIVA);
                System.out.println("🔴 Cambiando a INACTIVA");
            } else {
                // Dar de alta (de INACTIVA a ACTIVA)
                oferta.setEstado(com.example.demo.enums.EstadoOferta.ACTIVA);
                System.out.println("🟢 Cambiando a ACTIVA");
            }
            
            // Guardar cambios
            ofertaAcademicaRepository.save(oferta);
            System.out.println("✅ Estado cambiado exitosamente a: " + oferta.getEstado());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("nuevoEstado", oferta.getEstado().toString());
            response.put("message", "Estado de la oferta cambiado exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error cambiando estado de oferta " + id + ": " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            response.put("motivo", "ERROR_SERVIDOR");
            return ResponseEntity.ok(response); // Devolver 200 OK con success: false
        }
    }
    
    /**
     * Valida si una oferta puede ser dada de baja y devuelve el motivo si no puede
     */
    private String validarDarDeBaja(OfertaAcademica oferta) {
        java.time.LocalDate ahora = java.time.LocalDate.now();
        
        // Contar inscripciones activas
        int inscripcionesActivas = 0;
        if (oferta.getInscripciones() != null) {
            inscripcionesActivas = (int) oferta.getInscripciones().stream()
                    .filter(inscripcion -> inscripcion.getEstadoInscripcion() != null && 
                           inscripcion.getEstadoInscripcion() == true)
                    .count();
        }
        
        System.out.println("📊 Validando baja - Inscripciones activas: " + inscripcionesActivas);
        System.out.println("📊 Fechas - Inicio: " + oferta.getFechaInicio() + ", Fin: " + oferta.getFechaFin() + ", Hoy: " + ahora);
        
        // Si ya terminó la oferta, siempre se puede dar de baja
        if (oferta.getFechaFin() != null && oferta.getFechaFin().isBefore(ahora)) {
            System.out.println("✅ Oferta ya terminó, se puede dar de baja");
            return null; // Se puede dar de baja
        }
        
        // Si tiene inscripciones activas y ya comenzó, no se puede dar de baja
        if (inscripcionesActivas > 0) {
            if (oferta.getFechaInicio() != null && !oferta.getFechaInicio().isAfter(ahora)) {
                return "No se puede dar de baja esta oferta porque ya comenzó y tiene " + 
                       inscripcionesActivas + " inscripcion" + (inscripcionesActivas > 1 ? "es" : "") + " activa" + 
                       (inscripcionesActivas > 1 ? "s" : "") + ". Las inscripciones deben ser canceladas primero.";
            } else {
                return "No se puede dar de baja esta oferta porque tiene " + 
                       inscripcionesActivas + " inscripcion" + (inscripcionesActivas > 1 ? "es" : "") + " activa" + 
                       (inscripcionesActivas > 1 ? "s" : "") + ". Las inscripciones deben ser canceladas primero.";
            }
        }
        
        // Si no tiene inscripciones activas, siempre se puede dar de baja
        System.out.println("✅ No hay inscripciones activas, se puede dar de baja");
        return null; // Se puede dar de baja
    }
    
    /**
     * Método auxiliar para mapear una oferta a un objeto de respuesta con validaciones defensivas
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
     * Obtiene el detalle completo de una oferta usando los métodos específicos del modelo
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

    // =================== ENDPOINTS PARA DOCENTES ===================
    
    @GetMapping("/admin/docentes/buscar")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> buscarDocentes(@RequestParam(value = "q", defaultValue = "") String query) {
        try {
            // Si la query está vacía, devolver todos los docentes
            List<Docente> docentes = query.trim().isEmpty() ? 
                docenteRepository.findAllDocentes() : 
                docenteRepository.buscarPorNombreApellidoOMatricula(query);
            
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
            @RequestParam(required = false) Integer duracionEstimada,
            @RequestParam(required = false) String disertantesCharla,
            @RequestParam(required = false) String publicoObjetivoCharla,
            // Campos específicos para SEMINARIO
            @RequestParam(required = false) String lugarSeminario,
            @RequestParam(required = false) String enlaceSeminario,
            @RequestParam(required = false) Integer duracionMinutos,
            @RequestParam(required = false) String disertantesSeminario,
            @RequestParam(required = false) String publicoObjetivoSeminario) {
        
        try {
            System.out.println("🔥 REGISTRO DE OFERTA INICIADO");
            System.out.println("Tipo: " + tipoOferta);
            System.out.println("Nombre: " + nombre);
            System.out.println("Descripción: " + descripcion);
            System.out.println("Cupos: " + cupos);
            System.out.println("Costo Inscripción: " + costoInscripcion);
            System.out.println("Modalidad: " + modalidad);
            System.out.println("Otorga Certificado: " + otorgaCertificado);
            System.out.println("Categorías: " + categorias);
            System.out.println("Horarios: " + horarios);
            
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
            LocalDate fechaInicioDate, fechaFinDate;
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
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Formato de fecha inválido. Use el formato YYYY-MM-DD");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Normalizar costos: convertir null a 0
            if (costoInscripcion == null) costoInscripcion = 0.0;
            if (costoCuota == null) costoCuota = 0.0;
            if (costoMora == null) costoMora = 0.0;
            if (costoCuotaFormacion == null) costoCuotaFormacion = 0.0;
            if (costoMoraFormacion == null) costoMoraFormacion = 0.0;
            
            OfertaAcademica oferta;
            
            // Crear la instancia específica según el tipo
            switch (tipoOferta.toUpperCase()) {
                case "CURSO":
                    oferta = crearCurso(nombre, descripcion, cupos, costoInscripcion, fechaInicio, fechaFin, modalidad,
                                      temario, docentesCurso, costoCuota, costoMora, nrCuotas, diaVencimiento);
                    break;
                case "FORMACION":
                    oferta = crearFormacion(nombre, descripcion, cupos, costoInscripcion, fechaInicio, fechaFin, modalidad,
                                          planFormacion, docentesFormacion, 
                                          costoCuotaFormacion, costoMoraFormacion, nrCuotasFormacion, diaVencimientoFormacion);
                    break;
                case "CHARLA":
                    oferta = crearCharla(nombre, descripcion, cupos, costoInscripcion, fechaInicio, fechaFin, modalidad,
                                       lugarCharla, enlaceCharla, duracionEstimada, disertantesCharla, 
                                       publicoObjetivoCharla);
                    break;
                case "SEMINARIO":
                    oferta = crearSeminario(nombre, descripcion, cupos, costoInscripcion, fechaInicio, fechaFin, modalidad,
                                          lugarSeminario, enlaceSeminario, duracionMinutos, disertantesSeminario,
                                          publicoObjetivoSeminario);
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de oferta no válido: " + tipoOferta);
            }
            
            // Asociar categorías si se proporcionaron
            if (categorias != null && !categorias.trim().isEmpty()) {
                asociarCategorias(oferta, categorias);
            }
            
            // Calcular duración en meses antes de guardar
            oferta.calcularDuracionMeses();
            
            // Establecer valor del certificado
            if (otorgaCertificado != null && !otorgaCertificado.trim().isEmpty()) {
                boolean certificado = "true".equalsIgnoreCase(otorgaCertificado.trim());
                oferta.setCertificado(certificado);
                System.out.println("Certificado establecido: " + certificado + " (desde string: '" + otorgaCertificado + "')");
            } else {
                oferta.setCertificado(false);
                System.out.println("Certificado establecido por defecto: false");
            }
            
            // Manejar imagen si existe (por ahora comentado hasta implementar el servicio)
            /*
            if (imagen != null && !imagen.isEmpty()) {
                try {
                    String rutaImagen = imagenService.guardarImagen(imagen, "ofertas");
                    oferta.setImagenPresentacion(rutaImagen);
                } catch (Exception e) {
                    // Log error pero continuar sin imagen
                    System.err.println("Error al guardar imagen: " + e.getMessage());
                }
            }
            */
            
            // Guardar en la base de datos
            OfertaAcademica nuevaOferta = ofertaAcademicaRepository.save(oferta);
            
            // ✅ Si es una Formación con docentes, guardar los docentes para persistir la relación ManyToMany
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
            
            // Asociar horarios si se proporcionaron
            if (horarios != null && !horarios.trim().isEmpty()) {
                asociarHorarios(nuevaOferta, horarios);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta académica registrada exitosamente");
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
                response.put("message", "Ya existe una oferta académica con este nombre. Por favor, elija un nombre diferente.");
            } else {
                response.put("message", "Error: La oferta no pudo registrarse debido a una restricción de datos. Verifique que no exista una oferta duplicada.");
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
                           String fechaInicio, String fechaFin, String modalidad,
                           String temario, String docentesIds, Double costoCuota, Double costoMora, 
                           Integer nrCuotas, Integer diaVencimiento) {
        Curso curso = new Curso();
        configurarOfertaBase(curso, nombre, descripcion, cupos, costo, fechaInicio, fechaFin, modalidad);
        
        // Campos específicos del curso
        curso.setTemario(temario);
        if (costoCuota != null) curso.setCostoCuota(costoCuota);
        if (costoMora != null) {
            curso.setCostoMora(costoMora);
            curso.setRecargoMora(costoMora);
        }
        if (nrCuotas != null) curso.setNrCuotas(nrCuotas);
        if (diaVencimiento != null) curso.setDiaVencimiento(diaVencimiento);
        
        // Asociar docentes si se proporcionaron
        if (docentesIds != null && !docentesIds.trim().isEmpty()) {
            List<Docente> docentes = obtenerDocentesPorIds(docentesIds);
            curso.setDocentes(docentes);
        }
        
        return curso;
    }
    
    private Formacion crearFormacion(String nombre, String descripcion, Integer cupos, Double costo,
                                   String fechaInicio, String fechaFin, String modalidad,
                                   String plan, String docentesIds, Double costoCuota, Double costoMora,
                                   Integer nrCuotas, Integer diaVencimiento) {
        Formacion formacion = new Formacion();
        configurarOfertaBase(formacion, nombre, descripcion, cupos, costo, fechaInicio, fechaFin, modalidad);
        
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
                             String fechaInicio, String fechaFin, String modalidad,
                             String lugar, String enlace, Integer duracionEstimada,
                             String disertantesStr, String publicoObjetivo) {
        Charla charla = new Charla();
        configurarOfertaBase(charla, nombre, descripcion, cupos, costo, fechaInicio, fechaFin, modalidad);
        
        // Campos específicos de la charla
        charla.setLugar(lugar);
        charla.setEnlace(enlace);
        if (duracionEstimada != null) charla.setDuracionEstimada(duracionEstimada);
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
                                   String fechaInicio, String fechaFin, String modalidad,
                                   String lugar, String enlace, Integer duracionMinutos,
                                   String disertantesStr, String publicoObjetivo) {
        Seminario seminario = new Seminario();
        configurarOfertaBase(seminario, nombre, descripcion, cupos, costo, fechaInicio, fechaFin, modalidad);
        
        // Campos específicos del seminario
        seminario.setLugar(lugar);
        seminario.setEnlace(enlace);
        if (duracionMinutos != null) seminario.setDuracionMinutos(duracionMinutos);
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
                                    String fechaFin, String modalidad) {
        oferta.setNombre(nombre);
        oferta.setDescripcion(descripcion);
        
        if (cupos != null) oferta.setCupos(cupos);
        if (costo != null) oferta.setCostoInscripcion(costo);
        
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            oferta.setFechaInicio(LocalDate.parse(fechaInicio));
        }
        
        if (fechaFin != null && !fechaFin.isEmpty()) {
            oferta.setFechaFin(LocalDate.parse(fechaFin));
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
            @RequestParam(required = false) Integer duracionEstimada,
            @RequestParam(required = false) String disertantesCharla,
            @RequestParam(required = false) String publicoObjetivoCharla,
            // Campos específicos para SEMINARIO
            @RequestParam(required = false) String lugarSeminario,
            @RequestParam(required = false) String enlaceSeminario,
            @RequestParam(required = false) Integer duracionMinutos,
            @RequestParam(required = false) String disertantesSeminario,
            @RequestParam(required = false) String publicoObjetivoSeminario) {
        
        try {
            System.out.println("🔄 MODIFICACIÓN DE OFERTA INICIADA");
            System.out.println("ID Oferta: " + idOferta);
            System.out.println("Tipo: " + tipoOferta);
            System.out.println("Nombre: " + nombre);
            
            // Buscar la oferta existente
            Optional<OfertaAcademica> ofertaOpt = ofertaAcademicaRepository.findById(idOferta);
            if (ofertaOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Oferta no encontrada");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            OfertaAcademica ofertaExistente = ofertaOpt.get();
            
            // Verificar si se puede modificar
            if (!ofertaExistente.puedeSerEditada()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No se puede modificar esta oferta porque ya finalizó o tiene inscripciones activas");
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
            
            // Validar que el tipo de oferta coincida
            if (!ofertaExistente.getTipoOferta().equals(tipoOferta)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "No se puede cambiar el tipo de oferta existente");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Llamar al servicio apropiado según el tipo de oferta
            OfertaAcademica ofertaModificada = null;
            String tipoOfertaUpper = tipoOferta.toUpperCase();
            
            // Actualizar los campos básicos de la oferta existente
            ofertaExistente.setNombre(nombre);
            ofertaExistente.setDescripcion(descripcion);
            ofertaExistente.setFechaInicio(fechaInicioDate);
            ofertaExistente.setFechaFin(fechaFinDate);
            
            if (cupos != null) {
                ofertaExistente.setCupos(cupos);
            }
            
            if (modalidad != null && !modalidad.isEmpty()) {
                ofertaExistente.setModalidad(Modalidad.valueOf(modalidad.toUpperCase()));
            }
            
            if (otorgaCertificado != null) {
                ofertaExistente.setCertificado(Boolean.parseBoolean(otorgaCertificado));
            }
            
            // Actualizar campos específicos según el tipo de oferta
            switch (tipoOfertaUpper) {
                case "CURSO":
                    if (ofertaExistente instanceof Curso) {
                        Curso curso = (Curso) ofertaExistente;
                        if (costoCuota != null) curso.setCostoCuota(costoCuota);
                        if (costoMora != null) {
                            curso.setCostoMora(costoMora);
                            curso.setRecargoMora(costoMora);
                        }
                        if (nrCuotas != null) curso.setNrCuotas(nrCuotas);
                        if (diaVencimiento != null) curso.setDiaVencimiento(diaVencimiento);
                    }
                    break;
                    
                case "FORMACION":
                    if (ofertaExistente instanceof Formacion) {
                        Formacion formacion = (Formacion) ofertaExistente;
                        if (costoCuotaFormacion != null) formacion.setCostoCuota(costoCuotaFormacion);
                        if (costoMoraFormacion != null) {
                            formacion.setCostoMora(costoMoraFormacion);
                            formacion.setRecargoMora(costoMoraFormacion);
                        }
                        if (nrCuotasFormacion != null) formacion.setNrCuotas(nrCuotasFormacion);
                        if (diaVencimientoFormacion != null) formacion.setDiaVencimiento(diaVencimientoFormacion);
                    }
                    break;
                    
                case "CHARLA":
                    if (ofertaExistente instanceof Charla) {
                        Charla charla = (Charla) ofertaExistente;
                        if (costoInscripcion != null) charla.setCostoInscripcion(costoInscripcion);
                    }
                    break;
                    
                case "SEMINARIO":
                    if (ofertaExistente instanceof Seminario) {
                        Seminario seminario = (Seminario) ofertaExistente;
                        if (costoInscripcion != null) seminario.setCostoInscripcion(costoInscripcion);
                    }
                    break;
                    
                default:
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Tipo de oferta no válido: " + tipoOferta);
                    return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Guardar la oferta modificada
            ofertaModificada = ofertaAcademicaRepository.save(ofertaExistente);
            
            if (ofertaModificada != null) {
                System.out.println("✅ Oferta modificada exitosamente: " + ofertaModificada.getNombre());
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Oferta modificada exitosamente");
                response.put("oferta", mapearOfertaAResponse(ofertaModificada));
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Error al modificar la oferta");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error al modificar oferta: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al modificar oferta: " + e.getMessage());
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
            System.out.println("📝 Registrando usuario desde admin:");
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

            // Convertir ciudadId a Long
            Long ciudadIdLong = Long.valueOf(ciudadId);

            // ✅ Procesar horarios si es docente y hay horarios
            List<Map<String, String>> horariosList = new ArrayList<>();
            if ("DOCENTE".equals(rol) && horariosDisponibilidad != null && !horariosDisponibilidad.isEmpty()) {
                try {
                    // Parsear el JSON de horarios
                    ObjectMapper objectMapper = new ObjectMapper();
                    horariosList = objectMapper.readValue(horariosDisponibilidad, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                    
                    System.out.println("📅 Horarios procesados: " + horariosList.size());
                    for (Map<String, String> horario : horariosList) {
                        System.out.println("   - " + horario.get("diaSemana") + ": " + 
                                        horario.get("horaInicio") + " - " + horario.get("horaFin"));
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Error parseando horarios: " + e.getMessage());
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
            response.put("message", "Usuario registrado exitosamente. Las credenciales han sido enviadas al correo electrónico.");
            response.put("id", nuevoUsuario.getId());
            response.put("dni", nuevoUsuario.getDni());
            response.put("nombre", nuevoUsuario.getNombre() + " " + nuevoUsuario.getApellido());
            response.put("rol", rol);
            
            System.out.println("✅ Usuario registrado exitosamente desde admin: " + nuevoUsuario.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error al registrar usuario desde admin: " + e.getMessage());
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
            Optional<Usuario> usuarioOpt = buscarUsuarioPorIdentificador(identificador);
            if (usuarioOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Long ciudadIdLong = Long.valueOf(ciudadId);

            Usuario actualizado = registroService.actualizarUsuarioAdministrativo(
                    usuarioOpt.get(),
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
                    estado
            );

            response.put("success", true);
            response.put("message", "Usuario actualizado correctamente");
            response.put("data", mapearDetalleUsuario(actualizado));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al actualizar usuario: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
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

            registroService.eliminarUsuarioAdministrativo(usuarioOpt.get());

            response.put("success", true);
            response.put("message", "Usuario eliminado correctamente");
            return ResponseEntity.ok(response);
        } catch (DataIntegrityViolationException ex) {
            response.put("success", false);
            response.put("message", "El usuario no puede eliminarse porque tiene registros asociados");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar usuario: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/usuarios/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarUsuarios(
            @RequestParam(defaultValue = "0") int page,  // ✅ CAMBIAR A 0 POR DEFECTO
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
            
            System.out.println("📄 Usuarios en esta página: " + usuariosPagina.size() + " (start: " + start + ", end: " + end + ")");
            
            // Convertir usuarios reales a formato para frontend
            List<Map<String, Object>> usuariosResponse = new ArrayList<>();
            
            for (Usuario usuario : usuariosPagina) {
                Map<String, Object> usuarioMap = new HashMap<>();
                usuarioMap.put("dni", usuario.getDni());
                usuarioMap.put("nombreCompleto", usuario.getNombre() + " " + usuario.getApellido());
                usuarioMap.put("correo", usuario.getCorreo());
                usuarioMap.put("foto", null);
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
            data.put("totalPages", Math.max(1, (int) Math.ceil((double) todosUsuarios.size() / size))); // ✅ Mínimo 1 página
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
        if (rol == null) return "Sin rol";
        
        switch(rol.toUpperCase()) {
            case "ALUMNO": return "Alumno";
            case "DOCENTE": return "Docente"; 
            case "ADMIN": return "Administrador";
            case "COORDINADOR": return "Coordinador";
            default: return rol;
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
        data.put("telefono", usuario.getNumTelefono());
        data.put("fechaNacimiento", usuario.getFechaNacimiento());
        data.put("genero", usuario.getGenero());
        data.put("estado", usuario.getEstado());
        data.put("estadoBoolean", usuario.isEstado());
        data.put("fechaRegistro", usuario.getFechaRegistro());

        List<String> rolesRaw = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toList());
        data.put("roles", rolesRaw.stream().map(this::convertirRolALegible).collect(Collectors.toList()));
        data.put("rolesRaw", rolesRaw);
        data.put("rolPrincipal", rolesRaw.isEmpty() ? null : rolesRaw.get(0));

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

        if (usuario instanceof Alumno) {
            Alumno alumno = (Alumno) usuario;
            data.put("colegioEgreso", alumno.getColegioEgreso());
            data.put("añoEgreso", alumno.getAñoEgreso());
            data.put("ultimosEstudios", alumno.getUltimosEstudios());
        }

        if (usuario instanceof Docente) {
            Docente docente = (Docente) usuario;
            data.put("matricula", docente.getMatricula());
            data.put("experiencia", docente.getAñosExperiencia());

            if (docente.getHorario() != null) {
                List<Map<String, Object>> horarios = docente.getHorario().stream().map(horario -> {
                    Map<String, Object> horarioMap = new HashMap<>();
                    horarioMap.put("diaSemana", horario.getDia() != null ? horario.getDia().name() : null);
                    horarioMap.put("horaInicio", horario.getHoraInicio() != null ? horario.getHoraInicio().toString().substring(0, 5) : null);
                    horarioMap.put("horaFin", horario.getHoraFin() != null ? horario.getHoraFin().toString().substring(0, 5) : null);
                    return horarioMap;
                }).collect(Collectors.toList());
                data.put("horariosDisponibilidad", horarios);
            }
        }

        return data;
    }
    
    

    // =================   CONFIGURACIONES INSTITUCIONALES   =================
    
    @GetMapping("/admin/configuracion")
    public String configuracionInstitucional(Model model) {
        // Obtener configuración actual del instituto
        Instituto instituto = institutoService.obtenerInstituto();
        
        // Obtener imágenes del carrusel
        List<CarruselImagen> imagenesCarrusel = carruselImagenRepository.findByInstitutoAndActivaTrueOrderByOrden(instituto);
        
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
            instituto.setDireccion(params.get("direccion"));
            instituto.setTelefono(params.get("telefono"));
            instituto.setEmail(params.get("email"));
            instituto.setFacebook(params.get("facebook"));
            instituto.setInstagram(params.get("instagram"));
            instituto.setX(params.get("x"));
            instituto.setMoneda(params.get("moneda"));
            instituto.setCuentaBancaria(params.get("cuentaBancaria"));
            instituto.setPoliticaPagos(params.get("politicaPagos"));
            
            // Configuraciones automáticas - Los checkboxes solo envían valor si están marcados
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
            
            if (params.get("diasMoraBloqueoMaterial") != null && !params.get("diasMoraBloqueoMaterial").trim().isEmpty()) {
                try {
                    int dias = Integer.parseInt(params.get("diasMoraBloqueoMaterial").trim());
                    instituto.setDiasMoraBloqueoMaterial(dias);
                    System.out.println("Días mora material: " + dias);
                } catch (NumberFormatException e) {
                    System.out.println("Error parseando diasMoraBloqueoMaterial: " + params.get("diasMoraBloqueoMaterial"));
                }
            } else {
                instituto.setDiasMoraBloqueoMaterial(null);
                System.out.println("Días mora material: null (campo vacío)");
            }
            
            if (params.get("diasMoraBloqueoActividad") != null && !params.get("diasMoraBloqueoActividad").trim().isEmpty()) {
                try {
                    int dias = Integer.parseInt(params.get("diasMoraBloqueoActividad").trim());
                    instituto.setDiasMoraBloqueoActividad(dias);
                    System.out.println("Días mora actividad: " + dias);
                } catch (NumberFormatException e) {
                    System.out.println("Error parseando diasMoraBloqueoActividad: " + params.get("diasMoraBloqueoActividad"));
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
                params.get("colorTexto") != null ? params.get("colorTexto") : "#374151"
            ));
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
            
            List<CarruselImagen> imagenesGuardadas = imagenService.guardarMultiplesImagenesCarrusel(imagenes, instituto);
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
        // Crear directorio si no existe
        Path directorioLogos = Paths.get("src/main/resources/static/img/logos");
        if (!Files.exists(directorioLogos)) {
            Files.createDirectories(directorioLogos);
        }
        
        // Generar nombre único
        String nombreOriginal = logo.getOriginalFilename();
        String extension = nombreOriginal != null ? nombreOriginal.substring(nombreOriginal.lastIndexOf(".")) : ".jpg";
        String nombreArchivo = "logo_" + UUID.randomUUID().toString() + extension;
        
        // Guardar archivo
        Path rutaArchivo = directorioLogos.resolve(nombreArchivo);
        Files.copy(logo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
        
        return "/img/logos/" + nombreArchivo;
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
     * @param oferta La oferta académica guardada
     * @param horariosJson JSON con los horarios en formato: [{"dia":"LUNES","horaInicio":"09:00","horaFin":"11:00","docenteId":"1"}]
     */
    private void asociarHorarios(OfertaAcademica oferta, String horariosJson) {
        try {
            System.out.println("🕒 ASOCIANDO HORARIOS - JSON recibido: " + horariosJson);
            
            // Parsear JSON manualmente (implementación simple)
            List<Map<String, String>> horariosData = parseHorariosJson(horariosJson);
            System.out.println("🕒 HORARIOS PARSEADOS: " + horariosData.size() + " horarios");
            
            for (Map<String, String> horarioData : horariosData) {
                System.out.println("  📅 Procesando horario: " + horarioData);
                Horario horario = new Horario();
                
                // Configurar día
                String diaStr = horarioData.get("dia");
                System.out.println("    - Día: " + diaStr);
                if (diaStr != null && !diaStr.isEmpty()) {
                    horario.setDia(Dias.valueOf(diaStr.toUpperCase()));
                }
                
                // Configurar horas
                String horaInicioStr = horarioData.get("horaInicio");
                String horaFinStr = horarioData.get("horaFin");
                System.out.println("    - Hora inicio (string): " + horaInicioStr);
                System.out.println("    - Hora fin (string): " + horaFinStr);
                
                if (horaInicioStr != null && !horaInicioStr.isEmpty()) {
                    try {
                        Time horaInicio = Time.valueOf(horaInicioStr + ":00");
                        horario.setHoraInicio(horaInicio);
                        System.out.println("    - ✅ Hora inicio guardada: " + horaInicio);
                    } catch (Exception e) {
                        System.err.println("    - ❌ Error al parsear hora inicio: " + e.getMessage());
                    }
                }
                
                if (horaFinStr != null && !horaFinStr.isEmpty()) {
                    try {
                        Time horaFin = Time.valueOf(horaFinStr + ":00");
                        horario.setHoraFin(horaFin);
                        System.out.println("    - ✅ Hora fin guardada: " + horaFin);
                    } catch (Exception e) {
                        System.err.println("    - ❌ Error al parsear hora fin: " + e.getMessage());
                    }
                }
                
                // Asociar docente si se especifica
                String docenteIdStr = horarioData.get("docenteId");
                if (docenteIdStr != null && !docenteIdStr.isEmpty()) {
                    try {
                        UUID docenteId = UUID.fromString(docenteIdStr);
                        Optional<Docente> docente = docenteRepository.findById(docenteId);
                        if (docente.isPresent()) {
                            horario.setDocente(docente.get());
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error al parsear UUID de docente: " + docenteIdStr);
                    }
                }
                
                // Asociar oferta
                horario.setOfertaAcademica(oferta);
                
                // Debug: mostrar estado antes de guardar
                System.out.println("    🔍 ESTADO ANTES DE GUARDAR:");
                System.out.println("       - Día: " + horario.getDia());
                System.out.println("       - Hora inicio: " + horario.getHoraInicio());
                System.out.println("       - Hora fin: " + horario.getHoraFin());
                System.out.println("       - Docente: " + (horario.getDocente() != null ? horario.getDocente().getNombre() : "null"));
                System.out.println("       - Oferta: " + (horario.getOfertaAcademica() != null ? horario.getOfertaAcademica().getNombre() : "null"));
                
                // Guardar horario
                Horario horarioGuardado = horarioRepository.save(horario);
                System.out.println("    ✅ Horario guardado con ID: " + horarioGuardado.getIdHorario());
            }
            
        } catch (Exception e) {
            System.err.println("Error al procesar horarios: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parsea JSON de horarios de forma simple
     */
    private List<Map<String, String>> parseHorariosJson(String json) {
        List<Map<String, String>> horarios = new ArrayList<>();
        
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return horarios;
        }
        
        try {
            System.out.println("📋 Parseando JSON: " + json);
            
            // Remover corchetes
            json = json.trim();
            if (json.startsWith("[")) json = json.substring(1);
            if (json.endsWith("]")) json = json.substring(0, json.length() - 1);
            
            if (json.trim().isEmpty()) {
                return horarios;
            }
            
            // Dividir por objetos (asumiendo formato simple)
            String[] objetos = json.split("\\},\\s*\\{");
            
            for (String objeto : objetos) {
                objeto = objeto.trim();
                if (objeto.startsWith("{")) objeto = objeto.substring(1);
                if (objeto.endsWith("}")) objeto = objeto.substring(0, objeto.length() - 1);
                
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
    
}