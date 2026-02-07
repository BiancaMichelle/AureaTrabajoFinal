// ================================================
// GESTIÓN DE OFERTAS ACADÉMICAS - REPLICANDO LÓGICA DE GESTIÓN USUARIOS
// ================================================

console.log('🔥 SCRIPT GESTION OFERTAS CARGADO');

// Fallback de loading si no existe implementacion global
if (typeof window.mostrarLoading !== 'function') {
    window.mostrarLoading = function(mensaje) {
        console.log('[LOADING] ' + (mensaje || 'Cargando...'));
        const overlayId = 'loading-overlay-horarios';
        if (!document.getElementById(overlayId)) {
            const div = document.createElement('div');
            div.id = overlayId;
            div.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.35);display:flex;align-items:center;justify-content:center;z-index:9999;color:#fff;font-size:1rem;';
            div.textContent = mensaje || 'Cargando...';
            document.body.appendChild(div);
        } else {
            document.getElementById(overlayId).textContent = mensaje || 'Cargando...';
            document.getElementById(overlayId).style.display = 'flex';
        }
    };
}
if (typeof window.ocultarLoading !== 'function') {
    window.ocultarLoading = function() {
        const overlay = document.getElementById('loading-overlay-horarios');
        if (overlay) overlay.style.display = 'none';
    };
}

// Helpers CSRF (meta/input/cookie)
function getCsrfToken() {
    const metaToken = document.querySelector('meta[name="_csrf"]');
    if (metaToken) return metaToken.getAttribute('content');
    const inputToken = document.querySelector('input[name="_csrf"]');
    if (inputToken) return inputToken.value;
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}
function getCsrfHeaderName() {
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    return metaHeader ? metaHeader.getAttribute('content') : 'X-XSRF-TOKEN';
}
function normalizeNumber(value) {
    if (value == null) return NaN;
    const v = String(value).trim().replace(',', '.');
    return parseFloat(v);
}
function isUuid(value) {
    return /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(value);
}



function showGlobalAlertModal(title, message) {
    const modal = document.getElementById('globalConfirmationModal');
    if (!modal) return false;

    const titleEl = document.getElementById('confTitle');
    const msgEl = document.getElementById('confirmationMessage');
    const btnConfirm = document.getElementById('btnAcceptConfirm');
    const btnCancel = modal.querySelector('.modal-footer .btn-secondary');
    const btnClose = modal.querySelector('.btn-close');

    if (titleEl) titleEl.textContent = title;
    if (msgEl) msgEl.textContent = message;

    // Guardar estado previo
    const prevCancelDisplay = btnCancel ? btnCancel.style.display : '';
    const prevConfirmText = btnConfirm ? btnConfirm.textContent : '';

    if (btnCancel) btnCancel.style.display = 'none';
    if (btnConfirm) btnConfirm.textContent = 'Aceptar';

    const restore = () => {
        if (btnCancel) btnCancel.style.display = prevCancelDisplay;
        if (btnConfirm) btnConfirm.textContent = prevConfirmText || 'Confirmar';
    };

    if (btnConfirm) {
        const newBtn = btnConfirm.cloneNode(true);
        btnConfirm.parentNode.replaceChild(newBtn, btnConfirm);
        newBtn.addEventListener('click', () => {
            restore();
            if (typeof ModalConfirmacion !== 'undefined' && ModalConfirmacion.close) {
                ModalConfirmacion.close();
            } else {
                modal.style.display = 'none';
            }
        });
    }

    if (btnClose) {
        btnClose.addEventListener('click', restore, { once: true });
    }

    modal.classList.remove('fade-out');
    modal.style.display = 'flex';
    return true;
}

// Fallback de alertas para el modulo de horarios automaticos
if (typeof window.mostrarAlerta !== 'function') {
    window.mostrarAlerta = function(mensaje, tipo) {
        const title = tipo === 'error' ? 'Error' : tipo === 'warning' ? 'Advertencia' : 'Aviso';
        if (showGlobalAlertModal(title, mensaje)) {
            return;
        }
        if (typeof window.mostrarNotificacion === 'function') {
            return window.mostrarNotificacion(mensaje, tipo);
        }
        if (typeof window.showNotification === 'function') {
            return window.showNotification(mensaje, tipo);
        }
        alert((tipo === 'error' ? '[ERROR] ' : tipo === 'warning' ? '[WARN] ' : '[OK] ') + mensaje);
    };
}

