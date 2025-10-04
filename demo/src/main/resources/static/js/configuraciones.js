// =================   CONFIGURACIONES INSTITUCIONALES JS   =================

document.addEventListener('DOMContentLoaded', function() {
    initializeConfiguraciones();
});

function initializeConfiguraciones() {
    setupColorInputs();
    setupFileUpload();
    setupAutomaticConfigToggle();
    setupFormValidation();
    setupFormSubmission();
    setupColorPreview();
    loadSavedConfiguration();
}

// =================   COLORES INSTITUCIONALES   =================
function setupColorInputs() {
    const colorInputs = document.querySelectorAll('.color-input');
    
    colorInputs.forEach(input => {
        const textInput = input.parentElement.querySelector('.color-text');
        
        // Sincronizar color picker con texto
        input.addEventListener('input', function() {
            textInput.value = this.value.toUpperCase();
            updateColorPreview();
        });
        
        // Permitir edición manual del código de color
        textInput.addEventListener('input', function() {
            const colorValue = this.value;
            if (isValidHexColor(colorValue)) {
                input.value = colorValue;
                updateColorPreview();
            }
        });
        
        // Inicializar valores
        textInput.value = input.value.toUpperCase();
    });
}

function isValidHexColor(hex) {
    return /^#([0-9A-F]{3}){1,2}$/i.test(hex);
}

function updateColorPreview() {
    const colorPrimario = document.getElementById('colorPrimario').value;
    const colorSecundario = document.getElementById('colorSecundario').value;
    const colorTexto = document.getElementById('colorTexto').value;
    
    const previewCard = document.getElementById('color-preview');
    const previewHeader = previewCard.querySelector('.preview-header');
    const previewContent = previewCard.querySelector('.preview-content');
    const previewButton = previewCard.querySelector('.preview-button');
    
    // Aplicar colores a la vista previa
    previewHeader.style.backgroundColor = colorPrimario;
    previewHeader.style.color = '#ffffff';
    previewContent.style.backgroundColor = colorSecundario;
    previewContent.style.color = colorTexto;
    previewButton.style.backgroundColor = colorPrimario;
    previewButton.style.color = '#ffffff';
}

function setupColorPreview() {
    // Inicializar vista previa
    updateColorPreview();
    
    // Actualizar nombre del instituto en vista previa
    const nombreInstituto = document.getElementById('nombreInstituto');
    const previewTitle = document.querySelector('.preview-header h5');
    
    nombreInstituto.addEventListener('input', function() {
        previewTitle.textContent = this.value || 'Instituto Aurea';
    });
}

// =================   UPLOAD DE LOGO   =================
function setupFileUpload() {
    const logoInput = document.getElementById('logo-input');
    const logoPreview = document.getElementById('logo-preview');
    const logoPlaceholder = document.getElementById('logo-placeholder');
    
    logoInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        
        if (file) {
            // Validar tipo de archivo
            if (!file.type.startsWith('image/')) {
                showNotification('Por favor seleccione un archivo de imagen válido', 'error');
                return;
            }
            
            // Validar tamaño (5MB máximo)
            if (file.size > 5 * 1024 * 1024) {
                showNotification('El archivo debe ser menor a 5MB', 'error');
                return;
            }
            
            // Mostrar vista previa
            const reader = new FileReader();
            reader.onload = function(e) {
                logoPreview.src = e.target.result;
                logoPreview.style.display = 'block';
                logoPlaceholder.style.display = 'none';
            };
            reader.readAsDataURL(file);
        }
    });
}

