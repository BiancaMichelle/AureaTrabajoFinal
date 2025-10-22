// Script de depuración para categorías
console.log('=== CATEGORIA DEBUG SCRIPT LOADED ===');

// Función de prueba para verificar que el script se carga
function testScript() {
    console.log('Test script ejecutado correctamente');
    return 'OK';
}

// Función simplificada para probar el guardado
function testGuardarCategoria() {
    console.log('=== INICIANDO TEST GUARDAR CATEGORIA ===');
    
    // Verificar elementos del DOM
    const modal = document.getElementById('categoriaModal');
    const nombreInput = document.getElementById('categoriaNombre');
    const descripcionInput = document.getElementById('categoriaDescripcion');
    const btnGuardar = document.getElementById('btnGuardarCategoria');
    
    console.log('Modal encontrado:', modal ? 'SÍ' : 'NO');
    console.log('Input nombre encontrado:', nombreInput ? 'SÍ' : 'NO');
    console.log('Input descripción encontrado:', descripcionInput ? 'SÍ' : 'NO');
    console.log('Botón guardar encontrado:', btnGuardar ? 'SÍ' : 'NO');
    
    if (!nombreInput || !descripcionInput) {
        console.error('ERROR: Elementos del formulario no encontrados');
        return;
    }
    
    // Datos de prueba
    const datosTest = {
        nombre: 'Categoria Test',
        descripcion: 'Descripción de prueba'
    };
    
    console.log('Datos a enviar:', datosTest);
    console.log('URL actual:', window.location.href);
    console.log('Cookies:', document.cookie);
    
    // Hacer la petición fetch con mejor manejo de errores
    fetch('/admin/categorias/crear', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'same-origin', // Incluir cookies de sesión
        body: JSON.stringify(datosTest)
    })
    .then(response => {
        console.log('Response status:', response.status);
        console.log('Response ok:', response.ok);
        console.log('Response headers:', response.headers);
        
        if (response.status === 403) {
            console.error('❌ ERROR 403: No estás autenticado como administrador');
            alert('Error: Necesitas hacer login como administrador primero.\n\nVe a: /login\nCredenciales: admin@admin.com / admin123');
            return Promise.reject('No authenticated');
        }
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        return response.json();
    })
    .then(data => {
        console.log('Respuesta del servidor:', data);
        if (data.success) {
            console.log('✅ ÉXITO: Categoría creada correctamente');
            alert('✅ Categoría creada correctamente!');
        } else {
            console.log('❌ ERROR:', data.message);
            alert('❌ Error: ' + data.message);
        }
    })
    .catch(error => {
        console.error('❌ ERROR EN FETCH:', error);
        if (error !== 'No authenticated') {
            alert('❌ Error de conexión: ' + error.message);
        }
    });
}

// Función para verificar el estado del botón guardar
function verificarBotonGuardar() {
    const btnGuardar = document.getElementById('btnGuardarCategoria');
    if (btnGuardar) {
        console.log('Botón guardar encontrado');
        console.log('Event listeners actuales:', getEventListeners ? getEventListeners(btnGuardar) : 'getEventListeners no disponible');
        
        // Agregar listener de prueba
        btnGuardar.addEventListener('click', function(e) {
            console.log('=== CLICK EN BOTÓN GUARDAR DETECTADO ===');
            e.preventDefault();
            testGuardarCategoria();
        });
        
        console.log('Listener de prueba agregado al botón');
    } else {
        console.log('❌ Botón guardar NO encontrado');
    }
}

// Ejecutar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    console.log('=== DOM LOADED - INICIANDO DEBUG ===');
    
    setTimeout(() => {
        verificarBotonGuardar();
    }, 1000);
});

// Función para verificar el estado de autenticación
function verificarAutenticacion() {
    console.log('=== VERIFICANDO AUTENTICACIÓN ===');
    console.log('URL actual:', window.location.href);
    console.log('Cookies:', document.cookie);
    
    // Hacer una petición de prueba al endpoint de administración
    fetch('/admin/dashboard', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        console.log('Status de autenticación:', response.status);
        if (response.status === 200) {
            console.log('✅ AUTENTICADO: Tienes acceso de administrador');
            return true;
        } else if (response.status === 403) {
            console.log('❌ NO AUTENTICADO: Necesitas hacer login como admin');
            alert('❌ No estás autenticado como administrador.\n\n1. Ve a: /crear-admin-temporal\n2. Luego a: /login\n3. Usa: admin@admin.com / admin123');
            return false;
        } else {
            console.log('⚠️ Estado desconocido:', response.status);
            return false;
        }
    })
    .catch(error => {
        console.error('Error verificando autenticación:', error);
        return false;
    });
}

// Funciones globales para testing manual
window.testGuardarCategoria = testGuardarCategoria;
window.verificarBotonGuardar = verificarBotonGuardar;
window.verificarAutenticacion = verificarAutenticacion;
window.testScript = testScript;

console.log('=== CATEGORIA DEBUG SCRIPT READY ===');
console.log('Comandos disponibles:');
console.log('- testScript()');
console.log('- verificarAutenticacion()');
console.log('- verificarBotonGuardar()');
console.log('- testGuardarCategoria()');