if (!window.__skipGestionOfertasInit) {
    document.addEventListener('DOMContentLoaded', function () {
    console.log('✅ DOM CARGADO - Iniciando gestión de ofertas');
    
    // Referencias a elementos principales
    const btnShowForm = document.getElementById('btn-show-form');
    const formContainer = document.getElementById('form-container');
    const btnCloseForm = document.getElementById('btn-close-form');
    
    console.log('🔍 Elementos encontrados:');
    console.log('- btnShowForm:', btnShowForm);
    console.log('- formContainer:', formContainer);
    console.log('- btnCloseForm:', btnCloseForm);
    const btnCancelForm = document.getElementById('btn-cancel-form');
    const tipoOfertaSelect = document.getElementById('tipoOferta');
    const otorgaCertificadoCheckbox = document.getElementById('otorgaCertificado');
    const certificadoFields = document.getElementById('certificado-fields');

    // Elementos para upload de imagen
    const imagenInput = document.getElementById('imagen');
    const imagePreview = document.getElementById('image-preview');
    const uploadPlaceholder = document.querySelector('.upload-placeholder');

    // Elementos para horarios
    const btnAddHorario = document.getElementById('btn-add-horario');
    const horariosTableBody = document.querySelector('#horarios-table tbody');
    const diaSelect = document.getElementById('dia-semana');
    const horaDesdeInput = document.getElementById('hora-desde');
    const horaHastaInput = document.getElementById('hora-hasta');

    // Elementos para filtros
    const searchInput = document.getElementById('search-input');
    const filtroTipo = document.getElementById('filtro-tipo');
    const filtroModalidad = document.getElementById('filtro-modalidad');
    const filtroEstado = document.getElementById('filtro-estado');
    const filtroCertificado = document.getElementById('filtro-certificado');
    const filtroCostoMin = document.getElementById('filtro-costo-min');
    const filtroCostoMax = document.getElementById('filtro-costo-max');
    const btnApplyFilters = document.getElementById('btn-apply-filters');
    const btnClearFilters = document.getElementById('btn-clear-filters');

    // Inicialización
    initializeFormHandlers();
    initializeImageUpload();
    initializeHorarios();
    initializeCertificado();
    initializeTipoOferta();
    initializeModalidad();
    initializeCategorias();
    initializeFilters();
    initializeTable();

    // Mostrar/ocultar formulario
    function initializeFormHandlers() {
        console.log('🔧 Inicializando form handlers');
        
        if (btnShowForm) {
            console.log('✅ btnShowForm encontrado, agregando event listener');
            btnShowForm.addEventListener('click', function () {
                console.log('👆 CLICK EN BOTÓN NUEVA OFERTA DETECTADO!');
                showForm();
            });
        } else {
            console.error('❌ btnShowForm NO ENCONTRADO!');
        }

        if (btnCloseForm) {
            btnCloseForm.addEventListener('click', function () {
                hideForm();
            });
        }

        if (btnCancelForm) {
            btnCancelForm.addEventListener('click', function () {
                hideForm();
            });
        }
    }

    function showForm() {
        console.log('🚀 EJECUTANDO showForm()');
        console.log('- formContainer antes:', formContainer.style.display);
        
        formContainer.style.display = 'block';
        console.log('- formContainer después de display block:', formContainer.style.display);
        
        setTimeout(() => {
            formContainer.classList.add('show');
            console.log('- Clase "show" agregada');
            console.log('- Classes actuales:', formContainer.className);
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
        document.getElementById('oferta-form').reset();
        document.getElementById('idOfertaModificar').value = '';
        
        const btnSubmit = document.getElementById('btn-submit-text');
        if (btnSubmit) btnSubmit.textContent = 'Registrar Oferta';
        
        hideAllTipoSpecificFields();
        resetImageUpload();
        clearHorariosTable();
        hideCertificadoFields();
        resetCategorias();
        
        // Resetear visibilidad de ubicación
        if (window.updateUbicacionFields) {
            window.updateUbicacionFields(''); // Ocultar todo
        }
    }

    function resetCategorias() {
        if (window.resetCategorias) {
            window.resetCategorias();
        }
    }

    // Manejo del upload de imagen
    function initializeImageUpload() {
        if (imagenInput) {
            imagenInput.addEventListener('change', function(event) {
                const file = event.target.files[0];
                if (file) {
                    if (file.size > 5 * 1024 * 1024) { // 5MB
                        alert('El archivo es demasiado grande. El tamaño máximo es 5MB.');
                        return;
                    }

                    const reader = new FileReader();
                    reader.onload = function(e) {
                        imagePreview.src = e.target.result;
                        imagePreview.style.display = 'block';
                        uploadPlaceholder.style.display = 'none';
                    }
                    reader.readAsDataURL(file);
                }
            });
        }
    }

    function resetImageUpload() {
        if (imagePreview && uploadPlaceholder) {
            imagePreview.style.display = 'none';
            uploadPlaceholder.style.display = 'flex';
            imagePreview.src = '';
        }
    }

    // Manejo de campos específicos por tipo de oferta
    function initializeTipoOferta() {
        if (tipoOfertaSelect) {
            tipoOfertaSelect.addEventListener('change', function () {
                const selectedType = this.value;
                hideAllTipoSpecificFields();
                showCostFields(selectedType);
                showTipoSpecificFields(selectedType);
            });
        }
    }

    function hideAllTipoSpecificFields() {
        const tipoSpecificSections = document.querySelectorAll('.tipo-specific');
        tipoSpecificSections.forEach(section => {
            section.style.display = 'none';
        });

        // Ocultar campos de costo específicos
        hideCostFields();
    }

    function showTipoSpecificFields(tipo) {
        const fieldsMap = {
            'CURSO': '#fields-curso',
            'FORMACION': '#fields-formacion',
            'CHARLA': '#fields-charla',
            'SEMINARIO': '#fields-seminario'
        };

        const sectionId = fieldsMap[tipo];
        if (sectionId) {
            const section = document.querySelector(sectionId);
            if (section) {
                section.style.display = 'block';
            }
        }
    }

    function showCostFields(tipo) {
        const costFields = ['grupo-costo-cuota', 'grupo-nro-cuotas', 'grupo-costo-mora', 'grupo-dia-vencimiento'];
        
        if (tipo === 'CURSO' || tipo === 'FORMACION') {
            costFields.forEach(fieldId => {
                const field = document.getElementById(fieldId);
                if (field) field.style.display = 'block';
            });
        }
    }

    function hideCostFields() {
        const costFields = ['grupo-costo-cuota', 'grupo-nro-cuotas', 'grupo-costo-mora', 'grupo-dia-vencimiento'];
        costFields.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) field.style.display = 'none';
        });
    }

    // Manejo de campos de ubicación según modalidad
    function initializeModalidad() {
        const modalidadSelect = document.getElementById('modalidad');
        const grupoLugar = document.getElementById('grupo-lugar');
        const grupoEnlace = document.getElementById('grupo-enlace');
        
        if (modalidadSelect) {
            modalidadSelect.addEventListener('change', function() {
                const modalidad = this.value;
                updateUbicacionFields(modalidad);
            });
        }
        
        // Función interna para actualizar visibilidad
        window.updateUbicacionFields = function(modalidad) {
            if (!grupoLugar || !grupoEnlace) return;
            
            const lugarInput = document.getElementById('lugar');
            const enlaceInput = document.getElementById('enlace');
            
            if (modalidad === 'PRESENCIAL') {
                grupoLugar.style.display = 'block';
                grupoEnlace.style.display = 'none';
                if(lugarInput) lugarInput.required = true;
                if(enlaceInput) enlaceInput.required = false;
            } else if (modalidad === 'VIRTUAL') {
                grupoLugar.style.display = 'none';
                grupoEnlace.style.display = 'block';
                if(lugarInput) lugarInput.required = false;
                if(enlaceInput) enlaceInput.required = true;
            } else if (modalidad === 'HIBRIDA') {
                grupoLugar.style.display = 'block';
                grupoEnlace.style.display = 'block';
                if(lugarInput) lugarInput.required = true;
                if(enlaceInput) enlaceInput.required = true;
            } else {
                grupoLugar.style.display = 'none';
                grupoEnlace.style.display = 'none';
                if(lugarInput) lugarInput.required = false;
                if(enlaceInput) enlaceInput.required = false;
            }
        };
    }

    // Manejo de certificación
    function initializeCertificado() {
        if (otorgaCertificadoCheckbox) {
            otorgaCertificadoCheckbox.addEventListener('change', function() {
                if (this.checked) {
                    showCertificadoFields();
                } else {
                    hideCertificadoFields();
                }
            });
        }
    }

    function showCertificadoFields() {
        if (certificadoFields) {
            certificadoFields.style.display = 'block';
        }
    }

    function hideCertificadoFields() {
        if (certificadoFields) {
            certificadoFields.style.display = 'none';
        }
    }

    // Manejo de categorías con combobox
    function initializeCategorias() {
        const categoriaSelect = document.getElementById('categoria-select');
        const selectedCategoriesContainer = document.getElementById('selected-categories');
        const selectedChipsContainer = document.getElementById('selected-chips');
        let selectedCategories = [];

        // Mapeo de iconos para cada categoría
        const categoryIcons = {
            programacion: 'fas fa-code',
            diseno: 'fas fa-palette',
            marketing: 'fas fa-chart-line',
            administracion: 'fas fa-briefcase',
            idiomas: 'fas fa-language',
            oficios: 'fas fa-tools',
            salud: 'fas fa-heartbeat',
            tecnologia: 'fas fa-microchip',
            finanzas: 'fas fa-dollar-sign',
            educacion: 'fas fa-graduation-cap',
            arte: 'fas fa-paint-brush',
            gastronomia: 'fas fa-utensils'
        };

        if (categoriaSelect) {
            categoriaSelect.addEventListener('change', function() {
                const selectedValue = this.value;
                const selectedText = this.options[this.selectedIndex].text;
                
                if (selectedValue && !selectedCategories.some(cat => cat.value === selectedValue)) {
                    selectedCategories.push({
                        value: selectedValue,
                        text: selectedText,
                        icon: categoryIcons[selectedValue] || 'fas fa-tag'
                    });
                    
                    updateSelectedCategories();
                    this.value = ''; // Resetear el select
                }
            });
        }

        function updateSelectedCategories() {
            if (selectedCategories.length > 0) {
                selectedCategoriesContainer.classList.add('show');
                selectedChipsContainer.innerHTML = selectedCategories.map(cat => `
                    <div class="category-chip" data-category="${cat.value}">
                        <i class="${cat.icon}"></i>
                        <span>${cat.text}</span>
                        <button type="button" class="chip-remove" onclick="removeCategory('${cat.value}')">
                            ×
                        </button>
                    </div>
                `).join('');
            } else {
                selectedCategoriesContainer.classList.remove('show');
            }
        }

        // Función global para remover categorías
        window.removeCategory = function(value) {
            selectedCategories = selectedCategories.filter(cat => cat.value !== value);
            updateSelectedCategories();
        };

        // Función para obtener las categorías seleccionadas (para el formulario)
        window.getSelectedCategories = function() {
            return selectedCategories.map(cat => cat.value);
        };

        // Función para resetear categorías
        window.resetCategorias = function() {
            selectedCategories = [];
            selectedCategoriesContainer.classList.remove('show');
            selectedChipsContainer.innerHTML = '';
            if (categoriaSelect) {
                categoriaSelect.value = '';
            }
        };
    }

    // Manejo de horarios
    function initializeHorarios() {
        if (btnAddHorario) {
            btnAddHorario.addEventListener('click', function() {
                addHorario();
            });
        }

        if (horariosTableBody) {
            horariosTableBody.addEventListener('click', function(event) {
                if (event.target.closest('.btn-delete-horario')) {
                    event.target.closest('tr').remove();
                }
            });
        }
    }

    function addHorario() {
        const dia = diaSelect.value;
        const horaDesde = horaDesdeInput.value;
        const horaHasta = horaHastaInput.value;

        if (!dia || !horaDesde || !horaHasta) {
            alert('Por favor, complete todos los campos del horario.');
            return;
        }

        if (horaDesde >= horaHasta) {
            alert('La hora de inicio debe ser anterior a la hora de fin.');
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
        horariosTableBody.appendChild(newRow);

        // Limpiar campos
        diaSelect.value = '';
        horaDesdeInput.value = '';
        horaHastaInput.value = '';
    }

    function clearHorariosTable() {
        if (horariosTableBody) {
            horariosTableBody.innerHTML = '';
        }
    }

    // Búsqueda de docentes (simulada)
    function initializeDocenteSearch() {
        const docenteSearchInputs = document.querySelectorAll('[id^="docente-search"]');
        
        docenteSearchInputs.forEach(input => {
            let searchTimeout;
            
            input.addEventListener('input', function() {
                clearTimeout(searchTimeout);
                const query = this.value;
                const resultsContainer = this.nextElementSibling;

                if (query.length < 3) {
                    resultsContainer.style.display = 'none';
                    return;
                }

                searchTimeout = setTimeout(() => {
                    searchDocentes(query, resultsContainer);
                }, 300);
            });

            // Ocultar resultados al hacer clic fuera
            document.addEventListener('click', function(event) {
                if (!input.contains(event.target)) {
                    const resultsContainer = input.nextElementSibling;
                    if (resultsContainer) {
                        resultsContainer.style.display = 'none';
                    }
                }
            });
        });
    }

    function searchDocentes(query, resultsContainer) {
        // Simulación de búsqueda de docentes
        const mockDocentes = [
            { id: 1, nombre: 'Juan Pérez', especialidad: 'Programación', dni: '12345678' },
            { id: 2, nombre: 'María García', especialidad: 'Diseño', dni: '87654321' },
            { id: 3, nombre: 'Carlos Rodríguez', especialidad: 'Marketing', dni: '11223344' }
        ];

        const filteredDocentes = mockDocentes.filter(docente => 
            docente.nombre.toLowerCase().includes(query.toLowerCase()) ||
            docente.dni.includes(query)
        );

        resultsContainer.innerHTML = '';
        if (filteredDocentes.length > 0) {
            filteredDocentes.forEach(docente => {
                const resultDiv = document.createElement('div');
                resultDiv.textContent = `${docente.nombre} - ${docente.especialidad} (DNI: ${docente.dni})`;
                resultDiv.onclick = () => addDocente(docente, resultsContainer);
                resultsContainer.appendChild(resultDiv);
            });
            resultsContainer.style.display = 'block';
        } else {
            resultsContainer.style.display = 'none';
        }
    }

    function addDocente(docente, resultsContainer) {
        // Lógica para agregar docente a la tabla correspondiente
        console.log('Agregar docente:', docente);
        resultsContainer.style.display = 'none';
        
        // Limpiar input
        const input = resultsContainer.previousElementSibling;
        if (input) input.value = '';
    }

    // Filtros y búsqueda
    function initializeFilters() {
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', function() {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    applyFilters();
                }, 300);
            });
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
            tipo: filtroTipo ? filtroTipo.value : '',
            modalidad: filtroModalidad ? filtroModalidad.value : '',
            estado: filtroEstado ? filtroEstado.value : '',
            certificado: filtroCertificado ? filtroCertificado.value : '',
            costoMin: filtroCostoMin ? parseFloat(filtroCostoMin.value) || 0 : 0,
            costoMax: filtroCostoMax ? parseFloat(filtroCostoMax.value) || Infinity : Infinity
        };

        // Aplicar filtros a la tabla
        filterTable(filters);
    }

    function clearFilters() {
        if (searchInput) searchInput.value = '';
        if (filtroTipo) filtroTipo.value = '';
        if (filtroModalidad) filtroModalidad.value = '';
        if (filtroEstado) filtroEstado.value = '';
        if (filtroCertificado) filtroCertificado.value = '';
        if (filtroCostoMin) filtroCostoMin.value = '';
        if (filtroCostoMax) filtroCostoMax.value = '';
        
        // Mostrar todas las filas
        const table = document.getElementById('ofertas-table');
        if (table) {
            const rows = table.querySelectorAll('tbody tr');
            rows.forEach(row => {
                row.style.display = '';
            });
            updateTableStats(rows.length);
        }
    }

    function filterTable(filters) {
        const table = document.getElementById('ofertas-table');
        if (!table) return;

        const rows = table.querySelectorAll('tbody tr');
        let visibleCount = 0;

        // Función para normalizar texto (ignorar tildes y mayúsculas)
        const normalize = (str) => str.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toUpperCase();

        rows.forEach(row => {
            const cells = row.querySelectorAll('td');
            if (cells.length === 0) return;

            const nombre = cells[1].textContent.toLowerCase();
            const tipo = cells[2].textContent.trim();
            const modalidad = cells[3].textContent.trim();
            const estado = cells[8].textContent.trim(); // Ajustado índice si es necesario, verificar estructura
            const costo = parseFloat(cells[6].textContent.replace(/[^0-9.]/g, '')) || 0;
            
            let visible = true;

            // Aplicar filtros
            if (filters.search && !nombre.includes(filters.search)) {
                visible = false;
            }
            if (filters.tipo && normalize(tipo) !== filters.tipo) {
                visible = false;
            }
            if (filters.modalidad && normalize(modalidad) !== filters.modalidad) {
                visible = false;
            }
            if (filters.estado && normalize(estado) !== filters.estado) {
                visible = false;
            }
            if (costo < filters.costoMin || costo > filters.costoMax) {
                visible = false;
            }

            row.style.display = visible ? '' : 'none';
            if (visible) visibleCount++;
        });

        updateTableStats(visibleCount);
    }

    function updateTableStats(count) {
        const totalElement = document.getElementById('total-ofertas');
        if (totalElement) {
            totalElement.textContent = count;
        }
    }

    // Inicializar tabla
    function initializeTable() {
        const table = document.getElementById('ofertas-table');
        if (table) {
            // Agregar eventos para botones de acción
            table.addEventListener('click', function(event) {
                const target = event.target.closest('.btn-icon');
                if (!target) return;

                const row = target.closest('tr');
                const id = row.cells[0].textContent;
                const nombre = row.cells[1].textContent;

                if (target.classList.contains('btn-edit')) {
                    editOferta(id, nombre);
                } else if (target.classList.contains('btn-view')) {
                    viewOferta(id, nombre);
                } else if (target.classList.contains('btn-delete')) {
                    deleteOferta(id, nombre);
                }
            });
        }
    }

    function editOferta(id, nombre) {
        console.log(`Editar oferta ${id}: ${nombre}`);
        // Implementar lógica de edición
    }

    function viewOferta(id, nombre) {
        console.log(`Ver oferta ${id}: ${nombre}`);
        // Llamar a la función global para abrir el modal de detalle
        if (window.verDetalleOferta) {
            window.verDetalleOferta(id);
        } else {
            console.error('❌ Función verDetalleOferta no disponible');
            showNotification('Error al abrir el detalle de la oferta', 'error');
        }
    }

    function deleteOferta(id, nombre) {
        ModalConfirmacion.show(
            'Confirmar Eliminación',
            `¿Está seguro de que desea eliminar la oferta "${nombre}"?`,
            () => {
                console.log(`Eliminar oferta ${id}: ${nombre}`);
                // Implementar lógica de eliminación
            }
        );
    }

    // Validación del formulario
    function validateForm() {
        const requiredFields = document.querySelectorAll('#oferta-form [required]');
        let isValid = true;

        requiredFields.forEach(field => {
            if (!field.value.trim()) {
                field.style.borderColor = 'var(--danger-color)';
                isValid = false;
            } else {
                field.style.borderColor = '';
            }
        });

        return isValid;
    }

    // Manejo del envío del formulario
    const form = document.getElementById('oferta-form');
    if (form) {
        form.addEventListener('submit', function(event) {
            if (!validateForm()) {
                event.preventDefault();
                alert('Por favor, complete todos los campos requeridos.');
                return;
            }

            // Aquí se puede agregar lógica adicional antes del envío
            console.log('Formulario válido, enviando...');
        });
    }

    // Actualizar la función submitForm para enviar datos correctamente
    function submitForm() {
        console.log('🚀 Enviando formulario...');
        
        const form = document.getElementById('oferta-form');
        const formData = new FormData(form);
        
        // Agregar categorías seleccionadas
        const categoriasSeleccionadas = Array.from(selectedCategories);
        categoriasSeleccionadas.forEach(categoria => {
            formData.append('categorias', categoria);
        });
        
        // Agregar docentes seleccionados (IDs separados por coma)
        const docentesTable = document.getElementById('docentes-table');
        if (docentesTable) {
            const docentesIds = [];
            const rows = docentesTable.getElementsByTagName('tbody')[0].rows;
            for (let i = 0; i < rows.length; i++) {
                const docenteId = rows[i].dataset.docenteId;
                if (docenteId) {
                    docentesIds.push(docenteId);
                }
            }
            formData.append('docentesIds', docentesIds.join(','));
        }
        
        // Agregar horarios como string estructurada
        const horariosTable = document.getElementById('horarios-table').getElementsByTagName('tbody')[0];
        const horariosArray = [];
        for (let i = 0; i < horariosTable.rows.length; i++) {
            const row = horariosTable.rows[i];
            const dia = row.cells[0].textContent;
            const horario = row.cells[1].textContent; // "08:00 - 10:00"
            const partes = horario.split(' - ');
            horariosArray.push(dia + ':' + partes[0] + '-' + partes[1]);
        }
        formData.append('horariosJson', horariosArray.join(','));
        
        // Determinar URL (registrar o modificar)
        const idOferta = document.getElementById('idOfertaModificar').value;
        const url = idOferta ? '/admin/ofertas/modificar' : '/admin/ofertas/registrar';
        
        console.log(`🚀 Enviando a ${url} (ID: ${idOferta})`);

        // Enviar al servidor
        fetch(url, {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification(idOferta ? '✅ Oferta modificada exitosamente' : '✅ Oferta registrada exitosamente', 'success');
                resetForm();
                hideForm();
                loadOfertas(); // Recargar tabla
            } else {
                showNotification('❌ Error: ' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('💥 Error:', error);
            showNotification('❌ Error al procesar la oferta', 'error');
        });
    }

    // Función para cargar ofertas en la tabla
    function loadOfertas() {
        console.log('🔄 Cargando ofertas desde el servidor...');
        
        fetch('/admin/ofertas/listar', {
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
                populateTable(data.data); // Corregido: usar data.data en lugar de data.ofertas
            } else {
                console.error('Error del servidor:', data.message);
                showNotification('Error al cargar ofertas: ' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error cargando ofertas:', error);
            showNotification('Error al cargar ofertas desde el servidor', 'error');
        });
    }

    // Función para poblar la tabla con datos
    function populateTable(ofertas) {
        console.log('📋 Poblando tabla con', ofertas.length, 'ofertas');
        
        const tableBody = document.querySelector('#ofertas-table tbody');
        if (!tableBody) {
            console.error('No se encontró el tbody de la tabla');
            return;
        }

        // Limpiar tabla existente
        tableBody.innerHTML = '';

        if (ofertas.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="10" class="text-center">No hay ofertas registradas</td>
                </tr>
            `;
            updateTableStats(0);
            return;
        }

        // Crear filas para cada oferta
        ofertas.forEach(oferta => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${oferta.idOferta || 'N/A'}</td>
                <td>${oferta.nombre || 'Sin nombre'}</td>
                <td><span class="status-badge status-${(oferta.tipo || 'curso').toLowerCase()}">${oferta.tipo || 'CURSO'}</span></td>
                <td><span class="status-badge status-${(oferta.modalidad || 'presencial').toLowerCase()}">${oferta.modalidad || 'PRESENCIAL'}</span></td>
                <td>${oferta.cupos || 'N/A'}</td>
                <td>${oferta.fechaInicio ? new Date(oferta.fechaInicio).toLocaleDateString() : 'N/A'}</td>
                <td>$${oferta.costoInscripcion || '0'}</td>
                <td>${oferta.otorgaCertificado ? '<i class="fas fa-check-circle text-success"></i>' : '<i class="fas fa-times-circle text-danger"></i>'}</td>
                <td><span class="status-badge status-${(oferta.estado || 'activa').toLowerCase()}">${oferta.estado || 'ACTIVA'}</span></td>
                <td class="actions">
                    <button class="btn-icon btn-view" onclick="viewOferta(${oferta.idOferta}, '${oferta.nombre}')" title="Ver">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn-icon btn-edit" onclick="editOferta(${oferta.idOferta}, '${oferta.nombre}')" title="Editar">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-icon btn-delete" onclick="deleteOferta(${oferta.idOferta}, '${oferta.nombre}')" title="Eliminar">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });

        updateTableStats(ofertas.length);
    }

    // Función para mostrar notificaciones
    function showNotification(message, type) {
        // Crear elemento de notificación
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <span>${message}</span>
                <button class="notification-close">&times;</button>
            </div>
        `;
        
        // Agregar al DOM
        document.body.appendChild(notification);
        
        // Auto-remover después de 5 segundos
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 5000);
        
        // Permitir cerrar manualmente
        notification.querySelector('.notification-close').addEventListener('click', () => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        });
    }

    // Inicializar búsqueda de docentes después de que el DOM esté listo
    setTimeout(initializeDocenteSearch, 100);

    // Cargar ofertas existentes al inicializar la página
    loadOfertas();

    console.log('Gestión de Ofertas inicializada correctamente');

    // ==========================================
    // LÓGICA DE EDICIÓN (Agregada dinámicamente)
    // ==========================================
    
    window.modificarOferta = function(id, nombre) {
        console.log(`📝 Modificando oferta ${id}: ${nombre}`);
        
        fetch(`/admin/ofertas/${id}`)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    cargarDatosEnFormulario(data.oferta);
                    showForm();
                    // Cambiar texto del botón
                    const btnSubmit = document.getElementById('btn-submit-text');
                    if (btnSubmit) btnSubmit.textContent = 'Guardar Cambios';
                } else {
                    showNotification('Error al cargar la oferta: ' + data.message, 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showNotification('Error al cargar la oferta', 'error');
            });
    };

    function cargarDatosEnFormulario(oferta) {
        console.log('📋 Cargando datos en formulario:', oferta);
        resetForm();
        
        // Campos ocultos
        document.getElementById('idOfertaModificar').value = oferta.id;
        
        // Campos básicos
        const tipoSelect = document.getElementById('tipoOferta');
        tipoSelect.value = oferta.tipo;
        // Disparar evento change para mostrar campos específicos
        tipoSelect.dispatchEvent(new Event('change'));
        
        document.getElementById('nombre').value = oferta.nombre;
        document.getElementById('descripcion').value = oferta.descripcion;
        document.getElementById('cupos').value = oferta.cupos;
        document.getElementById('costoInscripcion').value = oferta.costoInscripcion;
        document.getElementById('fechaInicio').value = oferta.fechaInicio;
        document.getElementById('fechaFin').value = oferta.fechaFin;
        
        const modalidadSelect = document.getElementById('modalidad');
        modalidadSelect.value = oferta.modalidad;
        // Actualizar visibilidad de lugar/enlace
        if (window.updateUbicacionFields) {
            window.updateUbicacionFields(oferta.modalidad);
        }
        
        // Llenar lugar y enlace
        if (document.getElementById('lugar')) document.getElementById('lugar').value = oferta.lugar || '';
        if (document.getElementById('enlace')) document.getElementById('enlace').value = oferta.enlace || '';
        
        // Imagen
        const imagePreview = document.getElementById('image-preview');
        const uploadPlaceholder = document.querySelector('.upload-placeholder');
        if (oferta.imagenUrl) {
            imagePreview.src = oferta.imagenUrl;
            imagePreview.style.display = 'block';
            if (uploadPlaceholder) uploadPlaceholder.style.display = 'none';
        } else {
            imagePreview.style.display = 'none';
            imagePreview.src = '';
            if (uploadPlaceholder) uploadPlaceholder.style.display = 'flex';
        }
        
        // Certificado
        const chkCertificado = document.getElementById('otorgaCertificado');
        if (chkCertificado) {
            chkCertificado.checked = (oferta.certificado === 'true' || oferta.certificado === true);
            chkCertificado.dispatchEvent(new Event('change'));
        }
        
        // Campos específicos según tipo
        if (oferta.tipo === 'CURSO') {
            if (document.getElementById('temario')) document.getElementById('temario').value = oferta.temario || '';
            if (document.getElementById('costoCuota')) document.getElementById('costoCuota').value = oferta.costoCuota || '';
            if (document.getElementById('nrCuotas')) document.getElementById('nrCuotas').value = oferta.nrCuotas || '';
            if (document.getElementById('diaVencimiento')) document.getElementById('diaVencimiento').value = oferta.diaVencimiento || '';
        } else if (oferta.tipo === 'FORMACION') {
            if (document.getElementById('planFormacion')) document.getElementById('planFormacion').value = oferta.plan || '';
            if (document.getElementById('costoCuota')) document.getElementById('costoCuota').value = oferta.costoCuota || '';
            if (document.getElementById('nrCuotas')) document.getElementById('nrCuotas').value = oferta.nrCuotas || '';
            if (document.getElementById('diaVencimiento')) document.getElementById('diaVencimiento').value = oferta.diaVencimiento || '';
        } else if (oferta.tipo === 'CHARLA') {
            if (document.getElementById('publicoObjetivo')) document.getElementById('publicoObjetivo').value = oferta.publicoObjetivo || '';
        } else if (oferta.tipo === 'SEMINARIO') {
            if (document.getElementById('publicoObjetivoSeminario')) document.getElementById('publicoObjetivoSeminario').value = oferta.publicoObjetivo || '';
        }
    }
});

}

