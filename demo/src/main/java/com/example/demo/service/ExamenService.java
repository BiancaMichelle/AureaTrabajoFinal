package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoExamen;
import com.example.demo.event.ActivityCreatedEvent;
import com.example.demo.model.Examen;
import com.example.demo.model.Modulo;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.repository.ExamenRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.PoolRepository;
import com.example.demo.repository.PreguntaRepository;

@Service
public class ExamenService {

    @Autowired
    private ExamenRepository examenRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private PreguntaRepository preguntaRepository;

    @Transactional
    public Examen crearExamen(Examen examen, UUID moduloId, List<PoolDTO> poolsDTO, List<UUID> modulosRelacionadosIds) {
        // Buscar el módulo
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

        // Validar fechas
        if (examen.getFechaApertura() != null && examen.getFechaCierre() != null && 
            examen.getFechaCierre().isBefore(examen.getFechaApertura())) {
            throw new RuntimeException("La fecha de cierre no puede ser anterior a la fecha de apertura");
        }

        // Configurar el examen
        examen.setModulo(modulo);
        examen.setFechaCreacion(LocalDateTime.now());
        examen.setEstado(EstadoExamen.PENDIENTE);

        // Guardar el examen primero
        Examen examenGuardado = examenRepository.save(examen);

        // Asociar módulos relacionados para contexto IA
        List<Modulo> modulosRelacionados = new ArrayList<>();
        if (modulosRelacionadosIds != null && !modulosRelacionadosIds.isEmpty()) {
            modulosRelacionados = moduloRepository.findAllById(modulosRelacionadosIds);
        }
        if (!modulosRelacionados.contains(modulo)) {
            modulosRelacionados.add(modulo);
        }
        examenGuardado.setModulosRelacionados(modulosRelacionados);
        examenRepository.save(examenGuardado);

        // Procesar los pools
        if (poolsDTO != null && !poolsDTO.isEmpty()) {
            List<Pool> pools = new ArrayList<>();
            java.util.Set<String> poolsUsados = new java.util.HashSet<>();

            for (PoolDTO poolDTO : poolsDTO) {
                Pool poolGuardado;
                
                // Validar duplicados si es reutilización
                if (poolDTO.getIdReal() != null && !poolDTO.getIdReal().isEmpty()) {
                    if (!poolsUsados.add(poolDTO.getIdReal())) {
                        throw new RuntimeException("No se puede agregar el mismo pool más de una vez.");
                    }
                
                    poolGuardado = poolRepository.findById(UUID.fromString(poolDTO.getIdReal()))
                            .orElseThrow(
                                    () -> new RuntimeException("Pool existente no encontrado: " + poolDTO.getIdReal()));
                                    
                    // Validar que el pool existente tenga preguntas
                    if (poolGuardado.getPreguntas() == null || poolGuardado.getPreguntas().isEmpty()) {
                        throw new RuntimeException("El pool '" + poolGuardado.getNombre() + "' no tiene preguntas. Agrega preguntas antes de crear el examen.");
                    }
                    
                    // Validar estado IA
                    if (poolGuardado.getIaStatus() == com.example.demo.enums.IaGenerationStatus.PENDING) {
                         throw new RuntimeException("El pool '" + poolGuardado.getNombre() + "' aún se está generando con IA. Espera a que termine."); 
                    }
                    
                } else {
                    // Validaciones para pool nuevo
                    if (poolDTO.getPreguntas() == null || poolDTO.getPreguntas().isEmpty()) {
                         throw new RuntimeException("El nuevo pool '" + poolDTO.getNombre() + "' no tiene preguntas.");
                    }
                
                    // Crear un nuevo pool y asociarlo a la oferta del módulo
                    Pool pool = new Pool();
                    pool.setIdPool(UUID.randomUUID());
                    pool.setNombre(poolDTO.getNombre());
                    pool.setDescripcion(poolDTO.getDescripcion());
                    pool.setCantidadPreguntas(poolDTO.getCantidadPreguntas());
                    pool.setOferta(modulo.getCurso());

                    // Guardar el pool
                    poolGuardado = poolRepository.save(pool);

                    // Procesar las preguntas del pool (si vinieron inline)
                    if (poolDTO.getPreguntas() != null && !poolDTO.getPreguntas().isEmpty()) {
                        List<Pregunta> preguntas = new ArrayList<>();
                        for (PreguntaDTO preguntaDTO : poolDTO.getPreguntas()) {
                            Pregunta pregunta = new Pregunta();
                            pregunta.setIdPregunta(UUID.randomUUID());
                            pregunta.setEnunciado(preguntaDTO.getEnunciado());
                            pregunta.setTipoPregunta(preguntaDTO.getTipoPregunta());
                            pregunta.setPuntaje(preguntaDTO.getPuntaje());
                            pregunta.setPool(poolGuardado);
                            preguntas.add(preguntaRepository.save(pregunta));
                        }
                        poolGuardado.setPreguntas(preguntas);
                    }
                }
                pools.add(poolGuardado);
            }

            // Asignar los pools al examen (lado owner de la relación ManyToMany)
            examenGuardado.setPoolPreguntas(pools);
            examenRepository.save(examenGuardado);
        }

        eventPublisher.publishEvent(new ActivityCreatedEvent(
                modulo.getCurso().getIdOferta(),
                examenGuardado.getIdActividad(),
                "EXAMEN",
                examenGuardado.getFechaCierre(),
                examenGuardado.getTitulo()
        ));

        return examenGuardado;
    }

    // DTOs internos para transferencia de datos
    public static class PoolDTO {
        private String idReal; // UUID en string
        private Boolean esExistente;
        private String nombre;
        private String descripcion;
        private Integer cantidadPreguntas;
        private List<PreguntaDTO> preguntas;

        // Getters y Setters
        public String getIdReal() {
            return idReal;
        }

        public void setIdReal(String idReal) {
            this.idReal = idReal;
        }

        public Boolean getEsExistente() {
            return esExistente;
        }

        public void setEsExistente(Boolean esExistente) {
            this.esExistente = esExistente;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public Integer getCantidadPreguntas() {
            return cantidadPreguntas;
        }

        public void setCantidadPreguntas(Integer cantidadPreguntas) {
            this.cantidadPreguntas = cantidadPreguntas;
        }

        public List<PreguntaDTO> getPreguntas() {
            return preguntas;
        }

        public void setPreguntas(List<PreguntaDTO> preguntas) {
            this.preguntas = preguntas;
        }
    }

    public static class PreguntaDTO {
        private String enunciado;
        private com.example.demo.enums.TipoPregunta tipoPregunta;
        private Float puntaje;

        // Getters y Setters
        public String getEnunciado() {
            return enunciado;
        }

        public void setEnunciado(String enunciado) {
            this.enunciado = enunciado;
        }

        public com.example.demo.enums.TipoPregunta getTipoPregunta() {
            return tipoPregunta;
        }

        public void setTipoPregunta(com.example.demo.enums.TipoPregunta tipoPregunta) {
            this.tipoPregunta = tipoPregunta;
        }

        public Float getPuntaje() {
            return puntaje;
        }

        public void setPuntaje(Float puntaje) {
            this.puntaje = puntaje;
        }
    }

    @Transactional
    public void eliminarActividad(Long actividadId) {
        Examen examen = examenRepository.findById(actividadId)
                .orElseThrow(() -> new RuntimeException("Actividad no encontrada"));
        examenRepository.delete(examen);
    }
}
