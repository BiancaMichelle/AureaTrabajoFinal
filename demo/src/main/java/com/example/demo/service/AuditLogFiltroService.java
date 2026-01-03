package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AuditLogDTO;
import com.example.demo.model.AuditLog;
import com.example.demo.repository.AuditLogRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogFiltroService {

    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private AuditLogDTOService auditLogDTOService;

    /**
     * Buscar logs con filtros avanzados usando Criteria API
     */
    public Page<AuditLogDTO> buscarConFiltros(
            String usuario, String accion, String entidad, 
            Date fechaDesde, Date fechaHasta, String ip, 
            Pageable pageable) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLog> query = cb.createQuery(AuditLog.class);
        Root<AuditLog> root = query.from(AuditLog.class);
        
        // Join con Usuario y Rol con FETCH para evitar lazy loading
        root.fetch("usuario", JoinType.LEFT);
        root.fetch("rol", JoinType.LEFT);
        
        // Join para filtros (sin fetch para evitar duplicados)
        Join<Object, Object> usuarioJoin = root.join("usuario", JoinType.LEFT);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Filtro por usuario (DNI o nombre)
        if (usuario != null && !usuario.trim().isEmpty()) {
            String usuarioPattern = "%" + usuario.toLowerCase() + "%";
            Predicate dniPredicate = cb.like(cb.lower(usuarioJoin.get("dni")), usuarioPattern);
            Predicate nombrePredicate = cb.like(cb.lower(usuarioJoin.get("nombre")), usuarioPattern);
            Predicate apellidoPredicate = cb.like(cb.lower(usuarioJoin.get("apellido")), usuarioPattern);
            
            predicates.add(cb.or(dniPredicate, nombrePredicate, apellidoPredicate));
        }
        
        // Filtro por acción
        if (accion != null && !accion.trim().isEmpty()) {
            predicates.add(cb.equal(root.get("accion"), accion));
        }
        
        // Filtro por entidad afectada
        if (entidad != null && !entidad.trim().isEmpty()) {
            predicates.add(cb.equal(root.get("afecta"), entidad));
        }
        
        // Filtro por rango de fechas
        if (fechaDesde != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
        }
        
        if (fechaHasta != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
        }
        
        // Filtro por IP
        if (ip != null && !ip.trim().isEmpty()) {
            predicates.add(cb.like(root.get("ip"), "%" + ip + "%"));
        }
        
        // Aplicar predicates
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        
        // Ordenamiento por fecha y hora descendente
        query.orderBy(
            cb.desc(root.get("fecha")),
            cb.desc(root.get("hora"))
        );
        
        // DISTINCT para evitar duplicados por los JOINs
        query.distinct(true);
        
        // Ejecutar query con paginación
        TypedQuery<AuditLog> typedQuery = entityManager.createQuery(query);
        
        // Contar total de resultados
        long total = contarResultadosConFiltros(usuario, accion, entidad, fechaDesde, fechaHasta, ip);
        
        // Aplicar paginación
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<AuditLog> resultados = typedQuery.getResultList();
        
        // Convertir a DTOs
        List<AuditLogDTO> dtos = resultados.stream()
                .map(auditLogDTOService::convertirADTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, total);
    }
    
    /**
     * Contar resultados para paginación
     */
    private long contarResultadosConFiltros(
            String usuario, String accion, String entidad, 
            Date fechaDesde, Date fechaHasta, String ip) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<AuditLog> root = query.from(AuditLog.class);
        
        Join<Object, Object> usuarioJoin = root.join("usuario", JoinType.LEFT);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Aplicar los mismos filtros que en la búsqueda principal
        if (usuario != null && !usuario.trim().isEmpty()) {
            String usuarioPattern = "%" + usuario.toLowerCase() + "%";
            Predicate dniPredicate = cb.like(cb.lower(usuarioJoin.get("dni")), usuarioPattern);
            Predicate nombrePredicate = cb.like(cb.lower(usuarioJoin.get("nombre")), usuarioPattern);
            Predicate apellidoPredicate = cb.like(cb.lower(usuarioJoin.get("apellido")), usuarioPattern);
            
            predicates.add(cb.or(dniPredicate, nombrePredicate, apellidoPredicate));
        }
        
        if (accion != null && !accion.trim().isEmpty()) {
            predicates.add(cb.equal(root.get("accion"), accion));
        }
        
        if (entidad != null && !entidad.trim().isEmpty()) {
            predicates.add(cb.equal(root.get("afecta"), entidad));
        }
        
        if (fechaDesde != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
        }
        
        if (fechaHasta != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
        }
        
        if (ip != null && !ip.trim().isEmpty()) {
            predicates.add(cb.like(root.get("ip"), "%" + ip + "%"));
        }
        
        query.select(cb.count(root));
        
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        
        return entityManager.createQuery(query).getSingleResult();
    }
    
    
    /**
     * Obtener todos los logs sin filtros para exportación
     */
    public List<AuditLogDTO> obtenerTodosParaExportacion(
            String usuario, String accion, String entidad, 
            Date fechaDesde, Date fechaHasta, String ip) {
        
        // Si no hay filtros, obtener solo los últimos 1000 registros para evitar sobrecarga
        if (todosFiltrosVacios(usuario, accion, entidad, fechaDesde, fechaHasta, ip)) {
            fechaDesde = Date.valueOf(LocalDate.now().minusDays(30)); // Últimos 30 días por defecto
        }
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLog> query = cb.createQuery(AuditLog.class);
        Root<AuditLog> root = query.from(AuditLog.class);
        
        // Join con Usuario y Rol con FETCH para evitar lazy loading
        root.fetch("usuario", JoinType.LEFT);
        root.fetch("rol", JoinType.LEFT);
        
        // Join para filtros (sin fetch para evitar duplicados)
        Join<Object, Object> usuarioJoin = root.join("usuario", JoinType.LEFT);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Aplicar filtros (misma lógica que buscarConFiltros)
        if (usuario != null && !usuario.trim().isEmpty()) {
            String usuarioPattern = "%" + usuario.toLowerCase() + "%";
            Predicate dniPredicate = cb.like(cb.lower(usuarioJoin.get("dni")), usuarioPattern);
            Predicate nombrePredicate = cb.like(cb.lower(usuarioJoin.get("nombre")), usuarioPattern);
            Predicate apellidoPredicate = cb.like(cb.lower(usuarioJoin.get("apellido")), usuarioPattern);
            predicates.add(cb.or(dniPredicate, nombrePredicate, apellidoPredicate));
        }
        
        if (accion != null && !accion.trim().isEmpty()) {
            predicates.add(cb.equal(root.get("accion"), accion));
        }
        
        if (entidad != null && !entidad.trim().isEmpty()) {
            predicates.add(cb.equal(root.get("afecta"), entidad));
        }
        
        if (fechaDesde != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
        }
        
        if (fechaHasta != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
        }
        
        if (ip != null && !ip.trim().isEmpty()) {
            predicates.add(cb.like(root.get("ip"), "%" + ip + "%"));
        }
        
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        
        // DISTINCT para evitar duplicados por los JOINs
        query.distinct(true);
        
        query.orderBy(cb.desc(root.get("fecha")), cb.desc(root.get("hora")));
        
        // Limitar a 5000 registros máximo para exportación
        TypedQuery<AuditLog> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(5000);
        
        List<AuditLog> resultados = typedQuery.getResultList();
        
        return resultados.stream()
                .map(auditLogDTOService::convertirADTO)
                .collect(Collectors.toList());
    }
    
    private boolean todosFiltrosVacios(String usuario, String accion, String entidad, 
                                      Date fechaDesde, Date fechaHasta, String ip) {
        return (usuario == null || usuario.trim().isEmpty()) &&
               (accion == null || accion.trim().isEmpty()) &&
               (entidad == null || entidad.trim().isEmpty()) &&
               fechaDesde == null &&
               fechaHasta == null &&
               (ip == null || ip.trim().isEmpty());
    }
}