// ================= GESTIÓN DE CATEGORÍAS =================

/**
 * Carga la tabla de categorías al cargar la página
 */
document.addEventListener('DOMContentLoaded', function() {
    cargarTablaCategoriasInicial();
    configurarEventosModal();
});

/**
 * Carga inicial de la tabla de categorías
 */
function cargarTablaCategoriasInicial() {
    console.log('Cargando tabla de categorías...');
    cargarTablaCategoriasAdmin();
}

/**
 * Configura los eventos del modal de categorías
 */
function configurarEventosModal() {
    console.log('=== Configurando eventos del modal ===');
    
    // Evento para abrir modal de gestión de categorías
    const btnGestionarCategorias = document.getElementById('btn-gestionar-categorias');
    if (btnGestionarCategorias) {
        console.log('✓ Botón gestionar categorías encontrado');
        btnGestionarCategorias.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Click en gestionar categorías');
            abrirModalGestionCategorias();
        });
    } else {
        console.error('✗ No se encontró el botón btn-gestionar-categorias');
    }
    
    // Evento para guardar categoría
    const btnGuardarCategoria = document.getElementById('btnGuardarCategoria');
    if (btnGuardarCategoria) {
        console.log('✓ Botón guardar categoría encontrado');
        btnGuardarCategoria.addEventListener('click', function(e) {
            e.preventDefault();
            console.log('Click en botón guardar categoría');
            guardarCategoria();
        });
    } else {
        console.error('✗ No se encontró el botón btnGuardarCategoria');
    }
    
    // Evento para cerrar modal con X
    const btnCerrarModal = document.querySelector('#categoriaModal .btn-close');
    if (btnCerrarModal) {
        console.log('✓ Botón cerrar modal encontrado');
        btnCerrarModal.addEventListener('click', function(e) {
            e.preventDefault();
            cerrarModalCategoria();
        });
    } else {
        console.error('✗ No se encontró el botón de cerrar modal');
    }
    
    // Cerrar modal al hacer clic fuera
    const modal = document.getElementById('categoriaModal');
    if (modal) {
        console.log('✓ Modal encontrado para click fuera');
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                cerrarModalCategoria();
            }
        });
    } else {
        console.error('✗ No se encontró el modal categoriaModal');
    }
    
    console.log('=== Configuración de eventos completada ===');
}

/**
 * Abre el modal de gestión de categorías
 */
function abrirModalGestionCategorias() {
    console.log('Abriendo modal de gestión de categorías');
    
    // Limpiar formulario
    document.getElementById('categoriaForm').reset();
    document.getElementById('categoriaId').value = '';
    
    // Configurar como modal de gestión (no crear nueva)
    document.getElementById('categoriaModalLabel').textContent = 'Gestión de Categorías';
    document.getElementById('btnGuardarCategoria').textContent = 'Crear Categoría';
    
    // Limpiar mensajes de error
    limpiarErroresFormulario();
    
    // Mostrar modal
    document.getElementById('categoriaModal').style.display = 'flex';
    
    // Cargar tabla de categorías
    cargarTablaCategoriasAdmin();
    
    // Enfocar el primer campo
    setTimeout(() => {
        document.getElementById('categoriaNombre').focus();
    }, 100);
}

/**
 * Abre el modal para crear una nueva categoría
 */
function abrirModalNuevaCategoria() {
    console.log('Abriendo modal de nueva categoría');
    
    // Limpiar formulario
    document.getElementById('categoriaForm').reset();
    document.getElementById('categoriaId').value = '';
    
    // Actualizar título y botón
    document.getElementById('categoriaModalLabel').textContent = 'Nueva Categoría';
    document.getElementById('btnGuardarCategoria').textContent = 'Crear Categoría';
    
    // Limpiar mensajes de error
    limpiarErroresFormulario();
    
    // Mostrar modal
    document.getElementById('categoriaModal').style.display = 'flex';
    
    // Enfocar el primer campo
    setTimeout(() => {
        document.getElementById('categoriaNombre').focus();
    }, 100);
}

/**
 * Abre el modal para editar una categoría existente
 */