// ================================================
// FUNCIONES PARA MODO AUTOMÁTICO DE HORARIOS
// ================================================

/**
 * Toggle entre modo manual y automático de horarios
 */
window.toggleModoHorario = function() {
    const modoSwitch = document.getElementById('modo-horario-switch');
    const modoLabel = document.getElementById('modo-label');
    const modoDescripcion = document.getElementById('modo-descripcion');
    const manualContainer = document.getElementById('horarios-manual-container');
    const automaticoContainer = document.getElementById('horarios-automatico-container');
    const limpiarHorarios = () => {
        if (typeof window.horariosSeleccionados !== 'undefined') {
            window.horariosSeleccionados = [];
        }
        if (typeof horariosArray !== 'undefined') {
            horariosArray = [];
        }
        if (typeof actualizarHorariosChips === 'function') {
            actualizarHorariosChips();
        }
        if (typeof actualizarListaHorarios === 'function') {
            actualizarListaHorarios();
        }
    };
    const cargarHorariosSeleccionadosEnManual = () => {
        if (!Array.isArray(window.horariosSeleccionados)) return;
        const normalizar = (valor) => {
            if (!valor) return '';
            return valor.length > 5 ? valor.substring(0, 5) : valor;
        };
        if (typeof horariosSeleccionados !== 'undefined') {
            const manualActual = Array.isArray(horariosSeleccionados) ? horariosSeleccionados : [];
            const autoActual = window.horariosSeleccionados.map(h => ({
                ...h,
                horaInicio: normalizar(h.horaInicio),
                horaFin: normalizar(h.horaFin)
            }));
            const key = (h) => `${h.dia}|${normalizar(h.horaInicio)}|${normalizar(h.horaFin)}|${h.docentesIds || ''}|${h.docenteId || ''}`;
            const map = new Map();
            manualActual.forEach(h => map.set(key(h), h));
            autoActual.forEach(h => map.set(key(h), h));
            horariosSeleccionados = Array.from(map.values());
        }
        if (typeof actualizarHorariosChips === 'function') {
            actualizarHorariosChips();
        }
        if (typeof actualizarListaHorarios === 'function') {
            actualizarListaHorarios();
        }
    };
    
    if (modoSwitch.checked) {
        // Modo Automático activado
        console.log('🤖 Cambiando a modo AUTOMÁTICO');
        modoLabel.textContent = 'Modo Automático';
        modoDescripcion.textContent = 'El sistema generará propuestas optimizadas';
        
        // Mostrar/ocultar contenedores
        manualContainer.style.display = 'none';
        automaticoContainer.style.display = 'block';
        
        // Mantener horarios manuales/autom??ticos existentes (no limpiar)
    } else {
        // Modo Manual activado
        console.log('✋ Cambiando a modo MANUAL');
        modoLabel.textContent = 'Modo Manual';
        modoDescripcion.textContent = 'Selecciona días y horarios manualmente';
        
        // Mostrar/ocultar contenedores
        manualContainer.style.display = 'block';
        automaticoContainer.style.display = 'none';
        
        // Limpiar propuestas automáticas
        const propuestasContainer = document.getElementById('propuestas-container');
        const previewContainer = document.getElementById('preview-horarios-automaticos');
        const infoDocenteContainer = document.getElementById('info-docente-carga');
        
        if (propuestasContainer) propuestasContainer.style.display = 'none';
        if (previewContainer) previewContainer.style.display = 'none';
        if (infoDocenteContainer) infoDocenteContainer.style.display = 'none';
        
        // Combinar horarios manuales con los seleccionados del modo automático
        cargarHorariosSeleccionadosEnManual();
    }
}

