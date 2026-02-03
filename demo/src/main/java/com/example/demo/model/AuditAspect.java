package com.example.demo.model;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.demo.service.AuditLogService;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Map;
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

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void registrar(JoinPoint jp, Auditable auditable, Object result) {

        AuditLog log = new AuditLog();
        LocalDateTime now = LocalDateTime.now();
        log.setFecha(Date.valueOf(now.toLocalDate()));
        log.setHora(Time.valueOf(now.toLocalTime()));

        log.setAccion(auditable.action());
        log.setAfecta(auditable.entity());
        
        // Analyze result for success/failure
        boolean exito = true;
        StringBuilder detalles = new StringBuilder("Método ejecutado: " + jp.getSignature().getName());

        if (result instanceof ResponseEntity) {
            ResponseEntity<?> response = (ResponseEntity<?>) result;
            if (!response.getStatusCode().is2xxSuccessful()) {
                exito = false;
                detalles.append(" | Status: ").append(response.getStatusCode());
            }
            if (response.getBody() instanceof Map) {
                Map<?, ?> body = (Map<?, ?>) response.getBody();
                if (Boolean.FALSE.equals(body.get("success"))) {
                    exito = false;
                    if (body.containsKey("error")) {
                        detalles.append(" | Error: ").append(body.get("error"));
                    }
                }
            }
        }
        
        log.setDetalles(detalles.toString());
        log.setExito(exito);

        setUsuarioFromAuth(log);
        log.setIp(obtenerIpCliente(request));

        auditService.registrar(log);
    }
    
    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void registrarFallo(JoinPoint jp, Auditable auditable, Throwable ex) {
        AuditLog log = new AuditLog();
        LocalDateTime now = LocalDateTime.now();
        log.setFecha(Date.valueOf(now.toLocalDate()));
        log.setHora(Time.valueOf(now.toLocalTime()));
        log.setAccion(auditable.action());
        log.setAfecta(auditable.entity());
        log.setExito(false);
        log.setDetalles("Exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage() 
            + " | Args: " + resumirArgs(jp.getArgs()));
        log.setIp(obtenerIpCliente(request));
        
        setUsuarioFromAuth(log);
        
        auditService.registrar(log);
    }

    private void setUsuarioFromAuth(AuditLog log) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUsuarioDetails) {
                CustomUsuarioDetails usuarioDetails = (CustomUsuarioDetails) principal;
                Usuario usuario = usuarioDetails.getUsuario();
                log.setUsuario(usuario);
                Set<Rol> roles = usuario.getRoles();
                if (!roles.isEmpty()) {
                    log.setRol(roles.iterator().next());
                }
            } else {
                 if (auth.getName() != null && !auth.getName().isEmpty()) {
                    // Fallback handled by services if needed, or left empty
                }
            }
        }
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

    private String resumirArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Object a : args) {
            if (a == null) continue;
            if (a instanceof Long || a instanceof Integer || a instanceof String) {
                sb.append(a.toString()).append(";");
            } else if (a instanceof Usuario) {
                Usuario u = (Usuario)a;
                sb.append("usuario=").append(u.getDni()).append(";");
            } 
        }
        return sb.toString();
    }
}
