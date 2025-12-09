/**
 * Sistema de Chat IA - Frontend
 * Maneja la interacción del usuario con el asistente de IA
 */

// Usamos una expresión de clase asignada a window para evitar el error "Identifier has already been declared"
if (typeof window.AIChat === 'undefined') {
    window.AIChat = class {
        constructor() {
            this.isOpen = false;
            this.sessionId = null;
            this.isTyping = false;
            this.messageQueue = [];
            this.rateLimitTimer = null;
            this.messagesPerMinute = 0;
            
            this.initializeElements();
            // Solo vincular eventos si se encontraron los elementos
            if (this.toggleBtn) {
                this.bindEvents();
                this.initializeSession();
            }
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
    }
    
    createChatStructure() {
        const chatHTML = `
            <!-- Botón flotante -->
            <button id="ai-chat-toggle" class="ai-chat-toggle" title="Asistente IA">
                <i class="fas fa-robot"></i>
                <div id="ai-chat-badge" class="ai-chat-badge" style="display: none;">1</div>
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
                        <button id="ai-clear-btn" class="ai-action-btn" title="Limpiar chat">
                            <i class="fas fa-trash"></i>
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
                    Conectando con el asistente...
                </div>
                
                <!-- Cuerpo del chat -->
                <div class="ai-chat-body">
                    <!-- Mensajes -->
                    <div id="ai-chat-messages" class="ai-chat-messages">
                        <div class="ai-welcome-message">
                            <h4><i class="fas fa-brain"></i> ¡Hola! Soy tu asistente académico</h4>
                            <p>Puedo ayudarte con consultas sobre cursos, materiales, evaluaciones y más.</p>
                        </div>
                        
                        <!-- Sugerencias rápidas -->
                        <div class="ai-quick-suggestions">
                            <button class="ai-suggestion-btn" onclick="aiChat.sendQuickMessage('¿Cómo puedo ver mis cursos?')">
                                Mis cursos
                            </button>
                            <button class="ai-suggestion-btn" onclick="aiChat.sendQuickMessage('¿Cómo funciona la plataforma?')">
                                Ayuda general
                            </button>
                            <button class="ai-suggestion-btn" onclick="aiChat.sendQuickMessage('¿Cuándo son mis exámenes?')">
                                Exámenes
                            </button>
                        </div>
                    </div>
                    
                    <!-- Indicador de escritura -->
                    <div id="ai-typing-indicator" class="ai-typing-indicator">
                        <div class="ai-message-avatar">
                            <i class="fas fa-robot"></i>
                        </div>
                        <div style="font-size: 12px; color: #7f8c8d;">
                            El asistente está escribiendo
                            <div class="ai-typing-dots">
                                <span></span><span></span><span></span>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Input -->
                <div class="ai-chat-input">
                    <input type="text" id="ai-chat-input" class="ai-input-field" 
                           placeholder="Escribe tu consulta..." maxlength="500">
                    <button id="ai-send-btn" class="ai-send-btn">
                        <i class="fas fa-paper-plane"></i>
                    </button>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', chatHTML);
    }
    
    bindEvents() {
        // Botón toggle
        if (this.toggleBtn) {
            this.toggleBtn.addEventListener('click', () => this.toggleChat());
        }
        
        // Botón enviar
        if (this.sendBtn) {
            this.sendBtn.addEventListener('click', () => this.sendMessage());
        }
        
        // Enter en input
        if (this.inputField) {
            this.inputField.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });
        }
        
        // Botón limpiar
        const clearBtn = document.getElementById('ai-clear-btn');
        if (clearBtn) {
            clearBtn.addEventListener('click', () => this.clearChat());
        }
        
        // Botón cerrar
        const closeBtn = document.getElementById('ai-close-btn');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => this.closeChat());
        }
        
        // Botón minimizar
        if (this.minimizeBtn) {
            this.minimizeBtn.addEventListener('click', () => this.minimizeChat());
        }
        
        // Click fuera del chat para cerrar
        document.addEventListener('click', (e) => {
            if (this.isOpen && this.chatContainer && this.toggleBtn && 
                !this.chatContainer.contains(e.target) && !this.toggleBtn.contains(e.target)) {
                this.closeChat();
            }
        });
        
        // Control de rate limiting
        setInterval(() => {
            this.messagesPerMinute = 0;
        }, 60000);
    }
    
    async initializeSession() {
        try {
            const response = await fetch('/ia/chat/new-session', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            
            const data = await response.json();
            if (data.success) {
                this.sessionId = data.sessionId;
                this.updateConnectionStatus('connected');
            }
        } catch (error) {
            console.error('Error inicializando sesión:', error);
            this.updateConnectionStatus('error');
        }
    }
    
    toggleChat() {
        if (this.isOpen) {
            this.closeChat();
        } else {
            this.openChat();
        }
    }
    
    openChat() {
        this.isOpen = true;
        if (this.chatContainer) this.chatContainer.classList.add('show');
        if (this.toggleBtn) {
            this.toggleBtn.classList.add('active');
            this.toggleBtn.innerHTML = '<i class="fas fa-times"></i>';
        }
        if (this.inputField) this.inputField.focus();
        this.hideBadge();
    }
    
    closeChat() {
        this.isOpen = false;
        if (this.chatContainer) this.chatContainer.classList.remove('show');
        if (this.toggleBtn) {
            this.toggleBtn.classList.remove('active');
            this.toggleBtn.innerHTML = '<i class="fas fa-robot"></i>';
        }
    }
    
    minimizeChat() {
        if (this.chatContainer && this.minimizeBtn) {
            this.chatContainer.classList.toggle('minimized');
            const icon = this.minimizeBtn.querySelector('i');
            if (icon) {
                if (this.chatContainer.classList.contains('minimized')) {
                    icon.className = 'fas fa-plus';
                } else {
                    icon.className = 'fas fa-minus';
                }
            }
        }
    }
    
    async sendMessage() {
        if (!this.inputField) return;
        
        const message = this.inputField.value.trim();
        
        if (!message) return;
        
        // Verificar rate limiting
        if (this.messagesPerMinute >= 20) {
            this.showErrorMessage('Has enviado muchos mensajes. Espera un momento.');
            return;
        }
        
        // Limpiar input y deshabilitar
        this.inputField.value = '';
        this.setInputState(false);
        
        // Mostrar mensaje del usuario
        this.addMessage(message, 'user');
        
        // Mostrar indicador de escritura
        this.showTyping(true);
        
        this.messagesPerMinute++;
        
        try {
            const response = await fetch('/ia/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({
                    message: message,
                    sessionId: this.sessionId
                })
            });
            
            const data = await response.json();
            
            // Ocultar indicador de escritura
            this.showTyping(false);
            
            if (data.success) {
                // Mostrar respuesta de IA con animación de escritura
                await this.addMessageWithTypingEffect(data.response, 'ai');
                this.sessionId = data.sessionId; // Actualizar session ID
            } else {
                this.showErrorMessage(data.error || 'Error procesando mensaje');
            }
            
        } catch (error) {
            console.error('Error enviando mensaje:', error);
            this.showTyping(false);
            this.showErrorMessage('Error de conexión. Intenta nuevamente.');
        } finally {
            this.setInputState(true);
            if (this.inputField) this.inputField.focus();
        }
    }
    
    sendQuickMessage(message) {
        if (this.inputField) {
            this.inputField.value = message;
            this.sendMessage();
        }
    }
    
    addMessage(text, sender, timestamp = null) {
        if (!this.messagesContainer) return null;

        const messageDiv = document.createElement('div');
        messageDiv.className = `ai-message ${sender}`;
        
        const avatar = document.createElement('div');
        avatar.className = 'ai-message-avatar';
        avatar.innerHTML = sender === 'user' ? 
            '<i class="fas fa-user"></i>' : 
            '<i class="fas fa-robot"></i>';
        
        const content = document.createElement('div');
        content.className = 'ai-message-content';
        content.innerHTML = this.formatMessage(text);
        
        const time = document.createElement('div');
        time.className = 'ai-message-time';
        time.textContent = timestamp || new Date().toLocaleTimeString('es-ES', {
            hour: '2-digit',
            minute: '2-digit'
        });
        
        messageDiv.appendChild(avatar);
        messageDiv.appendChild(content);
        content.appendChild(time);
        
        // Remover sugerencias si existen
        const suggestions = this.messagesContainer.querySelector('.ai-quick-suggestions');
        if (suggestions) suggestions.remove();
        
        const welcome = this.messagesContainer.querySelector('.ai-welcome-message');
        if (welcome) welcome.remove();
        
        this.messagesContainer.appendChild(messageDiv);
        this.scrollToBottom();
        
        return messageDiv;
    }
    
    async addMessageWithTypingEffect(text, sender) {
        const messageDiv = this.addMessage('', sender);
        const contentDiv = messageDiv.querySelector('.ai-message-content');
        const timeDiv = contentDiv.querySelector('.ai-message-time');
        
        // Simular efecto de escritura
        const words = text.split(' ');
        let currentText = '';
        
        for (let i = 0; i < words.length; i++) {
            currentText += (i > 0 ? ' ' : '') + words[i];
            contentDiv.innerHTML = this.formatMessage(currentText) + '<div class="ai-message-time">' + timeDiv.textContent + '</div>';
            this.scrollToBottom();
            await this.sleep(50); // Velocidad de escritura
        }
    }
    
    formatMessage(text) {
        // Formatear texto con markdown básico
        return text
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/`(.*?)`/g, '<code>$1</code>')
            .replace(/\n/g, '<br>');
    }
    
    showTyping(show) {
        if (this.typingIndicator) {
            this.typingIndicator.style.display = show ? 'flex' : 'none';
            if (show) {
                this.scrollToBottom();
            }
        }
    }
    
    showErrorMessage(message) {
        this.addMessage(`❗ ${message}`, 'ai');
    }
    
    setInputState(enabled) {
        if (this.inputField) {
            this.inputField.disabled = !enabled;
            if (enabled) {
                this.inputField.placeholder = 'Escribe tu consulta...';
            } else {
                this.inputField.placeholder = 'Procesando...';
            }
        }
        if (this.sendBtn) {
            this.sendBtn.disabled = !enabled;
        }
    }
    
    async clearChat() {
        if (!confirm('¿Estás seguro de que quieres limpiar el chat?')) {
            return;
        }
        
        try {
            if (this.sessionId) {
                await fetch(`/ia/chat/session/${this.sessionId}`, {
                    method: 'DELETE',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });
            }
            
            if (this.messagesContainer) {
                this.messagesContainer.innerHTML = `
                    <div class="ai-welcome-message">
                        <h4><i class="fas fa-brain"></i> Chat limpiado</h4>
                        <p>Puedes empezar una nueva conversación.</p>
                    </div>
                `;
            }
            
            // Inicializar nueva sesión
            await this.initializeSession();
            
        } catch (error) {
            console.error('Error limpiando chat:', error);
            this.showErrorMessage('Error limpiando el chat');
        }
    }
    
    scrollToBottom() {
        if (this.messagesContainer) {
            setTimeout(() => {
                this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
            }, 50);
        }
    }
    
    showBadge(count = 1) {
        const badge = document.getElementById('ai-chat-badge');
        if (badge) {
            badge.textContent = count;
            badge.style.display = 'flex';
        }
    }
    
    hideBadge() {
        const badge = document.getElementById('ai-chat-badge');
        if (badge) {
            badge.style.display = 'none';
        }
    }
    
    updateConnectionStatus(status) {
        const statusEl = document.getElementById('ai-connection-status');
        const indicator = this.statusIndicator;
        
        if (statusEl) {
            statusEl.className = 'ai-connection-status';
            
            switch (status) {
                case 'connected':
                    statusEl.style.display = 'none';
                    if (indicator) indicator.style.background = '#2ecc71';
                    break;
                case 'connecting':
                    statusEl.classList.add('connecting');
                    statusEl.textContent = 'Conectando...';
                    if (indicator) indicator.style.background = '#f39c12';
                    break;
                case 'error':
                    statusEl.classList.add('offline');
                    statusEl.textContent = 'Error de conexión';
                    if (indicator) indicator.style.background = '#e74c3c';
                    break;
            }
        }
    }
    
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    };
}

// Inicializar chat cuando el DOM esté listo
// Usamos una variable global para evitar reinicializaciones
if (typeof window.aiChatInitialized === 'undefined') {
    window.aiChatInitialized = false;
}

document.addEventListener('DOMContentLoaded', function() {
    // Evitar doble inicialización
    if (window.aiChatInitialized) return;
    
    setTimeout(() => {
        if (!window.aiChat) {
            window.aiChat = new window.AIChat();
            window.aiChatInitialized = true;
            console.log('🤖 Asistente IA inicializado');
        }
    }, 500);
});

// Exponer funciones globales si es necesario
window.initAIChat = function() {
    if (!window.aiChat) {
        window.aiChat = new window.AIChat();
        window.aiChatInitialized = true;
    }
};
