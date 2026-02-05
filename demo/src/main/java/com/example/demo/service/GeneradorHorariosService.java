package com.example.demo.service;

import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.enums.Dias;
import com.example.demo.model.Docente;
import com.example.demo.model.Horario;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.service.DisponibilidadDocenteService.BloqueHorario;

/**
 * Servicio para generar automáticamente propuestas de horarios para ofertas académicas
 * basándose en la disponibilidad de los docentes asignados.
 */
@Service
public class GeneradorHorariosService {
    
    @Autowired
    private DisponibilidadDocenteService disponibilidadService;
    
    // Configuración de límites
    private static final int MAX_HORAS_DIARIAS = 4;
    private static final int MIN_HORAS_CLASE = 2;
    private static final int MAX_PROPUESTAS = 3;
    
    /**
     * Genera propuestas automáticas de horarios para una oferta académica.
     * 
     * @param oferta La oferta académica que necesita horarios
     * @param docente El docente principal asignado
     * @param horasSemanalesRequeridas Horas totales por semana que necesita la oferta
     * @return Lista con hasta 3 propuestas de distribución horaria
     */
    public List<PropuestaHorario> generarPropuestas(OfertaAcademica oferta, Docente docente, double horasSemanalesRequeridas) {
        // Nota: oferta puede ser null cuando se está creando una nueva oferta
        if (docente == null) {
            throw new IllegalArgumentException("El docente es obligatorio");
        }
        
        if (horasSemanalesRequeridas <= 0) {
            throw new IllegalArgumentException("Las horas semanales requeridas deben ser mayores a 0");
        }
        
        List<PropuestaHorario> propuestas = new ArrayList<>();
        
        // Obtener bloques horarios libres del docente
        Map<Dias, List<BloqueHorario>> horariosLibres = disponibilidadService.calcularHorariosLibresSemana(docente);
        
        // Filtrar solo días con bloques suficientes (al menos MIN_HORAS_CLASE)
        Map<Dias, List<BloqueHorario>> diasViables = filtrarDiasViables(horariosLibres);
        
        if (diasViables.isEmpty()) {
            throw new IllegalStateException("El docente no tiene disponibilidad suficiente para esta oferta");
        }
        
        // Estrategia 1: Distribución equilibrada (días alternados)
        PropuestaHorario propuesta1 = generarPropuestaEquilibrada(diasViables, horasSemanalesRequeridas, "Distribución Equilibrada");
        if (propuesta1 != null) {
            propuestas.add(propuesta1);
        }
        
        // Estrategia 2: Concentrada (menos días, más horas por día)
        PropuestaHorario propuesta2 = generarPropuestaConcentrada(diasViables, horasSemanalesRequeridas, "Concentrada");
        if (propuesta2 != null && !propuesta2.esEquivalenteA(propuesta1)) {
            propuestas.add(propuesta2);
        }
        
        // Estrategia 3: Distribuida (más días, menos horas por día)
        PropuestaHorario propuesta3 = generarPropuestaDistribuida(diasViables, horasSemanalesRequeridas, "Distribuida");
        if (propuesta3 != null && !propuesta3.esEquivalenteA(propuesta1) && !propuesta3.esEquivalenteA(propuesta2)) {
            propuestas.add(propuesta3);
        }
        
        // Calcular métricas para cada propuesta
        double cargaActual = disponibilidadService.calcularCargaHorariaSemanal(docente);
        for (PropuestaHorario propuesta : propuestas) {
            propuesta.calcularMetricas(cargaActual);
        }
        
        // Ordenar por score (mejor primero)
        propuestas.sort(Comparator.comparingDouble(PropuestaHorario::getScore).reversed());
        
        return propuestas.stream().limit(MAX_PROPUESTAS).collect(Collectors.toList());
    }
    