// =================   CONFIGURACIONES AUTOMÁTICAS   =================
function setupAutomaticConfigToggle() {
    const bajaAutomaticaCheckbox = document.getElementById('permisoBajaAutomatica');
    const bajaAutomaticaConfig = document.getElementById('baja-automatica-config');
    
    // Función para mostrar/ocultar configuración de baja automática
    function toggleBajaAutomaticaConfig() {
        if (bajaAutomaticaCheckbox.checked) {
            bajaAutomaticaConfig.style.display = 'block';
            // Hacer campos requeridos
            document.getElementById('minimoAlumnoBaja').required = true;
            document.getElementById('inactividadBaja').required = true;
        } else {
            bajaAutomaticaConfig.style.display = 'none';
            // Quitar requerimiento
            document.getElementById('minimoAlumnoBaja').required = false;
            document.getElementById('inactividadBaja').required = false;
        }
    }
    
    // Inicializar estado
    toggleBajaAutomaticaConfig();
    
    // Escuchar cambios
    bajaAutomaticaCheckbox.addEventListener('change', toggleBajaAutomaticaConfig);
}

// =================   VALIDACIÓN DEL FORMULARIO   =================
function setupFormValidation() {
    const form = document.getElementById('config-form');
    const inputs = form.querySelectorAll('input[required], select[required]');
    
    inputs.forEach(input => {
        input.addEventListener('blur', validateField);
        input.addEventListener('input', clearFieldError);
    });
}

function validateField(event) {
    const field = event.target;
    const value = field.value.trim();
    
    // Limpiar errores previos
    clearFieldError(event);
    
    if (field.hasAttribute('required') && !value) {
        showFieldError(field, 'Este campo es obligatorio');
        return false;
    }
    
    // Validaciones específicas
    switch (field.type) {
        case 'email':
            if (value && !isValidEmail(value)) {
                showFieldError(field, 'Ingrese un email válido');
                return false;
            }
            break;
        case 'url':
            if (value && !isValidUrl(value)) {
                showFieldError(field, 'Ingrese una URL válida');
                return false;
            }
            break;
        case 'number':
            if (value && parseInt(value) < 1) {
                showFieldError(field, 'El valor debe ser mayor a 0');
                return false;
            }
            break;
    }
    
    return true;
}

function showFieldError(field, message) {
    field.classList.add('error');
    
    // Crear o actualizar mensaje de error
    let errorElement = field.parentElement.querySelector('.field-error');
    if (!errorElement) {
        errorElement = document.createElement('div');
        errorElement.className = 'field-error';
        field.parentElement.appendChild(errorElement);
    }
    errorElement.textContent = message;
}

function clearFieldError(event) {
    const field = event.target;
    field.classList.remove('error');
    
    const errorElement = field.parentElement.querySelector('.field-error');
    if (errorElement) {
        errorElement.remove();
    }
}

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function isValidUrl(url) {
    try {
        new URL(url);
        return true;
    } catch {
        return false;
    }
}

// =================   ENVÍO DEL FORMULARIO   =================
function setupFormSubmission() {
    const form = document.getElementById('config-form');
    
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        // Validar formulario completo
        if (!validateForm()) {
            showNotification('Por favor corrija los errores en el formulario', 'error');
            return;
        }
        
        // Mostrar loading
        showLoadingState(true);
        
        // Enviar formulario
        submitConfiguration();
    });
    
    // Botón de restablecer
    const resetButton = document.getElementById('btn-reset-config');
    resetButton.addEventListener('click', function() {
        if (confirm('¿Está seguro de que desea restablecer la configuración? Se perderán todos los cambios no guardados.')) {
            resetConfiguration();
        }
    });
}

function validateForm() {
    const form = document.getElementById('config-form');
    const requiredFields = form.querySelectorAll('input[required], select[required]');
    let isValid = true;
    
    requiredFields.forEach(field => {
        const event = { target: field };
        if (!validateField(event)) {
            isValid = false;
        }
    });
    
    return isValid;
}

function submitConfiguration() {
    const form = document.getElementById('config-form');
    const formData = new FormData(form);
    
    // Enviar al servidor
    fetch('/admin/configuracion/guardar', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        showLoadingState(false);
        if (data.success) {
            showNotification('Configuración guardada exitosamente', 'success');
            updateSaveStatus('success');
        } else {
            showNotification(data.message || 'Error al guardar la configuración', 'error');
            updateSaveStatus('error');
        }
    })
    .catch(error => {
        showLoadingState(false);
        console.error('Error:', error);
        showNotification('Error de conexión al servidor', 'error');
        updateSaveStatus('error');
    });
}

