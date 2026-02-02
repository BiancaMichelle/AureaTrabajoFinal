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

import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;
    
    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public String verPaginaReportes() {
        return "admin/reportes";
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

        if ("excel".equalsIgnoreCase(formato)) {
            stream = reporteService.generarReporteExcel(ofertas);
            filename = "reporte_" + tipoReporte + ".xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
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
        }
        
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
                log.setDetalles("Exportación de ofertas. Formato: " + (formato != null ? formato : "PDF") + 
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
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/usuarios/descargar")
    public ResponseEntity<InputStreamResource> descargarReporteUsuarios(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean estado,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            HttpServletRequest request
    ) {
        List<Usuario> usuarios = reporteService.filtrarUsuarios(rol, estado, nombre, fechaInicio, fechaFin);
        ByteArrayInputStream stream = reporteService.generarReporteUsuariosPDF(usuarios, fechaInicio, fechaFin);
        
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
                log.setDetalles("Exportación de usuarios. Registros: " + usuarios.size());
                log.setExito(true);
                log.setIp(request.getRemoteAddr());
                auditLogService.registrar(log);
            }
        } catch (Exception e) {}

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_usuarios.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(stream));
    }
}