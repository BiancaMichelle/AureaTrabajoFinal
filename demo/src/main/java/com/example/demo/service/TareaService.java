package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.event.ActivityCreatedEvent;
import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Auditable;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Tarea;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.TareaRepository;

@Service
public class TareaService {

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Crea una nueva tarea siguiendo el flujo del CU-30.
     * 
     * Precondiciones (validadas):
     * - El módulo debe existir
     * - El curso (OfertaAcademica) debe existir y estar en estado ACTIVA
     * - El docente debe tener permisos (validado en el controller con @PreAuthorize)
     * 
     * @param tarea La tarea a crear con los datos completados
     * @param moduloId El ID del módulo donde se creará la tarea
     * @return La tarea creada
     * @throws RuntimeException si no se cumplen las precondiciones o validaciones
     */
    @Transactional
    @Auditable(action = "CREAR_TAREA", entity = "Tarea")
    public Tarea crearTarea(Tarea tarea, UUID moduloId) {
        // Paso 8 del CU-30: Validar datos ingresados
        
        // Precondición 1: El módulo debe existir
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new RuntimeException("El módulo no existe"));

        // Precondición 2: El curso debe existir y estar en estado ACTIVA o ENCURSO
        OfertaAcademica curso = modulo.getCurso();
        if (curso == null) {
            throw new RuntimeException("El módulo no está asociado a un curso válido");
        }
        
        if (curso.getEstado() != EstadoOferta.ACTIVA && curso.getEstado() != EstadoOferta.ENCURSO) {
            throw new RuntimeException("El curso debe estar en estado 'ACTIVA' o 'EN CURSO' para crear tareas. Estado actual: " + curso.getEstado());
        }

        // Validación: Campos obligatorios
        if (tarea.getTitulo() == null || tarea.getTitulo().trim().isEmpty()) {
            throw new RuntimeException("El nombre de la tarea es obligatorio");
        }

        if (tarea.getDescripcion() == null || tarea.getDescripcion().trim().isEmpty()) {
            throw new RuntimeException("La descripción es obligatoria");
        }

        if (tarea.getLimiteEntrega() == null) {
            throw new RuntimeException("La fecha y hora límite de entrega es obligatoria");
        }

        if (tarea.getTipoEntrega() == null || tarea.getTipoEntrega().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos un tipo de entrega (texto o archivos)");
        }

        // Validación: La fecha límite debe ser futura
        if (tarea.getLimiteEntrega().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("La fecha límite de entrega debe ser futura");
        }

        // Establecer valores por defecto si no se especificaron
        if (tarea.getEntregasTardias() == null) {
            tarea.setEntregasTardias(false);
        }

        if (tarea.getModificaciones() == null) {
            tarea.setModificaciones(false);
        }

        if (tarea.getVisibilidad() == null) {
            tarea.setVisibilidad(true); // Por defecto, visible
        }

        // Asociar la tarea al módulo
        tarea.setModulo(modulo);
        tarea.setFechaCreacion(LocalDateTime.now());

        // Paso 10: Guardar la tarea en el sistema
        Tarea tareaGuardada = tareaRepository.save(tarea);

        // Paso 11: Notificar a los alumnos (solo si la tarea está visible)
        if (tarea.getVisibilidad()) {
            notificarAlumnosNuevaTarea(tareaGuardada, modulo);
        }

        return tareaGuardada;
    }

    /**
     * Notifica a los alumnos sobre la disponibilidad de una nueva tarea.
     */
    private void notificarAlumnosNuevaTarea(Tarea tarea, Modulo modulo) {
        if (eventPublisher != null) {
             eventPublisher.publishEvent(new ActivityCreatedEvent(
                modulo.getCurso().getIdOferta(),
                tarea.getIdActividad(),
                "TAREA",
                tarea.getLimiteEntrega(),
                tarea.getTitulo()
            ));
        }
        System.out.println("📧 [NOTIFICACIÓN] Evento de tarea publicado: '" + tarea.getTitulo() + "'");
    }
}