    /**
     * Estrategia 1: Distribución equilibrada con días alternados
     */
    private PropuestaHorario generarPropuestaEquilibrada(Map<Dias, List<BloqueHorario>> diasViables, 
                                                          double horasRequeridas, String nombre) {
        PropuestaHorario propuesta = new PropuestaHorario(nombre);
        propuesta.setDescripcion("Clases distribuidas en días alternados para mejor balance");
        
        // Preferir días alternados: Lunes-Miércoles-Viernes o Martes-Jueves-Sábado
        List<Dias> diasPreferidos = Arrays.asList(Dias.LUNES, Dias.MIERCOLES, Dias.VIERNES);
        
        double horasAsignadas = 0;
        int diasUsados = 0;
        
        for (Dias dia : diasPreferidos) {
            if (!diasViables.containsKey(dia)) continue;
            if (horasAsignadas >= horasRequeridas) break;
            
            List<BloqueHorario> bloques = diasViables.get(dia);
            BloqueHorario mejorBloque = seleccionarMejorBloque(bloques, horasRequeridas - horasAsignadas);
            
            if (mejorBloque != null) {
                double horasAUsar = Math.min(MAX_HORAS_DIARIAS, horasRequeridas - horasAsignadas);
                HorarioAsignado horario = crearHorario(mejorBloque, horasAUsar);
                propuesta.agregarHorario(horario);
                horasAsignadas += horasAUsar;
                diasUsados++;
            }
        }
        
        // Si no alcanzó, usar otros días disponibles
        if (horasAsignadas < horasRequeridas) {
            for (Dias dia : diasViables.keySet()) {
                if (diasPreferidos.contains(dia)) continue;
                if (horasAsignadas >= horasRequeridas) break;
                
                List<BloqueHorario> bloques = diasViables.get(dia);
                BloqueHorario mejorBloque = seleccionarMejorBloque(bloques, horasRequeridas - horasAsignadas);
                
                if (mejorBloque != null) {
                    double horasAUsar = Math.min(MAX_HORAS_DIARIAS, horasRequeridas - horasAsignadas);
                    HorarioAsignado horario = crearHorario(mejorBloque, horasAUsar);
                    propuesta.agregarHorario(horario);
                    horasAsignadas += horasAUsar;
                    diasUsados++;
                }
            }
        }
        
        return horasAsignadas >= horasRequeridas ? propuesta : null;
    }
    
    /**
     * Estrategia 2: Concentrada (menos días, más horas por día)
     */
    private PropuestaHorario generarPropuestaConcentrada(Map<Dias, List<BloqueHorario>> diasViables, 
                                                          double horasRequeridas, String nombre) {
        PropuestaHorario propuesta = new PropuestaHorario(nombre);
        propuesta.setDescripcion("Clases concentradas en pocos días con sesiones más largas");
        
        // Ordenar días por mayor disponibilidad
        List<Map.Entry<Dias, List<BloqueHorario>>> diasOrdenados = diasViables.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(
                calcularHorasDisponiblesDia(e2.getValue()),
                calcularHorasDisponiblesDia(e1.getValue())
            ))
            .collect(Collectors.toList());
        
        double horasAsignadas = 0;
        
        for (Map.Entry<Dias, List<BloqueHorario>> entry : diasOrdenados) {
            if (horasAsignadas >= horasRequeridas) break;
            
            Dias dia = entry.getKey();
            List<BloqueHorario> bloques = entry.getValue();
            BloqueHorario mejorBloque = seleccionarMejorBloque(bloques, MAX_HORAS_DIARIAS);
            
            if (mejorBloque != null) {
                double horasAUsar = Math.min(MAX_HORAS_DIARIAS, horasRequeridas - horasAsignadas);
                HorarioAsignado horario = crearHorario(mejorBloque, horasAUsar);
                propuesta.agregarHorario(horario);
                horasAsignadas += horasAUsar;
            }
        }
        
