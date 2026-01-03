package com.example.demo.model;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.demo.service.AuditLogService;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Set;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditService;
    private final HttpServletRequest request;

    public AuditAspect(AuditLogService auditService, HttpServletRequest request) {
        this.auditService = auditService;
        this.request = request;
    }

    @AfterReturning("@annotation(auditable)")
    public void registrar(JoinPoint jp, Auditable auditable) {

        AuditLog log = new AuditLog();

        // ------------------------------------------------------------
        // FECHA Y HORA
        // ------------------------------------------------------------
        LocalDateTime now = LocalDateTime.now();
        log.setFecha(Date.valueOf(now.toLocalDate()));
        log.setHora(Time.valueOf(now.toLocalTime()));

        // ------------------------------------------------------------
        // ACCIÓN Y DETALLES
        // ------------------------------------------------------------
        log.setAccion(auditable.action());
        log.setAfecta(auditable.entity());
        log.setDetalles("Método ejecutado: " + jp.getSignature().getName());

        // ------------------------------------------------------------
        // USUARIO Y ROL DESDE SPRING SECURITY
        // ------------------------------------------------------------
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        System.out.println("🔍 Debug Auditoría:");
        System.out.println("  - Authentication: " + (auth != null ? "Presente" : "Null"));
        
        if (auth != null) {
            System.out.println("  - Autenticado: " + auth.isAuthenticated());
            System.out.println("  - Nombre: " + auth.getName());
            System.out.println("  - Principal tipo: " + auth.getPrincipal().getClass().getSimpleName());
        }

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {

            Object principal = auth.getPrincipal();

            // Si usás un UserDetails personalizado
            if (principal instanceof CustomUsuarioDetails usuarioDetails) {
                System.out.println(" CustomUsuarioDetails encontrado");
                Usuario usuario = usuarioDetails.getUsuario();
                System.out.println("  - Usuario: " + usuario.getNombre() + " " + usuario.getApellido());
                
                log.setUsuario(usuario);
                
                // Obtener el primer rol del usuario (o el rol principal)
                Set<Rol> roles = usuario.getRoles();
                System.out.println("  - Roles disponibles: " + roles.size());
                
                if (!roles.isEmpty()) {
                    Rol rolPrincipal = roles.iterator().next();
                    System.out.println("  - Rol asignado: " + rolPrincipal.getNombre());
                    log.setRol(rolPrincipal);
                } else {
                    System.out.println("  ⚠️ Usuario sin roles");
                }
            } else {
                System.out.println("  ❌ Principal no es CustomUsuarioDetails: " + principal.getClass().getName());
                
                // Fallback: intentar obtener usuario por DNI desde authentication name
                if (auth.getName() != null && !auth.getName().isEmpty()) {
                    System.out.println("  🔄 Intentando fallback con DNI: " + auth.getName());
                    // Aquí podrías agregar lógica para buscar el usuario por DNI si fuera necesario
                }
            }
        } else {
            System.out.println("Usuario no autenticado o anónimo");
        }

        // ------------------------------------------------------------
        // IP Y DETALLES DE RED
        // ------------------------------------------------------------
        String clientIp = obtenerIpCliente(request);
        log.setIp(clientIp);

        // ------------------------------------------------------------
        // ESTADO
        // ------------------------------------------------------------
        log.setExito(true);

        // ------------------------------------------------------------
        // GUARDAR LOG
        // ------------------------------------------------------------
        auditService.registrar(log);
    }
    
    private String obtenerIpCliente(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
                           "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
