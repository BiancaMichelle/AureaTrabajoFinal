package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AuditLogDTO;
import com.example.demo.model.AuditLog;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.AuditLogDTOService;
import com.example.demo.service.AuditLogFiltroService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/auditoria")
public class AuditLogController {
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private AuditLogDTOService auditLogDTOService;
    
    @Autowired
    private AuditLogFiltroService auditLogFiltroService;
    
    /**
     * Página principal de auditoría
     */
    @GetMapping
    public String mostrarAuditoria(Model model) {
        // Agregar datos iniciales para la vista
        model.addAttribute("totalRegistros", auditLogService.obtenerTodos().size());
        return "admin/auditoria";
    }
    
    /**
     * API REST para obtener logs de auditoría paginados con filtros
     */
    @GetMapping("/api/logs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerLogsPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) Boolean exito,
            @RequestParam(required = false) String ip) {
        
        try {
            System.out.println("=== DEBUG AUDITORIA ===");
            System.out.println("Parámetros recibidos:");
            System.out.println("- page: " + page);
            System.out.println("- size: " + size);
            System.out.println("- usuario: " + usuario);
            System.out.println("- accion: " + accion);
            System.out.println("- entidad: " + entidad);
            System.out.println("- fechaDesde: " + fechaDesde);
            System.out.println("- fechaHasta: " + fechaHasta);
            System.out.println("- exito: " + exito);
            System.out.println("- ip: " + ip);
            
            // Verificar total de registros en la base de datos
            long totalRegistros = auditLogService.obtenerTodos().size();
            System.out.println("Total de registros en BD: " + totalRegistros);
            
            // Si no hay registros, crear algunos datos de ejemplo para debug
            if (totalRegistros == 0) {
                System.out.println("¡No hay registros en la BD! Creando datos de ejemplo...");
                // Retornar respuesta vacía con mensaje
                Map<String, Object> response = new HashMap<>();
                response.put("content", List.of());
                response.put("totalElements", 0L);
                response.put("totalPages", 0);
                response.put("number", page);
                response.put("size", size);
                response.put("message", "No hay registros de auditoría en la base de datos");
                return ResponseEntity.ok(response);
            }
            
            // Crear paginación con ordenamiento por fecha descendente
            Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "fecha", "hora"));
            
            // Normalizar acciones legacy
            if (accion != null && !accion.isBlank()) {
                if ("LOGIN".equalsIgnoreCase(accion)) accion = "INICIO_SESION";
                if ("LOGOUT".equalsIgnoreCase(accion)) accion = "CIERRE_SESION";
                if ("ELIMINAR_USUARIO".equalsIgnoreCase(accion)) accion = "BAJA_USUARIO";
                if ("ACTUALIZAR_USUARIO".equalsIgnoreCase(accion)) accion = "MODIFICACION_USUARIO";
            }

            // Aplicar filtros y obtener datos
            Page<AuditLogDTO> logs = aplicarFiltrosYObtenerDatos(
                pageable, usuario, accion, entidad, fechaDesde, fechaHasta, exito, ip);
            
            System.out.println("Registros encontrados: " + logs.getTotalElements());
            System.out.println("Contenido de la página: " + logs.getContent().size());
            
            // Mostrar algunos datos de ejemplo
            if (!logs.getContent().isEmpty()) {
                AuditLogDTO primer = logs.getContent().get(0);
                System.out.println("Primer registro:");
                System.out.println("- ID: " + primer.getId());
                System.out.println("- Fecha: " + primer.getFecha());
                System.out.println("- Hora: " + primer.getHora());
                System.out.println("- Usuario: " + primer.getUsuarioNombre());
                System.out.println("- DNI: " + primer.getUsuarioDni());
                System.out.println("- Rol: " + primer.getRolNombre());
                System.out.println("- Acción: " + primer.getAccion());
                System.out.println("- Afecta: " + primer.getAfecta());
                System.out.println("- Éxito: " + primer.isExito());
            }
            
            // Calcular estadísticas
            Map<String, Object> estadisticas = calcularEstadisticas(logs.getContent());
            
            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("content", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());
            response.put("number", logs.getNumber());
            response.put("size", logs.getSize());
            response.put("estadisticas", estadisticas);
            
