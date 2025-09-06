// Funciones para controlar la visibilidad del modal
function openAuthModal() {
    document.getElementById('authModal').style.display = 'flex';
}

function closeAuthModal() {
    document.getElementById('authModal').style.display = 'none';
}

// Función para cambiar entre el formulario de login y registro
function toggleForms() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const container = document.querySelector('.forms-container');

    if (loginForm.style.display === 'none') {
        // Cambiar a login
        container.classList.remove('show-register');
        setTimeout(() => {
            loginForm.style.display = 'block';
            registerForm.style.display = 'none';
        }, 400); // Coincide con la duración de la transición
    } else {
        // Cambiar a registro
        container.classList.add('show-register');
        setTimeout(() => {
            loginForm.style.display = 'none';
            registerForm.style.display = 'block';
        }, 400);
    }
}


// Funciones para la navegación por pasos del formulario de registro
let currentStep = 1;

function nextStep(step) {
    document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
    document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');
    
    document.querySelector(`.step[data-step="${currentStep}"]`).classList.remove('active');
    document.querySelector(`.step[data-step="${step}"]`).classList.add('active');
    
    currentStep = step;
}

function prevStep(step) {
    document.querySelector(`.form-step[data-step="${currentStep}"]`).classList.remove('active');
    document.querySelector(`.form-step[data-step="${step}"]`).classList.add('active');

    document.querySelector(`.step[data-step="${currentStep}"]`).classList.remove('active');
    document.querySelector(`.step[data-step="${step}"]`).classList.add('active');

    currentStep = step;
}