function editarCategoria(id, nombre, descripcion) {
    console.log('Editando categoría:', id, nombre, descripcion);
    
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
    document.getElementById('categoriaModal').style.display = 'flex';
    
    // Enfocar el primer campo
    setTimeout(() => {
        document.getElementById('categoriaNombre').focus();
    }, 100);
}

/**
 * Cierra el modal de categorías
 */
function cerrarModalCategoria() {
    document.getElementById('categoriaModal').style.display = 'none';
    document.getElementById('categoriaForm').reset();
    limpiarErroresFormulario();
}

/**
 * Guarda una categoría (crear o actualizar)
 */
function guardarCategoria() {
    console.log('=== INICIO guardarCategoria ===');
    
    try {
        // Verificar que los elementos existen
        const formElement = document.getElementById('categoriaForm');
        const idElement = document.getElementById('categoriaId');
        const nombreElement = document.getElementById('categoriaNombre');
        const descripcionElement = document.getElementById('categoriaDescripcion');
        const btnGuardar = document.getElementById('btnGuardarCategoria');
        
        console.log('Elementos encontrados:', {
            form: !!formElement,
            id: !!idElement,
            nombre: !!nombreElement,
            descripcion: !!descripcionElement,
            boton: !!btnGuardar
        });
        
        if (!nombreElement || !descripcionElement || !btnGuardar) {
            console.error('Elementos requeridos no encontrados');
            alert('Error: No se pudieron encontrar los elementos del formulario');
            return;
        }
        
        // Obtener datos del formulario
        const id = idElement ? idElement.value : '';
        const nombre = nombreElement.value.trim();
        const descripcion = descripcionElement.value.trim();
        
        console.log('Datos del formulario:', { id, nombre, descripcion });
        
        // Validaciones básicas
        if (!nombre) {
            alert('El nombre de la categoría es obligatorio');
            nombreElement.focus();
            return;
        }
        
        if (!descripcion) {
            alert('La descripción de la categoría es obligatoria');
            descripcionElement.focus();
            return;
        }
        
        if (nombre.length < 2 || nombre.length > 100) {
            alert('El nombre debe tener entre 2 y 100 caracteres');
            nombreElement.focus();
            return;
        }
        
        if (descripcion.length < 5 || descripcion.length > 500) {
            alert('La descripción debe tener entre 5 y 500 caracteres');
            descripcionElement.focus();
            return;
        }
        
        // Deshabilitar botón mientras se procesa
        const textoOriginal = btnGuardar.textContent;
        btnGuardar.disabled = true;
        btnGuardar.textContent = 'Guardando...';
        
        // Determinar si es crear o actualizar
        const esEdicion = id && id.trim() !== '';
        const url = esEdicion ? `/admin/categorias/editar/${id}` : '/admin/categorias/crear';
        
        console.log('URL a llamar:', url);
        console.log('Es edición:', esEdicion);
        
        // Enviar datos al servidor
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                nombre: nombre,
                descripcion: descripcion
            })
        })
        .then(response => {
            console.log('Respuesta HTTP status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Respuesta del servidor:', data);
            
            if (data && data.success) {
                alert('Categoría guardada exitosamente: ' + (data.message || ''));
                cerrarModalCategoria();
                
                // Recargar tabla si la función existe
                if (typeof cargarTablaCategoriasAdmin === 'function') {
                    cargarTablaCategoriasAdmin();
                }
            } else {
                const mensaje = data && data.message ? data.message : 'Error desconocido del servidor';
                alert('Error: ' + mensaje);
            }
        })
        .catch(error => {
            console.error('Error en fetch:', error);
            alert('Error de conexión: ' + error.message);
        })
        .finally(() => {
            // Restaurar botón
            if (btnGuardar) {
                btnGuardar.disabled = false;
                btnGuardar.textContent = textoOriginal;
            }
            console.log('=== FIN guardarCategoria ===');
        });
        
    } catch (error) {
        console.error('Error general en guardarCategoria:', error);
        alert('Error interno: ' + error.message);
    }
}

/**
 * Elimina una categoría
 */
