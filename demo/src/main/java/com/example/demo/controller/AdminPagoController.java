package com.example.demo.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.example.demo.repository.CuotaRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.service.ReporteService;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.EmailService;
import com.example.demo.model.AuditLog;
import com.example.demo.model.CustomUsuarioDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private CuotaRepository cuotaRepository;

    @Autowired
    private EmailService emailService;
    
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
                              @RequestParam(required = false) Long ofertaId,
                              @RequestParam(required = false) Integer atrasoMinDias) {
        
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
        model.addAttribute("atrasoMinDias", atrasoMinDias);

        // === Morosidad basada en vencimientos ===
        final LocalDate hoy = LocalDate.now();
        final int atrasoMin = (atrasoMinDias != null && atrasoMinDias >= 0) ? atrasoMinDias : 0;
        List<Map<String, Object>> morosidadRows = new ArrayList<>();

        for (Cuota cuota : cuotaRepository.findAll()) {
            if (cuota == null || cuota.getFechaVencimiento() == null) continue;
            boolean vencida = (cuota.getEstado() == EstadoCuota.VENCIDA)
                    || (cuota.getEstado() == EstadoCuota.PENDIENTE && cuota.getFechaVencimiento().isBefore(hoy));
            if (!vencida) continue;

            long diasAtraso = ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy);
            if (diasAtraso <= atrasoMin) continue;

            if (cuota.getInscripcion() == null || cuota.getInscripcion().getAlumno() == null) continue;
            var alumno = cuota.getInscripcion().getAlumno();
            var oferta = cuota.getInscripcion().getOferta();

            if (dni != null && !dni.isBlank()) {
                String dniAlumno = alumno.getDni() != null ? alumno.getDni() : "";
                if (!dniAlumno.contains(dni)) continue;
            }
            if (ofertaId != null) {
                if (oferta == null || !ofertaId.equals(oferta.getIdOferta())) continue;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("cuotaId", cuota.getIdCuota());
            row.put("dni", alumno.getDni() != null ? alumno.getDni() : "-");
            String nombreCompleto = ((alumno.getNombre() != null ? alumno.getNombre() : "") + " "
                    + (alumno.getApellido() != null ? alumno.getApellido() : "")).trim();
            row.put("alumno", nombreCompleto.isBlank() ? "-" : nombreCompleto);
            row.put("correo", alumno.getCorreo() != null ? alumno.getCorreo() : "");
            row.put("oferta", oferta != null && oferta.getNombre() != null ? oferta.getNombre() : "-");
            row.put("fechaVencimiento", cuota.getFechaVencimiento());
            row.put("diasAtraso", diasAtraso);
            row.put("monto", cuota.getMonto());
            morosidadRows.add(row);
        }
        morosidadRows.sort((a, b) -> Long.compare((Long) b.get("diasAtraso"), (Long) a.get("diasAtraso")));
        model.addAttribute("morosidadRows", morosidadRows);

        // === Graficos temporales (frontend admin) ===
        List<Pago> pagosCompletados = pagos.stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.COMPLETADO && p.getFechaPago() != null)
                .collect(Collectors.toList());
        YearMonth mesActual = YearMonth.now();
        YearMonth mesAnterior = mesActual.minusMonths(1);
        double totalMesActual = pagosCompletados.stream()
                .filter(p -> YearMonth.from(p.getFechaPago()).equals(mesActual))
                .mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0.0)
                .sum();
        double totalMesAnterior = pagosCompletados.stream()
                .filter(p -> YearMonth.from(p.getFechaPago()).equals(mesAnterior))
                .mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0.0)
                .sum();
        model.addAttribute("mesActualLabel", mesActual.getMonth().name() + " " + mesActual.getYear());
        model.addAttribute("mesAnteriorLabel", mesAnterior.getMonth().name() + " " + mesAnterior.getYear());
        model.addAttribute("totalMesActual", totalMesActual);
        model.addAttribute("totalMesAnterior", totalMesAnterior);

        List<String> labelsDias = new ArrayList<>();
        List<Double> ingresosDias = new ArrayList<>();
        for (int d = 1; d <= mesActual.lengthOfMonth(); d++) {
            final int dia = d;
            labelsDias.add(String.valueOf(dia));
            double totalDia = pagosCompletados.stream()
                    .filter(p -> YearMonth.from(p.getFechaPago()).equals(mesActual))
                    .filter(p -> p.getFechaPago().getDayOfMonth() == dia)
                    .mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0.0)
                    .sum();
            ingresosDias.add(totalDia);
        }
        model.addAttribute("labelsDias", labelsDias);
        model.addAttribute("ingresosDias", ingresosDias);
        
        return "admin/gestionPagos";
    }

    @PostMapping("/morosidad/recordatorio/{cuotaId}")
    public ResponseEntity<Map<String, Object>> enviarRecordatorioMorosidad(@PathVariable Long cuotaId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Cuota cuota = cuotaRepository.findById(cuotaId).orElse(null);
            if (cuota == null || cuota.getInscripcion() == null || cuota.getInscripcion().getAlumno() == null) {
                response.put("success", false);
                response.put("message", "No se encontro la cuota/alumno para enviar recordatorio.");
                return ResponseEntity.badRequest().body(response);
            }

            var alumno = cuota.getInscripcion().getAlumno();
            if (alumno.getCorreo() == null || alumno.getCorreo().isBlank()) {
                response.put("success", false);
                response.put("message", "El alumno no tiene correo configurado.");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate hoy = LocalDate.now();
            if (cuota.getFechaVencimiento() == null || !cuota.getFechaVencimiento().isBefore(hoy)) {
                response.put("success", false);
                response.put("message", "La cuota no esta vencida.");
                return ResponseEntity.badRequest().body(response);
            }

            long diasAtraso = ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy);
            String oferta = (cuota.getInscripcion().getOferta() != null && cuota.getInscripcion().getOferta().getNombre() != null)
                    ? cuota.getInscripcion().getOferta().getNombre()
                    : "su oferta academica";
            String subject = "Recordatorio de pago pendiente - " + oferta;
            String body = "Hola " + (alumno.getNombre() != null ? alumno.getNombre() : "estudiante") + ",\n\n"
                    + "Detectamos una cuota pendiente vencida de " + diasAtraso + " dias.\n"
                    + "Oferta: " + oferta + "\n"
                    + "Cuota: C-" + cuota.getNumeroCuota() + "\n"
                    + "Vencimiento: " + cuota.getFechaVencimiento() + "\n\n"
                    + "Por favor, regulariza el pago para evitar bloqueos de acceso.\n\n"
                    + "Saludos,\nAdministracion";
            emailService.sendEmail(alumno.getCorreo(), subject, body);

            response.put("success", true);
            response.put("message", "Recordatorio enviado a " + alumno.getCorreo());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "No se pudo enviar el recordatorio: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
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

            String periodoTexto = "Histórico completo";
            if (fechaInicio != null && fechaFin != null) {
                periodoTexto = fechaInicio.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " al " + fechaFin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else if (fechaInicio != null) {
                periodoTexto = "Desde " + fechaInicio.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else if (fechaFin != null) {
                periodoTexto = "Hasta " + fechaFin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }

            java.io.ByteArrayInputStream pdfStream = reporteService.generarReportePagosPDF(pagos, cursoId);
            byte[] data = pdfStream.readAllBytes();
            String filename = construirNombreArchivo("reporte-pagos", periodoTexto, "pdf");
            
            // SAVE FILE
            String savedFile = null;
            try {
                Path uploadPath = Paths.get("uploads/informes");
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                Path filePath = uploadPath.resolve(filename);
                Files.write(filePath, data);
                savedFile = filename;
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
                    log.setDetalles("Archivo: " + savedFile + " | Exportación de pagos. Periodo: " + periodoTexto + "."); 
                    log.setExito(true);
                    log.setIp(request.getRemoteAddr());
                    
                    auditLogService.registrar(log);
                }
            } catch (Exception e) {}

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

    private String construirNombreArchivo(String tipo, String periodo, String extension) {
        String fechaDescarga = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yy"));
        String periodoSeguro = sanitizarNombre(periodo)
                .replace(" al ", "_a_")
                .replace(" desde ", "_desde_")
                .replace(" hasta ", "_hasta_");
        return tipo + "-" + fechaDescarga + "-" + periodoSeguro + "." + extension;
    }

    private String sanitizarNombre(String input) {
        if (input == null || input.isBlank()) return "historico";
        return input
                .toLowerCase()
                .replace("/", "-")
                .replace("\\", "-")
                .replace(":", "-")
                .replace("*", "")
                .replace("?", "")
                .replace("\"", "")
                .replace("<", "")
                .replace(">", "")
                .replace("|", "")
                .replaceAll("\\s+", "_");
    }
}
