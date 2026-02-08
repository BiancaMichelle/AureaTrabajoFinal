document.addEventListener('DOMContentLoaded', function () {
    // Referencias a elementos principales
    const btnShowForm = document.getElementById('btn-show-form');
    const formContainer = document.getElementById('form-container');
    const btnCloseForm = document.getElementById('btn-close-form');
    const btnCancelForm = document.getElementById('btn-cancel-form');
    const defaultCancelMarkup = btnCancelForm ? btnCancelForm.innerHTML : '';
    const rolSelect = document.getElementById('rol-select');
    const fieldsDocente = document.getElementById('fields-docente');

    // Elementos para upload de foto
    const fotoInput = document.getElementById('foto');
    const fotoPreview = document.getElementById('foto-preview');
    const uploadPlaceholder = document.querySelector('.upload-placeholder');
    const btnBorrarFotoForm = document.getElementById('btn-borrar-foto-form');

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

    const modalDetalleUsuario = document.getElementById('modalDetalleUsuario');
    const btnDarBajaUsuario = document.getElementById('btn-dar-baja-usuario'); // Cambiado a botón de baja
    const btnBorrarFotoUsuario = document.getElementById('btn-borrar-foto-usuario');
    const modalDetalleRefs = {
        foto: document.getElementById('detalle-usuario-foto'),
        sinFoto: document.getElementById('detalle-usuario-sin-foto'),
        nombre: document.getElementById('detalle-usuario-nombre'),
        dni: document.getElementById('detalle-usuario-dni'),
        correo: document.getElementById('detalle-usuario-correo'),
        telefono: document.getElementById('detalle-usuario-telefono'),
        genero: document.getElementById('detalle-usuario-genero'),
        fechaNacimiento: document.getElementById('detalle-usuario-fecha-nacimiento'),
        estado: document.getElementById('detalle-usuario-estado'),
        fechaRegistro: document.getElementById('detalle-usuario-fecha-registro'),
        roles: document.getElementById('detalle-usuario-roles'),
        pais: document.getElementById('detalle-usuario-pais'),
        provincia: document.getElementById('detalle-usuario-provincia'),
        ciudad: document.getElementById('detalle-usuario-ciudad'),
        alumnoColegio: document.getElementById('detalle-usuario-colegio'),
        alumnoAnioEgreso: document.getElementById('detalle-usuario-anio-egreso'),
        alumnoUltimosEstudios: document.getElementById('detalle-usuario-ultimos-estudios'),
        alumnoDocumentacionContainer: document.getElementById('detalle-usuario-doc-global-container'), // Nuevo contenedor
        alumnoDocCheck: document.getElementById('detalle-usuario-doc-check'), // Nuevo checkbox global
        alumnoDocLabel: document.getElementById('detalle-usuario-doc-label'), // Nuevo label global
        alumnoInscripciones: document.getElementById('detalle-usuario-inscripciones'),
        docenteMatricula: document.getElementById('detalle-usuario-matricula'),
        docenteExperiencia: document.getElementById('detalle-usuario-experiencia'),
        docenteHorarios: document.getElementById('detalle-usuario-horarios')
    };
    const modalDetalleSections = {
        alumno: document.getElementById('detalle-usuario-alumno'),
        docente: document.getElementById('detalle-usuario-docente')
    };

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || '';
    const usuariosCache = new Map(); // Preserve hydrated rows when the table refreshes

    // Variables para roles seleccionados
    let selectedRoles = [];
    let usuarioEnEdicion = null;
    let locationSystemInitialized = false;
    
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
    const defaultPageSize = 10;
    let ciudadesDisponibles = false;

    // Inicialización
    initializeFormHandlers();
    initializeFotoUpload();
    initializeRoles();
    initializeHorariosDocente();
    initializeFilters();
    initializeTable();
    initializeDateInput(); // ✅ Nueva función para configurar el input de fecha
    initializeDetalleUsuarioModal();
    initializeFotoDeleteForm();

    // ✅ Configurar fecha máxima para el input de fecha (16 años atrás desde hoy)
    function initializeDateInput() {
        const fechaInput = document.getElementById('fechaNacimiento');
        if (fechaInput) {
            const hoy = new Date();
            const hace16Anos = new Date(hoy.getFullYear() - 16, hoy.getMonth(), hoy.getDate());
            const fechaMaxima = hace16Anos.toISOString().split('T')[0];
            fechaInput.setAttribute('max', fechaMaxima);
            console.log('📅 Fecha máxima configurada:', fechaMaxima);
        }
    }

    function confirmAction(title, message, onConfirm) {
        if (typeof ModalConfirmacion !== 'undefined' && ModalConfirmacion.show) {
            ModalConfirmacion.show(title, message, onConfirm);
            return;
        }
        if (confirm(message)) {
            onConfirm();
        }
    }

    function initializeDetalleUsuarioModal() {
        if (!modalDetalleUsuario) {
            return;
        }

        modalDetalleUsuario.addEventListener('click', (event) => {
            if (event.target === modalDetalleUsuario) {
                cerrarModalDetalleUsuario();
            }
        });

        if (btnBorrarFotoUsuario) {
            btnBorrarFotoUsuario.addEventListener('click', () => {
                const identifier = modalDetalleUsuario.dataset.identifier;
                if (!identifier) {
                    showNotification('❌ No se pudo determinar el usuario', 'error');
                    return;
                }
                confirmAction(
                    'Eliminar foto',
                    '¿Está seguro de que desea eliminar la foto de este usuario?',
                    () => {
                        const headers = {};
                        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
                        fetch(`/admin/usuarios/${identifier}/foto`, {
                            method: 'DELETE',
                            headers
                        })
                        .then(async (resp) => {
                            let data = {};
                            try { data = await resp.json(); } catch (e) {}
                            if (!resp.ok || data.success === false) {
                                throw new Error(data.message || 'No se pudo borrar la foto');
                            }
                            renderDetalleUsuario(data.data || {});
                            showNotification('✅ Foto eliminada', 'success');
                        })
                        .catch((error) => {
                            console.error('Error eliminando foto:', error);
                            showNotification(`❌ ${error.message || 'Error al eliminar foto'}`, 'error');
                        });
                    }
                );
            });
        }

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && modalDetalleUsuario.classList.contains('show')) {
                cerrarModalDetalleUsuario();
            }
        });

        if (btnDarBajaUsuario) {
            btnDarBajaUsuario.addEventListener('click', () => {
                const identifier = modalDetalleUsuario.dataset.identifier;
                const canBeDeleted = modalDetalleUsuario.dataset.canBeDeleted === 'true';
                const isActive = modalDetalleUsuario.dataset.active === 'true'; // Leer estado
                const blockingReason = modalDetalleUsuario.dataset.blockingReason;
                const warningsStr = modalDetalleUsuario.dataset.warnings || '';
                
                if (!identifier) return;

                // SI EL USUARIO ESTÁ INACTIVO -> DAR DE ALTA
                if (!isActive) {
                    ModalConfirmacion.show(
                        'Reactivar Usuario',
                        '¿Está seguro de que desea reactivar este usuario? Tendrá acceso nuevamente al sistema.',
                        () => {
                            darDeAltaUsuario(identifier);
                        }
                    );
                    return;
                }

                // SI EL USUARIO ESTÁ ACTIVO -> DAR DE BAJA
                // 1. Si hay bloqueo, mostrar error y no continuar
                if (!canBeDeleted) {
                    mostrarNotificacion(blockingReason || 'No se puede dar de baja al usuario.', 'error');
                    // Opcional: Usar un alert modal si mostrarNotificacion es muy sutil
                    ModalConfirmacion.show(
                        '⛔ ACCIÓN NO PERMITIDA', 
                        blockingReason || 'El usuario no puede ser eliminado en este momento.'
                    ).then(() => {}); // Solo cerrar
                    return;
                }

                // 2. Construir mensaje de confirmación
                let mensaje = '¿Está seguro de que desea dar de baja a este usuario?';
                let type = 'warning'; // Por defecto

                if (warningsStr) {
                    const warnings = JSON.parse(warningsStr);
                    if (warnings.length > 0) {
                        mensaje = '⚠️ ADVERTENCIAS:\n\n' + warnings.map(w => '• ' + w).join('\n') + '\n\n' + mensaje;
                        type = 'danger'; // Rojo para advertencias fuertes
                    }
                }

                // 3. Mostrar modal de confirmación
                // Usamos callback ya que ModalConfirmacion.show no devuelve una promesa
                ModalConfirmacion.show(
                    'Confirmar Baja de Usuario', 
                    mensaje, 
                    () => {
                        darDeBajaUsuario(identifier);
                    }
                );
            });
        }
    }

    function darDeBajaUsuario(identifier) {
        // Obtener tokens CSRF del layout base
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const headers = {
            'Content-Type': 'application/json'
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(`/admin/usuarios/${identifier}`, {
            method: 'DELETE',
            headers: headers
        })
        .then(response => {
            if (response.status === 403) {
                throw new Error('No tiene permisos para realizar esta acción (Error 403)');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                mostrarNotificacion('Usuario dado de baja exitosamente', 'success');
                cerrarModalDetalleUsuario();
                loadUsuarios(currentPage); // Recargar tabla
            } else {
                mostrarNotificacion(data.message || 'Error al dar de baja', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            mostrarNotificacion(error.message || 'Error de conexión', 'error');
        });
    }

    function darDeAltaUsuario(identifier) {
        // Obtener tokens CSRF del layout base
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const headers = {
            'Content-Type': 'application/json'
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(`/admin/usuarios/${identifier}/reactivar`, {
            method: 'PUT',
            headers: headers
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                mostrarNotificacion('Usuario reactivado exitosamente', 'success');
                cerrarModalDetalleUsuario();
                loadUsuarios(currentPage); // Recargar tabla
            } else {
                mostrarNotificacion(data.message || 'Error al reactivar', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            mostrarNotificacion('Error de conexión', 'error');
        });
    }

    function updateActionButton(isActive) {
        if (!btnDarBajaUsuario) return;
        
        // true si es indefinido (por defecto activo)
        const active = isActive !== false && isActive !== 'false';

        if (active) {
            // Estado: ACTIVO -> Botón DAR DE BAJA
            btnDarBajaUsuario.innerHTML = '<i class="fas fa-user-slash"></i> Dar de Baja';
            btnDarBajaUsuario.className = 'btn btn-outline-danger';
        } else {
            // Estado: INACTIVO -> Botón DAR DE ALTA
            btnDarBajaUsuario.innerHTML = '<i class="fas fa-user-check"></i> Dar de Alta';
            btnDarBajaUsuario.className = 'btn btn-success'; // Verde para activar
        }
    }

    function mostrarModalDetalleUsuario(detalle, context = {}) {
        if (!modalDetalleUsuario) {
            return;
        }

        renderDetalleUsuario(detalle);

        if (context.identifier) {
            modalDetalleUsuario.dataset.identifier = context.identifier;
        } else if (detalle?.dni) {
            modalDetalleUsuario.dataset.identifier = String(detalle.dni);
        } else {
            delete modalDetalleUsuario.dataset.identifier;
        }

        // Guardar metadata de validación en el dataset del modal
        modalDetalleUsuario.dataset.canBeDeleted = detalle.canBeDeleted !== undefined ? detalle.canBeDeleted : 'true';
        modalDetalleUsuario.dataset.blockingReason = detalle.blockingReason || '';
        modalDetalleUsuario.dataset.warnings = JSON.stringify(detalle.warnings || []);
        modalDetalleUsuario.dataset.active = detalle.estadoBoolean !== undefined ? detalle.estadoBoolean : 'true'; // Nuevo: Estado activo/inactivo

        updateActionButton(detalle.estadoBoolean); // Actualizar UI del botón

        if (context.nombre) {
            modalDetalleUsuario.dataset.nombre = context.nombre;
        } else if (detalle?.nombreCompleto) {
            modalDetalleUsuario.dataset.nombre = detalle.nombreCompleto;
        } else {
            delete modalDetalleUsuario.dataset.nombre;
        }

        modalDetalleUsuario.style.display = 'flex';
        requestAnimationFrame(() => {
            modalDetalleUsuario.classList.add('show');
        });
    }

    function cerrarModalDetalleUsuario() {
        if (!modalDetalleUsuario) {
            return;
        }

        modalDetalleUsuario.classList.remove('show');
        setTimeout(() => {
            modalDetalleUsuario.style.display = 'none';
            delete modalDetalleUsuario.dataset.identifier;
            delete modalDetalleUsuario.dataset.nombre;
        }, 200);
    }

    window.cerrarModalDetalleUsuario = cerrarModalDetalleUsuario;

    function renderDetalleUsuario(detalle = {}) {
        if (!modalDetalleUsuario) {
            return;
        }

        const rolesRaw = Array.isArray(detalle.rolesRaw) ? detalle.rolesRaw.map((rol) => `${rol}`.toUpperCase()) : [];

        setModalTexto(modalDetalleRefs.nombre, detalle.nombreCompleto || combinarNombre(detalle.nombre, detalle.apellido));
        setModalTexto(modalDetalleRefs.dni, detalle.dni);
        setModalTexto(modalDetalleRefs.correo, detalle.correo);
        setModalTexto(modalDetalleRefs.telefono, detalle.telefono || 'No registrado');
        setModalTexto(modalDetalleRefs.genero, formatGenero(detalle.genero));
        setModalTexto(modalDetalleRefs.fechaNacimiento, formatFechaLarga(detalle.fechaNacimiento));
        setModalTexto(modalDetalleRefs.fechaRegistro, formatFechaLarga(detalle.fechaRegistro));

        renderEstadoDetalle(detalle);
        renderRolesDetalle(detalle.roles, rolesRaw);
        renderFotoDetalle(detalle);
        renderUbicacionDetalle(detalle);
        renderAlumnoDetalle(detalle, rolesRaw);
        renderDocenteDetalle(detalle, rolesRaw);
    }

    function setModalTexto(target, value) {
        if (!target) {
            return;
        }
        const texto = value != null && value !== '' ? value : '-';
        target.textContent = texto;
    }

    function combinarNombre(nombre, apellido) {
        if (!nombre && !apellido) {
            return '';
        }
        return [nombre, apellido].filter(Boolean).join(' ');
    }

    function formatGenero(generoValor) {
        if (!generoValor) {
            return 'No especificado';
        }

        const generoStr = `${generoValor}`.toUpperCase();
        const map = {
            MASCULINO: 'Masculino',
            FEMENINO: 'Femenino',
            OTRO: 'Otro'
        };
        return map[generoStr] || capitalizarTexto(generoValor);
    }

    function capitalizarTexto(valor) {
        if (!valor) {
            return '';
        }

        const texto = `${valor}`.toLowerCase();
        return texto.charAt(0).toUpperCase() + texto.slice(1);
    }

    function formatFechaLarga(valor) {
        if (!valor) {
            return '-';
        }

        const fecha = convertirAFecha(valor);
        if (!fecha) {
            return typeof valor === 'string' ? valor : '-';
        }

        return fecha.toLocaleDateString();
    }

    function convertirAFecha(valor) {
        if (!valor) {
            return null;
        }

        if (valor instanceof Date) {
            return Number.isNaN(valor.getTime()) ? null : valor;
        }

        const fecha = new Date(valor);
        return Number.isNaN(fecha.getTime()) ? null : fecha;
    }

    function renderEstadoDetalle(detalle) {
        const estadoBadge = modalDetalleRefs.estado;
        if (!estadoBadge) {
            return;
        }

        const literal = inferirEstadoLiteral(detalle);
        estadoBadge.textContent = literal === 'ACTIVO' ? 'Activo' : 'Inactivo';
        estadoBadge.className = `status-badge status-${literal.toLowerCase()}`;
    }

    function inferirEstadoLiteral(detalle = {}) {
        if (typeof detalle.estado === 'string') {
            return detalle.estado.toUpperCase();
        }
        if (typeof detalle.estadoBoolean === 'boolean') {
            return detalle.estadoBoolean ? 'ACTIVO' : 'INACTIVO';
        }
        if (typeof detalle.estado === 'boolean') {
            return detalle.estado ? 'ACTIVO' : 'INACTIVO';
        }
        return 'ACTIVO';
    }

    function renderRolesDetalle(rolesLegibles = [], rolesCrudos = []) {
        const container = modalDetalleRefs.roles;
        if (!container) {
            return;
        }

        container.innerHTML = '';

        if (!Array.isArray(rolesLegibles) || rolesLegibles.length === 0) {
            container.textContent = 'Sin roles asignados';
            return;
        }

        rolesLegibles.forEach((rolLegible, index) => {
            const rolCrudo = rolesCrudos[index] || `${rolLegible}`.toUpperCase();
            const badge = document.createElement('span');
            badge.className = `role-badge role-${rolCrudo.toLowerCase()}`;
            const iconClass = roleIcons[rolCrudo] || 'fas fa-user';
            badge.innerHTML = `<i class="${iconClass}"></i> ${rolLegible}`;
            container.appendChild(badge);
        });
    }

    function renderFotoDetalle(detalle = {}) {
        const fotoElemento = modalDetalleRefs.foto;
        const placeholder = modalDetalleRefs.sinFoto;

        const posiblesCampos = [detalle.fotoUrl, detalle.foto, detalle.fotoPerfil, detalle.imagenPerfil];
        const urlValida = posiblesCampos.find((valor) => valor && `${valor}`.trim() !== '');

        if (fotoElemento && placeholder) {
            if (urlValida) {
                fotoElemento.src = urlValida;
                fotoElemento.style.display = 'block';
                placeholder.style.display = 'none';
            } else {
                fotoElemento.src = '';
                fotoElemento.style.display = 'none';
                placeholder.style.display = 'flex';
            }
        }
        if (btnBorrarFotoUsuario) {
            btnBorrarFotoUsuario.style.display = urlValida ? 'inline-flex' : 'none';
        }
    }


    function initializeFotoDeleteForm() {
        if (!btnBorrarFotoForm) {
            return;
        }
        btnBorrarFotoForm.addEventListener('click', () => {
            const form = document.getElementById('user-form');
            const identifier = form?.dataset?.identifier || form?.dataset?.originalDni;
            if (!identifier) {
                showNotification('❌ No se pudo determinar el usuario', 'error');
                return;
            }
            confirmAction(
                'Eliminar foto',
                '¿Está seguro de que desea eliminar la foto de este usuario?',
                () => {
                    const headers = {};
                    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
                    fetch(`/admin/usuarios/${identifier}/foto`, {
                        method: 'DELETE',
                        headers
                    })
                    .then(async (resp) => {
                        let data = {};
                        try { data = await resp.json(); } catch (e) {}
                        if (!resp.ok || data.success === false) {
                            throw new Error(data.message || 'No se pudo borrar la foto');
                        }
                        // Actualizar preview del formulario
                        if (fotoPreview && uploadPlaceholder) {
                            fotoPreview.src = '';
                            fotoPreview.style.display = 'none';
                            uploadPlaceholder.style.display = 'flex';
                        }
                        btnBorrarFotoForm.style.display = 'none';
                        showNotification('✅ Foto eliminada', 'success');
                    })
                    .catch((error) => {
                        console.error('Error eliminando foto:', error);
                        showNotification(`❌ ${error.message || 'Error al eliminar foto'}`, 'error');
                    });
                }
            );
        });
    }

    function renderUbicacionDetalle(detalle = {}) {
        setModalTexto(modalDetalleRefs.pais, detalle?.pais?.nombre);
        setModalTexto(modalDetalleRefs.provincia, detalle?.provincia?.nombre);
        setModalTexto(modalDetalleRefs.ciudad, detalle?.ciudad?.nombre);
    }

    function renderAlumnoDetalle(detalle = {}, rolesRaw = []) {
        const contenedor = modalDetalleSections.alumno;
        if (!contenedor) {
            return;
        }

        const esAlumno = rolesRaw.includes('ALUMNO');
        contenedor.style.display = esAlumno ? 'block' : 'none';

        if (!esAlumno) {
            return;
        }

        setModalTexto(modalDetalleRefs.alumnoColegio, detalle.colegioEgreso || 'No registrado');
        setModalTexto(modalDetalleRefs.alumnoAnioEgreso, detalle.añoEgreso || 'No especificado');
        setModalTexto(modalDetalleRefs.alumnoUltimosEstudios, detalle.ultimosEstudios || 'No especificado');

        // Renderizar checkbox global de documentación
        renderDocumentacionGlobal(detalle);

        renderInscripcionesAlumno(detalle.inscripciones || []);
    }

    function renderDocumentacionGlobal(detalle) {
        const checkbox = modalDetalleRefs.alumnoDocCheck;
        const label = modalDetalleRefs.alumnoDocLabel;
        const container = modalDetalleRefs.alumnoDocumentacionContainer;

        if (!checkbox || !label || !container) return; // Si no existen en el DOM, ignorar

        // Solo mostrar para alumnos (siempre se llama desde renderAlumnoDetalle, pero por seguridad)
        // Ya validamos antes, así que asumimos que es alumno.
        
        const entregada = detalle.documentacionEntregada === true;
        
        checkbox.checked = entregada;
        checkbox.dataset.alumnoId = detalle.id; // Guardamos ID para el evento change
        
        updateDocumentacionLabel(entregada);

        // Añadir evento one-time o manejarlo globalmente (mejor global para evitar duplicados)
        // El evento onchange ya está en el HTML llamando a toggleDocumentacionGlobal(this)
    }

    function updateDocumentacionLabel(entregada) {
        const label = modalDetalleRefs.alumnoDocLabel;
        label.textContent = 'Documentación Entregada';
        if (entregada) {
            label.className = 'form-check-label text-success fw-bold';
        } else {
            label.className = 'form-check-label';
        }
    }

    // Función GLOBAL llamada desde el HTML
    window.toggleDocumentacionGlobal = function(checkbox) {
        const idAlumno = modalDetalleRefs.alumnoDocCheck.dataset.alumnoId;
        if (!idAlumno) return;

        const label = modalDetalleRefs.alumnoDocLabel;
        const originalState = !checkbox.checked;

        // Feedback visual
        label.textContent = 'Actualizando...';
        label.className = 'form-check-label text-muted';

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        fetch(`/admin/alumnos/${idAlumno}/toggle-documentacion`, {
            method: 'PUT',
            headers: headers
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                const checked = data.nuevoEstado;
                checkbox.checked = checked;
                updateDocumentacionLabel(checked);
                mostrarNotificacion('Estado de documentación actualizado', 'success');
            } else {
                throw new Error(data.message);
            }
        })
        .catch(err => {
            console.error(err);
            checkbox.checked = originalState;
            updateDocumentacionLabel(originalState);
            mostrarNotificacion('Error al actualizar: ' + err.message, 'error');
        });
    };

    function renderInscripcionesAlumno(inscripciones) {
        const container = modalDetalleRefs.alumnoInscripciones;
        if (!container) return; // Si no existe el contenedor en HTML, salir.

        container.innerHTML = '';

        if (!Array.isArray(inscripciones) || inscripciones.length === 0) {
            container.innerHTML = '<p class="text-muted small"><i class="fas fa-info-circle"></i> No hay inscripciones registradas.</p>';
            return;
        }

        const table = document.createElement('table');
        table.className = 'table table-sm table-borderless align-middle mb-0';
        table.style.fontSize = '0.9rem';
        table.innerHTML = `
            <thead class="table-light">
                <tr>
                    <th>Oferta Académica</th>
                    <th>Estado</th>
                    <th class="text-end">Acciones</th>
                </tr>
            </thead>
            <tbody></tbody>
        `;

        const tbody = table.querySelector('tbody');

        inscripciones.forEach(insc => {
            const tr = document.createElement('tr');
            const estadoClass = insc.estadoInscripcion ? 'text-success' : 'text-danger';
            const estadoIcon = insc.estadoInscripcion ? 'fa-check-circle' : 'fa-times-circle';
            const estadoText = insc.estadoInscripcion ? 'Activa' : 'Cancelada/Finalizada';
            
            tr.innerHTML = `
                <td><i class="fas fa-book-reader text-primary me-2"></i> ${insc.ofertaTitulo || 'Oferta sin título'}</td>
                <td><span class="${estadoClass}"><i class="fas ${estadoIcon}"></i> ${estadoText}</span></td>
                <td class="text-end">
                    ${insc.estadoInscripcion ? `
                        <button class="btn btn-sm btn-outline-danger" onclick="confirmarCancelarInscripcion(${insc.idInscripcion})" title="Cancelar Inscripción">
                            <i class="fas fa-ban"></i> Cancelar
                        </button>
                    ` : '<span class="text-muted small">-</span>'}
                </td>
            `;
            tbody.appendChild(tr);
        });

        container.appendChild(table);
    }


    window.confirmarCancelarInscripcion = function(idInscripcion) {
        ModalConfirmacion.show(
            'Cancelar Inscripción',
            '¿Está seguro de que desea cancelar esta inscripción? El alumno perderá acceso al curso. Esta acción no se puede deshacer fácilmente.',
            () => cancelarInscripcion(idInscripcion)
        );
    };

    function cancelarInscripcion(idInscripcion) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        fetch(`/admin/inscripciones/${idInscripcion}/cancelar`, {
            method: 'PUT',
            headers: headers
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                mostrarNotificacion('Inscripción cancelada correctamente', 'success');
                // Recargar el modal si es posible, o cerrar y recargar tabla
                cerrarModalDetalleUsuario();
                // Opcional: recargar solo el modal, pero requiere ID de usuario
                loadUsuarios(currentPage);
            } else {
                mostrarNotificacion(data.message || 'Error al cancelar inscripción', 'error');
            }
        })
        .catch(err => {
            mostrarNotificacion('Error de conexión', 'error');
        });
    }

    function renderDocenteDetalle(detalle = {}, rolesRaw = []) {
        const contenedor = modalDetalleSections.docente;
        if (!contenedor) {
            return;
        }

        const esDocente = rolesRaw.includes('DOCENTE');
        contenedor.style.display = esDocente ? 'block' : 'none';

        if (!esDocente) {
            return;
        }

        setModalTexto(modalDetalleRefs.docenteMatricula, detalle.matricula || 'No registrada');
        setModalTexto(modalDetalleRefs.docenteExperiencia, formatExperienciaDocente(detalle.experiencia));
        renderHorariosDetalle(detalle.horariosDisponibilidad);
    }

    function renderHorariosDetalle(horarios = []) {
        const contenedor = modalDetalleRefs.docenteHorarios;
        if (!contenedor) {
            return;
        }

        contenedor.innerHTML = '';

        if (!Array.isArray(horarios) || horarios.length === 0) {
            contenedor.textContent = 'Sin horarios cargados';
            return;
        }

        horarios.forEach((horario) => {
            const chip = document.createElement('div');
            chip.className = 'horario-item';

            const dia = formatearDiaParaMostrar(horario?.diaSemana || horario?.dia);
            const inicio = (horario?.horaInicio || '').toString().substring(0, 5);
            const fin = (horario?.horaFin || '').toString().substring(0, 5);

            chip.innerHTML = `<i class="fas fa-clock"></i> ${dia || 'Día no especificado'} ${inicio || '--:--'} - ${fin || '--:--'}`;
            contenedor.appendChild(chip);
        });
    }

    function formatExperienciaDocente(valor) {
        if (valor == null) {
            return 'No especificado';
        }

        const numero = Number(valor);
        if (Number.isNaN(numero)) {
            return `${valor}`;
        }

        const sufijo = numero === 1 ? 'año' : 'años';
        return `${numero} ${sufijo}`;
    }

    function initializeLocationSystem() {
        console.log("📍 Inicializando sistema de ubicación...");
        
        if (!paisSelect) {
            console.error("❌ No se encontró el select de país");
            return;
        }

        if (locationSystemInitialized) {
            return;
        }
        locationSystemInitialized = true;
    
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
        
        return fetch(`/api/ubicaciones/provincias/${paisCode}`)
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
                return provincias;
            })
            .catch(error => {
                console.error('❌ Error cargando provincias:', error);
                provinciaSelect.innerHTML = '<option value="">Error al cargar provincias</option>';
                throw error;
            });
    }
    
    function cargarCiudadesAdmin(paisCode, provinciaCode) {
        console.log("🏙️ Cargando ciudades para país:", paisCode, "provincia:", provinciaCode);
        
        ciudadSelect.innerHTML = '<option value="">Cargando ciudades...</option>';
        ciudadSelect.disabled = true;
        
        return fetch(`/api/ubicaciones/ciudades/${paisCode}/${provinciaCode}`)
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
                    ciudadSelect.required = true;
                    ciudadesDisponibles = true;
                } else {
                    ciudadSelect.innerHTML = '<option value="">No hay ciudades disponibles</option>';
                    ciudadSelect.disabled = true;
                    ciudadSelect.required = false;
                    ciudadesDisponibles = false;
                }
                
                document.getElementById('ciudadId').value = '';
                return ciudades;
            })
            .catch(error => {
                console.error('❌ Error cargando ciudades:', error);
                ciudadSelect.innerHTML = '<option value="">Error al cargar ciudades</option>';
                ciudadSelect.disabled = true;
                ciudadSelect.required = false;
                ciudadesDisponibles = false;
                throw error;
            });
    }

    async function setUbicacionDesdeDetalle(detalleUsuario) {
        if (!detalleUsuario || !paisSelect) {
            return;
        }

        const paisHidden = document.getElementById('paisCodigo');
        const provinciaHidden = document.getElementById('provinciaCodigo');
        const ciudadHidden = document.getElementById('ciudadId');

        const paisCodigoValor = detalleUsuario.pais?.codigo || '';
        const provinciaCodigoValor = detalleUsuario.provincia?.codigo || '';
        const ciudadIdValor = detalleUsuario.ciudad?.id != null ? String(detalleUsuario.ciudad.id) : '';

        console.log('📍 Cargando ubicación del usuario:', {
            pais: paisCodigoValor,
            provincia: provinciaCodigoValor,
            ciudad: ciudadIdValor
        });

        try {
            // 1. Primero seleccionar y cargar país
            if (paisCodigoValor) {
                const paisOption = Array.from(paisSelect.options).find(option => option.getAttribute('data-codigo') === paisCodigoValor);
                if (paisOption) {
                    paisSelect.value = paisOption.value;
                    if (paisHidden) {
                        paisHidden.value = paisCodigoValor;
                    }
                    console.log('✅ País seleccionado:', paisOption.textContent);
                    
                    // Cargar provincias y ESPERAR a que terminen de cargar
                    await cargarProvinciasAdmin(paisCodigoValor);
                } else {
                    console.warn('⚠️ No se encontró la opción de país con código:', paisCodigoValor);
                }
            }

            // 2. DESPUÉS de cargar provincias, seleccionar provincia
            if (provinciaCodigoValor) {
                // Ahora sí buscar la provincia DESPUÉS de que se cargaron
                const provinciaOption = Array.from(provinciaSelect.options).find(option => option.getAttribute('data-code') === provinciaCodigoValor);
                if (provinciaOption) {
                    provinciaSelect.value = provinciaOption.value;
                    if (provinciaHidden) {
                        provinciaHidden.value = provinciaCodigoValor;
                    }
                    console.log('✅ Provincia seleccionada:', provinciaOption.textContent);

                    const paisCodigoActual = paisHidden ? paisHidden.value : paisCodigoValor;
                    if (paisCodigoActual) {
                        // Cargar ciudades y ESPERAR a que terminen de cargar
                        await cargarCiudadesAdmin(paisCodigoActual, provinciaCodigoValor);
                    }
                } else {
                    console.warn('⚠️ No se encontró la opción de provincia con código:', provinciaCodigoValor);
                }
            }

            // 3. DESPUÉS de cargar ciudades, seleccionar ciudad
            if (ciudadIdValor) {
                // Ahora sí buscar la ciudad DESPUÉS de que se cargaron
                const ciudadOption = Array.from(ciudadSelect.options).find(option => `${option.getAttribute('data-id')}` === ciudadIdValor);
                if (ciudadOption) {
                    ciudadSelect.value = ciudadOption.value;
                    if (ciudadHidden) {
                        ciudadHidden.value = ciudadIdValor;
                    }
                    console.log('✅ Ciudad seleccionada:', ciudadOption.textContent);
                } else {
                    console.warn('⚠️ No se encontró la opción de ciudad con ID:', ciudadIdValor);
                }
            }

            console.log('✅ Ubicación cargada correctamente');
        } catch (error) {
            console.error('❌ Error asignando ubicación del usuario:', error);
        }
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
        
        if (ciudadesDisponibles) {
            if (!ciudadId) {
                showFieldError(ciudadSelect, 'Por favor, selecciona una ciudad');
                isValid = false;
            } else {
                hideFieldError(ciudadSelect);
            }
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

    function clearFieldErrors() {
        const form = document.getElementById('user-form');
        if (!form) {
            return;
        }

        form.querySelectorAll('.error').forEach(element => {
            element.classList.remove('error');
        });

        form.querySelectorAll('.error-message').forEach(message => {
            message.remove();
        });
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
                const form = document.getElementById('user-form');
                const isEditMode = form?.dataset?.mode === 'edit';
                if (isEditMode) {
                    confirmAction(
                        'Cancelar edición',
                        '¿Está seguro de que desea cancelar? Los cambios no guardados se perderán.',
                        hideForm
                    );
                    return;
                }
                hideForm();
            });
        }

        if (btnCancelForm) {
            btnCancelForm.addEventListener('click', function() {
                const form = document.getElementById('user-form');
                const isEditMode = form?.dataset?.mode === 'edit';
                if (isEditMode) {
                    confirmAction(
                        'Cancelar edición',
                        '¿Está seguro de que desea cancelar? Los cambios no guardados se perderán.',
                        hideForm
                    );
                    return;
                }
                hideForm();
            });
        }
    }

    function showForm() {
        formContainer.style.display = 'block';
        setTimeout(() => {
            formContainer.classList.add('show');
            initializeLocationSystem();
            // Configurar fecha máxima del input de fecha al abrir el formulario
            if (typeof initializeDateInput === 'function') {
                initializeDateInput();
            }
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
        const form = document.getElementById('user-form');
        if (form) {
            setFormReadOnly(false);
            form.reset();
            form.dataset.mode = 'create';
            form.dataset.readonly = 'false';
            delete form.dataset.identifier;
            delete form.dataset.originalDni;
        }

        usuarioEnEdicion = null;

        const headerTitle = document.querySelector('#form-container .form-header h3');
        if (headerTitle) {
            headerTitle.innerHTML = '<i class="fas fa-user-plus"></i> Registrar Nuevo Usuario';
        }

        if (form) {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="fas fa-save"></i> Registrar Usuario';
            }
        }

        if (btnCancelForm && defaultCancelMarkup) {
            btnCancelForm.innerHTML = defaultCancelMarkup;
        }

        resetFotoUpload();
        clearHorariosDocenteTable();
        hideDocenteFields();
        hideAlumnoFields();
        resetRoles();
        resetLocation();
        removeAllSpecificRequired();
        
        clearFieldErrors();
        
        const contador = document.getElementById('contador-horarios');
        if (contador) {
            contador.style.display = 'none';
        }
    }

    function setFormReadOnly(readOnly) {
        const form = document.getElementById('user-form');
        if (!form) {
            return;
        }

        form.dataset.readonly = readOnly ? 'true' : 'false';

        const interactiveElements = form.querySelectorAll('input, select, textarea, button');
        interactiveElements.forEach(element => {
            const shouldSkip = element.id === 'btn-cancel-form' || element.id === 'btn-close-form';
            if (shouldSkip) {
                return;
            }

            if (readOnly) {
                if (element.disabled) {
                    element.dataset.disabledByView = 'persist';
                } else {
                    element.dataset.disabledByView = 'true';
                    element.disabled = true;
                }
            } else if (element.dataset.disabledByView) {
                if (element.dataset.disabledByView === 'true') {
                    element.disabled = false;
                }
                delete element.dataset.disabledByView;
            }
        });

        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            if (readOnly) {
                submitBtn.dataset.hiddenByView = 'true';
                submitBtn.style.display = 'none';
            } else if (submitBtn.dataset.hiddenByView === 'true') {
                submitBtn.style.display = '';
                delete submitBtn.dataset.hiddenByView;
            }
        }

        const addHorarioBtn = document.getElementById('btn-add-horario-docente');
        if (addHorarioBtn) {
            if (readOnly) {
                if (!addHorarioBtn.disabled) {
                    addHorarioBtn.dataset.disabledByView = 'true';
                    addHorarioBtn.disabled = true;
                } else {
                    addHorarioBtn.dataset.disabledByView = 'persist';
                }
            } else if (addHorarioBtn.dataset.disabledByView) {
                if (addHorarioBtn.dataset.disabledByView === 'true') {
                    addHorarioBtn.disabled = false;
                }
                delete addHorarioBtn.dataset.disabledByView;
            }
        }

        const horarioDeleteBtns = form.querySelectorAll('.btn-delete-horario');
        horarioDeleteBtns.forEach(btn => {
            if (readOnly) {
                btn.dataset.hiddenByView = 'true';
                btn.style.display = 'none';
            } else if (btn.dataset.hiddenByView === 'true') {
                btn.style.display = '';
                delete btn.dataset.hiddenByView;
            }
        });

        if (selectedChipsContainer) {
            selectedChipsContainer.style.pointerEvents = readOnly ? 'none' : '';
            selectedChipsContainer.style.opacity = readOnly ? '0.7' : '';
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

    // ✅ Función para validar la fecha en el formulario
    function validateFechaNacimiento() {
        const fechaInput = document.getElementById('fechaNacimiento');
        let isValid = true;
    
        if (!fechaInput.value) {
            showFieldError(fechaInput, 'La fecha de nacimiento es obligatoria');
            isValid = false;
        } else {
            const fechaSeleccionada = new Date(fechaInput.value);
            const hoy = new Date();
            const hace16Anos = new Date(hoy.getFullYear() - 16, hoy.getMonth(), hoy.getDate());
            
            if (fechaSeleccionada > hace16Anos) {
                showFieldError(fechaInput, 'El usuario debe tener al menos 16 años');
                isValid = false;
            } else {
                hideFieldError(fechaInput);
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
        applyRoleSelection(null);
    }

    function initializeRoles() {
        selectedRolesContainer = document.getElementById('selected-roles');
        selectedChipsContainer = document.getElementById('selected-roles-chips');

        if (rolSelect) {
            rolSelect.addEventListener('change', function() {
                applyRoleSelection(this.value || null);
            });
        }

        window.removeSelectedRole = function() {
            applyRoleSelection(null);
        };

        window.getSelectedRole = function() {
            return selectedRoles.length > 0 ? selectedRoles[0] : null;
        };
    }

    // ========== FUNCIONES GLOBALES (FUERA DE initializeRoles) ==========

    function setAlumnoRequiredAttributes(isRequired) {
        const colegioEgreso = document.getElementById('colegioEgreso');
        const añoEgreso = document.getElementById('añoEgreso');
        const ultimosEstudios = document.getElementById('ultimosEstudios');

        const applyRequired = (element) => {
            if (!element) return;
            if (isRequired) {
                element.setAttribute('required', 'required');
                element.setAttribute('aria-required', 'true');
            } else {
                element.removeAttribute('required');
                element.removeAttribute('aria-required');
            }
        };

        applyRequired(colegioEgreso);
        applyRequired(añoEgreso);
        applyRequired(ultimosEstudios);
    }

    function applyRoleSelection(rol) {
        const normalizedRole = rol ? `${rol}`.toUpperCase() : '';

        selectedRoles = normalizedRole ? [normalizedRole] : [];

        if (rolSelect) {
            rolSelect.value = normalizedRole;
        }

        updateSelectedRoles();

        hideAllSpecificFields();
        removeAllSpecificRequired();

        if (!normalizedRole) {
            return;
        }

        switch (normalizedRole) {
            case 'DOCENTE':
                showDocenteFields(true);
                break;
            case 'ALUMNO':
                showAlumnoFields(true);
                setAlumnoRequiredAttributes(true);
                break;
            default:
                break;
        }
    }

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
        
        setAlumnoRequiredAttributes(false);

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
        function formatearDiaParaMostrar(diaClave) {
            if (!diaClave) {
                return '';
            }

            const mapa = {
                LUNES: 'Lunes',
                MARTES: 'Martes',
                MIERCOLES: 'Miércoles',
                JUEVES: 'Jueves',
                VIERNES: 'Viernes',
                SABADO: 'Sábado',
                DOMINGO: 'Domingo'
            };

            return mapa[diaClave.toUpperCase()] || diaClave;
        }

        function crearFilaHorario(dia, horaDesde, horaHasta) {
            const diaClave = normalizarNombreDia(dia);
            const diaMostrar = formatearDiaParaMostrar(diaClave);

            const row = document.createElement('tr');
            row.dataset.dia = diaClave;
            row.innerHTML = `
                <td>${diaMostrar}</td>
                <td>${horaDesde} - ${horaHasta}</td>
                <td class="actions">
                    <button type="button" class="btn-icon btn-delete btn-delete-horario" title="Eliminar">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            return row;
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
    
            // ✅ VERIFICAR HORARIOS SOLAPADOS
            if (existeHorarioSolapado(dia, horaDesde, horaHasta)) {
                showNotification('Ya existe un horario para este día en el mismo rango de horas', 'error');
                return;
            }
    
            const newRow = crearFilaHorario(dia, horaDesde, horaHasta);
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
            const toMinutes = (valor) => {
                if (!valor) return 0;
                const partes = valor.toString().split(':');
                const h = parseInt(partes[0] || '0', 10);
                const m = parseInt(partes[1] || '0', 10);
                return (h * 60) + m;
            };
            const nuevoInicio = toMinutes(nuevaHoraDesde);
            const nuevoFin = toMinutes(nuevaHoraHasta);
            
            for (let fila of filas) {
                const diaExistente = fila.cells[0].textContent;
                const horarioExistente = fila.cells[1].textContent;
                const [horaDesdeExistente, horaHastaExistente] = horarioExistente.split(' - ');
                
                if (diaExistente === dia) {
                    const existenteInicio = toMinutes(horaDesdeExistente);
                    const existenteFin = toMinutes(horaHastaExistente);
                    // Verificar solapamiento (permitir que termine exactamente cuando otro comienza)
                    if (nuevoInicio < existenteFin && nuevoFin > existenteInicio) {
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

        function populateHorariosDocente(horarios = []) {
            clearHorariosDocenteTable();

            if (!Array.isArray(horarios) || horarios.length === 0) {
                return;
            }

            horarios.forEach(horario => {
                if (!horariosDocenteTableBody) {
                    return;
                }

                const dia = horario?.diaSemana || horario?.dia;
                const horaInicioBruta = horario?.horaInicio;
                const horaFinBruta = horario?.horaFin;

                if (!dia || !horaInicioBruta || !horaFinBruta) {
                    return;
                }

                const horaInicio = `${horaInicioBruta}`.substring(0, 5);
                const horaFin = `${horaFinBruta}`.substring(0, 5);
                const row = crearFilaHorario(dia, horaInicio, horaFin);
                horariosDocenteTableBody.appendChild(row);
            });

            actualizarContadorHorarios();
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

        const filtersActive = hasActiveFilters(filters);
        if (filtersActive) {
            loadUsuarios(1, { forceAll: true, filtersOverride: filters });
            return;
        }

        // Aplicar filtros a la tabla
        filterTable(filters);
        setPaginationControlsVisible(true);
    }
    
    function clearFilters() {
        if (searchInput) searchInput.value = '';
        if (filtroRol) filtroRol.value = '';
        if (filtroEstado) filtroEstado.value = '';
        if (filtroGenero) filtroGenero.value = '';

        applyFilters();
        pageSize = defaultPageSize;
        setPaginationControlsVisible(true);
        loadUsuarios(1);
    }

    function hasActiveFilters(filters = {}) {
        return Object.values(filters).some((value) => value != null && `${value}`.trim() !== '');
    }
    
    function filterTable(filters) {
        const table = document.getElementById('usuarios-table');
        if (!table) return;
    
            const rows = table.querySelectorAll('tbody tr');
            let visibleCount = 0;

            const normalizedFilters = {
                search: filters.search ? `${filters.search}`.toLowerCase() : '',
                rol: filters.rol ? `${filters.rol}`.toUpperCase() : '',
                estado: filters.estado ? `${filters.estado}`.toUpperCase() : '',
                genero: filters.genero ? `${filters.genero}`.toUpperCase() : ''
            };

            rows.forEach(row => {
                const cells = row.querySelectorAll('td');
                if (cells.length === 0) return;

                const dni = cells[0]?.textContent?.toLowerCase() || '';
                const nombre = cells[1]?.textContent?.toLowerCase() || '';
                const correo = cells[2]?.textContent?.toLowerCase() || '';
                const roles = cells[3]?.textContent || '';
                const estado = cells[4]?.textContent || '';
    
                let showRow = true;
    
                // Filtro de búsqueda
                if (normalizedFilters.search) {
                    const searchMatch = nombre.includes(normalizedFilters.search) ||
                                      dni.includes(normalizedFilters.search) ||
                                      correo.includes(normalizedFilters.search);
                    if (!searchMatch) showRow = false;
                }
    
                // Filtro de rol
                if (normalizedFilters.rol && !roles.toUpperCase().includes(normalizedFilters.rol)) {
                    showRow = false;
                }
    
                // Filtro de estado
                if (normalizedFilters.estado && !estado.toUpperCase().includes(normalizedFilters.estado)) {
                    showRow = false;
                }
    
                row.style.display = showRow ? '' : 'none';
                if (showRow) visibleCount++;
            });
    
        updateTableStats(visibleCount);
        return visibleCount;
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
                table.addEventListener('click', function(event) {
                    const button = event.target.closest('button[data-action]');
                    if (!button) {
                        return;
                    }

                    const row = button.closest('tr');
                    const action = button.dataset.action;
                    const cacheKey = button.dataset.userKey || row?.dataset.userKey || button.dataset.dni || row?.dataset.dni || '';
                    const usuario = cacheKey ? usuariosCache.get(cacheKey) : null;
                    const nombre = button.dataset.nombre || row?.dataset.nombre || usuario?.nombreCompleto || '';
                    const dni = button.dataset.dni || row?.dataset.dni || usuario?.dni || '';
                    const identifier = cacheKey || dni;

                    if (!identifier) {
                        showNotification('No se pudo determinar el usuario seleccionado', 'error');
                        return;
                    }

                    const context = {
                        identifier,
                        nombre,
                        usuario,
                        dni
                    };

                    if (action === 'edit') {
                        editUsuario(context);
                    } else if (action === 'delete') {
                        deleteUsuario(context);
                    } else if (action === 'view') {
                        viewUsuario(context);
                    }
                });
            }

            // Cargar usuarios al inicializar (página 1)
            loadUsuarios(1);
        }

        async function obtenerUsuarioDetalle(identifier) {
            if (!identifier) {
                throw new Error('Identificador inválido para consultar usuario');
            }

            const headers = {
                'Accept': 'application/json'
            };

            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch(`/admin/usuarios/${encodeURIComponent(identifier)}`, {
                method: 'GET',
                headers
            });

            const payload = await response.json().catch(() => ({}));

            if (!response.ok || payload.success === false) {
                const message = payload?.message || `Error al obtener el usuario (HTTP ${response.status})`;
                throw new Error(message);
            }

            const detalle = payload.data || {};

            usuariosCache.set(String(identifier), detalle);
            if (detalle.dni) {
                usuariosCache.set(String(detalle.dni), detalle);
            }

            return detalle;
        }

        async function hydrateUserForm(detalle, options = {}) {
            const { mode = 'edit', identifier } = options;
            const form = document.getElementById('user-form');

            if (!form) {
                return;
            }

            // ✅ NO resetear el formulario completo si estamos en modo edición
            // Solo limpiamos errores y reseteamos algunos campos específicos
            if (mode === 'create') {
                resetForm();
            } else {
                // En modo edición/view, solo limpiar errores y preparar el formulario
                clearFieldErrors();
                clearHorariosDocenteTable();
                setFormReadOnly(false);
            }

            if (!locationSystemInitialized) {
                initializeLocationSystem();
            }

            form.dataset.mode = mode;
            form.dataset.readonly = mode === 'view' ? 'true' : 'false';

            if (identifier) {
                form.dataset.identifier = identifier;
            }

            if (detalle?.dni) {
                form.dataset.originalDni = detalle.dni;
            }

            usuarioEnEdicion = detalle;

            const headerTitle = document.querySelector('#form-container .form-header h3');
            if (headerTitle) {
                if (mode === 'edit') {
                    headerTitle.innerHTML = '<i class="fas fa-user-edit"></i> Editar Usuario';
                } else if (mode === 'view') {
                    headerTitle.innerHTML = '<i class="fas fa-user"></i> Detalle del Usuario';
                }
            }

            if (btnCancelForm) {
                if (mode === 'view') {
                    btnCancelForm.innerHTML = '<i class="fas fa-times"></i> Cerrar';
                } else if (defaultCancelMarkup) {
                    btnCancelForm.innerHTML = defaultCancelMarkup;
                }
            }

            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn && mode === 'edit') {
                submitBtn.innerHTML = '<i class="fas fa-save"></i> Guardar Cambios';
            }

            const setValue = (id, value) => {
                const element = document.getElementById(id);
                if (element) {
                    element.value = value != null ? value : '';
                }
            };

            const setSelectValue = (id, value) => {
                const element = document.getElementById(id);
                if (element) {
                    element.value = value != null ? `${value}` : '';
                }
            };

            setValue('dni', detalle?.dni);
            setValue('nombre', detalle?.nombre);
            setValue('apellido', detalle?.apellido);
            setValue('correo', detalle?.correo);
            setValue('telefono', detalle?.telefono);
            setValue('fechaNacimiento', formatearFechaIso(detalle?.fechaNacimiento));
            setSelectValue('genero', detalle?.genero);

            // Foto en formulario (solo visualización)
            const fotoUrl = detalle?.foto || detalle?.fotoUrl || detalle?.fotoPerfil || detalle?.imagenPerfil;
            if (fotoPreview && uploadPlaceholder) {
                if (fotoUrl) {
                    fotoPreview.src = fotoUrl;
                    fotoPreview.style.display = 'block';
                    uploadPlaceholder.style.display = 'none';
                } else {
                    fotoPreview.src = '';
                    fotoPreview.style.display = 'none';
                    uploadPlaceholder.style.display = 'flex';
                }
            }
            if (btnBorrarFotoForm) {
                btnBorrarFotoForm.style.display = fotoUrl ? 'inline-flex' : 'none';
            }

            const estadoInferido = typeof detalle?.estado === 'string'
                ? detalle.estado
                : (detalle?.estadoBoolean === false ? 'INACTIVO' : 'ACTIVO');
            setSelectValue('estado', estadoInferido);

            const rolPrincipal = (detalle?.rolPrincipal || (Array.isArray(detalle?.rolesRaw) ? detalle.rolesRaw[0] : '') || '').toUpperCase();
            applyRoleSelection(rolPrincipal);

            console.log('👤 Rol principal del usuario:', rolPrincipal);

            if (rolPrincipal === 'ALUMNO') {
                console.log('📚 Cargando datos de alumno:', {
                    colegioEgreso: detalle?.colegioEgreso,
                    añoEgreso: detalle?.añoEgreso,
                    ultimosEstudios: detalle?.ultimosEstudios
                });
                setValue('colegioEgreso', detalle?.colegioEgreso);
                setValue('añoEgreso', detalle?.añoEgreso);
                setSelectValue('ultimosEstudios', detalle?.ultimosEstudios);
            } else if (rolPrincipal === 'DOCENTE') {
                console.log('👨‍🏫 Cargando datos de docente:', {
                    matricula: detalle?.matricula,
                    experienciaActualizada: detalle?.experiencia,
                    experienciaBase: detalle?.experienciaBase,
                    horariosDisponibilidad: detalle?.horariosDisponibilidad
                });
                setValue('matricula', detalle?.matricula);
                const experienciaBase = detalle?.experienciaBase ?? detalle?.experiencia;
                setValue('experiencia', experienciaBase);
                populateHorariosDocente(detalle?.horariosDisponibilidad || []);
            } else {
                populateHorariosDocente([]);
            }

            await setUbicacionDesdeDetalle(detalle);

            if (mode === 'view') {
                setFormReadOnly(true);
            } else {
                setFormReadOnly(false);
            }

            setTimeout(() => {
                if (mode === 'edit') {
                    document.getElementById('nombre')?.focus();
                }
            }, 150);
        }

        async function editUsuario(context = {}) {
            const { identifier, nombre } = context;

            if (!identifier) {
                showNotification('❌ No se pudo determinar el usuario a editar', 'error');
                return;
            }

            try {
                // showLoading(`Cargando datos de ${nombre || 'usuario'}...`);
                const detalle = await obtenerUsuarioDetalle(identifier);
                await hydrateUserForm(detalle, { mode: 'edit', identifier });
                showForm();
            } catch (error) {
                console.error('Error cargando usuario para edición:', error);
                showNotification(`❌ ${error.message || 'No se pudo cargar el formulario de edición'}`, 'error', 10000);
            } finally {
                // hideLoading();
            }
        }

        async function viewUsuario(context = {}) {
            const { identifier, nombre } = context;

            if (!identifier) {
                showNotification('❌ No se pudo determinar el usuario a visualizar', 'error');
                return;
            }

            try {
                // showLoading(`Cargando detalles de ${nombre || 'usuario'}...`);
                const detalle = await obtenerUsuarioDetalle(identifier);
                mostrarModalDetalleUsuario(detalle, context);
            } catch (error) {
                console.error('Error cargando usuario para visualización:', error);
                showNotification(`❌ ${error.message || 'No se pudo mostrar el usuario seleccionado'}`, 'error', 10000);
            } finally {
                // hideLoading();
            }
        }

        function deleteUsuario(context = {}) {
            const { identifier, nombre, dni } = context;

            if (!identifier) {
                showNotification('❌ No se pudo determinar el usuario a eliminar', 'error');
                return;
            }

            const displayName = nombre || 'usuario';
            const label = dni || identifier;

            ModalConfirmacion.show(
                'Confirmar Baja',
                `¿Está seguro de que desea dar de baja al usuario "${displayName}" (ID: ${label})?`,
                () => {
                    const headers = {
                        'Accept': 'application/json'
                    };

                    if (csrfToken && csrfHeader) {
                        headers[csrfHeader] = csrfToken;
                    }

                    // showLoading(`Eliminando ${displayName}...`);

                    fetch(`/admin/usuarios/${encodeURIComponent(identifier)}`, {
                        method: 'DELETE',
                        headers
                    })
                    .then(response => response.json().catch(() => ({})).then(body => ({ response, body })))
                    .then(({ response, body }) => {
                        if (!response.ok || body.success === false) {
                            const message = body?.message || `Error al eliminar el usuario (HTTP ${response.status})`;
                            throw new Error(message);
                        }

                        showNotification(body?.message || '✅ Usuario eliminado correctamente', 'success');
                        usuariosCache.delete(String(identifier));
                        if (dni) {
                            usuariosCache.delete(String(dni));
                        }
                        loadUsuarios(Math.max(1, currentPage));
                    })
                    .catch(error => {
                        console.error('Error eliminando usuario:', error);
                        showNotification(`❌ ${error.message || 'No se pudo eliminar el usuario'}`, 'error', 10000);
                    })
                    .finally(() => {
                        // hideLoading();
                    });
                }
            );
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
                #correo[required],
                #telefono[required],
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
            
            // ✅ Validar número de teléfono (solo números, mínimo 10 dígitos)
            const telefono = document.getElementById('telefono');
            if (telefono && telefono.value.trim()) {
                const telefonoLimpio = telefono.value.replace(/\D/g, ''); // Remover todo lo que no sea dígito
                if (telefonoLimpio.length < 10) {
                    showFieldError(telefono, 'El teléfono debe tener mínimo 10 dígitos');
                    isValid = false;
                } else if (!/^\d+$/.test(telefonoLimpio)) {
                    showFieldError(telefono, 'El teléfono solo debe contener números');
                    isValid = false;
                } else {
                    hideFieldError(telefono);
                    // Actualizar el valor con solo números
                    telefono.value = telefonoLimpio;
                }
            }
        
            // Validar que un rol esté seleccionado
            if (selectedRoles.length === 0) {
                showNotification('Debe seleccionar un rol para el usuario', 'error');
                isValid = false;
            }
        
            // ✅ Validar ubicación
            if (!validateLocation()) {
                isValid = false;
            }

            // ✅ Validar campos de docente si corresponde
            if (selectedRoles.includes('DOCENTE')) {
                const matricula = document.getElementById('matricula');
                const experiencia = document.getElementById('experiencia');

                if (!matricula || !matricula.value.trim()) {
                    if (matricula) showFieldError(matricula, 'La matrícula es obligatoria');
                    isValid = false;
                } else if (matricula) {
                    hideFieldError(matricula);
                }

                if (!experiencia || !experiencia.value.trim()) {
                    if (experiencia) showFieldError(experiencia, 'La experiencia es obligatoria');
                    isValid = false;
                } else if (experiencia) {
                    hideFieldError(experiencia);
                }
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
                console.log('🧾 Submit de formulario detectado');
                
                if (form.dataset.mode === 'view') {
                    return;
                }

                if (validateForm()) {
                    submitForm();
                } else {
                    console.warn('⚠️ Validación fallida, no se envía');
                }
            });
        }
    
        function submitForm(skipConfirm = false) {
            console.log('🚀 Enviando formulario de usuario...');
            
            const form = document.getElementById('user-form');
            const formData = new FormData(form);

            const isEditMode = form.dataset.mode === 'edit';
            const identifier = form.dataset.identifier || form.dataset.originalDni || formData.get('dni');

            if (isEditMode && (!identifier || `${identifier}`.trim() === '')) {
                showNotification('❌ No se pudo determinar el usuario a editar', 'error');
                return;
            }
            
            if (isEditMode && !skipConfirm) {
                confirmAction(
                    'Confirmar modificación',
                    '¿Está seguro de que desea guardar los cambios del usuario?',
                    () => submitForm(true)
                );
                return;
            }

            const loadingMessage = isEditMode ? 'Guardando cambios...' : 'Registrando usuario...';
            // showLoading(loadingMessage);

            const submitBtn = form.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;
            submitBtn.innerHTML = `<i class="fas fa-spinner fa-spin"></i> ${isEditMode ? 'Modificando...' : 'Registrando...'}`;
            submitBtn.disabled = true;
            
            if (selectedRoles.length > 0) {
                formData.set('rol', selectedRoles[0]);
            }
            
            // ✅ LOG: Verificar datos de ubicación
            console.log('📍 Datos de ubicación enviados:', {
                paisCodigo: formData.get('paisCodigo'),
                provinciaCodigo: formData.get('provinciaCodigo'),
                ciudadId: formData.get('ciudadId')
            });
            
            if (selectedRoles.includes('DOCENTE')) {
                const horarios = obtenerHorariosDeTabla();
                if (horarios.length > 0) {
                    formData.set('horariosDisponibilidad', JSON.stringify(horarios));
                    console.log('📅 Enviando horarios como JSON:', horarios);
                }
                // ✅ LOG: Verificar datos de docente
                console.log('👨‍🏫 Datos de docente enviados:', {
                    matricula: formData.get('matricula'),
                    experiencia: formData.get('experiencia'),
                    horariosDisponibilidad: formData.get('horariosDisponibilidad')
                });
            } else {
                formData.delete('horariosDisponibilidad');
                formData.delete('matricula');
                formData.delete('experiencia');
            }
            
            if (formData.get('experiencia') === '') {
                formData.delete('experiencia');
            }

            const headers = {};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const url = isEditMode ? `/admin/usuarios/${encodeURIComponent(identifier)}` : '/admin/usuarios/registrar';
            const method = isEditMode ? 'PUT' : 'POST';

            fetch(url, {
                method,
                headers,
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
                    const nextPage = isEditMode ? currentPage : 1;
                    loadUsuarios(nextPage || 1);
                } else {
                    // ✅ CAPTURAR MENSAJES DE ERROR ESPECÍFICOS DEL BACKEND
                    const errorMessage = data.message || (isEditMode ? 'Error desconocido al actualizar usuario' : 'Error desconocido al registrar usuario');
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
                const accion = isEditMode ? 'actualizar' : 'registrar';
                let errorMessage = `Error de conexión al ${accion} el usuario`;
                
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
                // hideLoading();
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
        function loadUsuarios(page = 1, options = {}) {
            console.log(`🔄 Cargando usuarios página ${page}...`);

            const serverPage = Math.max(0, page - 1);
            const { forceAll = false, filtersOverride = null } = options;
            const queryParams = new URLSearchParams();

            const activeFilters = filtersOverride || getCurrentFilters();
            const filtersActive = forceAll || hasActiveFilters(activeFilters);

        if (filtersActive) {
            queryParams.set('page', 0);
            queryParams.set('size', 10000);
        } else {
            pageSize = defaultPageSize;
            queryParams.set('page', serverPage);
            queryParams.set('size', pageSize);
        }
            Object.entries(activeFilters).forEach(([key, value]) => {
                if (value !== undefined && value !== null && `${value}`.trim() !== '') {
                    queryParams.append(key, value);
                }
            });

            // Mostrar loading en la tabla
            const tableBody = document.querySelector('#usuarios-table tbody');
            if (tableBody) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center">
                            <div class="loading-inline">
                                <i class="fas fa-spinner fa-spin"></i> Cargando usuarios...
                            </div>
                        </td>
                    </tr>
                `;
            }

            fetch(`/admin/usuarios/listar?${queryParams.toString()}`, {
                method: 'GET'
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
                    const renderStats = populateUsuariosTable(data.data);

                    if (!filtersActive) {
                        if (data.pagination) {
                            updatePagination(data.pagination);
                        } else {
                            const defaultPagination = {
                                totalElements: renderStats.totalElements,
                                totalPages: Math.max(1, Math.ceil(renderStats.totalElements / pageSize)),
                                currentPage: serverPage,
                                pageSize: pageSize
                            };
                            updatePagination(defaultPagination);
                            console.log("⚠️ Usando paginación por defecto:", defaultPagination);
                        }
                    }

                    const visibleCount = filterTable(activeFilters);
                    if (filtersActive) {
                        const singlePage = {
                            totalElements: visibleCount || 0,
                            totalPages: 1,
                            currentPage: 0,
                            pageSize: visibleCount || 1
                        };
                        updatePagination(singlePage);
                        setPaginationControlsVisible(false);
                    } else {
                        setPaginationControlsVisible(true);
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
                            <td colspan="7" class="text-center error-message">
                                <i class="fas fa-exclamation-triangle"></i> Error al cargar usuarios
                            </td>
                        </tr>
                    `;
                }
            });
        }

        function updatePagination(pagination) {
            const totalElements = pagination?.totalElements ?? 0;
            pageSize = pagination?.pageSize ?? pageSize;
            totalPages = Math.max(1, pagination?.totalPages ?? Math.ceil(totalElements / pageSize));
            const serverCurrent = pagination?.currentPage ?? 0;
            currentPage = Math.min(totalPages, serverCurrent + 1);

            console.log('📊 Actualizando paginación:', pagination);

            const paginationInfo = document.querySelector('.pagination-info');
            if (paginationInfo) {
                const startItem = totalElements === 0 ? 0 : ((currentPage - 1) * pageSize) + 1;
                const endItem = totalElements === 0 ? 0 : Math.min(currentPage * pageSize, totalElements);
                paginationInfo.textContent = `Mostrando ${startItem}-${endItem} de ${totalElements} usuarios`;
            }

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
            prevBtn.onclick = () => loadUsuarios(Math.max(1, currentPage - 1));
            nextBtn.onclick = () => loadUsuarios(Math.min(totalPages, currentPage + 1));
            
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

        function setPaginationControlsVisible(visible) {
            const controls = document.querySelector('.pagination-controls');
            if (controls) {
                controls.style.display = visible ? 'flex' : 'none';
            }
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
            const tableBody = document.querySelector('#usuarios-table tbody');
            if (!tableBody) {
                console.error('No se encontró el tbody de la tabla de usuarios');
                return { totalElements: 0, displayed: 0 };
            }

            usuariosCache.clear();

            let usuarios = [];
            let totalElements = 0;

            if (responseData && Array.isArray(responseData.content)) {
                usuarios = responseData.content;
                totalElements = responseData.totalElements ?? usuarios.length;
            } else if (Array.isArray(responseData)) {
                usuarios = responseData;
                totalElements = usuarios.length;
            } else if (responseData && typeof responseData === 'object') {
                const maybeContent = Array.isArray(responseData.content) ? responseData.content : [];
                usuarios = maybeContent;
                totalElements = responseData.totalElements ?? usuarios.length;
            } else {
                console.error('Estructura de datos no reconocida:', responseData);
            }

            console.log('👥 Usuarios a mostrar:', usuarios.length);

            tableBody.innerHTML = '';

            if (usuarios.length === 0) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center">No hay usuarios registrados</td>
                    </tr>
                `;
                return { totalElements, displayed: 0 };
            }

            usuarios.forEach(usuario => {
                const userKey = resolveUsuarioKey(usuario);
                const keyString = userKey != null ? String(userKey) : '';
                const dniMostrar = usuario.dni || keyString || 'N/A';
                const nombreMostrar = usuario.nombreCompleto || 'Sin nombre';
                const correoMostrar = usuario.correo || 'N/A';
                const estadoLiteral = (usuario.estado || 'ACTIVO').toString();

                if (keyString) {
                    usuariosCache.set(keyString, usuario);
                }

                const row = document.createElement('tr');
                if (keyString) {
                    row.dataset.userKey = keyString;
                }
                if (usuario.dni) {
                    row.dataset.dni = usuario.dni;
                }
                if (usuario.nombreCompleto) {
                    row.dataset.nombre = usuario.nombreCompleto;
                }

                const dniCell = document.createElement('td');
                dniCell.textContent = dniMostrar;
                row.appendChild(dniCell);

                const nombreCell = document.createElement('td');
                nombreCell.textContent = nombreMostrar;
                row.appendChild(nombreCell);

                const correoCell = document.createElement('td');
                correoCell.textContent = correoMostrar;
                row.appendChild(correoCell);

                const rolesCell = document.createElement('td');
                rolesCell.innerHTML = formatRoles(usuario.roles || []);
                row.appendChild(rolesCell);

                const estadoCell = document.createElement('td');
                estadoCell.innerHTML = `<span class="status-badge status-${estadoLiteral.toLowerCase()}">${estadoLiteral}</span>`;
                row.appendChild(estadoCell);

                const fechaCell = document.createElement('td');
                fechaCell.textContent = formatFechaRegistro(usuario.fechaRegistro);
                row.appendChild(fechaCell);

                const actionsCell = document.createElement('td');
                actionsCell.className = 'actions';

                const viewBtn = createActionButton('view', 'fas fa-eye', 'Ver', keyString, usuario);
                const editBtn = createActionButton('edit', 'fas fa-edit', 'Editar', keyString, usuario);
                const deleteBtn = createActionButton('delete', 'fas fa-trash', 'Eliminar', keyString, usuario);

                actionsCell.appendChild(viewBtn);
                actionsCell.appendChild(editBtn);
                actionsCell.appendChild(deleteBtn);
                row.appendChild(actionsCell);

                tableBody.appendChild(row);
            });

            return { totalElements, displayed: usuarios.length };
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

        function resolveUsuarioKey(usuario = {}) {
            return usuario.id ?? usuario.usuarioId ?? usuario.userId ?? usuario.dni ?? usuario.correo ?? null;
        }

        function createActionButton(kind, iconClass, title, userKey, usuario = {}) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = `btn-icon btn-${kind}`;
            button.dataset.action = kind;
            if (userKey) {
                button.dataset.userKey = userKey;
            }
            if (usuario.dni) {
                button.dataset.dni = usuario.dni;
            }
            if (usuario.nombreCompleto) {
                button.dataset.nombre = usuario.nombreCompleto;
            }
            button.title = title;
            button.innerHTML = `<i class="${iconClass}"></i>`;
            return button;
        }

        function formatFechaRegistro(fechaValor) {
            if (!fechaValor) {
                return 'N/A';
            }

            if (typeof fechaValor === 'string' && fechaValor.toLowerCase().includes('no disponible')) {
                return fechaValor;
            }

            const parsed = new Date(fechaValor);
            if (Number.isNaN(parsed.getTime())) {
                return typeof fechaValor === 'string' ? fechaValor : 'N/A';
            }

            return parsed.toLocaleDateString();
        }

        function formatearFechaIso(fechaValor) {
            if (!fechaValor) {
                return '';
            }

            if (typeof fechaValor === 'string') {
                return fechaValor.length >= 10 ? fechaValor.substring(0, 10) : fechaValor;
            }

            const parsed = new Date(fechaValor);
            if (Number.isNaN(parsed.getTime())) {
                return '';
            }

            return parsed.toISOString().split('T')[0];
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
    
        console.log('Gestión de Usuarios inicializada correctamente');
    });
