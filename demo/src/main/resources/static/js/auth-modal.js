// Funciones para controlar la visibilidad del modal
function openAuthModal() {
    document.getElementById('authModal').style.display = 'flex';
    // Limpiar mensajes anteriores al abrir el modal
    clearAuthMessages();
}

function closeAuthModal() {
    document.getElementById('authModal').style.display = 'none';
    // Limpiar parámetros de la URL al cerrar el modal
    cleanURLParameters();
    // Limpiar mensajes
    clearAuthMessages();
    // Resetear formulario de registro si está visible
    resetRegisterForm();
}

// Función para cambiar entre el formulario de login y registro
function toggleForms() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const container = document.querySelector('.forms-container');

    if (loginForm.style.display === 'none') {
        // Cambiar a login
        container.classList.remove('show-register');
        setTimeout(() => {
            loginForm.style.display = 'block';
            registerForm.style.display = 'none';
        }, 400);
    } else {
        // Cambiar a registro
        container.classList.add('show-register');
        setTimeout(() => {
            loginForm.style.display = 'none';
            registerForm.style.display = 'block';
            // Resetear el formulario de registro al cambiarlo
            resetRegisterForm();
        }, 400);
    }
}

// Función para mostrar específicamente el formulario de login
function showLoginForm() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const container = document.querySelector('.forms-container');
    
    container.classList.remove('show-register');
    loginForm.style.display = 'block';
    registerForm.style.display = 'none';
    resetRegisterForm();
}

// Función para mostrar específicamente el formulario de registro
function showRegisterForm() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const container = document.querySelector('.forms-container');
    
    container.classList.add('show-register');
    loginForm.style.display = 'none';
    registerForm.style.display = 'block';
}

// Funciones para la navegación por pasos del formulario de registro
let currentStep = 1;

function nextStep(step) {
    // Validar el paso actual antes de avanzar
    if (validateCurrentStep(currentStep)) {
        document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
        document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');
        
        document.querySelector(`.step[data-step="${currentStep}"]`).classList.remove('active');
        document.querySelector(`.step[data-step="${step}"]`).classList.add('active');
        
        currentStep = step;
    }
}

function prevStep(step) {
    document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
    document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');

    document.querySelector(`.step[data-step="${currentStep}"]`).classList.remove('active');
    document.querySelector(`.step[data-step="${step}"]`).classList.add('active');

    currentStep = step;
}

// Validación del paso 1 (Datos Personales)
function validateStep1AndNext() {
    const nombre = document.getElementById('reg-nombre').value.trim();
    const apellido = document.getElementById('reg-apellido').value.trim();
    const dni = document.getElementById('reg-dni').value.trim();
    
    if (!nombre) {
        showAuthMessage('Por favor ingresa tu nombre', 'error');
        return;
    }
    
    if (!apellido) {
        showAuthMessage('Por favor ingresa tu apellido', 'error');
        return;
    }
    
    if (!dni || !/^\d+$/.test(dni)) {
        showAuthMessage('Por favor ingresa un DNI válido (solo números)', 'error');
        return;
    }
    
    // Si todo está válido, avanzar al paso 2
    nextStep(2);
}

// Validación genérica del paso actual
function validateCurrentStep(step) {
    switch(step) {
        case 1:
            return validateStep1();
        case 2:
            return validateStep2();
        case 3:
            return validateStep3();
        default:
            return true;
    }
}

function validateStep1() {
    const nombre = document.getElementById('reg-nombre').value.trim();
    const apellido = document.getElementById('reg-apellido').value.trim();
    const dni = document.getElementById('reg-dni').value.trim();
    
    if (!nombre) {
        showAuthMessage('Por favor ingresa tu nombre', 'error');
        return false;
    }
    
    if (!apellido) {
        showAuthMessage('Por favor ingresa tu apellido', 'error');
        return false;
    }
    
    if (!dni || !/^\d+$/.test(dni)) {
        showAuthMessage('Por favor ingresa un DNI válido (solo números)', 'error');
        return false;
    }
    
    return true;
}

