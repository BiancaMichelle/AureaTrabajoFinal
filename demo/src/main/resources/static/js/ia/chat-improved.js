/**
 * Sistema de Chat IA - Frontend Mejorado
 * Maneja la interacción del usuario con el asistente de IA basado en Ollama
 */

class AIChat {
    constructor() {
        this.isOpen = false;
        this.sessionId = null;
        this.isTyping = false;
        this.messageQueue = [];
        this.rateLimitTimer = null;
        this.messagesPerMinute = 0;
        this.ollamaStatus = 'checking';
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        
        this.initializeElements();
        this.bindEvents();
        this.initializeSession();
        this.checkOllamaStatus();
        this.startStatusMonitoring();
    }
    
    initializeElements() {
        // Crear estructura HTML del chat si no existe
        if (!document.getElementById('ai-chat-toggle')) {
            this.createChatStructure();
        }
        
        this.toggleBtn = document.getElementById('ai-chat-toggle');
        this.chatContainer = document.getElementById('ai-chat-container');
        this.messagesContainer = document.getElementById('ai-chat-messages');
        this.inputField = document.getElementById('ai-chat-input');
        this.sendBtn = document.getElementById('ai-send-btn');
        this.typingIndicator = document.getElementById('ai-typing-indicator');
        this.statusIndicator = document.getElementById('ai-status-indicator');
        this.clearBtn = document.getElementById('ai-clear-btn');
        this.minimizeBtn = document.getElementById('ai-minimize-btn');
        this.connectionStatus = document.getElementById('ai-connection-status');
    }
    
