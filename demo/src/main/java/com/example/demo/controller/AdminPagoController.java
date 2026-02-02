package com.example.demo.controller;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import com.example.demo.enums.EstadoPago;
import com.example.demo.model.Pago;
import com.example.demo.repository.PagoRepository;
import com.example.demo.service.ReporteService;
import com.example.demo.service.AuditLogService;
import com.example.demo.model.AuditLog;
import com.example.demo.model.CustomUsuarioDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/pagos")
public class AdminPagoController {

    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private ReporteService reporteService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public String listarPagos(Model model, 
                              @RequestParam(required = false) String estado,
                              @RequestParam(required = false) String dni) {
        
        List<Pago> pagos = pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaPago"));

        // Filtrado básico en memoria (para evitar complejidad en queries por ahora)
        // Idealmente, esto debería moverse a Specifications o Query Methods si crece mucho
        if (estado != null && !estado.isEmpty()) {
            pagos = pagos.stream()
                .filter(p -> p.getEstadoPago().name().equals(estado))
                .collect(Collectors.toList());
        }

        if (dni != null && !dni.isEmpty()) {
            pagos = pagos.stream()
                .filter(p -> p.getUsuario() != null && p.getUsuario().getDni().contains(dni))
                .collect(Collectors.toList());
        }

        model.addAttribute("pagos", pagos);
        model.addAttribute("estados", EstadoPago.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("dniBusqueda", dni);
        
        return "admin/gestionPagos";
    }

    @GetMapping("/reporte")
    public ResponseEntity<?> descargarReportePagos(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            HttpServletRequest request) {
        
        try {
            List<Pago> pagos = pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaPago"));

            if (estado != null && !estado.isEmpty()) {
                pagos = pagos.stream()
                    .filter(p -> p.getEstadoPago().name().equals(estado))
                    .collect(Collectors.toList());
            }

            if (dni != null && !dni.isEmpty()) {
                pagos = pagos.stream()
                    .filter(p -> p.getUsuario() != null && p.getUsuario().getDni().contains(dni))
                    .collect(Collectors.toList());
            }
            
            if (cursoId != null) {
                pagos = pagos.stream()
                    .filter(p -> p.getOferta() != null && p.getOferta().getIdOferta().equals(cursoId))
                    .collect(Collectors.toList());
            }

            if (fechaInicio != null) {
                pagos = pagos.stream()
                    .filter(p -> p.getFechaPago().toLocalDate().isAfter(fechaInicio.minusDays(1)))
                    .collect(Collectors.toList());
            }

            if (fechaFin != null) {
                pagos = pagos.stream()
                    .filter(p -> p.getFechaPago().toLocalDate().isBefore(fechaFin.plusDays(1)))
                    .collect(Collectors.toList());
            }

            java.io.ByteArrayInputStream pdfStream = reporteService.generarReportePagosPDF(pagos, cursoId);
            byte[] data = pdfStream.readAllBytes();
            
            // SAVE FILE
            String savedFile = null;
            try {
                String baseName = "reporte_pagos_" + System.currentTimeMillis() + ".pdf";
                Path uploadPath = Paths.get("uploads/informes");
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                Path filePath = uploadPath.resolve(baseName);
                Files.write(filePath, data);
                savedFile = baseName;
            } catch (Exception e) {}
            
            ByteArrayResource resource = new ByteArrayResource(data);


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
                    log.setAfecta("Reportes Pagos");
                    log.setDetalles("Archivo: " + savedFile + " | Exportación de pagos."); 
                    log.setExito(true);
                    log.setIp(request.getRemoteAddr());
                    
                    auditLogService.registrar(log);
                }
            } catch (Exception e) {}

            String filename = "Reporte_Pagos_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy_HHmm")) + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename)
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body("Error al generar el reporte: " + e.getMessage());
        }
    }

    @GetMapping("/comprobante/{pagoId}")
    public ResponseEntity<ByteArrayResource> descargarComprobante(@PathVariable Long pagoId) {
        try {
            Pago pago = pagoRepository.findById(pagoId)
                    .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

            java.io.ByteArrayInputStream pdfStream = reporteService.generarComprobantePagoPDF(pago);
            byte[] data = pdfStream.readAllBytes();
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=Comprobante_" + pago.getIdPago() + ".pdf")
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .contentLength(data.length)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}