function eliminarCategoria(id, nombre) {
    if (!confirm(`¿Está seguro de que desea eliminar la categoría "${nombre}"?`)) {
        return;
    }
    
    console.log('Eliminando categoría:', id, nombre);
    
    fetch(`/admin/categorias/eliminar/${id}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => response.json())
    .then(data => {
        console.log('Respuesta del servidor:', data);
        
        if (data.success) {
            mostrarMensajeExito(data.message);
            cargarTablaCategoriasAdmin();
        } else {
            mostrarErrorGeneral(data.message);
        }
    })
    .catch(error => {
        console.error('Error al eliminar categoría:', error);
        mostrarErrorGeneral('Error de conexión al servidor');
    });
}

/**
 * Carga la tabla de categorías desde el servidor
 */
function cargarTablaCategoriasAdmin() {
    console.log('Cargando tabla de categorías desde servidor...');
    
    const tbody = document.querySelector('#categoriasTable tbody');
    const loadingMsg = document.getElementById('categoriasLoading');
    const errorMsg = document.getElementById('categoriasError');
    
    // Mostrar loading
    if (loadingMsg) loadingMsg.style.display = 'block';
    if (errorMsg) errorMsg.style.display = 'none';
    if (tbody) tbody.innerHTML = '<tr><td colspan="4" class="text-center">Cargando...</td></tr>';
    
    fetch('/admin/categorias/listar')
        .then(response => response.json())
        .then(data => {
            console.log('Categorías obtenidas:', data);
            
            if (data.success) {
                renderizarTablaCategoriasAdmin(data.categorias);
                actualizarContadorCategorias(data.total);
            } else {
                throw new Error(data.message || 'Error al obtener categorías');
            }
        })
        .catch(error => {
            console.error('Error al cargar categorías:', error);
            
            if (tbody) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Error al cargar categorías</td></tr>';
            }
            if (errorMsg) {
                errorMsg.textContent = 'Error al cargar categorías: ' + error.message;
                errorMsg.style.display = 'block';
            }
        })
        .finally(() => {
            if (loadingMsg) loadingMsg.style.display = 'none';
        });
}

/**
 * Renderiza la tabla de categorías con los datos recibidos
 */
function renderizarTablaCategoriasAdmin(categorias) {
    const tbody = document.querySelector('#categoriasTable tbody');
    
    if (!tbody) {
        console.error('No se encontró el tbody de la tabla de categorías');
        return;
    }
    
    if (!categorias || categorias.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No hay categorías registradas</td></tr>';
        return;
    }
    
    const filas = categorias.map(categoria => {
        const nombre = escaparHtml(categoria.nombre || '');
        const descripcion = escaparHtml(categoria.descripcion || '');
        const fechaCreacion = categoria.fechaCreacion ? 
            new Date(categoria.fechaCreacion).toLocaleDateString() : 'N/A';
        
        return `
            <tr>
                <td>
                    <strong>${nombre}</strong>
                </td>
                <td class="descripcion-cell">
                    ${descripcion.length > 100 ? 
                        `<span title="${descripcion}">${descripcion.substring(0, 100)}...</span>` : 
                        descripcion
                    }
                </td>
                <td class="text-center">
                    <small class="text-muted">${fechaCreacion}</small>
                </td>
                <td class="text-center">
                    <div class="btn-group btn-group-sm">
                        <button type="button" 
                                class="btn btn-outline-primary btn-sm" 
                                onclick="editarCategoria(${categoria.idCategoria}, '${nombre.replace(/'/g, "\\'")}', '${descripcion.replace(/'/g, "\\'")}')"
                                title="Editar categoría">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button type="button" 
                                class="btn btn-outline-danger btn-sm" 
                                onclick="eliminarCategoria(${categoria.idCategoria}, '${nombre.replace(/'/g, "\\'")}')"
                                title="Eliminar categoría">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
    
    tbody.innerHTML = filas;
}

/**
 * Actualiza el contador de categorías en la interfaz
 */
function actualizarContadorCategorias(total) {
    const contador = document.getElementById('totalCategorias');
    if (contador) {
        contador.textContent = total;
    }
    
    const contadorTabla = document.getElementById('contadorCategorias');
    if (contadorTabla) {
        contadorTabla.textContent = `Total: ${total} categoría${total !== 1 ? 's' : ''}`;
    }
}

/**
 * Valida el formulario de categoría
 */
