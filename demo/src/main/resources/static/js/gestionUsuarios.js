document.addEventListener('DOMContentLoaded', function () {
    // Referencias a elementos principales
    const btnShowForm = document.getElementById('btn-show-form');
    const formContainer = document.getElementById('form-container');
    const btnCloseForm = document.getElementById('btn-close-form');
    const btnCancelForm = document.getElementById('btn-cancel-form');
    const rolSelect = document.getElementById('rol-select');
    const fieldsDocente = document.getElementById('fields-docente');

    // Elementos para upload de foto
    const fotoInput = document.getElementById('foto');
    const fotoPreview = document.getElementById('foto-preview');
    const uploadPlaceholder = document.querySelector('.upload-placeholder');

    // Elementos para horarios de docente
    const btnAddHorarioDocente = document.getElementById('btn-add-horario-docente');
    const horariosDocenteTableBody = document.querySelector('#horarios-docente-table tbody');
    const diaSelect = document.getElementById('dia-semana-docente');
    const horaDesdeInput = document.getElementById('hora-desde-docente');
    const horaHastaInput = document.getElementById('hora-hasta-docente');

    // Elementos para filtros
    const searchInput = document.getElementById('search-input');
    const filtroRol = document.getElementById('filtro-rol');
    const filtroEstado = document.getElementById('filtro-estado');
    const filtroGenero = document.getElementById('filtro-genero');
    const btnApplyFilters = document.getElementById('btn-apply-filters');
    const btnClearFilters = document.getElementById('btn-clear-filters');

    const paisSelect = document.getElementById('pais');
    const provinciaSelect = document.getElementById('provincia');
    const ciudadSelect = document.getElementById('ciudad');

    // Variables para roles seleccionados
    let selectedRoles = [];
    
    // ✅ MOVER ESTAS REFERENCIAS A VARIABLES GLOBALES
    let selectedRolesContainer;
    let selectedChipsContainer;
    
    // ✅ MOVER roleIcons A GLOBAL
    const roleIcons = {
        ALUMNO: 'fas fa-user-graduate',
        DOCENTE: 'fas fa-chalkboard-teacher',
        ADMIN: 'fas fa-user-shield',
        COORDINADOR: 'fas fa-user-tie'
    };

    let currentPage = 1;
    let totalPages = 1;
    let pageSize = 10;

    // Inicialización
    initializeFormHandlers();
    initializeFotoUpload();
    initializeRoles();
    initializeHorariosDocente();
    initializeFilters();
    initializeTable();

    function initializeLocationSystem() {
        console.log("📍 Inicializando sistema de ubicación...");
        
        if (!paisSelect) {
            console.error("❌ No se encontró el select de país");
            return;
        }
    
        // Configurar listener para país
        paisSelect.addEventListener('change', function(e) {
            const select = e.target;
            const selectedOption = select.options[select.selectedIndex];
            const hiddenCodigo = document.getElementById('paisCodigo');
            
            console.log("País seleccionado:", select.value);
            console.log("Código del país:", selectedOption.getAttribute('data-codigo'));
            
            if (selectedOption.value && selectedOption.getAttribute('data-codigo')) {
                const countryCode = selectedOption.getAttribute('data-codigo');
                hiddenCodigo.value = countryCode;
                console.log("✅ País seleccionado - Código:", countryCode);
                
                cargarProvinciasAdmin(countryCode);
            } else {
                hiddenCodigo.value = '';
                provinciaSelect.disabled = true;
                provinciaSelect.innerHTML = '<option value="">Primero selecciona un país</option>';
                ciudadSelect.disabled = true;
                ciudadSelect.innerHTML = '<option value="">Primero selecciona una provincia</option>';
            }
        });
    
        // Configurar listener para provincia
        provinciaSelect.addEventListener('change', function(e) {
            const select = e.target;
            const selectedOption = select.options[select.selectedIndex];
            const hiddenCodigo = document.getElementById('provinciaCodigo');
            
            console.log("Provincia seleccionada:", select.value);
            console.log("Código de provincia:", selectedOption.getAttribute('data-code'));
            
            if (selectedOption.value && selectedOption.getAttribute('data-code')) {
                const provinceCode = selectedOption.getAttribute('data-code');
                hiddenCodigo.value = provinceCode;
                console.log("✅ Provincia seleccionada - Código:", provinceCode);
                
                const countryCode = document.getElementById('paisCodigo').value;
                cargarCiudadesAdmin(countryCode, provinceCode);
            } else {
                hiddenCodigo.value = '';
                ciudadSelect.disabled = true;
                ciudadSelect.innerHTML = '<option value="">Primero selecciona una provincia</option>';
            }
        });
    
        // Configurar listener para ciudad
        ciudadSelect.addEventListener('change', function(e) {
            const select = e.target;
            const selectedOption = select.options[select.selectedIndex];
            const hiddenId = document.getElementById('ciudadId');
            
            console.log("Ciudad seleccionada:", select.value);
            
            if (selectedOption.value && selectedOption.getAttribute('data-id')) {
                const cityId = selectedOption.getAttribute('data-id');
                hiddenId.value = cityId;
                console.log("✅ Ciudad seleccionada - ID:", cityId);
            } else {
                hiddenId.value = '';
            }
        });
    
        console.log("✅ Sistema de ubicación configurado");
    }
    
    // Funciones para cargar provincias y ciudades (versión admin)
    function cargarProvinciasAdmin(paisCode) {
        console.log("🌍 Cargando provincias para país:", paisCode);
        
        provinciaSelect.innerHTML = '<option value="">Cargando provincias...</option>';
        provinciaSelect.disabled = true;
        
        fetch(`/api/ubicaciones/provincias/${paisCode}`)
            .then(response => {
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
                ciudadSelect.innerHTML = '<option value="">Primero selecciona una provincia</option>';
                ciudadSelect.disabled = true;
                document.getElementById('ciudadId').value = '';
            })
            .catch(error => {
                console.error('❌ Error cargando provincias:', error);
                provinciaSelect.innerHTML = '<option value="">Error al cargar provincias</option>';
            });
    }
    
    function cargarCiudadesAdmin(paisCode, provinciaCode) {
        console.log("🏙️ Cargando ciudades para país:", paisCode, "provincia:", provinciaCode);
        
        ciudadSelect.innerHTML = '<option value="">Cargando ciudades...</option>';
        ciudadSelect.disabled = true;
        
        fetch(`/api/ubicaciones/ciudades/${paisCode}/${provinciaCode}`)
            .then(response => {
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
            });
    }
    
    // Función para validar la ubicación en el formulario
    function validateLocation() {
        let isValid = true;
        const paisCodigo = document.getElementById('paisCodigo').value;
        const provinciaCodigo = document.getElementById('provinciaCodigo').value;
        const ciudadId = document.getElementById('ciudadId').value;
        
        if (!paisCodigo) {
            showFieldError(paisSelect, 'Por favor, selecciona un país');
            isValid = false;
        } else {
            hideFieldError(paisSelect);
        }
        
        if (!provinciaCodigo) {
            showFieldError(provinciaSelect, 'Por favor, selecciona una provincia');
            isValid = false;
        } else {
            hideFieldError(provinciaSelect);
        }
        
        if (!ciudadId) {
            showFieldError(ciudadSelect, 'Por favor, selecciona una ciudad');
            isValid = false;
        } else {
            hideFieldError(ciudadSelect);
        }
        
        return isValid;
    }
    
    // Función para mostrar/ocultar errores en campos
    function showFieldError(input, message) {
        hideFieldError(input);
        input.classList.add('error');
        
        const errorElement = document.createElement('span');
        errorElement.className = 'error-message';
        errorElement.textContent = message;
        errorElement.id = `${input.id}-error`;
        input.parentNode.appendChild(errorElement);
        
        input.focus();
    }
    
    function hideFieldError(input) {
        input.classList.remove('error');
        const existingError = document.getElementById(`${input.id}-error`);
        if (existingError) {
            existingError.remove();
        }
    }


    // Mostrar/ocultar formulario
    function initializeFormHandlers() {
        
        if (btnShowForm) {
            btnShowForm.addEventListener('click', function() {
                showForm();
            });
        }

        if (btnCloseForm) {
            btnCloseForm.addEventListener('click', function() {
                hideForm();
            });
        }

        if (btnCancelForm) {
            btnCancelForm.addEventListener('click', function() {
                hideForm();
            });
        }
    }

    function showForm() {
        formContainer.style.display = 'block';
        setTimeout(() => {
            formContainer.classList.add('show');
            initializeLocationSystem();
            initializeDateMask();
        }, 10);
    }

    function hideForm() {
        formContainer.classList.remove('show');
        setTimeout(() => {
            formContainer.style.display = 'none';
        }, 500);
        resetForm();
    }

    function resetForm() {
        document.getElementById('user-form').reset();
        resetFotoUpload();
        clearHorariosDocenteTable();
        hideDocenteFields();
        hideAlumnoFields();
        resetRoles();
        resetLocation();
        resetFechaNacimiento();
        removeAllSpecificRequired();
        
        clearFieldErrors();
        
        const contador = document.getElementById('contador-horarios');
        if (contador) {
            contador.style.display = 'none';
        }
    }

    function resetFechaNacimiento() {
        const fechaBackendField = document.getElementById('fechaNacimientoBackend');
        if (fechaBackendField) {
            fechaBackendField.value = '';
        }
    }
    
    function resetLocation() {
        if (provinciaSelect) {
            provinciaSelect.disabled = true;
            provinciaSelect.innerHTML = '<option value="">Primero selecciona un país</option>';
        }
        if (ciudadSelect) {
            ciudadSelect.disabled = true;
            ciudadSelect.innerHTML = '<option value="">Primero selecciona una provincia</option>';
        }
        // Limpiar hidden fields
        document.getElementById('paisCodigo').value = '';
        document.getElementById('provinciaCodigo').value = '';
        document.getElementById('ciudadId').value = '';
    }

    function initializeDateMask() {
        const fechaInput = document.getElementById('fechaNacimiento');
        
        if (fechaInput) {
            // Aplicar máscara para formato DD/MM/AAAA
            fechaInput.addEventListener('input', function(e) {
                let value = e.target.value.replace(/\D/g, '');
                
                // Aplicar formato DD/MM/AAAA
                if (value.length > 2 && value.length <= 4) {
                    value = value.substring(0, 2) + '/' + value.substring(2);
                } else if (value.length > 4) {
                    value = value.substring(0, 2) + '/' + value.substring(2, 4) + '/' + value.substring(4, 8);
                }
                
                e.target.value = value;
            });
    
            // Validar fecha al perder foco
            fechaInput.addEventListener('blur', function(e) {
                const value = e.target.value;
                if (value && !isValidDate(value)) {
                    showFieldError(fechaInput, 'Fecha inválida. Usa el formato DD/MM/AAAA');
                } else {
                    hideFieldError(fechaInput);
                    // Actualizar campo hidden para backend
                    updateBackendDateField(value);
                }
            });
    
            // También validar al cambiar
            fechaInput.addEventListener('change', function(e) {
                const value = e.target.value;
                if (value && isValidDate(value)) {
                    updateBackendDateField(value);
                }
            });
        }
    }
    function isValidDate(dateString) {
        if (!dateString) return false;
        
        const pattern = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/;
        const match = dateString.match(pattern);
        
        if (!match) return false;
        
        const dia = parseInt(match[1], 10);
        const mes = parseInt(match[2], 10);
        const año = parseInt(match[3], 10);
        
        // Validar rangos básicos
        if (mes < 1 || mes > 12) return false;
        if (dia < 1 || dia > 31) return false;
        
        // Validar febrero y meses con 30 días
        const fecha = new Date(año, mes - 1, dia);
        return fecha.getDate() === dia && 
               fecha.getMonth() === mes - 1 && 
               fecha.getFullYear() === año;
    }

    
    
    // ✅ Función para convertir DD/MM/AAAA a yyyy-MM-dd (formato backend)
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
    
    // ✅ Función para actualizar el campo hidden del backend
    function updateBackendDateField(fechaDDMMAAAA) {
        const backendField = document.getElementById('fechaNacimientoBackend');
        if (backendField && fechaDDMMAAAA) {
            const fechaBackend = convertirFechaParaBackend(fechaDDMMAAAA);
            backendField.value = fechaBackend;
            console.log("📅 Fecha para backend:", fechaBackend);
        }
    }
    
    // ✅ Función para validar la fecha en el formulario
    function validateFechaNacimiento() {
        const fechaInput = document.getElementById('fechaNacimiento');
        let isValid = true;
    
        if (!fechaInput.value.trim()) {
            showFieldError(fechaInput, 'La fecha de nacimiento es obligatoria');
            isValid = false;
        } else if (!isValidDate(fechaInput.value)) {
            showFieldError(fechaInput, 'Fecha inválida. Usa el formato DD/MM/AAAA');
            isValid = false;
        } else {
            // ✅ Validar edad mínima (16 años)
            const partes = fechaInput.value.split('/');
            const dia = parseInt(partes[0], 10);
            const mes = parseInt(partes[1], 10);
            const año = parseInt(partes[2], 10);
            const fechaNac = new Date(año, mes - 1, dia);
            const hoy = new Date();
            let edad = hoy.getFullYear() - fechaNac.getFullYear();
            const mesActual = hoy.getMonth();
            const diaActual = hoy.getDate();
            
            if (mesActual < (mes - 1) || (mesActual === (mes - 1) && diaActual < dia)) {
                edad--;
            }
            
            if (edad < 16) {
                showFieldError(fechaInput, 'El usuario debe tener al menos 16 años');
                isValid = false;
            } else {
                hideFieldError(fechaInput);
                // Asegurar que el campo hidden esté actualizado
                updateBackendDateField(fechaInput.value);
            }
        }
    
        return isValid;
    }

    // Manejo del upload de foto
    function initializeFotoUpload() {
        if (fotoInput) {
            fotoInput.addEventListener('change', function(event) {
                const file = event.target.files[0];
                if (file) {
                    if (file.size > 2 * 1024 * 1024) { // 2MB
                        alert('El archivo es demasiado grande. Máximo 2MB.');
                        return;
                    }
                    
                    const reader = new FileReader();
                    reader.onload = function(e) {
                        fotoPreview.src = e.target.result;
                        fotoPreview.style.display = 'block';
                        if (uploadPlaceholder) {
                            uploadPlaceholder.style.display = 'none';
                        }
                    };
                    reader.readAsDataURL(file);
                }
            });
        }
    }

    function resetFotoUpload() {
        if (fotoPreview && uploadPlaceholder) {
            fotoPreview.style.display = 'none';
            uploadPlaceholder.style.display = 'flex';
            fotoPreview.src = '';
        }
    }

    function updateSelectedRoles() {
        if (selectedChipsContainer) {
            selectedChipsContainer.innerHTML = '';
            
            // ✅ SOLO MOSTRAR UN ROL
            if (selectedRoles.length > 0) {
                const role = selectedRoles[0];
                const chip = document.createElement('div');
                chip.className = 'category-chip';
                chip.setAttribute('data-role', role.toLowerCase());
                
                const icon = roleIcons[role] || 'fas fa-user';
                chip.innerHTML = `
                    <i class="${icon}"></i>
                    <span>${getRoleDisplayName(role)}</span>
                    <i class="fas fa-times chip-remove" onclick="removeSelectedRole()"></i>
                `;
                selectedChipsContainer.appendChild(chip);
                
                selectedRolesContainer.classList.add('show');
            } else {
                selectedRolesContainer.classList.remove('show');
            }
        }
    }

    function getRoleDisplayName(role) {
        const roleNames = {
            ALUMNO: 'Alumno',
            DOCENTE: 'Docente',
            ADMIN: 'Administrador',
            COORDINADOR: 'Coordinador'
        };
        return roleNames[role] || role;
    }
    
    function resetRoles() {
        selectedRoles = [];
        if (rolSelect) rolSelect.value = '';
        updateSelectedRoles(); // ← AHORA SÍ ESTÁ DEFINIDA
        hideAllSpecificFields();
        removeAllSpecificRequired();
    }

    function initializeRoles() {
        selectedRolesContainer = document.getElementById('selected-roles');
        selectedChipsContainer = document.getElementById('selected-roles-chips');
    
        if (rolSelect) {
            rolSelect.addEventListener('change', function() {
                const selectedValue = this.value;
                
                if (selectedValue) {
                    selectedRoles = [selectedValue];
                    updateSelectedRoles(); // ← AHORA ESTÁ DISPONIBLE
                    
                    showDocenteFields(selectedValue === 'DOCENTE');
                    showAlumnoFields(selectedValue === 'ALUMNO');
                    manageRequiredFields(selectedValue);
                } else {
                    selectedRoles = [];
                    updateSelectedRoles(); // ← AHORA ESTÁ DISPONIBLE
                    hideAllSpecificFields();
                    removeAllSpecificRequired();
                }
            });
        }
        
        // ✅ NUEVA: Función para manejar campos requeridos según el rol
        function manageRequiredFields(rol) {
            // Primero quitar todos los requeridos específicos
            removeAllSpecificRequired();
            
            // Luego agregar requeridos según el rol
            switch(rol.toUpperCase()) {
                case 'ALUMNO':
                    addAlumnoRequired();
                    break;
                case 'DOCENTE':
                    // Los campos de docente no son requeridos por ahora
                    break;
                case 'ADMIN':
                case 'COORDINADOR':
                    // No hay campos específicos requeridos
                    break;
            }
        }
    
        // ✅ NUEVA: Agregar requeridos para alumno
        function addAlumnoRequired() {
            const colegioEgreso = document.getElementById('colegioEgreso');
            const añoEgreso = document.getElementById('añoEgreso');
            const ultimosEstudios = document.getElementById('ultimosEstudios');
            
            if (colegioEgreso) {
                colegioEgreso.setAttribute('required', 'required');
                colegioEgreso.setAttribute('aria-required', 'true');
            }
            if (añoEgreso) {
                añoEgreso.setAttribute('required', 'required');
                añoEgreso.setAttribute('aria-required', 'true');
            }
            if (ultimosEstudios) {
                ultimosEstudios.setAttribute('required', 'required');
                ultimosEstudios.setAttribute('aria-required', 'true');
            }
        }
    
        // ✅ MODIFICADA: Función para remover el rol seleccionado
        window.removeSelectedRole = function() {
            selectedRoles = [];
            rolSelect.value = ''; // Resetear el select
            updateSelectedRoles();
            hideAllSpecificFields();
            removeAllSpecificRequired(); // ✅ Quitar requeridos al remover rol
        };
    
        // ✅ NUEVA: Función para obtener el rol seleccionado
        window.getSelectedRole = function() {
            return selectedRoles.length > 0 ? selectedRoles[0] : null;
        };
    }
    
        // ========== FUNCIONES GLOBALES (FUERA DE initializeRoles) ==========
    
        // ✅ Función para quitar requeridos de todos los campos específicos
        function removeAllSpecificRequired() {
            // Campos de alumno
            const colegioEgreso = document.getElementById('colegioEgreso');
            const añoEgreso = document.getElementById('añoEgreso');
            const ultimosEstudios = document.getElementById('ultimosEstudios');
            
            if (colegioEgreso) {
                colegioEgreso.removeAttribute('required');
                colegioEgreso.removeAttribute('aria-required');
            }
            if (añoEgreso) {
                añoEgreso.removeAttribute('required');
                añoEgreso.removeAttribute('aria-required');
            }
            if (ultimosEstudios) {
                ultimosEstudios.removeAttribute('required');
                ultimosEstudios.removeAttribute('aria-required');
            }
            
            // Campos de docente
            const matricula = document.getElementById('matricula');
            const experiencia = document.getElementById('experiencia');
            
            if (matricula) matricula.removeAttribute('required');
            if (experiencia) experiencia.removeAttribute('required');
        }
    
        // ✅ Función para mostrar campos de docente
        function showDocenteFields(show) {
            if (fieldsDocente) {
                fieldsDocente.style.display = show ? 'block' : 'none';
            }
        }
    
        // ✅ Función para ocultar campos de docente
        function hideDocenteFields() {
            if (fieldsDocente) {
                fieldsDocente.style.display = 'none';
            }
        }
    
        // ✅ Función para mostrar campos de alumno  
        function showAlumnoFields(show) {
            const fieldsAlumno = document.getElementById('fields-alumno');
            if (fieldsAlumno) {
                fieldsAlumno.style.display = show ? 'block' : 'none';
            }
        }
    
        // ✅ Función para ocultar campos de alumno
        function hideAlumnoFields() {
            const fieldsAlumno = document.getElementById('fields-alumno');
            if (fieldsAlumno) {
                fieldsAlumno.style.display = 'none';
            }
        }
    
        // ✅ Función para ocultar todos los campos específicos
        function hideAllSpecificFields() {
            hideDocenteFields();
            hideAlumnoFields();
        }
    
        // ✅ Función para resetear roles
        function resetRoles() {
            selectedRoles = [];
            if (rolSelect) rolSelect.value = '';
            updateSelectedRoles();
            hideAllSpecificFields();
            removeAllSpecificRequired();
        }
    
        // ========== CONTINUACIÓN DEL CÓDIGO ==========
    
        // Manejo de horarios para docente
        function initializeHorariosDocente() {
            if (btnAddHorarioDocente) {
                btnAddHorarioDocente.addEventListener('click', function() {
                    addHorarioDocente();
                });
            }
    
            if (horariosDocenteTableBody) {
                horariosDocenteTableBody.addEventListener('click', function(e) {
                    if (e.target.classList.contains('btn-delete-horario') || 
                        e.target.parentElement.classList.contains('btn-delete-horario')) {
                        const row = e.target.closest('tr');
                        if (row) {
                            row.remove();
                            actualizarContadorHorarios(); // ✅ Actualizar contador al eliminar
                        }
                    }
                });
            }
        }
    
        
        // ✅ FUNCIÓN addHorarioDocente MEJORADA
        function addHorarioDocente() {
            const dia = diaSelect.value;
            const horaDesde = horaDesdeInput.value;
            const horaHasta = horaHastaInput.value;
    
            if (!dia || !horaDesde || !horaHasta) {
                showNotification('Por favor, complete todos los campos de horario', 'error');
                return;
            }
    
            if (horaDesde >= horaHasta) {
                showNotification('La hora de inicio debe ser anterior a la hora de fin', 'error');
                return;
            }
    
            // ✅ VERIFICAR HORARIOS SOLAPADOS
            if (existeHorarioSolapado(dia, horaDesde, horaHasta)) {
                showNotification('Ya existe un horario para este día en el mismo rango de horas', 'error');
                return;
            }
    
            const newRow = document.createElement('tr');
            newRow.innerHTML = `
                <td>${dia}</td>
                <td>${horaDesde} - ${horaHasta}</td>
                <td class="actions">
                    <button type="button" class="btn-icon btn-delete btn-delete-horario" title="Eliminar">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            horariosDocenteTableBody.appendChild(newRow);
    
            // Limpiar campos
            diaSelect.value = '';
            horaDesdeInput.value = '';
            horaHastaInput.value = '';
            
            // ✅ ACTUALIZAR CONTADOR
            actualizarContadorHorarios();
        }
    
        // ✅ NUEVA: Función para verificar horarios solapados
        function existeHorarioSolapado(dia, nuevaHoraDesde, nuevaHoraHasta) {
            const filas = horariosDocenteTableBody.querySelectorAll('tr');
            
            for (let fila of filas) {
                const diaExistente = fila.cells[0].textContent;
                const horarioExistente = fila.cells[1].textContent;
                const [horaDesdeExistente, horaHastaExistente] = horarioExistente.split(' - ');
                
                if (diaExistente === dia) {
                    // Verificar solapamiento
                    if ((nuevaHoraDesde >= horaDesdeExistente && nuevaHoraDesde < horaHastaExistente) ||
                        (nuevaHoraHasta > horaDesdeExistente && nuevaHoraHasta <= horaHastaExistente) ||
                        (nuevaHoraDesde <= horaDesdeExistente && nuevaHoraHasta >= horaHastaExistente)) {
                        return true;
                    }
                }
            }
            
            return false;
        }
    
        // ✅ NUEVA: Función para actualizar contador de horarios
        function actualizarContadorHorarios() {
            const contador = document.getElementById('contador-horarios');
            const filas = horariosDocenteTableBody.querySelectorAll('tr').length;
            
            if (contador) {
                contador.textContent = `(${filas} horarios agregados)`;
                contador.style.display = filas > 0 ? 'inline-block' : 'none';
            }
        }
    
        function clearHorariosDocenteTable() {
            if (horariosDocenteTableBody) {
                horariosDocenteTableBody.innerHTML = '';
                actualizarContadorHorarios(); // ✅ Actualizar contador al limpiar
            }
        }
    
        // Filtros y búsqueda
        function initializeFilters() {
            if (searchInput) {
                searchInput.addEventListener('input', debounce(applyFilters, 300));
            }
    
            if (btnApplyFilters) {
                btnApplyFilters.addEventListener('click', applyFilters);
            }
    
            if (btnClearFilters) {
                btnClearFilters.addEventListener('click', clearFilters);
            }
        }
    
        function applyFilters() {
            const filters = {
                search: searchInput ? searchInput.value.toLowerCase() : '',
                rol: filtroRol ? filtroRol.value : '',
                estado: filtroEstado ? filtroEstado.value : '',
                genero: filtroGenero ? filtroGenero.value : ''
            };
    
            // Aplicar filtros a la tabla
            filterTable(filters);
        }
    
        function clearFilters() {
            if (searchInput) searchInput.value = '';
            if (filtroRol) filtroRol.value = '';
            if (filtroEstado) filtroEstado.value = '';
            if (filtroGenero) filtroGenero.value = '';
            
            // Mostrar todas las filas
            const table = document.getElementById('usuarios-table');
            if (table) {
                const rows = table.querySelectorAll('tbody tr');
                rows.forEach(row => {
                    row.style.display = '';
                });
                updateTableStats(rows.length);
            }
        }
    
        function filterTable(filters) {
            const table = document.getElementById('usuarios-table');
            if (!table) return;
    
            const rows = table.querySelectorAll('tbody tr');
            let visibleCount = 0;
    
            rows.forEach(row => {
                const cells = row.querySelectorAll('td');
                if (cells.length === 0) return;
    
                const nombre = cells[2]?.textContent?.toLowerCase() || '';
                const dni = cells[3]?.textContent?.toLowerCase() || '';
                const correo = cells[4]?.textContent?.toLowerCase() || '';
                const roles = cells[5]?.textContent || '';
                const estado = cells[6]?.textContent || '';
    
                let showRow = true;
    
                // Filtro de búsqueda
                if (filters.search) {
                    const searchMatch = nombre.includes(filters.search) ||
                                      dni.includes(filters.search) ||
                                      correo.includes(filters.search);
                    if (!searchMatch) showRow = false;
                }
    
                // Filtro de rol
                if (filters.rol && !roles.includes(filters.rol)) {
                    showRow = false;
                }
    
                // Filtro de estado
                if (filters.estado && !estado.includes(filters.estado)) {
                    showRow = false;
                }
    
                row.style.display = showRow ? '' : 'none';
                if (showRow) visibleCount++;
            });
    
            updateTableStats(visibleCount);
        }
    
        function updateTableStats(count) {
            const totalElement = document.getElementById('total-usuarios');
            if (totalElement) {
                totalElement.textContent = count;
            }
        }
    
        // Inicializar tabla
        function initializeTable() {
            const table = document.getElementById('usuarios-table');
            if (table) {
                table.addEventListener('click', function(e) {
                    if (e.target.classList.contains('btn-edit') || 
                        e.target.parentElement.classList.contains('btn-edit')) {
                        const row = e.target.closest('tr');
                        const id = row.cells[0].textContent;
                        const nombre = row.cells[2].textContent;
                        editUsuario(id, nombre);
                    } else if (e.target.classList.contains('btn-delete') || 
                              e.target.parentElement.classList.contains('btn-delete')) {
                        const row = e.target.closest('tr');
                        const id = row.cells[0].textContent;
                        const nombre = row.cells[2].textContent;
                        deleteUsuario(id, nombre);
                    } else if (e.target.classList.contains('btn-view') || 
                              e.target.parentElement.classList.contains('btn-view')) {
                        const row = e.target.closest('tr');
                        const id = row.cells[0].textContent;
                        const nombre = row.cells[2].textContent;
                        viewUsuario(id, nombre);
                    }
                });
            }
        
            // Cargar usuarios al inicializar (página 1)
            loadUsuarios(1);
        }
    
        function editUsuario(dni, nombre) {
            console.log(`Editar usuario DNI ${dni}: ${nombre}`);
            // Implementar lógica de edición usando DNI
        }
    
        function viewUsuario(dni, nombre) {
            console.log(`Ver usuario DNI ${dni}: ${nombre}`);
            // Implementar lógica de visualización usando DNI
        }
    
        function deleteUsuario(dni, nombre) {
            if (confirm(`¿Está seguro de que desea eliminar el usuario "${nombre}" (DNI: ${dni})?`)) {
                console.log(`Eliminar usuario DNI ${dni}: ${nombre}`);
                // Implementar lógica de eliminación usando DNI
            }
        }
    
        // Validación del formulario
        function validateForm() {
            let isValid = true;
        
            // ✅ Validar campos básicos requeridos (siempre visibles)
            const basicRequiredFields = document.querySelectorAll(`
                #dni[required], 
                #nombre[required], 
                #apellido[required], 
                #fechaNacimiento[required],
                #genero[required],
                #pais[required],
                #provincia[required],
                #ciudad[required],
                #correo[required],
                #rol-select[required]
            `);
            
            basicRequiredFields.forEach(field => {
                if (!field.value.trim()) {
                    field.classList.add('error');
                    isValid = false;
                    
                    // Mostrar mensaje de error específico
                    const fieldName = field.previousElementSibling?.textContent?.replace('*', '').trim() || 'Este campo';
                    showFieldError(field, `${fieldName} es obligatorio`);
                } else {
                    field.classList.remove('error');
                    hideFieldError(field);
                }
            });
        
            // Validar que un rol esté seleccionado
            if (selectedRoles.length === 0) {
                showNotification('Debe seleccionar un rol para el usuario', 'error');
                isValid = false;
            }
        
            // ✅ Validar ubicación
            if (!validateLocation()) {
                isValid = false;
            }
        
            // ✅ Validar fecha de nacimiento
            if (!validateFechaNacimiento()) {
                isValid = false;
            }
        
            // ✅ Validar campos específicos según el rol seleccionado
            if (selectedRoles.length > 0) {
                const rol = selectedRoles[0];
                if (rol === 'ALUMNO') {
                    if (!validateAlumnoFields()) {
                        isValid = false;
                    }
                }
                // Puedes agregar validaciones para DOCENTE aquí si es necesario
            }
        
            return isValid;
        }
        
        // ✅ NUEVA: Función para validar campos de alumno
        function validateAlumnoFields() {
            let isValid = true;
            
            const colegioEgreso = document.getElementById('colegioEgreso');
            const añoEgreso = document.getElementById('añoEgreso');
            const ultimosEstudios = document.getElementById('ultimosEstudios');
            
            if (colegioEgreso && !colegioEgreso.value.trim()) {
                showFieldError(colegioEgreso, 'El colegio de egreso es obligatorio');
                isValid = false;
            } else if (colegioEgreso) {
                hideFieldError(colegioEgreso);
            }
            
            if (añoEgreso && !añoEgreso.value) {
                showFieldError(añoEgreso, 'El año de egreso es obligatorio');
                isValid = false;
            } else if (añoEgreso) {
                hideFieldError(añoEgreso);
            }
            
            if (ultimosEstudios && !ultimosEstudios.value) {
                showFieldError(ultimosEstudios, 'Los últimos estudios son obligatorios');
                isValid = false;
            } else if (ultimosEstudios) {
                hideFieldError(ultimosEstudios);
            }
            
            return isValid;
        }
    
        // Manejo del envío del formulario
        const form = document.getElementById('user-form');
        if (form) {
            form.addEventListener('submit', function(event) {
                event.preventDefault();
                
                if (validateForm()) {
                    submitForm();
                }
            });
        }
    
        function submitForm() {
            console.log('🚀 Enviando formulario de usuario...');
            
            const form = document.getElementById('user-form');
            const formData = new FormData(form);
            
            // ✅ MOSTRAR LOADING
            showLoading('Registrando usuario...');
            
            // Deshabilitar el botón de envío
            const submitBtn = form.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Registrando...';
            submitBtn.disabled = true;
            
            // Agregar roles seleccionados
            if (selectedRoles.length > 0) {
                formData.append('rol', selectedRoles[0]);
            }
            
            // Procesar horarios para docente
            if (selectedRoles.includes('DOCENTE')) {
                const horarios = obtenerHorariosDeTabla();
                if (horarios.length > 0) {
                    formData.append('horariosDisponibilidad', JSON.stringify(horarios));
                    console.log('📅 Enviando horarios como JSON:', horarios);
                }
            }
        
            // Enviar al servidor
            fetch('/admin/usuarios/registrar', {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (!response.ok) {
                    // ✅ CAPTURAR ERRORES HTTP (400, 500, etc.)
                    return response.json().then(errorData => {
                        throw new Error(errorData.message || `Error HTTP: ${response.status}`);
                    });
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    showNotification('✅ ' + data.message, 'success', 8000);
                    resetForm();
                    hideForm();
                    loadUsuarios();
                } else {
                    // ✅ CAPTURAR MENSAJES DE ERROR ESPECÍFICOS DEL BACKEND
                    const errorMessage = data.message || 'Error desconocido al registrar usuario';
                    showNotification('❌ ' + errorMessage, 'error', 10000);
                    
                    // ✅ RESALTAR CAMPOS ESPECÍFICOS SI HAY ERRORES DE VALIDACIÓN
                    if (data.message && data.message.includes('correo electrónico')) {
                        const correoInput = document.getElementById('correo');
                        if (correoInput) {
                            showFieldError(correoInput, 'Este correo electrónico ya está registrado');
                        }
                    }
                    if (data.message && data.message.includes('DNI')) {
                        const dniInput = document.getElementById('dni');
                        if (dniInput) {
                            showFieldError(dniInput, 'Este DNI ya está registrado');
                        }
                    }
                }
            })
            .catch(error => {
                console.error('💥 Error:', error);
                
                // ✅ MEJOR MANEJO DE DIFERENTES TIPOS DE ERROR
                let errorMessage = 'Error de conexión al registrar el usuario';
                
                if (error.message.includes('correo electrónico')) {
                    errorMessage = 'El correo electrónico ya está registrado';
                    const correoInput = document.getElementById('correo');
                    if (correoInput) {
                        showFieldError(correoInput, errorMessage);
                    }
                } else if (error.message.includes('DNI')) {
                    errorMessage = 'El DNI ya está registrado';
                    const dniInput = document.getElementById('dni');
                    if (dniInput) {
                        showFieldError(dniInput, errorMessage);
                    }
                } else {
                    errorMessage = error.message || 'Error de conexión al registrar el usuario';
                }
                
                showNotification('❌ ' + errorMessage, 'error', 10000);
            })
            .finally(() => {
                // ✅ RESTAURAR BOTÓN
                submitBtn.innerHTML = originalText;
                submitBtn.disabled = false;
                hideLoading();
            });
        }
        
        // ✅ NUEVA FUNCIÓN: Mostrar loading
        function showLoading(message = 'Procesando...') {
            // Crear overlay de loading
            const loadingOverlay = document.createElement('div');
            loadingOverlay.id = 'loading-overlay';
            loadingOverlay.className = 'loading-overlay';
            loadingOverlay.innerHTML = `
                <div class="loading-content">
                    <div class="loading-spinner">
                        <i class="fas fa-spinner fa-spin"></i>
                    </div>
                    <div class="loading-text">${message}</div>
                </div>
            `;
            
            document.body.appendChild(loadingOverlay);
            
            // Mostrar con animación
            setTimeout(() => {
                loadingOverlay.classList.add('show');
            }, 10);
        }
        
        // ✅ NUEVA FUNCIÓN: Ocultar loading
        function hideLoading() {
            const loadingOverlay = document.getElementById('loading-overlay');
            if (loadingOverlay) {
                loadingOverlay.classList.remove('show');
                setTimeout(() => {
                    if (loadingOverlay.parentNode) {
                        loadingOverlay.parentNode.removeChild(loadingOverlay);
                    }
                }, 300);
            }
        }
        
        // ✅ MEJORAR LA FUNCIÓN DE NOTIFICACIONES
        function showNotification(message, type = 'info', duration = 8000) {
            // Cerrar notificaciones existentes del mismo tipo si es error
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
            
            // Iconos según el tipo
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
            
            // Mostrar con animación
            setTimeout(() => {
                notification.classList.add('show');
                
                // Animación de progreso
                const progressBar = notification.querySelector('.notification-progress');
                if (progressBar) {
                    progressBar.style.animation = `progress ${duration}ms linear`;
                }
            }, 100);
            
            // Auto-remover después del tiempo
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
            
            // Cerrar manualmente
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
    
        // Función para cargar usuarios en la tabla
        function loadUsuarios(page = 1) {
            console.log(`🔄 Cargando usuarios página ${page}...`);
            
            currentPage = page;
            
            // Mostrar loading en la tabla
            const tableBody = document.querySelector('#usuarios-table tbody');
            if (tableBody) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="9" class="text-center">
                            <div class="loading-inline">
                                <i class="fas fa-spinner fa-spin"></i> Cargando usuarios...
                            </div>
                        </td>
                    </tr>
                `;
            }
            
            fetch(`/admin/usuarios/listar?page=${page}&size=${pageSize}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Error en la respuesta del servidor: ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                console.log('📊 RESPUESTA COMPLETA:', data);
                
                if (data.success) {
                    populateUsuariosTable(data.data);
                    
                    // ✅ MANEJO MÁS ROBUSTO DE LA PAGINACIÓN
                    if (data.pagination) {
                        updatePagination(data.pagination);
                    } else {
                        // Si no viene pagination, crear uno con valores por defecto
                        const defaultPagination = {
                            totalElements: data.data?.totalElements || 0,
                            totalPages: data.data?.totalPages || 1,
                            currentPage: page,
                            pageSize: pageSize
                        };
                        updatePagination(defaultPagination);
                        console.log("⚠️ Usando paginación por defecto:", defaultPagination);
                    }
                } else {
                    console.error('Error del servidor:', data.message);
                    showNotification('❌ Error al cargar usuarios: ' + data.message, 'error', 10000);
                }
            })
            .catch(error => {
                console.error('Error cargando usuarios:', error);
                showNotification('❌ Error al cargar usuarios desde el servidor', 'error', 10000);
                
                // Mostrar mensaje de error en la tabla
                if (tableBody) {
                    tableBody.innerHTML = `
                        <tr>
                            <td colspan="9" class="text-center error-message">
                                <i class="fas fa-exclamation-triangle"></i> Error al cargar usuarios
                            </td>
                        </tr>
                    `;
                }
            });
        }

        function updatePagination(pagination) {
            // ✅ HACER EL CÓDIGO MÁS ROBUSTO
            const totalElements = pagination?.totalElements || 0;
            totalPages = pagination?.totalPages || 1;
            
            console.log("📊 Actualizando paginación:", pagination);
            
            // Actualizar información de paginación
            const paginationInfo = document.querySelector('.pagination-info');
            if (paginationInfo) {
                const startItem = ((currentPage - 1) * pageSize) + 1;
                const endItem = Math.min(currentPage * pageSize, totalElements);
                paginationInfo.textContent = `Mostrando ${startItem}-${endItem} de ${totalElements} usuarios`;
            }
            
            // Actualizar controles de paginación
            updatePaginationControls();
        }
        
        function updatePaginationControls() {
            const prevBtn = document.querySelector('.btn-pagination:first-child');
            const nextBtn = document.querySelector('.btn-pagination:last-child');
            const pagesContainer = document.querySelector('.pagination-pages');
            
            if (!prevBtn || !nextBtn || !pagesContainer) return;
            
            // Botones anterior/siguiente
            prevBtn.disabled = currentPage <= 1;
            nextBtn.disabled = currentPage >= totalPages;
            
            // Agregar event listeners a los botones
            prevBtn.onclick = () => loadUsuarios(currentPage - 1);
            nextBtn.onclick = () => loadUsuarios(currentPage + 1);
            
            // Generar números de página
            pagesContainer.innerHTML = '';
            
            // Mostrar máximo 5 páginas alrededor de la actual
            const startPage = Math.max(1, currentPage - 2);
            const endPage = Math.min(totalPages, currentPage + 2);
            
            for (let i = startPage; i <= endPage; i++) {
                const pageBtn = document.createElement('button');
                pageBtn.className = `btn-page ${i === currentPage ? 'active' : ''}`;
                pageBtn.textContent = i;
                pageBtn.onclick = () => loadUsuarios(i);
                pagesContainer.appendChild(pageBtn);
            }
        }

        function applyFilters() {
            // Volver a la primera página cuando se aplican filtros
            loadUsuarios(1);
        }

        function clearFilters() {
            if (searchInput) searchInput.value = '';
            if (filtroRol) filtroRol.value = '';
            if (filtroEstado) filtroEstado.value = '';
            if (filtroGenero) filtroGenero.value = '';
            
            // Volver a la primera página cuando se limpian filtros
            loadUsuarios(1);
        }

        function getCurrentFilters() {
            const filters = {};
            
            if (searchInput && searchInput.value) {
                filters.search = searchInput.value;
            }
            if (filtroRol && filtroRol.value) {
                filters.rol = filtroRol.value;
            }
            if (filtroEstado && filtroEstado.value) {
                filters.estado = filtroEstado.value;
            }
            if (filtroGenero && filtroGenero.value) {
                filters.genero = filtroGenero.value;
            }
            
            return filters;
        }
    
        // Función para poblar la tabla con datos
        function populateUsuariosTable(responseData) {
            console.log('📋 Poblando tabla con', responseData.content.length, 'usuarios');
            
            const tableBody = document.querySelector('#usuarios-table tbody');
            if (!tableBody) {
                console.error('No se encontró el tbody de la tabla de usuarios');
                return;
            }
        
            let usuarios = [];
            let totalElements = 0;
        
            if (responseData && responseData.content) {
                usuarios = responseData.content;
                totalElements = responseData.totalElements || usuarios.length;
            } else if (Array.isArray(responseData)) {
                usuarios = responseData;
                totalElements = usuarios.length;
            } else {
                console.error('Estructura de datos no reconocida:', responseData);
            }
        
            console.log('👥 Usuarios a mostrar:', usuarios.length);
        
            // Limpiar tabla existente
            tableBody.innerHTML = '';
        
            if (usuarios.length === 0) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center">No hay usuarios registrados</td> <!-- ✅ CAMBIAR A 8 COLUMNAS -->
                    </tr>
                `;
                updateTableStats(0);
                return;
            }
        
            // Crear filas para cada usuario
            usuarios.forEach(usuario => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <!-- ✅ NUEVO ORDEN: DNI primero, sin ID -->
                    <td>${usuario.dni || 'N/A'}</td>
                    <td>
                        ${usuario.foto ? 
                            `<img src="${usuario.foto}" alt="Foto" class="user-photo-small">` : 
                            '<i class="fas fa-user-circle user-icon"></i>'}
                    </td>
                    <td>${usuario.nombreCompleto || 'Sin nombre'}</td>
                    <td>${usuario.correo || 'N/A'}</td>
                    <td>${formatRoles(usuario.roles || [])}</td>
                    <td><span class="status-badge status-${(usuario.estado || 'activo').toLowerCase()}">${usuario.estado || 'ACTIVO'}</span></td>
                    <td>${usuario.fechaRegistro ? new Date(usuario.fechaRegistro).toLocaleDateString() : 'N/A'}</td>
                    <td class="actions">
                        <button class="btn-icon btn-view" onclick="viewUsuario('${usuario.dni}', '${usuario.nombreCompleto}')" title="Ver">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn-icon btn-edit" onclick="editUsuario('${usuario.dni}', '${usuario.nombreCompleto}')" title="Editar">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn-icon btn-delete" onclick="deleteUsuario('${usuario.dni}', '${usuario.nombreCompleto}')" title="Eliminar">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                `;
                tableBody.appendChild(row);
            });
        
            updateTableStats(totalElements);
        }
    
        function formatRoles(roles) {
            if (!roles || roles.length === 0) return 'Sin roles';
            
            return roles.map(role => {
                const roleClass = role.toLowerCase();
                const roleIcon = {
                    'alumno': 'fas fa-user-graduate',
                    'docente': 'fas fa-chalkboard-teacher',
                    'admin': 'fas fa-user-shield',
                    'coordinador': 'fas fa-user-tie'
                }[roleClass] || 'fas fa-user';
                
                return `<span class="role-badge role-${roleClass}"><i class="${roleIcon}"></i> ${role}</span>`;
            }).join(' ');
        }
    
        // Utility function for debouncing
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
    
        function obtenerHorariosDeTabla() {
            const horarios = [];
            const horariosTable = document.getElementById('horarios-docente-table').getElementsByTagName('tbody')[0];
            
            for (let i = 0; i < horariosTable.rows.length; i++) {
                const row = horariosTable.rows[i];
                let dia = row.cells[0].textContent;
                const horarioTexto = row.cells[1].textContent;
                
                // ✅ NORMALIZAR NOMBRES DE DÍAS (quitar acentos)
                dia = normalizarNombreDia(dia);
                
                // Parsear horario (formato: "08:00 - 12:00")
                const [horaInicio, horaFin] = horarioTexto.split(' - ');
                
                horarios.push({
                    diaSemana: dia,
                    horaInicio: horaInicio,
                    horaFin: horaFin
                });
            }
            
            return horarios;
        }
    
        // ✅ FUNCIÓN: Normalizar nombres de días para el backend
        function normalizarNombreDia(dia) {
            const normalizaciones = {
                'Lunes': 'LUNES',
                'Martes': 'MARTES',
                'Miércoles': 'MIERCOLES',
                'Miercoles': 'MIERCOLES',
                'Jueves': 'JUEVES',
                'Viernes': 'VIERNES',
                'Sábado': 'SABADO',
                'Sabado': 'SABADO',
                'Domingo': 'DOMINGO'
            };
            
            return normalizaciones[dia] || dia.toUpperCase();
        }
    
        // ✅ AGREGAR al resetForm para limpiar contador
        function resetForm() {
            document.getElementById('user-form').reset();
            resetFotoUpload();
            clearHorariosDocenteTable();
            hideDocenteFields();
            hideAlumnoFields();
            resetRoles();
            resetLocation();
            resetFechaNacimiento();
            removeAllSpecificRequired();
            
            // ✅ Asegurar que el contador se oculte
            const contador = document.getElementById('contador-horarios');
            if (contador) {
                contador.style.display = 'none';
            }
        }
    
        console.log('Gestión de Usuarios inicializada correctamente');
    });