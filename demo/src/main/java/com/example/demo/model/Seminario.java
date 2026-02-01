package com.example.demo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Seminario extends OfertaAcademica {
    
    // lugar y enlace movidos a OfertaAcademica
    
    @NotBlank(message = "El público objetivo no puede estar vacío")
    private String publicoObjetivo;
    @Min(10)
    private Integer duracionMinutos; // en minutos

    @ElementCollection
    @NotEmpty(message = "Debe haber al menos un disertante")
    private List<String> disertantes;

    // Constructor para DataSeeder
    public Seminario(String nombre, String descripcion, String publico, Modalidad modalidad, Double costo,
                     LocalDate inicio, LocalDate fin, String lugar, Integer duracionMinutos, List<String> disertantes) {
        super(nombre, descripcion, modalidad, costo, inicio, fin, 50, 1, true, EstadoOferta.ACTIVA);
        this.setLugar(lugar);
        this.publicoObjetivo = publico;
        this.duracionMinutos = duracionMinutos;
        this.disertantes = disertantes;
    }

    /**
     * Duración formateada
     */
    public String getDuracionTexto() {
        if (duracionMinutos == null) return "No definida";
        if (duracionMinutos < 60) return duracionMinutos + " min";
        return (duracionMinutos / 60) + "h " + (duracionMinutos % 60) + "min";
    }

    /**
     * Modifica los datos específicos del seminario
     */
    public void modificarDatosSeminario(String publicoObjetivo, Integer duracionMinutos, List<String> disertantes) {
        if (publicoObjetivo != null && !publicoObjetivo.trim().isEmpty()) {
            this.publicoObjetivo = publicoObjetivo;
        }
        
        if (duracionMinutos != null && duracionMinutos > 0) {
            this.duracionMinutos = duracionMinutos;
        }
        
        if (disertantes != null && !disertantes.isEmpty()) {
            this.disertantes = disertantes;
        }
    }

    /**
     * Modifica los datos específicos del seminario con ubicación
     */
    public void modificarDatosSeminario(String lugar, String enlace, String publicoObjetivo, Integer duracionMinutos, List<String> disertantes) {
        if (lugar != null) {
            setLugar(lugar.trim());
        }
        
        if (enlace != null) {
            setEnlace(enlace.trim());
        }
        
        if (publicoObjetivo != null) {
            this.publicoObjetivo = publicoObjetivo.trim();
        }
        
        if (duracionMinutos != null && duracionMinutos > 0) {
            this.duracionMinutos = duracionMinutos;
        }
        
        if (disertantes != null) {
            this.disertantes = new ArrayList<>(disertantes);
        }
    }

    /**
     * Valida los datos específicos del seminario
     */
    public List<String> validarDatosSeminario() {
        List<String> errores = new ArrayList<>();
        
        // Debe tener lugar O enlace, no ambos vacíos
        if ((getLugar() == null || getLugar().trim().isEmpty()) && 
            (getEnlace() == null || getEnlace().trim().isEmpty())) {
            errores.add("El seminario debe tener un lugar (presencial) o enlace (virtual)");
        }
        
        if (disertantes == null || disertantes.isEmpty()) {
            errores.add("El seminario debe tener al menos un disertante");
        }
        
        if (duracionMinutos != null && duracionMinutos <= 0) {
            errores.add("La duración debe ser mayor a 0 minutos");
        }
        
        // Si es virtual, validar que el enlace sea válido
        if (getEnlace() != null && !getEnlace().trim().isEmpty()) {
            if (!getEnlace().startsWith("http://") && !getEnlace().startsWith("https://")) {
                errores.add("El enlace debe comenzar con http:// o https://");
            }
        }
        
        // Validar duración mínima para seminarios (generalmente más largos que charlas)
        if (duracionMinutos != null && duracionMinutos < 30) {
            errores.add("Un seminario debe durar al menos 30 minutos");
        }
        
        return errores;
    }

    /**
     * Verifica si el seminario puede ser modificado
     */
    @Override
    public Boolean puedeSerEditada() {
        // Usar validación base
        boolean puedeEditarBase = super.puedeSerEditada();
        
        if (!puedeEditarBase) {
            return false;
        }
        
        // Si ya comenzó, verificar si está en progreso
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() != null && getFechaFin() != null) {
            // Si ya comenzó pero no terminó, solo modificaciones menores
            if (!getFechaInicio().isAfter(ahora) && getFechaFin().isAfter(ahora)) {
                return tieneInscripcionesActivas() == 0; // Solo si no hay inscripciones
            }
            
            // Si ya terminó, no se puede modificar
            if (getFechaFin().isBefore(ahora)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Cuenta las inscripciones activas del seminario
     */
    private int tieneInscripcionesActivas() {
        return (getInscripciones() != null) ? 
               (int) getInscripciones().stream()
                    .filter(i -> i.getEstadoInscripcion() != null && 
                           i.getEstadoInscripcion() == true)
                    .count() : 0;
    }

    /**
     * Valida si se puede dar de baja el seminario
     */
    @Override
    public Boolean puedeDarseDeBaja() {
        // Usar validación base
        boolean puedeBase = super.puedeDarseDeBaja();
        
        if (!puedeBase) {
            return false;
        }
        
        // Si ya comenzó, verificar progreso
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() != null && !getFechaInicio().isAfter(ahora)) {
            // Si ya comenzó y tiene inscripciones activas, verificar si tiene progreso significativo
            if (tieneInscripcionesActivas() > 0 && tieneProgresoSignificativo()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Verifica si el seminario tiene progreso significativo
     */
    private boolean tieneProgresoSignificativo() {
        LocalDate ahora = LocalDate.now();
        if (getFechaInicio() == null || getFechaFin() == null) {
            return false;
        }
        
        // Si ha transcurrido más del 50% del seminario
        long duracionTotal = getFechaFin().toEpochDay() - getFechaInicio().toEpochDay();
        long transcurrido = ahora.toEpochDay() - getFechaInicio().toEpochDay();
        
        return duracionTotal > 0 && transcurrido > (duracionTotal * 0.5);
    }

    /**
     * Añade un disertante al seminario
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
     * Remueve un disertante del seminario
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
     * Verifica si es un seminario virtual
     */
    public boolean esVirtual() {
        return getEnlace() != null && !getEnlace().trim().isEmpty();
    }

    /**
     * Verifica si es un seminario presencial
     */
    public boolean esPresencial() {
        return getLugar() != null && !getLugar().trim().isEmpty();
    }

    /**
     * Tipo de modalidad (presencial/online)
     */
    public String getTipoModalidad() {
        if (esVirtual()) {
            return "Online";
        }
        if (esPresencial()) {
            return "Presencial";
        }
        return "No definida";
    }

    /**
     * Obtiene información detallada del seminario para el modal
     */
    public SeminarioDetalle obtenerDetalleCompleto() {
        SeminarioDetalle detalle = new SeminarioDetalle();
        
        // Información básica heredada
        detalle.setId(this.getIdOferta());
        detalle.setNombre(this.getNombre());
        detalle.setDescripcion(this.getDescripcion());
        detalle.setTipo("SEMINARIO");
        detalle.setModalidad(this.getModalidad() != null ? this.getModalidad().toString() : "");
        detalle.setEstado(this.getEstado() != null ? this.getEstado().toString() : "");
        detalle.setFechaInicio(this.getFechaInicio());
        detalle.setFechaFin(this.getFechaFin());
        detalle.setCupos(this.getCupos());
        detalle.setCostoInscripcion(this.getCostoInscripcion());
        detalle.setCertificado(this.getCertificado() != null ? this.getCertificado().toString() : "");
        detalle.setVisibilidad(this.getVisibilidad());
        detalle.setImagenUrl(this.getImagenUrl());
        
        // Información específica de seminario
        detalle.setLugar(this.getLugar());
        detalle.setEnlace(this.getEnlace());
        detalle.setPublicoObjetivo(this.publicoObjetivo);
        detalle.setDuracionMinutos(this.duracionMinutos);
        detalle.setDisertantes(this.disertantes != null ? this.disertantes : new ArrayList<>());
        
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
        
        if (datos.containsKey("duracionMinutos")) {
            this.setDuracionMinutos(convertirEntero(datos.get("duracionMinutos")));
        }
        if (datos.containsKey("publicoObjetivo")) {
            this.setPublicoObjetivo((String) datos.get("publicoObjetivo"));
        }
        if (datos.containsKey("disertantes")) {
             Object obj = datos.get("disertantes");
             if (obj instanceof List) {
                 this.setDisertantes(new ArrayList<>((List<String>) obj));
             } else if (obj instanceof String && !((String)obj).trim().isEmpty()) {
                 this.setDisertantes(new ArrayList<>(java.util.Arrays.asList(((String)obj).split(","))));
             }
        }
    }

    /**
     * Clase interna para encapsular los detalles del seminario
     */
    public static class SeminarioDetalle {
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
        
        // Específicos de seminario
        private String lugar;
        private String enlace;
        private String publicoObjetivo;
        private Integer duracionMinutos;
        private List<String> disertantes;
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

        public String getImagenUrl() { return imagenUrl; }
        public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
        
        public String getLugar() { return lugar; }
        public void setLugar(String lugar) { this.lugar = lugar; }
        
        public String getEnlace() { return enlace; }
        public void setEnlace(String enlace) { this.enlace = enlace; }
        
        public String getPublicoObjetivo() { return publicoObjetivo; }
        public void setPublicoObjetivo(String publicoObjetivo) { this.publicoObjetivo = publicoObjetivo; }
        
        public Integer getDuracionMinutos() { return duracionMinutos; }
        public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
        
        public List<String> getDisertantes() { return disertantes; }
        public void setDisertantes(List<String> disertantes) { this.disertantes = disertantes; }
        
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
