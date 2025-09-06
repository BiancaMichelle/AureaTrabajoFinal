// Lógica para el panel de administración
document.addEventListener('DOMContentLoaded', function () {
    
    // Lógica para colapsar la sidebar
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    const sidebarToggle = document.getElementById('sidebar-toggle');

    sidebarToggle.addEventListener('click', () => {
        sidebar.classList.toggle('collapsed');
        mainContent.classList.toggle('sidebar-collapsed');
    });

    // --- GRÁFICOS CON CHART.JS ---

    // 1. Gráfico de Distribución de Ofertas (Dona)
    const ofertasCtx = document.getElementById('ofertasChart').getContext('2d');
    new Chart(ofertasCtx, {
        type: 'doughnut',
        data: {
            labels: ['Cursos', 'Formaciones', 'Seminarios', 'Charlas'],
            datasets: [{
                label: 'Distribución de Ofertas',
                data: [
                    chartData.totalCursos,
                    chartData.totalFormaciones,
                    chartData.totalSeminarios,
                    chartData.totalCharlas
                ],
                backgroundColor: [
                    '#4F46E5', // Primary Accent
                    '#3B82F6', // Blue
                    '#10B981', // Green
                    '#F59E0B'  // Amber
                ],
                borderColor: '#1F2937', // Main BG
                borderWidth: 3
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        color: '#F9FAFB' // Text Primary
                    }
                }
            }
        }
    });

    // 2. Gráfico de Distribución de Usuarios (Barras)
    const usuariosCtx = document.getElementById('usuariosChart').getContext('2d');
    new Chart(usuariosCtx, {
        type: 'bar',
        data: {
            labels: ['Alumnos', 'Docentes'],
            datasets: [{
                label: 'Total de Usuarios',
                data: [
                    chartData.totalAlumnos,
                    chartData.totalDocentes
                ],
                backgroundColor: [
                    '#3B82F6',
                    '#10B981'
                ],
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            indexAxis: 'y', // Barras horizontales
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                x: {
                    ticks: { color: '#9CA3AF' },
                    grid: { color: '#374151' }
                },
                y: {
                    ticks: { color: '#9CA3AF' },
                    grid: { color: '#374151' }
                }
            }
        }
    });

});