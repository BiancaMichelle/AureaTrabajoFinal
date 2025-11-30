package com.example.demo.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.demo.model.Usuario;

@Service
public class JitsiClaseService {

    @Value("${jitsi.meet.url:https://meet.jit.si}")
    private String videoMeetUrl;

    @Value("${jitsi.room.prefix:aula-}")
    private String roomPrefix;
    
    @Value("${jaas.appId:}")
    private String appId;

    private final JaaSTokenService jaasTokenService;

    public JitsiClaseService(JaaSTokenService jaasTokenService) {
        this.jaasTokenService = jaasTokenService;
    }

    public String generateRoomUrl(String roomName, Usuario usuario, boolean isModerator) {
        String safeRoomName = roomPrefix + roomName.toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-z0-9-]", "");

        String url;
        // Check if using JaaS (if appId is configured)
        if (appId != null && !appId.isEmpty()) {
             url = "https://8x8.vc/" + appId + "/" + safeRoomName;
             if (usuario != null) {
                 String token = jaasTokenService.generateToken(
                     usuario.getNombre() + " " + usuario.getApellido(), 
                     usuario.getCorreo(), 
                     "", 
                     isModerator
                 );
                 url += "?jwt=" + token;
             }
        } else {
            // Fallback to self-hosted/public Jitsi
            if (videoMeetUrl.endsWith("/")) {
                videoMeetUrl = videoMeetUrl.substring(0, videoMeetUrl.length() - 1);
            }
            url = videoMeetUrl + "/" + safeRoomName;
        }

        System.out.println("🎯 Generando URL Jitsi/JaaS:");
        System.out.println("   - Room Name: " + safeRoomName);
        System.out.println("   - URL Final: " + url);

        return url;
    }

    public String generateRoomUrlWithConfig(String roomName, Map<String, String> config, Usuario usuario, boolean isModerator) {
        String baseUrl = generateRoomUrl(roomName, usuario, isModerator);

        if (config != null && !config.isEmpty()) {
            try {
                String configString = config.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining("&"));
                baseUrl += "#" + configString;

                System.out.println("   - Configuración: " + configString);
                System.out.println("   - URL con Config: " + baseUrl);

            } catch (Exception e) {
                System.out.println("⚠️ Error en configuración, usando URL base: " + baseUrl);
            }
        }
        return baseUrl;
    }
    
    // Deprecated methods for backward compatibility if needed
    public String generateRoomUrl(String roomName) {
        return generateRoomUrl(roomName, null, false);
    }
    
    public String generateRoomUrlWithConfig(String roomName, Map<String, String> config) {
        return generateRoomUrlWithConfig(roomName, config, null, false);
    }
}