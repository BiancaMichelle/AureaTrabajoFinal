package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.model.CustomUsuarioDetails;
import com.example.demo.model.AuditLog;
import com.example.demo.service.ReporteService;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.AnalisisRendimientoService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;
    
    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AnalisisRendimientoService analisisRendimientoService;

    @GetMapping
    public String verPaginaReportes(org.springframework.ui.Model model) {
        // Obtener historial de reportes (AuditLog)
        try {
            List<AuditLog> reportes = auditLogService.obtenerPorAccion("EXPORTAR_REPORTE");
            // Ordenar por fecha desc (aunque el repo ya podría hacerlo, aseguramos)
            reportes.sort((a, b) -> {
                int fechaCmp = b.getFecha().compareTo(a.getFecha());
                if (fechaCmp != 0) return fechaCmp;
                return b.getHora().compareTo(a.getHora());
            });
            model.addAttribute("reportes", reportes);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("reportes", Collections.emptyList());
        }
        return "admin/reportes";
    }

    @PostMapping("/ejecutar-analisis")
    public String ejecutarAnalisisManual(RedirectAttributes redirectAttributes) {
        try {
            // Ejecutar anteriormente llamaba a la baja automática inmediata. Para evitar bajas sin control,
            // ahora redirigimos a la página y sugerimos usar el botón de 'Ver Ofertas en Peligro'.
            redirectAttributes.addFlashAttribute("mensaje", "Se puede revisar las ofertas en peligro desde el botón 'Mantenimiento Automático'.");
            redirectAttributes.addFlashAttribute("tipoMensaje", "info");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/admin/reportes";
    }

    // Endpoint JSON: devolver ofertas en peligro sin aplicar bajas
    @GetMapping(value = "/ofertas/en-peligro", produces = "application/json")
    @ResponseBody
    public List<?> listarOfertasEnPeligro() {
        return analisisRendimientoService.obtenerOfertasEnPeligro();
    }

    // Endpoint JSON: aplicar bajas para las ofertas seleccionadas (registra auditoría)
    @PostMapping(value = "/ofertas/aplicar-bajas", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> aplicarBajasSeleccionadas(@RequestBody List<Long> ofertaIds, HttpServletRequest request) {
        if (ofertaIds == null || ofertaIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "No se recibieron ids"));
        }
        try {
            // Obtener contexto de riesgos para incluir en auditoría
            List<?> riesgos = analisisRendimientoService.obtenerOfertasEnPeligro();
            java.util.Map<Long, Object> mapaRiesgos = new java.util.HashMap<>();
            for (Object o : riesgos) {
                try {
                    java.lang.reflect.Method m = o.getClass().getMethod("id");
                    Long id = (Long) m.invoke(o);
                    mapaRiesgos.put(id, o);
                } catch (Exception ignored) {}
            }

            // Construir detalles para auditoría antes de aplicar
            StringBuilder detalles = new StringBuilder();
            for (Long id : ofertaIds) {
                if (mapaRiesgos.containsKey(id)) {
                    Object o = mapaRiesgos.get(id);
                    try {
                        java.lang.reflect.Method getNombre = o.getClass().getMethod("nombre");
                        java.lang.reflect.Method getMotivo = o.getClass().getMethod("motivo");
                        detalles.append(getNombre.invoke(o)).append(" - ").append(getMotivo.invoke(o)).append("; ");
                    } catch (Exception ex) {
                        detalles.append("Oferta id= ").append(id).append("; ");
                    }
                } else {
                    detalles.append("Oferta id= ").append(id).append(" (no en lista de riesgo); ");
                }
            }

            // Ejecutar bajas
            analisisRendimientoService.aplicarBajasSeleccionadas(ofertaIds);

            // Registrar auditoría
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof CustomUsuarioDetails) {
                    com.example.demo.model.Usuario usuario = ((CustomUsuarioDetails) auth.getPrincipal()).getUsuario();
                    AuditLog log = new AuditLog();
                    long now = System.currentTimeMillis();
                    log.setFecha(new java.sql.Date(now));
                    log.setHora(new java.sql.Time(now));
                    log.setUsuario(usuario);
                    if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
                        log.setRol(usuario.getRoles().iterator().next());
                    }
                    log.setAccion("DAR_DE_BAJA_OFERTAS_MANUAL");
                    log.setAfecta("OfertaAcademica");
                    log.setDetalles(detalles.toString());
                    log.setExito(true);
                    log.setIp(request.getRemoteAddr());
                    auditLogService.registrar(log);
                }
            } catch (Exception e) {
                System.err.println("Error registrando auditoría: " + e.getMessage());
            }

            return ResponseEntity.ok(Collections.singletonMap("success", true));
        } catch (Exception e) {
            // Registrar auditoría de fallo
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof CustomUsuarioDetails) {
                    com.example.demo.model.Usuario usuario = ((CustomUsuarioDetails) auth.getPrincipal()).getUsuario();
                    AuditLog log = new AuditLog();
                    long now = System.currentTimeMillis();
                    log.setFecha(new java.sql.Date(now));
                    log.setHora(new java.sql.Time(now));
                    log.setUsuario(usuario);
                    if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
                        log.setRol(usuario.getRoles().iterator().next());
                    }
                    log.setAccion("DAR_DE_BAJA_OFERTAS_MANUAL");
                    log.setAfecta("OfertaAcademica");
                    log.setDetalles("Error: " + e.getMessage());
                    log.setExito(false);
                    log.setIp(request.getRemoteAddr());
                    auditLogService.registrar(log);
                }
            } catch (Exception ex) {}

            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/ofertas/descargar")
    public ResponseEntity<InputStreamResource> descargarReporte(
            @RequestParam(required = false) String formato, // pdf o excel
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) List<String> tipos,
            @RequestParam(required = false, defaultValue = "ofertas") String tipoReporte,
            HttpServletRequest request
    ) throws Exception {
        
        // Validación de backend: Se requiere al menos un tipo de oferta seleccionado
        if (tipos == null || tipos.isEmpty()) {
            return ResponseEntity.badRequest().body(null); 
        }

        List<OfertaAcademica> ofertas = reporteService.filtrarOfertas(nombre, estado, categoriaId, fechaInicio, fechaFin, tipos);
        
        ByteArrayInputStream stream;
        String filename;
        MediaType mediaType;
        String fileExtension;

        if ("excel".equalsIgnoreCase(formato)) {
            stream = reporteService.generarReporteExcel(ofertas);
            filename = "reporte_" + tipoReporte + ".xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            fileExtension = "xlsx";
        } else {
            // Default to PDF
            if ("estadistico".equalsIgnoreCase(tipoReporte)) {
                stream = reporteService.generarReporteEstadisticoPDF(ofertas, fechaInicio, fechaFin);
                filename = "reporte_estadistico.pdf";
            } else {
                stream = reporteService.generarReporteOfertasPDF(ofertas, fechaInicio, fechaFin);
                filename = "reporte_ofertas_profesional.pdf";
            }
            mediaType = MediaType.APPLICATION_PDF;
            fileExtension = "pdf";
        }
        
        byte[] content = stream.readAllBytes();
        String savedFile = guardarReporteEnDisco(content, "reporte_" + tipoReporte, fileExtension);

        // Registrar Auditoría
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUsuarioDetails) {
                com.example.demo.model.Usuario usuario = ((CustomUsuarioDetails) auth.getPrincipal()).getUsuario();
                
                AuditLog log = new AuditLog();
                long now = System.currentTimeMillis();
                log.setFecha(new java.sql.Date(now));
                log.setHora(new java.sql.Time(now));
                log.setUsuario(usuario);
                
                if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
                    log.setRol(usuario.getRoles().iterator().next());
                }
                
                log.setAccion("EXPORTAR_REPORTE");
                log.setAfecta("Reportes");
                // Guardamos el nombre del archivo al inicio de los detalles para parsing fácil
                log.setDetalles("Archivo: " + savedFile + " | Exportación de ofertas. Formato: " + (formato != null ? formato : "PDF") + 
                                ". Registros: " + ofertas.size());
                log.setExito(true);
                log.setIp(request.getRemoteAddr());
                
                auditLogService.registrar(log);
            }
        } catch (Exception e) {
            System.err.println("Error registrando auditoría: " + e.getMessage());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(new InputStreamResource(new java.io.ByteArrayInputStream(content)));
    }

    @GetMapping("/usuarios/descargar")
    public ResponseEntity<InputStreamResource> descargarReporteUsuarios(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean estado,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            HttpServletRequest request
    ) throws java.io.IOException { // Added exception
        List<Usuario> usuarios = reporteService.filtrarUsuarios(rol, estado, nombre, fechaInicio, fechaFin);
        ByteArrayInputStream stream = reporteService.generarReporteUsuariosPDF(usuarios, fechaInicio, fechaFin);
        
        byte[] content = stream.readAllBytes();
        String savedFile = guardarReporteEnDisco(content, "reporte_usuarios", "pdf");

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUsuarioDetails) {
                com.example.demo.model.Usuario usuario = ((CustomUsuarioDetails) auth.getPrincipal()).getUsuario();
                AuditLog log = new AuditLog();
                long now = System.currentTimeMillis();
                log.setFecha(new java.sql.Date(now));
                log.setHora(new java.sql.Time(now));
                log.setUsuario(usuario);
                if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
                    log.setRol(usuario.getRoles().iterator().next());
                }
                log.setAccion("EXPORTAR_REPORTE");
                log.setAfecta("Reportes Usuarios");
                log.setDetalles("Archivo: " + savedFile + " | Exportación de usuarios. Registros: " + usuarios.size());
                log.setExito(true);
                log.setIp(request.getRemoteAddr());
                auditLogService.registrar(log);
            }
        } catch (Exception e) {}

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_usuarios.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(new ByteArrayInputStream(content)));
    }

    @GetMapping("/archivo/{filename:.+}")
    public ResponseEntity<InputStreamResource> descargarArchivoGuardado(@PathVariable String filename) {
        try {
            // Validate filename to prevent directory traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path uploadPath = Paths.get("uploads/informes");
            Path file = uploadPath.resolve(filename).normalize();
            
            System.out.println("Intentando descargar archivo: " + file.toAbsolutePath().toString());

            if (Files.exists(file)) {
                InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
                MediaType mediaType = MediaType.APPLICATION_PDF;
                if (filename.endsWith(".xlsx")) {
                    mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                }
                
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                        .body(resource);
            } else {
                System.out.println("Archivo no encontrado en ruta: " + file.toAbsolutePath().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.notFound().build();
    }

    private String guardarReporteEnDisco(byte[] contenido, String nombreBase, String extension) {
        try {
            String filename = nombreBase + "_" + System.currentTimeMillis() + "." + extension;
            Path uploadPath = Paths.get("uploads/informes");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, contenido);
            System.out.println("Archivo guardado exitosamente: " + filePath.toAbsolutePath());
            return filename;
        } catch (Exception e) {
            System.err.println("Error guardando reporte en disco: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}