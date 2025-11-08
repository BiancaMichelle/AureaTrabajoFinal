// ================================================
// GESTIÓN DE OFERTAS ACADÉMICAS - VERSIÓN SIMPLIFICADA
// ================================================

console.log('🔥 GESTION OFERTAS JS CARGADO');

document.addEventListener('DOMContentLoaded', function () {
    console.log('✅ DOM CARGADO - Iniciando gestión de ofertas');
    
    // Referencias a elementos principales
    const btnShowForm = document.getElementById('btn-show-form');
    const formContainer = document.getElementById('form-container');
    const btnCloseForm = document.getElementById('btn-close-form');
    const btnCancelForm = document.getElementById('btn-cancel-form');

    console.log('🔍 Elementos encontrados:');
    console.log('- btnShowForm:', btnShowForm);
    console.log('- formContainer:', formContainer);
    console.log('- btnCloseForm:', btnCloseForm);

    // Inicializar manejadores de formulario
    initializeFormHandlers();

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
        const form = document.getElementById('oferta-form');
        if (form) {
            form.reset();
        }
    }

    console.log('Gestión de Ofertas inicializada correctamente');
});