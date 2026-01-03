package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AuditLogDTO;
import com.example.demo.model.AuditLog;
import com.example.demo.repository.AuditLogRepository;

import java.sql.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditLogDTOService {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    /**
     * Convierte AuditLog a AuditLogDTO
     */
    public AuditLogDTO convertirADTO(AuditLog auditLog) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(auditLog.getId());
        dto.setFecha(auditLog.getFecha());
        dto.setHora(auditLog.getHora());
        dto.setAccion(auditLog.getAccion());
        dto.setAfecta(auditLog.getAfecta());
        dto.setDetalles(auditLog.getDetalles());
        dto.setIp(auditLog.getIp());
        dto.setExito(auditLog.isExito());
        
        // Datos del usuario (si existe)
        if (auditLog.getUsuario() != null) {
            dto.setUsuarioNombre(auditLog.getUsuario().getNombre());
            dto.setUsuarioDni(auditLog.getUsuario().getDni());
        }
        
        // Datos del rol (si existe)
        if (auditLog.getRol() != null) {
            dto.setRolNombre(auditLog.getRol().getNombre());
        }
        
        return dto;
    }
    
    /**
     * Obtener todos los logs como DTOs paginados
     */
    public Page<AuditLogDTO> obtenerTodosDTOPaginados(Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findAll(pageable);
        return auditLogs.map(this::convertirADTO);
    }
    
    /**
     * Obtener logs por usuario como DTOs
     */
    public List<AuditLogDTO> obtenerPorUsuarioDTO(UUID usuarioId) {
        List<AuditLog> auditLogs = auditLogRepository.findByUsuarioId(usuarioId);
        return auditLogs.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener logs por acción como DTOs
     */
    public List<AuditLogDTO> obtenerPorAccionDTO(String accion) {
        List<AuditLog> auditLogs = auditLogRepository.findByAccion(accion);
        return auditLogs.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener logs por rango de fechas como DTOs
     */
    public List<AuditLogDTO> obtenerPorRangoFechasDTO(Date fechaInicio, Date fechaFin) {
        List<AuditLog> auditLogs = auditLogRepository.findByFechaBetween(fechaInicio, fechaFin);
        return auditLogs.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener logs recientes como DTOs
     */
    public Page<AuditLogDTO> obtenerLogRecientesDTO(Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findRecentLogs(pageable);
        return auditLogs.map(this::convertirADTO);
    }
    
    /**
     * Obtener estadísticas de auditoría
     */
    public EstadisticasAuditoria obtenerEstadisticas() {
        long totalLogs = auditLogRepository.count();
        long logsExitosos = auditLogRepository.findByExito(true).size();
        long logsFallidos = auditLogRepository.findByExito(false).size();
        
        EstadisticasAuditoria stats = new EstadisticasAuditoria();
        stats.setTotalLogs(totalLogs);
        stats.setLogsExitosos(logsExitosos);
        stats.setLogsFallidos(logsFallidos);
        stats.setPorcentajeExito(totalLogs > 0 ? (double) logsExitosos / totalLogs * 100 : 0);
        
        return stats;
    }
    
    /**
     * Clase para estadísticas
     */
    public static class EstadisticasAuditoria {
        private long totalLogs;
        private long logsExitosos;
        private long logsFallidos;
        private double porcentajeExito;
        
        // Getters y setters
        public long getTotalLogs() { return totalLogs; }
        public void setTotalLogs(long totalLogs) { this.totalLogs = totalLogs; }
        
        public long getLogsExitosos() { return logsExitosos; }
        public void setLogsExitosos(long logsExitosos) { this.logsExitosos = logsExitosos; }
        
        public long getLogsFallidos() { return logsFallidos; }
        public void setLogsFallidos(long logsFallidos) { this.logsFallidos = logsFallidos; }
        
        public double getPorcentajeExito() { return porcentajeExito; }
        public void setPorcentajeExito(double porcentajeExito) { this.porcentajeExito = porcentajeExito; }
    }
}