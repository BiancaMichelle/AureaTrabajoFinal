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
                                            String mensajeHtml = String.format(
                                                "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Acceso Bloqueado</title>" +
                                                "<style>body{font-family:Arial,sans-serif;display:flex;justify-content:center;align-items:center;" +
                                                "height:100vh;margin:0;background:linear-gradient(135deg,#667eea 0%%,#764ba2 100%%)}" +
                                                ".container{background:white;padding:40px;border-radius:15px;box-shadow:0 10px 40px rgba(0,0,0,0.3);" +
                                                "text-align:center;max-width:500px}.icon{font-size:60px;color:#e74c3c;margin-bottom:20px}" +
                                                "h1{color:#333;margin-bottom:10px}p{color:#666;margin:15px 0}.stats{display:flex;justify-content:space-around;" +
                                                "margin:30px 0;padding:20px;background:#f8f9fa;border-radius:10px}.stat{text-align:center}" +
                                                ".stat-value{font-size:32px;font-weight:bold;color:#e74c3c}.stat-label{font-size:14px;color:#666;margin-top:5px}" +
                                                ".btn{display:inline-block;padding:12px 30px;margin:10px;background:#e74c3c;color:white;text-decoration:none;" +
                                                "border-radius:25px;transition:0.3s;cursor:pointer;border:none;font-size:16px}" +
                                                ".btn:hover{background:#c0392b;transform:translateY(-2px)}" +
                                                ".btn-secondary{background:#95a5a6}.btn-secondary:hover{background:#7f8c8d}</style>" +
                                                "<script>function goBack(){window.history.back();}</script></head><body>" +
                                                "<div class='container'><div class='icon'>🔒</div><h1>Material Bloqueado</h1>" +
                                                "<p>No puedes acceder a este material debido a pagos pendientes</p>" +
                                                "<div class='stats'><div class='stat'><div class='stat-value'>%d</div><div class='stat-label'>Días de Mora</div></div>" +
                                                "<div class='stat'><div class='stat-value'>%d</div><div class='stat-label'>Límite Permitido</div></div></div>" +
                                                "<a href='/alumno/mis-pagos' class='btn'>💳 Regularizar mis Pagos</a>" +
                                                "<button onclick='goBack()' class='btn btn-secondary'>← Volver</button></div></body></html>",
                                                maxDiasMora, diasMoraPermitidos
                                            );
                                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                    .contentType(MediaType.TEXT_HTML)
                                                    .body(mensajeHtml.getBytes());
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

