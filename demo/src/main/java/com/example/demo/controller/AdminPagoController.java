package com.example.demo.controller;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
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
import com.example.demo.enums.EstadoCuota;
import com.example.demo.model.Pago;
import com.example.demo.model.Cuota;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
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
    private OfertaAcademicaRepository ofertaAcademicaRepository;
    
    @Autowired
    private ReporteService reporteService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public String listarPagos(Model model, 
                              @RequestParam(required = false) String estado,
                              @RequestParam(required = false) String dni,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                              @RequestParam(required = false) String tipo,
                              @RequestParam(required = false) Long ofertaId) {
        
        // Obtener todos los pagos ordenados por fecha descendente
        List<Pago> pagos = pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaPago"));

        // Filtrado por estado (si se especifica)
        if (estado != null && !estado.isEmpty()) {
            pagos = pagos.stream()
                .filter(p -> p.getEstadoPago().name().equals(estado))
                .collect(Collectors.toList());
        }

        // Filtrado por DNI (si se especifica)
        if (dni != null && !dni.isEmpty()) {
            pagos = pagos.stream()
                .filter(p -> p.getUsuario() != null && p.getUsuario().getDni().contains(dni))
                .collect(Collectors.toList());
        }

        if (fechaInicio != null) {
            pagos = pagos.stream()
                .filter(p -> p.getFechaPago() != null && !p.getFechaPago().toLocalDate().isBefore(fechaInicio))
                .collect(Collectors.toList());
        }

        if (fechaFin != null) {
            pagos = pagos.stream()
                .filter(p -> p.getFechaPago() != null && !p.getFechaPago().toLocalDate().isAfter(fechaFin))
                .collect(Collectors.toList());
        }

        if (tipo != null && !tipo.isBlank()) {
            pagos = pagos.stream()
                .filter(p -> {
                    boolean esCuota = p.getCuotas() != null && !p.getCuotas().isEmpty();
                    if ("CUOTA".equalsIgnoreCase(tipo)) return esCuota;
                    if ("INSCRIPCION".equalsIgnoreCase(tipo)) return !esCuota;
                    return true;
                })
                .collect(Collectors.toList());
        }

        if (ofertaId != null) {
            pagos = pagos.stream()
                .filter(p -> p.getOferta() != null && p.getOferta().getIdOferta().equals(ofertaId))
                .collect(Collectors.toList());
        }

        Map<Long, String> estadoPagoVisual = new LinkedHashMap<>();
        for (Pago pago : pagos) {
            if (pago == null) continue;
            estadoPagoVisual.put(pago.getIdPago(), resolverEstadoVisualPago(pago));
        }

        model.addAttribute("pagos", pagos);
        model.addAttribute("estadoPagoVisual", estadoPagoVisual);
        model.addAttribute("ofertas", ofertaAcademicaRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre")));
        model.addAttribute("estados", EstadoPago.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("dniBusqueda", dni);
        model.addAttribute("fechaInicioSeleccionada", fechaInicio);
        model.addAttribute("fechaFinSeleccionada", fechaFin);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("ofertaSeleccionada", ofertaId);
        
        return "admin/gestionPagos";
    }

    private String resolverEstadoVisualPago(Pago pago) {
        boolean enMora = false;
        if (pago != null && pago.getCuotas() != null && !pago.getCuotas().isEmpty()) {
            LocalDate hoy = LocalDate.now();
            for (Cuota cuota : pago.getCuotas()) {
                if (cuota == null) continue;
                if (cuota.getEstado() == EstadoCuota.VENCIDA) {
                    enMora = true;
                    break;
                }
                if (cuota.getEstado() == EstadoCuota.PENDIENTE 
                        && cuota.getFechaVencimiento() != null 
                        && cuota.getFechaVencimiento().isBefore(hoy)) {
                    enMora = true;
                    break;
                }
            }
        }

        if (pago != null && pago.getEstadoPago() == EstadoPago.COMPLETADO) {
            return enMora ? "PAGADO_CON_MORA" : "PAGADO";
        }
        if (enMora) {
            return "MORA";
        }
        return "PENDIENTE";
    }

    @GetMapping("/reporte")
    public ResponseEntity<?> descargarReportePagos(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) String tipo,
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

            if (tipo != null && !tipo.isBlank()) {
                pagos = pagos.stream()
                    .filter(p -> {
                        boolean esCuota = p.getCuotas() != null && !p.getCuotas().isEmpty();
                        if ("CUOTA".equalsIgnoreCase(tipo)) return esCuota;
                        if ("INSCRIPCION".equalsIgnoreCase(tipo)) return !esCuota;
                        return true;
                    })
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
