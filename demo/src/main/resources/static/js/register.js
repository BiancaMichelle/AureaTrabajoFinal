// ================ REGISTRO MULTI-STEP ================
let currentStep = 1;
const totalSteps = 4; // Corregido a 4 pasos

// Inicializar cuando se carga el DOM
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Inicializando formulario de registro multi-step');
    initializeForm();
    enhanceStyles();
});

function initializeForm() {
    // Referencias a elementos del DOM
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const submitBtn = document.getElementById('submitBtn');
    const form = document.getElementById('registerForm');
    
    console.log('📋 Elementos del formulario:', {
        prevBtn: !!prevBtn,
        nextBtn: !!nextBtn,
        submitBtn: !!submitBtn,
        form: !!form
    });
    
    // Event listeners para navegación
    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('▶️ Botón siguiente presionado');
            nextStep();
        });
    }
    
    if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('◀️ Botón anterior presionado');
            prevStep();
        });
    }
    
    // Event listener para envío del formulario
    if (form) {
        form.addEventListener('submit', function(e) {
            console.log('📝 Intentando enviar formulario');
            if (!validateCurrentStep()) {
                e.preventDefault();
                console.log('❌ Validación del formulario falló');
                return false;
            }
            
            // Verificar términos en el último paso (paso 4)
            if (currentStep === totalSteps) {
                const termsCheckbox = document.getElementById('terms');
                if (termsCheckbox && !termsCheckbox.checked) {
                    e.preventDefault();
                    alert('Debes aceptar los términos y condiciones para continuar');
                    termsCheckbox.focus();
                    return false;
                }
            }
            
            console.log('✅ Formulario válido - Enviando...');
        });
    }
    
    // Inicializar la vista del primer paso
    showStep(currentStep);
    console.log('✅ Formulario inicializado correctamente');
}

function nextStep() {
    console.log(`➡️ Avanzando desde paso ${currentStep} de ${totalSteps}`);
    
    if (validateCurrentStep()) {
        if (currentStep < totalSteps) {
            // Generar resumen antes del último paso (paso 3 -> paso 4)
            if (currentStep === totalSteps - 1) {
                console.log('📊 Generando resumen para último paso');
                generateSummary();
            }
            
            currentStep++;
            showStep(currentStep);
            console.log(`✅ Avanzado al paso ${currentStep}`);
        } else {
            console.log('⚠️ Ya estás en el último paso');
        }
    } else {
        console.log(`❌ No se puede avanzar - Errores en paso ${currentStep}`);
        highlightErrors();
    }
}

function prevStep() {
    console.log(`⬅️ Retrocediendo desde paso ${currentStep}`);
    
    if (currentStep > 1) {
        currentStep--;
        showStep(currentStep);
        console.log(`✅ Retrocedido al paso ${currentStep}`);
    } else {
        console.log('⚠️ Ya estás en el primer paso');
    }
}

function showStep(step) {
    console.log(`� Mostrando paso ${step} de ${totalSteps}`);
    
    // Obtener elementos
    const stepContents = document.querySelectorAll('.step-content');
    const stepItems = document.querySelectorAll('.step-item');
    
    // Ocultar todos los pasos
    stepContents.forEach((content, index) => {
        content.classList.remove('active');
    });
    
    // Actualizar indicadores de pasos
    stepItems.forEach((item) => {
        const stepNumber = parseInt(item.dataset.step);
        item.classList.remove('active');
        
        if (stepNumber < step) {
            item.classList.add('completed');
        } else {
            item.classList.remove('completed');
        }
    });

    // Mostrar paso actual
    const currentContent = document.getElementById(`step${step}`);
    const currentItem = document.querySelector(`[data-step="${step}"]`);
    
    if (currentContent) {
        currentContent.classList.add('active');
        // Limpiar errores previos
        const errorFields = currentContent.querySelectorAll('.error');
        errorFields.forEach(field => field.classList.remove('error'));
        console.log(`✅ Paso ${step} mostrado correctamente`);
    } else {
        console.error(`❌ Contenido del paso ${step} no encontrado`);
    }
    
    if (currentItem) {
        currentItem.classList.add('active');
    } else {
        console.error(`❌ Indicador del paso ${step} no encontrado`);
    }

    updateButtons();
}

