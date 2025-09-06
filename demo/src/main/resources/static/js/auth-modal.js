// Funciones para la navegación por pasos del formulario de registro
// Funciones globales para el modal
window.openAuthModal = function() {
    document.getElementById('authModal').style.display = 'flex';
    clearAuthMessages();
};

window.closeAuthModal = function() {
    document.getElementById('authModal').style.display = 'none';
    cleanURLParameters();
    clearAuthMessages();
    resetRegisterForm();
};

window.toggleForms = function() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const container = document.querySelector('.forms-container');
    const toRegister = document.getElementById('to-register');
    const toLogin = document.getElementById('to-login');

    if (loginForm.style.display === 'none') {
        // Cambiar a login
        container.classList.remove('show-register');
        setTimeout(() => {
            loginForm.style.display = 'block';
            registerForm.style.display = 'none';
            if (toRegister) toRegister.style.display = 'block';
            if (toLogin) toLogin.style.display = 'none';
        }, 400);
    } else {
        // Cambiar a registro
        container.classList.add('show-register');
        setTimeout(() => {
            loginForm.style.display = 'none';
            registerForm.style.display = 'block';
            resetRegisterForm();
            if (toRegister) toRegister.style.display = 'none';
            if (toLogin) toLogin.style.display = 'block';
        }, 400);
    }
};

window.showLoginForm = function() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const container = document.querySelector('.forms-container');
    const toRegister = document.getElementById('to-register');
    const toLogin = document.getElementById('to-login');
    
    container.classList.remove('show-register');
    loginForm.style.display = 'block';
    registerForm.style.display = 'none';
    resetRegisterForm();
    if (toRegister) toRegister.style.display = 'block';
    if (toLogin) toLogin.style.display = 'none';
};

window.showRegisterForm = function() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const container = document.querySelector('.forms-container');
    const toRegister = document.getElementById('to-register');
    const toLogin = document.getElementById('to-login');
    
    container.classList.add('show-register');
    loginForm.style.display = 'none';
    registerForm.style.display = 'block';
    if (toRegister) toRegister.style.display = 'none';
    if (toLogin) toLogin.style.display = 'block';
};

// Funciones para la navegación por pasos (también deben ser globales)
window.nextStep = function(step) {
    // Validar el paso actual antes de avanzar
    if (validateCurrentStep(currentStep)) {
        document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
        document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');
        
        document.querySelector(`.step[data-step="${currentStep}"]`).classList.remove('active');
        document.querySelector(`.step[data-step="${step}"]`).classList.add('active');
        
        currentStep = step;
    }
};

window.prevStep = function(step) {
    document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
    document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');

    document.querySelector(`.step[data-step="${currentStep}"]`).classList.remove('active');
    document.querySelector(`.step[data-step="${step}"]`).classList.add('active');

    currentStep = step;
};

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
    
    clearFieldErrors(['nombre','apellido','dni']);

    let valid = true;
    if (!nombre) { setFieldError('nombre','Ingresa tu nombre'); valid = false; }
    if (!apellido) { setFieldError('apellido','Ingresa tu apellido'); valid = false; }
    if (!dni || !/^\d+$/.test(dni)) { setFieldError('dni','ingrese un DNI válido'); valid = false; }
    return valid;
}

function validateStep2() {
    const pais = document.getElementById('reg-pais').value;
    const provincia = document.getElementById('reg-provincia').value;
    const ciudad = document.getElementById('reg-ciudad').value;
    const domicilio = document.getElementById('reg-domicilio').value.trim();
    
    clearFieldErrors(['pais','provincia','ciudad','domicilio']);
    
    let valid = true;
    if (!pais) { setFieldError('pais','Selecciona tu país'); valid = false; }
    if (!provincia) { setFieldError('provincia','Selecciona tu provincia'); valid = false; }
    if (!ciudad) { setFieldError('ciudad','Selecciona tu ciudad'); valid = false; }
    if (!domicilio) { setFieldError('domicilio','Ingresa tu domicilio'); valid = false; }
    
    return valid;
}

