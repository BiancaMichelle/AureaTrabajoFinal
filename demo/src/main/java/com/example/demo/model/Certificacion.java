package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.enums.EstadoCertificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa el proceso de certificación de un alumno en una oferta académica.
 * Gestiona el flujo: Propuesta automática → Revisión docente → Emisión certificado
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Certificacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCertificacion;
    
    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripciones inscripcion;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCertificacion estado = EstadoCertificacion.PENDIENTE;
    
    // ============ CRITERIOS DE APROBACIÓN (calculados automáticamente) ============
    
    @Column(name = "promedio_general")
    private Double promedioGeneral;  // Promedio de todas las actividades
    
    @Column(name = "porcentaje_asistencia")
    private Double porcentajeAsistencia;  // % de asistencias
    
    @Column(name = "tareas_entregadas")
    private Integer tareasEntregadas;
    
    @Column(name = "tareas_totales")
    private Integer tareasTotales;
    
    @Column(name = "examenes_aprobados")
    private Integer examenesAprobados;
    
    @Column(name = "examenes_totales")
    private Integer examenesTotales;
    
    @Column(name = "cumple_criterios_automaticos")
    private Boolean cumpleCriteriosAutomaticos = false;  // true si pasa todos los filtros
    
    // ============ DECISIÓN DOCENTE ============
    
    @Column(name = "aprobado_docente")
    private Boolean aprobadoDocente;  // null = pendiente, true = aprobado, false = rechazado
    
    @Column(name = "observaciones_docente", length = 1000)
    private String observacionesDocente;  // Razón de aprobación/rechazo manual
    
    @ManyToOne
    @JoinColumn(name = "docente_revisor_id")
    private Docente docenteRevisor;  // Quién revisó
    
    @Column(name = "fecha_revision_docente")
    private LocalDateTime fechaRevisionDocente;
    
    // ============ CERTIFICADO / ACTA ============
    
    @Column(name = "certificado_emitido")
    private Boolean certificadoEmitido = false;  // Si fue incluido en acta de cierre
    
    @Column(name = "numero_certificado", unique = true)
    private String numeroCertificado;  // Ej: "AUREA-2026-CURSO-001234" (número de registro)
    
    @Column(name = "fecha_emision_certificado")
    private LocalDateTime fechaEmisionCertificado;  // Fecha de cierre de notas
    
    @Column(name = "url_certificado_pdf")
    private String urlCertificadoPdf;  // Ruta al acta de cierre (PDF único para toda la oferta)
    
    // ============ AUDITORÍA ============
    
    @Column(name = "fecha_calculo_automatico")
    private LocalDateTime fechaCalculoAutomatico;  // Cuándo el sistema calculó los criterios
    
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
    
    // ============ MÉTODOS AUXILIARES ============
    
    /**
     * Verifica si el alumno cumple con todos los requisitos mínimos configurados
     */
    public boolean verificarCriteriosMinimos(
            Double promedioMinimo, 
            Double asistenciaMinima,
            Double porcentajeTareasMinimo,
            Double porcentajeExamenesMinimo) {
        
        boolean cumplePromedio = this.promedioGeneral != null && this.promedioGeneral >= promedioMinimo;
        boolean cumpleAsistencia = this.porcentajeAsistencia != null && this.porcentajeAsistencia >= asistenciaMinima;
        
        double porcentajeTareas = (tareasTotales > 0) ? 
            (double) tareasEntregadas / tareasTotales * 100 : 100;
        boolean cumpleTareas = porcentajeTareas >= porcentajeTareasMinimo;
        
        double porcentajeExamenes = (examenesTotales > 0) ? 
            (double) examenesAprobados / examenesTotales * 100 : 100;
        boolean cumpleExamenes = porcentajeExamenes >= porcentajeExamenesMinimo;
        
        return cumplePromedio && cumpleAsistencia && cumpleTareas && cumpleExamenes;
    }
    
    /**
     * Marca como aprobado por el docente
     */
    public void aprobarPorDocente(Docente docente, String observaciones) {
        this.aprobadoDocente = true;
        this.docenteRevisor = docente;
        this.observacionesDocente = observaciones;
        this.fechaRevisionDocente = LocalDateTime.now();
        this.estado = EstadoCertificacion.APROBADO_DOCENTE;
        this.fechaActualizacion = LocalDateTime.now();
    }
    
    /**
     * Marca como rechazado por el docente
     */
    public void rechazarPorDocente(Docente docente, String observaciones) {
        this.aprobadoDocente = false;
        this.docenteRevisor = docente;
        this.observacionesDocente = observaciones;
        this.fechaRevisionDocente = LocalDateTime.now();
        this.estado = EstadoCertificacion.RECHAZADO_DOCENTE;
        this.fechaActualizacion = LocalDateTime.now();
    }
    
    /**
     * Genera el número de certificado único
     */
    public void generarNumeroCertificado(OfertaAcademica oferta) {
        String tipo = (oferta instanceof Curso) ? "CURSO" : 
                      (oferta instanceof Formacion) ? "FORMACION" : "SEMINARIO";
        int anio = LocalDateTime.now().getYear();
        String numero = String.format("%06d", this.idCertificacion);
        this.numeroCertificado = String.format("AUREA-%d-%s-%s", anio, tipo, numero);
    }
}
