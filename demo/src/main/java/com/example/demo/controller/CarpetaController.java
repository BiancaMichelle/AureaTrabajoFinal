package com.example.demo.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.enums.EstadoCuota;
import com.example.demo.model.Archivo;
import com.example.demo.model.Carpeta;
import com.example.demo.model.Cuota;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Material;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ArchivoRepository;
import com.example.demo.repository.CarpetaRepository;
import com.example.demo.repository.CuotaRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.MaterialRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.InstitutoService;

@Controller
@RequestMapping("/carpeta")
public class CarpetaController {

    private final CarpetaRepository carpetaRepository;
    private final ArchivoRepository archivoRepository;
    private final ModuloRepository moduloRepository;
    private final MaterialRepository materialRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CuotaRepository cuotaRepository;
    private final InstitutoService institutoService;

    public CarpetaController(CarpetaRepository carpetaRepository, ArchivoRepository archivoRepository,
            ModuloRepository moduloRepository, MaterialRepository materialRepository,
            UsuarioRepository usuarioRepository, InscripcionRepository inscripcionRepository,
            CuotaRepository cuotaRepository, InstitutoService institutoService) {
        this.carpetaRepository = carpetaRepository;
        this.archivoRepository = archivoRepository;
        this.moduloRepository = moduloRepository;
        this.materialRepository = materialRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.cuotaRepository = cuotaRepository;
        this.institutoService = institutoService;
    }

