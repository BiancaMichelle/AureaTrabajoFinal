package com.example.demo.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JitsiClaseService {
    
    @Value("${jitsi.meet.url:https://meet.jit.si}")
    private String videoMeetUrl;
    
    @Value("${jitsi.room.prefix:aula-}")
    private String roomPrefix;
    
    public String generateRoomUrl(String roomName) {
        String safeRoomName = roomPrefix + roomName.toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-z0-9-]", "");
        
        String url = videoMeetUrl + "/" + safeRoomName;
        
        System.out.println("🎯 Generando URL Jitsi Meet:");
        System.out.println("   - Room Name: " + safeRoomName);
        System.out.println("   - URL Final: " + url);
        
        return url;
    }
    
    public String generateRoomUrlWithConfig(String roomName, Map<String, String> config) {
        String baseUrl = generateRoomUrl(roomName);
        
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
}