function resetConfiguration() {
    const form = document.getElementById('config-form');
    form.reset();
    
    // Restablecer vista previa de imagen
    const logoPreview = document.getElementById('logo-preview');
    const logoPlaceholder = document.getElementById('logo-placeholder');
    logoPreview.style.display = 'none';
    logoPlaceholder.style.display = 'block';
    
    // Restablecer colores
    setupColorInputs();
    updateColorPreview();
    
    // Restablecer configuraciones automáticas
    setupAutomaticConfigToggle();
    
    showNotification('Configuración restablecida', 'success');
}

// =================   ESTADO DE GUARDADO   =================
function updateSaveStatus(status) {
    const saveStatus = document.getElementById('save-status');
    if (!saveStatus) return;
    
    const icon = saveStatus.querySelector('i');
    const textNode = saveStatus.childNodes[saveStatus.childNodes.length - 1];
    
    saveStatus.className = 'status-indicator';
    
    switch (status) {
        case 'success':
            saveStatus.classList.add('status-success');
            if (icon) icon.className = 'fas fa-check-circle';
            if (textNode) textNode.textContent = ' Configuración guardada';
            break;
        case 'error':
            saveStatus.classList.add('status-error');
            if (icon) icon.className = 'fas fa-exclamation-circle';
            if (textNode) textNode.textContent = ' Error al guardar';
            break;
        case 'saving':
            saveStatus.classList.add('status-saving');
            if (icon) icon.className = 'fas fa-spinner fa-spin';
            if (textNode) textNode.textContent = ' Guardando...';
            break;
    }
}

function showLoadingState(loading) {
    const submitButton = document.querySelector('.btn-submit');
    const buttonText = submitButton.querySelector('span') || submitButton;
    const buttonIcon = submitButton.querySelector('i');
    
    if (loading) {
        submitButton.disabled = true;
        buttonIcon.className = 'fas fa-spinner fa-spin';
        buttonText.textContent = 'Guardando...';
        updateSaveStatus('saving');
    } else {
        submitButton.disabled = false;
        buttonIcon.className = 'fas fa-save';
        buttonText.textContent = 'Guardar Configuración';
    }
}

// =================   CARGA DE CONFIGURACIÓN GUARDADA   =================
function loadSavedConfiguration() {
    // Aquí se cargaría la configuración desde el servidor
    // Por ahora simulamos con datos de ejemplo
    
    // Actualizar vista previa inicial
    updateColorPreview();
    
    // Si hay logo guardado, mostrarlo
    // const logoUrl = '/admin/configuracion/logo';
    // Si existe el logo, cargarlo en la vista previa
}

// =================   NOTIFICACIONES   =================
function showNotification(message, type = 'success') {
    // Crear notificación
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    
    notification.innerHTML = `
        <div class="notification-content">
            <span>${message}</span>
            <button class="notification-close" onclick="this.parentElement.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;
    
    // Agregar al DOM
    document.body.appendChild(notification);
    
    // Auto remover después de 5 segundos
    setTimeout(() => {
        if (notification.parentElement) {
            notification.remove();
        }
    }, 5000);
}

// =================   UTILIDADES   =================
function formatCurrency(amount, currency = 'ARS') {
    const formatter = new Intl.NumberFormat('es-AR', {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 2
    });
    return formatter.format(amount);
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Aplicar debounce a la actualización de vista previa
const debouncedUpdatePreview = debounce(updateColorPreview, 300);

// Reemplazar el event listener directo con la versión debounced
document.addEventListener('DOMContentLoaded', function() {
    const colorInputs = document.querySelectorAll('.color-input, .color-text');
    colorInputs.forEach(input => {
        input.addEventListener('input', debouncedUpdatePreview);
    });
});
