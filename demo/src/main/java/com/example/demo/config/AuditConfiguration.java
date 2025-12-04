package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuración para habilitar AspectJ y la auditoría automática
 */
@Configuration
@EnableAspectJAutoProxy
public class AuditConfiguration {
    
    // La configuración está implícita con la anotación @EnableAspectJAutoProxy
    // Esto habilita el procesamiento de @Aspect y permite que AuditAspect funcione
    
}