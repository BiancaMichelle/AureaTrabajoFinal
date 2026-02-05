package com.example.demo.model;

import java.sql.Time;

import com.example.demo.enums.Dias;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa la disponibilidad horaria de un docente.
 * Indica los bloques de tiempo en los que el docente PUEDE trabajar.
 * Es independiente de los horarios asignados a ofertas académicas.
 */
@Entity
@Table(name = "disponibilidad_docente")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DisponibilidadDocente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDisponibilidad;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private Dias dia;
    
    @Column(name = "hora_inicio", nullable = false)
    private Time horaInicio;
    
    @Column(name = "hora_fin", nullable = false)
    private Time horaFin;
    
    /**
     * Verifica si esta disponibilidad se superpone con otra
     */
    public boolean seSuperponeCon(DisponibilidadDocente otra) {
        if (otra == null || !this.dia.equals(otra.dia)) {
            return false;
        }
        
        // Verificar si hay superposición de horarios
        return !(this.horaFin.before(otra.horaInicio) || this.horaInicio.after(otra.horaFin));
    }
    
    /**
     * Verifica si esta disponibilidad contiene completamente un horario dado
     */
    public boolean contieneHorario(Dias dia, Time horaInicio, Time horaFin) {
        if (!this.dia.equals(dia)) {
            return false;
        }
        
        return !this.horaInicio.after(horaInicio) && !this.horaFin.before(horaFin);
    }
    
    /**
     * Calcula la duración en minutos de este bloque de disponibilidad
     */
    public long getDuracionMinutos() {
        if (horaInicio == null || horaFin == null) {
            return 0;
        }
        
        long diffMs = horaFin.getTime() - horaInicio.getTime();
        return diffMs / (1000 * 60); // Convertir de milisegundos a minutos
    }
    
    /**
     * Calcula la duración en horas de este bloque de disponibilidad
     */
    public double getDuracionHoras() {
        return getDuracionMinutos() / 60.0;
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s - %s", dia, horaInicio, horaFin);
    }
}