function updateButtons() {
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const submitBtn = document.getElementById('submitBtn');
    
    console.log(`🔄 Actualizando botones para paso ${currentStep}`);
    
    // Botón anterior - solo visible si no estamos en el primer paso
    if (prevBtn) {
        const showPrev = currentStep > 1;
        prevBtn.style.display = showPrev ? 'block' : 'none';
        console.log(`   ◀️ Anterior: ${showPrev ? 'visible' : 'oculto'}`);
    }
    
    // Botones siguiente/enviar según el paso actual
    if (currentStep === totalSteps) {
        // Último paso - mostrar botón de envío
        if (nextBtn) nextBtn.style.display = 'none';
        if (submitBtn) submitBtn.style.display = 'block';
        console.log('   📤 Botón envío visible');
    } else {
        // Pasos intermedios - mostrar botón siguiente
        if (nextBtn) {
            nextBtn.style.display = 'block';
            nextBtn.textContent = currentStep === totalSteps - 1 ? 'Revisar' : 'Siguiente';
            console.log(`   ▶️ Siguiente visible: "${nextBtn.textContent}"`);
        }
        if (submitBtn) submitBtn.style.display = 'none';
    }
}

function validateCurrentStep() {
    console.log(`🔍 Validando paso ${currentStep}`);
    
    const currentStepContent = document.getElementById(`step${currentStep}`);
    if (!currentStepContent) {
        console.error('❌ Contenido del paso no encontrado');
        return false;
    }
    
    const requiredInputs = currentStepContent.querySelectorAll('input[required], select[required]');
    let isValid = true;
    let errorCount = 0;
    
    console.log(`   📋 Validando ${requiredInputs.length} campos requeridos`);

    requiredInputs.forEach((input, index) => {
        input.classList.remove('error');
        
        const fieldName = input.name || input.id || `campo-${index}`;
        const value = input.value ? input.value.trim() : '';
        
        if (!value) {
            isValid = false;
            errorCount++;
            input.classList.add('error');
            console.log(`   ❌ Campo vacío: ${fieldName}`);
        } else {
            // Validaciones específicas por tipo de campo
            if (input.type === 'email' && !isValidEmail(value)) {
                isValid = false;
                errorCount++;
                input.classList.add('error');
                console.log(`   ❌ Email inválido: ${fieldName}`);
            }
            
            if (input.id === 'confirmPassword') {
                const password = document.getElementById('password');
                if (password && value !== password.value) {
                    isValid = false;
                    errorCount++;
                    input.classList.add('error');
                    console.log(`   ❌ Contraseñas no coinciden`);
                }
            }
            
            if (input.type === 'date') {
                const birthDate = new Date(value);
                const today = new Date();
                const age = today.getFullYear() - birthDate.getFullYear();
                
                if (age < 16 || age > 100 || birthDate > today) {
                    isValid = false;
                    errorCount++;
                    input.classList.add('error');
                    console.log(`   ❌ Fecha inválida: ${fieldName} (${age} años)`);
                }
            }
            
            if (input.id === 'dni') {
                const dniPattern = /^[0-9]{7,8}$/;
                if (!dniPattern.test(value)) {
                    isValid = false;
                    errorCount++;
                    input.classList.add('error');
                    console.log(`   ❌ DNI inválido: ${fieldName}`);
                }
            }
            
            if (input.id === 'telefono') {
                // Validar teléfono (al menos 8 dígitos)
                const phonePattern = /^[\d\s\-\(\)\+]{8,}$/;
                if (!phonePattern.test(value)) {
                    isValid = false;
                    errorCount++;
                    input.classList.add('error');
                    console.log(`   ❌ Teléfono inválido: ${fieldName}`);
                }
            }
            
            if (input.id === 'anoEgreso') {
                const year = parseInt(value);
                const currentYear = new Date().getFullYear();
                if (year < 1950 || year > currentYear + 5) {
                    isValid = false;
                    errorCount++;
                    input.classList.add('error');
                    console.log(`   ❌ Año de egreso inválido: ${fieldName}`);
                }
            }
            
            if (input.id === 'password') {
                if (value.length < 8) {
                    isValid = false;
                    errorCount++;
                    input.classList.add('error');
                    console.log(`   ❌ Contraseña muy corta: ${fieldName}`);
                }
            }
        }
    });

    // Validación especial para términos y condiciones en el último paso
    if (currentStep === totalSteps) {
        const termsCheckbox = document.getElementById('terms');
        if (termsCheckbox && !termsCheckbox.checked) {
            isValid = false;
            errorCount++;
            termsCheckbox.classList.add('error');
            console.log('   ❌ Términos no aceptados');
        }
    }

    console.log(`   ${isValid ? '✅' : '❌'} Resultado: ${errorCount} errores encontrados`);
    return isValid;
}

