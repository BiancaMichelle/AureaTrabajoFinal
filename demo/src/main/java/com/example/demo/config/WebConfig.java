package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Usar ruta absoluta para mayor seguridad y compatibilidad
        String uploadPath = java.nio.file.Paths.get("uploads").toAbsolutePath().toUri().toString();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
