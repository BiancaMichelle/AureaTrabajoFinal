package com.example.demo.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;
import com.example.demo.enums.EstadoProcesoCertificacion;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    

    // Horarios semanales (solo para CURSO y FORMACION)
    // Las Charlas y Seminarios tienen fecha/hora específica, no necesitan horarios semanales
    @OneToMany(mappedBy = "ofertaAcademica", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Horario> horarios = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "aula_id")
    private Aula aula;
    
    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;
    @NotNull(message = "La fecha de inicio no puede estar vacía")
    private LocalDate fechaInicio;
    @NotNull(message = "La fecha de fin no puede estar vacía")
    private LocalDate fechaFin;
    
    // Fechas de inscripción
    @NotNull(message = "La fecha de inicio de inscripción no puede estar vacía")
    private LocalDate fechaInicioInscripcion;
    @NotNull(message = "La fecha de fin de inscripción no puede estar vacía")
    private LocalDate fechaFinInscripcion;
    
    @NotNull(message = "El campo certificado no puede estar vacío")
    private Boolean certificado;

    @Enumerated(EnumType.STRING)
    private EstadoProcesoCertificacion estadoProcesoCertificacion = EstadoProcesoCertificacion.EN_GESTION_CERTIFICACION;
    
    // Campos de ubicación/acceso (movidos desde subclases)
    private String lugar;
    private String enlace;
    
    @Enumerated(EnumType.STRING)
    private EstadoOferta estado;
    @Min(1)
    private Integer cupos;
    private Boolean visibilidad;
    private Boolean permiteInscripcionTardia = false;

    // URL de la imagen de presentación
    private String imagenUrl;

    // Constructor personalizado para subclases
    public OfertaAcademica(String nombre, String descripcion, Modalidad modalidad, Double costoInscripcion, 
                           LocalDate fechaInicio, LocalDate fechaFin, Integer cupos, Integer duracionMeses, 
                           Boolean certificado, EstadoOferta estado) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.modalidad = modalidad;
        this.costoInscripcion = costoInscripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.cupos = cupos;
        this.duracionMeses = duracionMeses;
        this.certificado = certificado;
        this.estado = estado;
        this.visibilidad = true; // Default
        this.permiteInscripcionTardia = false;
        this.aula = new Aula();
        // Por defecto, inscripciones abiertas desde hoy hasta el inicio
        this.fechaInicioInscripcion = LocalDate.now();
        this.fechaFinInscripcion = fechaInicio != null ? fechaInicio : LocalDate.now().plusMonths(1);
    }
    
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
    
    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Inscripciones> inscripciones;

    @PrePersist
    @PreUpdate
    public void validarOferta() {
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalStateException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        
        if (fechaInicioInscripcion != null && fechaFinInscripcion != null && fechaInicioInscripcion.isAfter(fechaFinInscripcion)) {
            throw new IllegalStateException("La fecha de inicio de inscripción no puede ser posterior a la fecha de fin de inscripción.");
        }
        
    }

    // Helper methods para gestionar la relación bidireccional con Horario
    public void addHorario(Horario horario) {
        if (this.horarios == null) {
            this.horarios = new ArrayList<>();
        }
        this.horarios.add(horario);
        horario.setOfertaAcademica(this);
    }

    public void removeHorario(Horario horario) {
        if (this.horarios != null) {
            this.horarios.remove(horario);
            horario.setOfertaAcademica(null);
        }
    }

    /**
     * Valida si el nombre de la oferta está duplicado en la base de datos.
     * Devuelve una lista de errores (vacía si no hay conflicto).
     */
    public java.util.List<String> validarDuplicado(com.example.demo.repository.OfertaAcademicaRepository ofertaRepo) {
        java.util.List<String> errores = new java.util.ArrayList<>();
        if (this.nombre == null || this.nombre.trim().isEmpty()) return errores;

        java.util.Optional<OfertaAcademica> existente = ofertaRepo.findByNombreIgnoreCase(this.nombre.trim());
        if (existente.isPresent()) {
            OfertaAcademica otra = existente.get();
            // Si es la misma entidad (actualización), no consideramos duplicado
            if (this.idOferta == null || !this.idOferta.equals(otra.getIdOferta())) {
                errores.add("Ya existe una oferta con el mismo nombre: '" + this.nombre + "'");
            }
        }
        return errores;
    }

        public Boolean getEstaActiva() {
        // Consideramos activa si está en curso o planificada (ACTIVA)
        // y tiene cupos disponibles (opcional, según lógica de negocio)
        return (this.estado == EstadoOferta.ACTIVA || this.estado == EstadoOferta.ENCURSO) &&
               (this.cupos == null || this.cupos > 0);
    }
    
    /**
     * Verifica si las inscripciones están abiertas actualmente
     */
    public Boolean getInscripcionesAbiertas() {
        if (fechaInicioInscripcion == null || fechaFinInscripcion == null) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaInicioInscripcion) && !hoy.isAfter(fechaFinInscripcion);
    }
    
    /**
     * Obtiene el estado de inscripción para mostrar en la UI
     */
    public String getEstadoInscripcion() {
        if (fechaInicioInscripcion == null || fechaFinInscripcion == null) {
            return "No disponible";
        }
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(fechaInicioInscripcion)) {
            return "Próximamente";
        } else if (hoy.isAfter(fechaFinInscripcion)) {
            return "Cerradas";
        } else {
            return "Abiertas";
        }
    }
    
    /**
     * Verifica si se puede inscribir considerando fechas y cupos
     */
    public Boolean getPuedeInscribirse() {
        return getInscripcionesAbiertas() && tieneCuposDisponibles() && 
               (estado == EstadoOferta.ACTIVA || estado == EstadoOferta.ENCURSO);
    }

    public Boolean getHabilitarCalculoCertificacion() {
        if (this.fechaFin == null) return false;
        
        // Habilitado si el estado es FINALIZADA o CERRADA
        if (this.estado == EstadoOferta.FINALIZADA || this.estado == EstadoOferta.CERRADA) {
            return true;
        }

        // O si faltan 15 días o menos para finalizar
        LocalDate hoy = LocalDate.now();
        LocalDate fechaHabilitacion = this.fechaFin.minusDays(15);
        
        return !hoy.isBefore(fechaHabilitacion); 
    }

    /**
     * Devuelve el tipo específico de oferta para mostrar en la tabla
     */
    public String getTipoOferta() {
        if (this instanceof Curso) return "Curso";
        if (this instanceof Formacion) return "Formación"; 
        if (this instanceof Seminario) return "Seminario";
        if (this instanceof Charla) return "Charla";
        return "General";
    }

    public String getDuracionTexto() {
        if (this instanceof Curso) {
            return ((Curso) this).getDuracionMeses() + " Meses";
        }
        if (this instanceof Formacion) {
            return ((Formacion) this).getDuracionMeses() + " Meses";
        }
        if (this instanceof Charla) {
             Integer mins = ((Charla) this).getDuracionEstimada();
             return (mins != null ? mins : 0) + " Minutos";
        }
        // Seminario o Default: calcular dias
        if (fechaInicio != null && fechaFin != null) {
            long diff = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin);
            if (diff == 0) return "1 Día";
            return (diff + 1) + " Días";
        }
        return "N/A";
    }

    public String getPrecioDetalle() {
        String moneda = "$"; 
        if (this instanceof Curso) {
            Curso c = (Curso) this;
            boolean tieneCuotas = c.getNrCuotas() != null && c.getNrCuotas() > 0
                    && c.getCostoCuota() != null && c.getCostoCuota() > 0;
            if (!tieneCuotas) {
                return "Inscripción: " + moneda + formatMoney(costoInscripcion);
            }
            return "Inscripción: " + moneda + formatMoney(costoInscripcion) +
                   " + " + c.getNrCuotas() + " cuotas de " + moneda + formatMoney(c.getCostoCuota());
        }
        if (this instanceof Formacion) {
            Formacion f = (Formacion) this;
            boolean tieneCuotas = f.getNrCuotas() != null && f.getNrCuotas() > 0
                    && f.getCostoCuota() != null && f.getCostoCuota() > 0;
            if (!tieneCuotas) {
                return "Inscripción: " + moneda + formatMoney(costoInscripcion);
            }
            return "Inscripción: " + moneda + formatMoney(costoInscripcion) +
                   " + " + f.getNrCuotas() + " cuotas de " + moneda + formatMoney(f.getCostoCuota());
        }
        // Seminario y Charla (solo costo inscripcion)
        if (costoInscripcion == null || costoInscripcion == 0) {
            return "Gratuito";
        }
        return "Costo único: " + moneda + formatMoney(costoInscripcion);
    }
    
    /**
     * Calcula la carga horaria semanal en horas basándose en los horarios asignados.
     */
    public Double getCargaHorariaSemanal() {
        if (horarios == null || horarios.isEmpty()) {
            return 0.0;
        }
        double totalHoras = 0.0;
        for (Horario h : horarios) {
            if (h.getHoraInicio() != null && h.getHoraFin() != null) {
                long diffMs = h.getHoraFin().getTime() - h.getHoraInicio().getTime();
                // ms -> horas
                double horas = diffMs / (1000.0 * 60.0 * 60.0);
                totalHoras += horas;
            }
        }
        return Math.round(totalHoras * 100.0) / 100.0; // Redondear 2 decimales
    }

    /**
     * Devuelve el título o certificación que otorga.
     * Puede ser sobreescrito por las subclases.
     */
    public String getTituloCertificacion() {
        if (Boolean.TRUE.equals(certificado)) {
            return "Certificado de Aprobación";
        }
        return "Constancia de Participación";
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
            case "Curso": return "fas fa-book";
            case "Formación": return "fas fa-graduation-cap";
            case "Seminario": return "fas fa-users";
            case "Charla": return "fas fa-microphone";
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
        return "$" + formatMoney(costoInscripcion);
    }

    private String formatMoney(Double value) {
        double safeValue = value != null ? value : 0.0;
        return String.format(Locale.forLanguageTag("es-AR"), "%,.2f", safeValue);
    }
    /**
     * Información de cupos para la tabla
     */
    public String getInfoCupos() {
        if (cupos == null || cupos == Integer.MAX_VALUE) return "Sin límite";
        long ocupados = inscripciones != null ? inscripciones.stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count() : 0;
        return ocupados + " / " + cupos;
    }
    /**
     * Porcentaje de ocupación de cupos
     */
    public Double getPorcentajeOcupacion() {
        if (cupos == null || cupos == 0 || cupos == Integer.MAX_VALUE) return 0.0;
        long ocupados = inscripciones != null ? inscripciones.stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count() : 0;
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
        long ocupados = inscripciones != null ? inscripciones.stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count() : 0;
        return Math.max(0, cupos - (int)ocupados);
    }

    /**
     * Obtiene la cantidad de alumnos activos (inscripción activa)
     */
    public Long getCantidadAlumnosActivos() {
        return inscripciones != null 
            ? inscripciones.stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count() 
            : 0L;
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
        return estado != EstadoOferta.FINALIZADA;
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
        return estado != EstadoOferta.FINALIZADA;
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
            case CERRADA:
                // CERRADA: Solo puede cerrarse desde FINALIZADA
                if (this.estado != EstadoOferta.FINALIZADA) {
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
        
        // Validación: La fecha de inicio no debe ser anterior a hoy
        if (fechaInicio != null && fechaInicio.isBefore(hoy)) {
            return false;
        }

        // Determinar el estado correcto basado en la fecha
        if (fechaInicio != null && !fechaInicio.isAfter(hoy)) {
            // Si ya comenzó (solo HOY, pues anteriores se filtran arriba), intentamos ponerla EN CURSO
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
     * Indica si la fecha está dentro del rango de la oferta (inicio/fin) y no es futura.
     */
    public boolean estaDentroDeRango(LocalDate fecha) {
        if (fecha == null) return false;
        LocalDate hoy = LocalDate.now();
        if (fecha.isAfter(hoy)) return false; // no permitir futuras
        if (this.fechaInicio != null && fecha.isBefore(this.fechaInicio)) return false;
        if (this.fechaFin != null && fecha.isAfter(this.fechaFin)) return false;
        return true;
    }

    /**
     * Indica si el día corresponde a alguno de los horarios programados (por día de semana).
     * Si no hay horarios definidos, devuelve false para exigir definición explícita.
     */
    public boolean esDiaDeClase(LocalDate fecha) {
        if (fecha == null) return false;
        if (this.horarios == null || this.horarios.isEmpty()) return false;
        java.time.DayOfWeek dow = fecha.getDayOfWeek();
        for (Horario h : this.horarios) {
            if (h != null && h.getDia() != null) {
                switch (h.getDia()) {
                    case LUNES: if (dow == java.time.DayOfWeek.MONDAY) return true; break;
                    case MARTES: if (dow == java.time.DayOfWeek.TUESDAY) return true; break;
                    case MIERCOLES: if (dow == java.time.DayOfWeek.WEDNESDAY) return true; break;
                    case JUEVES: if (dow == java.time.DayOfWeek.THURSDAY) return true; break;
                    case VIERNES: if (dow == java.time.DayOfWeek.FRIDAY) return true; break;
                    case SABADO: if (dow == java.time.DayOfWeek.SATURDAY) return true; break;
                    case DOMINGO: if (dow == java.time.DayOfWeek.SUNDAY) return true; break;
                }
            }
        }
        return false;
    }

    /**
     * Valida en el modelo si el alumno puede registrar asistencia en una fecha dada.
     * Reglas:
     * - Fecha dentro del rango de la oferta y no futura.
     * - Fecha no anterior a la fecha de inscripción del alumno.
     * - Fecha corresponde a un día de clase según horarios.
     */
    public boolean puedeRegistrarAsistencia(LocalDate fecha, LocalDate fechaInscripcionAlumno) {
        if (!estaDentroDeRango(fecha)) return false;
        if (fechaInscripcionAlumno != null && fecha.isBefore(fechaInscripcionAlumno)) return false;
        return esDiaDeClase(fecha);
    }

    /**
     * Modifica los datos básicos de la oferta
     */
    public void modificarDatosBasicos(String nombre, String descripcion, String duracion,
                                    LocalDate fechaInicio, LocalDate fechaFin, 
                                    Modalidad modalidad, Integer cupos, Boolean visibilidad,
                                    Double costoInscripcion, Double recargoMora, Boolean certificado,
                                    String lugar, String enlace,
                                    LocalDate fechaInicioInscripcion, LocalDate fechaFinInscripcion) {
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
        
        if (fechaInicioInscripcion != null) {
            this.fechaInicioInscripcion = fechaInicioInscripcion;
        }
        
        if (fechaFinInscripcion != null) {
            this.fechaFinInscripcion = fechaFinInscripcion;
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
        
        if (fechaInicioInscripcion == null) {
            errores.add("La fecha de inicio de inscripción es obligatoria");
        }
        
        if (fechaFinInscripcion == null) {
            errores.add("La fecha de fin de inscripción es obligatoria");
        }
        
        if (fechaInicioInscripcion != null && fechaFinInscripcion != null && fechaFinInscripcion.isBefore(fechaInicioInscripcion)) {
            errores.add("La fecha de fin de inscripción debe ser posterior a la fecha de inicio de inscripción");
        }
        
        // REMOVIDO: Validación que las inscripciones deben cerrar antes del inicio
        // Las inscripciones pueden continuar incluso después del inicio de la oferta
        
        if (cupos != null && cupos <= 0) {
            errores.add("Los cupos deben ser mayor a 0");
        }
        
        if (costoInscripcion != null && costoInscripcion < 0) {
            errores.add("El costo de inscripción no puede ser negativo");
        }
        
        return errores;
    }

    /**
     * Actualiza los datos comunes de la oferta desde un mapa de datos.
     * Este método implementa la lógica de modificación delegada al modelo.
     */
    public void actualizarDatos(java.util.Map<String, Object> datos) {
        if (datos.containsKey("nombre")) {
            this.setNombre((String) datos.get("nombre"));
        }
        if (datos.containsKey("descripcion")) {
            this.setDescripcion((String) datos.get("descripcion"));
        }
        if (datos.containsKey("cupos")) {
            this.setCupos(convertirEntero(datos.get("cupos")));
        }
        if (datos.containsKey("costoInscripcion")) {
            this.setCostoInscripcion(convertirDouble(datos.get("costoInscripcion")));
        }
        if (datos.containsKey("recargoMora")) {
            this.setRecargoMora(convertirDouble(datos.get("recargoMora")));
        }
        if (datos.containsKey("fechaInicio")) {
            this.setFechaInicio(convertirFecha(datos.get("fechaInicio")));
        }
        if (datos.containsKey("fechaFin")) {
            this.setFechaFin(convertirFecha(datos.get("fechaFin")));
        }
        if (datos.containsKey("fechaInicioInscripcion")) {
            this.setFechaInicioInscripcion(convertirFecha(datos.get("fechaInicioInscripcion")));
        }
        if (datos.containsKey("fechaFinInscripcion")) {
            this.setFechaFinInscripcion(convertirFecha(datos.get("fechaFinInscripcion")));
        }
        if (datos.containsKey("modalidad")) {
             String modStr = (String) datos.get("modalidad");
             if (modStr != null && !modStr.isEmpty()) {
                 try {
                     this.setModalidad(Modalidad.valueOf(modStr.toUpperCase()));
                 } catch (IllegalArgumentException e) {
                     // Ignorar valor inválido
                 }
             }
        }
        if (datos.containsKey("certificado")) {
             Object cert = datos.get("certificado");
             if (cert instanceof String) {
                 this.setCertificado(Boolean.parseBoolean((String)cert) || "on".equals(cert) || "true".equalsIgnoreCase((String)cert));
             } else if (cert instanceof Boolean) {
                 this.setCertificado((Boolean) cert);
             }
        }
        if (datos.containsKey("lugar")) {
            this.setLugar((String) datos.get("lugar"));
        }
        if (datos.containsKey("enlace")) {
            this.setEnlace((String) datos.get("enlace"));
        }
        if (datos.containsKey("imagenUrl") && datos.get("imagenUrl") != null) {
            this.setImagenUrl((String) datos.get("imagenUrl"));
        }
    }

    // Helper methods for safe conversion
    protected Integer convertirEntero(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Integer) return (Integer) valor;
        if (valor instanceof String && !((String)valor).isEmpty()) {
            try { return Integer.parseInt((String) valor); } catch(NumberFormatException e) { return null; }
        }
        return null;
    }

    protected Double convertirDouble(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Double) return (Double) valor;
        if (valor instanceof Integer) return ((Integer) valor).doubleValue();
        if (valor instanceof String && !((String)valor).isEmpty()) {
            try { return Double.parseDouble((String) valor); } catch(NumberFormatException e) { return null; }
        }
        return null;
    }

    protected LocalDate convertirFecha(Object valor) {
        if (valor == null) return null;
        if (valor instanceof LocalDate) return (LocalDate) valor;
        if (valor instanceof java.sql.Date) return ((java.sql.Date) valor).toLocalDate();
        if (valor instanceof String && !((String)valor).isEmpty()) {
            try { return LocalDate.parse((String) valor); } catch(Exception e) { return null; }
        }
        return null;
    }

}
