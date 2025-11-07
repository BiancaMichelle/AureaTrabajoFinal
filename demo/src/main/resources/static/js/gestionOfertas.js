document.addEventListener('DOMContentLoaded', function () {
    // Referencias a elementos principales
    const btnShowForm = document.getElementById('btn-show-form');
    const formContainer = document.getElementById('form-container');
    const btnCloseForm = document.getElementById('btn-close-form');
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

    // Elemento para categorías
    const btnAsignarCategorias = document.getElementById('btn-asignar-categorias');

    // ========== MODAL DE CATEGORÍAS ==========
    
    const categoriaModal = {
        element: document.getElementById('categoriaModal'),
        
        show() {
            if (this.element) {
                this.element.style.display = 'flex';
                document.body.style.overflow = 'hidden';
                // Cargar categorías cuando se abre el modal
                console.log('📂 Abriendo modal, cargando categorías...');
                categoriaController.cargarCategorias();
            } else {
                console.error('Modal de categorías no encontrado');
            }
        },
        
        hide() {
            if (this.element) {
                this.element.style.display = 'none';
                document.body.style.overflow = 'auto';
                categoriaController.cancelarEdicion();
            }
        }
    };

    const categoriaController = {
        isEditing: false,
        

        async cargarCategorias() {
            try {
                console.log('📡 Iniciando carga de categorías...');
                this.mostrarLoading(true);
                
                const response = await fetch('/api/categorias', {
                    credentials: 'include'
                });
                
                console.log('📨 Respuesta de /api/categorias:', response.status, response.statusText);
                
                if (!response.ok) {
                    throw new Error(`Error ${response.status}: ${response.statusText}`);
                }
                
                const categorias = await response.json();
                console.log('✅ Categorías recibidas:', categorias);
                
                this.mostrarCategorias(categorias);
                
            } catch (error) {
                console.error('💥 Error cargando categorías:', error);
                this.mostrarError('Error al cargar las categorías: ' + error.message);
            } finally {
                this.mostrarLoading(false);
            }
        },

        mostrarCategorias(categorias) {
            console.log('🎨 Mostrando categorías en la tabla...');
            
            const tbody = document.getElementById('categoriasTableBody');
            const table = document.getElementById('categoriasTable');
            const loading = document.getElementById('categoriasLoading');
            const contador = document.getElementById('contadorCategorias');
            
            if (!tbody) {
                console.error('❌ No se encontró categoriasTableBody');
                return;
            }
            
            // Ocultar loading
            if (loading) {
                loading.style.display = 'none';
            }
            
            tbody.innerHTML = '';
            
            if (!categorias || categorias.length === 0) {
                console.log('📭 No hay categorías para mostrar');
                tbody.innerHTML = `
                    <tr>
                        <td colspan="3" class="text-center text-muted">
                            <i class="fas fa-inbox"></i> No hay categorías registradas
                        </td>
                    </tr>
                `;
            } else {
                console.log(`📊 Mostrando ${categorias.length} categorías`);
                categorias.forEach((categoria) => {
                    const tr = document.createElement('tr');
                    // Escapar comillas para evitar errores
                    const nombreEscapado = (categoria.nombre || '').replace(/'/g, "\\'");
                    const descripcionEscapada = (categoria.descripcion || '').replace(/'/g, "\\'");
                    
                    tr.innerHTML = `
                        <td>
                            <strong>${categoria.nombre || 'Sin nombre'}</strong>
                        </td>
                        <td>${categoria.descripcion || 'Sin descripción'}</td>
                        <td class="text-center">
                            <div class="btn-group btn-group-sm" role="group">
                                <button type="button" class="btn btn-outline-primary" 
                                        onclick="categoriaController.editarCategoria(${categoria.idCategoria}, '${nombreEscapado}', '${descripcionEscapada}')"
                                        title="Editar categoría">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <button type="button" class="btn btn-outline-danger" 
                                        onclick="categoriaController.eliminarCategoria(${categoria.idCategoria})"
                                        title="Eliminar categoría">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            }
            
            if (contador) {
                contador.textContent = `Total: ${categorias ? categorias.length : 0} categorías`;
            }
            
            if (table) {
                table.style.display = 'table';
                console.log('✅ Tabla de categorías mostrada');
            }
        },

        mostrarLoading(mostrar) {
            const loading = document.getElementById('categoriasLoading');
            const table = document.getElementById('categoriasTable');
            
            if (loading) {
                loading.style.display = mostrar ? 'block' : 'none';
            }
            if (table && !mostrar) {
                table.style.display = 'table';
            }
        },

        mostrarError(mensaje) {
            const errorDiv = document.getElementById('categoriasError');
            if (errorDiv) {
                errorDiv.textContent = mensaje;
                errorDiv.style.display = 'block';
                
                setTimeout(() => {
                    errorDiv.style.display = 'none';
                }, 5000);
            }
        },

        mostrarAlerta(mensaje, tipo = 'success') {
            const alertDiv = document.getElementById('categoriaAlert');
            if (alertDiv) {
                const alertClass = tipo === 'success' ? 'alert-success' : 'alert-danger';
                const icon = tipo === 'success' ? 'check' : 'exclamation';
                
                alertDiv.innerHTML = `
                    <div class="alert ${alertClass}">
                        <i class="fas fa-${icon}-circle"></i>
                        ${mensaje}
                    </div>
                `;
                alertDiv.style.display = 'block';
                
                console.log(`📢 Alerta [${tipo}]: ${mensaje}`);
                
                // Auto-ocultar después de 5 segundos
                setTimeout(() => {
                    alertDiv.innerHTML = '';
                    alertDiv.style.display = 'none';
                }, 5000);
            } else {
                console.error('No se encontró el contenedor de alertas');
                // Fallback: usar alert nativo
                alert(mensaje);
            }
        },

        
        async guardarCategoria() {
            console.log('💾 Guardando categoría...');
            
            const nombre = document.getElementById('categoriaNombre').value.trim();
            const descripcion = document.getElementById('categoriaDescripcion').value.trim();
            const id = document.getElementById('categoriaId').value;
        
            if (!nombre || !descripcion) {
                alert('Por favor completa todos los campos');
                return;
            }
        
            try {
                const categoriaData = { nombre, descripcion };
                let url = '/api/categorias';
                let method = 'POST';
        
                // SI hay ID, es una edición
                if (id) {
                    url = `/api/categorias/${id}`;
                    method = 'PUT';
                    categoriaData.idCategoria = parseInt(id);
                    console.log('✏️ Editando categoría existente ID:', id);
                } else {
                    console.log('🆕 Creando nueva categoría');
                }
                
                const headers = {
                    'Content-Type': 'application/json'
                };
                
                const response = await fetch(url, {
                    method: method,
                    headers: headers,
                    credentials: 'include',
                    body: JSON.stringify(categoriaData)
                });
        
                if (response.ok) {
                    const categoriaGuardada = await response.json();
                    
                    if (id) {
                        alert('✅ Categoría actualizada correctamente!');
                        console.log('✅ Categoría actualizada:', categoriaGuardada);
                    } else {
                        alert('✅ Categoría creada correctamente!');
                        console.log('✅ Categoría creada:', categoriaGuardada);
                    }
                    
                    // LIMPIAR FORMULARIO Y RESTABLECER BOTÓN
                    this.cancelarEdicion();
                    
                    // Recargar la lista
                    this.cargarCategorias();
                } else {
                    throw new Error('Error del servidor: ' + response.status);
                }
        
            } catch (error) {
                console.error('Error:', error);
                alert('❌ Error: ' + error.message);
            }
        },

        editarCategoria(id, nombre, descripcion) {
            console.log('✏️ Iniciando edición de categoría ID:', id);
            this.isEditing = true;
            
            // Establecer valores en el formulario
            document.getElementById('categoriaId').value = id;
            document.getElementById('categoriaNombre').value = nombre || '';
            document.getElementById('categoriaDescripcion').value = descripcion || '';
            
            // Cambiar texto del botón
            document.getElementById('btnGuardarText').textContent = 'Actualizar Categoría';
            
            // Cambiar título de la sección
            const tituloSeccion = document.querySelector('.categoria-form-section h4');
            if (tituloSeccion) {
                tituloSeccion.innerHTML = '<i class="fas fa-edit"></i> Editando Categoría';
            }
            
            // Hacer scroll al formulario para mejor UX
            document.querySelector('.categoria-form-section').scrollIntoView({ 
                behavior: 'smooth' 
            });
            
            console.log('📝 Formulario listo para edición');
        },

        cancelarEdicion() {
            console.log('↩️ Cancelando edición y limpiando formulario');
            this.isEditing = false;
            
            // Limpiar formulario
            document.getElementById('categoriaForm').reset();
            document.getElementById('categoriaId').value = '';
            
            // Restablecer texto del botón
            document.getElementById('btnGuardarText').textContent = 'Crear Categoría';
            
            // Opcional: Restablecer el título de la sección
            const tituloSeccion = document.querySelector('.categoria-form-section h4');
            if (tituloSeccion) {
                tituloSeccion.innerHTML = '<i class="fas fa-plus"></i> Nueva Categoría';
            }
        },

        async eliminarCategoria(id) {
            if (!confirm('¿Estás seguro de que deseas eliminar esta categoría?')) {
                return;
            }

            try {
                const response = await fetch(`/api/categorias/${id}`, {
                    method: 'DELETE',
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
                    }
                });

                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    throw new Error(errorData.error || `Error ${response.status}`);
                }

                this.mostrarAlerta('Categoría eliminada correctamente');
                this.cargarCategorias();

            } catch (error) {
                console.error('Error eliminando categoría:', error);
                this.mostrarError('Error al eliminar la categoría: ' + error.message);
            }
        }
    };

    // Cerrar modal al hacer clic fuera
    document.addEventListener('click', (e) => {
        if (e.target === document.getElementById('categoriaModal')) {
            categoriaModal.hide();
        }
    });
    

    // Manejo del botón de asignar categorías
    if (btnAsignarCategorias) {
        btnAsignarCategorias.addEventListener('click', function() {
            categoriaModal.show();
        });
    }

    // Inicialización
    initializeFormHandlers();
    initializeImageUpload();
    initializeHorarios();
    initializeCertificado();
    initializeTipoOferta();
    initializeCategorias();
    initializeFilters();
    initializeTable();

    // Mostrar/ocultar formulario
    function initializeFormHandlers() {
        if (btnShowForm) {
            btnShowForm.addEventListener('click', function () {
                showForm();
            });
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
        document.getElementById('oferta-form').reset();
        hideAllTipoSpecificFields();
        resetImageUpload();
        clearHorariosTable();
        hideCertificadoFields();
        resetCategorias();
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

        rows.forEach(row => {
            const cells = row.querySelectorAll('td');
            if (cells.length === 0) return;

            const nombre = cells[1].textContent.toLowerCase();
            const tipo = cells[2].textContent.trim();
            const modalidad = cells[3].textContent.trim();
            const costo = parseFloat(cells[6].textContent.replace(/[^0-9.]/g, '')) || 0;
            
            let visible = true;

            // Aplicar filtros
            if (filters.search && !nombre.includes(filters.search)) {
                visible = false;
            }
            if (filters.tipo && !tipo.includes(filters.tipo)) {
                visible = false;
            }
            if (filters.modalidad && !modalidad.includes(filters.modalidad)) {
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
        // Implementar lógica de visualización
    }

    function deleteOferta(id, nombre) {
        if (confirm(`¿Está seguro de que desea eliminar la oferta "${nombre}"?`)) {
            console.log(`Eliminar oferta ${id}: ${nombre}`);
            // Implementar lógica de eliminación
        }
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

    // Función para cargar ofertas en la tabla
    function loadOfertas() {
        console.log('🔄 Cargando ofertas desde el servidor...');
        
        fetch('/admin/ofertas/listar')
        .then(response => {
            if (!response.ok) {
                throw new Error(`Error ${response.status}: ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('📊 Datos recibidos:', data);
            
            // Manejar diferentes estructuras de respuesta
            if (Array.isArray(data)) {
                // Si la respuesta es directamente un array
                populateTable(data);
            } else if (data.data && Array.isArray(data.data)) {
                // Si la respuesta tiene estructura {data: [...]}
                populateTable(data.data);
            } else if (data.ofertas && Array.isArray(data.ofertas)) {
                // Si la respuesta tiene estructura {ofertas: [...]}
                populateTable(data.ofertas);
            } else if (data.success !== undefined) {
                // Si la respuesta tiene estructura {success: true, data: [...]}
                if (data.success && data.data) {
                    populateTable(data.data);
                } else {
                    throw new Error(data.message || 'Error en la respuesta del servidor');
                }
            } else {
                throw new Error('Formato de respuesta no reconocido');
            }
        })
        .catch(error => {
            console.error('Error cargando ofertas:', error);
            showNotification('Error al cargar ofertas: ' + error.message, 'error');
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

    window.categoriaModal = categoriaModal;
    window.categoriaController = categoriaController;
    
    console.log('✅ categoríaModal definido:', typeof categoriaModal);
    console.log('✅ categoríaController definido:', typeof categoriaController);

    console.log('Gestión de Ofertas inicializada correctamente');

});