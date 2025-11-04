package com.example.demo.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JitsiClaseService {
    
    @Value("${jitsi.meet.url:https://meet.jit.si}")
    private String jitsiMeetUrl;
    
    @Value("${jitsi.room.prefix:aula-}")
    private String roomPrefix;
    
    public String generateRoomUrl(String roomName) {
        String safeRoomName = roomPrefix + roomName.toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-z0-9-]", "");
        
        return jitsiMeetUrl + "/" + safeRoomName;
    }
    
    public String generateRoomUrlWithConfig(String roomName, Map<String, String> config) {
        String baseUrl = generateRoomUrl(roomName);
        if (config != null && !config.isEmpty()) {
            try {
                String configString = config.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining("&"));
                baseUrl += "#config." + configString;
            } catch (Exception e) {
                // Fallback sin configuración
                return baseUrl;
            }
        }
        return baseUrl;
    }
}