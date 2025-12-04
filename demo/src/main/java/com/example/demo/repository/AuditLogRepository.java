package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.AuditLog;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Buscar por usuario
    List<AuditLog> findByUsuarioId(UUID usuarioId);
    
    // Buscar por acción
    List<AuditLog> findByAccion(String accion);
    
    // Buscar por fecha específica
    List<AuditLog> findByFecha(Date fecha);
    
    // Buscar por rango de fechas
    List<AuditLog> findByFechaBetween(Date fechaInicio, Date fechaFin);
    
    // Buscar por entidad afectada
    List<AuditLog> findByAfecta(String entidad);
    
    // Buscar por IP
    List<AuditLog> findByIp(String ip);
    
    // Buscar por éxito/fallo
    List<AuditLog> findByExito(boolean exito);
    
    // Consulta personalizada para obtener logs con información de usuario
    @Query("SELECT a FROM AuditLog a JOIN FETCH a.usuario u WHERE a.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY a.fecha DESC, a.hora DESC")
    Page<AuditLog> findLogsConUsuario(@Param("fechaInicio") Date fechaInicio, 
                                     @Param("fechaFin") Date fechaFin, 
                                     Pageable pageable);
    
    // Logs más recientes
    @Query("SELECT a FROM AuditLog a ORDER BY a.fecha DESC, a.hora DESC")
    Page<AuditLog> findRecentLogs(Pageable pageable);
    
    // Obtener todos los logs con usuario y rol cargados
    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.usuario u LEFT JOIN FETCH a.rol r ORDER BY a.fecha DESC, a.hora DESC")
    List<AuditLog> findAllWithUserAndRole();
    
    // Obtener logs paginados con usuario y rol
    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.usuario u LEFT JOIN FETCH a.rol r ORDER BY a.fecha DESC, a.hora DESC")
    Page<AuditLog> findAllWithUserAndRolePaginated(Pageable pageable);
    
    // Contar logs por usuario
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.usuario.id = :usuarioId")
    Long countByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
