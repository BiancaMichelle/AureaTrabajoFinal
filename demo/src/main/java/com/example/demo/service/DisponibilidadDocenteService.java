package com.example.demo.service;



import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.Dias;
import com.example.demo.model.Docente;
import com.example.demo.model.DisponibilidadDocente;
import com.example.demo.model.Horario;
import com.example.demo.repository.DisponibilidadDocenteRepository;
import com.example.demo.repository.HorarioRepository;

@Service
public class DisponibilidadDocenteService {
    
    @Autowired
    private DisponibilidadDocenteRepository disponibilidadRepository;
    
    @Autowired
    private HorarioRepository horarioRepository;
    
    /**
     * Obtiene todas las disponibilidades de un docente
     */
    public List<DisponibilidadDocente> obtenerDisponibilidades(Docente docente) {
        return disponibilidadRepository.findByDocenteOrderByDiaAndHora(docente);
    }
    
    /**
     * Guarda o actualiza las disponibilidades de un docente
     */
    @Transactional
    public void actualizarDisponibilidades(Docente docente, List<Map<String, String>> horariosData) {
        // Eliminar disponibilidades anteriores
        disponibilidadRepository.deleteByDocente(docente);
        
        if (horariosData == null || horariosData.isEmpty()) {
            return;
        }
        
        // Crear nuevas disponibilidades
        for (Map<String, String> data : horariosData) {
            DisponibilidadDocente disponibilidad = new DisponibilidadDocente();
            
            // Convertir día
            String diaString = data.get("diaSemana");
            Dias dia = convertirStringADias(diaString);
            disponibilidad.setDia(dia);
            
            // Convertir horarios
            String horaInicioStr = data.get("horaInicio");
            String horaFinStr = data.get("horaFin");
            
            if (horaInicioStr != null && horaFinStr != null) {
                Time horaInicio = Time.valueOf(horaInicioStr + ":00");
                Time horaFin = Time.valueOf(horaFinStr + ":00");
                
                disponibilidad.setHoraInicio(horaInicio);
                disponibilidad.setHoraFin(horaFin);
                disponibilidad.setDocente(docente);
                
                disponibilidadRepository.save(disponibilidad);
            }
        }
    }
    
    /**
     * Obtiene los horarios ocupados de un docente (clases asignadas en ofertas)
     */
    public List<Horario> obtenerHorariosOcupados(Docente docente) {
        return horarioRepository.findByDocente(docente);
    }
    
