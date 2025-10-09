package com.example.demo.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.demo.enums.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class OfertaAcademica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOferta;
    private String nombre;
    private String descripcion;
    private String duracion;
    private Double costoInscripcion;
    
    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;
    
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String objetivo;
    private Boolean certificado;
    
    @Enumerated(EnumType.STRING)
    private EstadoOferta estado;
    
    private Integer cupos;
    private Boolean visibilidad;
    
    @ManyToMany
    @JoinTable(
        name = "oferta_categoria",
        joinColumns = @JoinColumn(name = "oferta_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private List<Categoria> categorias;
    
    @ManyToOne
    @JoinColumn(name = "instituto_id")
    private Instituto instituto;
    
    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL)
    private List<Inscripciones> inscripciones;

        public Boolean getEstaActiva() {
            boolean estaActiva;
            if(this.estado == EstadoOferta.ACTIVA && 
               (this.fechaInicio.isBefore(LocalDate.now()) || this.fechaInicio.isEqual(LocalDate.now())) &&
               (this.fechaFin.isAfter(LocalDate.now()) || this.fechaFin.isEqual(LocalDate.now())) &&
               (this.cupos == null || this.cupos > 0)) {
                estaActiva = true;
            } else {
                estaActiva = false;
            }
        return estaActiva;
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
        if (cupos == null) return "Sin límite";
        int ocupados = inscripciones != null ? inscripciones.size() : 0;
        return ocupados + " / " + cupos;
    }
    /**
     * Porcentaje de ocupación de cupos
     */
    public Double getPorcentajeOcupacion() {
        if (cupos == null || cupos == 0) return 0.0;
        int ocupados = inscripciones != null ? inscripciones.size() : 0;
        return (ocupados * 100.0) / cupos;
    }
    /**
     * Verifica si tiene cupos disponibles
     */
    public Boolean tieneCuposDisponibles() {
        if (cupos == null) return true;
        return getPorcentajeOcupacion() < 100;
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
     * Verifica si la oferta puede ser editada
     */
    public Boolean puedeSerEditada() {
        return estado != EstadoOferta.FINALIZADA && 
               estado != EstadoOferta.CANCELADA;
    }
    /**
     * Verifica si puede ser eliminada
     */
    public Boolean puedeSerEliminada() {
        int inscriptos = inscripciones != null ? inscripciones.size() : 0;
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

}
