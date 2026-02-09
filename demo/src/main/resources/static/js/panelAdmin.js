// ================= ADMIN DASHBOARD PROFESSIONAL JAVASCRIPT =================

// Función global para preview de imágenes
function previewImages(input) {
    const previewContainer = document.getElementById('preview-container');
    const previewGrid = document.getElementById('preview-grid');
    
    if (!input.files || input.files.length === 0) {
        previewContainer.style.display = 'none';
        return;
    }
    
    previewGrid.innerHTML = '';
    
    Array.from(input.files).forEach((file, index) => {
        if (file.type.startsWith('image/')) {
            const reader = new FileReader();
            reader.onload = function(e) {
                const previewItem = document.createElement('div');
                previewItem.className = 'preview-item';
                previewItem.innerHTML = `
                    <img src="${e.target.result}" alt="Preview ${index + 1}">
                    <div class="preview-info">
                        <small>${file.name}</small><br>
                        <small>${(file.size / 1024).toFixed(1)} KB</small>
                    </div>
                `;
                previewGrid.appendChild(previewItem);
            };
            reader.readAsDataURL(file);
        }
    });
    
    previewContainer.style.display = 'block';
}

// Función global para mostrar botón de subida
function showUploadButton() {
    const uploadBtn = document.getElementById('upload-btn');
    const fileInput = document.getElementById('imageFiles');
    
    if (fileInput.files && fileInput.files.length > 0) {
        uploadBtn.style.display = 'block';
    } else {
        uploadBtn.style.display = 'none';
    }
}

