package com.example.demo.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.model.Clase;
import com.example.demo.model.Docente;
import com.example.demo.model.Modulo;
import com.example.demo.repository.ClaseRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.ModuloRepository;

@Service
public class ClaseService {
    
    private final ClaseRepository claseRepository;
    private final ModuloRepository moduloRepository;
    private final DocenteRepository docenteRepository; // Agregar este repositorio
    
    public ClaseService(ClaseRepository claseRepository, ModuloRepository moduloRepository,
                    DocenteRepository docenteRepository) {
        this.claseRepository = claseRepository;
        this.moduloRepository = moduloRepository;
        this.docenteRepository = docenteRepository;
    }
    
    public Clase crearClase(Clase clase, UUID moduloId, String dniDocente) {
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));
        
        // Buscar el Docente en lugar del Usuario
        Docente docente = docenteRepository.findByDni(dniDocente)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
        
        // Establecer relaciones
        clase.setModulo(modulo);
        clase.setDocente(docente); // Ahora sí funciona porque es tipo Docente
        
        // Generar meeting URL
        String meetingUrl = generarMeetingUrlPublica();
        clase.setMeetingUrl(meetingUrl);
        
        System.out.println("🎯 Creando clase:");
        System.out.println("   - Título: " + clase.getTitulo());
        System.out.println("   - Meeting URL: " + meetingUrl);
        System.out.println("   - Módulo: " + modulo.getNombre());
        System.out.println("   - Docente: " + docente.getNombre());
        
        return claseRepository.save(clase);
    }
    
    public String unirseAClase(UUID claseId, String dniUsuario) {
        try {
            Clase clase = claseRepository.findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));
            
            System.out.println("🎯 Unirse a clase:");
            System.out.println("   - Clase ID: " + claseId);
            System.out.println("   - Título: " + clase.getTitulo());
            System.out.println("   - Meeting URL en BD: " + clase.getMeetingUrl());
            System.out.println("   - Usuario: " + dniUsuario);
            
            String meetingUrl;
            
            if (clase.getMeetingUrl() == null || clase.getMeetingUrl().isEmpty()) {
                // Generar nueva URL pública de prueba
                meetingUrl = generarMeetingUrlPublica();
                clase.setMeetingUrl(meetingUrl);
                claseRepository.save(clase);
                System.out.println("   - Nueva Meeting URL pública generada: " + meetingUrl);
            } else {
                meetingUrl = clase.getMeetingUrl();
                System.out.println("   - Usando Meeting URL existente: " + meetingUrl);
            }
            
            // Verificar si la URL es accesible
            System.out.println("   - ¿URL pública?: " + esUrlPublica(meetingUrl));
            
            return meetingUrl;
            
        } catch (Exception e) {
            System.out.println("❌ Error al unirse a clase: " + e.getMessage());
            throw new RuntimeException("Error al unirse a la clase: " + e.getMessage());
        }
    }
    

    private boolean esUrlPublica(String url) {
        return url != null && 
               (url.contains("meet.jit.si") || 
                url.contains("8x8.vc") || 
                url.contains("zoom.us") ||
                url.contains("meet.google.com"));
    }
    
    public Optional<Clase> findById(UUID claseId) {
        return claseRepository.findById(claseId);
    }
    
    private String generarMeetingUrlPublica() {
        // Siempre usar Jitsi Meet público
        String roomId = "aula-" + System.currentTimeMillis() + "-" + 
                       UUID.randomUUID().toString().substring(0, 4);
        String meetingUrl = "https://meet.jit.si/" + roomId;
        
        System.out.println("✅ Meeting URL pública generada: " + meetingUrl);
        System.out.println("✅ Esta URL debería funcionar desde cualquier lugar");
        
        return meetingUrl;
    }
}