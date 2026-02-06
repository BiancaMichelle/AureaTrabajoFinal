package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Seminario;
import com.example.demo.repository.SeminarioRepository;

@Service
@Transactional
public class SeminarioService {

    @Autowired
    private SeminarioRepository seminarioRepository;

    /**
     * Obtiene un seminario por ID
     */
    public Optional<Seminario> obtenerPorId(Long id) {
        return seminarioRepository.findById(id);
    }

    /**
     * Guarda un seminario
     */
    public Seminario guardar(Seminario seminario) {
        // Validar datos antes de guardar
        List<String> errores = seminario.validarDatos();
        errores.addAll(seminario.validarDatosSeminario());
        
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("Datos del seminario inválidos: " + String.join(", ", errores));
        }
        
        return seminarioRepository.save(seminario);
    }

    /**
     * Modifica un seminario existente
     */
    public boolean modificar(Long id, Seminario datosNuevos) {
        Optional<Seminario> seminarioOpt = obtenerPorId(id);
        
        if (!seminarioOpt.isPresent()) {
            return false;
        }
        
        Seminario seminarioExistente = seminarioOpt.get();
        
        // Verificar si puede ser editado
        if (!seminarioExistente.puedeSerEditada()) {
            return false;
        }
        
        // Validar nuevos datos
        List<String> errores = datosNuevos.validarDatos();
        errores.addAll(datosNuevos.validarDatosSeminario());
        
        if (!errores.isEmpty()) {
            return false;
        }
        
        // Aplicar modificaciones básicas (heredadas)
        seminarioExistente.modificarDatosBasicos(
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
        
        // Aplicar modificaciones específicas de seminario
        seminarioExistente.modificarDatosSeminario(
            datosNuevos.getPublicoObjetivo(),
            datosNuevos.getDuracionMinutos(),
            datosNuevos.getDisertantes()
        );
        
        seminarioRepository.save(seminarioExistente);
        return true;
    }

    /**
     * Modifica un seminario con los parámetros del formulario
     */
    public Seminario modificar(Long idOferta, String nombre, String descripcion, 
                              java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin, Integer cupos, 
                              Double costo, String modalidad, Boolean otorgaCertificado,
                              org.springframework.web.multipart.MultipartFile imagen, String categorias,
                              String disertantes) {
        try {
            // Buscar el seminario existente
            Optional<Seminario> seminarioOpt = obtenerPorId(idOferta);
            if (!seminarioOpt.isPresent()) {
                throw new RuntimeException("Seminario no encontrado con ID: " + idOferta);
            }
            
            Seminario seminarioExistente = seminarioOpt.get();
            
            // Verificar si puede ser modificado
            if (!seminarioExistente.puedeSerEditada()) {
                throw new RuntimeException("El seminario no puede ser modificado porque ya finalizó o tiene inscripciones activas");
            }
            
            // Modificar datos básicos
            seminarioExistente.setNombre(nombre);
            seminarioExistente.setDescripcion(descripcion);
            seminarioExistente.setFechaInicio(fechaInicio);
            seminarioExistente.setFechaFin(fechaFin);
            seminarioExistente.setCupos(cupos);
            seminarioExistente.setCostoInscripcion(costo);
            
            if (modalidad != null && !modalidad.isEmpty()) {
                seminarioExistente.setModalidad(com.example.demo.enums.Modalidad.valueOf(modalidad.toUpperCase()));
            }
                        
            // Guardar cambios
            return seminarioRepository.save(seminarioExistente);
            
        } catch (Exception e) {
            System.err.println("Error al modificar seminario: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Cambia el estado de un seminario - usa método heredado
     */
    public boolean cambiarEstado(Long id, EstadoOferta nuevoEstado) {
        Optional<Seminario> seminarioOpt = obtenerPorId(id);
        
        if (!seminarioOpt.isPresent()) {
            return false;
        }
        
        Seminario seminario = seminarioOpt.get();
        boolean exito = seminario.cambiarEstado(nuevoEstado); // Método heredado
        
        if (exito) {
            seminarioRepository.save(seminario);
        }
        
        return exito;
    }

    /**
     * Da de baja un seminario - usa método heredado
     */
    public boolean darDeBaja(Long id) {
        return cambiarEstado(id, EstadoOferta.DE_BAJA);
    }

    /**
     * Da de alta un seminario - usa método heredado
     */
    public boolean darDeAlta(Long id) {
        return cambiarEstado(id, EstadoOferta.ACTIVA);
    }

    /**
     * Elimina un seminario con validaciones - usa método heredado
     */
    public boolean eliminar(Long id) {
        Optional<Seminario> seminarioOpt = obtenerPorId(id);
        
        if (!seminarioOpt.isPresent()) {
            return false;
        }
        
        Seminario seminario = seminarioOpt.get();
        
        if (!seminario.puedeSerEliminada()) { // Método heredado
            return false;
        }
        
        seminarioRepository.deleteById(id);
        return true;
    }

    /**
     * Obtiene seminarios por estado - usa repositorio directamente
     */
    public List<Seminario> obtenerPorEstado(EstadoOferta estado) {
        return seminarioRepository.findByEstado(estado);
    }

    /**
     * Busca seminarios por nombre - usa repositorio directamente
     */
    public List<Seminario> buscarPorNombre(String nombre) {
        return seminarioRepository.findByNombreContainingIgnoreCase(nombre);
    }

    /**
     * Añade un disertante a un seminario
     */
    public boolean agregarDisertante(Long seminarioId, String disertante) {
        Optional<Seminario> seminarioOpt = obtenerPorId(seminarioId);
        
        if (!seminarioOpt.isPresent()) {
            return false;
        }
        
        Seminario seminario = seminarioOpt.get();
        
        if (!seminario.puedeSerEditada()) {
            return false;
        }
        
        boolean agregado = seminario.agregarDisertante(disertante);
        
        if (agregado) {
            seminarioRepository.save(seminario);
        }
        
        return agregado;
    }

    /**
     * Remueve un disertante de un seminario
     */
    public boolean removerDisertante(Long seminarioId, String disertante) {
        Optional<Seminario> seminarioOpt = obtenerPorId(seminarioId);
        
        if (!seminarioOpt.isPresent()) {
            return false;
        }
        
        Seminario seminario = seminarioOpt.get();
        
        if (!seminario.puedeSerEditada()) {
            return false;
        }
        
        boolean removido = seminario.removerDisertante(disertante);
        
        if (removido) {
            seminarioRepository.save(seminario);
        }
        
        return removido;
    }
}