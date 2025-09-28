document.addEventListener("DOMContentLoaded", function () {
    const formSteps = document.querySelectorAll(".form-step");
    // Prefer .step-item inside .steps-indicator if present, otherwise fall back to .step-horizontal
    let stepIndicators = document.querySelectorAll(".steps-indicator .step-item");
    if (!stepIndicators || stepIndicators.length === 0) {
        stepIndicators = document.querySelectorAll(".step-horizontal");
    }
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const submitBtn = document.getElementById("submitBtn");
    let currentStep = 0;

    function showStep(step) {
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

        // Botones
        prevBtn.style.display = step === 0 ? "none" : "inline-block";
        nextBtn.style.display = step === formSteps.length - 1 ? "none" : "inline-block";
        submitBtn.style.display = step === formSteps.length - 1 ? "inline-block" : "none";
    }


    prevBtn.addEventListener("click", function () {
        if (currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        }
    });

    nextBtn.addEventListener("click", function () {
        // Avanzar sin validación en el cliente (petición del desarrollador: no añadir validaciones)
        if (currentStep < formSteps.length - 1) {
            currentStep++;
            showStep(currentStep);
        }
    });

    // Inicial
    showStep(currentStep);
});
