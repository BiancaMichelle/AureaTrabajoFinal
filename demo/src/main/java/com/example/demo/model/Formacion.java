package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Formacion extends OfertaAcademica {
    private String plan;
    
    @ManyToMany(mappedBy = "formaciones")
    private List<Docente> docentes = new ArrayList<>();
    
    private Double costoCuota;
    private Double costoMora;
    private Integer nrCuotas;
    private Integer diaVencimiento; // Día del mes límite para pago sin mora

    /**
     * Información de docentes
     */
    public String getDocentesTexto() {
        if (docentes == null || docentes.isEmpty()) {
            return "Sin docentes";
        }
        return docentes.size() + " docente(s)";
    }

    /**
     * Modifica los datos específicos de la formación
     */
    public void modificarDatosFormacion(String plan, List<Docente> docentes, 
                                       Double costoCuota, Double costoMora, 
                                       Integer nrCuotas, Integer diaVencimiento) {
        if (plan != null) {
            this.plan = plan.trim();
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
     * Valida los datos específicos de la formación
     */
    public List<String> validarDatosFormacion() {
        List<String> errores = new ArrayList<>();
        
        if (plan == null || plan.trim().isEmpty()) {
            errores.add("El plan de formación es obligatorio");
        }
        
        if (docentes == null || docentes.isEmpty()) {
            errores.add("La formación debe tener al menos un docente asignado");
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
     * Verifica si la formación puede ser modificada
     */
    @Override
    public Boolean puedeSerEditada() {
        // Usar validación base más validaciones específicas de la formación
        boolean puedeEditarBase = super.puedeSerEditada();
        
        if (!puedeEditarBase) {
            return false;
        }
        
        // Si ya comenzó y tiene estudiantes, solo se pueden modificar ciertos campos
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() != null && !getFechaInicio().isAfter(ahora)) {
            int inscripcionesActivas = (getInscripciones() != null) ? getInscripciones().size() : 0;
            if (inscripcionesActivas > 0) {
                // Si ya comenzó con estudiantes, verificar progreso
                return !tieneProgresoSignificativo();
            }
        }
        
        return true;
    }

    /**
     * Verifica si la formación tiene progreso significativo (más del 25%)
     */
    private boolean tieneProgresoSignificativo() {
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() == null || getFechaFin() == null) {
            return false;
        }
        
        long duracionTotal = getFechaInicio().toEpochDay() - getFechaFin().toEpochDay();
        long transcurrido = getFechaInicio().toEpochDay() - ahora.toEpochDay();
        
        // Si ha transcurrido más del 25% de la formación
        return Math.abs(transcurrido) > Math.abs(duracionTotal * 0.25);
    }

    /**
     * Valida si se puede dar de baja la formación
     */
    @Override
    public Boolean puedeDarseDeBaja() {
        // Usar validación base
        boolean puedeBase = super.puedeDarseDeBaja();
        
        if (!puedeBase) {
            return false;
        }
        
        // Validación específica: si ya comenzó con estudiantes y tiene progreso significativo, no se puede dar de baja
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() != null && !getFechaInicio().isAfter(ahora)) {
            int inscripcionesActivas = (getInscripciones() != null) ? getInscripciones().size() : 0;
            if (inscripcionesActivas > 0 && tieneProgresoSignificativo()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Añade un docente a la formación
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
     * Remueve un docente de la formación
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
     * Obtiene información detallada de la formación para el modal
     */
    public FormacionDetalle obtenerDetalleCompleto() {
        FormacionDetalle detalle = new FormacionDetalle();
        
        // Información básica heredada
        detalle.setId(this.getIdOferta());
        detalle.setNombre(this.getNombre());
        detalle.setDescripcion(this.getDescripcion());
        detalle.setTipo("FORMACION");
        detalle.setModalidad(this.getModalidad() != null ? this.getModalidad().toString() : "");
        detalle.setEstado(this.getEstado() != null ? this.getEstado().toString() : "");
        detalle.setFechaInicio(this.getFechaInicio());
        detalle.setFechaFin(this.getFechaFin());
        detalle.setCupos(this.getCupos());
        detalle.setCostoInscripcion(this.getCostoInscripcion());
        detalle.setCertificado(this.getCertificado() != null ? this.getCertificado().toString() : "");
        detalle.setVisibilidad(this.getVisibilidad());
        
        // Información específica de formación
        detalle.setPlan(this.plan);
        detalle.setDocentes(this.docentes != null ? this.docentes : new ArrayList<>());
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
     * Clase interna para encapsular los detalles de la formación
     */
    public static class FormacionDetalle {
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
        
        // Específicos de formación
        private String plan;
        private List<Docente> docentes;
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
        
        public String getPlan() { return plan; }
        public void setPlan(String plan) { this.plan = plan; }
        
        public List<Docente> getDocentes() { return docentes; }
        public void setDocentes(List<Docente> docentes) { this.docentes = docentes; }
        
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
