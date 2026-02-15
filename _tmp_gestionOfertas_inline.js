        console.log('?? SCRIPT INLINE CARGADO');
        
        document.addEventListener('DOMContentLoaded', function() {
            function getValueById(id) {
                const el = document.getElementById(id);
                return el ? el.value : null;
            }
            // Imagen: preview inmediato al seleccionar archivo
            const imagenInput = document.getElementById('imagen');
            const imagePreview = document.getElementById('image-preview');
            const uploadPlaceholder = document.querySelector('.upload-placeholder');
            if (imagenInput && imagePreview && uploadPlaceholder) {
                imagenInput.addEventListener('change', function(event) {
                    const file = event.target.files && event.target.files[0];
                    if (!file) return;
                    if (file.size > 5 * 1024 * 1024) {
                        if (typeof mostrarNotificacion === 'function') {
                            mostrarNotificacion('La imagen supera 5MB', 'warning');
                        } else {
                            alert('La imagen supera 5MB');
                        }
                        imagenInput.value = '';
                        return;
                    }
                    const reader = new FileReader();
                    reader.onload = function(e) {
                        imagePreview.src = e.target.result;
                        imagePreview.style.display = 'block';
                        uploadPlaceholder.style.display = 'none';
                    };
                    reader.readAsDataURL(file);
                });
            }
            const btnClearImage = document.getElementById('btn-clear-image');
            if (btnClearImage && imagenInput && imagePreview && uploadPlaceholder) {
                btnClearImage.addEventListener('click', function() {
                    imagenInput.value = '';
                    imagePreview.src = '';
                    imagePreview.style.display = 'none';
                    uploadPlaceholder.style.display = 'flex';
                });
            }
            console.log('? DOM CARGADO');
            
            // Test: Verificar que los botones de ver detalle estén presentes
            setTimeout(function() {
                const botonesVer = document.querySelectorAll('.btn-ver');
                console.log('?? Botones de ver encontrados:', botonesVer.length);
                botonesVer.forEach((btn, index) => {
                    console.log(`- Botón ${index + 1}:`, btn.getAttribute('data-id'));
                });
            }, 1000);
            
            // Referencias a elementos
            const btnShowForm = document.getElementById('btn-show-form');
            const formContainer = document.getElementById('form-container');
            const btnCloseForm = document.getElementById('btn-close-form');
            const btnCancelForm = document.getElementById('btn-cancel-form');
            const btnGestionarCategorias = document.getElementById('btn-gestionar-categorias');
            const categoriasFilterInput = document.getElementById('categorias-filter-input');
            const categoriasFilterClear = document.getElementById('categorias-filter-clear');
            const categoriaSearchInput = document.getElementById('categoria-search');
            
            console.log('Elementos encontrados:', {
                btnShowForm: !!btnShowForm,
                formContainer: !!formContainer,
                btnCloseForm: !!btnCloseForm,
                btnCancelForm: !!btnCancelForm,
                btnGestionarCategorias: !!btnGestionarCategorias
            });

            if (categoriasFilterInput) {
                categoriasFilterInput.addEventListener('input', aplicarFiltroCategorias);
            }
            
            if (categoriaSearchInput) {
                categoriaSearchInput.addEventListener('input', filtrarCategoriasSelect);
            }

            if (categoriasFilterClear) {
                categoriasFilterClear.addEventListener('click', function() {
                    if (categoriasFilterInput) {
                        categoriasFilterInput.value = '';
                        aplicarFiltroCategorias();
                        categoriasFilterInput.focus();
                    }
                });
            }
            
            // Manejador del botón mostrar formulario
            if (btnShowForm && formContainer) {
                btnShowForm.addEventListener('click', function(e) {
                    e.preventDefault();
                    console.log('?? CLICK EN NUEVA OFERTA');
                    
                    // Limpiar formulario para asegurar estado inicial
                    if (typeof limpiarFormularioCompleto === 'function') {
                        limpiarFormularioCompleto();
                    }
                    
                     // Establecer fecha mínima en los inputs de fecha (hoy)
                    const hoy = new Date().toISOString().split('T')[0];
                    const fechaInicioInput = document.getElementById('fechaInicio');
                    const fechaFinInput = document.getElementById('fechaFin');
                    
                    if (fechaInicioInput) fechaInicioInput.setAttribute('min', hoy);
                    if (fechaFinInput) fechaFinInput.setAttribute('min', hoy);
                    
                    // Mostrar formulario con animación
                    formContainer.style.display = 'block';
                    
                    setTimeout(() => {
                        formContainer.classList.add('show');
                        console.log('?? FORMULARIO MOSTRADO');
                    }, 10);
                });
            }
            
            function confirmarCancelacionFormulario() {
                const estaEnEdicion = typeof modoEdicion !== 'undefined' && modoEdicion;
                const titulo = estaEnEdicion ? 'Cancelar Modificación' : 'Cancelar Registro';
                const mensaje = estaEnEdicion
                    ? '¿Estás seguro que deseas cancelar la modificación? Los cambios no guardados se perderán.'
                    : '¿Estás seguro que deseas cancelar el registro? Los datos ingresados se perderán.';

                ModalConfirmacion.show(titulo, mensaje, () => {
                    if (estaEnEdicion && typeof resetearModoEdicion === 'function') {
                        resetearModoEdicion();
                    }
                    if (typeof limpiarFormularioCompleto === 'function') {
                        limpiarFormularioCompleto();
                    }
                    console.log('? CERRANDO FORMULARIO');
                    formContainer.classList.remove('show');
                    setTimeout(() => {
                        formContainer.style.display = 'none';
                    }, 500);
                });
            }

            // Manejador del botón cerrar formulario
            if (btnCloseForm && formContainer) {
                btnCloseForm.addEventListener('click', function(e) {
                    e.preventDefault();
                    confirmarCancelacionFormulario();
                });
            }

            // Manejador del botón cancelar formulario (botón inferior)
            if (btnCancelForm && formContainer) {
                btnCancelForm.addEventListener('click', function(e) {
                    e.preventDefault();
                    confirmarCancelacionFormulario();
                });
            }
            
            // Manejador del botón gestionar categorías
            if (btnGestionarCategorias) {
                btnGestionarCategorias.addEventListener('click', function(e) {
                    e.preventDefault();
                    console.log('?? ABRIENDO GESTI�N DE CATEGORÍAS');
                    
                    const modal = document.getElementById('categoriaModal');
                    if (modal) {
                        modal.style.display = 'flex';
                        if (categoriasFilterInput) {
                            categoriasFilterInput.value = '';
                        }
                        cargarCategorias();
                    }
                });
            }
            
            // Manejador del botón añadir categoría
            const btnAddCategoria = document.getElementById('btn-add-categoria');
            if (btnAddCategoria) {
                btnAddCategoria.addEventListener('click', function(e) {
                    e.preventDefault();
                    añadirCategoriaSeleccionada();
                });
            }
            
            // Manejador para mostrar campos específicos según tipo de oferta
            const tipoOfertaSelect = document.getElementById('tipoOferta');
            if (tipoOfertaSelect) {
                tipoOfertaSelect.addEventListener('change', function() {
                    mostrarCamposEspecificos(this.value);
                });
            }
            
            // Manejador para mostrar/ocultar campos según modalidad
            const modalidadSelect = document.getElementById('modalidad');
            if (modalidadSelect) {
                modalidadSelect.addEventListener('change', function() {
                    actualizarCamposModalidad(this.value);
                });
            }
            
            // Listeners para cálculo automático de cuotas
            const fechaInicio = document.getElementById('fechaInicio');
            const fechaFin = document.getElementById('fechaFin');
            const diaVencimiento = document.getElementById('diaVencimiento');
            
            if (fechaInicio) {
                fechaInicio.addEventListener('change', function() {
                    // Aplicar lógica si es CURSO, FORMACION o SEMINARIO
                    const tipoOferta = getValueById('tipoOferta');
                    if (tipoOferta === 'CURSO' || tipoOferta === 'FORMACION' || tipoOferta === 'SEMINARIO') {
                        // Habilitar fechaFin cuando se selecciona fechaInicio
                        if (fechaFin && this.value) {
                            fechaFin.disabled = false;
                            fechaFin.setAttribute('min', this.value);
                        }
                        if (tipoOferta === 'CURSO' || tipoOferta === 'FORMACION') {
                            calcularNumeroCuotas();
                        }
                    }
                });
            }
            if (fechaFin) {
                fechaFin.addEventListener('change', calcularNumeroCuotas);
            }
            if (diaVencimiento) {
                diaVencimiento.addEventListener('change', calcularNumeroCuotas);
            }
            
            // Event listeners para disertantes (Enter key)
            const disertantesCharlaInput = document.getElementById('disertantesCharlaInput');
            if (disertantesCharlaInput) {
                disertantesCharlaInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        agregarDisertante('charla');
                    }
                });
            }
            
            const disertantesSeminarioInput = document.getElementById('disertantesSeminarioInput');
            if (disertantesSeminarioInput) {
                disertantesSeminarioInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        agregarDisertante('seminario');
                    }
                });
            }
            
            // Cargar categorías al iniciar
            cargarCategoriasSelect();
            
            // Event listener para cerrar notificación
            const notificationClose = document.getElementById('notification-close');
            if (notificationClose) {
                notificationClose.addEventListener('click', ocultarNotificacion);
            }
            
            // Manejador de filtros
            const btnApplyFilters = document.getElementById('btn-apply-filters');
            const btnClearFilters = document.getElementById('btn-clear-filters');
            
            if (btnApplyFilters) {
                btnApplyFilters.addEventListener('click', function() {
                    aplicarFiltros();
                });
            }
            
            if (btnClearFilters) {
                btnClearFilters.addEventListener('click', function() {
                    limpiarFiltros();
                });
            }

            console.log('?? GESTI�N DE OFERTAS INICIALIZADA');
            
            // Cargar ofertas iniciales para paginación
            actualizarTablaOfertas();
            setupSortableOfferHeaders();
        });
        
        // Función para mostrar campos específicos según tipo de oferta
        function mostrarCamposEspecificos(tipoOferta) {
            console.log('?? Cambiando tipo de oferta a:', tipoOferta);
            
            // Limpiar datos anteriores
            limpiarDatosFormulario();
            
            // Ocultar todas las secciones específicas
            const seccionesEspecificas = document.querySelectorAll('.tipo-specific');
            seccionesEspecificas.forEach(seccion => {
                seccion.style.display = 'none';
            });
            
            // Ocultar sección de horarios por defecto
            const seccionHorarios = document.getElementById('seccion-horarios');
            seccionHorarios.style.display = 'none';
            
            // Controlar campos de cuotas en la sección principal de costos
            const grupoCostoCuota = document.getElementById('grupo-costo-cuota');
            const grupoNroCuotas = document.getElementById('grupo-nro-cuotas');
            const grupoDiaVencimiento = document.getElementById('grupo-dia-vencimiento');
            
            // Mostrar campos de cuotas solo para CURSO y FORMACION
            const mostrarCamposCuotas = tipoOferta === 'CURSO' || tipoOferta === 'FORMACION';
            
            if (grupoCostoCuota) grupoCostoCuota.style.display = mostrarCamposCuotas ? 'block' : 'none';
            if (grupoNroCuotas) grupoNroCuotas.style.display = mostrarCamposCuotas ? 'block' : 'none';
            if (grupoDiaVencimiento) grupoDiaVencimiento.style.display = mostrarCamposCuotas ? 'block' : 'none';
            
            // Ocultar fechaInicio y fechaFin solo para CHARLA (usa fecha específica)
            const grupoFechaInicio = document.getElementById('grupo-fecha-inicio');
            const grupoFechaFin = document.getElementById('grupo-fecha-fin');
            const mostrarFechasGenerales = tipoOferta === 'CURSO' || tipoOferta === 'FORMACION' || tipoOferta === 'SEMINARIO';
            
            if (grupoFechaInicio) {
                grupoFechaInicio.style.display = mostrarFechasGenerales ? 'block' : 'none';
                const inputFechaInicio = document.getElementById('fechaInicio');
                if (inputFechaInicio) {
                    inputFechaInicio.required = mostrarFechasGenerales;
                }
            }
            
            if (grupoFechaFin) {
                grupoFechaFin.style.display = mostrarFechasGenerales ? 'block' : 'none';
                const inputFechaFin = document.getElementById('fechaFin');
                if (inputFechaFin) {
                    inputFechaFin.required = mostrarFechasGenerales;
                }
            }
            
            // Mostrar la sección correspondiente al tipo seleccionado
            let seccionMostrar = null;
            switch(tipoOferta) {
                case 'CURSO':
                    seccionMostrar = document.getElementById('fields-curso');
                    // Mostrar horarios para curso
                    seccionHorarios.style.display = 'block';
                    // Inicializar búsqueda de docentes para curso
                    console.log('?? Inicializando búsqueda para CURSO');
                    inicializarBusquedaDocentes('docente-search', 'docente-results', 'docentes-table');
                    break;
                    
                case 'FORMACION':
                    seccionMostrar = document.getElementById('fields-formacion');
                    // Mostrar horarios para formación
                    seccionHorarios.style.display = 'block';
                    // Inicializar búsqueda de docentes para formación
                    console.log('?? Inicializando búsqueda para FORMACION');
                    inicializarBusquedaDocentes('docente-search-formacion', 'docente-results-formacion', 'docentes-table-formacion');
                    break;
                    
                case 'CHARLA':
                    seccionMostrar = document.getElementById('fields-charla');
                    break;
                    
                case 'SEMINARIO':
                    seccionMostrar = document.getElementById('fields-seminario');
                    break;
                    
                default:
                    console.log('?? Tipo de oferta no reconocido:', tipoOferta);
                    return;
            }
            
            if (seccionMostrar) {
                seccionMostrar.style.display = 'block';
                console.log('? Mostrando campos específicos para:', tipoOferta);
            }
            
            // Actualizar campos según el tipo y modalidad
            actualizarCamposModalidad();
        }
        
        // Función para limpiar datos del formulario cuando se cambia tipo
        function limpiarDatosFormulario() {
            // Limpiar docentes seleccionados
            docentesSeleccionados.curso = [];
            docentesSeleccionados.formacion = [];
            
            // Limpiar horarios
            horariosSeleccionados = [];
            
            // Limpiar disertantes
            disertantesCharla = [];
            disertantesSeminario = [];
            
            // Limpiar tablas de docentes
            const tablaCurso = document.getElementById('docentes-table');
            const tablaFormacion = document.getElementById('docentes-table-formacion');
            
            if (tablaCurso) {
                const tbody = tablaCurso.querySelector('tbody');
                if (tbody) tbody.innerHTML = '';
            }
            
            if (tablaFormacion) {
                const tbody = tablaFormacion.querySelector('tbody');
                if (tbody) tbody.innerHTML = '';
            }
            
            // Limpiar visualizaciones
            actualizarHorariosChips();
            actualizarDisertantesChips('charla');
            actualizarDisertantesChips('seminario');
            
            // Actualizar campos hidden
            actualizarDocentesHidden('curso');
            actualizarDocentesHidden('formacion');
            
            console.log('?? Datos del formulario limpiados');
        }
        
        // Función para actualizar campos según modalidad seleccionada
        function actualizarCamposModalidad() {
            const modalidadSelect = document.getElementById('modalidad');
            const tipoOfertaSelect = document.getElementById('tipoOferta');
            
            if (!modalidadSelect || !tipoOfertaSelect) return;
            
            const modalidad = modalidadSelect.value;
            const tipoOferta = tipoOfertaSelect.value;
            
            const seccionUbicacion = document.getElementById('seccion-ubicacion');
            const grupoLugar = document.getElementById('grupo-lugar');
            const grupoEnlace = document.getElementById('grupo-enlace');
            
            if (!seccionUbicacion || !grupoLugar || !grupoEnlace) return;
            
            // Si no hay tipo de oferta o modalidad seleccionada, ocultar todo
            if (!tipoOferta || !modalidad) {
                seccionUbicacion.style.display = 'none';
                grupoLugar.style.display = 'none';
                grupoEnlace.style.display = 'none';
                return;
            }
            
            // Para CURSO y FORMACI�N
            if (tipoOferta === 'CURSO' || tipoOferta === 'FORMACION') {
                grupoEnlace.style.display = 'none';
                
                // Mostrar ubicación solo para PRESENCIAL o HÍBRIDA
                if (modalidad === 'PRESENCIAL' || modalidad === 'HIBRIDA') {
                    seccionUbicacion.style.display = 'block';
                    grupoLugar.style.display = 'block';
                } else {
                    seccionUbicacion.style.display = 'none';
                    grupoLugar.style.display = 'none';
                }
                return;
            }
            
            // Para CHARLA
            if (tipoOferta === 'CHARLA') {
                if (modalidad === 'PRESENCIAL') {
                    // Solo ubicación, sin enlace
                    seccionUbicacion.style.display = 'block';
                    grupoLugar.style.display = 'block';
                    grupoEnlace.style.display = 'none';
                } else if (modalidad === 'VIRTUAL') {
                    // Solo enlace, sin ubicación
                    seccionUbicacion.style.display = 'block';
                    grupoLugar.style.display = 'none';
                    grupoEnlace.style.display = 'block';
                } else if (modalidad === 'HIBRIDA') {
                    // Ubicación y enlace
                    seccionUbicacion.style.display = 'block';
                    grupoLugar.style.display = 'block';
                    grupoEnlace.style.display = 'block';
                } else {
                    seccionUbicacion.style.display = 'none';
                    grupoLugar.style.display = 'none';
                    grupoEnlace.style.display = 'none';
                }
                return;
            }

            // Para SEMINARIO
            if (tipoOferta === 'SEMINARIO') {
                if (modalidad === 'PRESENCIAL') {
                    // Solo ubicación, sin enlace
                    seccionUbicacion.style.display = 'block';
                    grupoLugar.style.display = 'block';
                    grupoEnlace.style.display = 'none';
                } else if (modalidad === 'VIRTUAL') {
                    // Solo enlace, sin ubicación
                    seccionUbicacion.style.display = 'block';
                    grupoLugar.style.display = 'none';
                    grupoEnlace.style.display = 'block';
                } else if (modalidad === 'HIBRIDA') {
                    // Ubicación y enlace
                    seccionUbicacion.style.display = 'block';
                    grupoLugar.style.display = 'block';
                    grupoEnlace.style.display = 'block';
                } else {
                    seccionUbicacion.style.display = 'none';
                    grupoLugar.style.display = 'none';
                    grupoEnlace.style.display = 'none';
                }
                return;
            }
            
            // Caso por defecto: ocultar todo
            seccionUbicacion.style.display = 'none';
            grupoLugar.style.display = 'none';
            grupoEnlace.style.display = 'none';
        }
        
        // Función para calcular número de cuotas automáticamente
        function calcularNumeroCuotas() {
            const fechaInicio = document.getElementById('fechaInicio');
            const fechaFin = document.getElementById('fechaFin');
            const diaVencimiento = document.getElementById('diaVencimiento');
            const nrCuotas = document.getElementById('nrCuotas');
            
            if (!fechaInicio || !fechaFin || !diaVencimiento || !nrCuotas) return;
            
            const inicio = fechaInicio.value;
            const fin = fechaFin.value;
            const dia = parseInt(diaVencimiento.value);
            
            if (!inicio || !fin || !dia) return;
            
            const fechaInicioDate = new Date(inicio);
            const fechaFinDate = new Date(fin);
            
            // Calcular meses completos
            let meses = (fechaFinDate.getFullYear() - fechaInicioDate.getFullYear()) * 12;
            meses += fechaFinDate.getMonth() - fechaInicioDate.getMonth();
            
            // Ajustar según el día de vencimiento y el día de inicio
            const diaInicio = fechaInicioDate.getDate();
            const diaFinalizacion = fechaFinDate.getDate();
            
            // Si el curso inicia después del día de vencimiento, se pierde una cuota de ese mes
            let cuotas = meses;
            
            if (diaInicio > dia) {
                // Si el curso empieza después del día de vencimiento, no se paga ese mes
                // pero sí todos los siguientes hasta el mes de finalización
                cuotas = meses;
            } else {
                // Si el curso empieza antes o en el día de vencimiento, se paga ese mes
                cuotas = meses + 1;
            }
            
            // Si el curso termina antes del día de vencimiento del último mes, no se cuenta esa cuota
            if (diaFinalizacion < dia && cuotas > 1) {
                cuotas--;
            }
            
            // Mínimo 1 cuota
            cuotas = Math.max(1, cuotas);
            
            nrCuotas.value = cuotas;
        }
        
        // Función para generar videoconferencia
        window.generarVideoconferencia = function() {
            const tipoOferta = document.getElementById('tipoOferta').value;
            const nombreOferta = document.getElementById('nombre').value;
            
            if (!nombreOferta) {
                mostrarNotificacion('Por favor, ingresa el nombre de la oferta primero', 'warning');
                return;
            }
            
            // Validar fecha y hora según el tipo
            let fecha, hora;
            if (tipoOferta === 'CHARLA') {
                fecha = document.getElementById('fechaCharla').value;
                hora = document.getElementById('horaCharla').value;
                
                if (!fecha || !hora) {
                    mostrarNotificacion('Por favor, ingresa la fecha y hora de la charla', 'warning');
                    return;
                }
            } else if (tipoOferta === 'SEMINARIO') {
                fecha = document.getElementById('fechaInicio').value;
                hora = document.getElementById('horaSeminario').value;
                
                if (!fecha || !hora) {
                    mostrarNotificacion('Por favor, ingresa la fecha de inicio y la hora del seminario', 'warning');
                    return;
                }
            }
            
            // Obtener el primer disertante según el tipo
            let disertante = null;
            if (tipoOferta === 'CHARLA') {
                if (disertantesCharla.length > 0) {
                    disertante = disertantesCharla[0];
                }
            } else if (tipoOferta === 'SEMINARIO') {
                if (disertantesSeminario.length > 0) {
                    disertante = disertantesSeminario[0];
                }
            }
            
            if (!disertante) {
                mostrarNotificacion('Por favor, ingresa al menos un disertante primero', 'warning');
                return;
            }
            
            // Generar el nombre de la sala (similar a como se hace en clases)
            const roomName = nombreOferta.toLowerCase()
                .replace(/[áàäâ]/g, 'a')
                .replace(/[éèëê]/g, 'e')
                .replace(/[íìïî]/g, 'i')
                .replace(/[óòöô]/g, 'o')
                .replace(/[úùüû]/g, 'u')
                .replace(/ñ/g, 'n')
                .replace(/\s+/g, '-')
                .replace(/[^a-z0-9-]/g, '');
            
            const safeRoomName = 'aurea-charla-' + roomName;
            
            // URL hacia el HTML de Aurea que mostrará el iframe (será creado en el backend)
            // Pasar la fecha y hora como parámetros
            const videoUrl = `/charla/videoconferencia/${safeRoomName}?fecha=${fecha}&hora=${hora}&moderador=${encodeURIComponent(disertante)}`;
            
            // Establecer el enlace en el campo
            document.getElementById('enlace').value = window.location.origin + videoUrl;
            
            mostrarNotificacion(`Videoconferencia programada para ${fecha} ${hora}. Moderador: ${disertante}`, 'success');
            
            console.log('?? Videoconferencia generada:');
            console.log('   - Room: ' + safeRoomName);
            console.log('   - Moderador: ' + disertante);
            console.log('   - Fecha: ' + fecha);
            console.log('   - Hora: ' + hora);
            console.log('   - URL: ' + videoUrl);
        };

        function actualizarEncuentrosSeminario() {
            const tipoOferta = getValueById('tipoOferta');
            const preview = document.getElementById('encuentros-preview');
            const fechasList = document.getElementById('encuentros-fechas-list');
            const help = document.getElementById('help-encuentros');
            const numeroInput = document.getElementById('numeroEncuentros');
            const fechaInicio = getValueById('fechaInicio');
            const fechaFin = getValueById('fechaFin');

            if (!preview || !numeroInput || !fechasList) return;

            if (tipoOferta !== 'SEMINARIO') {
                preview.textContent = 'Completa fecha inicio, fecha fin y número de encuentros para ver el cronograma.';
                if (help) help.textContent = 'Debe ser menor o igual a los días entre fecha inicio y fecha fin.';
                fechasList.innerHTML = '';
                return;
            }

            if (!fechaInicio || !fechaFin) {
                preview.textContent = 'Completa fecha inicio y fecha fin para calcular los encuentros.';
                if (help) help.textContent = 'Debe ser menor o igual a los días entre fecha inicio y fecha fin.';
                fechasList.innerHTML = '';
                return;
            }

            const inicioObj = new Date(fechaInicio + 'T00:00:00');
            const finObj = new Date(fechaFin + 'T00:00:00');
            const diffDays = Math.floor((finObj - inicioObj) / (1000 * 60 * 60 * 24)) + 1;

            if (diffDays <= 0) {
                preview.textContent = 'Rango de fechas inválido.';
                fechasList.innerHTML = '';
                return;
            }

            numeroInput.max = diffDays;
            if (help) help.textContent = `Máximo ${diffDays} encuentros según el rango seleccionado.`;

            const numero = parseInt(numeroInput.value || '0', 10);
            if (!numero || numero <= 0) {
                preview.textContent = 'Ingresa el número de encuentros para ver las fechas.';
                fechasList.innerHTML = '';
                return;
            }
            if (numero > diffDays) {
                preview.textContent = 'El número de encuentros supera el rango disponible.';
                fechasList.innerHTML = '';
                return;
            }

            const existentes = Array.from(fechasList.querySelectorAll('input[name="fechasEncuentros"]'))
                .map((input) => input.value)
                .filter(Boolean);
            const fechas = [];
            for (let i = 0; i < numero; i++) {
                const d = new Date(inicioObj);
                d.setDate(inicioObj.getDate() + i);
                const yyyy = d.getFullYear();
                const mm = String(d.getMonth() + 1).padStart(2, '0');
                const dd = String(d.getDate()).padStart(2, '0');
                fechas.push(`${yyyy}-${mm}-${dd}`);
            }

            preview.textContent = `Selecciona ${numero} fechas (entre ${fechaInicio} y ${fechaFin}).`;
            fechasList.innerHTML = fechas.map((f, idx) => {
                const valor = existentes[idx] || f;
                return `
                    <div class="form-group">
                        <label for="encuentro-fecha-${idx + 1}">Encuentro ${idx + 1}</label>
                        <input type="date"
                               id="encuentro-fecha-${idx + 1}"
                               name="fechasEncuentros"
                               class="form-control"
                               min="${fechaInicio}"
                               max="${fechaFin}"
                               value="${valor}"
                               required>
                    </div>
                `;
            }).join('');
        }

        document.addEventListener('DOMContentLoaded', () => {
            ['fechaInicio', 'fechaFin', 'numeroEncuentros', 'tipoOferta'].forEach((id) => {
                const el = document.getElementById(id);
                if (!el) return;
                el.addEventListener('change', actualizarEncuentrosSeminario);
                el.addEventListener('input', actualizarEncuentrosSeminario);
            });
            actualizarEncuentrosSeminario();
        });


            // Variables globales para docentes seleccionados
            let docentesSeleccionados = {
                curso: [],
                formacion: []
            };

            // Variable para rastrear event listeners ya agregados
            let docenteSearchListenersAdded = {
                'docente-search': false,
                'docente-search-formacion': false
            };

            // Función para inicializar búsqueda de docentes
            function inicializarBusquedaDocentes(inputId, resultsId, tableId) {
                console.log('?? Inicializando búsqueda de docentes para:', inputId);

                const searchInput = document.getElementById(inputId);
                const resultsDiv = document.getElementById(resultsId);

                if (!searchInput || !resultsDiv) {
                    console.warn('?? No se encontraron elementos:', inputId, resultsId);
                    return;
                }

                console.log('? Elementos encontrados:', searchInput, resultsDiv);

                // Evitar agregar múltiples event listeners
                if (docenteSearchListenersAdded[inputId]) {
                    console.log('?? Event listener ya agregado para:', inputId);
                    return;
                }

                let timeoutId;

                searchInput.addEventListener('input', function () {
                    console.log('?? Input detectado:', this.value);
                    clearTimeout(timeoutId);
                    const query = this.value.trim();

                    timeoutId = setTimeout(() => {
                        buscarDocentes(query, resultsDiv, tableId);
                    }, 300);
                });

                // Agregar evento focus para mostrar todos los docentes al hacer click
                searchInput.addEventListener('focus', function () {
                    console.log('??? Focus en buscador de docentes');
                    const query = this.value.trim();
                    buscarDocentes(query, resultsDiv, tableId);
                });

                // Ocultar resultados al hacer click fuera
                document.addEventListener('click', function (e) {
                    if (!searchInput.contains(e.target) && !resultsDiv.contains(e.target)) {
                        resultsDiv.style.display = 'none';
                    }
                });

                docenteSearchListenersAdded[inputId] = true;
                console.log('? Event listener agregado correctamente para:', inputId);
            }

            // Función para buscar docentes
            function buscarDocentes(query, resultsDiv, tableId) {
                console.log('?? Buscando docentes:', query);

                // Si no hay query, usar cadena vacía para obtener todos
                const searchQuery = query || '';

                // Buscar docentes reales desde la base de datos
                fetch(`/admin/docentes/buscar?q=${encodeURIComponent(searchQuery)}`)
                    .then(response => {
                        if (!response.ok) {
                            throw new Error('Error en la respuesta del servidor');
                        }
                        return response.json();
                    })
                    .then(docentes => {
                        console.log('? Docentes encontrados:', docentes);
                        mostrarResultadosDocentes(docentes, resultsDiv, tableId);
                    })
                    .catch(error => {
                        console.error('? Error al buscar docentes:', error);
                        // Fallback con docentes simulados para desarrollo
                        const docentesSimulados = [
                            { id: 1, nombre: 'Dr. Juan Pérez', especialidad: 'Programación Java' },
                            { id: 2, nombre: 'Dra. María González', especialidad: 'Base de Datos' },
                            { id: 3, nombre: 'Ing. Carlos López', especialidad: 'Desarrollo Web' },
                            { id: 4, nombre: 'Dra. Ana Martínez', especialidad: 'Inteligencia Artificial' },
                            { id: 5, nombre: 'Ing. Roberto Silva', especialidad: 'Redes y Sistemas' }
                        ];

                        const resultados = docentesSimulados.filter(docente =>
                            docente.nombre.toLowerCase().includes(query.toLowerCase()) ||
                            docente.especialidad.toLowerCase().includes(query.toLowerCase())
                        );

                        mostrarResultadosDocentes(resultados, resultsDiv, tableId);
                    });
            }

            // Función para mostrar resultados de búsqueda
            function mostrarResultadosDocentes(docentes, resultsDiv, tableId) {
                resultsDiv.innerHTML = '';

                if (docentes.length === 0) {
                    resultsDiv.innerHTML = '<div class="no-results">No se encontraron docentes</div>';
                    resultsDiv.style.display = 'block';
                    return;
                }

                const ul = document.createElement('ul');
                ul.className = 'search-results-list';

                docentes.forEach(docente => {
                    const li = document.createElement('li');
                    li.className = 'search-result-item';
                    li.innerHTML = `
                    <div class="docente-info">
                        <span class="docente-nombre">${docente.nombre}</span>
                    </div>
                    <button type="button" class="btn-add-docente" onclick="agregarDocente('${docente.id}', '${docente.nombre}', '${tableId}')">
                        <i class="fas fa-plus"></i>
                    </button>
                `;
                    ul.appendChild(li);
                });

                resultsDiv.appendChild(ul);
                resultsDiv.style.display = 'block';
            }

            // Nota: agregarDocente está definido como window.agregarDocente más abajo (línea ~2303)

            // Función para remover docente
            function removerDocente(id, tableId, button) {
                const tipoSeccion = tableId.includes('formacion') ? 'formacion' : 'curso';

                // Remover de la lista
                docentesSeleccionados[tipoSeccion] = docentesSeleccionados[tipoSeccion].filter(d => d.id !== id);

                // Remover fila de la tabla
                const fila = button.closest('tr');
                fila.remove();

                console.log('? Docente removido');
            }

            // Array para almacenar categorías seleccionadas
            let categoriasSeleccionadas = [];

            // Función para añadir categoría seleccionada
            function añadirCategoriaSeleccionada() {
                const select = document.getElementById('categoria-select');
                const selectedValue = select.value;
                const selectedText = select.options[select.selectedIndex].text;

                if (!selectedValue || selectedValue === '') {
                    mostrarNotificacion('Por favor seleccione una categoría', 'warning');
                    return;
                }

                // Verificar si ya está seleccionada
                const yaSeleccionada = categoriasSeleccionadas.find(cat => cat.id === selectedValue);
                if (yaSeleccionada) {
                    mostrarNotificacion('Esta categoría ya ha sido seleccionada', 'warning');
                    return;
                }

                // Añadir a la lista
                categoriasSeleccionadas.push({
                    id: selectedValue,
                    nombre: selectedText
                });

                // Actualizar la visualización
                actualizarCategoriasChips();

                // Reset del select
                select.value = '';

                console.log('? Categoría añadida:', selectedText);
            }

            // Función para actualizar los chips de categorías
            function actualizarCategoriasChips() {
                const chipsContainer = document.getElementById('selected-chips');
                const selectedContainer = document.getElementById('selected-categories');

                if (!chipsContainer) return;

                // Limpiar chips existentes
                chipsContainer.innerHTML = '';

                if (categoriasSeleccionadas.length === 0) {
                    selectedContainer.style.display = 'none';
                    // Actualizar campo hidden del formulario
                    const formCategorias = document.getElementById('categorias');
                    if (formCategorias) {
                        formCategorias.value = '';
                    }
                    return;
                }

                // Mostrar contenedor
                selectedContainer.style.display = 'block';

                // Crear chips
                categoriasSeleccionadas.forEach((categoria, index) => {
                    const chip = document.createElement('span');
                    chip.className = 'categoria-chip';
                    chip.dataset.categoriaId = categoria.id; // Agregar el data attribute
                    chip.innerHTML = `
                    ${categoria.nombre}
                    <button type="button" class="chip-remove" onclick="removerCategoria(${index})">
                        <i class="fas fa-times"></i>
                    </button>
                `;
                    chipsContainer.appendChild(chip);
                });

                // Actualizar el campo hidden del formulario principal con IDs separados por coma
                const formCategorias = document.getElementById('categorias');
                if (formCategorias) {
                    const ids = categoriasSeleccionadas.map(cat => cat.id).join(',');
                    formCategorias.value = ids;
                    console.log('Categorías actualizadas:', ids);
                }
            }

            // Función para actualizar campo hidden de categorías
            function actualizarCategoriaHidden() {
                const categoriasSeleccionadas = Array.from(document.querySelectorAll('#selected-chips .categoria-chip'))
                    .map(chip => chip.dataset.categoriaId);

                const formCategorias = document.getElementById('categorias');
                if (formCategorias) {
                    formCategorias.value = categoriasSeleccionadas.join(',');
                    console.log('Categorías actualizadas en hidden field:', formCategorias.value);
                }
            }

            // Función para actualizar campo hidden de certificado
            function actualizarCertificadoHidden() {
                const checkbox = document.getElementById('otorgaCertificado');
                const hiddenField = document.getElementById('otorgaCertificadoHidden');

                if (checkbox && hiddenField) {
                    hiddenField.value = checkbox.checked ? 'true' : 'false';
                    console.log('Certificado actualizado:', hiddenField.value);
                }
            }

            // Función para actualizar campos hidden de disertantes
            function actualizarDisertantesHidden(tipo) {
                const hiddenId = tipo === 'charla' ? 'disertantesCharlaHidden' : 'disertantesSeminarioHidden';
                const hiddenField = document.getElementById(hiddenId);

                if (hiddenField) {
                    const lista = tipo === 'charla' ? disertantesCharla : disertantesSeminario;
                    hiddenField.value = JSON.stringify(lista);
                }
            }

            // Función para actualizar campo hidden de horarios
            function actualizarHorariosHidden() {
                const hiddenField = document.getElementById('horarios');
                if (hiddenField) {
                    hiddenField.value = JSON.stringify(horariosSeleccionados);
                }
            }

            // Función para remover categoría
            function removerCategoria(index) {
                categoriasSeleccionadas.splice(index, 1);
                actualizarCategoriasChips();
                console.log('? Categoría removida, total:', categoriasSeleccionadas.length);
            }

            // Funciones para gestión de categorías
            let categoriasCache = [];

            function aplicarFiltroCategorias() {
                const input = document.getElementById('categorias-filter-input');
                const termino = input ? input.value.trim().toLowerCase() : '';

                if (!termino) {
                    mostrarCategorias(categoriasCache);
                    return;
                }

                const coincideNombre = categoriasCache.filter(categoria => {
                    const nombre = (categoria.nombre || '').toLowerCase();
                    return nombre.includes(termino);
                });

                if (coincideNombre.length > 0) {
                    mostrarCategorias(coincideNombre, categoriasCache.length, 'nombre');
                    return;
                }

                const coincideDescripcion = categoriasCache.filter(categoria => {
                    const descripcion = (categoria.descripcion || '').toLowerCase();
                    return descripcion.includes(termino);
                });

                mostrarCategorias(coincideDescripcion, categoriasCache.length, 'descripcion');
            }

        function cargarCategorias() {
            console.log('?? Cargando categorías...');
            
            const loadingDiv = document.getElementById('categoriasLoading');
            const errorDiv = document.getElementById('categoriasError');
            const table = document.getElementById('categoriasTable');
            
            if (loadingDiv) loadingDiv.style.display = 'block';
            if (errorDiv) errorDiv.style.display = 'none';
            if (table) table.style.display = 'none';
            
            fetch('/api/categorias')
                .then(response => response.json())
                .then(categorias => {
                    console.log('? Categorías cargadas:', categorias);
                    
                    if (loadingDiv) loadingDiv.style.display = 'none';
                    
                    categoriasCache = Array.isArray(categorias) ? categorias : [];
                    aplicarFiltroCategorias();
                    if (table) table.style.display = 'table';
                })
                .catch(error => {
                    console.error('? Error:', error);
                    if (loadingDiv) loadingDiv.style.display = 'none';
                    mostrarError('Error de conexión al cargar categorías');
                });
        }
        
        let categoriasSelectCache = [];

        function renderCategoriasSelect(listado) {
            const select = document.getElementById('categoria-select');
            if (!select) return;
            // Limpiar opciones existentes (excepto la primera)
            while (select.children.length > 1) {
                select.removeChild(select.lastChild);
            }
            listado.forEach(categoria => {
                const option = document.createElement('option');
                option.value = categoria.idCategoria;
                option.textContent = categoria.nombre;
                select.appendChild(option);
            });
        }

        function filtrarCategoriasSelect() {
            const input = document.getElementById('categoria-search');
            const query = (input && input.value ? input.value : '').toLowerCase().trim();
            if (!query) {
                renderCategoriasSelect(categoriasSelectCache);
                return;
            }
            const filtradas = categoriasSelectCache.filter(c => {
                const nombre = (c.nombre || '').toLowerCase();
                return nombre.includes(query);
            });
            renderCategoriasSelect(filtradas);
        }

        function cargarCategoriasSelect() {
            console.log('?? Cargando categorías para select...');
            
            fetch('/api/categorias')
                .then(response => response.json())
                .then(categorias => {
                    categoriasSelectCache = Array.isArray(categorias) ? categorias : [];
                    renderCategoriasSelect(categoriasSelectCache);
                    filtrarCategoriasSelect();
                })
                .catch(error => {
                    console.error('? Error al cargar categorías para select:', error);
                });
        }
        
        function mostrarCategorias(categorias, totalCategorias = null, filtroLabel = '') {
            const tbody = document.getElementById('categoriasTableBody');
            const contador = document.getElementById('contadorCategorias');

                if (!tbody) return;

                tbody.innerHTML = '';

                if (categorias && categorias.length > 0) {
                    categorias.forEach(categoria => {
                        const row = document.createElement('tr');
                        row.innerHTML = `
                        <td>${categoria.nombre}</td>
                        <td>${categoria.descripcion}</td>
                        <td class="text-center">
                            <button class="btn btn-sm btn-outline-primary me-1" onclick="editarCategoria(${categoria.idCategoria})" title="Editar">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-danger" onclick="eliminarCategoria(${categoria.idCategoria})" title="Eliminar">
                                <i class="fas fa-trash"></i>
                            </button>
                        </td>
                    `;
                        tbody.appendChild(row);
                    });

                    if (contador) {
                        if (typeof totalCategorias === 'number' && totalCategorias !== categorias.length) {
                            const sufijo = filtroLabel ? ` (filtro por ${filtroLabel})` : '';
                            contador.textContent = `Mostrando ${categorias.length} de ${totalCategorias} categorías${sufijo}`;
                        } else {
                            contador.textContent = `Total: ${categorias.length} categorías`;
                        }
                    }
                } else {
                    const row = document.createElement('tr');
                    row.innerHTML = '<td colspan="3" class="text-center text-muted">No hay categorías registradas</td>';
                    tbody.appendChild(row);

                    if (contador) {
                        if (typeof totalCategorias === 'number') {
                            contador.textContent = `Mostrando 0 de ${totalCategorias} categorías`;
                        } else {
                            contador.textContent = 'Total: 0 categorías';
                        }
                    }
                }
            }

            function mostrarError(mensaje) {
                const errorDiv = document.getElementById('categoriasError');
                if (errorDiv) {
                    errorDiv.textContent = mensaje;
                    errorDiv.style.display = 'block';
                }
            }

            // Funciones globales para el modal
            window.categoriaModal = {
                hide: function () {
                    const modal = document.getElementById('categoriaModal');
                    if (modal) {
                        modal.style.display = 'none';
                    }
                }
            };

            // Controlador de categorías
            window.categoriaController = {
                _parseJsonResponse: function (response) {
                    if (response.ok) return response.json();
                    return response.json()
                        .catch(() => ({}))
                        .then(err => {
                            throw new Error(err.message || 'Error en la respuesta del servidor');
                        });
                },
                guardarCategoria: function () {
                    console.log('?? Guardando categoría...');

                    const form = document.getElementById('categoriaForm');
                    const formData = new FormData(form);
                    const categoriaId = document.getElementById('categoriaId').value;

                    const categoria = {
                        nombre: formData.get('categoriaNombre'),
                        descripcion: formData.get('categoriaDescripcion')
                    };

                    // Validaciones básicas
                    if (!categoria.nombre || categoria.nombre.trim().length < 2) {
                        mostrarNotificacion('El nombre debe tener al menos 2 caracteres', 'error');
                        return;
                    }

                    if (!categoria.descripcion || categoria.descripcion.trim().length < 5) {
                        mostrarNotificacion('La descripcion debe tener al menos 5 caracteres', 'error');
                        return;
                    }

                    // Determinar si es creación o actualización
                    const url = categoriaId ? `/api/categorias/${categoriaId}` : '/api/categorias';
                    const method = categoriaId ? 'PUT' : 'POST';

                    if (categoriaId) {
                        categoria.id = parseInt(categoriaId);
                    }

                    fetch(url, {
                        method: method,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(categoria)
                    })
                        .then(response => this._parseJsonResponse(response))
                        .then(data => {
                            console.log('? Categoría guardada:', data);
                            form.reset();
                            document.getElementById('categoriaId').value = '';
                            document.getElementById('btnGuardarText').textContent = 'Crear Categoría';
                            cargarCategorias();
                            cargarCategoriasSelect();
                            mostrarNotificacion(categoriaId ? 'Categoría actualizada exitosamente' : 'Categoría creada exitosamente', 'success');
                        })
                        .catch(error => {
                            console.error('? Error:', error);
                            mostrarNotificacion('Error al guardar categoría: ' + error.message, 'error');
                        });
                },

                cancelarEdicion: function () {
                    const form = document.getElementById('categoriaForm');
                    if (form) {
                        form.reset();
                        document.getElementById('categoriaId').value = '';
                        document.getElementById('btnGuardarText').textContent = 'Crear Categoría';
                        console.log('? Formulario cancelado y limpiado');
                    }
                }
            };

            // Funciones globales para botones
            window.editarCategoria = function (id) {
                console.log('?? Editando categoría:', id);

                fetch(`/api/categorias/${id}`)
                    .then(response => window.categoriaController._parseJsonResponse(response))
                    .then(categoria => {
                        const categoriaId = categoria.idCategoria || categoria.id;
                        document.getElementById('categoriaId').value = categoriaId;
                        document.getElementById('categoriaNombre').value = categoria.nombre || '';
                        document.getElementById('categoriaDescripcion').value = categoria.descripcion || '';
                        document.getElementById('btnGuardarText').textContent = 'Actualizar Categoría';

                        // Scroll al formulario
                        document.querySelector('.categoria-form-section').scrollIntoView({ behavior: 'smooth' });
                    })
                    .catch(error => {
                        console.error('Error al cargar categoría:', error);
                        mostrarNotificacion(error.message || 'Error al cargar la categoría para editar', 'error');
                    });
            };

            window.eliminarCategoria = function (id) {
                console.log('??? Eliminando categoría:', id);

                ModalConfirmacion.show(
                    'Eliminar Categoría',
                    '¿Estás seguro de que deseas eliminar esta categoría?',
                    () => {
                        fetch(`/api/categorias/${id}`, {
                            method: 'DELETE'
                        })
                            .then(response => window.categoriaController._parseJsonResponse(response))
                            .then(data => {
                                if (data.success) {
                                    console.log('? Categoría eliminada');
                                    cargarCategorias();
                                    cargarCategoriasSelect();
                                    mostrarNotificacion(data.message || 'Categoría eliminada exitosamente', 'success');
                                } else {
                                    console.error('? Error:', data.message);
                                    mostrarNotificacion(data.message || 'Error al eliminar la categoría', 'error');
                                }
                            })
                            .catch(error => {
                                console.error('? Error:', error);
                                mostrarNotificacion(error.message || 'Error al eliminar categoría', 'error');
                            });
                    }
                );
            };

            // Función global para remover categoría (accesible desde onclick)
            window.removerCategoria = function (index) {
                categoriasSeleccionadas.splice(index, 1);
                actualizarCategoriasChips();
                console.log('? Categoría removida, total:', categoriasSeleccionadas.length);
            };

            // Funciones globales para docentes (accesibles desde onclick)
            window.agregarDocente = function (id, nombre, tableId) {
                const tabla = document.getElementById(tableId);
                const tbody = tabla.querySelector('tbody');

                // Verificar si el docente ya está agregado
                const tipoSeccion = tableId.includes('formacion') ? 'formacion' : 'curso';
                const yaAgregado = docentesSeleccionados[tipoSeccion].find(d => d.id === id);

                if (yaAgregado) {
                    mostrarNotificacion('Este docente ya ha sido agregado', 'warning');
                    return;
                }

                // Agregar a la lista de seleccionados
                docentesSeleccionados[tipoSeccion].push({ id, nombre });

                // Crear fila en la tabla
                const fila = document.createElement('tr');
                fila.innerHTML = `
                <td>${nombre}</td>
                <td>
                    <button type="button" class="btn-remove-docente" onclick="removerDocente('${id}', '${tableId}', this)">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;

                tbody.appendChild(fila);

                // Ocultar resultados de búsqueda
                const resultsDiv = document.querySelector('.search-results[style*="block"]');
                if (resultsDiv) {
                    resultsDiv.style.display = 'none';
                }

                // Limpiar campo de búsqueda
                const searchInput = tableId.includes('formacion') ?
                    document.getElementById('docente-search-formacion') :
                    document.getElementById('docente-search');
                if (searchInput) {
                    searchInput.value = '';
                }

                console.log('? Docente agregado:', nombre);

                // Actualizar campo hidden del formulario
                actualizarDocentesHidden(tipoSeccion);
            };

            window.removerDocente = function (id, tableId, button) {
                const tipoSeccion = tableId.includes('formacion') ? 'formacion' : 'curso';

                // Remover de la lista
                docentesSeleccionados[tipoSeccion] = docentesSeleccionados[tipoSeccion].filter(d => d.id !== id);

                // Remover fila de la tabla
                const fila = button.closest('tr');
                fila.remove();

                // Actualizar campo hidden del formulario
                actualizarDocentesHidden(tipoSeccion);

                console.log('? Docente removido');
            };

            // Función para actualizar campos hidden de docentes
            function actualizarDocentesHidden(tipoSeccion) {
                const fieldName = tipoSeccion === 'formacion' ? 'docentesFormacion' : 'docentesCurso';
                const hiddenField = document.getElementById(fieldName);

                if (hiddenField && docentesSeleccionados[tipoSeccion]) {
                    const ids = docentesSeleccionados[tipoSeccion].map(d => d.id).join(',');
                    hiddenField.value = ids;
                    console.log(`?? Campo ${fieldName} actualizado:`, ids);
                }
            }

            // Variables globales para disertantes
            let disertantesCharla = [];
            let disertantesSeminario = [];

            // Función global para agregar disertante
            window.agregarDisertante = function (tipo) {
                const inputId = tipo === 'charla' ? 'disertantesCharlaInput' : 'disertantesSeminarioInput';
                const listId = tipo === 'charla' ? 'disertantes-charla-list' : 'disertantes-seminario-list';
                const hiddenId = tipo === 'charla' ? 'disertantesCharlaHidden' : 'disertantesSeminarioHidden';

                const input = document.getElementById(inputId);
                const nombre = input.value.trim();

                if (!nombre) {
                    mostrarNotificacion('Por favor ingrese el nombre del disertante', 'warning');
                    return;
                }

                // Verificar si ya existe
                const listaActual = tipo === 'charla' ? disertantesCharla : disertantesSeminario;
                if (listaActual.includes(nombre)) {
                    mostrarNotificacion('Este disertante ya ha sido agregado', 'warning');
                    return;
                }

                // Agregar a la lista
                if (tipo === 'charla') {
                    disertantesCharla.push(nombre);
                } else {
                    disertantesSeminario.push(nombre);
                }

                // Actualizar la visualización
                actualizarDisertantesChips(tipo);

                // Limpiar el input
                input.value = '';

                console.log('? Disertante agregado:', nombre);
            };

            // Función para actualizar los chips de disertantes
            function actualizarDisertantesChips(tipo) {
                const listId = tipo === 'charla' ? 'disertantes-charla-list' : 'disertantes-seminario-list';
                const hiddenId = tipo === 'charla' ? 'disertantesCharlaHidden' : 'disertantesSeminarioHidden';
                const lista = tipo === 'charla' ? disertantesCharla : disertantesSeminario;

                const container = document.getElementById(listId);
                const hiddenInput = document.getElementById(hiddenId);

                if (!container || !hiddenInput) {
                    console.error('? Elementos no encontrados para tipo:', tipo);
                    return;
                }

                // Limpiar contenedor
                container.innerHTML = '';

                // Crear chips
                lista.forEach((nombre, index) => {
                    const chip = document.createElement('span');
                    chip.className = 'categoria-chip';
                    chip.innerHTML = `
                    ${nombre}
                    <button type="button" class="remove-chip" onclick="removerDisertante(${index}, '${tipo}')">
                        <i class="fas fa-times"></i>
                    </button>
                `;
                    container.appendChild(chip);
                });

                // Actualizar input hidden
                hiddenInput.value = lista.join(',');

                console.log(`?? Chips actualizados para ${tipo}:`, lista.length);
            }

            // Función global para remover disertante
            window.removerDisertante = function (index, tipo) {
                if (tipo === 'charla') {
                    disertantesCharla.splice(index, 1);
                } else {
                    disertantesSeminario.splice(index, 1);
                }

                actualizarDisertantesChips(tipo);
                console.log('? Disertante removido');
            };

            // ================= GESTI�N DE HORARIOS =================

            // Variable global para horarios
            var horariosSeleccionados = [];

            // Función global para agregar horario
            window.agregarHorario = function () {
                const dia = document.getElementById('dia-horario').value;
                const horaInicio = document.getElementById('hora-inicio').value;
                const horaFin = document.getElementById('hora-fin').value;

                // Validaciones
                if (!dia) {
                    mostrarNotificacion('Por favor seleccione un día', 'warning');
                    return;
                }

                if (!horaInicio || !horaFin) {
                    mostrarNotificacion('Por favor complete las horas de inicio y fin', 'warning');
                    return;
                }

                if (horaInicio >= horaFin) {
                    mostrarNotificacion('La hora de inicio debe ser anterior a la hora de fin', 'warning');
                    return;
                }

                const toMinutes = (valor) => {
                    if (!valor) return 0;
                    const partes = valor.toString().split(':');
                    const h = parseInt(partes[0] || '0', 10);
                    const m = parseInt(partes[1] || '0', 10);
                    return (h * 60) + m;
                };
                const nuevoInicio = toMinutes(horaInicio);
                const nuevoFin = toMinutes(horaFin);

                // Verificar conflictos de horario
                const conflicto = horariosSeleccionados.find(h => {
                    if (h.dia !== dia) return false;
                    const existenteInicio = toMinutes(h.horaInicio);
                    const existenteFin = toMinutes(h.horaFin);
                    return nuevoInicio < existenteFin && nuevoFin > existenteInicio;
                });

                if (conflicto) {
                    mostrarNotificacion('Existe un conflicto de horario en el mismo día', 'error');
                    return;
                }

                // Crear objeto horario
                const horario = {
                    dia: dia,
                    horaInicio: horaInicio,
                    horaFin: horaFin
                };

                // Agregar a la lista
                horariosSeleccionados.push(horario);

                // Actualizar visualización
                actualizarHorariosChips();

                // Limpiar formulario
                document.getElementById('dia-horario').value = '';
                document.getElementById('hora-inicio').value = '';
                document.getElementById('hora-fin').value = '';

                console.log('? Horario agregado:', horario);
            };

            // Función para actualizar chips de horarios
            function actualizarHorariosChips() {
                const container = document.getElementById('lista-horarios');
                const hiddenInput = document.getElementById('horarios');

                // Limpiar contenedor
                container.innerHTML = '';

                if (horariosSeleccionados.length === 0) {
                    container.innerHTML = '<p class="no-horarios">No hay horarios programados</p>';
                    hiddenInput.value = '';
                    return;
                }

                // ? ORDENAR horarios por día y hora antes de mostrar
                const DIAS_ORDEN = {
                    'LUNES': 1, 'MARTES': 2, 'MIERCOLES': 3, 'MI�0RCOLES': 3,
                    'JUEVES': 4, 'VIERNES': 5, 'SABADO': 6, 'SÁBADO': 6, 'DOMINGO': 7
                };

                horariosSeleccionados.sort((a, b) => {
                    const diaA = (a.dia || '').toUpperCase().trim();
                    const diaB = (b.dia || '').toUpperCase().trim();
                    const ordenA = DIAS_ORDEN[diaA] || 999;
                    const ordenB = DIAS_ORDEN[diaB] || 999;

                    // Si son días diferentes, ordenar por día
                    if (ordenA !== ordenB) {
                        return ordenA - ordenB;
                    }

                    // Si es el mismo día, ordenar por hora de inicio
                    const horaA = a.horaInicio || '';
                    const horaB = b.horaInicio || '';
                    return horaA.localeCompare(horaB);
                });

                // Crear chips para cada horario
                horariosSeleccionados.forEach((horario, index) => {
                    const chip = document.createElement('div');
                    chip.className = 'horario-chip';
                    chip.innerHTML = `
                    <div class="horario-info">
                        <strong>${horario.dia || 'Sin día'}</strong><br>
                        ${horario.horaInicio || '--:--'} - ${horario.horaFin || '--:--'}
                    </div>
                    <button type="button" class="remove-horario" onclick="removerHorario(${index})">
                        <i class="fas fa-times"></i>
                    </button>
                `;
                    container.appendChild(chip);
                });

                // Actualizar input hidden con JSON
                hiddenInput.value = JSON.stringify(horariosSeleccionados);

                console.log('?? Horarios actualizados y ordenados:', horariosSeleccionados.length);
            }

            // Función global para remover horario
            window.removerHorario = function (index) {
                horariosSeleccionados.splice(index, 1);
                actualizarHorariosChips();
                console.log('? Horario removido');
            };

            // (Bloque de propuestas automáticas movido a static/js/gestionOfertas.js)

            /**
             * UTILIDAD: Wrapper para notificaciones (por si no tienes Toastr/Swal)
             * Si ya tienes una función mostrarNotificacion global, puedes borrar esto.
             */
            if (typeof window.mostrarNotificacion !== 'function') {
                window.mostrarNotificacion = function (mensaje, tipo) {
                    // Fallback simple por si no existe la librería de alertas
                    alert((tipo === 'error' ? '? ' : '? ') + mensaje);
                };
            }

            // ================= VALIDACI�N Y ENVÍO DEL FORMULARIO =================

            // Función para validar formulario
            function validarFormulario(tipoOferta) {
                const titulo = document.getElementById('nombre').value.trim();
                const descripcion = document.getElementById('descripcion').value.trim();

                if (!titulo || !descripcion) {
                    mostrarNotificacion('Por favor complete el título y la descripción', 'error');
                    return false;
                }

                // Validación de fechas obligatorias
                const fechaInicio = document.getElementById('fechaInicio').value;
                const fechaFin = document.getElementById('fechaFin').value;

                if (!fechaInicio) {
                    mostrarNotificacion('Por favor seleccione la fecha de inicio', 'error');
                    return false;
                }

                if (!fechaFin) {
                    mostrarNotificacion('Por favor seleccione la fecha de fin', 'error');
                    return false;
                }

                // ? Validar que las fechas no sean pasadas
                const hoy = new Date();
                hoy.setHours(0, 0, 0, 0); // Resetear horas para comparar solo fechas
                const fechaInicioObj = new Date(fechaInicio + 'T00:00:00');
                const fechaFinObj = new Date(fechaFin + 'T00:00:00');

                if (fechaInicioObj < hoy) {
                    mostrarNotificacion('La fecha de inicio no puede ser anterior a hoy', 'error');
                    return false;
                }

                if (fechaFinObj < hoy) {
                    mostrarNotificacion('La fecha de fin no puede ser anterior a hoy', 'error');
                    return false;
                }

                // Validar que la fecha de inicio no sea posterior a la fecha de fin
                if (fechaInicioObj > fechaFinObj) {
                    mostrarNotificacion('La fecha de inicio no puede ser posterior a la fecha de fin', 'error');
                    return false;
                }
                // Validar costo
                const costoVal = document.getElementById('costoInscripcion') ? parseFloat(document.getElementById('costoInscripcion').value) : NaN;
                if (!isNaN(costoVal) && costoVal < 0) {
                    mostrarNotificacion('El costo debe ser mayor o igual a 0', 'error');
                    return false;
                }

                // Validaciones específicas según tipo
                switch (tipoOferta) {
                    case 'CURSO':
                    case 'FORMACION': {
                        const tipoKey = tipoOferta === 'CURSO' ? 'curso' : 'formacion';
                        const docentes = docentesSeleccionados[tipoKey] || [];
                        if (docentes.length === 0) {
                            mostrarNotificacion('Debe asignarse al menos un docente para la oferta', 'error');
                            return false;
                        }
                        if (!horariosSeleccionados || horariosSeleccionados.length === 0) {
                            mostrarNotificacion('Debe agregarse al menos un horario para la oferta', 'error');
                            return false;
                        }
                        break;
                    }

                    case 'CHARLA':
                    case 'SEMINARIO': {
                        // Validar que la fecha de inscripción termine ANTES de la fecha del evento
                        const fechaFinInscripcion = getValueById('fechaFinInscripcion');
                        const fechaEvento = tipoOferta === 'CHARLA'
                            ? getValueById('fechaCharla')
                            : getValueById('fechaInicio');

                        if (fechaFinInscripcion && fechaEvento) {
                            const fechaFinInscripcionObj = new Date(fechaFinInscripcion + 'T00:00:00');
                            const fechaEventoObj = new Date(fechaEvento + 'T00:00:00');

                            if (fechaFinInscripcionObj >= fechaEventoObj) {
                                mostrarNotificacion(
                                    'La fecha de fin de inscripción debe ser ANTES de la fecha de la ' +
                                    (tipoOferta === 'CHARLA' ? 'charla' : 'seminario'),
                                    'error'
                                );
                                return false;
                            }
                        }

                        if (tipoOferta === 'SEMINARIO') {
                            const numeroEncuentros = parseInt(getValueById('numeroEncuentros') || '0', 10);
                            const fechaInicio = getValueById('fechaInicio');
                            const fechaFin = getValueById('fechaFin');

                            if (!numeroEncuentros || numeroEncuentros <= 0) {
                                mostrarNotificacion('Debes indicar el número de encuentros del seminario', 'error');
                                return false;
                            }

                            if (fechaInicio && fechaFin) {
                                const inicioObj = new Date(fechaInicio + 'T00:00:00');
                                const finObj = new Date(fechaFin + 'T00:00:00');
                                const diffDays = Math.floor((finObj - inicioObj) / (1000 * 60 * 60 * 24)) + 1;
                                if (numeroEncuentros > diffDays) {
                                    mostrarNotificacion('El número de encuentros no puede superar los días entre fecha inicio y fecha fin', 'error');
                                    return false;
                                }

                                const fechasEncuentrosInputs = Array.from(document.querySelectorAll('input[name="fechasEncuentros"]'));
                                if (fechasEncuentrosInputs.length !== numeroEncuentros) {
                                    mostrarNotificacion('Debe indicar la fecha de cada encuentro del seminario', 'error');
                                    return false;
                                }

                                for (let i = 0; i < fechasEncuentrosInputs.length; i++) {
                                    const valor = fechasEncuentrosInputs[i].value;
                                    if (!valor) {
                                        mostrarNotificacion('Todos los encuentros deben tener una fecha asignada', 'error');
                                        return false;
                                    }
                                    const fechaEncuentro = new Date(valor + 'T00:00:00');
                                    if (fechaEncuentro < inicioObj || fechaEncuentro > finObj) {
                                        mostrarNotificacion('Las fechas de los encuentros deben estar entre la fecha de inicio y fin del seminario', 'error');
                                        return false;
                                    }
                                }
                            }
                        }

                        // IDs genéricos para lugar y enlace
                        const enlaceEl = document.getElementById('enlace');
                        const lugarEl = document.getElementById('lugar');

                        const enlace = enlaceEl ? enlaceEl.value.trim() : '';
                        const lugar = lugarEl ? lugarEl.value.trim() : '';

                        const modalidad = document.getElementById('modalidad').value;
                        const esVirtual = modalidad === 'VIRTUAL' || modalidad === 'HIBRIDA';
                        const esPresencial = modalidad === 'PRESENCIAL' || modalidad === 'HIBRIDA';

                        if (esVirtual) {
                            if (!enlace) {
                                mostrarNotificacion('Para modalidad Virtual o Híbrida debe de ingresar un enlace', 'error');
                                return false;
                            }
                            try {
                                new URL(enlace); // valida formato
                            } catch (e) {
                                mostrarNotificacion('El enlace proporcionado no es una URL válida', 'error');
                                return false;
                            }
                        }

                        if (esPresencial) {
                            if (!lugar) {
                                mostrarNotificacion('Para modalidad Presencial o Híbrida debe de ingresar un lugar', 'error');
                                return false;
                            }
                        }

                        const disertantes = tipoOferta === 'CHARLA' ? disertantesCharla : disertantesSeminario;

                        if (!disertantes || disertantes.length === 0) {
                            mostrarNotificacion('Se requiere al menos un disertante', 'error');
                            return false;
                        }
                        break;
                    }

                    default:
                    // no-op
                }

                return true;
            }

            // ================= ENVÍO AJAX Y NOTIFICACIONES =================

            // Función para enviar formulario con AJAX
            function enviarFormularioAjax() {
                console.log('?? Iniciando envío AJAX');

                const formulario = document.getElementById('oferta-form');
                const formData = new FormData(formulario);

                // Manejar cupos infinitos: si está vacío, enviar Integer.MAX_VALUE
                const cuposVal = formData.get('cupos');
                if (!cuposVal || cuposVal.trim() === '') {
                    formData.set('cupos', '2147483647');
                    console.log('?? Cupos establecidos como ilimitados (MAX_VALUE)');
                }

                // Debug: mostrar datos del formulario
                console.log('?? Datos del formulario:');
                for (let [key, value] of formData.entries()) {
                    console.log(`  ${key}: ${value}`);
                }

                // Mostrar loading en el botón
                const btnSubmit = formulario.querySelector('button[type="submit"]');
                const textoOriginal = btnSubmit.innerHTML;
                btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Registrando...';
                btnSubmit.disabled = true;

                console.log('?? Enviando request a /admin/ofertas/registrar');

                fetch('/admin/ofertas/registrar', {
                    method: 'POST',
                    body: formData
                })
                    .then(response => {
                        console.log('?? Respuesta recibida:', response.status);
                        return response.json();
                    })
                    .then(data => {
                        console.log('?? Datos de respuesta:', data);

                        if (data.success) {
                            console.log('? Registro exitoso');
                            // Mostrar notificación de éxito
                            mostrarNotificacion('? ' + data.message, 'success');

                            // Limpiar formulario
                            limpiarFormularioCompleto();

                            // Cerrar formulario después de un momento
                            setTimeout(() => {
                                cerrarFormulario();
                            }, 1500);

                            // Actualizar tabla de ofertas
                            actualizarTablaOfertas();

                        } else {
                            console.log('? Error en registro:', data.message);
                            mostrarNotificacion('? ' + data.message, 'error');
                        }
                    })
                    .catch(error => {
                        console.error('? Error en fetch:', error);
                        mostrarNotificacion('? Error al registrar la oferta. Inténtelo nuevamente.', 'error');
                    })
                    .finally(() => {
                        // Restaurar botón
                        btnSubmit.innerHTML = textoOriginal;
                        btnSubmit.disabled = false;
                    });
            }


        
        function limpiarCategoriasSeleccionadas() {
            categoriasSeleccionadas = [];
            const selectedContainer = document.getElementById('selected-categories');
            const selectedChips = document.getElementById('selected-chips');
            const formCategorias = document.getElementById('categorias');
            const select = document.getElementById('categoria-select');
            const searchInput = document.getElementById('categoria-search');
            if (selectedChips) selectedChips.innerHTML = '';
            if (selectedContainer) selectedContainer.style.display = 'none';
            if (formCategorias) formCategorias.value = '';
            if (select) select.selectedIndex = 0;
            if (searchInput) searchInput.value = '';
            if (typeof renderCategoriasSelect === 'function') {
                renderCategoriasSelect(categoriasSelectCache);
            }
        }

        // Función para limpiar formulario completo
        function limpiarFormularioCompleto() {
            const formulario = document.getElementById('oferta-form');
            formulario.reset();
            
            // Limpiar datos específicos
            limpiarDatosFormulario();
            limpiarCategoriasSeleccionadas();
            
            // Ocultar todas las secciones específicas
            const seccionesEspecificas = document.querySelectorAll('.tipo-specific');
            seccionesEspecificas.forEach(seccion => {
                seccion.style.display = 'none';
            });
            
            // Ocultar sección de horarios
            const seccionHorarios = document.getElementById('seccion-horarios');
            if (seccionHorarios) {
                seccionHorarios.style.display = 'none';
            }
            
            console.log('?? Formulario limpiado completamente');
        }
        
        // Función para cerrar formulario
        function cerrarFormulario() {
            const formContainer = document.getElementById('form-container');
            if (formContainer) {
                formContainer.classList.remove('show');
                setTimeout(() => {
                    formContainer.style.display = 'none';
                }, 500);
            }
        }
        
        // Variables de paginación
        let currentPage = 1;
        const itemsPerPage = 10;
        let allOfertas = [];
        let filteredOfertas = [];
        let currentSort = { key: 'fechaInicio', direction: 'desc' };

        function normalizeText(value) {
            if (value == null) return '';
            return String(value)
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .toUpperCase();
        }

        function parseDateValue(value) {
            if (!value) return null;
            if (value instanceof Date) {
                return Number.isNaN(value.getTime()) ? null : value.getTime();
            }
            if (typeof value === 'string') {
                const match = value.match(/^(\d{4})-(\d{2})-(\d{2})/);
                if (match) {
                    const year = Number(match[1]);
                    const month = Number(match[2]) - 1;
                    const day = Number(match[3]);
                    return new Date(year, month, day).getTime();
                }
            }
            const parsed = new Date(value);
            return Number.isNaN(parsed.getTime()) ? null : parsed.getTime();
        }

        function getOfertaSortValue(oferta, key) {
            switch (key) {
                case 'nombre':
                    return normalizeText(oferta.nombre || '');
                case 'tipo':
                    return normalizeText(oferta.tipo || oferta.tipoOferta || '');
                case 'modalidad':
                    return normalizeText(oferta.modalidad || '');
                case 'cupos': {
                    const cupos = oferta.cupos;
                    if (cupos == null) return null;
                    return Number(cupos === 2147483647 ? Number.MAX_SAFE_INTEGER : cupos);
                }
                case 'fechaInicio':
                    return parseDateValue(oferta.fechaInicio);
                case 'fechaFin':
                    return parseDateValue(oferta.fechaFin);
                case 'estado':
                    return normalizeText(oferta.estado || '');
                default:
                    return null;
            }
        }

        function sortOfertasData(data) {
            const sortKey = (currentSort ? currentSort.key : null);
            const dir = (currentSort ? currentSort.direction : null) === 'desc' ? -1 : 1;
            if (!sortKey) return Array.isArray(data) ? [...data] : [];
            const list = Array.isArray(data) ? [...data] : [];
            list.sort((a, b) => {
                const va = getOfertaSortValue(a, sortKey);
                const vb = getOfertaSortValue(b, sortKey);
                if (va == null && vb == null) return 0;
                if (va == null) return 1;
                if (vb == null) return -1;
                if (typeof va === 'number' && typeof vb === 'number') {
                    return (va - vb) * dir;
                }
                return String(va).localeCompare(String(vb), 'es', { sensitivity: 'base' }) * dir;
            });
            return list;
        }

        function setupSortableOfferHeaders() {
            const table = document.getElementById('ofertas-table');
            if (!table || table.dataset.sortInit === '1') return;
            table.dataset.sortInit = '1';
            const headers = table.querySelectorAll('thead th');
            const keyMap = ['nombre', 'tipo', 'modalidad', 'cupos', 'fechaInicio', 'fechaFin', 'estado', null];
            headers.forEach((th, index) => {
                const key = keyMap[index];
                if (!key) return;
                th.style.cursor = 'pointer';
                th.addEventListener('click', () => {
                    if (currentSort.key === key) {
                        currentSort.direction = currentSort.direction === 'asc' ? 'desc' : 'asc';
                    } else {
                        currentSort.key = key;
                        currentSort.direction = 'asc';
                    }
                    renderizarPaginaActual();
                });
            });
        }

            // Función para actualizar tabla de ofertas
            function actualizarTablaOfertas() {
                console.log('?? Actualizando tabla de ofertas...');
                fetch('/admin/ofertas/listar')
                    .then(response => response.json())
                    .then(data => {
                        console.log('?? Respuesta del servidor:', data);
                        if (data.success && data.data) {
                            // Guardar datos globales
                            allOfertas = Array.isArray(data.data) ? data.data : [];
                            filteredOfertas = [...allOfertas]; // Inicialmente sin filtros adicionales

                            // Resetear a página 1
                            currentPage = 1;

                            // Renderizar
                            renderizarPaginaActual();

                            console.log('? Tabla actualizada con', data.data.length, 'ofertas');
                        }
                    })
                    .catch(error => {
                        console.error('? Error al actualizar tabla:', error);
                    });
            }

            // Funciones de paginación
            window.cambiarPagina = function (delta) {
                const totalPages = Math.ceil(filteredOfertas.length / itemsPerPage);
                const newPage = currentPage + delta;

                if (newPage >= 1 && newPage <= totalPages) {
                    currentPage = newPage;
                    renderizarPaginaActual();
                }
            };

            window.irAPagina = function (page) {
                currentPage = page;
                renderizarPaginaActual();
            };

            function renderizarPaginaActual() {
                const start = (currentPage - 1) * itemsPerPage;
                const end = start + itemsPerPage;
                const sorted = sortOfertasData(filteredOfertas);
                const pageData = sorted.slice(start, end);

                actualizarTablaConDatos(pageData);
                actualizarControlesPaginacion();
                actualizarInfoPaginacion(start, end);
            }

            function actualizarControlesPaginacion() {
                const totalPages = Math.ceil(filteredOfertas.length / itemsPerPage);
                const btnPrev = document.getElementById('btn-prev');
                const btnNext = document.getElementById('btn-next');
                const pagesContainer = document.getElementById('pagination-pages');

                if (btnPrev) btnPrev.disabled = currentPage === 1;
                if (btnNext) btnNext.disabled = currentPage === totalPages || totalPages === 0;

                if (pagesContainer) {
                    pagesContainer.innerHTML = '';

                    // Lógica simple de paginación: mostrar rango alrededor de actual
                    let startPage = Math.max(1, currentPage - 2);
                    let endPage = Math.min(totalPages, startPage + 4);

                    if (endPage - startPage < 4) {
                        startPage = Math.max(1, endPage - 4);
                    }

                    for (let i = startPage; i <= endPage; i++) {
                        const btn = document.createElement('button');
                        btn.className = `btn-page ${i === currentPage ? 'active' : ''}`;
                        btn.textContent = i;
                        btn.onclick = () => irAPagina(i);
                        pagesContainer.appendChild(btn);
                    }
                }
            }

            function actualizarInfoPaginacion(start, end) {
                const infoDiv = document.getElementById('pagination-info');
                const total = filteredOfertas.length;
                const realEnd = Math.min(end, total);
                const realStart = total === 0 ? 0 : start + 1;

                if (infoDiv) {
                    infoDiv.textContent = `Mostrando ${realStart}-${realEnd} de ${total} ofertas`;
                }

                // Actualizar también el contador del header si existe
                actualizarContadorOfertas(total);
            }

            function aplicarFiltros() {
                const searchInput = document.getElementById('search-input').value.toLowerCase();
                const tipo = document.getElementById('filtro-tipo').value;
                const modalidad = document.getElementById('filtro-modalidad').value;
                const estado = document.getElementById('filtro-estado').value;
                const certificado = document.getElementById('filtro-certificado').value;

                filteredOfertas = allOfertas.filter(oferta => {
                    // Filtro por nombre
                    const nombreOferta = (oferta.nombre || '').toLowerCase();
                    if (searchInput && !nombreOferta.includes(searchInput)) return false;

                    // Filtro por tipo
                    const tipoOferta = normalizeText(oferta.tipo || oferta.tipoOferta || '');
                    const tipoFiltro = normalizeText(tipo);
                    if (tipoFiltro && tipoOferta !== tipoFiltro) return false;

                    // Filtro por modalidad
                    const modalidadOferta = normalizeText(oferta.modalidad || '');
                    const modalidadFiltro = normalizeText(modalidad);
                    if (modalidadFiltro && modalidadOferta !== modalidadFiltro) return false;

                    // Filtro por estado
                    const estadoOferta = normalizeText(oferta.estado || '');
                    const estadoFiltro = normalizeText(estado);
                    if (estadoFiltro && estadoOferta !== estadoFiltro) return false;

                    // Filtro por certificado
                    if (certificado) {
                        const tieneCertificado = oferta.certificado === true || oferta.certificado === 'true' || oferta.certificado === 'SI';
                        if (certificado === 'SI' && !tieneCertificado) return false;
                        if (certificado === 'NO' && tieneCertificado) return false;
                    }

                    return true;
                });

                currentPage = 1;
                renderizarPaginaActual();
            }

            function limpiarFiltros() {
                document.getElementById('search-input').value = '';
                document.getElementById('filtro-tipo').value = '';
                document.getElementById('filtro-modalidad').value = '';
                document.getElementById('filtro-estado').value = '';
                document.getElementById('filtro-certificado').value = '';

                filteredOfertas = [...allOfertas];
                currentPage = 1;
                renderizarPaginaActual();
            }

            // Función para actualizar tabla con nuevos datos
            function actualizarTablaConDatos(ofertas) {
                console.log('?? Actualizando tabla con', ofertas.length, 'ofertas');
                const tbody = document.querySelector('#ofertas-table tbody');
                if (!tbody) {
                    console.log('? No se encontró tbody de la tabla');
                    return;
                }

                tbody.innerHTML = '';

                if (ofertas.length === 0) {
                    tbody.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center text-muted py-4">
                            <i class="fas fa-inbox fa-2x mb-2"></i><br>
                            No hay ofertas académicas registradas
                        </td>
                    </tr>
                `;
                    return;
                }

                ofertas.forEach((oferta, index) => {
                    console.log(`?? Procesando oferta ${index + 1}:`, oferta.nombre);
                    const fila = document.createElement('tr');

                    const rawId = oferta.id || oferta.idOferta || oferta.id_oferta;
                    const idLiteral = rawId != null ? JSON.stringify(rawId) : 'null';
                    const dataId = rawId != null ? rawId : '';
                    const nombreLiteral = JSON.stringify(oferta.nombre || '');

                    fila.innerHTML = `
                    <td>${oferta.nombre || '-'}</td>
                    <td>${oferta.tipoOferta || oferta.tipo || '-'}</td>
                    <td>${oferta.modalidad || '-'}</td>
                    <td class="cupos-cell">
                        ${(oferta.cupos && oferta.cupos !== 2147483647) ?
                            `<span class="cupos-disponibles">${oferta.cupos}</span>` :
                            '<span class="cupos-ilimitados">8</span>'
                        }
                    </td>
                    <td>${oferta.fechaInicio ? formatearFecha(oferta.fechaInicio) : '-'}</td>
                    <td>${oferta.fechaFin ? formatearFecha(oferta.fechaFin) : '-'}</td>
                    <td><span class="estado-badge">${oferta.estado || 'ACTIVA'}</span></td>
                    <td class="acciones-cell">
                        <button class="btn-accion btn-ver" title="Ver información" data-id="${dataId}" onclick="verDetalleOferta(${idLiteral})">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn-accion btn-modificar" title="Modificar" data-id="${dataId}" onclick='modificarOferta(${idLiteral}, ${nombreLiteral})'>
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn-accion btn-eliminar" title="Eliminar" data-id="${dataId}" onclick='eliminarOferta(${idLiteral}, ${nombreLiteral})'>
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                `;
                    tbody.appendChild(fila);
                });
            }
            // Función para actualizar contador de ofertas
            function actualizarContadorOfertas(total) {
                const contador = document.getElementById('total-ofertas');
                if (contador) {
                    contador.textContent = total;
                }
            }

            // ================= FUNCIONES PARA EL MODAL DE DETALLE =================

            // Variable global para almacenar el ID de la oferta actual
            let ofertaActualId = null;

            // Función global para abrir modal de detalle
            window.verDetalleOferta = function (id) {
                console.log('?? FUNCI�N verDetalleOferta EJECUTADA con ID:', id);
                console.log('- Tipo de ID:', typeof id);

                if (!id) {
                    console.error('? ID de oferta no válido:', id);
                    mostrarNotificacion('ID de oferta no válido', 'error');
                    return;
                }

                ofertaActualId = id;
                console.log('- ID almacenado:', ofertaActualId);

                // Obtener datos de la oferta desde el servidor
                console.log('?? Realizando fetch a:', `/admin/ofertas/${id}`);
                fetch(`/admin/ofertas/${id}`)
                    .then(response => {
                        console.log('?? Respuesta recibida:', response.status, response.statusText);
                        if (!response.ok) {
                            throw new Error(`Error ${response.status}: ${response.statusText}`);
                        }
                        return response.json();
                    })
                    .then(data => {
                        console.log('? Datos de la oferta recibidos:', data);

                        // Verificar estructura de la respuesta
                        if (data.success && data.oferta) {
                            console.log('?? Estructura correcta - cargando en modal...');
                            cargarDatosEnModal(data.oferta);
                        } else if (data.oferta) {
                            console.log('?? Datos directos - cargando en modal...');
                            cargarDatosEnModal(data.oferta);
                        } else if (data) {
                            console.log('?? Datos directos (sin wrapper) - cargando en modal...');
                            cargarDatosEnModal(data);
                        } else {
                            console.error('? Estructura de respuesta no reconocida:', data);
                            mostrarNotificacion('Error: Estructura de datos no válida', 'error');
                        }

                        // Mostrar el modal después de cargar los datos
                        const modal = document.getElementById('modalDetalleOferta');
                        if (modal) {
                            modal.style.display = 'flex';
                            document.body.style.overflow = 'hidden';
                            console.log('?? Modal mostrado');
                        } else {
                            console.error('? Modal modalDetalleOferta no encontrado');
                        }
                    })
                    .catch(error => {
                        console.error('? Error al cargar detalle de la oferta:', error);
                        mostrarNotificacion('Error al cargar los datos de la oferta', 'error');
                    });
            };

            // Función para cargar datos en el modal
            function cargarDatosEnModal(oferta) {
                console.log('?? Cargando datos en el modal:', oferta);
                console.log('?? Propiedades disponibles:', Object.keys(oferta));
                console.log('?? Valores de campos clave:');
                console.log('  - nombre:', oferta.nombre);
                console.log('  - descripcion:', oferta.descripcion);
                console.log('  - tipo:', oferta.tipo);
                console.log('  - modalidad:', oferta.modalidad);
                console.log('  - estado:', oferta.estado);
                console.log('  - costoInscripcion:', oferta.costoInscripcion);
                console.log('  - cupos:', oferta.cupos);
                console.log('  - certificado:', oferta.certificado);

                // Verificar si oferta tiene datos
                if (!oferta) {
                    console.error('? Objeto oferta está vacío o undefined');
                    return;
                }

                // Información general - verificar que los elementos existen antes de asignar
                const nombre = oferta.nombre || '-';
                const nombreElement = document.getElementById('detalle-nombre');
                if (nombreElement) {
                    nombreElement.textContent = nombre;
                    console.log('? Nombre asignado:', nombre);
                } else {
                    console.error('? Elemento detalle-nombre no encontrado');
                }

                const tipo = oferta.tipo || '-';
                const tipoElement = document.getElementById('detalle-tipo');
                if (tipoElement) {
                    tipoElement.textContent = tipo;
                    tipoElement.className = `badge tipo-${tipo}`;
                    console.log('? Tipo asignado:', tipo);
                } else {
                    console.error('? Elemento detalle-tipo no encontrado');
                }

                const modalidad = oferta.modalidad || '-';
                const modalidadElement = document.getElementById('detalle-modalidad');
                if (modalidadElement) {
                    modalidadElement.textContent = modalidad;
                    modalidadElement.className = `badge modalidad-${modalidad}`;
                    console.log('? Modalidad asignada:', modalidad);
                } else {
                    console.error('? Elemento detalle-modalidad no encontrado');
                }

                const estado = oferta.estado || '-';
                const estadoElement = document.getElementById('detalle-estado');
                if (estadoElement) {
                    estadoElement.textContent = estado;
                    estadoElement.className = `badge estado-${estado}`;
                    console.log('? Estado asignado:', estado);
                } else {
                    console.error('? Elemento detalle-estado no encontrado');
                }

                // Descripción
                const descripcion = oferta.descripcion || 'Sin descripción disponible';
                const descripcionElement = document.getElementById('detalle-descripcion');
                if (descripcionElement) {
                    descripcionElement.innerHTML = descripcion;
                    console.log('? Descripción asignada:', descripcion ? 'Disponible' : 'No disponible');
                } else {
                    console.error('? Elemento detalle-descripcion no encontrado');
                }

                // Usar costoInscripcion que es el campo real de CursoDetalle
                const costo = oferta.costoInscripcion || oferta.costo || 0;
                const costoElement = document.getElementById('detalle-costo');
                if (costoElement) {
                    costoElement.textContent = costo ? `$${costo.toLocaleString()}` : '-';
                    console.log('? Costo asignado:', costo);
                } else {
                    console.error('? Elemento detalle-costo no encontrado');
                }

                // Fechas - usar los nombres exactos de LocalDate
                const fechaInicio = formatearFecha(oferta.fechaInicio) || '-';
                const fechaInicioElement = document.getElementById('detalle-fecha-inicio');
                if (fechaInicioElement) {
                    fechaInicioElement.textContent = fechaInicio;
                    console.log('? Fecha inicio asignada:', fechaInicio);
                } else {
                    console.error('? Elemento detalle-fecha-inicio no encontrado');
                }

                const fechaFin = formatearFecha(oferta.fechaFin) || '-';
                const fechaFinElement = document.getElementById('detalle-fecha-fin');
                if (fechaFinElement) {
                    fechaFinElement.textContent = fechaFin;
                    console.log('? Fecha fin asignada:', fechaFin);
                } else {
                    console.error('? Elemento detalle-fecha-fin no encontrado');
                }

                // Cupos
                const cupos = (oferta.cupos && oferta.cupos !== 2147483647) ? oferta.cupos : 'Sin límite';
                const capacidadMaxElement = document.getElementById('detalle-capacidad-max');
                if (capacidadMaxElement) {
                    capacidadMaxElement.textContent = cupos;
                    console.log('? Cupos asignados:', cupos);
                } else {
                    console.error('? Elemento detalle-capacidad-max no encontrado');
                }

                // Horarios
                const horariosContainer = document.getElementById('detalle-horarios-container');
                if (horariosContainer) {
                    if (oferta.horarios && oferta.horarios.length > 0) {
                        horariosContainer.innerHTML = oferta.horarios.map(h => {
                            // Formatear hora (cortar segundos si existen)
                            const inicio = h.horaInicio ? h.horaInicio.toString().substring(0, 5) : '??:??';
                            const fin = h.horaFin ? h.horaFin.toString().substring(0, 5) : '??:??';
                            return `<span class="badge" style="background-color: #6c757d; color: white; padding: 0.5em 0.8em; border-radius: 20px; font-weight: normal; font-size: 0.9em; display: inline-flex; align-items: center; gap: 0.4rem;">
                                    <i class="fas fa-calendar-day"></i> ${h.dia} 
                                    <i class="fas fa-clock" style="margin-left: 0.3rem;"></i> ${inicio} - ${fin}
                                 </span>`;
                        }).join('');
                        console.log('? Horarios asignados:', oferta.horarios.length);
                    } else {
                        horariosContainer.innerHTML = '<span class="text-muted">Sin horarios definidos</span>';
                    }
                }

                // Categorías
                const categoriasContainer = document.getElementById('detalle-categorias');
                if (categoriasContainer) {
                    if (oferta.categorias && oferta.categorias.length > 0) {
                        categoriasContainer.innerHTML = oferta.categorias.map(cat => {
                            const nombre = typeof cat === 'object' ? (cat.nombre || cat) : cat;
                            return `<span class="categoria-chip">${nombre}</span>`;
                        }).join('');
                    } else {
                        categoriasContainer.innerHTML = '<span class="text-muted">Sin categorías</span>';
                    }
                }

                // Imagen
                const imagenElement = document.getElementById('detalle-imagen');
                const sinImagenElement = document.getElementById('detalle-sin-imagen');
                if (imagenElement && sinImagenElement) {
                    const imagenSrc = oferta.imagenUrl || oferta.imagen;
                    if (imagenSrc) {
                        imagenElement.src = imagenSrc;
                        imagenElement.style.display = 'block';
                        sinImagenElement.style.display = 'none';
                    } else {
                        imagenElement.style.display = 'none';
                        sinImagenElement.style.display = 'flex';
                    }
                }

                // Certificación
                const certificado = oferta.certificado;
                const seccionCertificacion = document.getElementById('seccion-certificacion');
                if (seccionCertificacion) {
                    if (certificado === true || certificado === 'true' || certificado === 'Sí') {
                        seccionCertificacion.style.display = 'block';
                        const certificadoElement = document.getElementById('detalle-otorga-certificado');
                        if (certificadoElement) certificadoElement.textContent = 'Sí';
                    } else {
                        seccionCertificacion.style.display = 'none';
                    }
                }

                // Mostrar datos específicos según el tipo
                mostrarDatosEspecificosPorTipo(oferta, tipo);

                // Configurar botón de estado
                configurarBotonEstado(estado);

                console.log('? Carga de datos en modal completada');
            }

            // Función para mostrar datos específicos según el tipo
            function mostrarDatosEspecificosPorTipo(oferta, tipo) {
                // Ocultar todas las secciones específicas por tipo
                document.querySelectorAll('.tipo-especifico').forEach(seccion => {
                    seccion.style.display = 'none';
                });

                switch (tipo) {
                    case 'CURSO':
                        mostrarDatosCurso(oferta);
                        break;
                    case 'FORMACION':
                        mostrarDatosFormacion(oferta);
                        break;
                    case 'CHARLA':
                        mostrarDatosCharla(oferta);
                        break;
                    case 'SEMINARIO':
                        mostrarDatosSeminario(oferta);
                        break;
                    default:
                        console.log('?? Tipo de oferta no reconocido:', tipo);
                }
            }

            // Funciones para mostrar datos específicos por tipo
            function mostrarDatosCurso(oferta) {
                const seccion = document.getElementById('datos-curso');
                seccion.style.display = 'block';

                // Temario
                document.getElementById('detalle-curso-temario').textContent = oferta.temario || 'Sin temario definido';

                // Docentes a Cargo
                const docentesContainer = document.getElementById('detalle-curso-docentes');
                if (oferta.docentes && oferta.docentes.length > 0) {
                    docentesContainer.innerHTML = oferta.docentes.map(docente => `
                    <div class="docente-item">
                        <i class="fas fa-user-tie"></i>
                        <div class="docente-info">
                            <div class="docente-nombre">${docente.nombre} ${docente.apellido || ''}</div>
                            ${docente.matricula ? `<div class="docente-especialidad">Matrícula: ${docente.matricula}</div>` : ''}
                        </div>
                    </div>
                `).join('');
                } else {
                    docentesContainer.innerHTML = '<span class="text-muted">Sin docentes asignados</span>';
                }

                console.log('?? Datos de curso cargados');
            }

            function mostrarDatosFormacion(oferta) {
                const seccion = document.getElementById('datos-formacion');
                seccion.style.display = 'block';

                // Plan de Formación
                document.getElementById('detalle-formacion-plan').textContent = oferta.plan || 'Sin plan definido';

                // Docentes a Cargo
                const docentesContainer = document.getElementById('detalle-formacion-docentes');
                if (oferta.docentes && oferta.docentes.length > 0) {
                    docentesContainer.innerHTML = oferta.docentes.map(docente => `
                    <div class="docente-item">
                        <i class="fas fa-user-tie"></i>
                        <div class="docente-info">
                            <div class="docente-nombre">${docente.nombre} ${docente.apellido || ''}</div>
                            ${docente.matricula ? `<div class="docente-especialidad">Matrícula: ${docente.matricula}</div>` : ''}
                        </div>
                    </div>
                `).join('');
                } else {
                    docentesContainer.innerHTML = '<span class="text-muted">Sin docentes asignados</span>';
                }

                console.log('?? Datos de formación cargados');
            }

            function mostrarDatosCharla(oferta) {
                const seccion = document.getElementById('datos-charla');
                seccion.style.display = 'block';

                // Lugar
                document.getElementById('detalle-charla-lugar').textContent = oferta.lugar || '-';

                // Enlace
                const enlaceSpan = document.getElementById('detalle-charla-enlace');
                if (oferta.enlace) {
                    enlaceSpan.innerHTML = `<a href="${oferta.enlace}" target="_blank">${oferta.enlace}</a>`;
                } else {
                    enlaceSpan.textContent = '-';
                }

                // Público Objetivo
                document.getElementById('detalle-charla-publico').textContent = oferta.publicoObjetivo || 'No especificado';

                // Disertantes
                const disertantesContainer = document.getElementById('detalle-charla-disertantes');
                if (oferta.disertantes && oferta.disertantes.length > 0) {
                    disertantesContainer.innerHTML = oferta.disertantes.map(disertante => `
                    <div class="disertante-item">
                        <i class="fas fa-microphone"></i>
                        <div class="disertante-info">
                            <div class="disertante-nombre">${disertante}</div>
                        </div>
                    </div>
                `).join('');
                } else {
                    disertantesContainer.innerHTML = '<span class="text-muted">Sin disertantes asignados</span>';
                }

                console.log('?? Datos de charla cargados');
            }

            function mostrarDatosSeminario(oferta) {
                const seccion = document.getElementById('datos-seminario');
                seccion.style.display = 'block';

                // Lugar
                document.getElementById('detalle-seminario-lugar').textContent = oferta.lugar || '-';

                // Enlace
                const enlaceSpan = document.getElementById('detalle-seminario-enlace');
                if (oferta.enlace) {
                    enlaceSpan.innerHTML = `<a href="${oferta.enlace}" target="_blank">${oferta.enlace}</a>`;
                } else {
                    enlaceSpan.textContent = '-';
                }

                // Público Objetivo
                document.getElementById('detalle-seminario-publico').textContent = oferta.publicoObjetivo || 'No especificado';

                // Disertantes
                const disertantesContainer = document.getElementById('detalle-seminario-disertantes');
                if (oferta.disertantes && oferta.disertantes.length > 0) {
                    disertantesContainer.innerHTML = oferta.disertantes.map(disertante => `
                    <div class="disertante-item">
                        <i class="fas fa-chalkboard-teacher"></i>
                        <div class="disertante-info">
                            <div class="disertante-nombre">${disertante}</div>
                        </div>
                    </div>
                `).join('');
            } else {
                disertantesContainer.innerHTML = '<span class="text-muted">Sin disertantes asignados</span>';
            }
            
            console.log('??? Datos de seminario cargados');
        }
        
        // Función para configurar botón de estado
        function configurarBotonEstado(estado) {
            const btnCambiarEstado = document.getElementById('btn-cambiar-estado');
            const iconEstado = document.getElementById('icon-estado');
            const textEstado = document.getElementById('text-estado');
            
            console.log('?? Configurando botón para estado:', estado);
            
            if (btnCambiarEstado && iconEstado && textEstado) {
                // Resetear estado del botón
                btnCambiarEstado.disabled = false;
                
                if (estado === 'ACTIVA' || estado === 'ENCURSO') {
                    btnCambiarEstado.className = 'btn-danger';  // Rojo para dar de baja
                    iconEstado.className = 'fas fa-times-circle';
                    textEstado.textContent = 'Dar de Baja';
                    console.log('?? Botón configurado para dar de baja');
                } else if (estado === 'DE_BAJA') {
                    btnCambiarEstado.className = 'btn-success';  // Verde para dar de alta
                    iconEstado.className = 'fas fa-check-circle';
                    textEstado.textContent = 'Dar de Alta';
                    console.log('?? Botón configurado para dar de alta');
                } else if (estado === 'FINALIZADA') {
                    btnCambiarEstado.className = 'btn-secondary';
                    iconEstado.className = 'fas fa-flag-checkered';
                    textEstado.textContent = 'Finalizada';
                    btnCambiarEstado.disabled = true;
                    console.log('?? Botón configurado como finalizada (deshabilitado)');
                } else {
                    btnCambiarEstado.className = 'btn-secondary';
                    iconEstado.className = 'fas fa-question-circle';
                    textEstado.textContent = estado;
                    console.log('?? Estado desconocido:', estado);
                }
            } else {
                console.error('? No se pudieron encontrar los elementos del botón de estado');
                console.log('Elementos encontrados:', {
                    btnCambiarEstado: !!btnCambiarEstado,
                    iconEstado: !!iconEstado,
                    textEstado: !!textEstado
                });
            }
        }
        
        // Función global para cerrar modal
        window.cerrarModalDetalle = function() {
            const modal = document.getElementById('modalDetalleOferta');
            if (modal) {
                modal.style.display = 'none';
                document.body.style.overflow = 'auto';
                ofertaActualId = null;
                console.log('?? Modal cerrado');
            }
        };
        
        // Función para cambiar estado de la oferta
        window.cambiarEstadoOferta = function() {
            if (!ofertaActualId) {
                const hiddenId = getValueById('idOfertaModificar');
                if (hiddenId) {
                    ofertaActualId = hiddenId;
                }
            }
            if (!ofertaActualId) {
                console.error('? No hay oferta seleccionada para cambiar estado');
                mostrarNotificacion('Error: No hay oferta seleccionada', 'error');
                return;
            }
            
            console.log('?? Cambiando estado de oferta con ID:', ofertaActualId);
            
            // Obtener elementos del DOM
            const btnCambiarEstado = document.getElementById('btn-cambiar-estado');
            const textEstado = document.getElementById('text-estado');
            
            // Verificar que el botón existe
            if (!btnCambiarEstado) {
                console.error('? Botón cambiar estado no encontrado');
                mostrarNotificacion('Error: Botón de estado no encontrado', 'error');
                return;
            }

                // Determinar acción y mensaje
                let tituloAccion = 'Cambiar Estado';
                let mensajeConfirmacion = '¿Está seguro de que desea cambiar el estado de esta oferta?';

                if (textEstado) {
                    const textoActual = textEstado.textContent.trim();
                    if (textoActual === 'Dar de Baja') {
                        tituloAccion = 'Dar de Baja';
                        mensajeConfirmacion = '¿Está seguro de que desea dar de baja esta oferta? No estará disponible para nuevas inscripciones.';
                    } else if (textoActual === 'Dar de Alta') {
                        tituloAccion = 'Activar Oferta';
                        mensajeConfirmacion = '¿Está seguro de que desea reactivar esta oferta?';
                    }
                }

                // Mostrar modal de confirmación
                ModalConfirmacion.show(
                    tituloAccion,
                    mensajeConfirmacion,
                    () => {
                        ejecutarCambioEstado(btnCambiarEstado);
                    }
                );
            };

            function ejecutarCambioEstado(btnCambiarEstado) {
                // Deshabilitar botón durante la operación
                const textoOriginal = btnCambiarEstado.innerHTML;
                btnCambiarEstado.disabled = true;
                btnCambiarEstado.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Procesando...';

                const restaurarContenidoBoton = () => {
                    btnCambiarEstado.innerHTML = textoOriginal;
                };

                // Realizar petición al servidor
                const token = obtenerCsrfToken();
                const headerName = obtenerCsrfHeaderName();

                const headers = {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                };

                if (token && headerName) {
                    headers[headerName] = token;
                }

                fetch(`/admin/ofertas/cambiar-estado/${ofertaActualId}`, {
                    method: 'POST',
                    headers: headers,
                    credentials: 'same-origin'
                })
                    .then(response => {
                        console.log('?? Respuesta del servidor:', response.status);
                        // No lanzar error aquí, siempre procesar la respuesta JSON
                        return response.json();
                    })
                    .then(data => {
                        console.log('? Respuesta procesada:', data);
                        console.log('?? Detalles de la respuesta:', {
                            success: data.success,
                            message: data.message,
                            motivo: data.motivo,
                            nuevoEstado: data.nuevoEstado
                        });

                        if (data.success) {
                            console.log('?? Estado cambiado exitosamente a:', data.nuevoEstado);

                            restaurarContenidoBoton();

                            const iconEstadoActual = document.getElementById('icon-estado');
                            const textEstadoActual = document.getElementById('text-estado');

                            if (!iconEstadoActual || !textEstadoActual) {
                                console.warn('?? No se pudieron recuperar los elementos del botón tras restaurar su contenido');
                            }

                            // Actualizar interfaz del botón usando la función centralizada
                            configurarBotonEstado(data.nuevoEstado);

                            // Actualizar badge de estado en el modal
                            const estadoElement = document.getElementById('detalle-estado');
                            if (estadoElement) {
                                estadoElement.textContent = data.nuevoEstado;
                                estadoElement.className = `badge estado-${data.nuevoEstado}`;
                            }

                            // Mostrar notificación de éxito
                            let mensaje = 'Estado actualizado correctamente';
                            if (data.nuevoEstado === 'ACTIVA' || data.nuevoEstado === 'ENCURSO') {
                                mensaje = 'Oferta activada correctamente';
                            } else if (data.nuevoEstado === 'DE_BAJA') {
                                mensaje = 'Oferta dada de baja correctamente';
                            }
                            mostrarNotificacion(mensaje, 'success');

                            console.log('?? Estado actualizado en la interfaz');

                            // Actualizar tabla de ofertas para reflejar el cambio
                            actualizarTablaOfertas();
                        } else {
                            console.error('? Error del servidor:', data.message || 'Error desconocido');
                            console.log('?? Motivo del error:', data.motivo);
                            console.log('?? Datos completos del error:', data);

                            // Crear mensaje detallado para alert
                            let mensajeDetallado = data.message || 'Error al cambiar estado';
                            if (data.motivo) {
                                switch (data.motivo) {
                                    case 'VALIDACION_BAJA':
                                        mensajeDetallado += '\n\nMotivo: La oferta tiene inscripciones activas';
                                        mensajeDetallado += '\n\nSugerencias:';
                                        mensajeDetallado += '\n- Cancela las inscripciones activas primero';
                                        mensajeDetallado += '\n- O edita la oferta para cambiar las fechas';
                                        mensajeDetallado += '\n- Espera a que termine la oferta naturalmente';
                                        break;
                                    case 'ESTADO_FINAL':
                                        mensajeDetallado += '\n\nMotivo: La fecha de finalización ya pasó';
                                        mensajeDetallado += '\n\nSugerencias:';
                                        mensajeDetallado += '\n- Las ofertas finalizadas no pueden cambiar de estado';
                                        mensajeDetallado += '\n- Crea una nueva oferta similar si necesitas reactivarla';
                                        break;
                                    case 'ERROR_SERVIDOR':
                                        mensajeDetallado += '\n\nMotivo: Error interno del servidor';
                                        mensajeDetallado += '\n\nSugerencias:';
                                        mensajeDetallado += '\n- Intenta nuevamente en unos segundos';
                                        mensajeDetallado += '\n- Si persiste, contacta al administrador';
                                        break;
                                    default:
                                        if (data.motivo !== 'ERROR_GENERAL') {
                                            mensajeDetallado += '\n\nMotivo: ' + data.motivo;
                                        }
                                }
                            }

                            // Mostrar el mensaje detallado
                            mostrarNotificacion(mensajeDetallado, 'error');

                            // Restaurar botón original
                            restaurarContenidoBoton();
                        }
                    })
                    .catch(error => {
                        console.error('? Error en la petición:', error);

                        // Mensaje de error más simple
                        mostrarNotificacion('Error de conexión al cambiar el estado de la oferta. Intenta nuevamente en unos segundos.', 'error');

                        // Restaurar botón original
                        restaurarContenidoBoton();
                    })
                    .finally(() => {
                        // Siempre rehabilitar el botón
                        btnCambiarEstado.disabled = false;
                    });
            };

            // Función para mostrar modal de error con motivo específico
            window.mostrarModalError = function (mensaje, tipoError) {
                console.log('?? Mostrando modal de error:', mensaje, tipoError);

                const modal = document.getElementById('modalErrorEstado');
                const errorIcon = document.getElementById('error-icon');
                const errorTitle = document.getElementById('error-title');
                const errorDescription = document.getElementById('error-description');
                const errorSuggestions = document.getElementById('error-suggestions');
                const errorSuggestionsList = document.getElementById('error-suggestions-list');

                if (!modal || !errorTitle || !errorDescription) {
                    console.error('? Elementos del modal de error no encontrados');
                    // Fallback a alert simple
                    mostrarNotificacion('Error: ' + mensaje + '. Motivo: ' + tipoError, 'error');
                    return;
                }

                // Configurar contenido según el tipo de error
                let mensajeFinal = mensaje;
                switch (tipoError) {
                    case 'VALIDACION_BAJA':
                        errorIcon.className = 'fas fa-users text-warning';
                        errorTitle.textContent = 'No se puede dar de baja la oferta';
                        errorDescription.textContent = mensaje;
                        mensajeFinal = mensaje + ' (La oferta tiene inscripciones activas)';

                        // Mostrar sugerencias específicas
                        if (errorSuggestions) {
                            errorSuggestions.style.display = 'block';
                            errorSuggestionsList.innerHTML = `
                            <li><i class="fas fa-user-minus"></i> Cancela las inscripciones activas primero</li>
                            <li><i class="fas fa-edit"></i> O edita la oferta para cambiar las fechas</li>
                            <li><i class="fas fa-clock"></i> Espera a que termine la oferta naturalmente</li>
                        `;
                        }
                        break;

                    case 'ESTADO_FINAL':
                        errorIcon.className = 'fas fa-flag text-danger';
                        errorTitle.textContent = 'Oferta en estado final';
                        errorDescription.textContent = mensaje;
                        mensajeFinal = mensaje + ' (La fecha de finalización ya pasó)';

                        if (errorSuggestions) {
                            errorSuggestions.style.display = 'block';
                            errorSuggestionsList.innerHTML = `
                            <li><i class="fas fa-info-circle"></i> Las ofertas finalizadas o canceladas no pueden cambiar de estado</li>
                            <li><i class="fas fa-plus"></i> Crea una nueva oferta similar si necesitas reactivarla</li>
                        `;
                        }
                        break;

                    case 'ERROR_SERVIDOR':
                        errorIcon.className = 'fas fa-server text-danger';
                        errorTitle.textContent = 'Error del sistema';
                        errorDescription.textContent = mensaje;
                        mensajeFinal = mensaje + ' (Error interno del servidor)';

                        if (errorSuggestions) {
                            errorSuggestions.style.display = 'block';
                            errorSuggestionsList.innerHTML = `
                            <li><i class="fas fa-refresh"></i> Intenta nuevamente en unos segundos</li>
                            <li><i class="fas fa-phone"></i> Si el problema persiste, contacta al administrador</li>
                        `;
                        }
                        break;

                    default:
                        errorIcon.className = 'fas fa-exclamation-triangle text-warning';
                        errorTitle.textContent = 'Error';
                        errorDescription.textContent = mensaje;
                        mensajeFinal = mensaje;
                        if (errorSuggestions) {
                            errorSuggestions.style.display = 'none';
                        }
                        break;
                }

                // Intentar mostrar el modal, usar alert como fallback
                try {
                    modal.style.display = 'flex';
                    document.body.style.overflow = 'hidden';
                    console.log('? Modal de error mostrado');
                } catch (e) {
                    console.error('? Error mostrando modal, usando alert:', e);
                    mostrarNotificacion('Error: ' + mensajeFinal, 'error');
                }
            };

            // Función para cerrar modal de error
            window.cerrarModalError = function () {
                const modal = document.getElementById('modalErrorEstado');
                if (modal) {
                    modal.style.display = 'none';
                    document.body.style.overflow = 'auto';
                    console.log('?? Modal de error cerrado');
                }
            };

            // Función para cerrar modal
            function cerrarModal() {
                const modal = document.getElementById('modalDetalleOferta');
                if (modal) {
                    modal.style.display = 'none';
                    document.body.style.overflow = 'auto'; // Restaurar scroll
                    console.log('?? Modal cerrado');
                }
            }

            // Función auxiliar para formatear fechas
            function formatearFecha(fecha) {
                if (!fecha) return null;
                try {
                    if (Array.isArray(fecha) && fecha.length >= 3) {
                        // Si es array [año, mes, día] - formato que devuelve LocalDate en JSON
                        const [año, mes, dia] = fecha;
                        const fechaObj = new Date(año, mes - 1, dia); // mes - 1 porque Date usa índice 0
                        return fechaObj.toLocaleDateString('es-ES', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric'
                        });
                    } else if (typeof fecha === 'string') {
                        // Si es string, asumimos formato ISO (yyyy-mm-dd)
                        return new Date(fecha + 'T00:00:00').toLocaleDateString('es-ES', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric'
                        });
                    } else if (fecha instanceof Date) {
                        return fecha.toLocaleDateString('es-ES', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric'
                        });
                    }
                    return null;
                } catch (e) {
                    console.error('Error formateando fecha:', e, fecha);
                    return null;
                }
            }

            // Cerrar modal al hacer clic fuera
            window.onclick = function (event) {
                const modal = document.getElementById('modalDetalleOferta');
                if (event.target === modal) {
                    cerrarModal();
                }
            };

            console.log('? Funciones del modal de detalle inicializadas');

            // ================= FUNCI�N PARA MODIFICAR OFERTA =================

            // Variable global para rastrear si está en modo edición
            let modoEdicion = false;
            let idOfertaModificar = null;
            let estadoOfertaEnEdicion = null;

            // Variables para manejar eliminaciones
            let ofertaEliminarId = null;
            let ofertaEliminarNombre = '';

            function obtenerCsrfToken() {
                const metaToken = document.querySelector('meta[name="_csrf"]');
                if (metaToken) {
                    return metaToken.getAttribute('content');
                }
                const inputToken = document.querySelector('input[name="_csrf"]');
                return inputToken ? inputToken.value : null;
            }

            function obtenerCsrfHeaderName() {
                const metaHeader = document.querySelector('meta[name="_csrf_header"]');
                if (metaHeader) {
                    return metaHeader.getAttribute('content');
                }
                return 'X-CSRF-TOKEN';
            }

            function mostrarModalConfirmacion() {
                const modal = document.getElementById('confirmDeleteModal');
                if (modal) {
                    modal.style.display = 'flex';
                }
            }

            window.cerrarModalConfirmacion = function () {
                const modal = document.getElementById('confirmDeleteModal');
                if (modal) {
                    modal.style.display = 'none';
                }
                ofertaEliminarId = null;
                ofertaEliminarNombre = '';
            };

            window.cerrarModalVerOferta = function () {
                const modal = document.getElementById('modal-ver-oferta');
                if (modal) {
                    modal.style.display = 'none';
                }
            };

            window.eliminarOferta = function (id, nombre) {
                if (!id) {
                    console.error('? ID inválido para eliminar oferta');
                    mostrarNotificacion('No se pudo identificar la oferta a eliminar.', 'error');
                    return;
                }

                ModalConfirmacion.show(
                    'Eliminar Oferta',
                    `¿Estás seguro de que deseas eliminar la oferta académica "${nombre || 'seleccionada'}"?`,
                    () => {
                        ofertaEliminarId = id;
                        ejecutarEliminacion();
                    }
                );
            };

            window.eliminarOfertaDesdeModal = function () {
                if (!ofertaActualId) {
                    console.error('? No hay oferta seleccionada en el modal de detalle');
                    mostrarNotificacion('No se encontró la oferta seleccionada para eliminar.', 'error');
                    return;
                }

                const nombreDetalle = document.getElementById('detalle-nombre');
                const nombre = nombreDetalle ? nombreDetalle.textContent : ofertaEliminarNombre;
                eliminarOferta(ofertaActualId, nombre);
            };

            window.ejecutarEliminacion = function () {
                if (!ofertaEliminarId) {
                    console.error('? No hay oferta pendiente de eliminación');
                    mostrarNotificacion('No se pudo procesar la eliminación.', 'error');
                    return;
                }

                const idAEliminar = ofertaEliminarId;
                const token = obtenerCsrfToken();
                const headerName = obtenerCsrfHeaderName();

                const headers = {
                    'X-Requested-With': 'XMLHttpRequest'
                };

                if (token) {
                    headers[headerName] = token;
                }

                const btnConfirmar = document.getElementById('btn-confirmar-eliminar');
                const textoOriginal = btnConfirmar ? btnConfirmar.innerHTML : '';

                if (btnConfirmar) {
                    btnConfirmar.disabled = true;
                    btnConfirmar.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Eliminando...';
                }

                fetch(`/admin/ofertas/eliminar/${idAEliminar}`, {
                    method: 'POST',
                    headers,
                    credentials: 'same-origin'
                })
                    .then(response => response.text().then(texto => ({
                        ok: response.ok,
                        status: response.status,
                        body: texto
                    })))
                    .then(({ ok, status, body }) => {
                        let data = {};
                        if (body) {
                            try {
                                data = JSON.parse(body);
                            } catch (err) {
                                console.warn('?? No se pudo parsear la respuesta JSON:', err);
                            }
                        }

                        if (!ok) {
                            const mensaje = data.message || `Error al eliminar la oferta (código ${status})`;
                            mostrarNotificacion(`? ${mensaje}`, 'error');
                            throw new Error(mensaje);
                        }

                        mostrarNotificacion(data.message || 'Oferta eliminada correctamente', 'success');

                        cerrarModalConfirmacion();
                        cerrarModalDetalle();
                        cerrarModalVerOferta();

                        if (modoEdicion && idOfertaModificar && String(idOfertaModificar) === String(idAEliminar)) {
                            limpiarFormularioCompleto();
                            resetearModoEdicion();
                            cerrarFormulario();
                        }

                        if (ofertaActualId && String(ofertaActualId) === String(idAEliminar)) {
                            ofertaActualId = null;
                        }

                        ofertaEliminarId = null;
                        ofertaEliminarNombre = '';

                        actualizarTablaOfertas();
                    })
                    .catch(error => {
                        console.error('? Error durante la eliminación:', error);
                        mostrarNotificacion('? Error al eliminar la oferta. Inténtalo nuevamente.', 'error');
                    })
                    .finally(() => {
                        if (btnConfirmar) {
                            btnConfirmar.disabled = false;
                            btnConfirmar.innerHTML = textoOriginal;
                        }
                    });
            };

            /**
             * Función para modificar una oferta académica
             */
            window.modificarOferta = function (idOferta, nombreOferta) {
                console.log('?? Iniciando modificación de oferta:', idOferta, nombreOferta);

                // Primero obtenemos los datos de la oferta para verificar su estado y fechas
                fetch(`/admin/ofertas/${idOferta}`)
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            const oferta = data.oferta;
                            const fechaInicio = new Date(oferta.fechaInicio + 'T00:00:00');
                            const hoy = new Date();
                            hoy.setHours(0, 0, 0, 0); // Resetear horas para comparar solo fechas

                            let mensajeConfirmacion = `¿Estás seguro que quieres modificar la oferta "${nombreOferta}"?`;

                            // Lógica de advertencia solicitada
                            if (oferta.estado === 'ACTIVA' && fechaInicio < hoy) {
                                mensajeConfirmacion = `?? ADVERTENCIA: Esta oferta está ACTIVA y su fecha de inicio (${oferta.fechaInicio}) ya ha pasado.\n\n¿Estás seguro que deseas modificar información de una oferta que ya está en curso?`;
                            } else if (oferta.estado === 'DE_BAJA') {
                                mensajeConfirmacion = `?? ADVERTENCIA: Estás a punto de modificar una oferta que está DE BAJA.\n\n¿Estás seguro que deseas continuar?`;
                            }

                            // Mostrar confirmación
                            ModalConfirmacion.show(
                                'Modificar Oferta',
                                mensajeConfirmacion,
                                () => {
                                    console.log('? Usuario confirmó modificación');

                                    // Activar modo edición
                                    modoEdicion = true;
                                    idOfertaModificar = idOferta;
                                    estadoOfertaEnEdicion = oferta.estado;
                                    console.log('?? Modo edición activado. Estado original:', estadoOfertaEnEdicion);

                                    // Cambiar el texto del botón submit
                                    const btnSubmit = document.getElementById('btn-submit-form');
                                    const btnSubmitText = document.getElementById('btn-submit-text');
                                    if (btnSubmit && btnSubmitText) {
                                        btnSubmitText.textContent = 'Guardar Modificaciones';
                                        btnSubmit.classList.add('btn-update');
                                    }

                                    // Poblar el formulario con los datos ya obtenidos
                                    poblarFormulario(oferta);

                                    // Mostrar el formulario
                                    mostrarFormulario();
                                }
                            );
                        } else {
                            mostrarNotificacion('Error al obtener los datos de la oferta para verificar.', 'error');
                        }
                    })
                    .catch(error => {
                        console.error('? Error al verificar oferta:', error);
                        mostrarNotificacion('Error de conexión al verificar la oferta.', 'error');
                    });
            };

            /**
             * Función para cargar los datos de una oferta en el formulario
             */
            function cargarDatosOferta(idOferta) {
                console.log('?? Cargando datos de la oferta:', idOferta);

                fetch(`/admin/ofertas/${idOferta}`)
                    .then(response => response.json())
                    .then(data => {
                        console.log('?? Respuesta del servidor:', data);

                        if (data.success) {
                            poblarFormulario(data.oferta);
                        } else {
                            console.error('? Error al cargar datos:', data.message);
                            mostrarNotificacion('Error al cargar los datos de la oferta: ' + data.message, 'error');
                        }
                    })
                    .catch(error => {
                        console.error('? Error en la petición:', error);
                        mostrarNotificacion('Error de conexión al cargar los datos de la oferta', 'error');
                    });
            }

            /**
             * Función para poblar el formulario con los datos de la oferta
             */
            function poblarFormulario(oferta) {
                console.log('?? Poblando formulario con datos:', oferta);

                try {
                    const normalizarFecha = (valor) => {
                        if (!valor) return '';
                        const texto = valor.toString();
                        return texto.includes('T') ? texto.split('T')[0] : texto;
                    };

                    // Establecer ID de la oferta para modificar
                    if (document.getElementById('idOfertaModificar')) {
                        document.getElementById('idOfertaModificar').value = oferta.id || oferta.idOferta || '';
                    }

                    // Datos básicos (usando los IDs reales del formulario)
                    if (document.getElementById('nombre')) {
                        document.getElementById('nombre').value = oferta.nombre || '';
                    }
                    if (document.getElementById('descripcion')) {
                        document.getElementById('descripcion').value = oferta.descripcion || '';
                    }
                    if (document.getElementById('tipoOferta')) {
                        document.getElementById('tipoOferta').value = oferta.tipo || oferta.tipoOferta || '';
                    }
                    if (document.getElementById('modalidad')) {
                        document.getElementById('modalidad').value = oferta.modalidad || '';
                    }
                    if (document.getElementById('fechaInicio')) {
                        document.getElementById('fechaInicio').value = normalizarFecha(oferta.fechaInicio);
                    }
                    if (document.getElementById('fechaFin')) {
                        const fechaInicioInput = document.getElementById('fechaInicio');
                        const fechaFinInput = document.getElementById('fechaFin');
                        const fechaInicioVal = fechaInicioInput ? fechaInicioInput.value : '';
                        fechaFinInput.value = normalizarFecha(oferta.fechaFin);
                        if (fechaInicioVal) {
                            fechaFinInput.disabled = false;
                            fechaFinInput.setAttribute('min', fechaInicioVal);
                        }
                    }
                    if (document.getElementById('cupos')) {
                        const cuposVal = oferta.cupos;
                        // Si es MAX_VALUE (2147483647), dejar vacío para indicar "Sin límite"
                        if (cuposVal === 2147483647) {
                            document.getElementById('cupos').value = '';
                        } else {
                            document.getElementById('cupos').value = cuposVal || '';
                        }
                    }

                    // Certificado - manejar diferentes formatos
                    const certificadoField = document.getElementById('otorgaCertificado');
                    if (certificadoField && oferta.certificado !== undefined) {
                        if (typeof oferta.certificado === 'string') {
                            certificadoField.checked = oferta.certificado === 'true' || oferta.certificado === 'SI';
                        } else {
                            certificadoField.checked = oferta.certificado === true;
                        }
                    }

                    // Costo de inscripción general
                    if (document.getElementById('costoInscripcion')) {
                        document.getElementById('costoInscripcion').value = oferta.costoInscripcion || '';
                    }

                    // Imagen de presentación
                    const imagePreview = document.getElementById('image-preview');
                    if (imagePreview) {
                        if (oferta.imagenUrl) {
                            // Asegurar que la ruta sea correcta si viene relativa
                            const src = oferta.imagenUrl.startsWith('http') || oferta.imagenUrl.startsWith('/')
                                ? oferta.imagenUrl
                                : '/' + oferta.imagenUrl;
                            imagePreview.src = src;
                            imagePreview.style.display = 'block';
                        } else {
                            imagePreview.style.display = 'none';
                            imagePreview.src = '';
                        }
                    }

                    // Ubicación y Enlace para todos los tipos
                    if (document.getElementById('lugar')) {
                        document.getElementById('lugar').value = oferta.lugar || '';
                    }
                    if (document.getElementById('enlace')) {
                        document.getElementById('enlace').value = oferta.enlace || '';
                    }

                    // Mostrar campos específicos según el tipo
                    const tipoOferta = oferta.tipo || oferta.tipoOferta;
                    mostrarCamposEspecificos(tipoOferta);

                    // Dar tiempo a que se muestren los campos antes de poblarlos
                    setTimeout(() => {
                        // Campos específicos según el tipo
                        if (tipoOferta === 'CURSO') {
                            console.log('?? Cargando datos de CURSO');
                            if (document.getElementById('temario')) {
                                document.getElementById('temario').value = oferta.temario || '';
                                console.log('? Temario cargado:', oferta.temario);
                            }
                            if (document.getElementById('costoCuota')) {
                                document.getElementById('costoCuota').value = oferta.costoCuota || '';
                            }
                            if (document.getElementById('nrCuotas')) {
                                document.getElementById('nrCuotas').value = oferta.nrCuotas || '';
                            }
                            if (document.getElementById('diaVencimiento')) {
                                document.getElementById('diaVencimiento').value = oferta.diaVencimiento || '';
                            }

                            // Fechas de inscripción para CURSO
                            if (document.getElementById('fechaInicioInscripcion')) {
                                document.getElementById('fechaInicioInscripcion').value = oferta.fechaInicioInscripcion || '';
                                console.log('? Fecha inicio inscripción cargada:', oferta.fechaInicioInscripcion);
                            }
                            if (document.getElementById('fechaFinInscripcion')) {
                                document.getElementById('fechaFinInscripcion').value = oferta.fechaFinInscripcion || '';
                                console.log('? Fecha fin inscripción cargada:', oferta.fechaFinInscripcion);
                            }

                            // Cargar docentes de CURSO
                            if (oferta.docentes && Array.isArray(oferta.docentes)) {
                                console.log('????? Cargando docentes de CURSO:', oferta.docentes.length);
                                docentesSeleccionados.curso = [];
                                const tablaCurso = document.getElementById('docentes-table');
                                if (tablaCurso) {
                                    const tbody = tablaCurso.querySelector('tbody');
                                    tbody.innerHTML = '';

                                    oferta.docentes.forEach(docente => {
                                        const id = docente.id;
                                        const nombre = `${docente.nombre} ${docente.apellido}`;
                                        docentesSeleccionados.curso.push({ id, nombre });

                                        const fila = document.createElement('tr');
                                        fila.innerHTML = `
                                        <td>${nombre}</td>
                                        <td>
                                            <button type="button" class="btn-remove" 
                                                    onclick="removerDocente('${id}', 'docentes-table', this)">
                                                <i class="fas fa-times"></i>
                                            </button>
                                        </td>
                                    `;
                                        tbody.appendChild(fila);
                                    });

                                    actualizarDocentesHidden('curso');
                                    console.log('? Docentes de CURSO cargados:', docentesSeleccionados.curso.length);
                                }
                            }

                        } else if (tipoOferta === 'FORMACION') {
                            console.log('?? Cargando datos de FORMACION');
                            if (document.getElementById('planFormacion')) {
                                document.getElementById('planFormacion').value = oferta.plan || '';
                                console.log('? Plan cargado:', oferta.plan);
                            }
                            if (document.getElementById('costoCuotaFormacion')) {
                                document.getElementById('costoCuotaFormacion').value = oferta.costoCuota || '';
                                console.log('? Costo cuota cargado:', oferta.costoCuota);
                            }
                            if (document.getElementById('nrCuotasFormacion')) {
                                document.getElementById('nrCuotasFormacion').value = oferta.nrCuotas || '';
                                console.log('? Nro cuotas cargado:', oferta.nrCuotas);
                            }
                            if (document.getElementById('diaVencimientoFormacion')) {
                                document.getElementById('diaVencimientoFormacion').value = oferta.diaVencimiento || '';
                                console.log('? Día vencimiento cargado:', oferta.diaVencimiento);
                            }

                            // Fechas de inscripción para FORMACION
                            if (document.getElementById('fechaInicioInscripcion')) {
                                document.getElementById('fechaInicioInscripcion').value = oferta.fechaInicioInscripcion || '';
                                console.log('? Fecha inicio inscripción cargada:', oferta.fechaInicioInscripcion);
                            }
                            if (document.getElementById('fechaFinInscripcion')) {
                                document.getElementById('fechaFinInscripcion').value = oferta.fechaFinInscripcion || '';
                                console.log('? Fecha fin inscripción cargada:', oferta.fechaFinInscripcion);
                            }

                            // Cargar docentes si existen
                            if (oferta.docentes && Array.isArray(oferta.docentes)) {
                                console.log('????? Cargando docentes:', oferta.docentes.length);
                                docentesSeleccionados.formacion = [];
                                const tablaFormacion = document.getElementById('docentes-table-formacion');
                                if (tablaFormacion) {
                                    const tbody = tablaFormacion.querySelector('tbody');
                                    tbody.innerHTML = '';

                                    oferta.docentes.forEach(docente => {
                                        const id = docente.id;
                                        const nombre = `${docente.nombre} ${docente.apellido}`;
                                        docentesSeleccionados.formacion.push({ id, nombre });

                                        const fila = document.createElement('tr');
                                        fila.innerHTML = `
                                        <td>${nombre}</td>
                                        <td>
                                            <button type="button" class="btn-remove" 
                                                    onclick="removerDocente('${id}', 'docentes-table-formacion', this)">
                                                <i class="fas fa-times"></i>
                                            </button>
                                        </td>
                                    `;
                                    tbody.appendChild(fila);
                                });
                                
                                actualizarDocentesHidden('formacion');
                                console.log('? Docentes cargados:', docentesSeleccionados.formacion.length);
                            }
                        }
                        
                    } else if (tipoOferta === 'CHARLA') {
                        console.log('?? Cargando datos de CHARLA');
                        
                        // Fecha de la charla
                        if (document.getElementById('fechaCharla')) {
                            document.getElementById('fechaCharla').value = oferta.fechaInicio || '';
                            console.log('? Fecha charla cargada:', oferta.fechaInicio);
                        }
                        
                        // Hora de inicio
                        if (document.getElementById('horaCharla') && oferta.horaInicio) {
                            const horaStr = oferta.horaInicio.toString().substring(0, 5);
                            document.getElementById('horaCharla').value = horaStr;
                            console.log('? Hora charla cargada:', horaStr);
                        }
                        
                        // Fechas de inscripción
                        if (document.getElementById('fechaInicioInscripcion')) {
                            document.getElementById('fechaInicioInscripcion').value = oferta.fechaInicioInscripcion || '';
                            console.log('? Fecha inicio inscripción cargada:', oferta.fechaInicioInscripcion);
                        }
                        if (document.getElementById('fechaFinInscripcion')) {
                            document.getElementById('fechaFinInscripcion').value = oferta.fechaFinInscripcion || '';
                            console.log('? Fecha fin inscripción cargada:', oferta.fechaFinInscripcion);
                        }
                        
                        if (document.getElementById('duracionEstimada')) {
                            document.getElementById('duracionEstimada').value = oferta.duracionEstimada || '';
                        }
                        if (document.getElementById('publicoObjetivo')) {
                            document.getElementById('publicoObjetivo').value = oferta.publicoObjetivo || '';
                            console.log('? Público objetivo cargado:', oferta.publicoObjetivo);
                        }
                        
                        // Cargar disertantes
                        if (oferta.disertantes && Array.isArray(oferta.disertantes)) {
                            // Limpiar disertantes de formato anidado [[...]]
                            disertantesCharla = oferta.disertantes.map(d => {
                                if (Array.isArray(d)) {
                                    return d[0]; // Extraer el primer elemento si es un array
                                }
                                return d;
                            }).filter(d => d); // Filtrar valores vacíos
                            actualizarDisertantesChips('charla');
                            console.log('? Disertantes charla cargados:', disertantesCharla);
                        }
                        
                    } else if (tipoOferta === 'SEMINARIO') {
                        console.log('??? Cargando datos de SEMINARIO');
                        
                        if (document.getElementById('numeroEncuentros')) {
                            document.getElementById('numeroEncuentros').value = oferta.numeroEncuentros || '';
                            console.log('? Número de encuentros cargado:', oferta.numeroEncuentros);
                        }
                        
                        // Hora de inicio
                        if (document.getElementById('horaSeminario') && oferta.horaInicio) {
                            const horaStr = oferta.horaInicio.toString().substring(0, 5);
                            document.getElementById('horaSeminario').value = horaStr;
                            console.log('? Hora seminario cargada:', horaStr);
                        }
                        
                        // Fechas de inscripción
                        if (document.getElementById('fechaInicioInscripcion')) {
                            document.getElementById('fechaInicioInscripcion').value = oferta.fechaInicioInscripcion || '';
                            console.log('? Fecha inicio inscripción cargada:', oferta.fechaInicioInscripcion);
                        }
                        if (document.getElementById('fechaFinInscripcion')) {
                            document.getElementById('fechaFinInscripcion').value = oferta.fechaFinInscripcion || '';
                            console.log('? Fecha fin inscripción cargada:', oferta.fechaFinInscripcion);
                        }
                        
                        if (document.getElementById('duracionMinutos')) {
                            document.getElementById('duracionMinutos').value = oferta.duracionMinutos || '';
                        }
                        if (document.getElementById('publicoObjetivoSeminario')) {
                            document.getElementById('publicoObjetivoSeminario').value = oferta.publicoObjetivo || '';
                            console.log('? Público objetivo cargado:', oferta.publicoObjetivo);
                        }
                        if (typeof actualizarEncuentrosSeminario === 'function') {
                            actualizarEncuentrosSeminario();
                        }
                        
                        // Cargar disertantes
                        if (oferta.disertantes && Array.isArray(oferta.disertantes)) {
                            // Limpiar disertantes de formato anidado [[...]]
                            disertantesSeminario = oferta.disertantes.map(d => {
                                if (Array.isArray(d)) {
                                    return d[0]; // Extraer el primer elemento si es un array
                                }
                                return d;
                            }).filter(d => d); // Filtrar valores vacíos
                            actualizarDisertantesChips('seminario');
                            console.log('? Disertantes seminario cargados:', disertantesSeminario);
                        }
                    }
                    
                    console.log('Campos específicos cargados');
                }, 100);
                
                // Cargar categorías
                if (oferta.categorias && Array.isArray(oferta.categorias)) {
                    categoriasSeleccionadas = oferta.categorias.map(cat => ({
                        id: cat.id.toString(),
                        nombre: cat.nombre
                    }));
                    actualizarCategoriasChips();
                    console.log('Categorías cargadas:', categoriasSeleccionadas.length);
                }
                
                // Cargar horarios si existen
                if (oferta.horarios && Array.isArray(oferta.horarios)) {
                    console.log('Horarios recibidos:', oferta.horarios);
                    horariosSeleccionados = oferta.horarios.map(h => {
                        console.log('  - Horario individual:', h);
                        return {
                            dia: h.dia || h.diaSemana,
                            horaInicio: h.horaInicio,
                            horaFin: h.horaFin
                        };
                    });
                    actualizarHorariosChips();
                    console.log('Horarios cargados:', horariosSeleccionados.length, horariosSeleccionados);
                }
                
                // Actualizar visibilidad de campos según modalidad y tipo
                actualizarCamposModalidad();
                
                console.log('Formulario poblado exitosamente');
                
            } catch (error) {
                console.error('Error poblando formulario:', error);
                mostrarNotificacion('Error al cargar los datos en el formulario: ' + error.message, 'error');
            }
        }
        
        /**
         * Función para mostrar el formulario
         */
        function mostrarFormulario() {
            const formContainer = document.getElementById('form-container');
            if (formContainer) {
                formContainer.style.display = 'block';
                formContainer.classList.add('show');
                
                // ? Establecer fecha mínima como hoy
                const hoy = new Date().toISOString().split('T')[0];
                const fechaInicio = document.getElementById('fechaInicio');
                const fechaFin = document.getElementById('fechaFin');
                if (fechaInicio) fechaInicio.setAttribute('min', hoy);
                if (fechaFin) fechaFin.setAttribute('min', hoy);
                
                // Scroll hasta el formulario
                formContainer.scrollIntoView({ behavior: 'smooth' });
            }
        }
        
        /**
         * Función para resetear el modo edición
         */
        function resetearModoEdicion() {
            modoEdicion = false;
            idOfertaModificar = null;
            estadoOfertaEnEdicion = null;
            
            // Resetear el botón submit
            const btnSubmit = document.getElementById('btn-submit-form');
            const btnSubmitText = document.getElementById('btn-submit-text');
            if (btnSubmit && btnSubmitText) {
                btnSubmitText.textContent = 'Registrar Oferta';
                btnSubmit.classList.remove('btn-update');
            }
            
            // Limpiar ID de oferta
            document.getElementById('idOfertaModificar').value = '';
            limpiarCategoriasSeleccionadas();
        }
        
        // Modificar la función de envío del formulario para manejar actualizaciones
        const formularioOriginal = document.getElementById('oferta-form');
        if (formularioOriginal) {
            // Interceptar el envío del formulario
            formularioOriginal.addEventListener('submit', function(e) {
                e.preventDefault();
                
                // Sincronizar campos calculados antes de validar/enviar
                const tipoOferta = document.getElementById('tipoOferta') ? document.getElementById('tipoOferta').value : '';
                
                console.log('?? Tipo de oferta:', tipoOferta);
                
                // Para CHARLA y SEMINARIO: copiar fechas específicas a campos generales PRIMERO
                if (tipoOferta === 'CHARLA') {
                    const fechaCharla = getValueById('fechaCharla');
                    console.log('?? Fecha Charla:', fechaCharla);
                    if (fechaCharla) {
                        const inputFechaInicio = document.getElementById('fechaInicio');
                        const inputFechaFin = document.getElementById('fechaFin');
                        
                        // Habilitar campos antes de asignar valores (para que se envíen en FormData)
                        inputFechaInicio.disabled = false;
                        inputFechaFin.disabled = false;
                        
                        inputFechaInicio.value = fechaCharla;
                        inputFechaFin.value = fechaCharla;
                        console.log('? Copiado fechaCharla a fechaInicio y fechaFin:', fechaCharla);
                    } else {
                        console.error('? No se encontró fechaCharla');
                    }
                }
                
                actualizarHorariosHidden();
                actualizarDocentesHidden('curso');
                actualizarDocentesHidden('formacion');
                actualizarDisertantesHidden('charla');
                actualizarDisertantesHidden('seminario');
                actualizarCategoriaHidden();
                actualizarCertificadoHidden();

                    if (!validarFormulario(tipoOferta)) {
                        console.warn('Validación fallida, formulario no enviado');
                        return;
                    }

                if (modoEdicion) {
                    // Confirmación para modificar
                    ModalConfirmacion.show(
                        'Modificar Oferta',
                        '¿Está seguro que desea modificar los datos de esta oferta?',
                        () => {
                            console.log('Enviando actualización de oferta');
                            enviarActualizacionOferta();
                        }
                    );
                } else {
                    // Confirmación para registrar
                    ModalConfirmacion.show(
                        'Registrar Oferta',
                        '¿Está seguro que desea registrar esta nueva oferta?',
                        async () => {
                            console.log('Enviando nueva oferta');
                            enviarFormularioAjax(); // Función existente para crear
                        }
                    );
                }
            });
        }
        
        /**
         * Función para enviar actualización de oferta
         */
        function enviarActualizacionOferta() {
            console.log('Iniciando actualización de oferta');
            
            // Sincronizar fechas para CHARLA y SEMINARIO antes de crear FormData
            const tipoOferta = getValueById('tipoOferta');
            if (tipoOferta === 'CHARLA') {
                const fechaCharla = getValueById('fechaCharla');
                if (fechaCharla) {
                    const inputFechaInicio = document.getElementById('fechaInicio');
                    const inputFechaFin = document.getElementById('fechaFin');
                    
                    // Habilitar campos antes de asignar valores
                    inputFechaInicio.disabled = false;
                    inputFechaFin.disabled = false;
                    
                    inputFechaInicio.value = fechaCharla;
                    inputFechaFin.value = fechaCharla;
                    console.log('? [EDICI�N] Copiado fechaCharla:', fechaCharla);
                }
            }
            
            const formulario = document.getElementById('oferta-form');
            const formData = new FormData(formulario);
            
            // Agregar el ID de la oferta
            if (typeof idOfertaModificar !== 'undefined' && idOfertaModificar) {
                formData.append('idOferta', idOfertaModificar);
            }
            
            // Mostrar loading
            const btnSubmit = document.getElementById('btn-submit-form');
            let textoOriginal = '';
            if (btnSubmit) {
                textoOriginal = btnSubmit.innerHTML;
                btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Actualizando...';
                btnSubmit.disabled = true;
            }
            
            fetch('/admin/ofertas/modificar', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                console.log('Respuesta del servidor:', data);
                
                if (data.success) {
                    console.log('Oferta actualizada exitosamente. Estado original era:', estadoOfertaEnEdicion);
                    mostrarNotificacion('Oferta actualizada exitosamente', 'success');
                    const idOfertaActual = (data.oferta && (data.oferta.id || data.oferta.idOferta))
                        || idOfertaModificar
                        || getValueById('idOfertaModificar')
                        || ofertaActualId;
                    if (idOfertaActual) {
                        ofertaActualId = idOfertaActual;
                    }
                    
                    // Verificar si estaba DE_BAJA para ofrecer dar de alta
                    if (estadoOfertaEnEdicion === 'DE_BAJA') {
                        console.log('?? Oferta estaba DE_BAJA, preguntando si activar...');
                        
                        setTimeout(() => {
                            ModalConfirmacion.show(
                                'Activar Oferta',
                                'La oferta se ha modificado correctamente. ¿Desea darla de alta ahora?',
                                () => {
                                    // Intentar dar de alta
                                    const token = obtenerCsrfToken();
                                    const headerName = obtenerCsrfHeaderName();
                                    const headers = {
                                        'Content-Type': 'application/json',
                                        'X-Requested-With': 'XMLHttpRequest'
                                    };
                                    if (token && headerName) headers[headerName] = token;
        
                                    const idParaCambiar = idOfertaActual
                                        || idOfertaModificar
                                        || getValueById('idOfertaModificar')
                                        || ofertaActualId;
                                    if (!idParaCambiar) {
                                        mostrarNotificacion('Error: No se pudo determinar el ID de la oferta', 'error');
                                        return;
                                    }
                                    fetch(`/admin/ofertas/cambiar-estado/${idParaCambiar}`, {
                                        method: 'POST',
                                        headers: headers
                                    })
                                    .then(r => r.json())
                                    .then(resp => {
                                        if (resp.success) {
                                            mostrarNotificacion('Oferta activada correctamente', 'success');
                                            cargarOfertas();
                                        } else {
                                            mostrarNotificacion('No se pudo activar la oferta: ' + resp.message, 'warning');
                                        }
                                    });
                                }
                            );
                        }, 500);

                                limpiarFormularioCompleto();
                                resetearModoEdicion();
                                cerrarFormulario();
                                actualizarTablaOfertas();
                            } else {
                                limpiarFormularioCompleto();
                                resetearModoEdicion();
                                cerrarFormulario();
                                actualizarTablaOfertas();
                            }
                        } else {
                            console.error('Error al actualizar:', data.message);
                            mostrarNotificacion('Error al actualizar la oferta: ' + data.message, 'error');
                        }
                    })
                    .catch(error => {
                        console.error('Error en la petición:', error);
                        mostrarNotificacion('Error de conexión al actualizar la oferta', 'error');
                    })
                    .finally(() => {
                        if (btnSubmit) {
                            btnSubmit.innerHTML = textoOriginal;
                            btnSubmit.disabled = false;
                        }
                    });
            }


            console.log('?? Sistema de modificación de ofertas inicializado');