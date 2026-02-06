package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "El plan de formación no puede estar vacío")
    private String plan;
    
    @ManyToMany(mappedBy = "formaciones")
    private List<Docente> docentes = new ArrayList<>();
    
    @NotNull(message = "El costo por cuota no puede estar vacío")
    private Double costoCuota;
    @NotNull(message = "El costo de mora no puede estar vacío")
    private Double costoMora;
    @NotNull(message = "El número de cuotas no puede estar vacío")
    private Integer nrCuotas;
    @NotNull(message = "El día de vencimiento no puede estar vacío")
    private Integer diaVencimiento; // Día del mes límite para pago sin mora

    // Constructor para DataSeeder
    public Formacion(String nombre, String descripcion, String plan, Modalidad modalidad, Double costo, 
                     LocalDate inicio, LocalDate fin, Integer cupos, Integer duracionMeses, 
                     Double costoMora, Integer diaVencimiento, List<Docente> docentes) {
        super(nombre, descripcion, modalidad, costo, inicio, fin, cupos, duracionMeses, true, EstadoOferta.ACTIVA);
        this.plan = plan;
        this.docentes = docentes;
        this.nrCuotas = duracionMeses;
        this.costoCuota = duracionMeses > 0 ? costo / duracionMeses : costo;
        this.costoMora = costoMora;
        this.diaVencimiento = diaVencimiento;
    }

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
        detalle.setFechaInicioInscripcion(this.getFechaInicioInscripcion());
        detalle.setFechaFinInscripcion(this.getFechaFinInscripcion());
        detalle.setCupos(this.getCupos());
        detalle.setCostoInscripcion(this.getCostoInscripcion());
        detalle.setCertificado(this.getCertificado() != null ? this.getCertificado().toString() : "");
        detalle.setVisibilidad(this.getVisibilidad());
        detalle.setLugar(this.getLugar());
        detalle.setEnlace(this.getEnlace());
        detalle.setImagenUrl(this.getImagenUrl());
        
        // Información específica de formación
        detalle.setPlan(this.plan);
        
        // Convertir docentes a DTO simple (evitar referencia circular)
        List<DocenteSimple> docentesSimples = new ArrayList<>();
        if (this.docentes != null) {
            for (Docente docente : this.docentes) {
                docentesSimples.add(new DocenteSimple(
                    docente.getId(),
                    docente.getNombre(),
                    docente.getApellido(),
                    docente.getMatricula()
                ));
            }
        }
        detalle.setDocentes(docentesSimples);
        
        // Convertir horarios a DTO simple (evitar referencia circular)
        List<HorarioSimple> horariosSimples = new ArrayList<>();
        if (this.getHorarios() != null) {
            for (Horario horario : this.getHorarios()) {
                // Formatear hora para mostrar solo HH:mm (sin segundos)
                String horaInicio = horario.getHoraInicio() != null ? 
                    horario.getHoraInicio().toString().substring(0, 5) : null;
                String horaFin = horario.getHoraFin() != null ? 
                    horario.getHoraFin().toString().substring(0, 5) : null;
                    
                horariosSimples.add(new HorarioSimple(
                    horario.getDia() != null ? horario.getDia().toString() : null,
                    horaInicio,
                    horaFin
                ));
            }
        }
        detalle.setHorarios(horariosSimples);
        
        // Convertir categorías a DTO simple
        List<CategoriaSimple> categoriasSimples = new ArrayList<>();
        if (this.getCategorias() != null) {
            for (Categoria categoria : this.getCategorias()) {
                categoriasSimples.add(new CategoriaSimple(
                    categoria.getIdCategoria(),
                    categoria.getNombre()
                ));
            }
        }
        detalle.setCategorias(categoriasSimples);
        
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
     * DTO simple para categorías (evita referencia circular)
     */
    public static class CategoriaSimple {
        private Long id;
        private String nombre;
        
        public CategoriaSimple(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }
    
    /**
     * DTO simple para docentes (evita referencia circular)
     */
    public static class DocenteSimple {
        private UUID id;
        private String nombre;
        private String apellido;
        private String matricula;
        
        public DocenteSimple(UUID id, String nombre, String apellido, String matricula) {
            this.id = id;
            this.nombre = nombre;
            this.apellido = apellido;
            this.matricula = matricula;
        }
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        
        public String getApellido() { return apellido; }
        public void setApellido(String apellido) { this.apellido = apellido; }
        
        public String getMatricula() { return matricula; }
        public void setMatricula(String matricula) { this.matricula = matricula; }
    }
    
    @Override
    public void actualizarDatos(java.util.Map<String, Object> datos) {
        super.actualizarDatos(datos);
        
        if (datos.containsKey("plan")) {
            this.setPlan((String) datos.get("plan"));
        }
        if (datos.containsKey("costoCuota")) {
            Double val = convertirDouble(datos.get("costoCuota"));
            if (val != null) this.setCostoCuota(val);
        }
        if (datos.containsKey("costoMora")) {
            Double val = convertirDouble(datos.get("costoMora"));
            if (val != null) {
                this.setCostoMora(val);
                this.setRecargoMora(val);
            }
        }
        if (datos.containsKey("nrCuotas")) {
            Integer val = convertirEntero(datos.get("nrCuotas"));
            if (val != null) this.setNrCuotas(val);
        }
        if (datos.containsKey("diaVencimiento")) {
            Integer val = convertirEntero(datos.get("diaVencimiento"));
            if (val != null) this.setDiaVencimiento(val);
        }
        if (datos.containsKey("docentes")) {
             Object obj = datos.get("docentes");
             if (obj instanceof java.util.List) {
                 this.setDocentes(new ArrayList<>((java.util.List<Docente>) obj));
             }
        }
    }

    /**
     * DTO simple para horarios (evita referencia circular)
     */
    public static class HorarioSimple {
        private String dia;
        private String horaInicio;
        private String horaFin;
        
        public HorarioSimple(String dia, String horaInicio, String horaFin) {
            this.dia = dia;
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
        }
        
        public String getDia() { return dia; }
        public void setDia(String dia) { this.dia = dia; }
        
        public String getHoraInicio() { return horaInicio; }
        public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
        
        public String getHoraFin() { return horaFin; }
        public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
    }
    
    
    // actualizarDatos duplicado eliminado


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
        private LocalDate fechaInicioInscripcion;
        private LocalDate fechaFinInscripcion;
        private Integer cupos;
        private Double costoInscripcion;
        private String certificado;
        private Boolean visibilidad;
        private String lugar;
        private String enlace;
        private String imagenUrl;
        
        // Específicos de formación
        private String plan;
        private List<DocenteSimple> docentes;
        private List<HorarioSimple> horarios;
        private List<CategoriaSimple> categorias;
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
        
        public LocalDate getFechaInicioInscripcion() { return fechaInicioInscripcion; }
        public void setFechaInicioInscripcion(LocalDate fechaInicioInscripcion) { this.fechaInicioInscripcion = fechaInicioInscripcion; }
        
        public LocalDate getFechaFinInscripcion() { return fechaFinInscripcion; }
        public void setFechaFinInscripcion(LocalDate fechaFinInscripcion) { this.fechaFinInscripcion = fechaFinInscripcion; }
        
        public Integer getCupos() { return cupos; }
        public void setCupos(Integer cupos) { this.cupos = cupos; }
        
        public Double getCostoInscripcion() { return costoInscripcion; }
        public void setCostoInscripcion(Double costoInscripcion) { this.costoInscripcion = costoInscripcion; }
        
        public String getCertificado() { return certificado; }
        public void setCertificado(String certificado) { this.certificado = certificado; }
        
        public Boolean getVisibilidad() { return visibilidad; }
        public void setVisibilidad(Boolean visibilidad) { this.visibilidad = visibilidad; }

        public String getLugar() { return lugar; }
        public void setLugar(String lugar) { this.lugar = lugar; }

        public String getEnlace() { return enlace; }
        public void setEnlace(String enlace) { this.enlace = enlace; }

        public String getImagenUrl() { return imagenUrl; }
        public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
        
        public String getPlan() { return plan; }
        public void setPlan(String plan) { this.plan = plan; }
        
        public List<DocenteSimple> getDocentes() { return docentes; }
        public void setDocentes(List<DocenteSimple> docentes) { this.docentes = docentes; }
        
        public List<CategoriaSimple> getCategorias() { return categorias; }
        public void setCategorias(List<CategoriaSimple> categorias) { this.categorias = categorias; }
        
        public List<HorarioSimple> getHorarios() { return horarios; }
        public void setHorarios(List<HorarioSimple> horarios) { this.horarios = horarios; }
        
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