document.addEventListener('DOMContentLoaded', function () {
    
    // ================= SIDEBAR TOGGLE FUNCTIONALITY =================
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    const sidebarToggle = document.getElementById('sidebar-toggle');

    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', (e) => {
            e.preventDefault(); // Prevenir navegación del enlace
            sidebar.classList.toggle('collapsed');
            mainContent.classList.toggle('sidebar-collapsed');
            localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
        });
    }

    // Restore sidebar state from localStorage
    const savedSidebarState = localStorage.getItem('sidebarCollapsed');
    if (savedSidebarState === 'true') {
        sidebar.classList.add('collapsed');
        mainContent.classList.add('sidebar-collapsed');
    }

    // ================= MOBILE RESPONSIVE SIDEBAR =================
    function handleMobileMenu() {
        if (window.innerWidth <= 1024) {
            sidebar.classList.add('mobile');
            // Add backdrop for mobile
            if (!document.querySelector('.sidebar-backdrop')) {
                const backdrop = document.createElement('div');
                backdrop.className = 'sidebar-backdrop';
                backdrop.style.cssText = `
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    background: rgba(0, 0, 0, 0.5);
                    z-index: 999;
                    display: none;
                `;
                backdrop.addEventListener('click', () => {
                    sidebar.classList.remove('open');
                    backdrop.style.display = 'none';
                });
                document.body.appendChild(backdrop);
            }
            
            // Toggle mobile sidebar on button click
            if (sidebarToggle) {
                const mobileToggleHandler = (e) => {
                    e.preventDefault();
                    sidebar.classList.toggle('open');
                    const backdrop = document.querySelector('.sidebar-backdrop');
                    if (sidebar.classList.contains('open')) {
                        backdrop.style.display = 'block';
                    } else {
                        backdrop.style.display = 'none';
                    }
                };
                
                // Remove existing listeners to avoid duplicates
                sidebarToggle.removeEventListener('click', mobileToggleHandler);
                sidebarToggle.addEventListener('click', mobileToggleHandler);
            }
        } else {
            sidebar.classList.remove('mobile', 'open');
            const backdrop = document.querySelector('.sidebar-backdrop');
            if (backdrop) backdrop.remove();
        }
    }

    window.addEventListener('resize', handleMobileMenu);
    handleMobileMenu();

    // ================= CHARTS CONFIGURATION =================
    if (typeof Chart !== 'undefined') {
        Chart.defaults.font.family = "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif";
        Chart.defaults.color = '#64748b';
        Chart.defaults.borderColor = '#e2e8f0';

        const formatEntero = (value) => {
            const numero = Number(value);
            if (!Number.isFinite(numero)) return value;
            return Math.round(numero);
        };

        // 1. Gráfico de Distribución de Ofertas (Dona)
        const ofertasCtx = document.getElementById('ofertasChart');
        if (ofertasCtx) {
            // Check if chart instance exists and destroy it
            const existingOffersChart = Chart.getChart(ofertasCtx);
            if (existingOffersChart) {
                existingOffersChart.destroy();
            }
            new Chart(ofertasCtx, {
                type: 'doughnut',
                data: {
                    labels: ['Cursos', 'Formaciones', 'Seminarios', 'Charlas'],
                    datasets: [{
                        label: 'Distribución de Ofertas',
                        data: [
                            chartData.totalCursos || 0,
                            chartData.totalFormaciones || 0,
                            chartData.totalSeminarios || 0,
                            chartData.totalCharlas || 0
                        ],
                        backgroundColor: [
                            '#3b82f6', // Primary
                            '#10b981', // Success
                            '#06b6d4', // Accent
                            '#f59e0b'  // Warning
                        ],
                        borderColor: '#ffffff',
                        borderWidth: 3,
                        hoverBorderWidth: 4,
                        hoverOffset: 8
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: {
                                padding: 20,
                                usePointStyle: true,
                                font: {
                                    size: 12,
                                    weight: 500
                                }
                            }
                        },
                        tooltip: {
                            backgroundColor: '#1e293b',
                            titleColor: '#f8fafc',
                            bodyColor: '#f8fafc',
                            cornerRadius: 8,
                            padding: 12,
                            callbacks: {
                                label: function(context) {
                                    const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                    const percentage = total > 0 ? ((context.parsed / total) * 100).toFixed(1) : 0;
                                    return `${context.label}: ${formatEntero(context.parsed)} (${percentage}%)`;
                                }
                            }
                        }
                    },
                    animation: {
                        animateRotate: true,
                        duration: 1500
                    }
                }
            });
        }

        // 2. Gráfico de Distribución de Usuarios (Barras)
        const usuariosCtx = document.getElementById('usuariosChart');
        if (usuariosCtx) {
            const existingUsersChart = Chart.getChart(usuariosCtx);
            if (existingUsersChart) {
                existingUsersChart.destroy();
            }
            new Chart(usuariosCtx, {
                type: 'bar',
                data: {
                    labels: ['Alumnos', 'Docentes'],
                    datasets: [{
                        label: 'Total de Usuarios',
                        data: [
                            chartData.totalAlumnos || 0,
                            chartData.totalDocentes || 0
                        ],
                        backgroundColor: [
                            '#3b82f6',
                            '#10b981'
                        ],
                        borderRadius: 8,
                        borderSkipped: false,
                        hoverBackgroundColor: [
                            '#2563eb',
                            '#059669'
                        ]
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y',
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            backgroundColor: '#1e293b',
                            titleColor: '#f8fafc',
                            bodyColor: '#f8fafc',
                            cornerRadius: 8,
                            padding: 12,
                            callbacks: {
                                label: (context) => `${context.label}: ${formatEntero(context.raw)}`
                            }
                        }
                    },
                    scales: {
                        x: {
                            beginAtZero: true,
                            ticks: {
                                color: '#64748b',
                                font: {
                                    size: 12
                                },
                                stepSize: 1,
                                precision: 0
                            },
                            grid: {
                                color: '#e2e8f0',
                                drawBorder: false
                            }
                        },
                        y: {
                            ticks: {
                                color: '#64748b',
                                font: {
                                    size: 12,
                                    weight: 500
                                }
                            },
                            grid: {
                                display: false
                            }
                        }
                    },
                    animation: {
                        duration: 1500,
                        easing: 'easeOutCubic'
                    }
                }
            });
        }

        // 3. Gráfico de Actividad Mensual (Líneas)
        const actividadCtx = document.getElementById('actividadChart');
        if (actividadCtx) {
            // Sample data for monthly activity
            const actividadData = chartData.actividadMensual || [
                { mes: 'Ene', inscripciones: 15, completados: 12 },
                { mes: 'Feb', inscripciones: 22, completados: 18 },
                { mes: 'Mar', inscripciones: 28, completados: 25 },
                { mes: 'Abr', inscripciones: 35, completados: 30 },
                { mes: 'May', inscripciones: 42, completados: 38 },
                { mes: 'Jun', inscripciones: 38, completados: 35 }
            ];

            new Chart(actividadCtx, {
                type: 'line',
                data: {
                    labels: actividadData.map(item => item.mes),
                    datasets: [
                        {
                            label: 'Inscripciones',
                            data: actividadData.map(item => item.inscripciones),
                            borderColor: '#3b82f6',
                            backgroundColor: 'rgba(59, 130, 246, 0.1)',
                            fill: true,
                            tension: 0.4,
                            pointBackgroundColor: '#3b82f6',
                            pointBorderColor: '#ffffff',
                            pointBorderWidth: 2,
                            pointRadius: 6
                        },
                        {
                            label: 'Completados',
                            data: actividadData.map(item => item.completados),
                            borderColor: '#10b981',
                            backgroundColor: 'rgba(16, 185, 129, 0.1)',
                            fill: true,
                            tension: 0.4,
                            pointBackgroundColor: '#10b981',
                            pointBorderColor: '#ffffff',
                            pointBorderWidth: 2,
                            pointRadius: 6
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: {
                                padding: 20,
                                usePointStyle: true,
                                font: {
                                    size: 12,
                                    weight: 500
                                }
                            }
                        },
                        tooltip: {
                            backgroundColor: '#1e293b',
                            titleColor: '#f8fafc',
                            bodyColor: '#f8fafc',
                            cornerRadius: 8,
                            padding: 12
                        }
                    },
                    scales: {
                        x: {
                            ticks: {
                                color: '#64748b',
                                font: {
                                    size: 12
                                }
                            },
                            grid: {
                                color: '#e2e8f0',
                                drawBorder: false
                            }
                        },
                        y: {
                            beginAtZero: true,
                            ticks: {
                                stepSize: 1,
                                precision: 0,
                                color: '#64748b',
                                font: {
                                    size: 12
                                }
                            },
                            grid: {
                                color: '#e2e8f0',
                                drawBorder: false
                            }
                        }
                    },
                    animation: {
                        duration: 1500,
                        easing: 'easeOutCubic'
                    }
                }
            });
        }
    }

    // ================= KPI CARDS ANIMATION =================
    function animateKPICards() {
        const kpiCards = document.querySelectorAll('.kpi-card p');
        kpiCards.forEach((card, index) => {
            const finalValue = parseInt(card.textContent) || 0;
            let currentValue = 0;
            const increment = finalValue / 50;
            const timer = setInterval(() => {
                currentValue += increment;
                if (currentValue >= finalValue) {
                    currentValue = finalValue;
                    clearInterval(timer);
                }
                if (card.textContent.includes('%')) {
                    card.innerHTML = Math.floor(currentValue) + '%';
                } else {
                    card.textContent = Math.floor(currentValue);
                }
            }, 30);
        });
    }

    // Start KPI animation after a small delay
    setTimeout(animateKPICards, 500);

    // ================= COLLAPSIBLE FORMS =================
    const toggleButtons = document.querySelectorAll('.btn-primary[data-toggle]');
    toggleButtons.forEach(button => {
        button.addEventListener('click', function() {
            const targetId = this.getAttribute('data-toggle');
            const target = document.getElementById(targetId);
            const icon = this.querySelector('i');
            
            if (target && target.classList.contains('show')) {
                target.classList.remove('show');
                if (icon) icon.className = 'fas fa-plus';
            } else if (target) {
                target.classList.add('show');
                if (icon) icon.className = 'fas fa-minus';
            }
        });
    });

    // ================= REAL-TIME UPDATES (Optional) =================
    function updateDashboardData() {
        // This function can be used to fetch real-time data
        // fetch('/api/dashboard-data')
        //     .then(response => response.json())
        //     .then(data => {
        //         // Update KPI cards
        //         updateKPIValues(data);
        //         // Update charts
        //         updateCharts(data);
        //     });
    }

    // Update dashboard every 5 minutes (optional)
    // setInterval(updateDashboardData, 300000);

    // ================= SMOOTH SCROLL FOR NAVIGATION =================
    const navLinks = document.querySelectorAll('.sidebar-nav a[href^="#"]');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('href').substring(1);
            const targetElement = document.getElementById(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // ================= ACTIVE NAVIGATION HIGHLIGHTING =================
    function updateActiveNavigation() {
        const sections = document.querySelectorAll('section[id]');
        const navLinks = document.querySelectorAll('.sidebar-nav a');
        
        let current = '';
        sections.forEach(section => {
            const sectionTop = section.offsetTop;
            if (window.scrollY >= sectionTop - 60) {
                current = section.getAttribute('id');
            }
        });

        navLinks.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === `#${current}`) {
                link.classList.add('active');
            }
        });
    }

    window.addEventListener('scroll', updateActiveNavigation);

    // ================= THEME TOGGLE (Optional) =================
    function initializeThemeToggle() {
        const themeToggle = document.getElementById('theme-toggle');
        if (themeToggle) {
            themeToggle.addEventListener('click', () => {
                document.body.classList.toggle('dark-theme');
                localStorage.setItem('darkTheme', document.body.classList.contains('dark-theme'));
            });

            // Restore theme preference
            const savedTheme = localStorage.getItem('darkTheme');
            if (savedTheme === 'true') {
                document.body.classList.add('dark-theme');
            }
        }
    }

    initializeThemeToggle();

    console.log('Admin Dashboard initialized successfully');
});
// ================= GLOBAL NOTIFICATION SYSTEM =================

