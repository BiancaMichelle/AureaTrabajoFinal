document.addEventListener('DOMContentLoaded', function() {
    // Elementos del DOM
    const btnShowForm = document.getElementById('btn-show-form');
    const btnCancelForm = document.getElementById('btn-cancel-form');
    const formContainer = document.getElementById('form-container');
    
    const rolSelect = document.getElementById('rol');
    const datosAcademicos = document.getElementById('datos-academicos-alumno');
    const datosProfesionales = document.getElementById('datos-profesionales-docente');

    const fotoInput = document.getElementById('foto');
    const photoPreview = document.getElementById('photo-preview');
    const photoUploadText = document.getElementById('photo-upload-text');

    // --- Lógica para mostrar/ocultar el formulario ---
    if (btnShowForm) {
        btnShowForm.addEventListener('click', function() {
            if (formContainer.classList.contains('show')) {
                formContainer.classList.remove('show');
            } else {
                formContainer.style.display = 'block';
                // Pequeño delay para permitir que el display:block se aplique antes de la transición
                setTimeout(() => {
                    formContainer.classList.add('show');
                }, 10);
            }
        });
    }

    if (btnCancelForm) {
        btnCancelForm.addEventListener('click', function() {
            formContainer.classList.remove('show');
             // Esperar a que la transición termine para ocultar el elemento
            setTimeout(() => {
                if (!formContainer.classList.contains('show')) {
                    formContainer.style.display = 'none';
                }
            }, 500); // Coincide con la duración de la transición en CSS
        });
    }

    // --- Lógica para la vista previa de la foto ---
    if (fotoInput) {
        fotoInput.addEventListener('change', function(event) {
            const file = event.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    photoPreview.src = e.target.result;
                    photoPreview.style.display = 'block';
                    if (photoUploadText) {
                        photoUploadText.style.display = 'none';
                    }
                }
                reader.readAsDataURL(file);
            }
        });
    }

    // --- Lógica para el gestor de disponibilidad ---
    const btnAddAvailability = document.getElementById('btn-add-availability');
    if (btnAddAvailability) {
        btnAddAvailability.addEventListener('click', function() {
            const diaSelect = document.getElementById('dia-semana');
            const horaDesdeInput = document.getElementById('hora-desde');
            const horaHastaInput = document.getElementById('hora-hasta');
            const availabilityTableBody = document.querySelector('#availability-table tbody');

            const dia = diaSelect.options[diaSelect.selectedIndex].text;
            const horaDesde = horaDesdeInput.value;
            const horaHasta = horaHastaInput.value;

            if (!horaDesde || !horaHasta) {
                alert('Por favor, seleccione una hora de inicio y fin.');
                return;
            }
            if (horaDesde >= horaHasta) {
                alert('La hora de inicio debe ser anterior a la hora de fin.');
                return;
            }

            // Crear la nueva fila
            const newRow = document.createElement('tr');
            newRow.innerHTML = `
                <td>${dia}</td>
                <td>${horaDesde} - ${horaHasta}</td>
                <td class="actions">
                    <button type="button" class="btn-icon btn-delete btn-delete-availability"><i class="fas fa-trash-alt"></i></button>
                </td>
            `;

            // Añadir evento de borrado a la nueva fila
            newRow.querySelector('.btn-delete-availability').addEventListener('click', function() {
                this.closest('tr').remove();
            });

            availabilityTableBody.appendChild(newRow);

            // Limpiar inputs
            horaDesdeInput.value = '';
            horaHastaInput.value = '';
        });
    }

    // --- Lógica para campos condicionales según el rol ---
    if (rolSelect) {
        rolSelect.addEventListener('change', function() {
            const selectedRol = this.value;
            
            // Ocultar ambos por defecto
            datosAcademicos.style.display = 'none';
            datosProfesionales.style.display = 'none';

            // Mostrar el correspondiente si es necesario
            if (selectedRol === 'ALUMNO') {
                datosAcademicos.style.display = 'block';
            } else if (selectedRol === 'DOCENTE') {
                datosProfesionales.style.display = 'block';
            }
        });
    }
});