function highlightErrors() {
    const currentStepContent = document.getElementById(`step${currentStep}`);
    if (!currentStepContent) return;
    
    const errorFields = currentStepContent.querySelectorAll('.error');
    if (errorFields.length > 0) {
        console.log(`🔍 Destacando ${errorFields.length} campos con errores`);
        
        // Hacer scroll al primer campo con error y darle foco
        errorFields[0].scrollIntoView({ 
            behavior: 'smooth', 
            block: 'center' 
        });
        
        setTimeout(() => {
            errorFields[0].focus();
        }, 300);
    }
}

function generateSummary() {
    console.log('📊 Generando resumen del formulario');
    
    const summaryContent = document.getElementById('summaryContent');
    if (!summaryContent) {
        console.error('❌ Contenedor de resumen no encontrado');
        return;
    }
    
    const form = document.getElementById('registerForm');
    if (!form) {
        console.error('❌ Formulario no encontrado');
        return;
    }
    
    const formData = new FormData(form);
    
    // Generar HTML del resumen
    const summaryHTML = `
        <div class="summary-section">
            <h4><i>👤</i> Datos Personales</h4>
            ${createSummaryItem('Nombre completo', `${formData.get('nombre') || ''} ${formData.get('apellido') || ''}`)}
            ${createSummaryItem('DNI', formData.get('dni') || '')}
            ${createSummaryItem('Género', formData.get('genero') || '')}
            ${createSummaryItem('Fecha de Nacimiento', formatDate(formData.get('fechaNacimiento')))}
            ${createSummaryItem('Teléfono', formData.get('telefono') || '')}
            ${createSummaryItem('Correo Electrónico', formData.get('email') || '')}
        </div>
        
        <div class="summary-section">
            <h4><i>🏠</i> Domicilio</h4>
            ${createSummaryItem('País', formData.get('pais') || '')}
            ${createSummaryItem('Provincia', formData.get('provincia') || '')}
            ${createSummaryItem('Ciudad', formData.get('ciudad') || '')}
            ${createSummaryItem('Domicilio', formData.get('domicilio') || '')}
        </div>
        
        <div class="summary-section">
            <h4><i>🎓</i> Datos Académicos</h4>
            ${createSummaryItem('Últimos Estudios', getEstudiosLabel(formData.get('ultimosEstudios')))}
            ${createSummaryItem('Colegio de Egreso', formData.get('colegioEgreso') || '')}
            ${createSummaryItem('Año de Egreso', formData.get('anoEgreso') || '')}
        </div>
        
        <div class="summary-section">
            <h4><i>�</i> Cuenta</h4>
            ${createSummaryItem('Correo de Usuario', formData.get('email') || '')}
            ${createSummaryItem('Contraseña', '••••••••')}
        </div>
    `;
    
    summaryContent.innerHTML = summaryHTML;
    console.log('✅ Resumen generado correctamente');
}

