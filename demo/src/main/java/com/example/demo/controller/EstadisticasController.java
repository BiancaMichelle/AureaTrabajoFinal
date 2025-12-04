package com.example.demo.controller;

import com.example.demo.service.EstadisticasService;
import com.example.demo.model.Auditable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/estadisticas")
public class EstadisticasController {

    @Autowired
    private EstadisticasService estadisticasService;

    /**
     * Página principal del módulo de estadísticas
     */
    @GetMapping
    @Auditable(action = "ACCESO_ESTADISTICAS", entity = "Sistema")
    public String mostrarEstadisticas(Model model) {
        try {
            // Usar el service real para obtener datos
            Map<String, Object> metricas = estadisticasService.obtenerMetricasGenerales();
            List<Map<String, Object>> demandaOfertas = estadisticasService.analizarDemandaOfertas();
            Map<String, Object> analisisDesercion = estadisticasService.analizarDesercion();
            Map<String, Object> proyeccionCrecimiento = estadisticasService.proyectarCrecimiento();
            
            // Agregar campos adicionales que necesita el template
            if (metricas != null && !metricas.containsKey("totalOfertas")) {
                metricas.put("totalOfertas", calcularTotalOfertas());
            }
            if (metricas != null && !metricas.containsKey("totalInscripciones")) {
                metricas.put("totalInscripciones", calcularTotalInscripciones());
            }
            if (metricas != null && !metricas.containsKey("ingresosTotales")) {
                metricas.put("ingresosTotales", calcularIngresosTotales());
            }
            
            model.addAttribute("metricas", metricas != null ? metricas : crearMetricasPorDefecto());
            model.addAttribute("demandaOfertas", demandaOfertas != null ? demandaOfertas : new ArrayList<>());
            model.addAttribute("analisisDesercion", analisisDesercion);
            model.addAttribute("proyeccionCrecimiento", proyeccionCrecimiento);
            model.addAttribute("fechaActualizacion", LocalDate.now());
            
            return "admin/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar estadísticas: " + e.getMessage());
            model.addAttribute("metricas", crearMetricasPorDefecto());
            model.addAttribute("demandaOfertas", new ArrayList<>());
            return "admin/estadisticas";
        }
    }

    /**
     * API REST - Obtener métricas generales
     */
    @GetMapping("/api/metricas-generales")
    @ResponseBody
    @Auditable(action = "CONSULTA_METRICAS_API", entity = "Sistema")
    public ResponseEntity<Map<String, Object>> obtenerMetricasGenerales() {
        try {
            Map<String, Object> metricas = estadisticasService.obtenerMetricasGenerales();
            return ResponseEntity.ok(metricas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al obtener métricas: " + e.getMessage()));
        }
    }

    /**
     * API REST - Análisis de demanda de ofertas
     */
    @GetMapping("/api/demanda")
    @ResponseBody
    @Auditable(action = "ANALISIS_DEMANDA", entity = "OfertaAcademica")
    public ResponseEntity<List<Map<String, Object>>> analizarDemanda() {
        try {
            List<Map<String, Object>> demanda = estadisticasService.analizarDemandaOfertas();
            return ResponseEntity.ok(demanda);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API REST - Análisis de deserción estudiantil
     */
    @GetMapping("/api/desercion")
    @ResponseBody
    @Auditable(action = "ANALISIS_DESERCION", entity = "Usuario")
    public ResponseEntity<Map<String, Object>> analizarDesercion() {
        try {
            Map<String, Object> desercion = estadisticasService.analizarDesercion();
            return ResponseEntity.ok(desercion);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al analizar deserción: " + e.getMessage()));
        }
    }

    /**
     * API REST - Proyección de crecimiento
     */
    @GetMapping("/api/proyeccion")
    @ResponseBody
    @Auditable(action = "PROYECCION_CRECIMIENTO", entity = "Sistema")
    public ResponseEntity<Map<String, Object>> proyectarCrecimiento() {
        try {
            Map<String, Object> proyeccion = estadisticasService.proyectarCrecimiento();
            return ResponseEntity.ok(proyeccion);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al proyectar crecimiento: " + e.getMessage()));
        }
    }

    /**
     * API REST - Ejecutar acciones automáticas (COMENTADO - método eliminado del servicio)
     */
    /*
    @PostMapping("/api/acciones-automaticas")
    @ResponseBody
    @Auditable(action = "ACCIONES_AUTOMATICAS", entity = "Sistema")
    public ResponseEntity<Map<String, Object>> ejecutarAccionesAutomaticas() {
        try {
            List<String> acciones = estadisticasService.ejecutarAccionesAutomaticas();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Acciones automáticas ejecutadas exitosamente",
                "acciones", acciones,
                "totalAcciones", acciones.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Error ejecutando acciones automáticas: " + e.getMessage()
            ));
        }
    }
    */

    /**
     * API REST - Datos para gráfico de ofertas por tipo
     */
    @GetMapping("/api/grafico-ofertas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDatosGraficoOfertas() {
        try {
            Map<String, Object> metricas = estadisticasService.obtenerMetricasGenerales();
            
            @SuppressWarnings("unchecked")
            Map<String, Long> ofertasPorTipo = (Map<String, Long>) metricas.get("ofertasPorTipo");
            
            if (ofertasPorTipo == null || ofertasPorTipo.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "labels", new String[]{"Sin datos"},
                    "data", new Integer[]{0},
                    "total", 0
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "labels", ofertasPorTipo.keySet().toArray(new String[0]),
                "data", ofertasPorTipo.values().toArray(new Long[0]),
                "total", ofertasPorTipo.values().stream().mapToLong(Long::longValue).sum()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API REST - Datos para gráfico de usuarios por rol
     */
    @GetMapping("/api/grafico-usuarios")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDatosGraficoUsuarios() {
        try {
            Map<String, Object> metricas = estadisticasService.obtenerMetricasGenerales();
            
            @SuppressWarnings("unchecked")
            Map<String, Long> usuariosPorRol = (Map<String, Long>) metricas.get("usuariosPorRol");
            
            if (usuariosPorRol == null || usuariosPorRol.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "labels", new String[]{"Sin datos"},
                    "data", new Integer[]{0},
                    "total", 0
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "labels", usuariosPorRol.keySet().toArray(new String[0]),
                "data", usuariosPorRol.values().toArray(new Long[0]),
                "total", usuariosPorRol.values().stream().mapToLong(Long::longValue).sum()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API REST - Datos para gráfico de inscripciones mensuales
     */
    @GetMapping("/api/grafico-inscripciones")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDatosGraficoInscripciones() {
        try {
            Map<String, Object> metricas = estadisticasService.obtenerMetricasGenerales();
            
            @SuppressWarnings("unchecked")
            List<Object[]> inscripcionesPorMes = (List<Object[]>) metricas.get("inscripcionesPorMes");
            
            String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", 
                             "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            Long[] datos = new Long[12];
            
            // Inicializar con ceros
            for (int i = 0; i < 12; i++) {
                datos[i] = 0L;
            }
            
            // Llenar con datos reales
            if (inscripcionesPorMes != null) {
                for (Object[] row : inscripcionesPorMes) {
                    int mes = ((Number) row[0]).intValue() - 1; // Mes 1-12 a índice 0-11
                    Long cantidad = ((Number) row[1]).longValue();
                    if (mes >= 0 && mes < 12) {
                        datos[mes] = cantidad;
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "labels", meses,
                "data", datos,
                "total", java.util.Arrays.stream(datos).mapToLong(Long::longValue).sum()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para generar reportes (funcionalidad futura)
     */
    @GetMapping("/api/generar-reporte")
    @ResponseBody
    @Auditable(action = "GENERAR_REPORTE", entity = "Sistema")
    public ResponseEntity<Map<String, Object>> generarReporte(
            @RequestParam(required = false, defaultValue = "general") String tipoReporte) {
        
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Funcionalidad de reportes en desarrollo",
                "tipoReporte", tipoReporte,
                "fecha", LocalDate.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Error generando reporte: " + e.getMessage()
            ));
        }
    }
    
    // ================ MÉTODOS AUXILIARES PRIVADOS ================
    
    @Autowired
    private jakarta.persistence.EntityManager entityManager;
    
    /**
     * Calcula el total de ofertas académicas activas
     */
    private Long calcularTotalOfertas() {
        try {
            jakarta.persistence.Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM oferta_academica WHERE estado = 'ACTIVA'"
            );
            Number resultado = (Number) query.getSingleResult();
            return resultado.longValue();
        } catch (Exception e) {
            System.out.println("Error calculando total ofertas: " + e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Calcula el total de inscripciones
     */
    private Long calcularTotalInscripciones() {
        try {
            jakarta.persistence.Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM inscripciones WHERE estado_inscripcion = true"
            );
            Number resultado = (Number) query.getSingleResult();
            return resultado.longValue();
        } catch (Exception e) {
            System.out.println("Error calculando total inscripciones: " + e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Calcula los ingresos totales de pagos aprobados
     */
    private String calcularIngresosTotales() {
        try {
            jakarta.persistence.Query query = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(monto), 0) FROM pago WHERE estado = 'APROBADO'"
            );
            Number resultado = (Number) query.getSingleResult();
            return String.format("%.2f", resultado.doubleValue());
        } catch (Exception e) {
            System.out.println("Error calculando ingresos totales: " + e.getMessage());
            return "0.00";
        }
    }
    
    /**
     * Crea métricas por defecto en caso de error
     */
    private Map<String, Object> crearMetricasPorDefecto() {
        Map<String, Object> metricas = new HashMap<>();
        metricas.put("totalUsuarios", 0L);
        metricas.put("totalOfertas", 0L);
        metricas.put("totalInscripciones", 0L);
        metricas.put("ingresosTotales", "0.00");
        return metricas;
    }
}