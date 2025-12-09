package com.example.demo.ia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_dni")
    private String userDni;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(columnDefinition = "TEXT")
    private String userMessage;
    
    @Column(columnDefinition = "TEXT")
    private String aiResponse;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    private MessageType messageType;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    public enum MessageType {
        CHAT_GENERAL,
        CONSULTA_ACADEMICA,
        AYUDA_CURSO,
        PREGUNTA_EXAMEN,
        SOPORTE_TECNICO
    }
    
    // Constructors
    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
        this.messageType = MessageType.CHAT_GENERAL;
    }
    
    public ChatMessage(String userDni, String sessionId, String userMessage) {
        this();
        this.userDni = userDni;
        this.sessionId = sessionId;
        this.userMessage = userMessage;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getUserDni() { return userDni; }
    public void setUserDni(String userDni) { this.userDni = userDni; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
    
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
    
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
}
