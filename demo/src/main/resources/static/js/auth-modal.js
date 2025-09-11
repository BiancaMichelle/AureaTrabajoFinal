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
  if (!validateStep(currentStep)) {
    return; // no avanza hasta que se complete este paso
  }

  // Desactivar required en paso actual
  toggleRequired(currentStep, false);
  // Activar required en paso siguiente
  toggleRequired(step, true);

  // ocultar paso actual
  document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
  document.querySelector(`.step-navigation .step[data-step="${currentStep}"]`).classList.remove('active');
  document.querySelector(`.action-group[data-step="${currentStep}"]`).classList.remove('active');

  // mostrar siguiente paso
  document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');
  document.querySelector(`.step-navigation .step[data-step="${step}"]`).classList.add('active');
  document.querySelector(`.action-group[data-step="${step}"]`).classList.add('active');

  currentStep = step;
}
function validateStep(step) {
  const inputs = document.querySelectorAll(`.form-step[data-step="${step}"] [required]`);
  for (let input of inputs) {
    if (!input.checkValidity()) {
      input.reportValidity(); // muestra el mensaje de error nativo
      return false;
    }
  }
  return true;
}

function toggleRequired(step, enable) {
  const inputs = document.querySelectorAll(`.form-step[data-step="${step}"] [required]`);
  inputs.forEach(input => {
    if (enable) {
      input.setAttribute("data-required", "true");
      input.setAttribute("required", "true");
    } else {
      input.removeAttribute("required");
    }
  });
}



function prevStep(step) {
  // Desactivar required en paso actual
  toggleRequired(currentStep, false);
  // Activar required en paso anterior
  toggleRequired(step, true);

  document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
  document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');

  document.querySelector(`.step[data-step="${currentStep}"]`).classList.remove('active');
  document.querySelector(`.step[data-step="${step}"]`).classList.add('active');

  document.querySelector(`.action-group[data-step="${currentStep}"]`).classList.remove('active');
  document.querySelector(`.action-group[data-step="${step}"]`).classList.add('active');

  currentStep = step;
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

window.showAuthMessage = function (message, type) {
    const container = document.querySelector('.auth-modal-right');
    if (!container) return;

    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.textContent = message;

    container.prepend(alert);

    // opcional: auto-ocultar
    setTimeout(() => {
        alert.remove();
    }, 5000);
};
