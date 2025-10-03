package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Curso extends OfertaAcademica {
    
    @OneToMany(mappedBy = "curso")
    private List<Horario> horarios;
    
    private String temario;
    
    @ManyToMany
    @JoinTable(
        name = "curso_docente",
        joinColumns = @JoinColumn(name = "curso_id"),
        inverseJoinColumns = @JoinColumn(name = "docente_id")
    )
    private List<Docente> docentes;
    
    @ManyToMany
    @JoinTable(
        name = "curso_requisitos",
        joinColumns = @JoinColumn(name = "curso_id"),
        inverseJoinColumns = @JoinColumn(name = "requisito_id")
    )
    private List<OfertaAcademica> requisitos;
    
    private Double costoCuota;
    private Double costoMora;
    private Integer nrCuotas;
   // private LocalDate vencimientoCuota;
    private Integer diaVencimiento; // Día del mes límite para pago sin mora
    
    @OneToMany(mappedBy = "curso")
    private List<Modulo> modulos;
    
    @OneToMany(mappedBy = "curso")
    private List<Clase> clases;

    /**
     * Información de docentes para la tabla
     */
    public String getDocentesTexto() {
        if (docentes == null || docentes.isEmpty()) {
            return "Sin docentes";
        }
        if (docentes.size() == 1) {
            Docente d = docentes.get(0);
            return d.getNombre() + " " + d.getApellido();
        }
        return docentes.size() + " docentes";
    }
    /**
     * Lista completa de docentes
     */
    public String getDocentesCompleto() {
        if (docentes == null || docentes.isEmpty()) {
            return "Sin docentes asignados";
        }
        return docentes.stream()
                .map(d -> d.getNombre() + " " + d.getApellido())
                .collect(Collectors.joining(", "));
    }
    /**
     * Información de horarios condensada
     */
    public String getHorariosTexto() {
        if (horarios == null || horarios.isEmpty()) {
            return "Sin horarios";
        }
        if (horarios.size() == 1) {
            Horario h = horarios.get(0);
            return h.getDia() + " " + h.getHoraInicio() + "-" + h.getHoraFin();
        }
        return horarios.size() + " horarios";
    }
    /**
     * Información de cuotas para mostrar
     */
    public String getInfoCuotas() {
        if (costoCuota == null || nrCuotas == null) {
            return "Sin cuotas";
        }
        return nrCuotas + " cuotas de $" + String.format("%.2f", costoCuota);
    }
    /**
     * Información de mora para administración
     */
    public String getInfoMora() {
        if (costoMora == null || diaVencimiento == null) {
            return "Sin mora definida";
        }
        return "Mora: $" + String.format("%.2f", costoMora) + 
               " después del día " + diaVencimiento;
    }
}
