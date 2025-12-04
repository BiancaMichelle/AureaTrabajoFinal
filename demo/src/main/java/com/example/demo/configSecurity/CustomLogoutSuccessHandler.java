package com.example.demo.configSecurity;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.demo.model.AuditLog;
import com.example.demo.model.CustomUsuarioDetails;
import com.example.demo.model.Usuario;
import com.example.demo.service.AuditLogService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler personalizado para logout con auditoría
 */
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AuditLogService auditLogService;

    public CustomLogoutSuccessHandler(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        // Registrar auditoría de logout solo si hay autenticación
        if (authentication != null) {
            registrarLogout(authentication);
        }
        
        // Redirigir a login con mensaje de logout
        response.sendRedirect("/login?logout=true");
    }

    /**
     * Registra el logout en la auditoría
     */
    private void registrarLogout(Authentication authentication) {
        try {
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof CustomUsuarioDetails userDetails) {
                Usuario usuario = userDetails.getUsuario();
                
                AuditLog log = new AuditLog();
                
                // Fecha y hora actual
                log.setFecha(Date.valueOf(LocalDate.now()));
                log.setHora(Time.valueOf(LocalTime.now()));
                
                // Usuario y rol
                log.setUsuario(usuario);
                if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
                    log.setRol(usuario.getRoles().iterator().next());
                }
                
                // Detalles de la acción
                log.setAccion("CIERRE_SESION");
                log.setAfecta("Sistema");
                log.setDetalles(String.format("Usuario %s %s (DNI: %s) cerró sesión", 
                        usuario.getNombre(), usuario.getApellido(), usuario.getDni()));
                log.setExito(true);
                
                // IP se puede obtener desde el request si es necesario
                log.setIp(null);
                
                auditLogService.registrar(log);
                
                System.out.println("📋 Auditoría LOGOUT registrada para: " + usuario.getDni());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error al registrar logout en auditoría: " + e.getMessage());
            // No interrumpir el flujo de logout por errores de auditoría
        }
    }
}