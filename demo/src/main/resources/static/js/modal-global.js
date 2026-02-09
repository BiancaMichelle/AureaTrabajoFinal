/**
 * Sistema global de modales de confirmación
 */
window.ModalConfirmacion = {
    modalId: 'globalConfirmationModal',
    typeConfig: {
        confirm: { icon: 'fa-exclamation-circle', color: '#f59e0b', buttonColor: '#ef4444' },
        success: { icon: 'fa-check-circle', color: '#22c55e', buttonColor: '#22c55e' },
        error: { icon: 'fa-times-circle', color: '#ef4444', buttonColor: '#ef4444' },
        warning: { icon: 'fa-exclamation-triangle', color: '#f59e0b', buttonColor: '#f59e0b' },
        info: { icon: 'fa-info-circle', color: '#3b82f6', buttonColor: '#3b82f6' }
    },
    
    /**
     * Muestra el modal de confirmación
     * @param {string} title - Título del modal
     * @param {string} message - Mensaje del cuerpo
     * @param {function} onConfirm - Callback al confirmar
     * @param {object} options - Opciones adicionales (confirmText, cancelText, showCancel, type)
     */
    show: function(title, message, onConfirm, options) {
        const opts = options || {};
        const confirmText = opts.confirmText || 'Confirmar';
        const cancelText = opts.cancelText || 'Cancelar';
        const showCancel = opts.showCancel !== false;
        const type = opts.type || 'confirm';
        const closeOnConfirm = opts.closeOnConfirm !== false;
        const modal = document.getElementById(this.modalId);
        if (!modal) {
            console.error('Modal de confirmación no encontrado en el DOM');
            // Fallback for critical actions if modal is missing
            if (showCancel) {
                if(confirm(message)) {
                    if(onConfirm) onConfirm();
                }
            } else {
                alert(message);
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

        // Icono y color segun tipo
        const iconEl = document.getElementById('confIcon');
        const typeCfg = this.typeConfig[type] || this.typeConfig.confirm;
        if (iconEl && typeCfg) {
            iconEl.className = `fas ${typeCfg.icon}`;
            iconEl.style.color = typeCfg.color;
        }

        // Boton cancelar
        const btnCancel = document.getElementById('btnCancelConfirm');
        if (btnCancel) {
            btnCancel.textContent = cancelText;
            btnCancel.style.display = showCancel ? 'inline-flex' : 'none';
        }
        
        // Setup Confirm Button
        const btnConfirm = document.getElementById('btnAcceptConfirm');
        if(btnConfirm) {
            const newBtn = btnConfirm.cloneNode(true);
            btnConfirm.parentNode.replaceChild(newBtn, btnConfirm);

            newBtn.textContent = confirmText;
            if (typeCfg && typeCfg.buttonColor) {
                newBtn.style.backgroundColor = typeCfg.buttonColor;
                newBtn.style.color = '#ffffff';
                newBtn.style.border = 'none';
            }
            
            newBtn.addEventListener('click', () => {
                if (closeOnConfirm) {
                    this.close();
                }
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