    @PostMapping("/crear")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearCarpeta(@RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam UUID moduloId) {
        try {
            Modulo modulo = moduloRepository.findById(moduloId)
                    .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

            Carpeta carpeta = new Carpeta(titulo, descripcion, modulo);
            carpetaRepository.save(carpeta);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Carpeta creada exitosamente");
            response.put("carpetaId", carpeta.getIdActividad());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al crear carpeta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{carpetaId}/archivos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerArchivos(@PathVariable Long carpetaId) {
        List<Archivo> archivos = archivoRepository.findByCarpetaIdActividad(carpetaId);

        List<Map<String, Object>> archivosInfo = archivos.stream().map(archivo -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", archivo.getIdArchivo());
            info.put("nombre", archivo.getNombre());
            info.put("tipoMime", archivo.getTipoMime());
            info.put("tamano", archivo.getTamano());
            info.put("fechaSubida", archivo.getFechaSubida().toString());
            return info;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(archivosInfo);
    }

    @PostMapping("/{carpetaId}/subir")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> subirArchivo(@PathVariable Long carpetaId,
            @RequestParam("archivo") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No se seleccionó archivo"));
            }

            // Validar que sea PDF
            if (!file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Solo se permiten archivos PDF"));
            }

            Carpeta carpeta = carpetaRepository.findById(carpetaId)
                    .orElseThrow(() -> new RuntimeException("Carpeta no encontrada"));

            Archivo archivo = new Archivo();
            archivo.setNombre(file.getOriginalFilename());
            archivo.setTipoMime(file.getContentType());
            archivo.setTamano(file.getSize());
            archivo.setContenido(file.getBytes());
            archivo.setFechaSubida(LocalDateTime.now());
            archivo.setCarpeta(carpeta);

            archivoRepository.save(archivo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Archivo subido exitosamente");
            response.put("archivoId", archivo.getIdArchivo());
            response.put("nombre", archivo.getNombre());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al leer el archivo: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al subir archivo: " + e.getMessage()));
        }
    }

    @GetMapping("/archivo/{archivoId}/descargar")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long archivoId, 
            @RequestParam(required = false, defaultValue = "false") boolean download,
            Principal principal) {
        try {
            Archivo archivo = archivoRepository.findById(archivoId)
                    .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

            // Validar mora solo para alumnos
            if (principal != null) {
                String dni = principal.getName(); // En este sistema el DNI es el username
                Usuario usuario = usuarioRepository.findByDni(dni).orElse(null);
                
                if (usuario != null) {
                    // Verificar si es alumno
                    boolean esAlumno = usuario.getRoles().stream()
                            .anyMatch(rol -> "ALUMNO".equals(rol.getNombre()));
                    
                    if (esAlumno) {
                        // Obtener carpeta y su módulo
                        Carpeta carpeta = archivo.getCarpeta();
                        if (carpeta != null) {
                            Modulo modulo = carpeta.getModulo();
                            if (modulo != null) {
                                OfertaAcademica oferta = modulo.getCurso();
                            
                                // Buscar inscripción activa
                                List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDniAndOfertaId(dni, oferta.getIdOferta());
                                Inscripciones inscripcionActiva = inscripciones.stream()
                                        .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                                        .findFirst()
                                        .orElse(null);
                            
                                if (inscripcionActiva != null) {
                                    // Validar mora
                                    System.out.println("🎯 Validando mora para descarga de archivo carpeta ID: " + archivoId);
                                    com.example.demo.model.Instituto instituto = institutoService.obtenerInstituto();
                                    Integer diasMoraPermitidos = instituto.getDiasMoraBloqueoMaterial();
                                    if (diasMoraPermitidos == null) diasMoraPermitidos = 0;

                                    List<Cuota> cuotas = cuotaRepository.findByInscripcionIdInscripcion(inscripcionActiva.getIdInscripcion());
                                    List<Cuota> cuotasVencidas = cuotas.stream()
                                            .filter(c -> (c.getEstado() == EstadoCuota.PENDIENTE || c.getEstado() == EstadoCuota.VENCIDA) &&
                                                    c.getFechaVencimiento() != null &&
                                                    c.getFechaVencimiento().isBefore(LocalDate.now()))
                                            .collect(Collectors.toList());

                                    if (!cuotasVencidas.isEmpty()) {
                                        long maxDiasMora = cuotasVencidas.stream()
                                                .mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getFechaVencimiento(), LocalDate.now()))
                                                .max().orElse(0);

                                        System.out.println("🎯 Días máximos de mora: " + maxDiasMora);
                                        System.out.println("🎯 Límite permitido: " + diasMoraPermitidos);

                                        if (maxDiasMora > diasMoraPermitidos) {
                                            System.out.println("❌ ACCESO BLOQUEADO POR MORA");
                                            
                                            // Crear vista HTML usando template
                                            String htmlContent = String.format(
                                                "<!DOCTYPE html>" +
                                                "<html lang=\"es\">" +
                                                "<head>" +
                                                "<meta charset=\"UTF-8\">" +
                                                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                                                "<title>Material Bloqueado - Pagos Pendientes</title>" +
                                                "<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">" +
                                                "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">" +
                                                "<style>" +
                                                "body{margin:0;padding:0;min-height:100vh;background:linear-gradient(135deg,#f59e0b 0%%,#d97706 100%%);font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;display:flex;align-items:center;justify-content:center}" +
                                                ".mora-container{max-width:600px;width:100%%;padding:20px}" +
                                                ".mora-card{background:#ffffff;border-radius:16px;padding:40px;text-align:center;box-shadow:0 20px 60px rgba(0,0,0,0.3)}" +
                                                ".mora-icon{width:80px;height:80px;margin:0 auto 20px;background:linear-gradient(135deg,#f59e0b 0%%,#d97706 100%%);border-radius:50%%;display:flex;align-items:center;justify-content:center;color:white;font-size:40px}" +
                                                ".mora-title{font-size:28px;font-weight:700;color:#1f2937;margin-bottom:16px}" +
                                                ".mora-description{color:#6b7280;font-size:16px;line-height:1.6;margin-bottom:24px}" +
                                                ".mora-stats{display:flex;justify-content:center;gap:30px;margin:30px 0;padding:20px;background:#fef3c7;border-radius:12px;border:2px solid #fbbf24}" +
                                                ".mora-stat{text-align:center}" +
                                                ".mora-stat-label{font-size:12px;color:#92400e;text-transform:uppercase;letter-spacing:0.5px;margin-bottom:8px;font-weight:600}" +
                                                ".mora-stat-value{font-size:32px;font-weight:700;color:#d97706}" +
                                                ".mora-stat-value.limit{color:#059669}" +
                                                ".mora-buttons{display:flex;flex-direction:column;gap:12px;margin-top:24px}" +
                                                ".mora-button{display:inline-flex;align-items:center;justify-content:center;gap:8px;padding:14px 32px;border-radius:8px;text-decoration:none;font-weight:600;font-size:16px;transition:transform 0.2s,box-shadow 0.2s;border:none;cursor:pointer}" +
                                                ".mora-button-primary{background:linear-gradient(135deg,#f59e0b 0%%,#d97706 100%%);color:white;box-shadow:0 4px 12px rgba(245,158,11,0.3)}" +
                                                ".mora-button-primary:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(245,158,11,0.4);color:white}" +
                                                ".mora-button-secondary{background:#f3f4f6;color:#374151;border:2px solid #e5e7eb}" +
                                                ".mora-button-secondary:hover{background:#e5e7eb;transform:translateY(-2px);color:#374151}" +
                                                ".mora-footer{margin-top:30px;padding-top:20px;border-top:1px solid #e5e7eb;color:#6b7280;font-size:14px}" +
                                                ".alert-warning-custom{background:#fef3c7;border:2px solid #fbbf24;border-radius:8px;padding:16px;margin-bottom:24px;display:flex;align-items:center;gap:12px;text-align:left}" +
                                                ".alert-warning-custom i{color:#d97706;font-size:24px;flex-shrink:0}" +
                                                ".alert-warning-custom p{margin:0;color:#92400e;font-weight:500}" +
                                                "@media (max-width:640px){.mora-card{padding:30px 20px}.mora-stats{flex-direction:column;gap:20px}.mora-title{font-size:24px}}" +
                                                "</style>" +
                                                "</head>" +
                                                "<body>" +
                                                "<div class=\"mora-container\">" +
                                                "<div class=\"mora-card\">" +
                                                "<div class=\"mora-icon\"><i class=\"fas fa-exclamation-triangle\"></i></div>" +
                                                "<h1 class=\"mora-title\">Material Bloqueado</h1>" +
                                                "<div class=\"alert-warning-custom\">" +
                                                "<i class=\"fas fa-info-circle\"></i>" +
                                                "<p>No puede descargar este archivo debido a pagos pendientes. El resto del curso permanece accesible.</p>" +
                                                "</div>" +
                                                "<p class=\"mora-description\">Para poder descargar materiales del curso, debe regularizar sus cuotas vencidas. Puede seguir accediendo a las clases, tareas y otras actividades del curso.</p>" +
                                                "<div class=\"mora-stats\">" +
                                                "<div class=\"mora-stat\"><div class=\"mora-stat-label\">Días de Mora</div><div class=\"mora-stat-value\">%d</div></div>" +
                                                "<div class=\"mora-stat\"><div class=\"mora-stat-label\">Límite Permitido</div><div class=\"mora-stat-value limit\">%d</div></div>" +
                                                "</div>" +
                                                "<div class=\"mora-buttons\">" +
                                                "<a href=\"/alumno/mis-pagos\" class=\"mora-button mora-button-primary\"><i class=\"fas fa-credit-card\"></i> Regularizar mis Pagos</a>" +
                                                "<button onclick=\"window.history.back()\" class=\"mora-button mora-button-secondary\"><i class=\"fas fa-arrow-left\"></i> Volver</button>" +
                                                "</div>" +
                                                "<div class=\"mora-footer\"><p><i class=\"fas fa-check-circle\"></i> Una vez acreditado el pago, podrá descargar los materiales automáticamente.</p></div>" +
                                                "</div></div>" +
                                                "</body></html>",
                                                maxDiasMora, diasMoraPermitidos
                                            );
                                            
                                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                    .contentType(MediaType.TEXT_HTML)
                                                    .body(htmlContent.getBytes());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Si no está bloqueado, proceder con la descarga normal
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(archivo.getTipoMime()));
            
            org.springframework.http.ContentDisposition disposition = org.springframework.http.ContentDisposition
                    .builder(download ? "attachment" : "inline")
                    .filename(archivo.getNombre(), java.nio.charset.StandardCharsets.UTF_8)
                    .build();
            
            headers.setContentDisposition(disposition);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(archivo.getContenido());
        } catch (Exception e) {
            System.err.println("Error al descargar archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/archivo/{archivoId}/eliminar")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarArchivo(@PathVariable Long archivoId) {
        try {
            archivoRepository.deleteById(archivoId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Archivo eliminado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al eliminar archivo"));
        }
    }

    @PostMapping("/{carpetaId}/material")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearMaterial(@PathVariable Long carpetaId,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> files) {
        try {
            Carpeta carpeta = carpetaRepository.findById(carpetaId)
                    .orElseThrow(() -> new RuntimeException("Carpeta no encontrada"));

            Material material = new Material();
            material.setTitulo(titulo);
            material.setDescripcion(descripcion);
            material.setFechaCreacion(LocalDateTime.now());
            material.setVisibilidad(true);
            material.setModulo(carpeta.getModulo());
            material.setCarpeta(carpeta);

            // Si hay archivos, guardarlos
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        Archivo archivo = new Archivo();
                        archivo.setNombre(file.getOriginalFilename());
                        archivo.setTipoMime(file.getContentType());
                        archivo.setTamano(file.getSize());
                        archivo.setContenido(file.getBytes());
                        archivo.setFechaSubida(LocalDateTime.now());
                        archivo.setMaterial(material);
                        archivo.setCarpeta(carpeta);
                        
                        material.getArchivos().add(archivo);
                    }
                }
            }

            materialRepository.save(material);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Material creado exitosamente");
            response.put("materialId", material.getIdActividad());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al procesar los archivos: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al crear material: " + e.getMessage()));
        }
    }

    @GetMapping("/{carpetaId}/materiales")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerMateriales(@PathVariable Long carpetaId) {
        List<Material> materiales = materialRepository.findByCarpetaIdActividad(carpetaId);

        List<Map<String, Object>> materialesInfo = materiales.stream().map(material -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", material.getIdActividad());
            info.put("titulo", material.getTitulo());
            info.put("descripcion", material.getDescripcion());
            info.put("fechaCreacion", material.getFechaCreacion().toString());
            
            // Incluir información de los archivos
            List<Map<String, Object>> archivosInfo = material.getArchivos().stream().map(archivo -> {
                Map<String, Object> aInfo = new HashMap<>();
                aInfo.put("id", archivo.getIdArchivo());
                aInfo.put("nombre", archivo.getNombre());
                aInfo.put("tipoMime", archivo.getTipoMime());
                aInfo.put("tamano", archivo.getTamano());
                return aInfo;
            }).collect(Collectors.toList());
            
            info.put("archivos", archivosInfo);
            
            return info;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(materialesInfo);
    }

    @GetMapping("/{carpetaId}/contenido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerContenido(@PathVariable Long carpetaId) {
        Carpeta carpeta = carpetaRepository.findById(carpetaId).orElse(null);
        if (carpeta == null) return ResponseEntity.notFound().build();
        
        // 1. Archivos directos
        List<Map<String, Object>> archivosDirectos = carpeta.getArchivos().stream().map(archivo -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", archivo.getIdArchivo());
            info.put("nombre", archivo.getNombre());
            info.put("tipoMime", archivo.getTipoMime());
            info.put("tamano", archivo.getTamano());
            info.put("tipo", "ARCHIVO");
            return info;
        }).collect(Collectors.toList());

        // 2. Materiales (con sus archivos)
        List<Map<String, Object>> materiales = carpeta.getMateriales().stream().map(material -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", material.getIdActividad());
            info.put("titulo", material.getTitulo());
            info.put("descripcion", material.getDescripcion());
            info.put("tipo", "MATERIAL");
            
            List<Map<String, Object>> archs = material.getArchivos().stream().map(archivo -> {
                Map<String, Object> aInfo = new HashMap<>();
                aInfo.put("id", archivo.getIdArchivo());
                aInfo.put("nombre", archivo.getNombre());
                return aInfo;
            }).collect(Collectors.toList());
            
            info.put("archivos", archs);
            return info;
        }).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("archivos", archivosDirectos);
        response.put("materiales", materiales);
        
        return ResponseEntity.ok(response);
    }
}