function validateStep2() {
    const pais = document.getElementById('reg-pais').value.trim();
    const provincia = document.getElementById('reg-provincia').value.trim();
    
    if (!pais) {
        showAuthMessage('Por favor ingresa tu país', 'error');
        return false;
    }
    
    if (!provincia) {
        showAuthMessage('Por favor ingresa tu provincia', 'error');
        return false;
    }
    
    return true;
}

function validateStep3() {
    const estudios = document.getElementById('reg-estudios').value.trim();
    const password = document.getElementById('reg-password').value;
    const terms = document.getElementById('reg-terms').checked;
    
    if (!estudios) {
        showAuthMessage('Por favor ingresa tus últimos estudios', 'error');
        return false;
    }
    
    if (!password || password.length < 6) {
        showAuthMessage('La contraseña debe tener al menos 6 caracteres', 'error');
        return false;
    }
    
    if (!terms) {
        showAuthMessage('Debes aceptar los términos y condiciones', 'error');
        return false;
    }
    
    return true;
}

// Función para limpiar parámetros de la URL
function cleanURLParameters() {
    const url = new URL(window.location);
    const params = new URLSearchParams(url.search);
    
    // Remover parámetros de autenticación
    if (params.has('error') || params.has('logout') || params.has('timeout')) {
        params.delete('error');
        params.delete('logout');
        params.delete('timeout');
        
        const newUrl = params.toString() ? `${url.pathname}?${params.toString()}` : url.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
}

// Función para resetear el formulario de registro
function resetRegisterForm() {
    // Resetear todos los campos del formulario
    document.getElementById('reg-nombre').value = '';
    document.getElementById('reg-apellido').value = '';
    document.getElementById('reg-dni').value = '';
    document.getElementById('reg-pais').value = '';
    document.getElementById('reg-provincia').value = '';
    document.getElementById('reg-estudios').value = '';
    document.getElementById('reg-password').value = '';
    document.getElementById('reg-terms').checked = false;
    
    // Volver al paso 1
    if (currentStep !== 1) {
        document.querySelectorAll('.form-step').forEach(step => {
            step.classList.remove('active');
        });
        document.querySelectorAll('.step').forEach(step => {
            step.classList.remove('active');
        });
        
        document.querySelector('.form-step[data-step="1"]').classList.add('active');
        document.querySelector('.step[data-step="1"]').classList.add('active');
        currentStep = 1;
    }
}

// Función para mostrar mensajes en el modal
function showAuthMessage(message, type) {
    clearAuthMessages();
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `alert alert-${type}`;
    messageDiv.innerHTML = `<p>${message}</p>`;
    
    const authRight = document.querySelector('.auth-modal-right');
    const formsContainer = document.querySelector('.forms-container');
    
    // Insertar el mensaje antes del forms container
    authRight.insertBefore(messageDiv, formsContainer);
    
    // Auto-eliminar el mensaje después de 5 segundos
    setTimeout(() => {
        if (messageDiv.parentNode) {
            messageDiv.parentNode.removeChild(messageDiv);
        }
    }, 5000);
}

// Función para limpiar mensajes
function clearAuthMessages() {
    const alerts = document.querySelectorAll('.auth-modal-right .alert');
    alerts.forEach(alert => {
        alert.parentNode.removeChild(alert);
    });
}

// Event listener para abrir el modal automáticamente cuando hay parámetros de error
document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    
    // Verificar si hay mensajes de Thymeleaf
    const hasError = document.querySelector('.alert-error') !== null;
    const hasTimeout = document.querySelector('.alert-warning') !== null;
    
    if (urlParams.has('error') || urlParams.has('timeout') || hasError || hasTimeout) {
        // Pequeño delay para asegurar que el DOM esté completamente cargado
        setTimeout(() => {
            openAuthModal();
            showLoginForm();
            
            // Si hay mensajes de Thymeleaf, limpiar la URL
            if (hasError || hasTimeout) {
                cleanURLParameters();
            }
        }, 100);
    }
    
    // Cerrar modal al hacer clic fuera del contenido
    document.getElementById('authModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeAuthModal();
        }
    });
    
    // Cerrar modal con la tecla ESC
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && document.getElementById('authModal').style.display === 'flex') {
            closeAuthModal();
        }
    });
});