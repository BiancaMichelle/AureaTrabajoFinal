package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Auditable;
import com.example.demo.service.AuditLogService;

import java.util.Map;

/**
 * Controlador de prueba para verificar que el sistema de auditoría funciona
 */
@Controller
@RequestMapping("/test-auditoria")
public class TestAuditoriaController {
    
    @Autowired
    private AuditLogService auditLogService;
    
    /**
     * Endpoint de prueba para generar un log de auditoría
     */
    @PostMapping("/prueba")
    @Auditable(action = "PRUEBA_AUDITORIA", entity = "Sistema")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pruebaAuditoria() {
        Map<String, Object> response = Map.of(
            "success", true,
            "message", "Acción de prueba ejecutada correctamente",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
}