/**
 * Genera propuestas automáticas de horarios
 */


function syncPinStyles(container = document) {
    container.querySelectorAll('.pin-check').forEach(chk => {
        const chip = chk.closest('.horario-chip-editable');
        if (!chip) return;
        if (chk.checked) {
            chip.classList.add('pinned');
        } else {
            chip.classList.remove('pinned');
        }
    });
}

function marcarPinsUI(pinnedList) {
    if (!Array.isArray(pinnedList) || pinnedList.length === 0) return;
    const norm = (v) => (v && v.length > 5 ? v.substring(0, 5) : v || '00:00');
    const key = (h) => `${h.dia}|${norm(h.horaInicio)}|${norm(h.horaFin)}`;
    const set = new Set(pinnedList.map(key));

    document.querySelectorAll('.pin-check').forEach(chk => {
        const k = `${chk.dataset.dia}|${norm(chk.dataset.inicio)}|${norm(chk.dataset.fin)}`;
        if (set.has(k)) {
            chk.checked = true;
        }
    });
    syncPinStyles();
}

function buildPinnedFromSelected() {
    const normalizar = (valor) => {
        if (!valor) return '00:00';
        return valor.length > 5 ? valor.substring(0, 5) : valor;
    };

    const idx = window.propuestaSeleccionadaIndex;
    if (idx != null && window.propuestasGeneradas && window.propuestasGeneradas[idx]) {
        return window.propuestasGeneradas[idx].horarios.map(h => ({
            dia: h.dia,
            horaInicio: normalizar(h.horaInicio),
            horaFin: normalizar(h.horaFin),
            docenteId: h.docenteId || h.docenteUUID || '',
            docentesIds: Array.isArray(h.docentesIds) ? h.docentesIds.join(',') : (h.docentesIds || '')
        }));
    }

    if (Array.isArray(window.horariosSeleccionados) && window.horariosSeleccionados.length > 0) {
        return window.horariosSeleccionados.map(h => ({
            dia: h.dia,
            horaInicio: normalizar(h.horaInicio),
            horaFin: normalizar(h.horaFin),
            docenteId: h.docenteId || '',
            docentesIds: h.docentesIds || ''
        }));
    }

    return [];
}

