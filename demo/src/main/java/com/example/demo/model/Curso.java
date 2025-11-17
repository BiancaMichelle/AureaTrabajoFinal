package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
    private Integer diaVencimiento; // Día del mes límite para pago sin mora
    
    @OneToMany(mappedBy = "curso")
    private List<Modulo> modulos;
    
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
    public List<Horario> getHorariosCurso() {
        // Esto dependerá de cómo implementes la relación en OfertaAcademica
        // Si agregas la relación en OfertaAcademica, sería:
        // return this.getHorarios();
        return Collections.emptyList(); // temporal
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

    /**
     * Modifica los datos específicos del curso
     */
    public void modificarDatosCurso(String temario, List<Docente> docentes, 
                                   Double costoCuota, Double costoMora, 
                                   Integer nrCuotas, Integer diaVencimiento) {
        if (temario != null) {
            this.temario = temario.trim();
        }
        
        if (docentes != null) {
            this.docentes = new ArrayList<>(docentes);
        }
        
        if (costoCuota != null && costoCuota >= 0) {
            this.costoCuota = costoCuota;
        }
        
        if (costoMora != null && costoMora >= 0) {
            this.costoMora = costoMora;
        }
        
        if (nrCuotas != null && nrCuotas > 0) {
            this.nrCuotas = nrCuotas;
        }
        
        if (diaVencimiento != null && diaVencimiento >= 1 && diaVencimiento <= 31) {
            this.diaVencimiento = diaVencimiento;
        }
    }

    /**
     * Valida los datos específicos del curso
     */
    public List<String> validarDatosCurso() {
        List<String> errores = new ArrayList<>();
        
        if (docentes == null || docentes.isEmpty()) {
            errores.add("El curso debe tener al menos un docente asignado");
        }
        
        if (costoCuota != null && costoCuota < 0) {
            errores.add("El costo por cuota no puede ser negativo");
        }
        
        if (costoMora != null && costoMora < 0) {
            errores.add("El costo de mora no puede ser negativo");
        }
        
        if (nrCuotas != null && nrCuotas <= 0) {
            errores.add("El número de cuotas debe ser mayor a 0");
        }
        
        if (diaVencimiento != null && (diaVencimiento < 1 || diaVencimiento > 31)) {
            errores.add("El día de vencimiento debe estar entre 1 y 31");
        }
        
        // Si tiene cuotas, debe tener costo por cuota
        if ((nrCuotas != null && nrCuotas > 0) && costoCuota == null) {
            errores.add("Si hay cuotas, debe especificar el costo por cuota");
        }
        
        return errores;
    }

    /**
     * Verifica si el curso puede ser modificado
     */
    @Override
    public Boolean puedeSerEditada() {
        // Usar validación base más validaciones específicas del curso
        boolean puedeEditarBase = super.puedeSerEditada();
        
        if (!puedeEditarBase) {
            return false;
        }
        
        // Si ya comenzó y tiene estudiantes, solo se pueden modificar ciertos campos
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() != null && !getFechaInicio().isAfter(ahora)) {
            int inscripcionesActivas = (getInscripciones() != null) ? getInscripciones().size() : 0;
            /*if (inscripcionesActivas > 0) {
                // Si ya comenzó con estudiantes, verificar si hay clases dictadas
                return !tieneClasesDictadas();
            }*/
        }
        
        return true;
    }

    /**
     * Verifica si el curso tiene clases ya dictadas
     */
    /*private boolean tieneClasesDictadas() {
        if (clases == null || clases.isEmpty()) {
            return false;
        }
        
        LocalDate ahora = LocalDate.now();
        return clases.stream()
                .anyMatch(clase -> clase.getFecha() != null && 
                         clase.getFecha().toLocalDate().isBefore(ahora));
    }*/

    /**
     * Valida si se puede dar de baja el curso
     */
    @Override
    public Boolean puedeDarseDeBaja() {
        // Usar validación base
        boolean puedeBase = super.puedeDarseDeBaja();
        
        if (!puedeBase) {
            return false;
        }
        
        // Validación específica: si ya comenzó con estudiantes y tiene clases dictadas, no se puede dar de baja
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() != null && !getFechaInicio().isAfter(ahora)) {
            int inscripcionesActivas = (getInscripciones() != null) ? getInscripciones().size() : 0;
            /*if (inscripcionesActivas > 0 && tieneClasesDictadas()) {
                return false;
            }*/
        }
        
        return true;
    }

    /**
     * Añade un docente al curso
     */
    public boolean agregarDocente(Docente docente) {
        if (docente == null) {
            return false;
        }
        
        if (docentes == null) {
            docentes = new ArrayList<>();
        }
        
        // Verificar que no esté ya agregado
        if (docentes.stream().anyMatch(d -> d.getId().equals(docente.getId()))) {
            return false;
        }
        
        docentes.add(docente);
        return true;
    }

    /**
     * Remueve un docente del curso
     */
    public boolean removerDocente(UUID docenteId) {
        if (docentes == null || docenteId == null) {
            return false;
        }
        
        // No se puede remover si queda menos de un docente
        if (docentes.size() <= 1) {
            return false;
        }
        
        return docentes.removeIf(d -> d.getId().equals(docenteId));
    }

    /**
     * Obtiene información detallada del curso para el modal
     */
    public CursoDetalle obtenerDetalleCompleto() {
        CursoDetalle detalle = new CursoDetalle();
        
        // Información básica heredada
        detalle.setId(this.getIdOferta());
        detalle.setNombre(this.getNombre());
        detalle.setDescripcion(this.getDescripcion());
        detalle.setTipo("CURSO");
        detalle.setModalidad(this.getModalidad() != null ? this.getModalidad().toString() : "");
        detalle.setEstado(this.getEstado() != null ? this.getEstado().toString() : "");
        detalle.setFechaInicio(this.getFechaInicio());
        detalle.setFechaFin(this.getFechaFin());
        detalle.setCupos(this.getCupos());
        detalle.setCostoInscripcion(this.getCostoInscripcion());
        detalle.setCertificado(this.getCertificado() != null ? this.getCertificado().toString() : "");
        detalle.setVisibilidad(this.getVisibilidad());
        
        // Información específica de curso
        detalle.setTemario(this.temario);
        detalle.setDocentes(this.docentes != null ? this.docentes : new ArrayList<>());
        detalle.setRequisitos(this.requisitos != null ? this.requisitos : new ArrayList<>());
        detalle.setCostoCuota(this.costoCuota);
        detalle.setCostoMora(this.costoMora);
        detalle.setNrCuotas(this.nrCuotas);
        detalle.setDiaVencimiento(this.diaVencimiento);
        
        // Información adicional
        detalle.setTotalInscripciones(this.getInscripciones() != null ? this.getInscripciones().size() : 0);
        detalle.setInscripcionesActivas(this.getInscripciones() != null ? 
            (int) this.getInscripciones().stream()
                .filter(ins -> ins.getEstadoInscripcion() != null && ins.getEstadoInscripcion())
                .count() : 0);
        detalle.setCuposDisponibles(this.tieneCuposDisponibles());
        
        return detalle;
    }

    /**
     * Clase interna para encapsular los detalles del curso
     */
    public static class CursoDetalle {
        private Long id;
        private String nombre;
        private String descripcion;
        private String tipo;
        private String modalidad;
        private String estado;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Integer cupos;
        private Double costoInscripcion;
        private String certificado;
        private Boolean visibilidad;
        
        // Específicos de curso
        private String temario;
        private List<Docente> docentes;
        private List<OfertaAcademica> requisitos;
        private Double costoCuota;
        private Double costoMora;
        private Integer nrCuotas;
        private Integer diaVencimiento;
        
        // Información adicional
        private int totalInscripciones;
        private int inscripcionesActivas;
        private boolean cuposDisponibles;
        
        // Getters y Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        
        public String getModalidad() { return modalidad; }
        public void setModalidad(String modalidad) { this.modalidad = modalidad; }
        
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        
        public LocalDate getFechaInicio() { return fechaInicio; }
        public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
        
        public LocalDate getFechaFin() { return fechaFin; }
        public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
        
        public Integer getCupos() { return cupos; }
        public void setCupos(Integer cupos) { this.cupos = cupos; }
        
        public Double getCostoInscripcion() { return costoInscripcion; }
        public void setCostoInscripcion(Double costoInscripcion) { this.costoInscripcion = costoInscripcion; }
        
        public String getCertificado() { return certificado; }
        public void setCertificado(String certificado) { this.certificado = certificado; }
        
        public Boolean getVisibilidad() { return visibilidad; }
        public void setVisibilidad(Boolean visibilidad) { this.visibilidad = visibilidad; }
        
        public String getTemario() { return temario; }
        public void setTemario(String temario) { this.temario = temario; }
        
        public List<Docente> getDocentes() { return docentes; }
        public void setDocentes(List<Docente> docentes) { this.docentes = docentes; }
        
        public List<OfertaAcademica> getRequisitos() { return requisitos; }
        public void setRequisitos(List<OfertaAcademica> requisitos) { this.requisitos = requisitos; }
        
        public Double getCostoCuota() { return costoCuota; }
        public void setCostoCuota(Double costoCuota) { this.costoCuota = costoCuota; }
        
        public Double getCostoMora() { return costoMora; }
        public void setCostoMora(Double costoMora) { this.costoMora = costoMora; }
        
        public Integer getNrCuotas() { return nrCuotas; }
        public void setNrCuotas(Integer nrCuotas) { this.nrCuotas = nrCuotas; }
        
        public Integer getDiaVencimiento() { return diaVencimiento; }
        public void setDiaVencimiento(Integer diaVencimiento) { this.diaVencimiento = diaVencimiento; }
        
        public int getTotalInscripciones() { return totalInscripciones; }
        public void setTotalInscripciones(int totalInscripciones) { this.totalInscripciones = totalInscripciones; }
        
        public int getInscripcionesActivas() { return inscripcionesActivas; }
        public void setInscripcionesActivas(int inscripcionesActivas) { this.inscripcionesActivas = inscripcionesActivas; }
        
        public boolean isCuposDisponibles() { return cuposDisponibles; }
        public void setCuposDisponibles(boolean cuposDisponibles) { this.cuposDisponibles = cuposDisponibles; }
    }
}
