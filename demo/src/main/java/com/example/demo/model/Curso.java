package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

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

    // Construtor para DataSeeder
    public Curso(String nombre, String descripcion, Modalidad modalidad, Double costo, Boolean certificado, 
                 LocalDate inicio, LocalDate fin, Integer cupos, Integer duracionMeses, 
                 Double costoMora, Integer diaVencimiento, List<Docente> docentes) {
        super(nombre, descripcion, modalidad, costo, inicio, fin, cupos, duracionMeses, certificado, EstadoOferta.ACTIVA);
        this.docentes = docentes;
        this.nrCuotas = duracionMeses; // Por defecto cuotas = duración
        this.costoCuota = duracionMeses > 0 ? costo / duracionMeses : costo;
        this.costoMora = costoMora;
        this.diaVencimiento = diaVencimiento;
    }

    @PrePersist
    @PreUpdate
    public void validarCurso() {
        super.validarOferta();
        if (docentes == null || docentes.isEmpty()) {
            throw new IllegalStateException("El curso debe tener al menos un docente asignado.");
        }
    }
    
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
        detalle.setFechaInicioInscripcion(this.getFechaInicioInscripcion());
        detalle.setFechaFinInscripcion(this.getFechaFinInscripcion());
        detalle.setCupos(this.getCupos());
        detalle.setCostoInscripcion(this.getCostoInscripcion());
        detalle.setCertificado(this.getCertificado() != null ? this.getCertificado().toString() : "");
        detalle.setVisibilidad(this.getVisibilidad());
        detalle.setLugar(this.getLugar());
        detalle.setEnlace(this.getEnlace());
        detalle.setImagenUrl(this.getImagenUrl());
        
        // Información específica de curso
        detalle.setTemario(this.temario);
        
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
     * Clase interna para encapsular los detalles del curso
     */
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
    
    @Override
    public void actualizarDatos(java.util.Map<String, Object> datos) {
        super.actualizarDatos(datos);
        
        if (datos.containsKey("temario")) {
            this.setTemario((String) datos.get("temario"));
        }
        if (datos.containsKey("costoCuota")) {
            this.setCostoCuota(convertirDouble(datos.get("costoCuota")));
        }
        if (datos.containsKey("costoMora")) {
            Double val = convertirDouble(datos.get("costoMora"));
            this.setCostoMora(val);
            if (val != null) this.setRecargoMora(val); 
        }
        if (datos.containsKey("nrCuotas")) {
            this.setNrCuotas(convertirEntero(datos.get("nrCuotas")));
        }
        if (datos.containsKey("diaVencimiento")) {
            this.setDiaVencimiento(convertirEntero(datos.get("diaVencimiento")));
        }
        if (datos.containsKey("docentes")) {
            Object obj = datos.get("docentes");
            if (obj instanceof List) {
                this.setDocentes(new ArrayList<>((List<Docente>) obj));
            }
        }
    }

    public static class CursoDetalle {
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
        
        // Específicos de curso
        private String temario;
        private List<DocenteSimple> docentes;
        private List<HorarioSimple> horarios;
        private List<CategoriaSimple> categorias;
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
        
        public String getTemario() { return temario; }
        public void setTemario(String temario) { this.temario = temario; }
        
        public List<DocenteSimple> getDocentes() { return docentes; }
        public void setDocentes(List<DocenteSimple> docentes) { this.docentes = docentes; }
        
        public List<HorarioSimple> getHorarios() { return horarios; }
        public void setHorarios(List<HorarioSimple> horarios) { this.horarios = horarios; }
        
        public List<CategoriaSimple> getCategorias() { return categorias; }
        public void setCategorias(List<CategoriaSimple> categorias) { this.categorias = categorias; }
        
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
