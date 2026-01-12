/**
 * Sistema global de modales de confirmación
 */
const ModalConfirmacion = {
    modalId: 'globalConfirmationModal',
    
    /**
     * Muestra el modal de confirmación
     * @param {string} title - Título del modal
     * @param {string} message - Mensaje del cuerpo
     * @param {function} onConfirm - Callback al confirmar
     */
    show: function(title, message, onConfirm) {
        const modal = document.getElementById(this.modalId);
        if (!modal) {
            console.error('Modal de confirmación no encontrado en el DOM');
            // Fallback for critical actions if modal is missing
            if(confirm(message)) {
                if(onConfirm) onConfirm();
            }
            return;
        }
        
        // Set texts
        const titleEl = document.getElementById('confTitle');
        if(titleEl) {
            titleEl.textContent = title;
        } else {
            // Fallback strategy
            const titleSpan = modal.querySelector('h3 span');
             if(titleSpan) titleSpan.textContent = title;
        }

        const msgEl = document.getElementById('confirmationMessage');
        if(msgEl) msgEl.textContent = message;
        
        // Setup Confirm Button
        const btnConfirm = document.getElementById('btnAcceptConfirm');
        if(btnConfirm) {
            const newBtn = btnConfirm.cloneNode(true);
            btnConfirm.parentNode.replaceChild(newBtn, btnConfirm);
            
            newBtn.addEventListener('click', () => {
                this.close();
                if (onConfirm) onConfirm();
            });
        }

        // Show
        modal.classList.remove('fade-out');
        modal.style.display = 'flex';
    },
    
    close: function() {
        const modal = document.getElementById(this.modalId);
        if (modal) {
            modal.style.display = 'none';
        }
    }
};

// Global helper for close buttons
window.closeConfirmModal = function() {
    ModalConfirmacion.close();
};

// Close on outside click
window.addEventListener('click', function(event) {
    const modal = document.getElementById(ModalConfirmacion.modalId);
    if (event.target === modal) {
        ModalConfirmacion.close();
    }
});
