package com.example.demo.ia.service;

import com.example.demo.ia.config.IAConfig;
import com.example.demo.ia.model.ChatMessage;
import com.example.demo.ia.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ChatServiceSimple {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private com.example.demo.repository.OfertaAcademicaRepository ofertaAcademicaRepository;
    
    @Autowired
    private IAConfig iaConfig;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final int MAX_MESSAGES_PER_HOUR = 50;
    private static final int MAX_CONTEXT_MESSAGES = 10;
    
    public ChatMessage procesarMensaje(String userMessage, String userDni, String sessionId) {
        System.out.println("🚀 Procesando mensaje de usuario: " + userDni + " - " + userMessage);
        
        // Verificar límites de uso
        if (verificarLimitesUso(userDni)) {
            throw new RuntimeException("Has excedido el límite de mensajes por hora. Intenta más tarde.");
        }
        
        // Crear mensaje del usuario
        ChatMessage chatMessage = new ChatMessage(userDni, sessionId, userMessage);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Obtener contexto de la conversación
            List<Map<String, Object>> messages = construirHistorialMensajes(sessionId, userMessage);
            System.out.println("📚 Historial construido con " + messages.size() + " mensajes");
            
            // Generar respuesta de IA usando el patrón de chat de Ollama
            String aiResponse = generarRespuestaConChat(messages);
            System.out.println("✅ Respuesta generada: " + (aiResponse != null ? aiResponse.substring(0, Math.min(100, aiResponse.length())) + "..." : "null"));
            
            long endTime = System.currentTimeMillis();
            
            // Configurar la respuesta
            chatMessage.setAiResponse(aiResponse);
            chatMessage.setResponseTimeMs(endTime - startTime);
            chatMessage.setMessageType(determinarTipoMensaje(userMessage));
            
            // Guardar en base de datos
            return chatMessageRepository.save(chatMessage);
            
        } catch (Exception e) {
            System.err.println("❌ Error en procesarMensaje: " + e.getMessage());
            e.printStackTrace();
            chatMessage.setAiResponse(generarRespuestaError(e));
            chatMessage.setResponseTimeMs(0L);
            return chatMessageRepository.save(chatMessage);
        }
    }
    
    private List<Map<String, Object>> construirHistorialMensajes(String sessionId, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // Obtener información de ofertas académicas activas para el contexto
        String contextoOfertas = obtenerContextoOfertas();
        
        // Mensaje del sistema
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", 
            "Eres un asistente académico inteligente para una plataforma educativa llamada Aurea. " +
            "Ayudas a estudiantes y docentes con consultas sobre cursos, materiales de estudio, " +
            "evaluaciones y contenido académico. Responde de forma clara, profesional y educativa. " +
            "Mantén las respuestas concisas pero informativas. Usa formato markdown cuando sea apropiado.\n\n" +
            "INFORMACIÓN ACTUAL DE LA PLATAFORMA:\n" + contextoOfertas
        );
        messages.add(systemMessage);
        
        // Obtener contexto de conversaciones anteriores
        List<ChatMessage> recentMessages = chatMessageRepository
            .findSessionMessagesSince(sessionId, LocalDateTime.now().minusHours(2))
            .stream()
            .limit(MAX_CONTEXT_MESSAGES)
            .toList();
        
        // Agregar mensajes del historial
        for (ChatMessage msg : recentMessages) {
            if (msg.getUserMessage() != null) {
                Map<String, Object> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", msg.getUserMessage());
                messages.add(userMsg);
            }
            
            if (msg.getAiResponse() != null) {
                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", msg.getAiResponse());
                messages.add(assistantMsg);
            }
        }
        
        // Agregar el mensaje actual del usuario
        Map<String, Object> currentUserMessage = new HashMap<>();
        currentUserMessage.put("role", "user");
        currentUserMessage.put("content", userMessage);
        messages.add(currentUserMessage);
        
        return messages;
    }
    
    private String obtenerContextoOfertas() {
        try {
            List<com.example.demo.model.OfertaAcademica> ofertasActivas = ofertaAcademicaRepository.findByEstado(com.example.demo.enums.EstadoOferta.ACTIVA);
            
            if (ofertasActivas.isEmpty()) {
                return "No hay ofertas académicas activas en este momento.";
            }
            
            StringBuilder sb = new StringBuilder("Las ofertas académicas activas actualmente son:\n");
            for (com.example.demo.model.OfertaAcademica oferta : ofertasActivas) {
                sb.append("- ").append(oferta.getNombre())
                  .append(" (").append(oferta.getDescripcion()).append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("Error al obtener contexto de ofertas: " + e.getMessage());
            return "No se pudo obtener la información de ofertas académicas.";
        }
    }

    private String generarRespuestaConChat(List<Map<String, Object>> messages) {
        try {
            // Detectar modelo disponible automáticamente
            String modeloAUsar = detectarModeloDisponible();
            
            // Construir request siguiendo el patrón de la API de chat de Ollama
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modeloAUsar);
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.7,
                "top_p", 0.9,
                "num_predict", 512,
                "stop", Arrays.asList("[INST]", "[/INST]")
            ));
            
            System.out.println("🤖 Usando modelo: " + modeloAUsar);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Llamar a Ollama Chat API
            Map<String, Object> response = restTemplate.postForObject(
                iaConfig.getChatEndpoint(), 
                request, 
                Map.class
            );
            
            if (response != null && response.containsKey("message")) {
                Map<String, Object> message = (Map<String, Object>) response.get("message");
                if (message.containsKey("content")) {
                    return (String) message.get("content");
                }
            }
            
            return "Lo siento, no pude generar una respuesta en este momento.";
            
        } catch (ResourceAccessException e) {
            throw new RuntimeException("No se pudo conectar con el servidor de Ollama. " +
                                     "Verifica que Ollama esté ejecutándose en " + iaConfig.getOllamaBaseUrl(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar respuesta: " + e.getMessage(), e);
        }
    }
    
    private String generarRespuestaError(Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (errorMsg.contains("timed out") || (e.getCause() != null && e.getCause().getMessage() != null && e.getCause().getMessage().toLowerCase().contains("timed out"))) {
            return "⏱️ **El asistente está tardando demasiado**\n\n" +
                   "La consulta es compleja o el servidor está ocupado. Por favor:\n" +
                   "- Intenta con una pregunta más corta\n" +
                   "- Espera unos momentos y prueba de nuevo\n" +
                   "- Verifica que tu conexión sea estable";
        }

        if (e instanceof ResourceAccessException || e.getCause() instanceof ResourceAccessException) {
            return "🔌 **Servicio temporalmente no disponible**\n\n" +
                   "El asistente de IA no puede responder en este momento. Esto puede deberse a:\n" +
                   "- Ollama no está ejecutándose\n" +
                   "- Problemas de conectividad\n\n" +
                   "**Mientras tanto, puedes:**\n" +
                   "- Revisar la documentación de la plataforma\n" +
                   "- Contactar al soporte técnico\n" +
                   "- Intentar nuevamente en unos minutos";
        }
        
        return "⚠️ **Error temporal del sistema**\n\n" +
               "Estamos experimentando dificultades técnicas. Nuestro equipo está trabajando para resolverlo.\n\n" +
               "**Soluciones temporales:**\n" +
               "- Refrescar la página\n" +
               "- Contactar soporte si persiste";
    }
    
    private ChatMessage.MessageType determinarTipoMensaje(String mensaje) {
        String mensajeLower = mensaje.toLowerCase();
        
        if (mensajeLower.contains("curso") || mensajeLower.contains("materia") || mensajeLower.contains("clase")) {
            return ChatMessage.MessageType.AYUDA_CURSO;
        } else if (mensajeLower.contains("examen") || mensajeLower.contains("evaluación") || mensajeLower.contains("test")) {
            return ChatMessage.MessageType.PREGUNTA_EXAMEN;
        } else if (mensajeLower.contains("problema") || mensajeLower.contains("error") || mensajeLower.contains("no funciona")) {
            return ChatMessage.MessageType.SOPORTE_TECNICO;
        } else if (mensajeLower.contains("académic") || mensajeLower.contains("estudio") || mensajeLower.contains("material")) {
            return ChatMessage.MessageType.CONSULTA_ACADEMICA;
        } else {
            return ChatMessage.MessageType.CHAT_GENERAL;
        }
    }
    
    private boolean verificarLimitesUso(String userDni) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long messageCount = chatMessageRepository.countMessagesByUserSince(userDni, oneHourAgo);
        return messageCount >= MAX_MESSAGES_PER_HOUR;
    }
    
    public List<ChatMessage> obtenerHistorialSesion(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
    
    public void limpiarSesion(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }
    
    public String generarSessionId() {
        return UUID.randomUUID().toString();
    }
    
    // Método para verificar conectividad con Ollama
    public boolean verificarConexionOllama() {
        try {
            Map<String, Object> healthCheck = restTemplate.getForObject(
                iaConfig.getOllamaBaseUrl() + "/api/tags", 
                Map.class
            );
            return healthCheck != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Método para obtener modelos disponibles en Ollama
    public List<String> obtenerModelosDisponibles() {
        try {
            String url = iaConfig.getOllamaBaseUrl() + "/api/tags";
            System.out.println("🔗 Consultando modelos en: " + url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("models")) {
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
                List<String> modelNames = models.stream()
                    .map(model -> (String) model.get("name"))
                    .toList();
                System.out.println("📋 Encontrados " + modelNames.size() + " modelos: " + modelNames);
                return modelNames;
            } else {
                System.out.println("⚠️ Respuesta vacía o sin modelos");
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo modelos: " + e.getMessage());
            e.printStackTrace();
        }
        return Arrays.asList("No hay modelos disponibles");
    }
    
    // Método para detectar automáticamente un modelo disponible
    public String detectarModeloDisponible() {
        // Lista de modelos en orden de preferencia
        String[] modelosPreferidos = {
            iaConfig.getModelName(),
            "llama3.2:3b",
            "llama3.2:1b", 
            "llama3:8b",
            "gemma2:2b",
            "phi3:mini",
            "codellama:7b",
            "mistral:7b"
        };
        
        try {
            List<String> modelosDisponibles = obtenerModelosDisponibles();
            System.out.println("🔍 Modelos disponibles en Ollama: " + modelosDisponibles);
            
            // Buscar el primer modelo preferido que esté disponible
            for (String modeloPreferido : modelosPreferidos) {
                for (String modeloDisponible : modelosDisponibles) {
                    if (modeloDisponible.contains(modeloPreferido) || 
                        modeloPreferido.equals(modeloDisponible)) {
                        System.out.println("✅ Modelo encontrado: " + modeloDisponible);
                        return modeloDisponible;
                    }
                }
            }
            
            // Si no encuentra ninguno preferido, usar el primero disponible
            if (!modelosDisponibles.isEmpty() && !modelosDisponibles.get(0).equals("No hay modelos disponibles")) {
                String primerModelo = modelosDisponibles.get(0);
                System.out.println("⚠️ Usando primer modelo disponible: " + primerModelo);
                return primerModelo;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error detectando modelo: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fallback al modelo por defecto
        String modeloDefault = iaConfig.getModelName();
        System.out.println("🔄 Fallback al modelo por defecto: " + modeloDefault);
        return modeloDefault;
    }
}