function validarFormularioCategoria(nombre, descripcion) {
    limpiarErroresFormulario();
    
    let hayErrores = false;
    
    // Validar nombre
    if (!nombre) {
        mostrarErrorCampo('categoriaNombre', 'El nombre de la categoría es obligatorio');
        hayErrores = true;
    } else if (nombre.length < 2) {
        mostrarErrorCampo('categoriaNombre', 'El nombre debe tener al menos 2 caracteres');
        hayErrores = true;
    } else if (nombre.length > 100) {
        mostrarErrorCampo('categoriaNombre', 'El nombre no puede exceder 100 caracteres');
        hayErrores = true;
    }
    
    // Validar descripción
    if (!descripcion) {
        mostrarErrorCampo('categoriaDescripcion', 'La descripción de la categoría es obligatoria');
        hayErrores = true;
    } else if (descripcion.length < 5) {
        mostrarErrorCampo('categoriaDescripcion', 'La descripción debe tener al menos 5 caracteres');
        hayErrores = true;
    } else if (descripcion.length > 500) {
        mostrarErrorCampo('categoriaDescripcion', 'La descripción no puede exceder 500 caracteres');
        hayErrores = true;
    }
    
    return !hayErrores;
}

/**
 * Valida el nombre de categoría en tiempo real
 */
function validarNombreCategoriaEnTiempoReal() {
    const nombre = document.getElementById('categoriaNombre').value.trim();
    
    if (!nombre) {
        return;
    }
    
    // Verificar si el nombre ya existe
    fetch(`/admin/categorias/verificar-nombre?nombre=${encodeURIComponent(nombre)}`)
        .then(response => response.json())
        .then(data => {
            if (data.existe) {
                const id = document.getElementById('categoriaId').value;
                
                // Si estamos editando, verificar que no sea la misma categoría
                if (!id) {
                    mostrarErrorCampo('categoriaNombre', 'Ya existe una categoría con este nombre');
                }
            }
        })
        .catch(error => {
            console.error('Error al verificar nombre:', error);
        });
}

// ================= FUNCIONES AUXILIARES =================

/**
 * Muestra un error en un campo específico
 */
function mostrarErrorCampo(campoId, mensaje) {
    const campo = document.getElementById(campoId);
    if (campo) {
        campo.classList.add('is-invalid');
        
        // Buscar o crear div de error
        let errorDiv = campo.parentNode.querySelector('.invalid-feedback');
        if (!errorDiv) {
            errorDiv = document.createElement('div');
            errorDiv.className = 'invalid-feedback';
            campo.parentNode.appendChild(errorDiv);
        }
        errorDiv.textContent = mensaje;
    }
}

/**
 * Muestra un error general en el formulario
 */
function mostrarErrorFormulario(mensaje) {
    const alertContainer = document.getElementById('categoriaAlert');
    if (alertContainer) {
        alertContainer.innerHTML = `
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="fas fa-exclamation-triangle me-2"></i>
                ${mensaje}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
    }
}

/**
 * Muestra un mensaje de éxito
 */
function mostrarMensajeExito(mensaje) {
    // Aquí puedes usar tu sistema de notificaciones preferido
    // Por ahora, solo log en consola
    console.log('Éxito:', mensaje);
    
    // Opcional: mostrar un toast o notificación temporal
    if (typeof mostrarToast === 'function') {
        mostrarToast(mensaje, 'success');
    }
}

/**
 * Muestra un error general
 */
function mostrarErrorGeneral(mensaje) {
    console.error('Error:', mensaje);
    
    // Opcional: mostrar un toast o notificación temporal
    if (typeof mostrarToast === 'function') {
        mostrarToast(mensaje, 'error');
    }
}

/**
 * Limpia todos los errores del formulario
 */
function limpiarErroresFormulario() {
    // Remover clases is-invalid
    document.querySelectorAll('#categoriaForm .is-invalid').forEach(campo => {
        campo.classList.remove('is-invalid');
    });
    
    // Remover mensajes de error
    document.querySelectorAll('#categoriaForm .invalid-feedback').forEach(error => {
        error.remove();
    });
    
    // Limpiar alertas
    const alertContainer = document.getElementById('categoriaAlert');
    if (alertContainer) {
        alertContainer.innerHTML = '';
    }
}

/**
 * Escapa HTML para prevenir XSS
 */
function escaparHtml(texto) {
    const div = document.createElement('div');
    div.textContent = texto;
    return div.innerHTML;
}