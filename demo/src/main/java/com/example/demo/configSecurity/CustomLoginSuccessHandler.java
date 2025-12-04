package com.example.demo.configSecurity;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.demo.model.AuditLog;
import com.example.demo.model.Auditable;
import com.example.demo.model.CustomUsuarioDetails;
import com.example.demo.model.Usuario;
import com.example.demo.service.AuditLogService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler personalizado para login exitoso con auditoría
 */
@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuditLogService auditLogService;

    public CustomLoginSuccessHandler(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        // Registrar auditoría de login
        registrarLogin(authentication);
        
        // Redirigir según el rol del usuario
        var roles = authentication.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .toList();

        if (roles.contains("ADMIN")) {
            response.sendRedirect("/admin/dashboard");
        } else {
            response.sendRedirect("/");
        }
    }

    /**
     * Registra el login en la auditoría
     */
    private void registrarLogin(Authentication authentication) {
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
                log.setAccion("INICIO_SESION");
                log.setAfecta("Sistema");
                log.setDetalles(String.format("Usuario %s %s (DNI: %s) inició sesión exitosamente", 
                        usuario.getNombre(), usuario.getApellido(), usuario.getDni()));
                log.setExito(true);
                
                // IP se puede obtener desde el request si es necesario
                log.setIp(null);
                
                auditLogService.registrar(log);
                
                System.out.println("📋 Auditoría LOGIN registrada para: " + usuario.getDni());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error al registrar login en auditoría: " + e.getMessage());
            // No interrumpir el flujo de login por errores de auditoría
        }
    }
}