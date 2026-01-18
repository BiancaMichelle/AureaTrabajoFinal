package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Formacion;
import com.example.demo.repository.FormacionRepository;

@Service
@Transactional
public class FormacionService {

    @Autowired
    private FormacionRepository formacionRepository;

    /**
     * Obtiene una formación por ID
     */
    public Optional<Formacion> obtenerPorId(Long id) {
        return formacionRepository.findById(id);
    }

    /**
     * Guarda una formación
     */
    public Formacion guardar(Formacion formacion) {
        // Validar datos antes de guardar
        List<String> errores = formacion.validarDatos();
        errores.addAll(formacion.validarDatosFormacion());
        
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("Datos de la formación inválidos: " + String.join(", ", errores));
        }
        
        return formacionRepository.save(formacion);
    }

    /**
     * Modifica una formación existente
     */
    public boolean modificar(Long id, Formacion datosNuevos) {
        Optional<Formacion> formacionOpt = obtenerPorId(id);
        
        if (!formacionOpt.isPresent()) {
            return false;
        }
        
        Formacion formacionExistente = formacionOpt.get();
        
        // Verificar si puede ser editada
        if (!formacionExistente.puedeSerEditada()) {
            return false;
        }
        
        // Validar nuevos datos
        List<String> errores = datosNuevos.validarDatos();
        errores.addAll(datosNuevos.validarDatosFormacion());
        
        if (!errores.isEmpty()) {
            return false;
        }
        
        // Aplicar modificaciones
        formacionExistente.modificarDatosBasicos(
            datosNuevos.getNombre(),
            datosNuevos.getDescripcion(),
            datosNuevos.getDuracion(),
            datosNuevos.getFechaInicio(),
            datosNuevos.getFechaFin(),
            datosNuevos.getModalidad(),
            datosNuevos.getCupos(),
            datosNuevos.getVisibilidad(),
            datosNuevos.getCostoInscripcion(),
            datosNuevos.getCostoMora() != null ? datosNuevos.getCostoMora() : 0.0,
            datosNuevos.getCertificado(),
            datosNuevos.getLugar(),
            datosNuevos.getEnlace()
        );
        
        formacionExistente.modificarDatosFormacion(
            datosNuevos.getPlan(),
            datosNuevos.getDocentes(),
            datosNuevos.getCostoCuota(),
            datosNuevos.getCostoMora(),
            datosNuevos.getNrCuotas(),
            datosNuevos.getDiaVencimiento()
        );
        
        formacionRepository.save(formacionExistente);
        return true;
    }

    /**
     * Modifica una formación con los parámetros del formulario
     */
    public Formacion modificar(Long idOferta, String nombre, String descripcion, 
                              java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin, Integer cupos, 
                              Double costoCuota, Double costoMora, Integer nrCuotas, 
                              Integer diaVencimiento, String modalidad, Boolean otorgaCertificado,
                              org.springframework.web.multipart.MultipartFile imagen, String categorias, 
                              String horarios, String docentes) {
        try {
            // Buscar la formación existente
            Optional<Formacion> formacionOpt = obtenerPorId(idOferta);
            if (!formacionOpt.isPresent()) {
                throw new RuntimeException("Formación no encontrada con ID: " + idOferta);
            }
            
            Formacion formacionExistente = formacionOpt.get();
            
            // Verificar si puede ser modificada
            if (!formacionExistente.puedeSerEditada()) {
                throw new RuntimeException("La formación no puede ser modificada porque ya finalizó o tiene inscripciones activas");
            }
            
            // Modificar datos básicos
            formacionExistente.setNombre(nombre);
            formacionExistente.setDescripcion(descripcion);
            formacionExistente.setFechaInicio(fechaInicio);
            formacionExistente.setFechaFin(fechaFin);
            formacionExistente.setCupos(cupos);
            formacionExistente.setCertificado(otorgaCertificado);
            
            if (modalidad != null && !modalidad.isEmpty()) {
                formacionExistente.setModalidad(com.example.demo.enums.Modalidad.valueOf(modalidad.toUpperCase()));
            }
            
            // Modificar datos específicos de la formación
            formacionExistente.setCostoCuota(costoCuota);
            formacionExistente.setCostoMora(costoMora);
            formacionExistente.setNrCuotas(nrCuotas);
            formacionExistente.setDiaVencimiento(diaVencimiento);
                        
            // Guardar cambios
            return formacionRepository.save(formacionExistente);
            
        } catch (Exception e) {
            System.err.println("Error al modificar formación: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Cambia el estado de una formación - usa método heredado
     */
    public boolean cambiarEstado(Long id, EstadoOferta nuevoEstado) {
        Optional<Formacion> formacionOpt = obtenerPorId(id);
        
        if (!formacionOpt.isPresent()) {
            return false;
        }
        
        Formacion formacion = formacionOpt.get();
        boolean exito = formacion.cambiarEstado(nuevoEstado); // Método heredado de OfertaAcademica
        
        if (exito) {
            formacionRepository.save(formacion);
        }
        
        return exito;
    }

    /**
     * Da de baja una formación - usa método heredado
     */
    public boolean darDeBaja(Long id) {
        return cambiarEstado(id, EstadoOferta.DE_BAJA);
    }

    /**
     * Da de alta una formación - usa método heredado
     */
    public boolean darDeAlta(Long id) {
        return cambiarEstado(id, EstadoOferta.ACTIVA);
    }

    /**
     * Elimina una formación con validaciones - usa método heredado
     */
    public boolean eliminar(Long id) {
        Optional<Formacion> formacionOpt = obtenerPorId(id);
        
        if (!formacionOpt.isPresent()) {
            return false;
        }
        
        Formacion formacion = formacionOpt.get();
        
        if (!formacion.puedeSerEliminada()) { // Método heredado de OfertaAcademica
            return false;
        }
        
        formacionRepository.deleteById(id);
        return true;
    }

    /**
     * Obtiene formaciones por estado - usa repositorio directamente
     */
    public List<Formacion> obtenerPorEstado(EstadoOferta estado) {
        return formacionRepository.findByEstado(estado);
    }

    /**
     * Busca formaciones por nombre - usa repositorio directamente
     */
    public List<Formacion> buscarPorNombre(String nombre) {
        return formacionRepository.findByNombreContainingIgnoreCase(nombre);
    }
}