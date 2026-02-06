package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotEmpty;
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
public class Charla extends OfertaAcademica {
    // lugar y enlace movidos a OfertaAcademica
    private Integer duracionEstimada; // en minutos
    private java.sql.Time horaInicio; // Hora de inicio de la charla
    
    @ElementCollection
    @NotEmpty(message = "Debe haber al menos un disertante")
    private List<String> disertantes;
    
    private String publicoObjetivo;

    // Construtor para DataSeeder
    public Charla(String nombre, String descripcion, String publico, Modalidad modalidad, 
                  LocalDate inicio, String enlace, List<String> disertantes) {
        super(nombre, descripcion, modalidad, 0.0, inicio, inicio, 100, 1, false, EstadoOferta.ACTIVA);
        this.setEnlace(enlace);
        this.publicoObjetivo = publico;
        this.disertantes = disertantes;
        this.duracionEstimada = 60;
    }

    @PrePersist
    @PreUpdate
    public void validarCharla() {
        super.validarOferta();
        if (disertantes == null || disertantes.isEmpty()) {
            throw new IllegalStateException("La charla debe tener al menos un disertante.");
        }
    }

    /**
     * Duración estimada formateada
     */
    public String getDuracionEstimadaTexto() {
        if (duracionEstimada == null) return "No definida";
        if (duracionEstimada < 60) return duracionEstimada + " min";
        return (duracionEstimada / 60) + "h " + (duracionEstimada % 60) + "min";
    }
    /**
     * Tipo de modalidad (presencial/online)
     */
    public String getTipoModalidad() {
        if (getEnlace() != null && !getEnlace().trim().isEmpty()) {
            return "Online";
        }
        if (getLugar() != null && !getLugar().trim().isEmpty()) {
            return "Presencial";
        }
        return "No definida";
    }

    /**
     * Modifica los datos específicos de la charla
     */
    public void modificarDatosCharla(Integer duracionEstimada, List<String> disertantes, String publicoObjetivo) {
        if (duracionEstimada != null && duracionEstimada > 0) {
            this.duracionEstimada = duracionEstimada;
        }
        
        if (disertantes != null && !disertantes.isEmpty()) {
            this.disertantes = disertantes;
        }
        
        if (publicoObjetivo != null && !publicoObjetivo.trim().isEmpty()) {
            this.publicoObjetivo = publicoObjetivo;
        }
    }

    /**
     * Modifica los datos específicos de la charla
     */
    public void modificarDatosCharla(String lugar, String enlace, Integer duracionEstimada, 
                                    List<String> disertantes, String publicoObjetivo) {
        if (lugar != null) {
            setLugar(lugar.trim());
        }
        
        if (enlace != null) {
            setEnlace(enlace.trim());
        }
        
        if (duracionEstimada != null && duracionEstimada > 0) {
            this.duracionEstimada = duracionEstimada;
        }
        
        if (disertantes != null) {
            this.disertantes = new ArrayList<>(disertantes);
        }
        
        if (publicoObjetivo != null) {
            this.publicoObjetivo = publicoObjetivo.trim();
        }
    }

    /**
     * Valida los datos específicos de la charla
     */
    public List<String> validarDatosCharla() {
        List<String> errores = new ArrayList<>();
        
        // Debe tener lugar O enlace, no ambos vacíos
        if ((getLugar() == null || getLugar().trim().isEmpty()) && 
            (getEnlace() == null || getEnlace().trim().isEmpty())) {
            errores.add("La charla debe tener un lugar (presencial) o enlace (virtual)");
        }
        
        if (disertantes == null || disertantes.isEmpty()) {
            errores.add("La charla debe tener al menos un disertante");
        }
        
        if (duracionEstimada != null && duracionEstimada <= 0) {
            errores.add("La duración estimada debe ser mayor a 0 minutos");
        }
        
        // Si es virtual, validar que el enlace sea válido
        if (getEnlace() != null && !getEnlace().trim().isEmpty()) {
            if (!getEnlace().startsWith("http://") && !getEnlace().startsWith("https://")) {
                errores.add("El enlace debe comenzar con http:// o https://");
            }
        }
        
        return errores;
    }

