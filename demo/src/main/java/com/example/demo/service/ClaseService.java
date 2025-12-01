package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.model.Clase;
import com.example.demo.model.Docente;
import com.example.demo.model.Modulo;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ClaseRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class ClaseService {

    private final ClaseRepository claseRepository;
    private final ModuloRepository moduloRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final JitsiClaseService jitsiClaseService; // Cambiar esto

    public ClaseService(ClaseRepository claseRepository,
            ModuloRepository moduloRepository,
            DocenteRepository docenteRepository,
            UsuarioRepository usuarioRepository,
            JitsiClaseService jitsiClaseService) { // Cambiar esto
        this.claseRepository = claseRepository;
        this.moduloRepository = moduloRepository;
        this.docenteRepository = docenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.jitsiClaseService = jitsiClaseService; // Cambiar esto
    }

    public Optional<Clase> findById(UUID claseId) {
        return claseRepository.findById(claseId);
    }

    public Clase crearClase(Clase clase, UUID moduloId, String dniDocente) {
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

        Docente docente = docenteRepository.findByDni(dniDocente)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

        clase.setModulo(modulo);
        clase.setDocente(docente);
        clase.setCurso(modulo.getCurso());

        if (clase.getRoomName() == null) {
            clase.generateRoomName();
        }

        // ✅ Genera URL de Jitsi Meet
        String meetingUrl = jitsiClaseService.generateRoomUrl(clase.getRoomName(), docente, true);
        clase.setMeetingUrl(meetingUrl);

        System.out.println("🎯 Clase creada con Jitsi Meet:");
        System.out.println("   - Room: " + clase.getRoomName());
        System.out.println("   - Meeting URL: " + meetingUrl);
        System.out.println("   - Room Name: " + clase.getRoomName());

        return claseRepository.save(clase);
    }

    public String unirseAClase(UUID claseId, String dniUsuario) {
        try {
            Clase clase = findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));

            Usuario usuario = usuarioRepository.findByDni(dniUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean isModerator = usuario.getRoles().stream()
                    .anyMatch(r -> r.getNombre().equals("DOCENTE") || r.getNombre().equals("ADMIN"));

            System.out.println("🎯 Uniéndose a clase 8x8:");
            System.out.println("   - Clase ID: " + claseId);
            System.out.println("   - Título: " + clase.getTitulo());

            Map<String, String> config = new HashMap<>();
            config.put("prejoinPageEnabled", "false");
            config.put("startWithAudioMuted", "true");
            config.put("startWithVideoMuted", "false");

            String meetingUrl = jitsiClaseService.generateRoomUrlWithConfig(
                    clase.getRoomName(), config, usuario, isModerator);

            System.out.println("   - URL 8x8 final: " + meetingUrl);

            return meetingUrl;

        } catch (Exception e) {
            System.out.println("❌ Error al unirse a clase Jitsi: " + e.getMessage());
            throw new RuntimeException("Error al unirse a la clase: " + e.getMessage());
        }
    }

    // ✅ ELIMINAR métodos específicos de BigBlueButton
    // Ya no necesitas unirseComoEstudiante o unirseComoModerador
    // Con Jitsi todos usan la misma URL

    public List<Clase> findByModulo(UUID moduloId) {
        return claseRepository.findByModuloIdModulo(moduloId);
    }

    public List<Clase> findByDocente(String dniDocente) {
        return claseRepository.findByDocenteDni(dniDocente);
    }
}