async function generarPropuestasAutomaticas(pinnedSchedules = []) {
    // Si viene del evento onclick del bot?n, pinnedSchedules ser? un evento o undefined, lo normalizamos
    const pinnedFromSelected = buildPinnedFromSelected();
    const hasExplicitPinned = Array.isArray(pinnedSchedules) && pinnedSchedules.length > 0;
    const isRegeneration = hasExplicitPinned || pinnedFromSelected.length > 0;
    const finalPinned = hasExplicitPinned ? pinnedSchedules : pinnedFromSelected;

    console.log('?? Generando propuestas autom?ticas...', finalPinned);
    
    // Validaciones
    const idOferta = document.getElementById('idOferta')?.value;
    const horasSemanalesRaw = document.getElementById('horas-semanales').value;
    const horasSemanales = normalizeNumber(horasSemanalesRaw);
    const maxHorasRaw = document.getElementById('max-horas-diarias')?.value || 4;
    const maxHoras = parseInt(maxHorasRaw, 10);
    
    // Obtener docente seleccionado (del campo de docentes)
    let idDocente = null;
    const docentesCurso = document.getElementById('docentesCursoSelect') || document.getElementById('docentesCurso');
    const docentesFormacion = document.getElementById('docentesFormacionSelect') || document.getElementById('docentesFormacion');
    const docentesIdsRaw = (docentesCurso && docentesCurso.value) ? docentesCurso.value : (docentesFormacion && docentesFormacion.value) ? docentesFormacion.value : '';
    
    const pickFirstId = (val) => {
        if (!val) return null;
        const parts = val.split(',').map(v => v.trim()).filter(Boolean);
        return parts.length > 0 ? parts[0] : null;
    };

    if (docentesCurso && docentesCurso.value) {
        idDocente = pickFirstId(docentesCurso.value);
    } else if (docentesFormacion && docentesFormacion.value) {
        idDocente = pickFirstId(docentesFormacion.value);
    }
    
    // Validar campos
    if (!idDocente) {
        mostrarAlerta('Por favor, selecciona un docente primero', 'warning');
        return;
    }
    if (!isUuid(idDocente)) {
        mostrarAlerta('El docente seleccionado no tiene un ID v??lido. Re-selecciona el docente.', 'error');
        console.error('ID docente inv??lido:', idDocente);
        return;
    }
    
    if (!horasSemanales || isNaN(horasSemanales) || horasSemanales <= 0) {
        mostrarAlerta('Por favor, ingresa las horas semanales requeridas (ej: 6 o 6.5)', 'warning');
        return;
    }

    if (isNaN(maxHoras) || maxHoras < 2) {
        mostrarAlerta('El m??nimo de horas diarias es 2', 'warning');
        return;
    }

    if (maxHoras < 2) {
        mostrarAlerta('El mínimo de horas diarias es 2', 'warning');
        return;
    }
    
    // Mostrar loading
    mostrarLoading(isRegeneration ? 'Refinando propuesta...' : 'Generando propuestas optimizadas...');
    
    try {
        const formData = new FormData();
        formData.append('idOferta', idOferta || 0); // Si es nueva oferta, enviar 0
        formData.append('idDocente', idDocente);
        if (docentesIdsRaw) formData.append('docentesIds', docentesIdsRaw);
        formData.append('horasSemanales', String(horasSemanales));
        formData.append('maxHorasDiarias', String(maxHoras));

        if (isRegeneration) {
            formData.append('horariosFijadosJson', JSON.stringify(finalPinned));
            formData.append('buscarAlternativas', 'true');
        } else {
             const container = document.getElementById('propuestas-container');
             if (container && container.style.display !== 'none' && window.propuestasGeneradas && window.propuestasGeneradas.length > 0) {
                 formData.append('buscarAlternativas', 'true');
             }
        }
        
        const csrfToken = getCsrfToken();
        const csrfHeader = getCsrfHeaderName();
        const headers = {};
        if (csrfToken) headers[csrfHeader] = csrfToken;

        const response = await fetch('/admin/ofertas/generar-horarios-automaticos', {
            method: 'POST',
            headers,
            body: formData
        });
        
        let data = null;
        let rawText = '';
        if (response.ok) {
            data = await response.json();
        } else {
            const contentType = response.headers.get('content-type') || '';
            if (contentType.includes('application/json')) {
                data = await response.json();
            } else {
                rawText = await response.text();
                data = { success: false, message: rawText || ('HTTP ' + response.status) };
            }
        }

        if (data && data.success) {
            console.log('??? Propuestas generadas:', data);
            mostrarPropuestas(data.propuestas, data.infoDocente);
            ocultarLoading();
            mostrarAlerta('Propuestas generadas exitosamente', 'success');
        } else {
            ocultarLoading();
            let msg = data && data.message ? data.message : ('Error al generar propuestas (HTTP ' + response.status + ')');
            if (data && data.debug) {
                const d = data.debug;
                msg += ` Disponibilidad libre: ${d.disponibilidadLibre}h (total ${d.disponibilidadTotal}h). ` +
                       `Carga actual: ${d.cargaActual}h. Requeridas: ${d.horasRequeridas}h.`;
            }
            mostrarAlerta(msg, 'error');
            console.error('??? Respuesta error:', { status: response.status, data, rawText });
        }
    } catch (error) {
        console.error('❌ Error:', error);
        ocultarLoading();
        mostrarAlerta('Error al generar propuestas: ' + error.message, 'error');
    }
}

