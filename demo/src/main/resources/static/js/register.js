document.addEventListener("DOMContentLoaded", function () {
    console.log("🚀 Inicializando aplicación...");

    const stepIndicators = document.querySelectorAll(".steps-indicator .step-item");
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const submitBtn = document.getElementById("submitBtn");
    const formSteps = document.querySelectorAll(".form-step");
    const dniInput = document.getElementById('dni');
    const telefonoInput = document.getElementById('telefono');
    let currentStep = 0;

    // ✅ CONFIGURACIÓN PARA CAMPO DE FECHA
    const fechaInput = document.getElementById('fechaNacimiento');
    if (fechaInput) {
        // Establecer fecha por defecto 18 años atrás (formato YYYY-MM-DD para input date)
        const fechaSugerida = obtenerFecha18AñosAtrasISO();
        fechaInput.value = fechaSugerida;
        
        console.log(`📅 Fecha sugerida establecida: ${fechaSugerida}`);
        
        // Validar fecha al cambiar
        fechaInput.addEventListener('change', function(e) {
            const fechaSeleccionada = e.target.value;
            if (fechaSeleccionada) {
                // Validar que la persona tenga al menos 16 años
                const hoy = new Date();
                const fechaNacimiento = new Date(fechaSeleccionada);
                const edad = calcularEdad(fechaNacimiento, hoy);
                
                if (edad < 16) {
                    showFieldError(fechaInput, 'Debes tener al menos 16 años para registrarte');
                } else if (edad > 100) {
                    showFieldError(fechaInput, 'Por favor verifica la fecha, parece ser demasiado antigua');
                } else {
                    hideFieldError(fechaInput);
                    console.log(`✅ Edad válida: ${edad} años`);
                }
            }
        });
        
        // Validar al perder foco
        fechaInput.addEventListener('blur', function(e) {
            if (!e.target.value) {
                showFieldError(fechaInput, 'La fecha de nacimiento es obligatoria');
            }
        });
    }

    // ✅ Función para validar fecha DD/MM/AAAA
    function isValidDate(dateString) {
        if (!dateString) return false;
        
        const pattern = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/;
        const match = dateString.match(pattern);
        
        if (!match) return false;
        
        const dia = parseInt(match[1], 10);
        const mes = parseInt(match[2], 10);
        const año = parseInt(match[3], 10);
        
        // Validar rangos
        if (mes < 1 || mes > 12) return false;
        if (dia < 1 || dia > 31) return false;
        
        // Validar febrero y meses con 30 días
        const fecha = new Date(año, mes - 1, dia);
        return fecha.getDate() === dia && 
               fecha.getMonth() === mes - 1 && 
               fecha.getFullYear() === año;
    }

    // ✅ Función para obtener fecha 18 años atrás de la fecha actual (formato DD/MM/AAAA)
    function obtenerFecha18AñosAtras() {
        const hoy = new Date();
        const año18AñosAtras = hoy.getFullYear() - 18;
        const mes = hoy.getMonth() + 1; // getMonth() devuelve 0-11, necesitamos 1-12
        const dia = hoy.getDate();
        
        // Formatear con ceros a la izquierda si es necesario
        const diaFormateado = dia.toString().padStart(2, '0');
        const mesFormateado = mes.toString().padStart(2, '0');
        
        return `${diaFormateado}/${mesFormateado}/${año18AñosAtras}`;
    }

    // ✅ Función para obtener fecha 18 años atrás en formato ISO (YYYY-MM-DD) para input date
    function obtenerFecha18AñosAtrasISO() {
        const hoy = new Date();
        const año18AñosAtras = hoy.getFullYear() - 18;
        const mes = hoy.getMonth() + 1; // getMonth() devuelve 0-11, necesitamos 1-12
        const dia = hoy.getDate();
        
        // Formatear con ceros a la izquierda si es necesario
        const diaFormateado = dia.toString().padStart(2, '0');
        const mesFormateado = mes.toString().padStart(2, '0');
        
        return `${año18AñosAtras}-${mesFormateado}-${diaFormateado}`;
    }

    // ✅ Función para calcular edad en años
    function calcularEdad(fechaNacimiento, fechaActual = new Date()) {
        let edad = fechaActual.getFullYear() - fechaNacimiento.getFullYear();
        const mesActual = fechaActual.getMonth();
        const mesNacimiento = fechaNacimiento.getMonth();
        
        if (mesActual < mesNacimiento || (mesActual === mesNacimiento && fechaActual.getDate() < fechaNacimiento.getDate())) {
            edad--;
        }
        
        return edad;
    }

    // ✅ Función para convertir DD/MM/AAAA a yyyy-MM-dd
    function convertirFechaParaBackend(fechaDDMMAAAA) {
        if (!fechaDDMMAAAA) return null;
        
        const partes = fechaDDMMAAAA.split('/');
        if (partes.length === 3) {
            const dia = partes[0].padStart(2, '0');
            const mes = partes[1].padStart(2, '0');
            const año = partes[2];
            return `${año}-${mes}-${dia}`;
        }
        return null;
    }

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

    // ✅ Notificaciones globales (usa estilos de auth.css)
    function showNotification(message, type = 'info', duration = 8000) {
        if (type === 'error') {
            const existingErrors = document.querySelectorAll('.notification-error');
            existingErrors.forEach(notif => {
                if (notif.parentNode) {
                    notif.parentNode.removeChild(notif);
                }
            });
        }

        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;

        const icons = {
            success: 'fas fa-check-circle',
            error: 'fas fa-exclamation-circle',
            warning: 'fas fa-exclamation-triangle',
            info: 'fas fa-info-circle'
        };

        notification.innerHTML = `
            <div class="notification-content">
                <div class="notification-icon">
                    <i class="${icons[type] || icons.info}"></i>
                </div>
                <div class="notification-message">${message}</div>
                <button class="notification-close" title="Cerrar">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="notification-progress"></div>
        `;

        document.body.appendChild(notification);

        setTimeout(() => {
            notification.classList.add('show');
            const progressBar = notification.querySelector('.notification-progress');
            if (progressBar) {
                progressBar.style.animation = `progress ${duration}ms linear`;
            }
        }, 100);

        const autoRemove = setTimeout(() => {
            if (notification.parentNode) {
                notification.classList.remove('show');
                setTimeout(() => {
                    if (notification.parentNode) {
                        notification.parentNode.removeChild(notification);
                    }
                }, 300);
            }
        }, duration);

        notification.querySelector('.notification-close').addEventListener('click', () => {
            clearTimeout(autoRemove);
            if (notification.parentNode) {
                notification.classList.remove('show');
                setTimeout(() => {
                    if (notification.parentNode) {
                        notification.parentNode.removeChild(notification);
                    }
                }, 300);
            }
        });
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

    function setupNumericInput(input, maxLength) {
        if (!input) return;
        input.addEventListener('input', () => {
            const digits = input.value.replace(/\D/g, '');
            input.value = maxLength ? digits.slice(0, maxLength) : digits;
        });
        input.addEventListener('keydown', (e) => {
            if (['e', 'E', '+', '-', '.'].includes(e.key)) {
                e.preventDefault();
            }
        });
    }

    setupNumericInput(dniInput, 8);
    setupNumericInput(telefonoInput, 15);

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

    async function validateStep(step) {
        console.log("🔍 Validando paso:", step + 1);
        
        const stepNode = formSteps[step];
        if (!stepNode) return true;
    
        // Limpiar errores previos del paso actual
        clearStepErrors(step);
        
        const inputs = stepNode.querySelectorAll('input, select, textarea');
        let isValid = true;
        let firstInvalidInput = null;
    
        // Validaciones síncronas primero
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
    
        // Si hay errores síncronos, no continuar
        if (!isValid) {
            if (firstInvalidInput) {
                firstInvalidInput.focus();
                firstInvalidInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
            return false;
        }
    
        // ✅ Validación específica para cada paso (incluyendo las asíncronas)
        let stepValidationResult = true;
        switch (step) {
            case 0: // Paso 1: Datos Personales
                stepValidationResult = await validatePersonalData();
                break;
            case 1: // Paso 2: Domicilio
                stepValidationResult = await validateLocation();
                break;
            case 2: // Paso 3: Datos Académicos
                stepValidationResult = validateAcademicData();
                break;
            case 3: // Paso 4: Cuenta y Confirmación
                stepValidationResult = validateAccountData();
                break;
        }
    
        // Hacer scroll al primer error si hay problemas
        if (!stepValidationResult && firstInvalidInput) {
            firstInvalidInput.focus();
            firstInvalidInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    
        return stepValidationResult;
    }

    // ✅ Función para verificar si el DNI ya existe (con país)
    function verificarDNIExistente(dni, paisCodigo) {
        return new Promise((resolve, reject) => {
            if (!dni || dni.length < 7 || !paisCodigo) {
                resolve({ existe: false, mensaje: '' });
                return;
            }

            // Timeout de 10 segundos
            const timeoutPromise = new Promise((_, reject) => 
                setTimeout(() => reject(new Error('Timeout verificando DNI')), 10000)
            );

            const params = new URLSearchParams({
                dni: dni,
                paisCodigo: paisCodigo
            });

            const fetchPromise = fetch(`/api/usuarios/verificar-dni-pais?${params.toString()}`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            })
            .then(response => {
                console.log("📡 Respuesta DNI+País - Status:", response.status);
                if (!response.ok) {
                    throw new Error(`Error del servidor: ${response.status}`);
                }
                const contentType = response.headers.get('content-type') || '';
                if (!contentType.includes('application/json')) {
                    throw new Error('Respuesta no JSON');
                }
                return response.json();
            })
            .then(data => {
                console.log("📊 Datos DNI+País recibidos:", data);
                return data;
            });

            Promise.race([fetchPromise, timeoutPromise])
                .then(resolve)
                .catch(error => {
                    console.error('❌ Error verificando DNI:', error);
                    reject(error);
                });
        });
    }

    function verificarEmailExistente(email) {
        return new Promise((resolve, reject) => {
            if (!email || !email.includes('@')) {
                resolve(false);
                return;
            }
    
            // Timeout de 10 segundos
            const timeoutPromise = new Promise((_, reject) => 
                setTimeout(() => reject(new Error('Timeout verificando email')), 10000)
            );
    
            const fetchPromise = fetch(`/api/usuarios/verificar-email?email=${encodeURIComponent(email)}`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            })
            .then(response => {
                console.log("📡 Respuesta email - Status:", response.status);
                if (!response.ok) {
                    throw new Error(`Error del servidor: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log("📊 Datos email recibidos:", data);
                return data.existe;
            });
    
            Promise.race([fetchPromise, timeoutPromise])
                .then(resolve)
                .catch(error => {
                    console.error('❌ Error verificando email:', error);
                    reject(error);
                });
        });
    }


    async function validatePersonalData() {
        return new Promise(async (resolve) => {
            let isValid = true;
            const dni = document.getElementById('dni');
            const telefono = document.getElementById('telefono');
            const email = document.getElementById('email');
            const fechaNacimiento = document.getElementById('fechaNacimiento');
            const nombre = document.getElementById('nombre');
            const apellido = document.getElementById('apellido');
    
            // ✅ Validar fecha de nacimiento (input tipo date)
            if (fechaNacimiento.value) {
                // El input type="date" ya viene en formato YYYY-MM-DD, perfecto para el backend
                const fechaValue = fechaNacimiento.value;
                
                // Validar que la fecha no sea futura
                const fechaSeleccionada = new Date(fechaValue);
                const hoy = new Date();
                
                if (fechaSeleccionada > hoy) {
                    showFieldError(fechaNacimiento, 'La fecha de nacimiento no puede ser futura');
                    isValid = false;
                } else {
                    // ✅ Crear campo hidden con el formato correcto para el backend (ya está en YYYY-MM-DD)
                    let hiddenFecha = document.getElementById('fechaNacimientoBackend');
                    if (!hiddenFecha) {
                        hiddenFecha = document.createElement('input');
                        hiddenFecha.type = 'hidden';
                        hiddenFecha.id = 'fechaNacimientoBackend';
                        hiddenFecha.name = 'fechaNacimiento';
                        fechaNacimiento.parentNode.appendChild(hiddenFecha);
                    }
                    hiddenFecha.value = fechaValue; // Ya está en formato YYYY-MM-DD
                    
                    // ✅ Validar edad mínima (16 años)
                    const edad = calcularEdad(fechaSeleccionada, hoy);
                    
                    if (edad < 16) {
                        showFieldError(fechaNacimiento, 'Debes tener al menos 16 años para registrarte');
                        isValid = false;
                    } else if (edad > 100) {
                        showFieldError(fechaNacimiento, 'Por favor verifica la fecha, parece ser demasiado antigua');
                        isValid = false;
                    } else {
                        hideFieldError(fechaNacimiento);
                        console.log(`✅ Edad válida: ${edad} años`);
                    }
                }
            } else {
                showFieldError(fechaNacimiento, 'La fecha de nacimiento es obligatoria');
                isValid = false;
            }
    
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
            } else {
                hideFieldError(dni);
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
            } else {
                // ✅ VERIFICAR SI EL EMAIL YA EXISTE (ASÍNCRONO)
                try {
                    const emailExiste = await verificarEmailExistente(email.value);
                    if (emailExiste) {
                        showFieldError(email, 'Este correo electrónico ya está registrado en el sistema');
                        isValid = false;
                    } else {
                        hideFieldError(email);
                    }
                } catch (error) {
                    console.error('Error verificando email:', error);
                    // En caso de error, permitir continuar pero mostrar advertencia
                    showFieldError(email, 'Error verificando email. Intenta nuevamente.');
                    isValid = false;
                }
            }
    
            console.log("✅ Validación personal data completada, resultado:", isValid);
            resolve(isValid);
        });
    }
    
    async function validateLocation() {
        let isValid = true;
        const paisSelect = document.getElementById('pais');
        const provinciaSelect = document.getElementById('provincia');
        const ciudadSelect = document.getElementById('ciudad');
        const paisCodigo = document.getElementById('paisCodigo').value;
        const provinciaCodigo = document.getElementById('provinciaCodigo').value;
        const ciudadId = document.getElementById('ciudadId').value;
        const dni = document.getElementById('dni');
        
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
        
        // ✅ Verificar DNI existente en el país seleccionado (paso 2)
        if (isValid && dni && paisCodigo) {
            if (!/^\d{7,8}$/.test(dni.value)) {
                showFieldError(dni, 'El DNI debe tener 7 u 8 dígitos numéricos');
                isValid = false;
            } else {
                try {
                    const data = await verificarDNIExistente(dni.value, paisCodigo);
                    if (data && data.existe) {
                        const mensaje = data.mensaje || 'Ya existe una cuenta con este DNI en el pais seleccionado';
                        showFieldError(dni, 'Este DNI ya está registrado en el sistema');
                        showNotification(mensaje, 'error');
                        currentStep = 0;
                        showStep(currentStep);
                        dni.focus();
                        dni.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        isValid = false;
                    } else {
                        hideFieldError(dni);
                    }
                } catch (error) {
                    console.error('Error verificando DNI:', error);
                    showFieldError(dni, 'Error verificando DNI. Intenta nuevamente.');
                    isValid = false;
                }
            }
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

        // Coherencia: al menos 10 años entre nacimiento y egreso
        const fechaNacimientoInput = document.getElementById('fechaNacimientoBackend') || document.getElementById('fechaNacimiento');
        if (fechaNacimientoInput && fechaNacimientoInput.value) {
            const nacimientoYear = new Date(fechaNacimientoInput.value).getFullYear();
            if (!isNaN(nacimientoYear) && !isNaN(año)) {
                const diff = año - nacimientoYear;
                if (diff < 10) {
                    showFieldError(añoEgreso, 'El año de egreso debe ser al menos 10 años después de la fecha de nacimiento');
                    isValid = false;
                }
            }
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

        // Validar contraseña (mínimo 8 caracteres, al menos 1 mayúscula y 1 carácter especial)
        // Regex: Al menos una mayúscula, al menos un carácter especial (no letra ni número), longitud 8+
        if (!/^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$/.test(password.value)) {
            showFieldError(password, 'La contraseña debe tener al menos 8 caracteres, una mayúscula y un carácter especial');
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

    // ===== Feedback en vivo de contraseña =====
    const pwdField = document.getElementById('password');
    const pwdConfirmField = document.getElementById('confirmPassword');
    const ruleLength = document.getElementById('pwd-length');
    const ruleUpper = document.getElementById('pwd-upper');
    const ruleSpecial = document.getElementById('pwd-special');
    const confirmFeedback = document.getElementById('confirm-feedback');

    function setRuleState(el, ok) {
        if (!el) return;
        el.style.color = ok ? '#15803d' : '#ef4444';
        el.style.fontWeight = ok ? '600' : '400';
    }

    function updatePasswordFeedback() {
        if (!pwdField) return;
        const val = pwdField.value || '';
        const hasLength = val.length >= 8;
        const hasUpper = /[A-Z]/.test(val);
        const hasSpecial = /[^a-zA-Z0-9]/.test(val);

        setRuleState(ruleLength, hasLength);
        setRuleState(ruleUpper, hasUpper);
        setRuleState(ruleSpecial, hasSpecial);

        if (confirmFeedback && pwdConfirmField) {
            if (!pwdConfirmField.value) {
                confirmFeedback.textContent = '';
            } else if (pwdConfirmField.value === val) {
                confirmFeedback.style.color = '#15803d';
                confirmFeedback.textContent = 'Las contraseñas coinciden';
            } else {
                confirmFeedback.style.color = '#ef4444';
                confirmFeedback.textContent = 'Las contraseñas no coinciden';
            }
        }
    }

    if (pwdField) {
        pwdField.addEventListener('input', updatePasswordFeedback);
    }
    if (pwdConfirmField) {
        pwdConfirmField.addEventListener('input', updatePasswordFeedback);
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

    // Botón cancelar: preguntar con modal global antes de salir
    const cancelBtn = document.getElementById('cancelBtn');
    if (cancelBtn) {
        cancelBtn.addEventListener('click', function () {
            if (typeof ModalConfirmacion !== 'undefined' && ModalConfirmacion && ModalConfirmacion.show) {
                ModalConfirmacion.show('Confirmar cancelación', '¿Estás seguro de cancelar el registro? No se creará ninguna cuenta.', function () {
                    // Redirigir al inicio o página pública
                    window.location.href = '/';
                });
            } else {
                // Fallback simple
                if (confirm('¿Estás seguro de cancelar el registro? No se creará ninguna cuenta.')) {
                    window.location.href = '/';
                }
            }
        });
    }

    nextBtn.addEventListener("click", async function () {
        console.log("🔄 Validando paso antes de avanzar...");
        
        // Deshabilitar el botón temporalmente para evitar múltiples clics
        nextBtn.disabled = true;
        nextBtn.textContent = "Validando...";
        
        try {
            const isValid = await validateStep(currentStep);
            console.log("✅ Resultado validación:", isValid);
            
            if (isValid) {
                // ✅ Si es el paso 2 (domicilio), guardar ubicaciones antes de avanzar
                if (currentStep === 1) { // Paso 2 es índice 1
                    console.log("💾 Guardando ubicaciones antes de avanzar...");
                    const success = await guardarUbicaciones();
                    if (success) {
                        currentStep++;
                        showStep(currentStep);
                    } else {
                        alert('Error al guardar la ubicación. Intenta nuevamente.');
                    }
                } else {
                    currentStep++;
                    showStep(currentStep);
                }
            } else {
                console.log("❌ Validación fallida, no se avanza");
                const stepNode = formSteps[currentStep];
                const firstError = stepNode.querySelector('.input-error');
                if (firstError) {
                    firstError.focus();
                    firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }
        } catch (error) {
            console.error('❌ Error durante la validación:', error);
            alert('Error durante la validación. Intenta nuevamente.');
        } finally {
            // Rehabilitar el botón
            nextBtn.disabled = false;
            nextBtn.textContent = "Siguiente";
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

            const csrfInput = document.querySelector('input[name="_csrf"]');
            const csrfToken = csrfInput ? csrfInput.value : null;

            const formData = new FormData();
            if (csrfToken) {
                formData.append('_csrf', csrfToken);
            }
            formData.append('paisCodigo', paisCodigo);
            formData.append('provinciaCodigo', provinciaCodigo);
            formData.append('ciudadId', ciudadId);

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
    

    // Manejar envío del formulario
    const form = document.getElementById('registerForm');
    if (form) {
        form.addEventListener('submit', async function (e) {
            console.log("📤 Formulario enviándose...");
            
            // Prevenir envío por defecto para validar asíncronamente
            e.preventDefault();
            
            if (currentStep !== formSteps.length - 1) {
                console.log("❌ No es el último paso, previniendo envío");
                return false;
            }
            
            const isValid = await validateStep(formSteps.length - 1);
            
            if (!isValid) {
                console.log("❌ Validación falló, previniendo envío");
                return false;
            }
            
            console.log("✅ Formulario válido, enviando...");
            submitBtn.disabled = true;
            submitBtn.textContent = "Registrando...";
            
            // Enviar formulario manualmente una vez validado
            form.submit();
        });
    }

    // Inicializar
    showStep(currentStep);
    console.log("✅ Aplicación inicializada correctamente");
});
