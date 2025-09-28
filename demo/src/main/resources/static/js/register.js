document.addEventListener("DOMContentLoaded", function () {

        function setupDatalistListeners() {
    // Manejar selección de país
    document.getElementById('pais').addEventListener('input', function(e) {
        const input = e.target;
        const options = document.getElementById('paisList').options;
        const hiddenCodigo = document.getElementById('paisCodigo'); // Cambiado
        
        // Buscar la opción que coincide
        for (let option of options) {
            if (option.value === input.value) {
                hiddenCodigo.value = option.getAttribute('data-code'); // Usar data-code
                console.log("País seleccionado - Código:", hiddenCodigo.value);
                
                // Cargar provincias para este país
                const countryCode = option.getAttribute('data-code');
                if (countryCode) {
                    cargarProvincias(countryCode);
                }
                return;
            }
        }
        // Si no encuentra coincidencia, limpiar código
        hiddenCodigo.value = '';
    });

    // Manejar selección de provincia
    document.getElementById('provincia').addEventListener('input', function(e) {
        const input = e.target;
        const options = document.getElementById('provinciaList').options;
        const hiddenCodigo = document.getElementById('provinciaCodigo'); // Cambiado
        
        for (let option of options) {
            if (option.value === input.value) {
                hiddenCodigo.value = option.getAttribute('data-code'); // Usar data-code
                console.log("Provincia seleccionada - Código:", hiddenCodigo.value);
                
                // Cargar ciudades para esta provincia
                const countryCode = document.getElementById('paisCodigo').value;
                const provinceCode = option.getAttribute('data-code');
                if (countryCode && provinceCode) {
                    cargarCiudades(countryCode, provinceCode);
                }
                return;
            }
        }
        hiddenCodigo.value = '';
    });

    // Manejar selección de ciudad
    document.getElementById('ciudad').addEventListener('input', function(e) {
        const input = e.target;
        const options = document.getElementById('ciudadList').options;
        const hiddenId = document.getElementById('ciudadId'); // Mantener igual
        
        for (let option of options) {
            if (option.value === input.value) {
                hiddenId.value = option.getAttribute('data-id');
                console.log("Ciudad seleccionada - ID:", hiddenId.value);
                return;
            }
        }
        hiddenId.value = '';
    });

    // TEMPORAL: Comenta la institución por ahora
    /*
    document.getElementById('colegioEgreso').addEventListener('input', function(e) {
        const input = e.target;
        const options = document.getElementById('institucionList').options;
        const hiddenId = document.getElementById('institucionId');
        
        for (let option of options) {
            if (option.value === input.value) {
                hiddenId.value = option.getAttribute('data-id');
                console.log("Institución seleccionada - ID:", hiddenId.value);
                return;
            }
        }
        hiddenId.value = '';
    });
    */
}

    // ✅ Funciones para cargar provincias y ciudades
    function cargarProvincias(paisCode) {
        fetch(`/provincias/${paisCode}`)
            .then(response => response.json())
            .then(provincias => {
                const datalist = document.getElementById('provinciaList');
                datalist.innerHTML = '';
                
                provincias.forEach(provincia => {
                    const option = document.createElement('option');
                    option.value = provincia.nombre;
                    option.setAttribute('data-id', provincia.id);
                    option.setAttribute('data-code', provincia.codigo);
                    datalist.appendChild(option);
                });
                
                // Limpiar campos dependientes
                document.getElementById('provincia').value = '';
                document.getElementById('provinciaId').value = '';
                document.getElementById('ciudad').value = '';
                document.getElementById('ciudadId').value = '';
                document.getElementById('ciudadList').innerHTML = '';
            })
            .catch(error => console.error('Error cargando provincias:', error));
    }

    function cargarCiudades(paisCode, provinciaCode) {
        fetch(`/ciudades/${paisCode}/${provinciaCode}`)
            .then(response => response.json())
            .then(ciudades => {
                const datalist = document.getElementById('ciudadList');
                datalist.innerHTML = '';
                
                ciudades.forEach(ciudad => {
                    const option = document.createElement('option');
                    option.value = ciudad.nombre;
                    option.setAttribute('data-id', ciudad.id);
                    datalist.appendChild(option);
                });
                
                // Limpiar campo ciudad
                document.getElementById('ciudad').value = '';
                document.getElementById('ciudadId').value = '';
            })
            .catch(error => console.error('Error cargando ciudades:', error));
    }

    // ✅ Inicializar listeners cuando se muestre el paso 2
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                const step2 = document.querySelector('[data-step="2"]');
                if (step2 && step2.style.display === 'block') {
                    setupDatalistListeners();
                }
            }
        });
    });

    const step2 = document.querySelector('[data-step="2"]');
    if (step2) {
        observer.observe(step2, { attributes: true });
    }


    const formSteps = document.querySelectorAll(".form-step");
    let stepIndicators = document.querySelectorAll(".steps-indicator .step-item");
    if (!stepIndicators || stepIndicators.length === 0) {
        stepIndicators = document.querySelectorAll(".step-horizontal");
    }
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const submitBtn = document.getElementById("submitBtn");
    let currentStep = 0;

    function showStep(step) {
        console.log("Mostrando paso:", step); // Debug
        
        // Mostrar solo el bloque del form correspondiente
        formSteps.forEach((s, i) => {
            s.style.display = (i === step) ? "block" : "none";
        });

        // Actualizar stepper visual
        stepIndicators.forEach((indicator, i) => {
            indicator.classList.remove("active", "complete");
            if (i < step) {
                indicator.classList.add("complete");
            } else if (i === step) {
                indicator.classList.add("active");
            }
        });

        // ✅ CORREGIDO: Lógica de botones
        if (step === 0) {
            // Primer paso
            prevBtn.style.display = "none";
            nextBtn.style.display = "inline-block";
            submitBtn.style.display = "none";
        } else if (step === formSteps.length - 1) {
            // Último paso
            prevBtn.style.display = "inline-block";
            nextBtn.style.display = "none";
            submitBtn.style.display = "inline-block"; // ✅ Mostrar botón registrar
        } else {
            // Pasos intermedios
            prevBtn.style.display = "inline-block";
            nextBtn.style.display = "inline-block";
            submitBtn.style.display = "none";
        }
    }

    function validateStep(step) {
        const stepNode = formSteps[step];
        if (!stepNode) return true;

        const inputs = stepNode.querySelectorAll('input, select, textarea');
        let isValid = true;

        for (let input of inputs) {
            if (input.disabled) continue;
            
            // Reset custom validity
            input.setCustomValidity('');
            
            if (input.type === 'checkbox' || input.type === 'radio') {
                if (input.required && !input.checked) {
                    input.focus();
                    if (input.reportValidity) {
                        input.reportValidity();
                    } else {
                        alert(`Por favor, completa: ${input.previousElementSibling?.textContent || input.name}`);
                    }
                    isValid = false;
                    break;
                }
            } else {
                if (!input.checkValidity()) {
                    input.focus();
                    if (input.reportValidity) {
                        input.reportValidity();
                    }
                    isValid = false;
                    break;
                }
            }
        }

        // Validaciones adicionales para el último paso
        if (step === formSteps.length - 1 && isValid) {
            const pwd = document.getElementById('password');
            const confirm = document.getElementById('confirmPassword');
            const terms = document.getElementById('terms');
            
            // Validar contraseñas
            if (pwd && confirm && pwd.value !== confirm.value) {
                confirm.setCustomValidity('Las contraseñas no coinciden');
                confirm.focus();
                if (confirm.reportValidity) {
                    confirm.reportValidity();
                } else {
                    alert('Las contraseñas no coinciden');
                }
                isValid = false;
            }
            
            // Validar términos y condiciones
            if (terms && terms.required && !terms.checked && isValid) {
                terms.focus();
                alert('Debes aceptar los términos y condiciones');
                isValid = false;
            }
        }

        return isValid;
    }

    prevBtn.addEventListener("click", function () {
        if (currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        }
    });

    nextBtn.addEventListener("click", function () {
        if (validateStep(currentStep)) {
            if (currentStep < formSteps.length - 1) {
                currentStep++;
                showStep(currentStep);
            }
        }
    });

    // ✅ Agregar evento de submit para debugging
    const form = document.getElementById('registerForm');
    if (form) {
        form.addEventListener('submit', function (e) {
            console.log("Formulario enviándose..."); // Debug
            
            // Validar último paso antes de enviar
            if (!validateStep(formSteps.length - 1)) {
                e.preventDefault();
                console.log("Validación falló, previniendo envío");
                return false;
            }
            
            console.log("Formulario válido, enviando...");
            // Mostrar loading o feedback visual
            submitBtn.disabled = true;
            submitBtn.textContent = "Registrando...";
            
            return true;
        });
    }

    // Inicializar
    showStep(currentStep);
    
    // ✅ Debug: Verificar que los elementos existen
    console.log("Elementos encontrados:");
    console.log("Form steps:", formSteps.length);
    console.log("Prev button:", prevBtn);
    console.log("Next button:", nextBtn);
    console.log("Submit button:", submitBtn);
});