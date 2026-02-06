package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Charla;
import com.example.demo.repository.CharlaRepository;

@Service
@Transactional
public class CharlaService {

    @Autowired
    private CharlaRepository charlaRepository;

    /**
     * Obtiene una charla por ID
     */
    public Optional<Charla> obtenerPorId(Long id) {
        return charlaRepository.findById(id);
    }

    /**
     * Guarda una charla
     */
    public Charla guardar(Charla charla) {
        // Validar datos antes de guardar
        List<String> errores = charla.validarDatos();
        errores.addAll(charla.validarDatosCharla());
        
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("Datos de la charla inválidos: " + String.join(", ", errores));
        }
        
        return charlaRepository.save(charla);
    }

    /**
     * Modifica una charla existente
     */
    public boolean modificar(Long id, Charla datosNuevos) {
        Optional<Charla> charlaOpt = obtenerPorId(id);
        
        if (!charlaOpt.isPresent()) {
            return false;
        }
        
        Charla charlaExistente = charlaOpt.get();
        
        // Verificar si puede ser editada
        if (!charlaExistente.puedeSerEditada()) {
            return false;
        }
        
        // Validar nuevos datos
        List<String> errores = datosNuevos.validarDatos();
        errores.addAll(datosNuevos.validarDatosCharla());
        
        if (!errores.isEmpty()) {
            return false;
        }
        
        // Aplicar modificaciones básicas (heredadas)
        charlaExistente.modificarDatosBasicos(
            datosNuevos.getNombre(),
            datosNuevos.getDescripcion(),
            datosNuevos.getDuracion(),
            datosNuevos.getFechaInicio(),
            datosNuevos.getFechaFin(),
            datosNuevos.getModalidad(),
            datosNuevos.getCupos(),
            datosNuevos.getVisibilidad(),
            datosNuevos.getCostoInscripcion(),
            0.0,
            datosNuevos.getCertificado(),
            datosNuevos.getLugar(),
            datosNuevos.getEnlace(),
            datosNuevos.getFechaInicioInscripcion(),
            datosNuevos.getFechaFinInscripcion()
        );
        
        // Aplicar modificaciones específicas de charla
        charlaExistente.modificarDatosCharla(
            datosNuevos.getDuracionEstimada(),
            datosNuevos.getDisertantes(),
            datosNuevos.getPublicoObjetivo()
        );
        
        charlaRepository.save(charlaExistente);
        return true;
    }

    /**
     * Modifica una charla con los parámetros del formulario
     */
    public Charla modificar(Long idOferta, String nombre, String descripcion, 
                           java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin, Integer cupos, 
                           Double costo, String modalidad, Boolean otorgaCertificado,
                           org.springframework.web.multipart.MultipartFile imagen, String categorias,
                           String disertantes) {
        try {
            // Buscar la charla existente
            Optional<Charla> charlaOpt = obtenerPorId(idOferta);
            if (!charlaOpt.isPresent()) {
                throw new RuntimeException("Charla no encontrada con ID: " + idOferta);
            }
            
            Charla charlaExistente = charlaOpt.get();
            
            // Verificar si puede ser modificada
            if (!charlaExistente.puedeSerEditada()) {
                throw new RuntimeException("La charla no puede ser modificada porque ya finalizó o tiene inscripciones activas");
            }
            
            // Modificar datos básicos
            charlaExistente.setNombre(nombre);
            charlaExistente.setDescripcion(descripcion);
            charlaExistente.setFechaInicio(fechaInicio);
            charlaExistente.setFechaFin(fechaFin);
            charlaExistente.setCupos(cupos);
            charlaExistente.setCostoInscripcion(costo); // Usar el método heredado de OfertaAcademica
            
            if (modalidad != null && !modalidad.isEmpty()) {
                charlaExistente.setModalidad(com.example.demo.enums.Modalidad.valueOf(modalidad.toUpperCase()));
            }
            
            // TODO: Manejar imagen, categorías y disertantes según sea necesario
            
            // Guardar cambios
            return charlaRepository.save(charlaExistente);
            
        } catch (Exception e) {
            System.err.println("Error al modificar charla: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Cambia el estado de una charla - usa método heredado
     */
    public boolean cambiarEstado(Long id, EstadoOferta nuevoEstado) {
        Optional<Charla> charlaOpt = obtenerPorId(id);
        
        if (!charlaOpt.isPresent()) {
            return false;
        }
        
        Charla charla = charlaOpt.get();
        boolean exito = charla.cambiarEstado(nuevoEstado); // Método heredado de OfertaAcademica
        
        if (exito) {
            charlaRepository.save(charla);
        }
        
        return exito;
    }

    /**
     * Da de baja una charla - usa método heredado
     */
    public boolean darDeBaja(Long id) {
        return cambiarEstado(id, EstadoOferta.DE_BAJA);
    }

    /**
     * Da de alta una charla - usa método heredado
     */
    public boolean darDeAlta(Long id) {
        return cambiarEstado(id, EstadoOferta.ACTIVA);
    }

    /**
     * Elimina una charla con validaciones - usa método heredado
     */
    public boolean eliminar(Long id) {
        Optional<Charla> charlaOpt = obtenerPorId(id);
        
        if (!charlaOpt.isPresent()) {
            return false;
        }
        
        Charla charla = charlaOpt.get();
        
        if (!charla.puedeSerEliminada()) { // Método heredado de OfertaAcademica
            return false;
        }
        
        charlaRepository.deleteById(id);
        return true;
    }

    /**
     * Obtiene charlas por estado - usa repositorio directamente
     */
    public List<Charla> obtenerPorEstado(EstadoOferta estado) {
        return charlaRepository.findByEstado(estado);
    }

    /**
     * Busca charlas por nombre - usa repositorio directamente
     */
    public List<Charla> buscarPorNombre(String nombre) {
        return charlaRepository.findByNombreContainingIgnoreCase(nombre);
    }

    /**
     * Añade un disertante a una charla
     */
    public boolean agregarDisertante(Long charlaId, String disertante) {
        Optional<Charla> charlaOpt = obtenerPorId(charlaId);
        
        if (!charlaOpt.isPresent()) {
            return false;
        }
        
        Charla charla = charlaOpt.get();
        
        if (!charla.puedeSerEditada()) {
            return false;
        }
        
        boolean agregado = charla.agregarDisertante(disertante);
        
        if (agregado) {
            charlaRepository.save(charla);
        }
        
        return agregado;
    }

    /**
     * Remueve un disertante de una charla
     */
    public boolean removerDisertante(Long charlaId, String disertante) {
        Optional<Charla> charlaOpt = obtenerPorId(charlaId);
        
        if (!charlaOpt.isPresent()) {
            return false;
        }
        
        Charla charla = charlaOpt.get();
        
        if (!charla.puedeSerEditada()) {
            return false;
        }
        
        boolean removido = charla.removerDisertante(disertante);
        
        if (removido) {
            charlaRepository.save(charla);
        }
        
        return removido;
    }
}