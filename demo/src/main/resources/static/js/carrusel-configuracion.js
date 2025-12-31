// Carrusel Configuration - Clean Version
document.addEventListener('DOMContentLoaded', function() {
    console.log('Carrusel configuration loaded');
    initializeUploadArea();
});

function initializeUploadArea() {
    const uploadArea = document.querySelector('.upload-area');
    const fileInput = document.getElementById('imageFiles');
    
    if (!uploadArea || !fileInput) {
        console.log('Upload elements not found');
        return;
    }
    
    // Setup drag and drop events
    setupDragAndDrop(uploadArea, fileInput);
    
    // Setup file input change event
    fileInput.addEventListener('change', function() {
        previewImages(this);
        if (this.files.length > 0) {
            showUploadButton();
        }
    });

    // Setup upload button click event (AJAX)
    const uploadBtn = document.getElementById('upload-btn');
    if (uploadBtn) {
        uploadBtn.addEventListener('click', function(e) {
            e.preventDefault();
            uploadImages(fileInput);
        });
    }
}

function uploadImages(fileInput) {
    const files = fileInput.files;
    if (!files || files.length === 0) return;

    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        formData.append('imagenes', files[i]);
    }

    const uploadBtn = document.getElementById('upload-btn');
    const originalText = uploadBtn.innerHTML;
    uploadBtn.disabled = true;
    uploadBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Subiendo...';

    fetch('/api/carrusel/subir', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) throw new Error('Error en la subida');
        return response.json();
    })
    .then(data => {
        if (data.success) {
            window.location.href = '/admin/configuracion?success=upload';
        } else {
            alert('Error: ' + data.message);
            uploadBtn.disabled = false;
            uploadBtn.innerHTML = originalText;
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al subir las imágenes. Por favor intente nuevamente.');
        uploadBtn.disabled = false;
        uploadBtn.innerHTML = originalText;
    });
}

// Funciones para el modal de imagen
function openImageModal(element) {
    const modal = document.getElementById('imageModal');
    const modalImg = document.getElementById('img01');
    const img = element.querySelector('img');
    
    modal.style.display = "flex";
    modalImg.src = img.getAttribute('data-full-src') || img.src;
}

function closeImageModal() {
    document.getElementById('imageModal').style.display = "none";
}

// Cerrar modal con tecla ESC
document.addEventListener('keydown', function(event) {
    if (event.key === "Escape") {
        closeImageModal();
    }
});

function setupDragAndDrop(uploadArea, fileInput) {
    uploadArea.addEventListener('dragover', function(e) {
        e.preventDefault();
        uploadArea.classList.add('drag-over');
    });
    
    uploadArea.addEventListener('dragleave', function(e) {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
    });
    
    uploadArea.addEventListener('drop', function(e) {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            fileInput.files = files;
            previewImages(fileInput);
            showUploadButton();
        }
    });
    
    uploadArea.addEventListener('click', function() {
        fileInput.click();
    });
}

function previewImages(input) {
    const previewContainer = document.getElementById('preview-container');
    const previewGrid = document.getElementById('preview-grid');
    
    if (!previewContainer || !previewGrid) {
        console.log('Preview containers not found');
        return;
    }
    
    if (input.files && input.files.length > 0) {
        previewContainer.style.display = 'block';
        previewGrid.innerHTML = '';
        
        Array.from(input.files).forEach((file, index) => {
            if (file.type.startsWith('image/')) {
                createImagePreview(file, index, previewGrid);
            }
        });
    } else {
        previewContainer.style.display = 'none';
    }
}

function createImagePreview(file, index, container) {
    const reader = new FileReader();
    reader.onload = function(e) {
        const previewItem = document.createElement('div');
        previewItem.className = 'preview-item mb-3';
        previewItem.innerHTML = '<div class="card">' +
            '<img src="' + e.target.result + '" class="card-img-top" alt="Preview ' + (index + 1) + '" style="height: 200px; object-fit: cover;">' +
            '<div class="card-body p-2">' +
            '<p class="card-text small mb-1"><strong>' + file.name + '</strong></p>' +
            '<p class="card-text small text-muted">' + formatFileSize(file.size) + '</p>' +
            '</div></div>';
        container.appendChild(previewItem);
    };
    reader.readAsDataURL(file);
}

function showUploadButton() {
    const uploadBtn = document.getElementById('upload-btn');
    if (uploadBtn) {
        uploadBtn.style.display = 'inline-flex';
        uploadBtn.classList.remove('d-none');
    }
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}
