// Chat Simple para pruebas rápidas
class SimpleChat {
    constructor() {
        this.init();
    }

    init() {
        this.createChatButton();
        this.createChatWindow();
        this.bindEvents();
        console.log('💬 Chat Simple iniciado');
    }

    createChatButton() {
        const button = document.createElement('button');
        button.id = 'chat-button-simple';
        button.innerHTML = '💬';
        button.style.cssText = `
            position: fixed;
            bottom: 20px;
            right: 20px;
            width: 60px;
            height: 60px;
            border-radius: 50%;
            background: #007bff;
            color: white;
            border: none;
            font-size: 24px;
            cursor: pointer;
            z-index: 1000;
            box-shadow: 0 2px 10px rgba(0,123,255,0.3);
            transition: all 0.3s ease;
        `;
        
        button.addEventListener('mouseenter', () => {
            button.style.transform = 'scale(1.1)';
        });
        
        button.addEventListener('mouseleave', () => {
            button.style.transform = 'scale(1)';
        });

        document.body.appendChild(button);
    }

    createChatWindow() {
        const chatWindow = document.createElement('div');
        chatWindow.id = 'chat-window-simple';
        chatWindow.style.cssText = `
            position: fixed;
            bottom: 100px;
            right: 20px;
            width: 350px;
            height: 500px;
            background: white;
            border-radius: 10px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
            display: none;
            flex-direction: column;
            z-index: 1001;
            overflow: hidden;
        `;

        chatWindow.innerHTML = `
            <div style="background: #007bff; color: white; padding: 15px; font-weight: bold;">
                Chat IA Aurea
                <button id="close-chat" style="float: right; background: none; border: none; color: white; font-size: 18px; cursor: pointer;">×</button>
            </div>
            <div id="chat-messages" style="flex: 1; padding: 15px; overflow-y: auto; max-height: 350px; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word;">
                <div style="color: #666; font-size: 14px; margin-bottom: 10px;">
                    ¡Hola! Soy tu asistente de IA. ¿En qué puedo ayudarte?
                </div>
            </div>
            <div style="padding: 15px; border-top: 1px solid #eee;">
                <div style="display: flex; gap: 10px;">
                    <input type="text" id="chat-input" placeholder="Escribe tu mensaje..." 
                           style="flex: 1; padding: 10px; border: 1px solid #ddd; border-radius: 5px; outline: none;">
                    <button id="send-message" style="padding: 10px 15px; background: #007bff; color: white; border: none; border-radius: 5px; cursor: pointer;">
                        Enviar
                    </button>
                </div>
                <div id="status" style="font-size: 12px; color: #666; margin-top: 5px; min-height: 16px;"></div>
            </div>
        `;

        document.body.appendChild(chatWindow);
    }

    bindEvents() {
        const button = document.getElementById('chat-button-simple');
        const chatWindow = document.getElementById('chat-window-simple');
        const closeButton = document.getElementById('close-chat');
        const sendButton = document.getElementById('send-message');
        const input = document.getElementById('chat-input');

        button.addEventListener('click', () => {
            if (chatWindow.style.display === 'none') {
                chatWindow.style.display = 'flex';
                button.style.display = 'none';
            }
        });

        closeButton.addEventListener('click', () => {
            chatWindow.style.display = 'none';
            button.style.display = 'block';
        });

        sendButton.addEventListener('click', () => {
            this.sendMessage();
        });

        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });
    }

    async sendMessage() {
        const input = document.getElementById('chat-input');
        const message = input.value.trim();
        
        if (!message) return;

        this.addMessage(message, 'user');
        input.value = '';
        this.setStatus('Enviando mensaje...');

        try {
            // Obtener información del usuario
            const userInfo = this.getUserInfo();
            
            const response = await fetch('/ia/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    message: message,
                    userDni: userInfo.dni,
                    sessionId: userInfo.sessionId
                })
            });

            console.log('Response status:', response.status);
            
            if (response.ok) {
                const data = await response.json();
                console.log('Response data:', data);
                
                if (data.aiResponse) {
                    this.addMessage(data.aiResponse, 'assistant');
                    this.setStatus('Mensaje enviado');
                } else {
                    this.addMessage('Error: No se recibió respuesta de la IA', 'error');
                    this.setStatus('Error en la respuesta');
                }
            } else {
                const errorText = await response.text();
                console.error('Error response:', errorText);
                this.addMessage(`Error ${response.status}: ${errorText}`, 'error');
                this.setStatus('Error en el servidor');
            }
        } catch (error) {
            console.error('Chat error:', error);
            this.addMessage('Error de conexión: ' + error.message, 'error');
            this.setStatus('Error de conexión');
        }
    }

    addMessage(message, type) {
        const messagesDiv = document.getElementById('chat-messages');
        const messageDiv = document.createElement('div');
        
        const isUser = type === 'user';
        const isError = type === 'error';
        
        messageDiv.style.cssText = `
            margin-bottom: 15px;
            padding: 10px;
            border-radius: 10px;
            max-width: 80%;
            word-wrap: break-word;
            ${isUser ? 
                'background: #007bff; color: white; margin-left: auto; text-align: right;' : 
                isError ?
                'background: #ff4757; color: white;' :
                'background: #f1f3f4; color: #333;'
            }
        `;
        
        messageDiv.innerHTML = message.replace(/\n/g, '<br>');
        messagesDiv.appendChild(messageDiv);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    setStatus(text) {
        const statusDiv = document.getElementById('status');
        statusDiv.textContent = text;
        
        if (text.includes('Error')) {
            statusDiv.style.color = '#ff4757';
        } else {
            statusDiv.style.color = '#666';
        }
        
        // Limpiar status después de 3 segundos
        setTimeout(() => {
            if (statusDiv.textContent === text) {
                statusDiv.textContent = '';
            }
        }, 3000);
    }

    getUserInfo() {
        // Intentar obtener información del usuario desde la sesión
        const scripts = document.getElementsByTagName('script');
        let userDni = 'guest';
        
        for (let script of scripts) {
            if (script.innerHTML.includes('usuario')) {
                const match = script.innerHTML.match(/usuario[^"]*"([^"]+)"/);
                if (match) {
                    userDni = match[1];
                    break;
                }
            }
        }
        
        // Generar sessionId único
        let sessionId = localStorage.getItem('chat-session-id');
        if (!sessionId) {
            sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
            localStorage.setItem('chat-session-id', sessionId);
        }
        
        return { dni: userDni, sessionId: sessionId };
    }
}

// Inicializar chat cuando la página esté lista
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => new SimpleChat());
} else {
    new SimpleChat();
}