    /**
     * Verifica si la charla puede ser modificada
     */
    @Override
    public Boolean puedeSerEditada() {
        // Usar validación base
        boolean puedeEditarBase = super.puedeSerEditada();
        
        if (!puedeEditarBase) {
            return false;
        }
        
        // Si ya comenzó, no se puede modificar
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() != null && getFechaInicio().isBefore(ahora)) {
            return false;
        }
        
        // Si es en las próximas 2 horas, tampoco se puede modificar
        if (getFechaInicio() != null && getFechaInicio().equals(ahora)) {
            // Aquí podrías agregar lógica de hora si tienes el campo de hora
            return false;
        }
        
        return true;
    }

    /**
     * Valida si se puede dar de baja la charla
     */
    @Override
    public Boolean puedeDarseDeBaja() {
        // Usar validación base
        boolean puedeBase = super.puedeDarseDeBaja();
        
        if (!puedeBase) {
            return false;
        }
        
        // Si ya comenzó, no se puede dar de baja (las charlas son eventos puntuales)
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() != null && getFechaInicio().isBefore(ahora)) {
            return false;
        }
        
        return true;
    }

    /**
     * Añade un disertante a la charla
     */
    public boolean agregarDisertante(String disertante) {
        if (disertante == null || disertante.trim().isEmpty()) {
            return false;
        }
        
        if (disertantes == null) {
            disertantes = new ArrayList<>();
        }
        
        String nombreDisertante = disertante.trim();
        
        // Verificar que no esté ya agregado
        if (disertantes.stream().anyMatch(d -> d.equalsIgnoreCase(nombreDisertante))) {
            return false;
        }
        
        disertantes.add(nombreDisertante);
        return true;
    }

    /**
     * Remueve un disertante de la charla
     */
    public boolean removerDisertante(String disertante) {
        if (disertantes == null || disertante == null || disertante.trim().isEmpty()) {
            return false;
        }
        
        // No se puede remover si queda menos de un disertante
        if (disertantes.size() <= 1) {
            return false;
        }
        
        return disertantes.removeIf(d -> d.equalsIgnoreCase(disertante.trim()));
    }

    /**
     * Información de disertantes para mostrar
     */
    public String getDisertantesTexto() {
        if (disertantes == null || disertantes.isEmpty()) {
            return "Sin disertantes";
        }
        if (disertantes.size() == 1) {
            return disertantes.get(0);
        }
        return disertantes.size() + " disertantes";
    }

    /**
     * Lista completa de disertantes
     */
    public String getDisertantesCompleto() {
        if (disertantes == null || disertantes.isEmpty()) {
            return "Sin disertantes asignados";
        }
        return String.join(", ", disertantes);
    }

    /**
     * Verifica si es una charla virtual
     */
    public boolean esVirtual() {
        return getEnlace() != null && !getEnlace().trim().isEmpty();
    }

    /**
     * Verifica si es una charla presencial
     */
    public boolean esPresencial() {
        return getLugar() != null && !getLugar().trim().isEmpty();
    }

    /**
     * Obtiene información detallada de la charla para el modal
     */
    public CharlaDetalle obtenerDetalleCompleto() {
        CharlaDetalle detalle = new CharlaDetalle();
        
        // Información básica heredada
        detalle.setId(this.getIdOferta());
        detalle.setNombre(this.getNombre());
        detalle.setDescripcion(this.getDescripcion());
        detalle.setTipo("CHARLA");
        detalle.setModalidad(this.getModalidad() != null ? this.getModalidad().toString() : "");
        detalle.setEstado(this.getEstado() != null ? this.getEstado().toString() : "");
        detalle.setFechaInicio(this.getFechaInicio());
        detalle.setFechaFin(this.getFechaFin());
        detalle.setCupos(this.getCupos());
        detalle.setCostoInscripcion(this.getCostoInscripcion());
        detalle.setCertificado(this.getCertificado() != null ? this.getCertificado().toString() : "");
        detalle.setVisibilidad(this.getVisibilidad());
        detalle.setImagenUrl(this.getImagenUrl());
        
        // Información específica de charla
        detalle.setLugar(this.getLugar());
        detalle.setEnlace(this.getEnlace());
        detalle.setDuracionEstimada(this.duracionEstimada);
        detalle.setDisertantes(this.disertantes != null ? this.disertantes : new ArrayList<>());
        detalle.setPublicoObjetivo(this.publicoObjetivo);
        detalle.setFechaInicioInscripcion(this.getFechaInicioInscripcion());
        detalle.setFechaFinInscripcion(this.getFechaFinInscripcion());
        detalle.setHoraInicio(this.horaInicio);
        
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
    
    @Override
    public void actualizarDatos(java.util.Map<String, Object> datos) {
        super.actualizarDatos(datos);
        
        if (datos.containsKey("duracionEstimada")) {
            this.setDuracionEstimada(convertirEntero(datos.get("duracionEstimada")));
        }
        if (datos.containsKey("publicoObjetivo")) {
            this.setPublicoObjetivo((String) datos.get("publicoObjetivo"));
        }
        if (datos.containsKey("horaInicio")) {
            Object horaObj = datos.get("horaInicio");
            if (horaObj instanceof java.sql.Time) {
                this.setHoraInicio((java.sql.Time) horaObj);
            }
        }
        if (datos.containsKey("disertantes")) {
             Object obj = datos.get("disertantes");
             if (obj instanceof List) {
                 this.setDisertantes(new ArrayList<>((List<String>) obj));
             } else if (obj instanceof String) {
                 String strDisertantes = ((String)obj).trim();
                 if (!strDisertantes.isEmpty()) {
                     // Si viene como array JSON ["Nico"], limpiarlo
                     if (strDisertantes.startsWith("[") && strDisertantes.endsWith("]")) {
                         strDisertantes = strDisertantes.substring(1, strDisertantes.length() - 1)
                             .replaceAll("\"", "")
                             .trim();
                     }
                     // Dividir por comas y limpiar espacios
                     this.setDisertantes(new ArrayList<>(java.util.Arrays.asList(strDisertantes.split(","))));
                 }
             }
        }
    }

    /**
     * Clase interna para encapsular los detalles de la charla
     */
    public static class CharlaDetalle {
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
        private String imagenUrl;
        
        // Específicos de charla
        private String lugar;
        private String enlace;
        private Integer duracionEstimada;
        private List<String> disertantes;
        private String publicoObjetivo;
        private List<CategoriaSimple> categorias;
        private LocalDate fechaInicioInscripcion;
        private LocalDate fechaFinInscripcion;
        private java.sql.Time horaInicio;
        
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

        public String getImagenUrl() { return imagenUrl; }
        public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
        
        public String getLugar() { return lugar; }
        public void setLugar(String lugar) { this.lugar = lugar; }
        
        public String getEnlace() { return enlace; }
        public void setEnlace(String enlace) { this.enlace = enlace; }
        
        public Integer getDuracionEstimada() { return duracionEstimada; }
        public void setDuracionEstimada(Integer duracionEstimada) { this.duracionEstimada = duracionEstimada; }
        
        public List<String> getDisertantes() { return disertantes; }
        public void setDisertantes(List<String> disertantes) { this.disertantes = disertantes; }
        
        public String getPublicoObjetivo() { return publicoObjetivo; }
        public void setPublicoObjetivo(String publicoObjetivo) { this.publicoObjetivo = publicoObjetivo; }
        
        public List<CategoriaSimple> getCategorias() { return categorias; }
        public void setCategorias(List<CategoriaSimple> categorias) { this.categorias = categorias; }
        
        public int getTotalInscripciones() { return totalInscripciones; }
        public void setTotalInscripciones(int totalInscripciones) { this.totalInscripciones = totalInscripciones; }
        
        public int getInscripcionesActivas() { return inscripcionesActivas; }
        public void setInscripcionesActivas(int inscripcionesActivas) { this.inscripcionesActivas = inscripcionesActivas; }
        
        public boolean isCuposDisponibles() { return cuposDisponibles; }
        public void setCuposDisponibles(boolean cuposDisponibles) { this.cuposDisponibles = cuposDisponibles; }
        
        public LocalDate getFechaInicioInscripcion() { return fechaInicioInscripcion; }
        public void setFechaInicioInscripcion(LocalDate fechaInicioInscripcion) { this.fechaInicioInscripcion = fechaInicioInscripcion; }
        
        public LocalDate getFechaFinInscripcion() { return fechaFinInscripcion; }
        public void setFechaFinInscripcion(LocalDate fechaFinInscripcion) { this.fechaFinInscripcion = fechaFinInscripcion; }
        
        public java.sql.Time getHoraInicio() { return horaInicio; }
        public void setHoraInicio(java.sql.Time horaInicio) { this.horaInicio = horaInicio; }
    }
}
