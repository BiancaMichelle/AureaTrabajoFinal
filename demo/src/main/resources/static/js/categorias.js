// ================= GESTIÓN DE CATEGORÍAS =================

/**
 * Carga la tabla de categorías al cargar la página
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Inicializando gestión de categorías...');
    cargarTablaCategoriasInicial();
    configurarEventosModal();
    configurarEventosFormulario();
});

/**
 * Carga inicial de la tabla de categorías
 */
function cargarTablaCategoriasInicial() {
    console.log('📋 Cargando tabla de categorías inicial...');
    cargarTablaCategoriasAdmin();
}

/**
 * Configura los eventos del modal de categorías
 */
function configurarEventosModal() {
    console.log('⚙️ Configurando eventos del modal...');
    
    // Evento para abrir modal de gestión de categorías
    const btnGestionarCategorias = document.getElementById('btn-gestionar-categorias');
    if (btnGestionarCategorias) {
        console.log('✅ Botón gestionar categorías encontrado');
        btnGestionarCategorias.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('🎯 Abriendo modal de gestión de categorías');
            abrirModalGestionCategorias();
        });
    } else {
        console.warn('⚠️ No se encontró el botón btn-gestionar-categorias');
    }
    
    // Evento para guardar categoría
    const btnGuardarCategoria = document.getElementById('btnGuardarCategoria');
    if (btnGuardarCategoria) {
        console.log('✅ Botón guardar categoría encontrado');
        btnGuardarCategoria.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('💾 Intentando guardar categoría...');
            guardarCategoria();
        });
    } else {
        console.warn('⚠️ No se encontró el botón btnGuardarCategoria');
    }
    
    // Eventos para cerrar modal
    const btnCerrarModal = document.querySelector('#categoriaModal .btn-close');
    const btnCancelar = document.getElementById('btnCancelarCategoria');
    
    if (btnCerrarModal) {
        btnCerrarModal.addEventListener('click', cerrarModalCategoria);
    }
    if (btnCancelar) {
        btnCancelar.addEventListener('click', cerrarModalCategoria);
    }
    
    // Cerrar modal al hacer clic fuera
    const modal = document.getElementById('categoriaModal');
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                cerrarModalCategoria();
            }
        });
    }
    
    console.log('✅ Configuración de eventos completada');
}

/**
 * Configura eventos del formulario
 */
function configurarEventosFormulario() {
    // Validación en tiempo real del nombre
    const nombreInput = document.getElementById('categoriaNombre');
    if (nombreInput) {
        nombreInput.addEventListener('blur', function() {
            validarNombreCategoriaEnTiempoReal();
        });
    }
    
    // Limpiar errores al escribir
    const inputs = document.querySelectorAll('#categoriaForm input, #categoriaForm textarea');
    inputs.forEach(input => {
        input.addEventListener('input', function() {
            if (this.classList.contains('is-invalid')) {
                this.classList.remove('is-invalid');
                const errorDiv = this.parentNode.querySelector('.invalid-feedback');
                if (errorDiv) errorDiv.remove();
            }
        });
    });
}

/**
 * Abre el modal de gestión de categorías
 */
function abrirModalGestionCategorias() {
    console.log('📂 Abriendo modal de gestión de categorías');
    
    try {
        // Limpiar formulario
        document.getElementById('categoriaForm').reset();
        document.getElementById('categoriaId').value = '';
        
        // Configurar como modal de gestión
        document.getElementById('categoriaModalLabel').textContent = 'Gestión de Categorías';
        document.getElementById('btnGuardarCategoria').textContent = 'Crear Categoría';
        
        // Limpiar mensajes de error
        limpiarErroresFormulario();
        
        // Mostrar modal usando Bootstrap
        const modal = new bootstrap.Modal(document.getElementById('categoriaModal'));
        modal.show();
        
        // Cargar tabla de categorías
        cargarTablaCategoriasAdmin();
        
        console.log('✅ Modal abierto exitosamente');
        
    } catch (error) {
        console.error('❌ Error al abrir modal:', error);
        alert('Error al abrir el modal de categorías');
    }
}

/**
 * Abre el modal para crear una nueva categoría
 */