/**
 * Muestra las propuestas generadas
 */
function mostrarPropuestas(propuestas, infoDocente) {
    const listaPropuestas = document.getElementById('lista-propuestas');
    const propuestasContainer = document.getElementById('propuestas-container');
    const infoDocenteDiv = document.getElementById('info-docente-carga');
    
    // Guardar info docente global para validaciones
    window.infoDocenteActual = infoDocente;
    window.infoDocentesMap = null;
    if (infoDocente && infoDocente.docentes) {
        window.infoDocentesMap = {};
        infoDocente.docentes.forEach(d => {
            window.infoDocentesMap[d.id] = d;
        });
    }
    window.propuestaSeleccionadaIndex = null;
    
    // Mostrar información del docente
    if (infoDocente) {
        const grid = document.getElementById('docentes-info-grid');
        if (grid && infoDocente.docentes && Array.isArray(infoDocente.docentes)) {
            grid.innerHTML = infoDocente.docentes.map(d => `
                <div class="docente-card">
                    <h6>${d.nombre || '-'}</h6>
                    <div class="docente-metrics">
                        <span>Carga: ${d.cargaActual || 0}h</span>
                        <span>Disp: ${d.disponibilidadTotal || 0}h</span>
                    </div>
                </div>
            `).join('');
        }
        if (infoDocenteDiv) infoDocenteDiv.style.display = 'block';
    }
    
    // Limpiar propuestas anteriores
    listaPropuestas.innerHTML = '';
    
    // Crear cards de propuestas
    propuestas.forEach((propuesta, index) => {
        const card = document.createElement('div');
        card.id = `propuesta-card-${index}`;
        card.className = 'propuesta-card';
        card.innerHTML = `
            <div class="propuesta-header">
                <div>
                    <h6 style="margin: 0; color: #2c3e50;">
                        <i class="fas fa-calendar-alt"></i> ${propuesta.nombre}
                    </h6>
                    <small style="color: #6c757d;">${propuesta.descripcion}</small>
                </div>
                <div class="score-badge" style="background: ${getScoreColor(propuesta.score)};">
                    ${propuesta.score}
                </div>
            </div>
            
            <div class="propuesta-stats">
                <div class="stat-item">
                    <i class="fas fa-clock"></i>
                    <span>${propuesta.totalHorasSemana}h/sem</span>
                </div>
                <div class="stat-item">
                    <i class="fas fa-calendar-day"></i>
                    <span>${propuesta.cantidadDias} días</span>
                </div>
                <div class="stat-item">
                    <i class="fas fa-hourglass-half"></i>
                    <span>${propuesta.promedioHorasPorDia}h/día</span>
                </div>
            </div>
            
            <div class="propuesta-horarios">
                <div style="margin-bottom:8px; font-size:0.8rem; color:#6c757d;">
                    <i class="fas fa-thumbtack"></i> Edita horarios manualmente si lo deseas. Marca casillas para mantener al regenerar:
                </div>
                <div style="display:flex; flex-wrap:wrap;">
                ${propuesta.horarios.map((h, hIdx) => {
                    // Asegurar formato HH:mm para input type="time"
                    const inicio = h.horaInicio.length > 5 ? h.horaInicio.substring(0, 5) : h.horaInicio;
                    const fin = h.horaFin.length > 5 ? h.horaFin.substring(0, 5) : h.horaFin;
                    
                    return `
                    <div class="horario-chip-editable" data-prop-index="${index}" data-h-index="${hIdx}" style="display:inline-flex; flex-direction:column; gap:4px; margin:4px; background:#f8f9fa; border-radius:8px; padding:6px 10px; border:1px solid #dee2e6;">
                        <div style="display:flex; align-items:center; gap:6px; margin-bottom:2px;">
                             <input type="checkbox" class="pin-check"
                                   data-dia="${h.dia}" data-inicio="${inicio}" data-fin="${fin}" data-docente-id="${h.docenteId || ''}" data-docentes-ids="${Array.isArray(h.docentesIds) ? h.docentesIds.join(',') : (h.docentesIds || '')}"
                                   title="Mantener este horario">
                             <span style="font-weight:600; font-size:0.85rem; color:#495057;">${formatearDia(h.dia)}</span>
                             ${(h.docentesNombres || h.docenteNombre) ? `<span style=\"font-size:0.75rem;color:#6b7280;\">(${h.docentesNombres || h.docenteNombre})</span>` : ''}
                        </div>
                        <div style="display:flex; align-items:center; gap:4px;">
                            <input type="time" value="${inicio}"
                                   data-field="inicio"
                                   onchange="actualizarHorarioPropuesta(${index}, ${hIdx}, 'inicio', this)"
                                   style="border:1px solid #ced4da; border-radius:4px; font-size:0.85rem; padding:2px; color:#495057; width:65px;">
                            <span style="color:#6c757d;">-</span>
                            <input type="time" value="${fin}"
                                   data-field="fin"
                                   onchange="actualizarHorarioPropuesta(${index}, ${hIdx}, 'fin', this)"
                                   style="border:1px solid #ced4da; border-radius:4px; font-size:0.85rem; padding:2px; color:#495057; width:65px;">
                        </div>
                    </div>
                    `;
                }).join('')}
                </div>
            </div>
            
            <div class="propuesta-actions" style="margin-top:15px; display:flex; gap:10px;">
                <button type="button" class="btn-seleccionar-propuesta" onclick="seleccionarPropuesta(${index})" style="flex:2;">
                    <i class="fas fa-check"></i> Seleccionar
                </button>
                <button type="button" class="btn-regenerar-propuesta" onclick="regenerarConFijados(${index})" 
                        style="flex:1; background:white; color:#6c757d; border:1px solid #dee2e6; border-radius:8px; cursor:pointer;" 
                        title="Buscar alternativas manteniendo seleccionados"
                        onmouseover="this.style.background='#f8f9fa'"
                        onmouseout="this.style.background='white'">
                    <i class="fas fa-sync-alt"></i> Refinar
                </button>
            </div>
        `;
        listaPropuestas.appendChild(card);
    });
    
    // Mostrar container de propuestas
    propuestasContainer.style.display = 'block';
    
    // Guardar propuestas en variable global para uso posterior
    window.propuestasGeneradas = propuestas;

    // Marcar autom?ticamente los horarios fijados en la UI
    try {
        marcarPinsUI(buildPinnedFromSelected());
    } catch (e) {
        console.warn('No se pudo marcar pins:', e);
    }

    // Delegaci?n para mantener estilos de pin
    if (!listaPropuestas.dataset.pinListener) {
        listaPropuestas.addEventListener('change', function(e) {
            if (e.target && e.target.classList.contains('pin-check')) {
                syncPinStyles(listaPropuestas);
            }
        });
        listaPropuestas.dataset.pinListener = '1';
    }

    // Marcar autom?ticamente los horarios fijados en la UI
    try {
        marcarPinsUI(buildPinnedFromSelected());
    } catch (e) {
        console.warn('No se pudo marcar pins:', e);
    }
}