            System.out.println("Respuesta preparada exitosamente");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("ERROR en obtenerLogsPaginados: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", "Error al obtener logs de auditoría: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * API REST para obtener estadísticas generales
     */
    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            AuditLogDTOService.EstadisticasAuditoria stats = auditLogDTOService.obtenerEstadisticas();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalLogs", stats.getTotalLogs());
            response.put("logsExitosos", stats.getLogsExitosos());
            response.put("logsFallidos", stats.getLogsFallidos());
            response.put("porcentajeExito", stats.getPorcentajeExito());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", "Error al obtener estadísticas: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Endpoint de debug para verificar datos directos de la BD
     */
    @GetMapping("/debug/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugCount() {
        try {
            System.out.println("=== DEBUG COUNT ===");
            
            // Contar directamente en el repositorio
            long count = auditLogService.obtenerTodos().size();
            System.out.println("Total registros via service: " + count);
            
            // Obtener algunos registros recientes directamente
            Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));
            Page<AuditLog> recientes = auditLogService.obtenerTodosPaginados(pageable);
            
            System.out.println("Registros recientes encontrados: " + recientes.getContent().size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalRegistros", count);
            response.put("registrosRecientes", recientes.getContent().size());
            response.put("primerRegistro", recientes.getContent().isEmpty() ? null : recientes.getContent().get(0));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("ERROR en debugCount: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", "Error en debug: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Exportar auditoría a CSV
     */
    @GetMapping("/export/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportarCSV(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) Boolean exito,
            @RequestParam(required = false) String ip) {
        
        try {
            if (accion != null && !accion.isBlank()) {
                if ("LOGIN".equalsIgnoreCase(accion)) accion = "INICIO_SESION";
                if ("LOGOUT".equalsIgnoreCase(accion)) accion = "CIERRE_SESION";
                if ("ELIMINAR_USUARIO".equalsIgnoreCase(accion)) accion = "BAJA_USUARIO";
                if ("ACTUALIZAR_USUARIO".equalsIgnoreCase(accion)) accion = "MODIFICACION_USUARIO";
            }

            // Obtener todos los datos filtrados (sin paginación para export)
            List<AuditLogDTO> logs = obtenerDatosFiltrados(usuario, accion, entidad, fechaDesde, fechaHasta, exito, ip);
            
            // Generar CSV
            String csv = generarCSV(logs);
            
            // Configurar headers para descarga
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", 
                "auditoria_" + LocalDate.now() + ".csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv.getBytes("UTF-8"));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    

    
    // === MÉTODOS AUXILIARES ===
    
    private Page<AuditLogDTO> aplicarFiltrosYObtenerDatos(
            Pageable pageable, String usuario, String accion, String entidad, 
            Date fechaDesde, Date fechaHasta, Boolean exito, String ip) {
        
        // Si no hay filtros, usar consulta simple
        if (todosFiltrosVacios(usuario, accion, entidad, fechaDesde, fechaHasta, exito, ip)) {
            System.out.println("Sin filtros - usando consulta simple");
            
            // Obtener datos directamente del servicio
            Page<AuditLog> logs = auditLogService.obtenerTodosPaginadosConUsuarioYRol(pageable);
            System.out.println("Logs obtenidos del servicio: " + logs.getTotalElements());
            
            // Convertir a DTOs
            List<AuditLogDTO> dtos = logs.getContent().stream()
                    .map(auditLogDTOService::convertirADTO)
                    .collect(Collectors.toList());
            
            return new PageImpl<>(dtos, pageable, logs.getTotalElements());
        } else {
            System.out.println("Con filtros - usando servicio de filtros");
            // Usar el servicio de filtros avanzados
            return auditLogFiltroService.buscarConFiltros(
                usuario, accion, entidad, fechaDesde, fechaHasta, exito, ip, pageable);
        }
    }
    
    private List<AuditLogDTO> obtenerDatosFiltrados(
            String usuario, String accion, String entidad, 
            Date fechaDesde, Date fechaHasta, Boolean exito, String ip) {
        
        // Usar el servicio de filtros para exportación
        return auditLogFiltroService.obtenerTodosParaExportacion(
            usuario, accion, entidad, fechaDesde, fechaHasta, exito, ip);
    }
    
    private boolean todosFiltrosVacios(String usuario, String accion, String entidad, 
                                      Date fechaDesde, Date fechaHasta, Boolean exito, String ip) {
        return (usuario == null || usuario.trim().isEmpty()) &&
               (accion == null || accion.trim().isEmpty()) &&
               (entidad == null || entidad.trim().isEmpty()) &&
               fechaDesde == null &&
               fechaHasta == null &&
               exito == null &&
               (ip == null || ip.trim().isEmpty());
    }
    
    private Map<String, Object> calcularEstadisticas(List<AuditLogDTO> logs) {
        Map<String, Object> stats = new HashMap<>();
        
        long total = logs.size();
        long exitosos = logs.stream().mapToLong(log -> log.isExito() ? 1 : 0).sum();
        long fallidos = total - exitosos;
        
        stats.put("total", total);
        stats.put("exitosos", exitosos);
        stats.put("fallidos", fallidos);
        stats.put("porcentajeExito", total > 0 ? (double) exitosos / total * 100 : 0);
        
        return stats;
    }
    
    private String generarCSV(List<AuditLogDTO> logs) {
        StringBuilder csv = new StringBuilder();
        
        // Headers
        csv.append("Fecha,Hora,Usuario,DNI,Rol,Accion,Entidad,Detalles,IP,Estado\n");
        
        // Datos
        for (AuditLogDTO log : logs) {
            csv.append(String.format("%s,%s,\"%s\",%s,\"%s\",\"%s\",\"%s\",\"%s\",%s,%s\n",
                log.getFecha(),
                log.getHora(),
                escaparCSV(log.getUsuarioNombre()),
                log.getUsuarioDni(),
                escaparCSV(log.getRolNombre()),
                escaparCSV(log.getAccion()),
                escaparCSV(log.getAfecta()),
                escaparCSV(log.getDetalles()),
                log.getIp(),
                log.isExito() ? "Exitoso" : "Fallido"
            ));
        }
        
        return csv.toString();
    }
    
    private String escaparCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