function getEstudiosLabel(value) {
    const labels = {
        'primaria-completa': 'Primaria Completa',
        'primaria-incompleta': 'Primaria Incompleta',
        'secundaria-completa': 'Secundaria Completa',
        'secundaria-incompleta': 'Secundaria Incompleta',
        'terciario-completo': 'Terciario Completo',
        'terciario-incompleto': 'Terciario Incompleto',
        'universitario-completo': 'Universitario Completo',
        'universitario-incompleto': 'Universitario Incompleto',
        'posgrado': 'Posgrado'
    };
    return labels[value] || value || 'No especificado';
}

function createSummaryItem(label, value) {
    const displayValue = value && value.trim() ? value.trim() : 'No especificado';
    return `
        <div class="summary-item">
            <span class="summary-label">${label}:</span>
            <span class="summary-value">${displayValue}</span>
        </div>
    `;
}

function formatDate(dateString) {
    if (!dateString) return 'No especificado';
    
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('es-ES', {
            day: '2-digit',
            month: '2-digit', 
            year: 'numeric'
        });
    } catch (error) {
        console.error('Error formateando fecha:', error);
        return dateString;
    }
}

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Función para mejorar estilos CSS dinámicamente
function enhanceStyles() {
    // Prevenir múltiples cargas
    if (document.querySelector('#register-enhanced-styles')) return;
    
    const style = document.createElement('style');
    style.id = 'register-enhanced-styles';
    style.textContent = `
        /* Estilos mejorados para campos con error */
        .form-group input.error,
        .form-group select.error {
            border-color: #dc3545 !important;
            box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25) !important;
            background-color: rgba(220, 53, 69, 0.05) !important;
            animation: shake 0.5s ease-in-out;
        }
        
        .checkbox-wrapper input.error + .checkmark {
            border-color: #dc3545 !important;
            box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25) !important;
        }
        
        @keyframes shake {
            0%, 100% { transform: translateX(0); }
            25% { transform: translateX(-5px); }
            75% { transform: translateX(5px); }
        }
        
        /* Estilos del resumen mejorados */
        .summary-section {
            margin-bottom: 1.5rem;
            padding: 1.2rem;
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%);
            border-radius: 12px;
            border: 1px solid rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(10px);
        }
        
        .summary-section h4 {
            color: var(--main-red);
            margin-bottom: 1rem;
            font-size: 1.1rem;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }
        
        .summary-section h4 i {
            font-style: normal;
            font-size: 1.2rem;
        }
        
        .summary-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 0.8rem;
            padding: 0.5rem 0;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
            transition: all 0.2s ease;
        }
        
        .summary-item:last-child {
            border-bottom: none;
            margin-bottom: 0;
        }
        
        .summary-item:hover {
            background-color: rgba(255, 255, 255, 0.03);
            padding-left: 0.5rem;
            border-radius: 6px;
        }
        
        .summary-label {
            font-weight: 500;
            color: var(--text-gray);
            min-width: 140px;
            font-size: 0.95rem;
        }
        
        .summary-value {
            color: var(--white);
            text-align: right;
            font-weight: 400;
            max-width: 60%;
            word-break: break-word;
        }
        
        /* Campos válidos */
        .form-group input:valid:not(:placeholder-shown),
        .form-group select:valid:not([value=""]) {
            border-color: rgba(40, 167, 69, 0.5);
            box-shadow: 0 0 0 0.1rem rgba(40, 167, 69, 0.15);
        }
        
        /* Mejora visual para checkboxes con error */
        .checkbox-wrapper.error::after {
            content: "Este campo es obligatorio";
            position: absolute;
            bottom: -20px;
            left: 0;
            color: #dc3545;
            font-size: 0.8rem;
            font-weight: 500;
        }
        
        /* Transiciones suaves para los pasos */
        .step-content {
            transition: opacity 0.3s ease-in-out;
        }
        
        .step-content.active {
            opacity: 1;
        }
        
        .step-content:not(.active) {
            opacity: 0;
        }
    `;
    
    document.head.appendChild(style);
    console.log('🎨 Estilos mejorados aplicados');
}