/**
 * Selecciona una propuesta y asigna sus horarios
 */
function seleccionarPropuesta(index) {
    const propuesta = window.propuestasGeneradas[index];
    console.log('? Propuesta seleccionada:', propuesta);

    // Toggle: si se vuelve a seleccionar la misma propuesta, deseleccionar
    if (window.propuestaSeleccionadaIndex === index) {
        window.propuestaSeleccionadaIndex = null;
        // Rehabilitar edici?n en la propuesta que se deselecciona
        habilitarEdicionPropuesta(true, index);
        // Desmarcar estilos de selecci?n
        document.querySelectorAll('.propuesta-card').forEach((card) => {
            card.style.border = '1px solid #dee2e6';
            card.style.background = 'white';
        });
        mostrarAlerta('Propuesta deseleccionada', 'info');
        return;
    }

    window.propuestaSeleccionadaIndex = index;

    const card = document.querySelectorAll('.propuesta-card')[index];
    const checks = card ? card.querySelectorAll('.pin-check:checked') : [];

    if (!checks || checks.length === 0) {
        mostrarAlerta('Debes marcar con el check los horarios que deseas seleccionar', 'warning');
        return;
    }

    const normalizar = (valor) => {
        if (!valor) return '00:00';
        return valor.length > 5 ? valor.substring(0, 5) : valor;
    };

    // Validar disponibilidad antes de seleccionar
    for (const c of checks) {
        const dia = c.dataset.dia;
        const inicio = normalizar(c.dataset.inicio);
        const fin = normalizar(c.dataset.fin);
        let dispoDia = window.infoDocenteActual?.disponibilidadDetallada?.[dia];
        const docenteId = c.dataset.docenteId || '';
        const docentesIds = (c.dataset.docentesIds || '').split(',').map(s => s.trim()).filter(Boolean);
        const idsToCheck = docentesIds.length > 0 ? docentesIds : (docenteId ? [docenteId] : []);
        let ok = true;
        if (idsToCheck.length > 0 && window.infoDocentesMap) {
            for (const id of idsToCheck) {
                const map = window.infoDocentesMap[id];
                if (!map) continue;
                const dd = map.disponibilidadDetallada?.[dia];
                if (dd && !dd.some(r => inicio >= normalizar(r.inicio) && fin <= normalizar(r.fin))) {
                    const rangos = dd.map(r => `${normalizar(r.inicio)}-${normalizar(r.fin)}`).join(', ');
                    mostrarAlerta(`El rango ${inicio}-${fin} no est? dentro de la disponibilidad del docente ${map.nombre || ''} para ${formatearDia(dia)}. Disponibilidad: ${rangos}`, 'error');
                    ok = false;
                    break;
                }
            }
        } else if (dispoDia) {
            const dentro = dispoDia.some(r => inicio >= normalizar(r.inicio) && fin <= normalizar(r.fin));
            if (!dentro) {
                const rangos = dispoDia.map(r => `${normalizar(r.inicio)}-${normalizar(r.fin)}`).join(', ');
                mostrarAlerta(`El rango ${inicio}-${fin} no est? dentro de la disponibilidad del docente para ${formatearDia(dia)}. Disponibilidad: ${rangos}`, 'error');
                ok = false;
            }
        }
        if (!ok) return;
        if (dispoDia) {
            const dentro = dispoDia.some(r => inicio >= normalizar(r.inicio) && fin <= normalizar(r.fin));
            if (!dentro) {
                const rangos = dispoDia.map(r => `${normalizar(r.inicio)}-${normalizar(r.fin)}`).join(', ');
                mostrarAlerta(`El rango ${inicio}-${fin} no est? dentro de la disponibilidad del docente para ${formatearDia(dia)}. Disponibilidad: ${rangos}`, 'error');
                return;
            }
        }
    }
    

    // Limpiar horarios anteriores
    if (typeof window.horariosSeleccionados !== 'undefined') {
        window.horariosSeleccionados = [];
    }
    if (typeof horariosArray !== 'undefined') {
        horariosArray = [];
    }

    // Agregar solo horarios marcados
    checks.forEach(c => {
        const item = {
            dia: c.dataset.dia,
            horaInicio: normalizar(c.dataset.inicio) + ':00',
            horaFin: normalizar(c.dataset.fin) + ':00',
            docenteId: c.dataset.docenteId || '',
            docentesIds: c.dataset.docentesIds || ''
        };
        if (typeof window.horariosSeleccionados !== 'undefined') {
            window.horariosSeleccionados.push(item);
        }
        if (typeof horariosArray !== 'undefined') {
            horariosArray.push(item);
        }
    });
    const hidden = document.getElementById('horarios');
    if (hidden && typeof window.horariosSeleccionados !== 'undefined') {
        hidden.value = JSON.stringify(window.horariosSeleccionados);
    }

    // Actualizar vista previa
    mostrarPreviewHorariosAutomaticos(propuesta, index);

    // Marcar visualmente la propuesta seleccionada
    document.querySelectorAll('.propuesta-card').forEach((cardEl, i) => {
        if (i === index) {
            cardEl.style.border = '2px solid #28a745';
            cardEl.style.background = '#f0fff4';
        } else {
            cardEl.style.border = '1px solid #dee2e6';
            cardEl.style.background = 'white';
        }
    });

    // Bloquear edici?n hasta deseleccionar
    habilitarEdicionPropuesta(false);

    mostrarAlerta(`Propuesta "${propuesta.nombre}" seleccionada`, 'success');
}

function habilitarEdicionPropuesta(enabled, indexOverride = null) {
    const idx = indexOverride != null ? indexOverride : window.propuestaSeleccionadaIndex;
    const card = idx != null ? document.querySelectorAll('.propuesta-card')[idx] : null;
    const preview = document.getElementById('preview-horarios-automaticos');

    if (card) {
        card.querySelectorAll('input[type="time"]').forEach(inp => inp.disabled = !enabled);
        card.querySelectorAll('.pin-check').forEach(chk => chk.disabled = !enabled);
    }
    if (preview) {
        preview.querySelectorAll('input[type="time"]').forEach(inp => inp.disabled = !enabled);
    }
}

