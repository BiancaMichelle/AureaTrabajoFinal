package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Charla extends OfertaAcademica {
    private String lugar;
    private String enlace;
    private Integer duracionEstimada; // en minutos
    
    @ElementCollection
    private List<String> disertantes;
    
    private String publicoObjetivo;

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
        if (enlace != null && !enlace.trim().isEmpty()) {
            return "Online";
        }
        if (lugar != null && !lugar.trim().isEmpty()) {
            return "Presencial";
        }
        return "No definida";
    }

    /**
     * Modifica los datos específicos de la charla
     */
    public void modificarDatosCharla(String lugar, String enlace, Integer duracionEstimada, 
                                    List<String> disertantes, String publicoObjetivo) {
        if (lugar != null) {
            this.lugar = lugar.trim();
        }
        
        if (enlace != null) {
            this.enlace = enlace.trim();
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
        if ((lugar == null || lugar.trim().isEmpty()) && 
            (enlace == null || enlace.trim().isEmpty())) {
            errores.add("La charla debe tener un lugar (presencial) o enlace (virtual)");
        }
        
        if (disertantes == null || disertantes.isEmpty()) {
            errores.add("La charla debe tener al menos un disertante");
        }
        
        if (duracionEstimada != null && duracionEstimada <= 0) {
            errores.add("La duración estimada debe ser mayor a 0 minutos");
        }
        
        // Si es virtual, validar que el enlace sea válido
        if (enlace != null && !enlace.trim().isEmpty()) {
            if (!enlace.startsWith("http://") && !enlace.startsWith("https://")) {
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
        return enlace != null && !enlace.trim().isEmpty();
    }

    /**
     * Verifica si es una charla presencial
     */
    public boolean esPresencial() {
        return lugar != null && !lugar.trim().isEmpty();
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
        
        // Información específica de charla
        detalle.setLugar(this.lugar);
        detalle.setEnlace(this.enlace);
        detalle.setDuracionEstimada(this.duracionEstimada);
        detalle.setDisertantes(this.disertantes != null ? this.disertantes : new ArrayList<>());
        detalle.setPublicoObjetivo(this.publicoObjetivo);
        
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
        
        // Específicos de charla
        private String lugar;
        private String enlace;
        private Integer duracionEstimada;
        private List<String> disertantes;
        private String publicoObjetivo;
        private List<CategoriaSimple> categorias;
        
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
    }
}
