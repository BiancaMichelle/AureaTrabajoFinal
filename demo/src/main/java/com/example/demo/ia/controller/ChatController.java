package com.example.demo.ia.controller;

import com.example.demo.ia.model.ChatMessage;
import com.example.demo.ia.service.ChatServiceSimple;
import com.example.demo.model.Auditable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ia")
public class ChatController {
    
    @Autowired
    private ChatServiceSimple chatServiceSimple;
    
    @GetMapping("/chat")
    public String mostrarChat(Model model) {
        model.addAttribute("pageTitle", "Asistente IA");
        return "ia/chat";
    }
    
    @PostMapping("/chat/message")
    @ResponseBody
    @Auditable(action = "INTERACCION_IA_CHAT", entity = "ChatMessage")
    public ResponseEntity<Map<String, Object>> enviarMensaje(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String userMessage = request.get("message");
            String sessionId = request.get("sessionId");
            
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "El mensaje no puede estar vacío"
                ));
            }
            
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = chatServiceSimple.generarSessionId();
            }
            
            String userDni = (userDetails != null) ? userDetails.getUsername() : "ANONIMO";
            
            ChatMessage chatMessage = chatServiceSimple.procesarMensaje(userMessage, userDni, sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("response", chatMessage.getAiResponse());
            response.put("sessionId", sessionId);
            response.put("messageId", chatMessage.getId().toString());
            response.put("responseTime", chatMessage.getResponseTimeMs());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("response", "Lo siento, no pude procesar tu consulta en este momento.");
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    @GetMapping("/chat/history/{sessionId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> obtenerHistorial(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<ChatMessage> historial = chatServiceSimple.obtenerHistorialSesion(sessionId);
            
            // Seguridad: Verificar que el historial pertenezca al usuario solicitante
            if (!historial.isEmpty()) {
                String ownerDni = historial.get(0).getUserDni();
                String currentDni = userDetails.getUsername();
                
                boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().contains("ADMIN"));
                
                if (!currentDni.equals(ownerDni) && !isAdmin) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
            }
            
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/chat/session/{sessionId}")
    @ResponseBody
    @Auditable(action = "LIMPIAR_SESSION_CHAT", entity = "ChatMessage")
    public ResponseEntity<Map<String, Object>> limpiarSesion(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            chatServiceSimple.limpiarSesion(sessionId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sesión limpiada exitosamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/chat/new-session")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> nuevaSesion() {
        String sessionId = chatServiceSimple.generarSessionId();
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "success", true
        ));
    }
    
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarEstado() {
        try {
            boolean ollamaConectado = chatServiceSimple.verificarConexionOllama();
            List<String> modelosDisponibles = chatServiceSimple.obtenerModelosDisponibles();
            
            Map<String, Object> status = new HashMap<>();
            status.put("ollamaConnected", ollamaConectado);
            status.put("availableModels", modelosDisponibles);
            status.put("status", ollamaConectado ? "online" : "offline");
            status.put("timestamp", System.currentTimeMillis());
            
            if (ollamaConectado) {
                status.put("message", "Servicio de IA disponible y funcionando correctamente");
            } else {
                status.put("message", "Servicio de IA no disponible. Verifica que Ollama esté ejecutándose.");
            }
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "ollamaConnected", false,
                "status", "error",
                "message", "Error al verificar estado del servicio: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarSalud() {
        try {
            boolean healthy = chatServiceSimple.verificarConexionOllama();
            Map<String, Object> health = new HashMap<>();
            health.put("status", healthy ? "UP" : "DOWN");
            health.put("service", "Ollama AI Assistant");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "DOWN",
                "service", "Ollama AI Assistant",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
