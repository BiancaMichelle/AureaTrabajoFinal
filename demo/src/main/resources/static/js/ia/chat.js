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

            // Deduplicación de notificaciones: almacenamos IDs ya procesados en localStorage
            this.processedIdsKey = 'ia_processed_notifications';
            this.processedIds = new Set();
            this.isChecking = false;
            try {
                const raw = localStorage.getItem(this.processedIdsKey);
                if (raw) {
                    JSON.parse(raw).forEach(id => this.processedIds.add(id));
                }
            } catch (e) {
                console.warn('No se pudo recuperar processedIds:', e);
            }
            
            this.initializeElements();
            // Solo vincular eventos si se encontraron los elementos
            if (this.toggleBtn) {
                this.bindEvents();
                this.initializeSession();
            }
            // Exponer la instancia inmediatamente para reducir carreras de inicialización
            try {
                window.aiChat = this;
                window.aiChatInitialized = true;
                // Exponer utilidad de diagnóstico accesible desde consola
                window.aiChat.dumpVisibilityDiagnostics = () => {
                    try { this.checkVisibilityDiagnostics(); } catch (e) { console.warn('AIChat: dumpVisibilityDiagnostics error', e); }
                };
                console.log('🤖 AIChat: instancia expuesta en window.aiChat y aiChatInitialized=true');
            } catch (e) {
                console.warn('AIChat: no se pudo exponer la instancia globalmente', e);
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
        
        console.log('AIChat: createChatStructure invoked');
        if (!document.body) {
            console.log('AIChat: document.body no existe aún, esperando DOMContentLoaded para insertar estructura del chat...');
            document.addEventListener('DOMContentLoaded', () => {
                document.body.insertAdjacentHTML('beforeend', chatHTML);
                console.log('AIChat: estructura de chat insertada después de DOMContentLoaded');
            }, { once: true });
        } else {
            document.body.insertAdjacentHTML('beforeend', chatHTML);
            console.log('AIChat: estructura de chat insertada en document.body');
        }
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
                // Iniciar polling de notificaciones una vez conectados
                if (typeof this.startNotificationPolling === 'function') {
                    this.startNotificationPolling();
                }
            }
        } catch (error) {
            console.error('Error inicializando sesión:', error);
            this.updateConnectionStatus('error');
        }
    }

    // Inicia el polling para buscar notificaciones tipo CHAT_IA (por ejemplo, análisis de rendimiento)
    startNotificationPolling() {
        // Evitar múltiples timers
        if (this._notificationPollTimer) return;
        // Consultar cada 20 segundos
        this._notificationPollTimer = setInterval(() => this.checkNotifications(), 20000);
        // Hacer una consulta inmediata al iniciar
        setTimeout(() => this.checkNotifications(), 3000);
    }

    async checkNotifications() {
        // Evitar llamadas concurrentes
        if (this.isChecking) return;
        this.isChecking = true;
        try {
            const resp = await fetch('/ia/chat/notifications');
            if (!resp.ok) return;
            const data = await resp.json();
            if (!Array.isArray(data) || data.length === 0) return;

            // Filtrar notificaciones ya procesadas usando IDs persistentes
            const newNotifs = data.filter(n => n && n.id && !this.processedIds.has(n.id));
            if (newNotifs.length === 0) return;

            // Mostrar badge si chat cerrado (count con sólo nuevos)
            if (!this.isOpen) {
                this.showBadge(newNotifs.length);
            }

            for (const n of newNotifs) {
                const msg = n.message || '';

                // Marcar como procesada localmente antes de mostrar para reducir races con otros clientes
                try {
                    this.processedIds.add(n.id);
                    localStorage.setItem(this.processedIdsKey, JSON.stringify(Array.from(this.processedIds)));
                } catch (e) {
                    console.warn('No se pudo persistir processedIds:', e);
                }

                // Si el chat está abierto mostramos inmediatamente con efecto de escritura
                if (this.isOpen && typeof this.addMessageWithTypingEffect === 'function') {
                    await this.addMessageWithTypingEffect(msg, 'ai');
                } else if (this.isOpen) {
                    this.addMessage(msg, 'ai');
                } else {
                    // Guardar en cola para mostrar al abrir chat
                    this.messageQueue.push(msg);
                }

                // Marcar como leída en el servidor (no bloqueante para UI)
                fetch(`/ia/chat/notifications/read/${n.id}`, { method: 'POST' })
                    .catch(er => console.warn('No se pudo marcar notificación leída:', er));
            }
        } catch (e) {
            console.error('Error fetching notifications:', e);
        } finally {
            this.isChecking = false;
        }
    }
    
    toggleChat() {
        console.log(`AIChat: toggleChat called. isOpen=${this.isOpen}`);
        if (this.isOpen) {
            console.log('AIChat: closing chat via toggleChat');
            this.closeChat();
        } else {
            console.log('AIChat: opening chat via toggleChat');
            this.openChat();
        }
    }
    
    async openChat() {
        console.log('AIChat: openChat() called');
         this.isOpen = true;
        if (this.chatContainer) {
            this.chatContainer.classList.add('show');
            // Añadir clase 'open' también para compatibilidad con estilos alternativos
            this.chatContainer.classList.add('open');
            console.log('AIChat: chatContainer classes show + open added');
        }
        if (this.toggleBtn) {
            this.toggleBtn.classList.add('active');
            this.toggleBtn.innerHTML = '<i class="fas fa-times"></i>';
        }
        if (this.inputField) this.inputField.focus();
        this.hideBadge();

        // Volcar mensajes pendientes (si los hubo durante el polling)
        if (this.messageQueue && this.messageQueue.length) {
            while (this.messageQueue.length) {
                const msg = this.messageQueue.shift();
                if (typeof this.addMessageWithTypingEffect === 'function') {
                    await this.addMessageWithTypingEffect(msg, 'ai');
                } else {
                    this.addMessage(msg, 'ai');
                }
            }
        }

        // Esperar a que la animación CSS termine (para que el usuario vea la ventana abriéndose antes de cualquier acción automática)
        await this.sleep(300);
        console.log('AIChat: openChat completed (post-animation)');

        // Ejecutar diagnóstico de visibilidad para detectar overlays/estilos que oculten el chat
        try {
            this.checkVisibilityDiagnostics();
        } catch (e) {
            console.warn('AIChat: checkVisibilityDiagnostics fallo', e);
        }
    }
    
    closeChat() {
        this.isOpen = false;
         if (this.chatContainer) {
             this.chatContainer.classList.remove('show');
             this.chatContainer.classList.remove('open');
             console.log('AIChat: chatContainer classes show + open removed');
         }
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

    async triggerAnalysis() {
        this.addMessage("📊 Analizar mi Rendimiento Completo", 'user');
        this.addMessage("Procesando tu solicitud... Esto puede tomar unos segundos. ⏳", 'ai');
        
        try {
            const response = await fetch('/alumno/ia/analisis-personal');
            if (response.ok) {
                const html = await response.text();
                // Send directly as HTML message, bypassing formatMessage in modified addMessage
                this.addMessage(html, 'ai', null, true);
            } else {
                this.addMessage("❌ Error al obtener el análisis.", 'ai');
            }
        } catch (e) {
            console.error(e);
            this.addMessage("❌ Error de comunicación.", 'ai');
        }
    }
    
    addMessage(text, sender, timestamp = null, isHtml = false) {
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
        // If isHtml is true, use text directly, otherwise format it
        content.innerHTML = isHtml ? text : this.formatMessage(text);
        
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
    
    clearChat() {
        ModalConfirmacion.show(
            'Limpiar Chat',
            '¿Estás seguro de que quieres limpiar el chat?',
            async () => {
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
        );
    }
    
    solicitarTutoria(ofertaId) {
        if (confirm("¿Confirmas que deseas enviar un correo solicitando tutoría al docente de este curso?")) {
            this.addMessage("Solicitando tutoría...", "user");
            this.showTyping();
            
            fetch('/alumno/ia/solicitar-tutoria?ofertaId=' + ofertaId, { method: 'POST' })
                .then(response => {
                    if (response.ok) return response.text();
                    throw new Error('Error en solicitud');
                })
                .then(msg => {
                    this.showTyping(false);
                    this.addMessage("✅ " + msg, "ai");
                })
                .catch(err => {
                    this.showTyping(false);
                    this.showErrorMessage("No se pudo enviar la solicitud. Intenta más tarde.");
                });
        }
    }
    
    scrollToBottom() {
        if (this.messagesContainer) {
            setTimeout(() => {
                this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
            }, 50);
        }
    }

    // Diagnóstico de visibilidad para detectar overlays, estilos inline o posicionamiento que oculte el chat
    checkVisibilityDiagnostics() {
        if (!this.chatContainer) {
            console.warn('AIChat: checkVisibilityDiagnostics: no hay chatContainer para diagnosticar');
            return;
        }

        const rect = this.chatContainer.getBoundingClientRect();
        const cs = window.getComputedStyle(this.chatContainer);

        console.log('AIChat: visibility diagnostics', {
            display: cs.display,
            visibility: cs.visibility,
            opacity: cs.opacity,
            zIndex: cs.zIndex,
            position: cs.position,
            left: cs.left,
            top: cs.top,
            right: cs.right,
            bottom: cs.bottom,
            rect: rect,
            offsetWidth: this.chatContainer.offsetWidth,
            offsetHeight: this.chatContainer.offsetHeight
        });

        // Revisar el elemento que ocupa el centro del contenedor
        const cx = rect.left + (rect.width / 2);
        const cy = rect.top + (rect.height / 2);
        let elAtCenter = null;
        try {
            const px = Math.max(0, Math.min(window.innerWidth - 1, cx));
            const py = Math.max(0, Math.min(window.innerHeight - 1, cy));
            elAtCenter = document.elementFromPoint(px, py);
        } catch (e) {
            console.warn('AIChat: error en elementFromPoint', e);
        }

        console.log('AIChat: element at center:', elAtCenter, 'containedByChat?', this.chatContainer.contains(elAtCenter));

        const hiddenOrOverlapped = (this.chatContainer.offsetWidth === 0 || this.chatContainer.offsetHeight === 0 || cs.display === 'none' || cs.visibility === 'hidden' || !this.chatContainer.contains(elAtCenter));

        if (hiddenOrOverlapped) {
            console.warn('AIChat: chat parece oculto o solapado. Aplicando estilos temporales de depuración para forzarlo visible.');

            // Estilos temporales de prueba para forzar visibilidad (no invasivos para pruebas manuales)
            this.chatContainer.style.zIndex = '99999';
            this.chatContainer.style.display = 'block';
            this.chatContainer.style.position = 'fixed';
            this.chatContainer.style.right = '20px';
            this.chatContainer.style.bottom = '20px';

            const cs2 = window.getComputedStyle(this.chatContainer);
            const rect2 = this.chatContainer.getBoundingClientRect();
            const cx2 = rect2.left + (rect2.width / 2);
            const cy2 = rect2.top + (rect2.height / 2);
            let elAtCenter2 = null;
            try {
                const px2 = Math.max(0, Math.min(window.innerWidth - 1, cx2));
                const py2 = Math.max(0, Math.min(window.innerHeight - 1, cy2));
                elAtCenter2 = document.elementFromPoint(px2, py2);
            } catch (e) {
                console.warn('AIChat: error en elementFromPoint post-force', e);
            }

            console.log('AIChat: post-force diagnostics', {
                display: cs2.display,
                visibility: cs2.visibility,
                opacity: cs2.opacity,
                zIndex: cs2.zIndex,
                rect: rect2,
                elAtCenter: elAtCenter2
            });
        } else {
            console.log('AIChat: chat visible según indicadores (no fuerza estilos)');
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
