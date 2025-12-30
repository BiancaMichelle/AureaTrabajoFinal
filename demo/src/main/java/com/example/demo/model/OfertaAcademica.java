package com.example.demo.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "oferta_academica", uniqueConstraints = {@UniqueConstraint(name = "uk_oferta_nombre", columnNames = {"nombre"})})

public class OfertaAcademica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOferta;
    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;
    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;
    private String duracion;
    @Min(1)
    private Integer duracionMeses; // Duración calculada en meses
    @NotNull(message = "El costo de inscripción no puede estar vacío")
    private Double costoInscripcion;
    
    @Min(0)
    private Double recargoMora;
    

    @OneToMany(mappedBy = "ofertaAcademica", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Horario> horarios = new ArrayList<>();

    
    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;
    @NotNull(message = "La fecha de inicio no puede estar vacía")
    private LocalDate fechaInicio;
    @NotNull(message = "La fecha de fin no puede estar vacía")
    private LocalDate fechaFin;
    @NotNull(message = "El campo certificado no puede estar vacío")
    private Boolean certificado;
    
    // Campos de ubicación/acceso (movidos desde subclases)
    private String lugar;
    private String enlace;
    
    @Enumerated(EnumType.STRING)
    private EstadoOferta estado;
    @Min(1)
    private Integer cupos;
    private Boolean visibilidad;
    
    @ManyToMany(fetch = FetchType.EAGER) // Cambié a EAGER para facilitar
    @JoinTable(
        name = "oferta_categoria",
        joinColumns = @JoinColumn(name = "oferta_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private List<Categoria> categorias = new ArrayList<>(); // Inicializa la lista
    
    @ManyToOne
    @JoinColumn(name = "instituto_id")
    private Instituto instituto;
    
    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL)
    private List<Inscripciones> inscripciones;

        public Boolean getEstaActiva() {
        // Consideramos activa si está en curso o planificada (ACTIVA)
        // y tiene cupos disponibles (opcional, según lógica de negocio)
        return (this.estado == EstadoOferta.ACTIVA || this.estado == EstadoOferta.ENCURSO) &&
               (this.cupos == null || this.cupos > 0);
    }

    /**
     * Devuelve el tipo específico de oferta para mostrar en la tabla
     */
    public String getTipoOferta() {
        if (this instanceof Curso) return "CURSO";
        if (this instanceof Formacion) return "FORMACION"; 
        if (this instanceof Seminario) return "SEMINARIO";
        if (this instanceof Charla) return "CHARLA";
        return "GENERAL";
    }
    
    /**
     * Devuelve el tipo para consultas de base de datos
     */
    public String getTipo() {
        return getTipoOferta();
    }

    /**
     * Devuelve el ícono CSS según el tipo para la interfaz
     */
    public String getIconoTipo() {
        switch (getTipoOferta()) {
            case "CURSO": return "fas fa-book";
            case "FORMACION": return "fas fa-graduation-cap";
            case "SEMINARIO": return "fas fa-users";
            case "CHARLA": return "fas fa-microphone";
            default: return "fas fa-folder";
        }
    }
    /**
     * Devuelve el costo formateado para mostrar en la tabla
     */
    public String getCostoFormateado() {
        if (costoInscripcion == null || costoInscripcion == 0) {
            return "Gratuito";
        }
        return String.format("$%.2f", costoInscripcion);
    }
    /**
     * Información de cupos para la tabla
     */
    public String getInfoCupos() {
        if (cupos == null || cupos == Integer.MAX_VALUE) return "Sin límite";
        int ocupados = inscripciones != null ? inscripciones.size() : 0;
        return ocupados + " / " + cupos;
    }
    /**
     * Porcentaje de ocupación de cupos
     */
    public Double getPorcentajeOcupacion() {
        if (cupos == null || cupos == 0 || cupos == Integer.MAX_VALUE) return 0.0;
        int ocupados = inscripciones != null ? inscripciones.size() : 0;
        return (ocupados * 100.0) / cupos;
    }
    /**
     * Verifica si tiene cupos disponibles
     */
    public Boolean tieneCuposDisponibles() {
        if (cupos == null || cupos == Integer.MAX_VALUE) return true;
        return getPorcentajeOcupacion() < 100;
    }
    
    /**
     * Obtiene la cantidad de cupos disponibles
     */
    public Integer getCuposDisponibles() {
        if (cupos == null || cupos == Integer.MAX_VALUE) return null; // Sin límite
        int ocupados = inscripciones != null ? inscripciones.size() : 0;
        return Math.max(0, cupos - ocupados);
    }
    /**
     * Devuelve las categorías como texto para filtros
     */
    public String getCategoriasTexto() {
        if (categorias == null || categorias.isEmpty()) {
            return "Sin categorías";
        }
        return categorias.stream()
                .map(Categoria::getNombre)
                .collect(Collectors.joining(", "));
    }
    /**
     * Duración calculada en días
     */
    public Long getDuracionDias() {
        if (fechaInicio == null || fechaFin == null) return null;
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
    }
    
    /**
     * Duración calculada en meses (aproximada)
     */
    public Long getDuracionMeses() {
        // Si ya tenemos el valor calculado, usarlo
        if (duracionMeses != null) {
            return duracionMeses.longValue();
        }
        
        // Calcular basado en fechas
        Long dias = getDuracionDias();
        if (dias == null) return null;
        return Math.max(1, Math.round(dias / 30.0)); // Mínimo 1 mes
    }
    
    /**
     * Calcula y establece la duración en meses
     */
    public void calcularDuracionMeses() {
        Long meses = getDuracionMeses();
        if (meses != null) {
            this.duracionMeses = meses.intValue();
        }
    }
    /**
     * Verifica si la oferta puede ser editada
     */
    public Boolean puedeSerEditada() {
        return estado != EstadoOferta.FINALIZADA && 
               estado != EstadoOferta.DE_BAJA;
    }
    /**
     * Verifica si puede ser eliminada
     */
    public Boolean puedeSerEliminada() {
        // Lógica defensiva por si algo es null
        if (this.estado == null) {
            return false; // Si no hay estado definido, no se puede eliminar por seguridad
        }
        
        int inscriptos = inscripciones != null ? inscripciones.size() : 0;
        
        // Puede ser eliminada solo si no hay inscriptos y no está finalizada
        return inscriptos == 0 && estado != EstadoOferta.FINALIZADA;
    }
    
    /**
     * Duración formateada para mostrar
     */
    public String getDuracionTexto() {
        Long dias = getDuracionDias();
        if (dias == null) return "No definida";
        if (dias == 1) return "1 día";
        if (dias < 7) return dias + " días";
        if (dias < 30) return (dias / 7) + " semanas";
        return (dias / 30) + " meses";
    }
    /**
     * Devuelve la clase CSS para el badge de estado
     */
    public String getClaseEstado() {
        if (estado == null) return "status-inactiva";
        return "status-" + estado.name().toLowerCase();
    }

    /**
     * Verifica si la oferta puede cambiar de estado (dar de baja/alta)
     */
    public Boolean puedeCambiarEstado() {
        return estado != EstadoOferta.FINALIZADA && estado != EstadoOferta.DE_BAJA;
    }

    /**
     * Verifica si se puede dar de baja la oferta
     * - No debe tener inscripciones activas
     * - Si ya comenzó pero no tiene inscripciones, se puede dar de baja
     * - Si la fecha de fin ya pasó, se puede dar de baja
     */
    public Boolean puedeDarseDeBaja() {
        // No se puede dar de baja si ya está de baja, finalizada o cancelada
        if (estado == EstadoOferta.DE_BAJA || 
            estado == EstadoOferta.FINALIZADA) {
            return false;
        }

        LocalDate ahora = LocalDate.now();
        int inscripcionesActivas = contarInscripcionesActivas();

        // Si ya terminó la oferta, siempre se puede dar de baja
        if (fechaFin != null && fechaFin.isBefore(ahora)) {
            return true;
        }

        // Si no hay inscripciones activas, se puede dar de baja
        if (inscripcionesActivas == 0) {
            return true;
        }

        // Si ya comenzó y tiene inscripciones activas, no se puede dar de baja
        if (fechaInicio != null && !fechaInicio.isAfter(ahora) && inscripcionesActivas > 0) {
            return false;
        }

        // En otros casos (no ha comenzado pero tiene inscripciones), también se puede dar de baja
        return true;
    }

    /**
     * Cuenta las inscripciones activas (no canceladas)
     */
    private int contarInscripcionesActivas() {
        if (inscripciones == null) {
            return 0;
        }
        return (int) inscripciones.stream()
                .filter(inscripcion -> inscripcion.getEstadoInscripcion() != null && 
                       inscripcion.getEstadoInscripcion() == true)
                .count();
    }

    /**
     * Cambia el estado de la oferta con validaciones
     */
    public Boolean cambiarEstado(EstadoOferta nuevoEstado) {
        if (!puedeCambiarEstado()) {
            return false;
        }

        LocalDate hoy = LocalDate.now();

        // Validaciones específicas según el nuevo estado
        switch (nuevoEstado) {
            case DE_BAJA:
                if (!puedeDarseDeBaja()) {
                    return false;
                }
                break;
            case ACTIVA:
                // ACTIVA: Si todavía no comenzó la fecha de inicio
                if (fechaInicio != null && !fechaInicio.isAfter(hoy)) {
                    return false; // No puede ser ACTIVA si ya comenzó o pasó
                }
                break;
            case ENCURSO:
                // EN CURSO: Cuando la fecha de inicio llega (y no ha finalizado)
                if (fechaInicio != null && fechaInicio.isAfter(hoy)) {
                    return false; // No puede ser EN CURSO si es futura
                }
                if (fechaFin != null && fechaFin.isBefore(hoy)) {
                    return false; // No puede ser EN CURSO si ya finalizó
                }
                break;
            case FINALIZADA:
                // FINALIZADA: Cuando la fecha de fin pasó
                if (fechaFin == null || !fechaFin.isBefore(hoy)) {
                    return false;
                }
                break;
        }

        this.estado = nuevoEstado;
        return true;
    }

    /**
     * Dar de baja la oferta (cambiar a INACTIVA)
     */
    public Boolean darDeBaja() {
        return cambiarEstado(EstadoOferta.DE_BAJA);
    }

    /**
     * Dar de alta la oferta (cambiar a ACTIVA o ENCURSO según fecha)
     */
    public Boolean darDeAlta() {
        LocalDate hoy = LocalDate.now();
        // Determinar el estado correcto basado en la fecha
        if (fechaInicio != null && !fechaInicio.isAfter(hoy)) {
            // Si ya comenzó, intentamos ponerla EN CURSO
            return cambiarEstado(EstadoOferta.ENCURSO);
        }
        // Si es futura, ACTIVA
        return cambiarEstado(EstadoOferta.ACTIVA);
    }

    /**
     * Verifica y actualiza el estado a FINALIZADA o ENCURSO automáticamente
     */
    public boolean actualizarEstadoSiFinalizada() {
        LocalDate hoy = LocalDate.now();
        boolean cambiado = false;

        // 1. Verificar si finalizó (fecha fin pasó)
        if (this.fechaFin != null && this.fechaFin.isBefore(hoy)) {
            if (this.estado != EstadoOferta.FINALIZADA && this.estado != EstadoOferta.DE_BAJA) {
                this.estado = EstadoOferta.FINALIZADA;
                cambiado = true;
            }
        }
        // 2. Verificar si comenzó (ACTIVA -> ENCURSO)
        else if (this.fechaInicio != null && !this.fechaInicio.isAfter(hoy)) {
            // Si hoy es >= fechaInicio y estado es ACTIVA, pasar a ENCURSO
            if (this.estado == EstadoOferta.ACTIVA) {
                this.estado = EstadoOferta.ENCURSO;
                cambiado = true;
            }
        }
        
        return cambiado;
    }

    /**
     * Modifica los datos básicos de la oferta
     */
    public void modificarDatosBasicos(String nombre, String descripcion, String duracion,
                                    LocalDate fechaInicio, LocalDate fechaFin, 
                                    Modalidad modalidad, Integer cupos, Boolean visibilidad,
                                    Double costoInscripcion, Double recargoMora, Boolean certificado,
                                    String lugar, String enlace) {
        if (nombre != null && !nombre.trim().isEmpty()) {
            this.nombre = nombre.trim();
        }
        
        if (descripcion != null) {
            this.descripcion = descripcion.trim();
        }
        
        if (duracion != null) {
            this.duracion = duracion.trim();
        }
        
        if (fechaInicio != null) {
            this.fechaInicio = fechaInicio;
        }
        
        if (fechaFin != null) {
            this.fechaFin = fechaFin;
            // Recalcular duración en meses
            calcularDuracionMeses();
        }
        
        if (modalidad != null) {
            this.modalidad = modalidad;
        }
        
        // Permitir null o MAX_VALUE para cupos ilimitados
        if (cupos != null) {
            this.cupos = cupos;
        }
        
        if (visibilidad != null) {
            this.visibilidad = visibilidad;
        }
        
        if (costoInscripcion != null && costoInscripcion >= 0) {
            this.costoInscripcion = costoInscripcion;
        }

        if (recargoMora != null && recargoMora >= 0) {
            this.recargoMora = recargoMora;
        }
        
        if (certificado != null) {
            this.certificado = certificado;
        }
        
        // Actualizar lugar y enlace (permitir null si se quiere limpiar, o manejar lógica de negocio)
        // Aquí asumimos que si viene null no se actualiza, si viene vacío se limpia
        if (lugar != null) {
            this.lugar = lugar;
        }
        if (enlace != null) {
            this.enlace = enlace;
        }
    }

    /**
     * Valida si los datos de la oferta son consistentes
     */
    public List<String> validarDatos() {
        List<String> errores = new ArrayList<>();
        
        if (nombre == null || nombre.trim().isEmpty()) {
            errores.add("El nombre es obligatorio");
        }
        
        if (fechaInicio == null) {
            errores.add("La fecha de inicio es obligatoria");
        }
        
        if (fechaFin == null) {
            errores.add("La fecha de fin es obligatoria");
        }
        
        if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            errores.add("La fecha de fin debe ser posterior a la fecha de inicio");
        }
        
        if (cupos != null && cupos <= 0) {
            errores.add("Los cupos deben ser mayor a 0");
        }
        
        if (costoInscripcion != null && costoInscripcion < 0) {
            errores.add("El costo de inscripción no puede ser negativo");
        }
        
        return errores;
    }

}