function abrirModalNuevaCategoria() {
    console.log('🆕 Abriendo modal para nueva categoría');
    
    // Limpiar formulario
    document.getElementById('categoriaForm').reset();
    document.getElementById('categoriaId').value = '';
    
    // Actualizar título y botón
    document.getElementById('categoriaModalLabel').textContent = 'Nueva Categoría';
    document.getElementById('btnGuardarCategoria').textContent = 'Crear Categoría';
    
    // Limpiar mensajes de error
    limpiarErroresFormulario();
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('categoriaModal'));
    modal.show();
}

/**
 * Abre el modal para editar una categoría existente
 */
function editarCategoria(id, nombre, descripcion) {
    console.log('✏️ Editando categoría:', { id, nombre, descripcion });
    
    try {
        // Llenar formulario con datos actuales
        document.getElementById('categoriaId').value = id;
        document.getElementById('categoriaNombre').value = nombre;
        document.getElementById('categoriaDescripcion').value = descripcion;
        
        // Actualizar título y botón
        document.getElementById('categoriaModalLabel').textContent = 'Editar Categoría';
        document.getElementById('btnGuardarCategoria').textContent = 'Actualizar Categoría';
        
        // Limpiar mensajes de error
        limpiarErroresFormulario();
        
        // Mostrar modal
        const modal = new bootstrap.Modal(document.getElementById('categoriaModal'));
        modal.show();
        
        console.log('✅ Modal de edición listo');
        
    } catch (error) {
        console.error('❌ Error al abrir modal de edición:', error);
        alert('Error al cargar los datos de la categoría');
    }
}

/**
 * Cierra el modal de categorías
 */
function cerrarModalCategoria() {
    try {
        const modal = bootstrap.Modal.getInstance(document.getElementById('categoriaModal'));
        if (modal) {
            modal.hide();
        }
        document.getElementById('categoriaForm').reset();
        limpiarErroresFormulario();
        console.log('🔒 Modal cerrado');
    } catch (error) {
        console.error('Error al cerrar modal:', error);
    }
}


/**
 * Elimina una categoría con confirmación
 */