function mostrarPreviewHorariosAutomaticos(propuesta, propIndex) {
    const previewContainer = document.getElementById('preview-horarios-automaticos');
    const previewChips = document.getElementById('preview-horarios-chips');
    
    previewChips.innerHTML = '';

    let horariosPreview = propuesta.horarios || [];
    if (propIndex != null) {
        const card = document.querySelectorAll('.propuesta-card')[propIndex];
        const checks = card ? card.querySelectorAll('.pin-check:checked') : [];
        if (checks && checks.length > 0) {
            horariosPreview = Array.from(checks).map(c => ({
                dia: c.dataset.dia,
                horaInicio: (c.dataset.inicio || '') + ':00',
                horaFin: (c.dataset.fin || '') + ':00',
                docenteId: c.dataset.docenteId || '',
                docentesIds: c.dataset.docentesIds || ''
            }));
        }
    }

    horariosPreview.forEach((h, hIdx) => {
        const inicio = h.horaInicio.length > 5 ? h.horaInicio.substring(0, 5) : h.horaInicio;
        const fin = h.horaFin.length > 5 ? h.horaFin.substring(0, 5) : h.horaFin;

        const chip = document.createElement('div');
        chip.className = 'horario-chip-editable preview';
        chip.dataset.propIndex = propIndex;
        chip.dataset.hIndex = hIdx;
        chip.innerHTML = `
            <div style="font-weight:600; font-size:0.85rem; color:#004085;">${formatearDia(h.dia)}${(h.docentesNombres || h.docenteNombre) ? ` <span style=\"font-size:0.75rem;color:#6b7280;\">(${h.docentesNombres || h.docenteNombre})</span>` : ''}</div>
            <div style="display:flex; align-items:center; gap:4px;">
                <input type="time" value="${inicio}" data-field="inicio"
                       onchange="actualizarHorarioPropuesta(${propIndex}, ${hIdx}, 'inicio', this, true)"
                       style="border:1px solid #b3d7ff; border-radius:4px; font-size:0.85rem; padding:2px; color:#004085; width:70px;">
                <span style="color:#6c757d;">-</span>
                <input type="time" value="${fin}" data-field="fin"
                       onchange="actualizarHorarioPropuesta(${propIndex}, ${hIdx}, 'fin', this, true)"
                       style="border:1px solid #b3d7ff; border-radius:4px; font-size:0.85rem; padding:2px; color:#004085; width:70px;">
            </div>
        `;
        previewChips.appendChild(chip);
    });
    
    previewContainer.style.display = 'block';
}

/**
 * Formatea el día para mostrar
 */
function formatearDia(dia) {
    const dias = {
        'LUNES': 'Lun',
        'MARTES': 'Mar',
        'MIERCOLES': 'Mié',
        'JUEVES': 'Jue',
        'VIERNES': 'Vie',
        'SABADO': 'Sáb',
        'DOMINGO': 'Dom'
    };
    return dias[dia] || dia;
}

/**
 * Retorna color según el score
 */
function getScoreColor(score) {
    if (score >= 90) return 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
    if (score >= 75) return 'linear-gradient(135deg, #00b4db 0%, #0083b0 100%)';
    return 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)';
}


/**
 * Regenera propuestas manteniendo los horarios fijados (checkboxes)
 */
function regenerarConFijados(index) {
    const card = document.querySelectorAll('.propuesta-card')[index];
    const checks = card.querySelectorAll('.pin-check:checked');
    const pinned = [];
    
    checks.forEach(c => {
        pinned.push({
            dia: c.dataset.dia,
            horaInicio: c.dataset.inicio,
            horaFin: c.dataset.fin,
            docenteId: c.dataset.docenteId || '',
            docentesIds: c.dataset.docentesIds || ''
        });
    });
    
    // Si no hay nada seleccionado, es como un 'shuffle' normal
    // Pero si hay seleccionados, la logica de backend excluye esos dias/horas y busca otros.
    generarPropuestasAutomaticas(pinned);
}


/**
 * Actualiza y valida el horario modificado manualmente en una propuesta
 */
function actualizarHorarioPropuesta(propIndex, horarioIndex, tipo, input, fromPreview = false) {
    const propuesta = window.propuestasGeneradas?.[propIndex];
    if (!propuesta) return;

    const horario = propuesta.horarios?.[horarioIndex];
    if (!horario) return;

    const normalizar = (valor) => {
        if (!valor) return '00:00';
        return valor.length > 5 ? valor.substring(0, 5) : valor;
    };

    const nuevoValor = normalizar(input.value);
    const inicioActual = normalizar(tipo === 'inicio' ? nuevoValor : horario.horaInicio);
    const finActual = normalizar(tipo === 'fin' ? nuevoValor : horario.horaFin);

    if (inicioActual >= finActual) {
        alert('La hora de inicio debe ser anterior a la de fin');
        input.value = tipo === 'inicio' ? normalizar(horario.horaInicio) : normalizar(horario.horaFin);
        return;
    }

    let dispoDia = window.infoDocenteActual?.disponibilidadDetallada?.[horario.dia];
    const docentesIds = (horario.docentesIds || '').split(',').map(s => s.trim()).filter(Boolean);
    const idsToCheck = docentesIds.length > 0 ? docentesIds : (horario.docenteId ? [horario.docenteId] : []);
    if (idsToCheck.length > 0 && window.infoDocentesMap) {
        for (const id of idsToCheck) {
            const map = window.infoDocentesMap[id];
            if (!map) continue;
            const dd = map.disponibilidadDetallada?.[horario.dia];
            if (dd && !dd.some(r => {
                const inicioR = normalizar(r.inicio);
                const finR = normalizar(r.fin);
                return inicioActual >= inicioR && finActual <= finR;
            })) {
                const rangos = dd.map(r => `${normalizar(r.inicio)}-${normalizar(r.fin)}`).join(', ');
                mostrarAlerta(`El rango ${inicioActual}-${finActual} no est? dentro de la disponibilidad del docente ${map.nombre || ''} para el ${formatearDia(horario.dia)}.\nDisponibilidad: ${rangos}`, 'error');
                input.value = tipo === 'inicio' ? normalizar(horario.horaInicio) : normalizar(horario.horaFin);
                return;
            }
        }
    } else if (dispoDia) {
        const dentroDeRango = dispoDia.some(r => {
            const inicioR = normalizar(r.inicio);
            const finR = normalizar(r.fin);
            return inicioActual >= inicioR && finActual <= finR;
        });
        if (!dentroDeRango) {
            const rangos = dispoDia.map(r => `${normalizar(r.inicio)}-${normalizar(r.fin)}`).join(', ');
            mostrarAlerta(`El rango ${inicioActual}-${finActual} no est? dentro de la disponibilidad del docente para el ${formatearDia(horario.dia)}.\nDisponibilidad: ${rangos}`, 'error');
            input.value = tipo === 'inicio' ? normalizar(horario.horaInicio) : normalizar(horario.horaFin);
            return;
        }
    }
    if (dispoDia) {
        const dentroDeRango = dispoDia.some(r => {
            const inicioR = normalizar(r.inicio);
            const finR = normalizar(r.fin);
            return inicioActual >= inicioR && finActual <= finR;
        });
        if (!dentroDeRango) {
            const rangos = dispoDia.map(r => `${normalizar(r.inicio)}-${normalizar(r.fin)}`).join(', ');
            mostrarAlerta(`El rango ${inicioActual}-${finActual} no est? dentro de la disponibilidad del docente para el ${formatearDia(horario.dia)}.
            Disponibilidad: ${rangos}`, 'error');
            input.value = tipo === 'inicio' ? normalizar(horario.horaInicio) : normalizar(horario.horaFin);
            return;
        }
    }

    if (tipo === 'inicio') {
        horario.horaInicio = `${nuevoValor}:00`;
    } else {
        horario.horaFin = `${nuevoValor}:00`;
    }

    const card = document.querySelectorAll('.propuesta-card')[propIndex];
    const cardWrapper = card?.querySelector(`.horario-chip-editable[data-h-index="${horarioIndex}"]`);
    const cardInput = cardWrapper?.querySelector(`input[data-field="${tipo}"]`);

    if (fromPreview && cardInput) {
        cardInput.value = nuevoValor;
    }

    if (!fromPreview && window.propuestaSeleccionadaIndex === propIndex) {
        const previewInput = document.querySelector(`.horario-chip-editable.preview[data-prop-index="${propIndex}"][data-h-index="${horarioIndex}"] input[data-field="${tipo}"]`);
        if (previewInput) {
            previewInput.value = nuevoValor;
        }
    }

    if (window.propuestaSeleccionadaIndex === propIndex) {
        const cardSel = document.querySelectorAll('.propuesta-card')[propIndex];
        const checksSel = cardSel ? cardSel.querySelectorAll('.pin-check:checked') : [];
        const normalizarSel = (valor) => {
            if (!valor) return '00:00';
            return valor.length > 5 ? valor.substring(0, 5) : valor;
        };
        const seleccionados = Array.from(checksSel).map(c => ({
            dia: c.dataset.dia,
            horaInicio: normalizarSel(c.dataset.inicio) + ':00',
            horaFin: normalizarSel(c.dataset.fin) + ':00',
            docenteId: c.dataset.docenteId || '',
            docentesIds: c.dataset.docentesIds || ''
        }));
        if (typeof horariosArray !== 'undefined') {
            horariosArray = seleccionados;
        }
        if (typeof window.horariosSeleccionados !== 'undefined') {
            window.horariosSeleccionados = seleccionados;
        }
        const hidden = document.getElementById('horarios');
        if (hidden && typeof window.horariosSeleccionados !== 'undefined') {
            hidden.value = JSON.stringify(window.horariosSeleccionados);
        }
        if (typeof actualizarHorariosChips === 'function') {
            actualizarHorariosChips();
        }
    }

    const check = cardWrapper?.querySelector('.pin-check');
    if (check) {
        check.dataset.inicio = inicioActual;
        check.dataset.fin = finActual;
    }

    console.log('✅ Horario actualizado manualmente:', horario);
}

/**
 * Aplica la propuesta seleccionada (o la propuesta indicada) a los horarios manuales
 */
function aplicarPropuestaSeleccionada(propIndex) {
    if (propIndex == null) {
        mostrarAlerta('No hay propuesta seleccionada', 'warning');
        return;
    }

    const propuesta = window.propuestasGeneradas?.[propIndex];
    if (!propuesta) {
        mostrarAlerta('Propuesta no encontrada', 'error');
        return;
    }

    if (!Array.isArray(window.horariosSeleccionados) || window.horariosSeleccionados.length === 0) {
        mostrarAlerta('Debes marcar con el check los horarios que deseas seleccionar', 'warning');
        return;
    }

    // Sincronizar con la estructura que usa el template (solo seleccionados)
    const hidden = document.getElementById('horarios');
    if (hidden) hidden.value = JSON.stringify(window.horariosSeleccionados);
    if (typeof actualizarHorariosChips === 'function') actualizarHorariosChips();

    mostrarAlerta('Horarios aplicados al formulario', 'success');
}


