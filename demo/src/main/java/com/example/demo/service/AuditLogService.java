package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.model.AuditLog;
import com.example.demo.repository.AuditLogRepository;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {
    
    @Autowired
    private AuditLogRepository repo;

    public void registrar(AuditLog log) {
        repo.save(log);
    }
    
    public List<AuditLog> obtenerTodos() {
        return repo.findAll();
    }
    
    public Page<AuditLog> obtenerTodosPaginados(Pageable pageable) {
        return repo.findAll(pageable);
    }
    
    public List<AuditLog> obtenerTodosConUsuarioYRol() {
        return repo.findAllWithUserAndRole();
    }
    
    public Page<AuditLog> obtenerTodosPaginadosConUsuarioYRol(Pageable pageable) {
        return repo.findAllWithUserAndRolePaginated(pageable);
    }
    
    public List<AuditLog> obtenerPorUsuario(UUID usuarioId) {
        return repo.findByUsuarioId(usuarioId);
    }
    
    public List<AuditLog> obtenerPorAccion(String accion) {
        return repo.findByAccion(accion);
    }
    
    public List<AuditLog> obtenerPorFecha(Date fecha) {
        return repo.findByFecha(fecha);
    }
    
    public List<AuditLog> obtenerPorRangoFechas(Date fechaInicio, Date fechaFin) {
        return repo.findByFechaBetween(fechaInicio, fechaFin);
    }
}