    /**
     * Calcula los bloques horarios libres de un docente en un día específico
     * Resta los horarios ocupados de la disponibilidad
     */
    public List<BloqueHorario> calcularHorariosLibres(Docente docente, Dias dia) {
        // Obtener disponibilidades del día
        List<DisponibilidadDocente> disponibilidades = disponibilidadRepository.findByDocenteAndDia(docente, dia);
        
        if (disponibilidades.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Obtener horarios ocupados del día
        List<Horario> ocupados = horarioRepository.findByDocente(docente).stream()
            .filter(h -> h.getDia().equals(dia))
            .collect(Collectors.toList());
        
        List<BloqueHorario> libres = new ArrayList<>();
        
        // Para cada disponibilidad, restar los horarios ocupados
        for (DisponibilidadDocente disp : disponibilidades) {
            List<BloqueHorario> bloquesLibres = calcularBloquesLibres(disp, ocupados);
            libres.addAll(bloquesLibres);
        }
        
        return libres;
    }
    
    /**
     * Calcula todos los bloques horarios libres de un docente para toda la semana
     */
    public Map<Dias, List<BloqueHorario>> calcularHorariosLibresSemana(Docente docente) {
        return java.util.Arrays.stream(Dias.values())
            .collect(Collectors.toMap(
                dia -> dia,
                dia -> calcularHorariosLibres(docente, dia)
            ));
    }
    
    /**
     * Calcula la carga horaria semanal actual del docente (horas ocupadas)
     */
    public double calcularCargaHorariaSemanal(Docente docente) {
        List<Horario> horariosOcupados = obtenerHorariosOcupados(docente);
        
        double totalHoras = 0.0;
        for (Horario h : horariosOcupados) {
            if (h.getHoraInicio() != null && h.getHoraFin() != null) {
                long diffMs = h.getHoraFin().getTime() - h.getHoraInicio().getTime();
                totalHoras += diffMs / (1000.0 * 60.0 * 60.0);
            }
        }
        
        return Math.round(totalHoras * 100.0) / 100.0;
    }
    
    /**
     * Calcula la disponibilidad total semanal del docente (horas disponibles)
     */
    public double calcularDisponibilidadTotalSemanal(Docente docente) {
        List<DisponibilidadDocente> disponibilidades = obtenerDisponibilidades(docente);
        
        double totalHoras = 0.0;
        for (DisponibilidadDocente d : disponibilidades) {
            totalHoras += d.getDuracionHoras();
        }
        
        return Math.round(totalHoras * 100.0) / 100.0;
    }
    
    /**
     * Calcula el porcentaje de ocupación del docente
     */
    public double calcularPorcentajeOcupacion(Docente docente) {
        double disponibilidadTotal = calcularDisponibilidadTotalSemanal(docente);
        
        if (disponibilidadTotal == 0) {
            return 0.0;
        }
        
        double cargaActual = calcularCargaHorariaSemanal(docente);
        return Math.round((cargaActual / disponibilidadTotal) * 10000.0) / 100.0;
    }
    
    /**
     * Verifica si un horario específico está disponible para el docente
     */
    public boolean estaDisponible(Docente docente, Dias dia, Time horaInicio, Time horaFin) {
        // Verificar que esté dentro de su disponibilidad
        List<DisponibilidadDocente> disponibilidades = disponibilidadRepository.findByDocenteAndDia(docente, dia);
        
        boolean dentroDisponibilidad = disponibilidades.stream()
            .anyMatch(d -> d.contieneHorario(dia, horaInicio, horaFin));
        
        if (!dentroDisponibilidad) {
            return false;
        }
        
        // Verificar que no esté ocupado
        List<Horario> ocupados = horarioRepository.findByDocente(docente).stream()
            .filter(h -> h.getDia().equals(dia))
            .collect(Collectors.toList());
        
        for (Horario ocupado : ocupados) {
            if (seSuperponen(horaInicio, horaFin, ocupado.getHoraInicio(), ocupado.getHoraFin())) {
                return false;
            }
        }
        
        return true;
    }
    
    // ============= MÉTODOS PRIVADOS AUXILIARES =============
    
    private List<BloqueHorario> calcularBloquesLibres(DisponibilidadDocente disponibilidad, List<Horario> ocupados) {
        List<BloqueHorario> libres = new ArrayList<>();
        
        Time inicioDisp = disponibilidad.getHoraInicio();
        Time finDisp = disponibilidad.getHoraFin();
        if (inicioDisp == null || finDisp == null) {
            return libres;
        }
        
        // Ordenar horarios ocupados por hora de inicio
        List<Horario> ocupadosOrdenados = ocupados.stream()
            .filter(h -> h.getHoraInicio() != null && h.getHoraFin() != null)
            .sorted((h1, h2) -> h1.getHoraInicio().compareTo(h2.getHoraInicio()))
            .collect(Collectors.toList());
        
        Time inicioActual = inicioDisp;
        
        for (Horario ocupado : ocupadosOrdenados) {
            if (ocupado.getHoraInicio() == null || ocupado.getHoraFin() == null) {
                continue;
            }
            // Si el horario ocupado está completamente fuera de la disponibilidad, ignorar
            if (ocupado.getHoraFin().before(inicioDisp) || ocupado.getHoraInicio().after(finDisp)) {
                continue;
            }
            
            // Si hay un hueco antes del horario ocupado, agregarlo
            if (inicioActual.before(ocupado.getHoraInicio())) {
                libres.add(new BloqueHorario(disponibilidad.getDia(), inicioActual, ocupado.getHoraInicio()));
            }
            
            // Mover el inicio actual al final del horario ocupado
            if (ocupado.getHoraFin().after(inicioActual)) {
                inicioActual = ocupado.getHoraFin();
            }
        }
        
        // Si queda un hueco al final, agregarlo
        if (inicioActual.before(finDisp)) {
            libres.add(new BloqueHorario(disponibilidad.getDia(), inicioActual, finDisp));
        }
        
        return libres;
    }
    
    private boolean seSuperponen(Time inicio1, Time fin1, Time inicio2, Time fin2) {
        if (inicio1 == null || fin1 == null || inicio2 == null || fin2 == null) {
            return false;
        }
        return !(fin1.before(inicio2) || inicio1.after(fin2));
    }
    
    private Dias convertirStringADias(String diaString) {
        if (diaString == null) return null;
        
        switch (diaString.toUpperCase()) {
            case "LUNES": return Dias.LUNES;
            case "MARTES": return Dias.MARTES;
            case "MIÉRCOLES": 
            case "MIERCOLES": return Dias.MIERCOLES;
            case "JUEVES": return Dias.JUEVES;
            case "VIERNES": return Dias.VIERNES;
            case "SÁBADO":
            case "SABADO": return Dias.SABADO;
            case "DOMINGO": return Dias.DOMINGO;
            default: return Dias.LUNES;
        }
    }
    
    /**
     * Clase interna para representar un bloque horario libre
     */
    public static class BloqueHorario {
        private Dias dia;
        private Time horaInicio;
        private Time horaFin;
        
        public BloqueHorario(Dias dia, Time horaInicio, Time horaFin) {
            this.dia = dia;
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
        }
        
        public Dias getDia() { return dia; }
        public Time getHoraInicio() { return horaInicio; }
        public Time getHoraFin() { return horaFin; }
        
        public long getDuracionMinutos() {
            return (horaFin.getTime() - horaInicio.getTime()) / (1000 * 60);
        }
        
        public double getDuracionHoras() {
            return getDuracionMinutos() / 60.0;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s - %s (%.1fh)", dia, horaInicio, horaFin, getDuracionHoras());
        }
    }

        /**
     * Calcula la disponibilidad libre semanal del docente (disponibilidad menos ocupados)
     */
    public double calcularDisponibilidadLibreSemanal(Docente docente) {
        Map<Dias, List<BloqueHorario>> libres = calcularHorariosLibresSemana(docente);
        double total = 0.0;
        for (List<BloqueHorario> bloques : libres.values()) {
            for (BloqueHorario b : bloques) {
                total += b.getDuracionHoras();
            }
        }
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Calcula la disponibilidad libre por dia (horas)
     */
    public Map<Dias, Double> calcularDisponibilidadLibrePorDia(Docente docente) {
        Map<Dias, List<BloqueHorario>> libres = calcularHorariosLibresSemana(docente);
        return libres.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Math.round(e.getValue().stream().mapToDouble(BloqueHorario::getDuracionHoras).sum() * 100.0) / 100.0
            ));
    }
}
