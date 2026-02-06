package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.enums.EstadoOferta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class EstadisticasService {
    
    private static final Logger logger = LoggerFactory.getLogger(EstadisticasService.class);

    @Autowired
    private OfertaAcademicaRepository ofertaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private InscripcionRepository inscripcionRepository;
    
    @Autowired
    private RolRepository rolRepository;
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;

    /**
     * Obtiene las métricas generales del sistema
     */
    public Map<String, Object> obtenerMetricasGenerales() {
        Map<String, Object> metricas = new HashMap<>();
        
        try {
            // Conteo de ofertas académicas por estado
            long ofertasActivas = ofertaRepository.countByEstado(EstadoOferta.ACTIVA);
            long ofertasInactivas = ofertaRepository.countByEstado(EstadoOferta.DE_BAJA);
            
            // Conteo de usuarios por rol
            Map<String, Long> usuariosPorRol = contarUsuariosPorRol();
            
            // Conteo total de inscripciones
            long totalInscripciones = inscripcionRepository.count();
            
            // Ingresos del último mes
            double ingresosMensuales = calcularIngresosMensuales();
            
            // Usuarios activos
            long totalUsuarios = usuarioRepository.findAll().stream()
                .filter(Usuario::isEstado)
                .count();
            
            metricas.put("ofertasActivas", ofertasActivas);
            metricas.put("ofertasInactivas", ofertasInactivas);
            metricas.put("totalOfertas", ofertaRepository.count());
            metricas.put("usuariosPorRol", usuariosPorRol);
            metricas.put("ofertasPorTipo", obtenerOfertasPorTipo());
            metricas.put("totalInscripciones", totalInscripciones);
            metricas.put("ingresosMensuales", ingresosMensuales);
            metricas.put("totalUsuarios", totalUsuarios);
            metricas.put("ingresosTotales", ingresosMensuales); // Para compatibilidad con template
            
            return metricas;
            
        } catch (Exception e) {
            System.err.println("Error obteniendo métricas: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Cuenta usuarios por rol
     */
    private Map<String, Long> contarUsuariosPorRol() {
        Map<String, Long> usuariosPorRol = new HashMap<>();
        
        try {
            List<Rol> roles = rolRepository.findAll();
            
            for (Rol rol : roles) {
                List<Usuario> usuarios = usuarioRepository.findByRolesNombre(rol.getNombre());
                long cantidad = usuarios.stream()
                    .filter(Usuario::isEstado) // Filtrar solo usuarios activos
                    .count();
                usuariosPorRol.put(rol.getNombre(), cantidad);
            }
            

            
        } catch (Exception e) {
            System.err.println("Error contando usuarios por rol: " + e.getMessage());
        }
        
        return usuariosPorRol;
    }

    /**
     * Calcula ingresos del último mes (inscripciones + cuotas)
     */
    private double calcularIngresosMensuales() {
        try {
            LocalDate inicioMes = LocalDate.now().minusMonths(1);
            LocalDate finMes = LocalDate.now();
            
            // Ingresos por inscripciones del último mes
            List<Inscripciones> inscripcionesMes = inscripcionRepository
                .findAll().stream()
                .filter(i -> i.getFechaInscripcion() != null && 
                           !i.getFechaInscripcion().isBefore(inicioMes) && 
                           !i.getFechaInscripcion().isAfter(finMes))
                .collect(Collectors.toList());
            
            double ingresosInscripciones = inscripcionesMes.stream()
                .filter(i -> i.getOferta() != null && i.getOferta().getCostoInscripcion() != null)
                .mapToDouble(i -> i.getOferta().getCostoInscripcion())
                .sum();
            
            // Ingresos por cuotas del último mes (usando fechas de pago)
            LocalDateTime inicioMesDT = inicioMes.atStartOfDay();
            LocalDateTime finMesDT = finMes.atTime(23, 59, 59);
            
            List<Pago> pagosMes = pagoRepository.findAll().stream()
                .filter(p -> p.getFechaPago() != null && 
                           !p.getFechaPago().isBefore(inicioMesDT) && 
                           !p.getFechaPago().isAfter(finMesDT))
                .collect(Collectors.toList());
            
            double ingresosCuotas = pagosMes.stream()
                .filter(p -> p.getMonto() != null)
                .mapToDouble(p -> p.getMonto().doubleValue())
                .sum();
            
            return ingresosInscripciones + ingresosCuotas;
            
        } catch (Exception e) {
            System.err.println("Error calculando ingresos mensuales: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Analiza la demanda de ofertas académicas
     */
    public List<Map<String, Object>> analizarDemandaOfertas() {
        try {
            List<EstadoOferta> estados = Arrays.asList(EstadoOferta.ACTIVA, EstadoOferta.ENCURSO);
            List<OfertaAcademica> ofertasActivas = ofertaRepository.findByEstadoIn(estados);

            // Logging para depuración
            System.out.println("🔍 Ofertas consideradas para demanda: " + ofertasActivas.size());
            for (OfertaAcademica o : ofertasActivas) {
                System.out.println("   - Oferta ID: " + o.getIdOferta() + ", estado=" + o.getEstado() + ", nombre=" + o.getNombre());
            }

            List<Map<String, Object>> demandaOfertas = new ArrayList<>();
            
            for (OfertaAcademica oferta : ofertasActivas) {
                Map<String, Object> ofertaData = new HashMap<>();
                
                // Contar inscripciones para esta oferta
                long cantidadInscritos = inscripcionRepository.countByOfertaAndEstadoInscripcionTrue(oferta);
                
                // Calcular nivel de demanda
                String nivelDemanda = calcularNivelDemanda(cantidadInscritos, oferta.getCupos());
                
                // Calcular tasa de ocupación
                double tasaOcupacion = oferta.getCupos() != null && oferta.getCupos() > 0 
                    ? (double) cantidadInscritos / oferta.getCupos() 
                    : 0.0;
                
                ofertaData.put("idOferta", oferta.getIdOferta());
                ofertaData.put("tituloOferta", oferta.getNombre());
                ofertaData.put("cantidadInscritos", cantidadInscritos);
                ofertaData.put("cupoMaximo", oferta.getCupos());
                ofertaData.put("nivelDemanda", nivelDemanda);
                ofertaData.put("tasaOcupacion", tasaOcupacion);
                ofertaData.put("fechaInicio", oferta.getFechaInicio());
                
                demandaOfertas.add(ofertaData);
            }
            
            // Ordenar por cantidad de inscritos (mayor a menor)
            demandaOfertas.sort((a, b) -> 
                Long.compare((Long) b.get("cantidadInscritos"), (Long) a.get("cantidadInscritos")));
            
            return demandaOfertas;
            
        } catch (Exception e) {
            System.err.println("Error analizando demanda: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Calcula el nivel de demanda según inscripciones vs cupos
     */
    private String calcularNivelDemanda(long inscripciones, Integer cupos) {
        if (cupos == null || cupos == 0) {
            return inscripciones > 0 ? "ALTA" : "SIN_DEMANDA";
        }
        
        double porcentaje = (double) inscripciones / cupos;
        
        if (porcentaje == 0) return "SIN_DEMANDA";
        else if (porcentaje < 0.3) return "BAJA";
        else if (porcentaje > 0.8) return "ALTA";
        else return "MEDIA";
    }

    /**
     * Analiza la deserción estudiantil basada únicamente en bajas de ofertas
     */
    public Map<String, Object> analizarDesercion() {
        Map<String, Object> analisis = new HashMap<>();
        
        try {
            // Total de inscripciones registradas
            long totalInscripciones = inscripcionRepository.count();
            
            // Inscripciones activas (no dadas de baja)
            long inscripcionesActivas = inscripcionRepository.findAll().stream()
                .filter(i -> i.getEstadoInscripcion() != null && i.getEstadoInscripcion())
                .count();
            
            // Bajas (inscripciones inactivas) - ESTA ES LA DESERCIÓN REAL
            long inscripcionesBaja = totalInscripciones - inscripcionesActivas;
            
            // Calcular tasa de deserción: bajas / total de inscripciones
            double tasaDesercion = totalInscripciones > 0 
                ? (double) inscripcionesBaja / totalInscripciones 
                : 0.0;
            
            // Estudiantes únicos con inscripciones activas
            long estudiantesConInscripcionesActivas = inscripcionRepository.findAll().stream()
                .filter(i -> i.getEstadoInscripcion() != null && i.getEstadoInscripcion())
                .map(i -> i.getAlumno().getId())
                .distinct()
                .count();
            
            // Estudiantes únicos que se dieron de baja
            long estudiantesConBajas = inscripcionRepository.findAll().stream()
                .filter(i -> i.getEstadoInscripcion() != null && !i.getEstadoInscripcion())
                .map(i -> i.getAlumno().getId())
                .distinct()
                .count();
            
            analisis.put("totalInscripciones", totalInscripciones);
            analisis.put("inscripcionesActivas", inscripcionesActivas);
            analisis.put("inscripcionesBaja", inscripcionesBaja); // BAJAS = DESERCIÓN
            analisis.put("tasaDesercion", tasaDesercion);
            analisis.put("estudiantesActivos", estudiantesConInscripcionesActivas);
            analisis.put("estudiantesConBajas", estudiantesConBajas);
            analisis.put("estudiantesInactivos", estudiantesConBajas); // Para compatibilidad con template
            analisis.put("totalEstudiantes", estudiantesConInscripcionesActivas + estudiantesConBajas);
            
        } catch (Exception e) {
            System.err.println("Error analizando deserción: " + e.getMessage());
        }
        
        return analisis;
    }

    /**
     * Proyecta crecimiento basado en inscripciones mensuales
     */
    public Map<String, Object> proyectarCrecimiento() {
        Map<String, Object> proyeccion = new HashMap<>();
        
        try {
            // Inscripciones por mes del año actual
            Map<Integer, Long> inscripcionesPorMes = new HashMap<>();
            
            for (int mes = 1; mes <= 12; mes++) {
                LocalDate inicioMes = LocalDate.of(LocalDate.now().getYear(), mes, 1);
                LocalDate finMes = inicioMes.plusMonths(1).minusDays(1);
                
                long inscripcionesMes = inscripcionRepository.findAll().stream()
                    .filter(i -> i.getFechaInscripcion() != null &&
                               !i.getFechaInscripcion().isBefore(inicioMes) &&
                               !i.getFechaInscripcion().isAfter(finMes))
                    .count();
                
                inscripcionesPorMes.put(mes, inscripcionesMes);
            }
            
            // Calcular tendencia (simplificada)
            List<Long> valores = new ArrayList<>(inscripcionesPorMes.values());
            double promedio = valores.stream().mapToLong(Long::longValue).average().orElse(0.0);
            
            proyeccion.put("inscripcionesPorMes", inscripcionesPorMes);
            proyeccion.put("promedioMensual", promedio);
            proyeccion.put("proyeccionProximoMes", Math.round(promedio * 1.1)); // 10% de crecimiento estimado
            
        } catch (Exception e) {
            System.err.println("Error proyectando crecimiento: " + e.getMessage());
        }
        
        return proyeccion;
    }

    /**
     * Obtiene el número de ofertas por tipo/categoría
     */
    private Map<String, Long> obtenerOfertasPorTipo() {
        try {
            // Obtener todas las ofertas activas y en curso
            List<EstadoOferta> estados = Arrays.asList(EstadoOferta.ACTIVA, EstadoOferta.ENCURSO);
            List<OfertaAcademica> ofertasActivas = ofertaRepository.findByEstadoIn(estados);
            
            // Agrupar por el método getTipoOferta() que devuelve "Curso", "Formación", etc.
            return ofertasActivas.stream()
                .collect(Collectors.groupingBy(OfertaAcademica::getTipoOferta, Collectors.counting()));
            
        } catch (Exception e) {
            System.err.println("Error en obtenerOfertasPorTipo: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 1.1 Oferta Académica (Estado y Rendimiento)
     */
    public Map<String, Object> obtenerEstadisticasOfertasDetalladas() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<OfertaAcademica> todas = ofertaRepository.findAll();
            
            // Cantidad por estado
            Map<EstadoOferta, Long> porEstado = todas.stream()
                .collect(Collectors.groupingBy(OfertaAcademica::getEstado, Collectors.counting()));
            
            stats.put("porEstado", porEstado);
            
            // Tasa de ocupación promedio
            double ocupacionPromedio = todas.stream()
                .filter(o -> o.getCupos() != null && o.getCupos() > 0)
                .mapToDouble(o -> {
                    long inscritos = inscripcionRepository.findByOfertaIdOferta(o.getIdOferta()).size();
                    return (double) inscritos / o.getCupos();
                })
                .average().orElse(0.0);
            stats.put("ocupacionPromedio", ocupacionPromedio);

            // Ofertas con alta demanda (> 80% ocupación)
            List<Map<String, Object>> altaDemanda = todas.stream()
                .map(o -> {
                    long inscritos = inscripcionRepository.findByOfertaIdOferta(o.getIdOferta()).size();
                    double ocupacion = (o.getCupos() != null && o.getCupos() > 0) ? (double) inscritos / o.getCupos() : 0;
                    if (ocupacion > 0.8) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("nombre", o.getNombre());
                        map.put("ocupacion", ocupacion);
                        map.put("inscritos", inscritos);
                        return map;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            stats.put("altaDemanda", altaDemanda);

             // Ofertas con baja demanda (< 10% ocupación)
            List<Map<String, Object>> bajaDemanda = todas.stream()
                .map(o -> {
                    long inscritos = inscripcionRepository.findByOfertaIdOferta(o.getIdOferta()).size();
                    double ocupacion = (o.getCupos() != null && o.getCupos() > 0) ? (double) inscritos / o.getCupos() : 0;
                    if (ocupacion < 0.1) {
                         Map<String, Object> map = new HashMap<>();
                        map.put("nombre", o.getNombre());
                        map.put("ocupacion", ocupacion);
                        map.put("inscritos", inscritos);
                        return map;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            stats.put("bajaDemanda", bajaDemanda);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stats;
    }

    /**
     * 1.2 Inscripciones y Alumnos (Evolución)
     */
    public Map<String, Object> obtenerEstadisticasInscripciones() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Inscripciones> todas = inscripcionRepository.findAll();
            logger.info("obtenerEstadisticasInscripciones: total inscripciones = {}", todas.size());
            // Evolución mensual (últimos 12 meses)
            Map<String, Long> evolucionMensual = new TreeMap<>();
            LocalDate hoy = LocalDate.now();
            for (int i = 11; i >= 0; i--) {
                LocalDate fecha = hoy.minusMonths(i);
                String key = fecha.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                long count = todas.stream()
                    .filter(ins -> ins.getFechaInscripcion() != null && 
                                   ins.getFechaInscripcion().getMonth() == fecha.getMonth() && 
                                   ins.getFechaInscripcion().getYear() == fecha.getYear())
                    .count();
                evolucionMensual.put(key, count);
            }
            stats.put("evolucionMensual", evolucionMensual);
            logger.info("evolucionMensual: {}", evolucionMensual);

            // Nuevos vs Recurrentes (Simplificado: si el alumno tiene > 1 inscripción es recurrente)
            // Se usa Object para la clave para evitar problemas con UUID vs Long si cambia el modelo
            Map<Object, Long> conteoPorAlumno = todas.stream()
                .filter(i -> i.getAlumno() != null)
                .collect(Collectors.groupingBy(i -> i.getAlumno().getId(), Collectors.counting()));
            
            long recurrentes = conteoPorAlumno.values().stream().filter(c -> c > 1).count();
            long nuevos = conteoPorAlumno.values().stream().filter(c -> c == 1).count();
            
            stats.put("alumnosNuevos", nuevos);
            stats.put("alumnosRecurrentes", recurrentes);
            logger.info("alumnosNuevos = {}, alumnosRecurrentes = {}", nuevos, recurrentes);
            
            // Tasa de abandono (Inscripciones inactivas / Total)
            long total = todas.size();
            long inactivas = todas.stream().filter(i -> i.getEstadoInscripcion() != null && !i.getEstadoInscripcion()).count();
            double tasaAbandono = total > 0 ? (double) inactivas / total : 0;
            stats.put("tasaAbandonoGlobal", tasaAbandono);
            logger.info("tasaAbandonoGlobal = {}", tasaAbandono);

        } catch (Exception e) {
            logger.error("Error en obtenerEstadisticasInscripciones", e);
        }
        return stats;
    }

    /**
     * 1.6 Estadísticas Económicas
     */
    public Map<String, Object> obtenerEstadisticasEconomicas() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Pago> pagos = pagoRepository.findAll();
            
            // Ingresos totales
            double ingresosTotales = pagos.stream()
                .filter(p -> p.getMonto() != null)
                .mapToDouble(p -> p.getMonto().doubleValue())
                .sum();
            stats.put("ingresosTotalesHist", ingresosTotales);

            // Ingresos por Oferta
            Map<String, Double> ingresosPorOferta = pagos.stream()
                .filter(p -> p.getMonto() != null)
                .map(p -> {
                    // Intentar obtener oferta directamente o a través de inscripción
                    String nombreOferta = "Desconocido";
                    if (p.getOferta() != null) {
                        nombreOferta = p.getOferta().getNombre();
                    } else if (p.getInscripcion() != null && p.getInscripcion().getOferta() != null) {
                        nombreOferta = p.getInscripcion().getOferta().getNombre();
                    }
                    return new AbstractMap.SimpleEntry<>(nombreOferta, p.getMonto().doubleValue());
                })
                .collect(Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.summingDouble(Map.Entry::getValue)
                ));
            stats.put("ingresosPorOferta", ingresosPorOferta);

            // Estado de Pagos (Pendiente vs Aprobado)
            Map<String, Long> estadoPagos = pagos.stream()
                .filter(p -> p.getEstadoPago() != null)
                .collect(Collectors.groupingBy(p -> p.getEstadoPago().toString(), Collectors.counting()));
            stats.put("estadoPagos", estadoPagos);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return stats;
    }
}