    createChatStructure() {
        const chatHTML = `
            <!-- Botón flotante -->
            <button id="ai-chat-toggle" class="ai-chat-toggle" title="Asistente IA">
                <i class="fas fa-robot"></i>
                <div id="ai-chat-badge" class="ai-chat-badge" style="display: none;">1</div>
                <div class="ai-pulse-ring"></div>
            </button>
            
            <!-- Contenedor del chat -->
            <div id="ai-chat-container" class="ai-chat-container">
                <!-- Header -->
                <div class="ai-chat-header">
                    <div class="ai-chat-title">
                        <i class="fas fa-robot"></i>
                        Asistente Académico
                        <div id="ai-status-indicator" class="ai-status-indicator"></div>
                    </div>
                    <div class="ai-chat-actions">
                        <button id="ai-clear-btn" class="ai-action-btn" title="Nueva conversación">
                            <i class="fas fa-plus"></i>
                        </button>
                        <button id="ai-minimize-btn" class="ai-action-btn" title="Minimizar">
                            <i class="fas fa-minus"></i>
                        </button>
                        <button id="ai-close-btn" class="ai-action-btn" title="Cerrar">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                </div>
                
                <!-- Estado de conexión -->
                <div id="ai-connection-status" class="ai-connection-status">
                    <i class="fas fa-circle-notch fa-spin"></i> Verificando conexión...
                </div>
                
                <!-- Cuerpo del chat -->
                <div class="ai-chat-body">
                    <!-- Mensajes -->
                    <div id="ai-chat-messages" class="ai-chat-messages">
                        <div class="ai-welcome-message">
                            <h4><i class="fas fa-brain"></i> ¡Hola! Soy tu asistente académico</h4>
                            <p>Estoy aquí para ayudarte con consultas sobre cursos, materiales, evaluaciones y cualquier duda académica.</p>
                            <div class="ai-features">
                                <small>
                                    <i class="fas fa-check"></i> Powered by Ollama & Llama 3.2<br>
                                    <i class="fas fa-check"></i> Conversación con memoria contextual<br>
                                    <i class="fas fa-check"></i> Respuestas en tiempo real
                                </small>
                            </div>
                        </div>
                        
                        <!-- Sugerencias rápidas -->
                        <div class="ai-quick-suggestions">
                            <button class="ai-suggestion-btn" onclick="aiChat.sendQuickMessage('¿Cómo puedo ver mis cursos activos?')">
                                📚 Mis cursos
                            </button>
                            <button class="ai-suggestion-btn" onclick="aiChat.sendQuickMessage('Explícame cómo funciona la plataforma Aurea')">
                                🎯 Guía de uso
                            </button>
                            <button class="ai-suggestion-btn" onclick="aiChat.sendQuickMessage('¿Cuándo son mis próximos exámenes?')">
                                📝 Exámenes
                            </button>
                            <button class="ai-suggestion-btn" onclick="aiChat.sendQuickMessage('¿Cómo contactar con soporte técnico?')">
                                🔧 Soporte
                            </button>
                        </div>
                    </div>
                    
                    <!-- Indicador de escritura -->
                    <div id="ai-typing-indicator" class="ai-typing-indicator">
                        <div class="ai-message-avatar">
                            <i class="fas fa-robot"></i>
                        </div>
                        <div class="ai-typing-text">
                            <span>El asistente está pensando</span>
                            <div class="ai-typing-dots">
                                <span></span>
                                <span></span>
                                <span></span>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Footer/Input -->
                <div class="ai-chat-footer">
                    <div class="ai-input-container">
                        <textarea 
                            id="ai-chat-input" 
                            class="ai-chat-input" 
                            placeholder="Escribe tu consulta académica..."
                            rows="1"
                            maxlength="500"></textarea>
                        <button id="ai-send-btn" class="ai-send-btn" title="Enviar mensaje">
                            <i class="fas fa-paper-plane"></i>
                        </button>
                    </div>
                    <div class="ai-footer-info">
                        <small>
                            <span id="ai-char-count">0/500</span> • 
                            <span id="ai-session-info">Nueva sesión</span> • 
                            <a href="#" onclick="aiChat.showHelp()">Ayuda</a>
                        </small>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', chatHTML);
    }

    bindEvents() {
        // Botón toggle
        this.toggleBtn.addEventListener('click', () => this.toggleChat());
        
        // Botones de acción
        this.clearBtn.addEventListener('click', () => this.clearSession());
        this.minimizeBtn.addEventListener('click', () => this.minimizeChat());
        
        // Botón cerrar
        document.getElementById('ai-close-btn').addEventListener('click', () => this.closeChat());
        
        // Input y envío
        this.inputField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
        
        this.inputField.addEventListener('input', () => this.updateCharCount());
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        
        // Cerrar al hacer clic fuera
        document.addEventListener('click', (e) => {
            if (!this.chatContainer.contains(e.target) && !this.toggleBtn.contains(e.target) && this.isOpen) {
                // Solo minimizar, no cerrar completamente
                this.minimizeChat();
            }
        });
    }

    // Verificar estado de Ollama
    async checkOllamaStatus() {
        try {
            const response = await fetch('/ia/status');
            const status = await response.json();
            
            this.ollamaStatus = status.ollamaConnected ? 'online' : 'offline';
            this.updateConnectionStatus(status);
            this.reconnectAttempts = 0;
            
        } catch (error) {
            console.error('Error verificando estado de Ollama:', error);
            this.ollamaStatus = 'offline';
            this.updateConnectionStatus({
                ollamaConnected: false,
                message: 'Error de conectividad',
                status: 'error'
            });
        }
    }

    // Actualizar indicador de estado de conexión
    updateConnectionStatus(status) {
        const statusElement = this.statusIndicator;
        const connectionElement = this.connectionStatus;
        
        if (status.ollamaConnected) {
            statusElement.className = 'ai-status-indicator online';
            statusElement.innerHTML = '<i class="fas fa-circle"></i>';
            statusElement.title = 'IA Online';
            
            connectionElement.className = 'ai-connection-status online';
            connectionElement.innerHTML = `
                <i class="fas fa-check-circle"></i> Asistente IA disponible
                <small>Modelo: ${status.availableModels ? status.availableModels[0] : 'Llama 3.2'}</small>
            `;
            
            // Ocultar después de 3 segundos si está online
            setTimeout(() => {
                if (this.ollamaStatus === 'online') {
                    connectionElement.style.display = 'none';
                }
            }, 3000);
            
        } else {
            statusElement.className = 'ai-status-indicator offline';
            statusElement.innerHTML = '<i class="fas fa-exclamation-circle"></i>';
            statusElement.title = 'IA Offline';
            
            connectionElement.className = 'ai-connection-status offline';
            connectionElement.innerHTML = `
                <i class="fas fa-exclamation-triangle"></i> ${status.message || 'Servicio no disponible'}
                <button onclick="aiChat.checkOllamaStatus()" class="ai-reconnect-btn">
                    <i class="fas fa-refresh"></i> Reconectar
                </button>
            `;
            connectionElement.style.display = 'block';
        }
    }

    // Monitoreo periódico del estado
    startStatusMonitoring() {
        // Verificar cada 30 segundos
        setInterval(() => {
            this.checkOllamaStatus();
        }, 30000);
    }

    // Inicializar sesión
    async initializeSession() {
        try {
            const response = await fetch('/ia/chat/new-session', { 
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });
            const data = await response.json();
            
            if (data.success) {
                this.sessionId = data.sessionId;
                this.updateSessionInfo('Nueva sesión iniciada');
            }
        } catch (error) {
            console.error('Error inicializando sesión:', error);
            this.sessionId = this.generateTempSessionId();
        }
    }

    // Generar ID temporal de sesión
    generateTempSessionId() {
        return 'temp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    // Actualizar información de sesión
    updateSessionInfo(info) {
        const sessionElement = document.getElementById('ai-session-info');
        if (sessionElement) {
            sessionElement.textContent = info;
        }
    }

    // Limpiar sesión y crear nueva
    async clearSession() {
        if (!confirm('¿Quieres iniciar una nueva conversación? Se perderá el historial actual.')) {
            return;
        }

        try {
            // Limpiar sesión anterior si existe
            if (this.sessionId) {
                await fetch(`/ia/chat/session/${this.sessionId}`, { method: 'DELETE' });
            }
            
            // Crear nueva sesión
            await this.initializeSession();
            
            // Limpiar mensajes en la interfaz
            const messagesContainer = this.messagesContainer;
            const welcomeMessage = messagesContainer.querySelector('.ai-welcome-message');
            const suggestions = messagesContainer.querySelector('.ai-quick-suggestions');
            
            messagesContainer.innerHTML = '';
            messagesContainer.appendChild(welcomeMessage);
            messagesContainer.appendChild(suggestions);
            
            this.showNotification('Nueva conversación iniciada', 'success');
            
        } catch (error) {
            console.error('Error limpiando sesión:', error);
            this.showNotification('Error al iniciar nueva conversación', 'error');
        }
    }

    // Enviar mensaje
    async sendMessage() {
        const message = this.inputField.value.trim();
        if (!message) return;

        // Verificar límite de velocidad
        if (this.isTyping) {
            this.showNotification('Espera a que termine de responder', 'warning');
            return;
        }

        // Verificar estado de Ollama
        if (this.ollamaStatus === 'offline') {
            this.showNotification('Servicio de IA no disponible', 'error');
            return;
        }

        // Limpiar input
        this.inputField.value = '';
        this.updateCharCount();

        // Mostrar mensaje del usuario
        this.addUserMessage(message);
        
        // Mostrar indicador de escritura
        this.showTypingIndicator();
        
        try {
            const response = await fetch('/ia/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    message: message,
                    sessionId: this.sessionId
                })
            });

            const data = await response.json();
            
            if (data.success) {
                this.addAIMessage(data.response, data.responseTime);
                this.updateSessionInfo(`Sesión activa • ${data.responseTime}ms`);
            } else {
                throw new Error(data.error || 'Error desconocido');
            }

        } catch (error) {
            console.error('Error enviando mensaje:', error);
            this.addAIMessage(
                '⚠️ Lo siento, ocurrió un error al procesar tu mensaje. ' +
                'Por favor, verifica tu conexión e intenta nuevamente.',
                0
            );
        } finally {
            this.hideTypingIndicator();
        }
    }

    // Enviar mensaje rápido
    sendQuickMessage(message) {
        this.inputField.value = message;
        this.updateCharCount();
        this.sendMessage();
        
        // Ocultar sugerencias después del primer mensaje
        const suggestions = this.messagesContainer.querySelector('.ai-quick-suggestions');
        if (suggestions) {
            suggestions.style.display = 'none';
        }
    }

    // Agregar mensaje del usuario
    addUserMessage(message) {
        const messageElement = document.createElement('div');
        messageElement.className = 'ai-message ai-user-message';
        messageElement.innerHTML = `
            <div class="ai-message-content">
                <div class="ai-message-text">${this.escapeHtml(message)}</div>
                <div class="ai-message-time">${this.formatTime(new Date())}</div>
            </div>
            <div class="ai-message-avatar">
                <i class="fas fa-user"></i>
            </div>
        `;

        this.messagesContainer.appendChild(messageElement);
        this.scrollToBottom();
    }

    // Agregar mensaje de IA
    addAIMessage(message, responseTime) {
        const messageElement = document.createElement('div');
        messageElement.className = 'ai-message ai-bot-message';
        
        const formattedMessage = this.formatAIMessage(message);
        const timeInfo = responseTime ? ` • ${responseTime}ms` : '';
        
        messageElement.innerHTML = `
            <div class="ai-message-avatar">
                <i class="fas fa-robot"></i>
            </div>
            <div class="ai-message-content">
                <div class="ai-message-text">${formattedMessage}</div>
                <div class="ai-message-time">${this.formatTime(new Date())}${timeInfo}</div>
                <div class="ai-message-actions">
                    <button onclick="aiChat.copyMessage(this)" title="Copiar respuesta">
                        <i class="fas fa-copy"></i>
                    </button>
                    <button onclick="aiChat.shareMessage(this)" title="Compartir">
                        <i class="fas fa-share"></i>
                    </button>
                </div>
            </div>
        `;

        this.messagesContainer.appendChild(messageElement);
        this.scrollToBottom();
    }

    // Formatear mensaje de IA (markdown básico)
    formatAIMessage(message) {
        return message
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/`(.*?)`/g, '<code>$1</code>')
            .replace(/\n/g, '<br>');
    }

    // Mostrar/ocultar indicador de escritura
    showTypingIndicator() {
        this.isTyping = true;
        this.typingIndicator.style.display = 'flex';
        this.scrollToBottom();
    }

    hideTypingIndicator() {
        this.isTyping = false;
        this.typingIndicator.style.display = 'none';
    }

    // Actualizar contador de caracteres
    updateCharCount() {
        const count = this.inputField.value.length;
        const counter = document.getElementById('ai-char-count');
        if (counter) {
            counter.textContent = `${count}/500`;
            counter.style.color = count > 450 ? '#e74c3c' : count > 400 ? '#f39c12' : '#7f8c8d';
        }
        
        // Auto-resize textarea
        this.inputField.style.height = 'auto';
        this.inputField.style.height = Math.min(this.inputField.scrollHeight, 120) + 'px';
    }

    // Toggle chat
    toggleChat() {
        if (this.isOpen) {
            this.closeChat();
        } else {
            this.openChat();
        }
    }

    // Abrir chat
    openChat() {
        this.isOpen = true;
        this.chatContainer.classList.add('open');
        this.toggleBtn.classList.add('active');
        
        // Focus en input
        setTimeout(() => {
            this.inputField.focus();
        }, 300);
        
        // Verificar estado al abrir
        this.checkOllamaStatus();
    }

    // Cerrar chat
    closeChat() {
        this.isOpen = false;
        this.chatContainer.classList.remove('open');
        this.toggleBtn.classList.remove('active');
    }

    // Minimizar chat
    minimizeChat() {
        this.closeChat();
    }

    // Scroll al final
    scrollToBottom() {
        setTimeout(() => {
            this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
        }, 100);
    }

    // Escape HTML
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Formatear tiempo
    formatTime(date) {
        return date.toLocaleTimeString('es-ES', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }

    // Copiar mensaje
    copyMessage(button) {
        const messageText = button.closest('.ai-message-content').querySelector('.ai-message-text');
        const text = messageText.textContent;
        
        navigator.clipboard.writeText(text).then(() => {
            this.showNotification('Mensaje copiado', 'success');
        }).catch(() => {
            this.showNotification('Error al copiar', 'error');
        });
    }

    // Compartir mensaje
    shareMessage(button) {
        const messageText = button.closest('.ai-message-content').querySelector('.ai-message-text');
        const text = messageText.textContent;
        
        if (navigator.share) {
            navigator.share({
                title: 'Respuesta del Asistente IA - Aurea',
                text: text
            });
        } else {
            this.copyMessage(button);
            this.showNotification('Mensaje copiado para compartir', 'info');
        }
    }

    // Mostrar ayuda
    showHelp() {
        const helpMessage = `
            **Ayuda del Asistente IA**
            
            🤖 **¿Qué puedo hacer?**
            • Responder consultas académicas
            • Explicar funcionamiento de la plataforma
            • Ayudar con cursos y evaluaciones
            • Proporcionar soporte general
            
            ⚡ **Comandos rápidos:**
            • "mis cursos" - Ver información de cursos
            • "ayuda exámenes" - Info sobre evaluaciones
            • "soporte" - Contacto técnico
            
            🔧 **Tips:**
            • Sé específico en tus consultas
            • Usa preguntas claras y directas
            • El contexto se mantiene en la conversación
        `;
        
        this.addAIMessage(helpMessage, 0);
    }

    // Mostrar notificación
    showNotification(message, type = 'info') {
        // Crear elemento de notificación
        const notification = document.createElement('div');
        notification.className = `ai-notification ai-notification-${type}`;
        notification.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check' : type === 'error' ? 'exclamation-triangle' : 'info-circle'}"></i>
            ${message}
        `;
        
        // Agregar al DOM
        document.body.appendChild(notification);
        
        // Mostrar con animación
        setTimeout(() => notification.classList.add('show'), 100);
        
        // Ocultar después de 3 segundos
        setTimeout(() => {
            notification.classList.remove('show');
            setTimeout(() => notification.remove(), 300);
        }, 3000);
    }
}

// Inicializar chat cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    window.aiChat = new AIChat();
});