        return horasAsignadas >= horasRequeridas ? propuesta : null;
    }
    
    /**
     * Estrategia 3: Distribuida (más días, menos horas por día)
     */
    private PropuestaHorario generarPropuestaDistribuida(Map<Dias, List<BloqueHorario>> diasViables, 
                                                          double horasRequeridas, String nombre) {
        PropuestaHorario propuesta = new PropuestaHorario(nombre);
        propuesta.setDescripcion("Clases distribuidas en varios días con sesiones cortas");
        
        double horasAsignadas = 0;
        int horasPorDia = MIN_HORAS_CLASE; // Intentar con mínimo de horas por día
        
        // Intentar distribuir en la mayor cantidad de días posible
        List<Dias> diasDisponibles = new ArrayList<>(diasViables.keySet());
        
        for (Dias dia : diasDisponibles) {
            if (horasAsignadas >= horasRequeridas) break;
            
            List<BloqueHorario> bloques = diasViables.get(dia);
            BloqueHorario mejorBloque = seleccionarMejorBloque(bloques, horasPorDia);
            
            if (mejorBloque != null) {
                double horasAUsar = Math.min(horasPorDia, horasRequeridas - horasAsignadas);
                HorarioAsignado horario = crearHorario(mejorBloque, horasAUsar);
                propuesta.agregarHorario(horario);
                horasAsignadas += horasAUsar;
            }
        }
        
        return horasAsignadas >= horasRequeridas ? propuesta : null;
    }
    
    // ============= MÉTODOS AUXILIARES =============
    
    private Map<Dias, List<BloqueHorario>> filtrarDiasViables(Map<Dias, List<BloqueHorario>> horariosLibres) {
        Map<Dias, List<BloqueHorario>> diasViables = new HashMap<>();
        
        for (Map.Entry<Dias, List<BloqueHorario>> entry : horariosLibres.entrySet()) {
            List<BloqueHorario> bloquesViables = entry.getValue().stream()
                .filter(bloque -> bloque.getDuracionHoras() >= MIN_HORAS_CLASE)
                .collect(Collectors.toList());
            
            if (!bloquesViables.isEmpty()) {
                diasViables.put(entry.getKey(), bloquesViables);
            }
        }
        
        return diasViables;
    }
    
    private BloqueHorario seleccionarMejorBloque(List<BloqueHorario> bloques, double horasNecesarias) {
        // Preferir bloques que se ajusten mejor a las horas necesarias
        return bloques.stream()
            .filter(b -> b.getDuracionHoras() >= horasNecesarias)
            .min(Comparator.comparingDouble(b -> Math.abs(b.getDuracionHoras() - horasNecesarias)))
            .orElse(bloques.stream()
                .max(Comparator.comparingDouble(BloqueHorario::getDuracionHoras))
                .orElse(null));
    }
    
    private HorarioAsignado crearHorario(BloqueHorario bloque, double horasAUsar) {
        Time horaInicio = bloque.getHoraInicio();
        
        // Calcular hora fin basada en las horas a usar
        LocalTime inicio = horaInicio.toLocalTime();
        LocalTime fin = inicio.plusMinutes((long)(horasAUsar * 60));
        Time horaFin = Time.valueOf(fin);
        
        return new HorarioAsignado(bloque.getDia(), horaInicio, horaFin);
    }
    
    private double calcularHorasDisponiblesDia(List<BloqueHorario> bloques) {
        return bloques.stream()
            .mapToDouble(BloqueHorario::getDuracionHoras)
            .sum();
    }
    
    // ============= CLASES INTERNAS =============
    
    /**
     * Representa una propuesta completa de distribución horaria
     */
    public static class PropuestaHorario {
        private String nombre;
        private String descripcion;
        private List<HorarioAsignado> horarios;
        private double totalHorasSemana;
        private int cantidadDias;
        private double promedioHorasPorDia;
        private double cargaAdicionalDocente;
        private double porcentajeCargaTotal;
        private double score; // Puntuación de calidad de la propuesta
        
        public PropuestaHorario(String nombre) {
            this.nombre = nombre;
            this.horarios = new ArrayList<>();
        }
        
        public void agregarHorario(HorarioAsignado horario) {
            this.horarios.add(horario);
            this.totalHorasSemana += horario.getDuracionHoras();
            
            // Recalcular estadísticas
            this.cantidadDias = (int) horarios.stream()
                .map(HorarioAsignado::getDia)
                .distinct()
                .count();
            
            this.promedioHorasPorDia = cantidadDias > 0 ? totalHorasSemana / cantidadDias : 0;
        }
        
        public void calcularMetricas(double cargaActualDocente) {
            this.cargaAdicionalDocente = this.totalHorasSemana;
            this.porcentajeCargaTotal = cargaActualDocente + this.totalHorasSemana;
            
            // Calcular score basado en varios factores
            double scoreDistribucion = calcularScoreDistribucion();
            double scoreCarga = calcularScoreCarga();
            double scoreVariedad = calcularScoreVariedad();
            
            this.score = (scoreDistribucion * 0.4) + (scoreCarga * 0.4) + (scoreVariedad * 0.2);
        }
        
        private double calcularScoreDistribucion() {
            // Preferir 2-3 horas por día (óptimo)
            if (promedioHorasPorDia >= 2 && promedioHorasPorDia <= 3) {
                return 100.0;
            } else if (promedioHorasPorDia < 2) {
                return 70.0;
            } else {
                return 85.0;
            }
        }
        
        private double calcularScoreCarga() {
            // Preferir no sobrecargar (menos del 80% de carga)
            if (porcentajeCargaTotal < 30) {
                return 100.0;
            } else if (porcentajeCargaTotal < 40) {
                return 80.0;
            } else {
                return 60.0;
            }
        }
        
        private double calcularScoreVariedad() {
            // Preferir 2-3 días por semana
            if (cantidadDias >= 2 && cantidadDias <= 3) {
                return 100.0;
            } else if (cantidadDias == 1) {
                return 60.0;
            } else {
                return 80.0;
            }
        }
        
        public boolean esEquivalenteA(PropuestaHorario otra) {
            if (otra == null) return false;
            if (this.cantidadDias != otra.cantidadDias) return false;
            if (Math.abs(this.totalHorasSemana - otra.totalHorasSemana) > 0.5) return false;
            
            // Comparar días utilizados
            List<Dias> diasEsta = this.horarios.stream()
                .map(HorarioAsignado::getDia)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            List<Dias> diasOtra = otra.horarios.stream()
                .map(HorarioAsignado::getDia)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            return diasEsta.equals(diasOtra);
        }
        
        public List<Map<String, Object>> toJSON() {
            return horarios.stream().map(h -> {
                Map<String, Object> map = new HashMap<>();
                map.put("dia", h.getDia().name());
                map.put("horaInicio", h.getHoraInicio().toString().substring(0, 5));
                map.put("horaFin", h.getHoraFin().toString().substring(0, 5));
                map.put("duracionHoras", h.getDuracionHoras());
                return map;
            }).collect(Collectors.toList());
        }
        
        // Getters
        public String getNombre() { return nombre; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public List<HorarioAsignado> getHorarios() { return horarios; }
        public double getTotalHorasSemana() { return totalHorasSemana; }
        public int getCantidadDias() { return cantidadDias; }
        public double getPromedioHorasPorDia() { return promedioHorasPorDia; }
        public double getCargaAdicionalDocente() { return cargaAdicionalDocente; }
        public double getPorcentajeCargaTotal() { return porcentajeCargaTotal; }
        public double getScore() { return score; }
    }
    
    /**
     * Representa un horario específico asignado
     */
    public static class HorarioAsignado {
        private Dias dia;
        private Time horaInicio;
        private Time horaFin;
        
        public HorarioAsignado(Dias dia, Time horaInicio, Time horaFin) {
            this.dia = dia;
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
        }
        
        public double getDuracionHoras() {
            long diffMs = horaFin.getTime() - horaInicio.getTime();
            return diffMs / (1000.0 * 60.0 * 60.0);
        }
        
        public Dias getDia() { return dia; }
        public Time getHoraInicio() { return horaInicio; }
        public Time getHoraFin() { return horaFin; }
        
        public Horario toHorario() {
            Horario horario = new Horario();
            horario.setDia(this.dia);
            horario.setHoraInicio(this.horaInicio);
            horario.setHoraFin(this.horaFin);
            return horario;
        }
    }
}
