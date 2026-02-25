package com.example.demo.scheduler;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.model.AuditLog;
import com.example.demo.model.Instituto;
import com.example.demo.model.Notificacion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pago;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacionRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.InstitutoService;
import com.example.demo.service.ReporteService;

@Component
public class ReporteSemanalScheduler {

    @Autowired
    private InstitutoService institutoService;

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private AuditLogService auditLogService;

    // Sábados 09:00
    @Scheduled(cron = "0 0 9 ? * SAT")
    public void generarReportesSemanales() {
        generarReportesSemanalesInterno(false);
    }

    // Ejecución manual (simula sábado)
    public String ejecutarReportesSemanalesManual() {
        return generarReportesSemanalesInterno(true);
    }

    private String generarReportesSemanalesInterno(boolean force) {
        Instituto instituto = institutoService.obtenerInstituto();
        if (instituto == null) {
            return "No hay instituto configurado.";
        }
        if (!force && !Boolean.TRUE.equals(instituto.getReportesAutomaticos())) {
            return "Reportes automáticos desactivados.";
        }

        LocalDate fechaFin = LocalDate.now();
        LocalDate fechaInicio = fechaFin.minusDays(7);
        LocalDateTime desde = fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin.plusDays(1).atStartOfDay().minusNanos(1);

        try {
            // Ofertas
            List<OfertaAcademica> ofertas = reporteService.filtrarOfertas(null, null, null, null, null, null, null);
            ByteArrayInputStream ofertasPdf = reporteService.generarReporteOfertasPDF(ofertas, null, null);
            String ofertasFile = guardarReporteEnDisco(ofertasPdf.readAllBytes(), "reporte_ofertas_semanal", "pdf");

            // Usuarios
            List<Usuario> usuarios = reporteService.filtrarUsuarios(null, null, null, fechaInicio, fechaFin);
            ByteArrayInputStream usuariosPdf = reporteService.generarReporteUsuariosPDF(usuarios, fechaInicio, fechaFin, null);
            String usuariosFile = guardarReporteEnDisco(usuariosPdf.readAllBytes(), "reporte_usuarios_semanal", "pdf");

            // Pagos
            List<Pago> pagos = pagoRepository.findPagosEnRangoFecha(desde, hasta);
            ByteArrayInputStream pagosPdf = reporteService.generarReportePagosPDF(pagos, null);
            String pagosFile = guardarReporteEnDisco(pagosPdf.readAllBytes(), "reporte_pagos_semanal", "pdf");

            // Resumen básico
            long totalOfertas = ofertas.size();
            long totalUsuarios = usuarios.size();
            long totalPagos = pagos.size();
            double totalRecaudado = pagos.stream()
                .mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0.0)
                .sum();
            long totalInscritos = ofertas.stream()
                .mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().size() : 0)
                .sum();

            String periodo = fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " al " + fechaFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String periodoOfertas = "Histórico completo";

            String resumen = "Reportes semanales generados (" + periodo + ").\n" +
                    "• Ofertas (" + periodoOfertas + "): " + totalOfertas + " | Inscripciones: " + totalInscritos + "\n" +
                    "• Usuarios nuevos: " + totalUsuarios + "\n" +
                    "• Pagos: " + totalPagos + " | Recaudado: $ " + String.format("%.2f", totalRecaudado) + "\n" +
                    "Archivos:\n" +
                    "- " + (ofertasFile != null ? ofertasFile : "reporte_ofertas_semanal") + "\n" +
                    "- " + (usuariosFile != null ? usuariosFile : "reporte_usuarios_semanal") + "\n" +
                    "- " + (pagosFile != null ? pagosFile : "reporte_pagos_semanal") + "\n" +
                    "Ir a la sección de Reportes para verlos y descargarlos.";

            // Notificar a admins por chat
            List<Usuario> admins = usuarioRepository.findByRolesNombre("ADMIN");
            for (Usuario admin : admins) {
                Notificacion notif = new Notificacion();
                notif.setUsuario(admin);
                notif.setTitulo("Reportes semanales generados");
                notif.setMensaje(resumen);
                notif.setTipo("CHAT_IA");
                notif.setLeida(false);
                notificacionRepository.save(notif);
            }

            // Auditoría por archivo
            if (!admins.isEmpty()) {
                Usuario admin = admins.get(0);
                registrarAuditLog(admin, "Reportes Ofertas", "Archivo: " + ofertasFile + " | Exportación semanal de ofertas. Registros: " + totalOfertas + " | Periodo: " + periodoOfertas);
                registrarAuditLog(admin, "Reportes Usuarios", "Archivo: " + usuariosFile + " | Exportación semanal de usuarios. Registros: " + totalUsuarios + " | Periodo: " + periodo);
                registrarAuditLog(admin, "Reportes Pagos", "Archivo: " + pagosFile + " | Exportación semanal de pagos. Registros: " + totalPagos + " | Periodo: " + periodo);
            }

            return resumen;

        } catch (Exception e) {
            System.err.println("Error generando reportes semanales: " + e.getMessage());
            e.printStackTrace();
            return "Error generando reportes semanales: " + e.getMessage();
        }
    }

    private void registrarAuditLog(Usuario admin, String afecta, String detalles) {
        try {
            AuditLog log = new AuditLog();
            long now = System.currentTimeMillis();
            log.setFecha(new java.sql.Date(now));
            log.setHora(new java.sql.Time(now));
            log.setUsuario(admin);
            if (admin.getRoles() != null && !admin.getRoles().isEmpty()) {
                log.setRol(admin.getRoles().iterator().next());
            }
            log.setAccion("EXPORTAR_REPORTE");
            log.setAfecta(afecta);
            log.setDetalles(detalles);
            log.setExito(true);
            log.setIp("SISTEMA");
            auditLogService.registrar(log);
        } catch (Exception e) {
            System.err.println("Error registrando auditoría semanal: " + e.getMessage());
        }
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
            return filename;
        } catch (Exception e) {
            System.err.println("Error guardando reporte semanal en disco: " + e.getMessage());
            return null;
        }
    }
}