function validateStep3() {
    const estudios = document.getElementById('reg-estudios').value.trim();
    const password = document.getElementById('reg-password').value;
    const password2 = document.getElementById('reg-password2').value;
    const terms = document.getElementById('reg-terms').checked;
    
    clearFieldErrors(['estudios','password','password2']);
    
    let valid = true;
    if (!estudios) { setFieldError('estudios','Ingresa tus estudios'); valid = false; }
    if (!password || password.length < 8) { setFieldError('password','Mínimo 8 caracteres'); valid = false; }
    if (password !== password2) { setFieldError('password2','Las contraseñas no coinciden'); valid = false; }
    if (!terms) { showAuthMessage('Debes aceptar los términos y condiciones', 'error'); valid = false; }
    
    return valid;
}

// ---- Manejo de errores por campo ----
function setFieldError(field, message) {
    const span = document.getElementById('error-' + field);
    const input = document.getElementById('reg-' + field);
    if (span) { span.textContent = message; span.style.display = 'block'; }
    if (input) { input.classList.add('input-error'); }
}

function clearFieldErrors(fields) {
    fields.forEach(f => {
        const span = document.getElementById('error-' + f);
        const input = document.getElementById('reg-' + f);
        if (span) { span.textContent=''; span.style.display='none'; }
        if (input) { input.classList.remove('input-error'); }
    });
}

// Cargar provincias y ciudades (si necesitas carga dinámica)
async function loadProvinces(paisCodigo) {
    try {
        const response = await fetch(`/register/provincias/${paisCodigo}`);
        const provincias = await response.json();
        
        const provinciaSelect = document.getElementById('reg-provincia');
        provinciaSelect.innerHTML = '<option value="">Seleccionar provincia</option>';
        
        provincias.forEach(provincia => {
            const option = document.createElement('option');
            option.value = provincia.codigo;
            option.textContent = provincia.nombre;
            provinciaSelect.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading provinces:', error);
    }
}

async function loadCities(paisCodigo, provinciaCodigo) {
    try {
        const response = await fetch(`/register/ciudades/${paisCodigo}/${provinciaCodigo}`);
        const ciudades = await response.json();
        
        const ciudadSelect = document.getElementById('reg-ciudad');
        ciudadSelect.innerHTML = '<option value="">Seleccionar ciudad</option>';
        
        ciudades.forEach(ciudad => {
            const option = document.createElement('option');
            option.value = ciudad.id;
            option.textContent = ciudad.nombre;
            ciudadSelect.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading cities:', error);
    }
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {

    const urlParams = new URLSearchParams(window.location.search);
    
    
    // Verificar si hay mensajes de registro
    if (urlParams.has('registroExitoso')) {
        showAuthMessage('Registro exitoso. Ya puedes iniciar sesión.', 'success');
        // Limpiar parámetro de la URL
        cleanURLParameters();
    }
    
    if (urlParams.has('registroError')) {
        showAuthMessage('Error en el registro. Intenta nuevamente.', 'error');
        // Abrir modal de registro automáticamente
        openAuthModal();
        showRegisterForm();
        // Limpiar parámetro de la URL
        cleanURLParameters();
    }

    function cleanURLParameters() {
        const url = new URL(window.location);
        const params = new URLSearchParams(url.search);
        
        // Remover parámetros de autenticación y registro
        ['error', 'logout', 'timeout', 'registroError', 'registroExitoso'].forEach(param => {
            if (params.has(param)) {
                params.delete(param);
            }
        });
        
        const newUrl = params.toString() ? `${url.pathname}?${params.toString()}` : url.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }

    // Event listeners para selects de ubicación
    document.getElementById('reg-pais')?.addEventListener('change', function() {
        if (this.value) {
            loadProvinces(this.value);
        }
    });
    
    document.getElementById('reg-provincia')?.addEventListener('change', function() {
        const paisCodigo = document.getElementById('reg-pais').value;
        if (paisCodigo && this.value) {
            loadCities(paisCodigo, this.value);
        }
    });
    
    // Cerrar modal al hacer clic fuera
    document.getElementById('authModal')?.addEventListener('click', function(e) {
        if (e.target === this) {
            closeAuthModal();
        }
    });
    
    // Cerrar modal con ESC
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeAuthModal();
        }
    });
});