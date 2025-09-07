document.addEventListener('DOMContentLoaded', function () {
    const btnShowForm = document.getElementById('btn-show-form');
    const formContainer = document.getElementById('form-container');
    const btnCancelForm = document.getElementById('btn-cancel-form');
    const collapsibleContent = formContainer.querySelector('.collapsible-content') || formContainer;

    // Lógica para mostrar/ocultar el formulario principal
    if (btnShowForm) {
        btnShowForm.addEventListener('click', function () {
            if (formContainer.style.display === 'none' || !formContainer.style.display) {
                formContainer.style.display = 'block';
                setTimeout(() => {
                    collapsibleContent.classList.add('show');
                    btnShowForm.innerHTML = '<i class="fas fa-minus-circle"></i> Ocultar Formulario';
                }, 10);
            } else {
                collapsibleContent.classList.remove('show');
                setTimeout(() => {
                    formContainer.style.display = 'none';
                    btnShowForm.innerHTML = '<i class="fas fa-plus-circle"></i> Registrar Nueva Oferta';
                }, 500); // Coincide con la duración de la transición
            }
        });
    }

    if (btnCancelForm) {
        btnCancelForm.addEventListener('click', function () {
            collapsibleContent.classList.remove('show');
            setTimeout(() => {
                formContainer.style.display = 'none';
                btnShowForm.innerHTML = '<i class="fas fa-plus-circle"></i> Registrar Nueva Oferta';
            }, 500);
        });
    }

    // Lógica para los campos condicionales según el tipo de oferta
    const tipoOfertaSelect = document.getElementById('tipoOferta');
    const fieldsCharlaSeminario = document.getElementById('fields-charla-seminario');
    const fieldsFormacionCurso = document.getElementById('fields-formacion-curso');
    const fieldsFormacion = document.getElementById('fields-formacion');
    const fieldsCurso = document.getElementById('fields-curso');

    if (tipoOfertaSelect) {
        tipoOfertaSelect.addEventListener('change', function () {
            const selectedType = this.value;

            // Ocultar todos los campos condicionales
            [fieldsCharlaSeminario, fieldsFormacionCurso, fieldsFormacion, fieldsCurso].forEach(field => {
                if(field) field.style.display = 'none';
            });

            // Mostrar campos según la selección
            if (selectedType === 'CHARLA' || selectedType === 'SEMINARIO') {
                fieldsCharlaSeminario.style.display = 'block';
            }
            
            if (selectedType === 'FORMACION' || selectedType === 'CURSO') {
                fieldsFormacionCurso.style.display = 'block';
            }

            if (selectedType === 'FORMACION') {
                fieldsFormacion.style.display = 'block';
            }

            if (selectedType === 'CURSO') {
                fieldsCurso.style.display = 'block';
            }
        });
    }
    
    // Lógica para la previsualización de la imagen
    const photoInput = document.getElementById('imagen');
    const photoPreview = document.getElementById('photo-preview');
    const photoUploadText = document.getElementById('photo-upload-text');

    if (photoInput && photoPreview && photoUploadText) {
        photoInput.addEventListener('change', function(event) {
            const file = event.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    photoPreview.src = e.target.result;
                    photoPreview.style.display = 'block';
                    photoUploadText.style.display = 'none';
                }
                reader.readAsDataURL(file);
            }
        });
    }

    // Lógica para añadir y quitar horarios
    const btnAddHorario = document.getElementById('btn-add-horario');
    const horariosTableBody = document.querySelector('#horarios-table tbody');

    if (btnAddHorario && horariosTableBody) {
        btnAddHorario.addEventListener('click', function() {
            const diaSelect = document.getElementById('dia-semana');
            const horaDesdeInput = document.getElementById('hora-desde');
            const horaHastaInput = document.getElementById('hora-hasta');

            const dia = diaSelect.value;
            const horaDesde = horaDesdeInput.value;
            const horaHasta = horaHastaInput.value;

            if (dia && horaDesde && horaHasta) {
                const newRow = document.createElement('tr');
                newRow.innerHTML = `
                    <td>${dia}</td>
                    <td>${horaDesde} - ${horaHasta}</td>
                    <td class="actions">
                        <button type="button" class="btn-icon btn-delete btn-delete-horario"><i class="fas fa-trash-alt"></i></button>
                    </td>
                `;
                horariosTableBody.appendChild(newRow);
                // Limpiar inputs
                horaDesdeInput.value = '';
                horaHastaInput.value = '';
            } else {
                alert('Por favor, complete todos los campos del horario.');
            }
        });

        horariosTableBody.addEventListener('click', function(event) {
            if (event.target.closest('.btn-delete-horario')) {
                event.target.closest('tr').remove();
            }
        });
    }

    // Lógica para buscar y asignar docentes
    const docenteSearchInput = document.getElementById('docente-search');
    const docenteSearchResults = document.getElementById('docente-search-results');
    const docentesTableBody = document.querySelector('#docentes-table tbody');
    let searchTimeout;

    if (docenteSearchInput && docenteSearchResults && docentesTableBody) {
        docenteSearchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            const query = this.value;

            if (query.length < 3) {
                docenteSearchResults.innerHTML = '';
                docenteSearchResults.style.display = 'none';
                return;
            }

            searchTimeout = setTimeout(() => {
                fetch(`/api/docentes/search?query=${query}`)
                    .then(response => response.json())
                    .then(data => {
                        docenteSearchResults.innerHTML = '';
                        if (data.length > 0) {
                            data.forEach(docente => {
                                const resultItem = document.createElement('div');
                                resultItem.textContent = `${docente.nombres} ${docente.apellidos} (DNI: ${docente.dni})`;
                                resultItem.dataset.docenteId = docente.id;
                                resultItem.dataset.docenteNombre = `${docente.nombres} ${docente.apellidos}`;
                                resultItem.dataset.docenteDni = docente.dni;
                                docenteSearchResults.appendChild(resultItem);
                            });
                            docenteSearchResults.style.display = 'block';
                        } else {
                            docenteSearchResults.style.display = 'none';
                        }
                    });
            }, 300); // Debounce para no saturar con peticiones
        });

        // Ocultar resultados al hacer clic fuera
        document.addEventListener('click', function(event) {
            if (!docenteSearchInput.contains(event.target)) {
                docenteSearchResults.style.display = 'none';
            }
        });

        // Añadir docente a la tabla
        docenteSearchResults.addEventListener('click', function(event) {
            if (event.target.dataset.docenteId) {
                const docenteId = event.target.dataset.docenteId;

                // Evitar duplicados
                if (docentesTableBody.querySelector(`tr[data-docente-id="${docenteId}"]`)) {
                    alert('Este docente ya ha sido asignado.');
                    return;
                }

                const newRow = document.createElement('tr');
                newRow.dataset.docenteId = docenteId;
                newRow.innerHTML = `
                    <td>${event.target.dataset.docenteNombre}</td>
                    <td>${event.target.dataset.docenteDni}</td>
                    <td class="actions">
                        <button type="button" class="btn-icon btn-delete btn-delete-docente"><i class="fas fa-trash-alt"></i></button>
                    </td>
                `;
                docentesTableBody.appendChild(newRow);
                docenteSearchInput.value = '';
                docenteSearchResults.style.display = 'none';
            }
        });

        // Eliminar docente de la tabla
        docentesTableBody.addEventListener('click', function(event) {
            if (event.target.closest('.btn-delete-docente')) {
                event.target.closest('tr').remove();
            }
        });
    }
});