async function eliminarCategoria(id, nombre) {
    console.log('🗑️ Solicitando eliminación de categoría:', { id, nombre });
    
    if (!confirm(`¿Está seguro de que desea eliminar la categoría "${nombre}"?\n\nEsta acción no se puede deshacer.`)) {
        return;
    }
    
    try {
        const response = await fetch(`/admin/categorias/eliminar/${id}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            mostrarMensajeExito(data.message || 'Categoría eliminada exitosamente');
            await cargarTablaCategoriasAdmin();
        } else {
            throw new Error(data.message || 'Error al eliminar categoría');
        }
        
    } catch (error) {
        console.error('❌ Error al eliminar categoría:', error);
        alert('Error al eliminar categoría: ' + error.message);
    }
}

/**
 * Renderiza la tabla de categorías
 */
function renderizarTablaCategoriasAdmin(categorias) {
    const tbody = document.querySelector('#categoriasTable tbody');
    
    if (!tbody) {
        console.error('❌ No se encontró el tbody de la tabla');
        return;
    }
    
    if (!categorias || categorias.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center text-muted py-4">
                    <i class="fas fa-inbox fa-2x mb-2 d-block"></i>
                    No hay categorías registradas
                </td>
            </tr>
        `;
        return;
    }
    
    const filas = categorias.map(categoria => {
        const nombre = escapeHtml(categoria.nombre || 'Sin nombre');
        const descripcion = escapeHtml(categoria.descripcion || 'Sin descripción');
        const fechaCreacion = categoria.fechaCreacion ? 
            new Date(categoria.fechaCreacion).toLocaleDateString('es-ES') : 'N/A';
        
        // Descripción corta para la tabla
        const descripcionCorta = descripcion.length > 80 ? 
            descripcion.substring(0, 80) + '...' : descripcion;
        
        return `
            <tr>
                <td>
                    <div class="d-flex align-items-center">
                        <i class="${categoria.iconoCSS || 'fas fa-folder'} me-2 text-primary"></i>
                        <strong>${nombre}</strong>
                    </div>
                </td>
                <td>
                    <span class="text-muted" title="${descripcion}">${descripcionCorta}</span>
                </td>
                <td class="text-center">
                    <small class="text-muted">${fechaCreacion}</small>
                </td>
                <td class="text-center">
                    <div class="btn-group btn-group-sm">
                        <button type="button" 
                                class="btn btn-outline-primary btn-sm" 
                                onclick="editarCategoria(${categoria.idCategoria}, '${escapeSingleQuotes(nombre)}', '${escapeSingleQuotes(descripcion)}')"
                                title="Editar categoría">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button type="button" 
                                class="btn btn-outline-danger btn-sm" 
                                onclick="eliminarCategoria(${categoria.idCategoria}, '${escapeSingleQuotes(nombre)}')"
                                title="Eliminar categoría">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
    
    tbody.innerHTML = filas;
    console.log(`✅ Tabla renderizada con ${categorias.length} categorías`);
}

/**
 * Actualiza el contador de categorías
 */
function actualizarContadorCategorias(total) {
    const contadores = [
        document.getElementById('totalCategorias'),
        document.getElementById('contadorCategorias')
    ];
    
    contadores.forEach(contador => {
        if (contador) {
            contador.textContent = total;
        }
    });
    
    console.log(`🔢 Contador actualizado: ${total} categorías`);
}

/**
 * Valida el formulario de categoría
 */
function validarFormularioCategoria(nombre, descripcion) {
    limpiarErroresFormulario();
    
    let valido = true;
    
    // Validar nombre
    if (!nombre) {
        mostrarErrorCampo('categoriaNombre', 'El nombre de la categoría es obligatorio');
        valido = false;
    } else if (nombre.length < 2) {
        mostrarErrorCampo('categoriaNombre', 'El nombre debe tener al menos 2 caracteres');
        valido = false;
    } else if (nombre.length > 100) {
        mostrarErrorCampo('categoriaNombre', 'El nombre no puede exceder 100 caracteres');
        valido = false;
    }
    
    // Validar descripción
    if (!descripcion) {
        mostrarErrorCampo('categoriaDescripcion', 'La descripción de la categoría es obligatoria');
        valido = false;
    } else if (descripcion.length < 5) {
        mostrarErrorCampo('categoriaDescripcion', 'La descripción debe tener al menos 5 caracteres');
        valido = false;
    } else if (descripcion.length > 500) {
        mostrarErrorCampo('categoriaDescripcion', 'La descripción no puede exceder 500 caracteres');
        valido = false;
    }
    
    return valido;
}

/**
 * Valida el nombre de categoría en tiempo real
 */
async function validarNombreCategoriaEnTiempoReal() {
    const nombre = document.getElementById('categoriaNombre').value.trim();
    const id = document.getElementById('categoriaId').value;
    
    if (!nombre || nombre.length < 2) {
        return;
    }
    
    try {
        const response = await fetch(`/admin/categorias/verificar-nombre?nombre=${encodeURIComponent(nombre)}`);
        const data = await response.json();
        
        if (data.existe && !id) {
            mostrarErrorCampo('categoriaNombre', 'Ya existe una categoría con este nombre');
        }
    } catch (error) {
        console.error('Error al verificar nombre:', error);
    }
}

// ================= FUNCIONES AUXILIARES =================

function mostrarErrorCampo(campoId, mensaje) {
    const campo = document.getElementById(campoId);
    if (campo) {
        campo.classList.add('is-invalid');
        
        let errorDiv = campo.parentNode.querySelector('.invalid-feedback');
        if (!errorDiv) {
            errorDiv = document.createElement('div');
            errorDiv.className = 'invalid-feedback';
            campo.parentNode.appendChild(errorDiv);
        }
        errorDiv.textContent = mensaje;
    }
}

function mostrarErrorFormulario(mensaje) {
    const alertContainer = document.getElementById('categoriaAlert');
    if (alertContainer) {
        alertContainer.innerHTML = `
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="fas fa-exclamation-triangle me-2"></i>
                <strong>Error:</strong> ${mensaje}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
    } else {
        alert('Error: ' + mensaje);
    }
}

function mostrarMensajeExito(mensaje) {
    // Usar toast de Bootstrap si está disponible
    if (typeof bootstrap !== 'undefined' && bootstrap.Toast) {
        // Crear toast dinámicamente
        const toastContainer = document.getElementById('toastContainer') || createToastContainer();
        const toastElement = document.createElement('div');
        toastElement.className = 'toast align-items-center text-white bg-success border-0';
        toastElement.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-check-circle me-2"></i> ${mensaje}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        toastContainer.appendChild(toastElement);
        
        const toast = new bootstrap.Toast(toastElement);
        toast.show();
        
        // Remover después de ocultar
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    } else {
        alert('Éxito: ' + mensaje);
    }
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container position-fixed top-0 end-0 p-3';
    container.style.zIndex = '9999';
    document.body.appendChild(container);
    return container;
}

function limpiarErroresFormulario() {
    document.querySelectorAll('#categoriaForm .is-invalid').forEach(campo => {
        campo.classList.remove('is-invalid');
    });
    
    document.querySelectorAll('#categoriaForm .invalid-feedback').forEach(error => {
        error.remove();
    });
    
    const alertContainer = document.getElementById('categoriaAlert');
    if (alertContainer) {
        alertContainer.innerHTML = '';
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function escapeSingleQuotes(text) {
    return text.replace(/'/g, "\\'");
}

// ================= CONFIGURACIÓN CSRF =================

/**
 * Obtiene el token CSRF de forma robusta
 */
function getCsrfToken() {
    // Método 1: Desde meta tags (Thymeleaf)
    const metaToken = document.querySelector('meta[name="_csrf"]');
    if (metaToken && metaToken.content) {
        console.log('🔐 CSRF Token encontrado en meta tag');
        return metaToken.content;
    }
    
    // Método 2: Desde input hidden
    const inputToken = document.querySelector('input[name="_csrf"]');
    if (inputToken && inputToken.value) {
        console.log('🔐 CSRF Token encontrado en input hidden');
        return inputToken.value;
    }
    
    // Método 3: Desde cookies (fallback)
    const cookieToken = getCookie('XSRF-TOKEN');
    if (cookieToken) {
        console.log('🔐 CSRF Token encontrado en cookies');
        return cookieToken;
    }
    
    console.warn('⚠️ CSRF Token no encontrado');
    return '';
}

/**
 * Obtiene cookie por nombre
 */
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

/**
 * Headers comunes para todas las peticiones
 */
function getAuthHeaders() {
    const token = getCsrfToken();
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (token) {
        headers['X-CSRF-TOKEN'] = token;
    }
    
    return headers;
}

// ================= GESTIÓN DE CATEGORÍAS ACTUALIZADA =================

/**
 * Guarda una categoría (crear o actualizar) - VERSIÓN CORREGIDA
 */
async function guardarCategoria() {
    console.log('💾 Iniciando guardado de categoría...');
    
    try {
        const idElement = document.getElementById('categoriaId');
        const nombreElement = document.getElementById('categoriaNombre');
        const descripcionElement = document.getElementById('categoriaDescripcion');
        const btnGuardar = document.getElementById('btnGuardarCategoria');
        
        if (!nombreElement || !descripcionElement || !btnGuardar) {
            throw new Error('Elementos del formulario no encontrados');
        }
        
        const id = idElement.value;
        const nombre = nombreElement.value.trim();
        const descripcion = descripcionElement.value.trim();
        
        console.log('📝 Datos a guardar:', { id, nombre, descripcion });
        
        // Validar formulario
        if (!validarFormularioCategoria(nombre, descripcion)) {
            return;
        }
        
        // Configurar para guardar
        btnGuardar.disabled = true;
        btnGuardar.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';
        
        // Determinar URL y método
        const esEdicion = id !== '';
        const url = esEdicion ? `/admin/categorias/editar/${id}` : '/admin/categorias/crear';
        
        // Obtener headers con CSRF
        const headers = getAuthHeaders();
        console.log('🔐 Headers de la petición:', headers);
        
        // Realizar petición
        const response = await fetch(url, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ 
                nombre: nombre,
                descripcion: descripcion 
            }),
            credentials: 'include' // Importante para enviar cookies de sesión
        });
        
        console.log('📨 Status de respuesta:', response.status, response.statusText);
        
        // Si es 403, es definitivamente CSRF
        if (response.status === 403) {
            throw new Error('Acceso denegado (403). Problema de token CSRF. Recarga la página e intenta nuevamente.');
        }
        
        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status} - ${response.statusText}`);
        }
        
        const data = await response.json();
        console.log('📨 Respuesta del servidor:', data);
        
        if (data.success) {
            mostrarMensajeExito(data.message || 'Categoría guardada exitosamente');
            cerrarModalCategoria();
            await cargarTablaCategoriasAdmin();
        } else {
            throw new Error(data.message || 'Error desconocido del servidor');
        }
        
    } catch (error) {
        console.error('❌ Error al guardar categoría:', error);
        mostrarErrorFormulario(error.message);
        
        // Si es error CSRF, sugerir recargar
        if (error.message.includes('CSRF')) {
            setTimeout(() => {
                if (confirm('Problema de seguridad detectado. ¿Deseas recargar la página?')) {
                    location.reload();
                }
            }, 2000);
        }
    } finally {
        const btnGuardar = document.getElementById('btnGuardarCategoria');
        if (btnGuardar) {
            btnGuardar.disabled = false;
            btnGuardar.innerHTML = '<i class="fas fa-save"></i> ' + 
                (document.getElementById('categoriaId').value ? 'Actualizar' : 'Crear') + ' Categoría';
        }
    }
}

/**
 * Carga la tabla de categorías desde el servidor - VERSIÓN CORREGIDA
 */
async function cargarTablaCategoriasAdmin() {
    console.log('🔄 Cargando tabla de categorías...');
    
    const tbody = document.querySelector('#categoriasTable tbody');
    const loadingMsg = document.getElementById('categoriasLoading');
    const errorMsg = document.getElementById('categoriasError');
    
    // Mostrar estado de carga
    if (loadingMsg) loadingMsg.style.display = 'block';
    if (errorMsg) errorMsg.style.display = 'none';
    if (tbody) tbody.innerHTML = '<tr><td colspan="4" class="text-center"><div class="spinner-border spinner-border-sm"></div> Cargando...</td></tr>';
    
    try {
        const response = await fetch('/admin/categorias/listar', {
            method: 'GET',
            headers: getAuthHeaders(),
            credentials: 'include'
        });
        
        console.log('📊 Status de respuesta categorías:', response.status);
        
        if (!response.ok) {
            if (response.status === 403) {
                throw new Error('Acceso denegado al cargar categorías. Verifica tu sesión.');
            }
            throw new Error(`Error HTTP: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('📊 Datos recibidos:', data);
        
        if (data.success) {
            renderizarTablaCategoriasAdmin(data.categorias || []);
            actualizarContadorCategorias(data.total || 0);
        } else {
            throw new Error(data.message || 'Error en la respuesta del servidor');
        }
        
    } catch (error) {
        console.error('❌ Error al cargar categorías:', error);
        
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center text-danger">
                        <i class="fas fa-exclamation-triangle"></i> ${error.message}
                    </td>
                </tr>
            `;
        }
        if (errorMsg) {
            errorMsg.textContent = 'Error: ' + error.message;
            errorMsg.style.display = 'block';
        }
    } finally {
        if (loadingMsg) loadingMsg.style.display = 'none';
    }
}

// ================= FUNCIÓN DE DEBUG MEJORADA =================

/**
 * Función de debug para verificar el estado de CSRF
 */
function debugCSRF() {
    console.log('=== DEBUG CSRF ===');
    
    // Verificar meta tags
    const metaToken = document.querySelector('meta[name="_csrf"]');
    console.log('Meta CSRF Token:', metaToken ? metaToken.content : 'NO ENCONTRADO');
    
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    console.log('Meta CSRF Header:', metaHeader ? metaHeader.content : 'NO ENCONTRADO');
    
    // Verificar inputs hidden
    const inputToken = document.querySelector('input[name="_csrf"]');
    console.log('Input CSRF Token:', inputToken ? inputToken.value : 'NO ENCONTRADO');
    
    // Verificar cookies
    console.log('Cookie CSRF:', getCookie('XSRF-TOKEN') || 'NO ENCONTRADA');
    
    // Verificar headers que se enviarían
    console.log('Headers de petición:', getAuthHeaders());
    
    console.log('=== FIN DEBUG CSRF ===');
}

// Ejecutar debug al cargar
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(debugCSRF, 1000);
});
console.log('✅ Módulo de categorías cargado correctamente');