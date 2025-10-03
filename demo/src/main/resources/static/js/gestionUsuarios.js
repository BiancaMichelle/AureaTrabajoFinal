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

    // Variables para roles seleccionados
    let selectedRoles = [];

    // Inicialización
    initializeFormHandlers();
    initializeFotoUpload();
    initializeRoles();
    initializeHorariosDocente();
    initializeFilters();
    initializeTable();

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
        resetRoles();
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

    // Manejo de roles múltiples (similar a categorías)
    function initializeRoles() {
        const selectedRolesContainer = document.getElementById('selected-roles');
        const selectedChipsContainer = document.getElementById('selected-roles-chips');

        // Mapeo de iconos para cada rol
        const roleIcons = {
            ALUMNO: 'fas fa-user-graduate',
            DOCENTE: 'fas fa-chalkboard-teacher',
            ADMIN: 'fas fa-user-shield',
            COORDINADOR: 'fas fa-user-tie'
        };

        if (rolSelect) {
            rolSelect.addEventListener('change', function() {
                const selectedValue = this.value;
                const selectedText = this.options[this.selectedIndex].text;

                if (selectedValue && !selectedRoles.includes(selectedValue)) {
                    selectedRoles.push(selectedValue);
                    updateSelectedRoles();
                    
                    // Mostrar campos específicos si se selecciona DOCENTE
                    if (selectedValue === 'DOCENTE') {
                        showDocenteFields();
                    }
                }

                // Resetear select
                this.value = '';
            });
        }

        function updateSelectedRoles() {
            if (selectedChipsContainer) {
                selectedChipsContainer.innerHTML = '';
                
                selectedRoles.forEach(role => {
                    const chip = document.createElement('div');
                    chip.className = 'category-chip';
                    chip.setAttribute('data-role', role.toLowerCase());
                    
                    const icon = roleIcons[role] || 'fas fa-user';
                    chip.innerHTML = `
                        <i class="${icon}"></i>
                        <span>${getRoleDisplayName(role)}</span>
                        <i class="fas fa-times chip-remove" onclick="removeRole('${role}')"></i>
                    `;
                    selectedChipsContainer.appendChild(chip);
                });

                // Mostrar/ocultar contenedor
                if (selectedRoles.length > 0) {
                    selectedRolesContainer.classList.add('show');
                } else {
                    selectedRolesContainer.classList.remove('show');
                    hideDocenteFields();
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

        // Función global para remover roles
        window.removeRole = function(value) {
            selectedRoles = selectedRoles.filter(role => role !== value);
            updateSelectedRoles();
        };

        // Función para obtener los roles seleccionados (para el formulario)
        window.getSelectedRoles = function() {
            return selectedRoles;
        };

        // Función para resetear roles
        window.resetRoles = function() {
            selectedRoles = [];
            selectedRolesContainer.classList.remove('show');
            selectedChipsContainer.innerHTML = '';
            hideDocenteFields();
        };
    }

    // Mostrar/ocultar campos específicos de docente
    function showDocenteFields() {
        if (fieldsDocente) {
            fieldsDocente.style.display = 'block';
        }
    }

    function hideDocenteFields() {
        if (fieldsDocente && !selectedRoles.includes('DOCENTE')) {
            fieldsDocente.style.display = 'none';
        }
    }

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
                    }
                }
            });
        }
    }

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
    }

    function clearHorariosDocenteTable() {
        if (horariosDocenteTableBody) {
            horariosDocenteTableBody.innerHTML = '';
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

        // Cargar usuarios al inicializar
        loadUsuarios();
    }

    function editUsuario(id, nombre) {
        console.log(`Editar usuario ${id}: ${nombre}`);
        // Implementar lógica de edición
    }

    function viewUsuario(id, nombre) {
        console.log(`Ver usuario ${id}: ${nombre}`);
        // Implementar lógica de visualización
    }

    function deleteUsuario(id, nombre) {
        if (confirm(`¿Está seguro de que desea eliminar el usuario "${nombre}"?`)) {
            console.log(`Eliminar usuario ${id}: ${nombre}`);
            // Implementar lógica de eliminación
        }
    }

    // Validación del formulario
    function validateForm() {
        const requiredFields = document.querySelectorAll('#user-form [required]');
        let isValid = true;

        requiredFields.forEach(field => {
            if (!field.value.trim()) {
                field.classList.add('error');
                isValid = false;
            } else {
                field.classList.remove('error');
            }
        });

        // Validar que al menos un rol esté seleccionado
        if (selectedRoles.length === 0) {
            showNotification('Debe seleccionar al menos un rol para el usuario', 'error');
            isValid = false;
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
        
        // Agregar roles seleccionados
        selectedRoles.forEach(role => {
            formData.append('roles', role);
        });
        
        // Agregar horarios de docente como string estructurada
        if (selectedRoles.includes('DOCENTE')) {
            const horariosTable = document.getElementById('horarios-docente-table').getElementsByTagName('tbody')[0];
            const horariosArray = [];
            for (let i = 0; i < horariosTable.rows.length; i++) {
                const row = horariosTable.rows[i];
                const dia = row.cells[0].textContent;
                const horario = row.cells[1].textContent;
                horariosArray.push(`${dia}:${horario}`);
            }
            formData.append('horariosDisponibilidad', horariosArray.join(','));
        }
        
        // Enviar al servidor
        fetch('/admin/usuarios/registrar', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('✅ Usuario registrado exitosamente', 'success');
                resetForm();
                hideForm();
                loadUsuarios(); // Recargar tabla
            } else {
                showNotification('❌ Error: ' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('💥 Error:', error);
            showNotification('❌ Error al registrar el usuario', 'error');
        });
    }

    // Función para cargar usuarios en la tabla
    function loadUsuarios() {
        console.log('🔄 Cargando usuarios desde el servidor...');
        
        fetch('/admin/usuarios/listar', {
            method: 'GET'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Error en la respuesta del servidor');
            }
            return response.json();
        })
        .then(data => {
            console.log('📊 Datos recibidos:', data);
            if (data.success) {
                populateUsuariosTable(data.data);
            } else {
                console.error('Error del servidor:', data.message);
                showNotification('Error al cargar usuarios: ' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error cargando usuarios:', error);
            showNotification('Error al cargar usuarios desde el servidor', 'error');
        });
    }

    // Función para poblar la tabla con datos
    function populateUsuariosTable(usuarios) {
        console.log('📋 Poblando tabla con', usuarios.length, 'usuarios');
        
        const tableBody = document.querySelector('#usuarios-table tbody');
        if (!tableBody) {
            console.error('No se encontró el tbody de la tabla de usuarios');
            return;
        }

        // Limpiar tabla existente
        tableBody.innerHTML = '';

        if (usuarios.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center">No hay usuarios registrados</td>
                </tr>
            `;
            updateTableStats(0);
            return;
        }

        // Crear filas para cada usuario
        usuarios.forEach(usuario => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${usuario.id || 'N/A'}</td>
                <td>
                    ${usuario.foto ? 
                        `<img src="${usuario.foto}" alt="Foto" class="user-photo-small">` : 
                        '<i class="fas fa-user-circle user-icon"></i>'}
                </td>
                <td>${usuario.nombreCompleto || 'Sin nombre'}</td>
                <td>${usuario.dni || 'N/A'}</td>
                <td>${usuario.correo || 'N/A'}</td>
                <td>${formatRoles(usuario.roles || [])}</td>
                <td><span class="status-badge status-${(usuario.estado || 'activo').toLowerCase()}">${usuario.estado || 'ACTIVO'}</span></td>
                <td>${usuario.fechaRegistro ? new Date(usuario.fechaRegistro).toLocaleDateString() : 'N/A'}</td>
                <td class="actions">
                    <button class="btn-icon btn-view" onclick="viewUsuario(${usuario.id}, '${usuario.nombreCompleto}')" title="Ver">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn-icon btn-edit" onclick="editUsuario(${usuario.id}, '${usuario.nombreCompleto}')" title="Editar">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-icon btn-delete" onclick="deleteUsuario(${usuario.id}, '${usuario.nombreCompleto}')" title="Eliminar">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });

        updateTableStats(usuarios.length);
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

    // Función para mostrar notificaciones
    function showNotification(message, type) {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <span>${message}</span>
                <button class="notification-close">&times;</button>
            </div>
        `;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 5000);
        
        notification.querySelector('.notification-close').addEventListener('click', () => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        });
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

    console.log('Gestión de Usuarios inicializada correctamente');
});