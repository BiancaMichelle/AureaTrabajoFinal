// Funciones para la navegación por pasos del formulario de registro
// Funciones globales para el modal


window.clearAuthMessages = function() {
    const alerts = document.querySelectorAll('.auth-modal-right .alert');
    alerts.forEach(alert => {
        alert.parentNode.removeChild(alert);
    });
};

window.resetRegisterForm = function() {
    // Solo resetea al paso 1, no borres los valores
    if (currentStep !== 1) {
        document.querySelectorAll('.form-step').forEach(step => step.classList.remove('active'));
        document.querySelectorAll('.step').forEach(step => step.classList.remove('active'));
        
        document.querySelector('.form-step[data-step="1"]').classList.add('active');
        document.querySelector('.step[data-step="1"]').classList.add('active');
        currentStep = 1;
    }
};

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
let currentStep = 1;

// Funciones para la navegación por pasos (también deben ser globales)
function nextStep(step) {
    console.log("nextStep() llamado, paso actual:", currentStep, "siguiente paso:", step);
    
    // Validar el paso actual antes de avanzar
    if (validateCurrentStep(currentStep)) {
        console.log("Validación exitosa, avanzando...");
        
        // Ocultar paso actual
        document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
        document.querySelector(`.step-navigation .step[data-step="${currentStep}"]`).classList.remove('active');
        
        // Mostrar siguiente paso
        document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');
        document.querySelector(`.step-navigation .step[data-step="${step}"]`).classList.add('active');
        
        currentStep = step;
        console.log("Paso actual cambiado a:", currentStep);
    } else {
        console.log("Validación fallida, no se avanza");
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
    console.log("validateStep2() llamado");
    const pais = document.getElementById('reg-pais').value;
    const provincia = document.getElementById('reg-provincia').value;
    const ciudad = document.getElementById('reg-ciudad').value;
    const domicilio = document.getElementById('reg-domicilio').value.trim();
    
    console.log("Valores:", {pais, provincia, ciudad, domicilio});
    
    clearFieldErrors(['pais', 'provincia', 'ciudad', 'domicilio']);
    
    let valid = true;
    if (!pais) { 
        console.log("Error: País no seleccionado");
        setFieldError('pais', 'Selecciona tu país'); 
        valid = false; 
    }
    if (!provincia) { 
        console.log("Error: Provincia no seleccionada");
        setFieldError('provincia', 'Selecciona tu provincia'); 
        valid = false; 
    }
    if (!ciudad) { 
        console.log("Error: Ciudad no seleccionada");
        setFieldError('ciudad', 'Selecciona tu ciudad'); 
        valid = false; 
    }
    if (!domicilio) { 
        console.log("Error: Domicilio vacío");
        setFieldError('domicilio', 'Ingresa tu domicilio'); 
        valid = false; 
    }
    
    console.log("Validación resultado:", valid);
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

function avanzarPaso1() {
    if (validateStep1()) {
        nextStep(2);
    }
}

function avanzarPaso2() {
    if (validateStep2()) {
        // Ocultar paso 2
        const formStep2 = document.querySelector('.form-step[data-step="2"]');
        const navStep2 = document.querySelector('.step-navigation .step[data-step="2"]');
        
        // Mostrar paso 3  
        const formStep3 = document.querySelector('.form-step[data-step="3"]');
        const navStep3 = document.querySelector('.step-navigation .step[data-step="3"]');
        
        if (formStep2 && formStep3 && navStep2 && navStep3) {
            formStep2.classList.remove('active');
            navStep2.classList.remove('active');
            
            formStep3.classList.add('active');
            navStep3.classList.add('active');
            
            currentStep = 3;
            console.log("Avanzado al paso 3");
            
            // FORZAR REFLOW - Esto asegura que los estilos se apliquen
            void formStep3.offsetWidth;
        }
    }
}


// ---- Manejo de errores por campo ----
function setFieldError(field, message) {
    const span = document.getElementById('error-' + field);
    const input = document.getElementById('reg-' + field);
    
    console.log("setFieldError para:", field, "span existe:", !!span, "input existe:", !!input);
    
    if (span) { 
        span.textContent = message; 
        span.style.display = 'block'; 
    }
    if (input) { 
        input.classList.add('input-error'); 
    }
}
function clearFieldErrors(fields) {
    fields.forEach(f => {
        const span = document.getElementById('error-' + f);
        const input = document.getElementById('reg-' + f);
        if (span) { span.textContent=''; span.style.display='none'; }
        if (input) { input.classList.remove('input-error'); }
    });
}

async function probarEndpoints() {
    try {
        // Probar endpoint de países
        const responsePaises = await fetch('/register/paises');
        const paisesText = await responsePaises.text();
        console.log("Respuesta de /register/paises:", paisesText.substring(0, 200));
        
        // Probar endpoint de provincias (usa un código de país conocido)
        const responseProvincias = await fetch('/register/provincias/AR');
        const provinciasText = await responseProvincias.text();
        console.log("Respuesta de /register/provincias/AR:", provinciasText.substring(0, 200));
        
    } catch (error) {
        console.error("Error probando endpoints:", error);
    }
}

// Cargar provincias y ciudades (si necesitas carga dinámica)
async function cargarProvincias() {
    const paisSelect = document.getElementById('reg-pais');
    const provinciaSelect = document.getElementById('reg-provincia');
    const ciudadSelect = document.getElementById('reg-ciudad');
    
    const paisCodigo = paisSelect.value;
    
    if (!paisCodigo) {
        provinciaSelect.innerHTML = '<option value="">Selecciona país primero</option>';
        ciudadSelect.innerHTML = '<option value="">Selecciona provincia primero</option>';
        return;
    }
    
    provinciaSelect.disabled = true;
    provinciaSelect.innerHTML = '<option value="">Cargando...</option>';
    ciudadSelect.disabled = true;
    ciudadSelect.innerHTML = '<option value="">Selecciona provincia primero</option>';
    
    try {
        const response = await fetch(`/api/ubicaciones/provincias/${paisCodigo}`);
        
        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }
        
        const provincias = await response.json();
        console.log("Provincias recibidas:", provincias);
        
        provinciaSelect.innerHTML = '<option value="">Seleccionar provincia</option>';
        
        if (Array.isArray(provincias) && provincias.length > 0) {
            provincias.forEach(provincia => {
                const option = document.createElement('option');
                // USA LAS PROPIEDADES CORRECTAS: iso2 y name
                option.value = provincia.iso2;  // ← Cambiado
                option.textContent = provincia.name;  // ← Cambiado
                provinciaSelect.appendChild(option);
            });
        } else {
            provinciaSelect.innerHTML = '<option value="">No hay provincias</option>';
        }
        
        provinciaSelect.disabled = false;
    } catch (error) {
        console.error('Error cargando provincias:', error);
        provinciaSelect.innerHTML = '<option value="">Error al cargar</option>';
    }
}

// Función corregida para cargar ciudades
async function cargarCiudades() {
    const paisSelect = document.getElementById('reg-pais');
    const provinciaSelect = document.getElementById('reg-provincia');
    const ciudadSelect = document.getElementById('reg-ciudad');
    
    const paisCodigo = paisSelect.value;
    const provinciaCodigo = provinciaSelect.value;
    
    if (!paisCodigo || !provinciaCodigo) {
        ciudadSelect.innerHTML = '<option value="">Selecciona provincia primero</option>';
        return;
    }
    
    ciudadSelect.disabled = true;
    ciudadSelect.innerHTML = '<option value="">Cargando...</option>';
    
    try {
        const response = await fetch(`/api/ubicaciones/ciudades/${paisCodigo}/${provinciaCodigo}`);
        
        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }
        
        const ciudades = await response.json();
        console.log("Ciudades recibidas:", ciudades);
        
        ciudadSelect.innerHTML = '<option value="">Seleccionar ciudad</option>';
        
        if (Array.isArray(ciudades) && ciudades.length > 0) {
            ciudades.forEach(ciudad => {
                const option = document.createElement('option');
                // USA LAS PROPIEDADES CORRECTAS: id y name
                option.value = ciudad.id;  // ← Cambiado
                option.textContent = ciudad.name;  // ← Cambiado
                ciudadSelect.appendChild(option);
            });
        } else {
            ciudadSelect.innerHTML = '<option value="">No hay ciudades</option>';
        }
        
        ciudadSelect.disabled = false;
    } catch (error) {
        console.error('Error cargando ciudades:', error);
        ciudadSelect.innerHTML = '<option value="">Error al cargar</option>';
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

    // Event listeners para selects de ubicación - CAMBIO AQUÍ
    document.getElementById('reg-pais')?.addEventListener('change', function() {
        if (this.value) {
            cargarProvincias(); // Cambiado de loadProvinces() a cargarProvincias()
        }
    });
    
    document.getElementById('reg-provincia')?.addEventListener('change', function() {
        const paisCodigo = document.getElementById('reg-pais').value;
        if (paisCodigo && this.value) {
            cargarCiudades(); // Cambiado de loadCities() a cargarCiudades()
        }
    });
    
    // Cargar provincias/ciudades si ya hay valores seleccionados (útil después de errores)
    const paisSelect = document.getElementById('reg-pais');
    const provinciaSelect = document.getElementById('reg-provincia');
    
    if (paisSelect && paisSelect.value) {
        // Pequeño delay para asegurar que el DOM esté listo
        setTimeout(() => cargarProvincias(), 100);
    }
    
    if (provinciaSelect && provinciaSelect.value && paisSelect && paisSelect.value) {
        setTimeout(() => cargarCiudades(), 150);
    }
    
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