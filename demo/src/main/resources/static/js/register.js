document.addEventListener("DOMContentLoaded", function () {
    console.log("🚀 Inicializando aplicación...");

    const stepIndicators = document.querySelectorAll(".steps-indicator .step-item");
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const submitBtn = document.getElementById("submitBtn");
    const formSteps = document.querySelectorAll(".form-step");
    let currentStep = 0;

    // ✅ Función para mostrar errores debajo del campo
    function showFieldError(input, message) {
        // Remover error previo
        hideFieldError(input);
        
        // Agregar clase de error al input
        input.classList.add('input-error');
        input.classList.remove('input-success');
        
        // Crear elemento de error
        const errorElement = document.createElement('span');
        errorElement.className = 'error-message';
        errorElement.textContent = message;
        errorElement.id = `${input.id}-error`;
        
        // Insertar después del input
        input.parentNode.appendChild(errorElement);
        
        console.log(`❌ Error en ${input.id}: ${message}`);
    }

    // ✅ Función para ocultar errores
    function hideFieldError(input) {
        input.classList.remove('input-error');
        
        const existingError = document.getElementById(`${input.id}-error`);
        if (existingError) {
            existingError.remove();
        }
    }

    // ✅ Función para mostrar éxito
    function showFieldSuccess(input) {
        input.classList.remove('input-error');
        input.classList.add('input-success');
        hideFieldError(input);
    }

    // ✅ Función para limpiar todos los errores de un paso
    function clearStepErrors(step) {
        const stepNode = formSteps[step];
        if (!stepNode) return;
        
        const inputs = stepNode.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            hideFieldError(input);
        });
    }

    // ✅ Inicializar listeners de ubicación inmediatamente
    function initializeLocationListeners() {
        console.log("🔄 Configurando listeners de ubicación...");
        
        const paisSelect = document.getElementById('pais');
        const provinciaSelect = document.getElementById('provincia');
        const ciudadSelect = document.getElementById('ciudad');

        if (!paisSelect) {
            console.error("❌ No se encontró el select de país");
            return;
        }

        // Limpiar event listeners previos
        const newPaisSelect = paisSelect.cloneNode(true);
        paisSelect.parentNode.replaceChild(newPaisSelect, paisSelect);

        // Configurar listener para país
        document.getElementById('pais').addEventListener('change', function(e) {
            const select = e.target;
            const selectedOption = select.options[select.selectedIndex];
            const hiddenCodigo = document.getElementById('paisCodigo');
            
            console.log("País seleccionado:", select.value);
            console.log("Data-codigo:", selectedOption.getAttribute('data-codigo'));
            
            hideFieldError(select);
            
            if (selectedOption.value && selectedOption.getAttribute('data-codigo')) {
                const countryCode = selectedOption.getAttribute('data-codigo');
                hiddenCodigo.value = countryCode;
                console.log("✅ País seleccionado - Código:", countryCode);
                
                showFieldSuccess(select);
                cargarProvincias(countryCode);
            } else {
                hiddenCodigo.value = '';
                showFieldError(select, 'Por favor selecciona un país válido');
                document.getElementById('provincia').disabled = true;
                document.getElementById('provincia').innerHTML = '<option value="">Primero selecciona un país</option>';
                document.getElementById('ciudad').disabled = true;
                document.getElementById('ciudad').innerHTML = '<option value="">Primero selecciona una provincia</option>';
            }
        });

        // Configurar listener para provincia
        document.getElementById('provincia').addEventListener('change', function(e) {
            const select = e.target;
            const selectedOption = select.options[select.selectedIndex];
            const hiddenCodigo = document.getElementById('provinciaCodigo');
            
            console.log("Provincia seleccionada:", select.value);
            
            hideFieldError(select);
            
            if (selectedOption.value && selectedOption.getAttribute('data-code')) {
                const provinceCode = selectedOption.getAttribute('data-code');
                hiddenCodigo.value = provinceCode;
                console.log("✅ Provincia seleccionada - Código:", provinceCode);
                
                showFieldSuccess(select);
                const countryCode = document.getElementById('paisCodigo').value;
                cargarCiudades(countryCode, provinceCode);
            } else {
                hiddenCodigo.value = '';
                showFieldError(select, 'Por favor selecciona una provincia válida');
                document.getElementById('ciudad').disabled = true;
                document.getElementById('ciudad').innerHTML = '<option value="">Primero selecciona una provincia</option>';
            }
        });

        // Configurar listener para ciudad
        document.getElementById('ciudad').addEventListener('change', function(e) {
            const select = e.target;
            const selectedOption = select.options[select.selectedIndex];
            const hiddenId = document.getElementById('ciudadId');
            
            console.log("Ciudad seleccionada:", select.value);
            
            hideFieldError(select);
            
            if (selectedOption.value && selectedOption.getAttribute('data-id')) {
                const cityId = selectedOption.getAttribute('data-id');
                hiddenId.value = cityId;
                console.log("✅ Ciudad seleccionada - ID:", cityId);
                showFieldSuccess(select);
            } else {
                hiddenId.value = '';
                showFieldError(select, 'Por favor selecciona una ciudad válida');
            }
        });

        console.log("✅ Listeners de ubicación configurados");
    }

    // ✅ Funciones para cargar datos
    function cargarProvincias(paisCode) {
        console.log("🌍 Cargando provincias para país:", paisCode);
        
        const provinciaSelect = document.getElementById('provincia');
        provinciaSelect.innerHTML = '<option value="">Cargando provincias...</option>';
        provinciaSelect.disabled = true;
        
        fetch(`/api/ubicaciones/provincias/${paisCode}`)
            .then(response => {
                console.log("✅ Respuesta recibida, status:", response.status);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(provincias => {
                console.log("📋 Provincias recibidas:", provincias);
                
                provinciaSelect.innerHTML = '<option value="">Selecciona una provincia</option>';
                
                if (provincias && provincias.length > 0) {
                    provincias.forEach(provincia => {
                        const option = document.createElement('option');
                        
                        const nombre = provincia.name || 'Sin nombre';
                        const codigo = provincia.iso2 || '';
                        const id = provincia.id || '';
                        
                        option.value = nombre;
                        option.textContent = nombre;
                        option.setAttribute('data-id', id);
                        option.setAttribute('data-code', codigo);
                        
                        provinciaSelect.appendChild(option);
                    });
                    provinciaSelect.disabled = false;
                    console.log(`✅ ${provincias.length} provincias cargadas correctamente`);
                } else {
                    provinciaSelect.innerHTML = '<option value="">No hay provincias disponibles</option>';
                }
                
                document.getElementById('provinciaCodigo').value = '';
                document.getElementById('ciudad').innerHTML = '<option value="">Primero selecciona una provincia</option>';
                document.getElementById('ciudad').disabled = true;
                document.getElementById('ciudadId').value = '';
            })
            .catch(error => {
                console.error('❌ Error cargando provincias:', error);
                provinciaSelect.innerHTML = '<option value="">Error al cargar provincias</option>';
                showFieldError(document.getElementById('pais'), 'Error al cargar las provincias. Intenta nuevamente.');
            });
    }

    function cargarCiudades(paisCode, provinciaCode) {
        console.log("🏙️ Cargando ciudades para país:", paisCode, "provincia:", provinciaCode);
        
        const ciudadSelect = document.getElementById('ciudad');
        ciudadSelect.innerHTML = '<option value="">Cargando ciudades...</option>';
        ciudadSelect.disabled = true;
        
        fetch(`/api/ubicaciones/ciudades/${paisCode}/${provinciaCode}`)
            .then(response => {
                console.log("Respuesta ciudades status:", response.status);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(ciudades => {
                console.log("📋 Ciudades recibidas:", ciudades);
                
                ciudadSelect.innerHTML = '<option value="">Selecciona una ciudad</option>';
                
                if (ciudades && ciudades.length > 0) {
                    ciudades.forEach(ciudad => {
                        const option = document.createElement('option');
                        
                        const nombre = ciudad.name || ciudad.nombre || 'Sin nombre';
                        const id = ciudad.id || '';
                        
                        option.value = nombre;
                        option.textContent = nombre;
                        option.setAttribute('data-id', id);
                        ciudadSelect.appendChild(option);
                    });
                    ciudadSelect.disabled = false;
                } else {
                    ciudadSelect.innerHTML = '<option value="">No hay ciudades disponibles</option>';
                }
                
                document.getElementById('ciudadId').value = '';
            })
            .catch(error => {
                console.error('❌ Error cargando ciudades:', error);
                ciudadSelect.innerHTML = '<option value="">Error al cargar ciudades</option>';
                showFieldError(document.getElementById('provincia'), 'Error al cargar las ciudades. Intenta nuevamente.');
            });
    }

    // ✅ Sistema de pasos del formulario
    function showStep(step) {
        console.log("📋 Mostrando paso:", step + 1);
        
        // Limpiar errores del paso anterior
        clearStepErrors(currentStep);
        
        formSteps.forEach((s, i) => {
            s.style.display = (i === step) ? "block" : "none";
        });

        stepIndicators.forEach((indicator, i) => {
            indicator.classList.remove("active", "complete");
            if (i < step) {
                indicator.classList.add("complete");
            } else if (i === step) {
                indicator.classList.add("active");
            }
        });

        // Mostrar/ocultar botones
        prevBtn.style.display = (step === 0) ? "none" : "inline-block";
        nextBtn.style.display = (step === formSteps.length - 1) ? "none" : "inline-block";
        submitBtn.style.display = (step === formSteps.length - 1) ? "inline-block" : "none";

        // ✅ Inicializar ubicación cuando se muestre el paso 2 (Domicilio)
        if (step === 1) {
            console.log("📍 Inicializando sistema de ubicación para paso 2...");
            setTimeout(initializeLocationListeners, 100);
        }
    }

    function validateStep(step) {
        console.log("🔍 Validando paso:", step + 1);
        
        const stepNode = formSteps[step];
        if (!stepNode) return true;

        // Limpiar errores previos del paso actual
        clearStepErrors(step);
    
        const inputs = stepNode.querySelectorAll('input, select, textarea');
        let isValid = true;
        let firstInvalidInput = null;

        for (let input of inputs) {
            if (input.disabled) continue;
            
            input.setCustomValidity('');
            hideFieldError(input);
            
            if (input.tagName === 'SELECT') {
                if (input.required && !input.value) {
                    showFieldError(input, `Por favor, selecciona ${input.previousElementSibling?.textContent?.toLowerCase() || 'una opción'}`);
                    if (!firstInvalidInput) firstInvalidInput = input;
                    isValid = false;
                } else {
                    showFieldSuccess(input);
                }
            } else {
                if (!input.checkValidity()) {
                    const errorMessage = getCustomErrorMessage(input);
                    showFieldError(input, errorMessage);
                    if (!firstInvalidInput) firstInvalidInput = input;
                    isValid = false;
                } else {
                    // Validaciones personalizadas adicionales
                    const customValidation = validateCustomRules(input);
                    if (!customValidation.isValid) {
                        showFieldError(input, customValidation.message);
                        if (!firstInvalidInput) firstInvalidInput = input;
                        isValid = false;
                    } else {
                        showFieldSuccess(input);
                    }
                }
            }
        }
    
        // ✅ Validación específica para cada paso
        if (isValid) {
            switch (step) {
                case 0: // Paso 1: Datos Personales
                    isValid = validatePersonalData();
                    break;
                case 1: // Paso 2: Domicilio
                    isValid = validateLocation();
                    break;
                case 2: // Paso 3: Datos Académicos
                    isValid = validateAcademicData();
                    break;
                case 3: // Paso 4: Cuenta y Confirmación
                    isValid = validateAccountData();
                    break;
            }
        }

        // Hacer scroll al primer error
        if (firstInvalidInput) {
            firstInvalidInput.focus();
            firstInvalidInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    
        return isValid;
    }

    function validatePersonalData() {
        let isValid = true;
        const dni = document.getElementById('dni');
        const telefono = document.getElementById('telefono');
        const email = document.getElementById('email');
        const fechaNacimiento = document.getElementById('fechaNacimiento');
        const nombre = document.getElementById('nombre');
        const apellido = document.getElementById('apellido');

        // Validar nombre (solo letras)
        if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(nombre.value)) {
            showFieldError(nombre, 'El nombre solo puede contener letras y espacios');
            isValid = false;
        }

        // Validar apellido (solo letras)
        if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(apellido.value)) {
            showFieldError(apellido, 'El apellido solo puede contener letras y espacios');
            isValid = false;
        }

        // Validar DNI (7 u 8 dígitos)
        if (!/^\d{7,8}$/.test(dni.value)) {
            showFieldError(dni, 'El DNI debe tener 7 u 8 dígitos numéricos');
            isValid = false;
        }

        // Validar teléfono (al menos 10 dígitos)
        const digitosTelefono = telefono.value.replace(/\D/g, '');
        if (digitosTelefono.length < 10) {
            showFieldError(telefono, 'El teléfono debe tener al menos 10 dígitos');
            isValid = false;
        }

        // Validar email (debe tener @ y dominio)
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
            showFieldError(email, 'Por favor ingresa un correo electrónico válido (ejemplo: usuario@dominio.com)');
            isValid = false;
        }

        // Validar fecha de nacimiento (mínimo 16 años)
        if (fechaNacimiento.value) {
            const fechaNac = new Date(fechaNacimiento.value);
            const hoy = new Date();
            let edad = hoy.getFullYear() - fechaNac.getFullYear();
            const mes = hoy.getMonth() - fechaNac.getMonth();
            
            if (mes < 0 || (mes === 0 && hoy.getDate() < fechaNac.getDate())) {
                edad--;
            }
            
            if (edad < 16) {
                showFieldError(fechaNacimiento, 'Debes tener al menos 16 años para registrarte');
                isValid = false;
            }
        }

        return isValid;
    }
    
    function validateLocation() {
        let isValid = true;
        const paisSelect = document.getElementById('pais');
        const provinciaSelect = document.getElementById('provincia');
        const ciudadSelect = document.getElementById('ciudad');
        const paisCodigo = document.getElementById('paisCodigo').value;
        const provinciaCodigo = document.getElementById('provinciaCodigo').value;
        const ciudadId = document.getElementById('ciudadId').value;
        
        if (!paisCodigo) {
            showFieldError(paisSelect, 'Por favor, selecciona un país');
            isValid = false;
        }
        
        if (!provinciaCodigo) {
            showFieldError(provinciaSelect, 'Por favor, selecciona una provincia');
            isValid = false;
        }
        
        if (!ciudadId) {
            showFieldError(ciudadSelect, 'Por favor, selecciona una ciudad');
            isValid = false;
        }
        
        return isValid;
    }
    
    function validateAcademicData() {
        let isValid = true;
        const añoEgreso = document.getElementById('añoEgreso');
        const colegioEgreso = document.getElementById('colegioEgreso');
        const ultimosEstudios = document.getElementById('ultimosEstudios');

        // Validar selección de últimos estudios
        if (!ultimosEstudios.value) {
            showFieldError(ultimosEstudios, 'Por favor, selecciona tu nivel de estudios');
            isValid = false;
        }

        // Validar año de egreso (1980-2025)
        const año = parseInt(añoEgreso.value);
        if (año < 1980 || año > 2025) {
            showFieldError(añoEgreso, 'El año de egreso debe estar entre 1980 y 2025');
            isValid = false;
        }

        // Validar colegio (solo letras, números y espacios)
        if (!/^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\s\-\.\(\)]+$/.test(colegioEgreso.value)) {
            showFieldError(colegioEgreso, 'Solo se permiten letras, números, espacios y los caracteres: - . ( )');
            isValid = false;
        }

        return isValid;
    }
    
    function validateAccountData() {
        let isValid = true;
        const password = document.getElementById('password');
        const confirmPassword = document.getElementById('confirmPassword');
        const terms = document.getElementById('terms');

        // Validar contraseña (mínimo 8 caracteres, al menos 1 mayúscula y 1 minúscula)
        if (!/^(?=.*[a-z])(?=.*[A-Z]).{8,}$/.test(password.value)) {
            showFieldError(password, 'La contraseña debe tener al menos 8 caracteres, incluyendo una mayúscula y una minúscula');
            isValid = false;
        }

        // Validar que las contraseñas coincidan
        if (password.value !== confirmPassword.value) {
            showFieldError(confirmPassword, 'Las contraseñas no coinciden');
            isValid = false;
        }

        // Validar términos y condiciones
        if (!terms.checked) {
            showFieldError(terms, 'Debes aceptar los términos y condiciones');
            isValid = false;
        }

        return isValid;
    }
    
    // ✅ Funciones auxiliares
    function getCustomErrorMessage(input) {
        switch (input.type) {
            case 'email':
                return 'Por favor ingresa un correo electrónico válido';
            case 'tel':
                return 'Por favor ingresa un número de teléfono válido';
            case 'number':
                if (input.validity.rangeUnderflow) return `El valor mínimo permitido es ${input.min}`;
                if (input.validity.rangeOverflow) return `El valor máximo permitido es ${input.max}`;
                return 'Por favor ingresa un número válido';
            default:
                if (input.validity.valueMissing) return 'Este campo es obligatorio';
                if (input.validity.patternMismatch) return 'El formato no es válido';
                return 'Por favor completa este campo correctamente';
        }
    }
    
    function validateCustomRules(input) {
        // Esta función ahora se maneja dentro de validatePersonalData
        return { isValid: true };
    }

    // Event Listeners para navegación
    prevBtn.addEventListener("click", function () {
        if (currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        }
    });

    nextBtn.addEventListener("click", function () {
        if (validateStep(currentStep)) {
            // ✅ SI ES EL PASO 2 (DOMICILIO), GUARDAR LAS UBICACIONES ANTES DE AVANZAR
            if (currentStep === 1) { // Paso 2 es índice 1
                guardarUbicaciones().then(success => {
                    if (success) {
                        currentStep++;
                        showStep(currentStep);
                    }
                }).catch(error => {
                    console.error('❌ Error guardando ubicaciones:', error);
                    alert('Error al guardar la ubicación. Intenta nuevamente.');
                });
            } else {
                currentStep++;
                showStep(currentStep);
            }
        }
    });
    
    // ✅ Función para guardar ubicaciones
    function guardarUbicaciones() {
        return new Promise((resolve, reject) => {
            const paisCodigo = document.getElementById('paisCodigo').value;
            const provinciaCodigo = document.getElementById('provinciaCodigo').value;
            const ciudadId = document.getElementById('ciudadId').value;
            
            console.log("💾 Guardando ubicaciones:", { paisCodigo, provinciaCodigo, ciudadId });
    
            if (!paisCodigo || !provinciaCodigo || !ciudadId) {
                reject(new Error('Faltan datos de ubicación'));
                return;
            }
    
            // ✅ Obtener el token CSRF del formulario principal
            const csrfToken = document.querySelector('input[name="_csrf"]').value;
            console.log("🔐 CSRF Token:", csrfToken);
    
            const formData = new FormData();
            formData.append('paisCodigo', paisCodigo);
            formData.append('provinciaCodigo', provinciaCodigo);
            formData.append('ciudadId', ciudadId);
            formData.append('_csrf', csrfToken); // ✅ Agregar el token CSRF
    
            fetch('/api/ubicaciones/guardar', {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Error del servidor: ' + response.status);
                }
                return response.text();
            })
            .then(message => {
                console.log("✅ Ubicaciones guardadas:", message);
                resolve(true);
            })
            .catch(error => {
                console.error('❌ Error:', error);
                reject(error);
            });
        });
    }
    
    // En el evento del botón Siguiente
    nextBtn.addEventListener("click", function () {
        if (validateStep(currentStep)) {
            if (currentStep === 1) { // Paso 2 es índice 1
                guardarUbicaciones().then(success => {
                    if (success) {
                        currentStep++;
                        showStep(currentStep);
                    }
                }).catch(error => {
                    console.error('❌ Error guardando ubicaciones:', error);
                    alert('Error al guardar la ubicación. Intenta nuevamente.');
                });
            } else {
                currentStep++;
                showStep(currentStep);
            }
        }
    });

    // Manejar envío del formulario
    const form = document.getElementById('registerForm');
    if (form) {
        form.addEventListener('submit', function (e) {
            console.log("📤 Formulario enviándose...");
            
            if (currentStep !== formSteps.length - 1) {
                e.preventDefault();
                console.log("❌ No es el último paso, previniendo envío");
                return false;
            }
            
            if (!validateStep(formSteps.length - 1)) {
                e.preventDefault();
                console.log("❌ Validación falló, previniendo envío");
                return false;
            }
            
            console.log("✅ Formulario válido, enviando...");
            submitBtn.disabled = true;
            submitBtn.textContent = "Registrando...";
            
            return true;
        });
    }

    // Inicializar
    showStep(currentStep);
    console.log("✅ Aplicación inicializada correctamente");
});