// Funci�n para mostrar notificaciones
function mostrarNotificacion(mensaje, tipo = 'success') {
    const notificationArea = document.getElementById('notification-area');
    const notification = document.getElementById('notification');
    const notificationIcon = document.getElementById('notification-icon');
    const notificationMessage = document.getElementById('notification-message');
    
    if (!notificationArea || !notification || !notificationIcon || !notificationMessage) {
        console.error('Elementos de notificaci�n no encontrados en el DOM');
        return;
    }

    // Configurar icono seg�n tipo
    let iconClass = 'fas fa-check-circle';
    if (tipo === 'error') {
        iconClass = 'fas fa-exclamation-circle';
    } else if (tipo === 'warning') {
        iconClass = 'fas fa-exclamation-triangle';
    }
    
    notificationIcon.className = iconClass;
    notificationMessage.textContent = mensaje;
    
    // Limpiar clases anteriores y agregar nueva
    notification.className = 'notification notification-' + tipo;
    
    // Mostrar notificaci�n
    notificationArea.style.display = 'block';
    setTimeout(() => {
        notification.classList.add('show');
    }, 10);
    
    // Auto-ocultar deshabilitado: solo cerrar con la X
}

// Funci�n para ocultar notificaci�n
function ocultarNotificacion() {
    const notificationArea = document.getElementById('notification-area');
    const notification = document.getElementById('notification');
    
    if (notification) {
        notification.classList.remove('show');
        setTimeout(() => {
            if (notificationArea) {
                notificationArea.style.display = 'none';
            }
        }, 300);
    }
}
