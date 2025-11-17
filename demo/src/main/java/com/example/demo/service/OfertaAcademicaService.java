package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Seminario;
import com.example.demo.repository.CharlaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.FormacionRepository;
import com.example.demo.repository.SeminarioRepository;

@Service
public class OfertaAcademicaService {
    @Autowired
    private CursoRepository cursoRepository;
    
    @Autowired
    private FormacionRepository formacionRepository;
    
    @Autowired
    private CharlaRepository charlaRepository;
    
    @Autowired
    private SeminarioRepository seminarioRepository;

    public List<OfertaAcademica> obtenerTodas() {
        List<OfertaAcademica> todasLasOfertas = new ArrayList<>();
        todasLasOfertas.addAll(cursoRepository.findAll());
        todasLasOfertas.addAll(formacionRepository.findAll());
        todasLasOfertas.addAll(charlaRepository.findAll());
        todasLasOfertas.addAll(seminarioRepository.findAll());
        return todasLasOfertas;
    }
    public OfertaAcademica guardar(OfertaAcademica oferta) {
        if (oferta instanceof Curso) {
            return cursoRepository.save((Curso) oferta);
        } else if (oferta instanceof Formacion) {
            return formacionRepository.save((Formacion) oferta);
        } else if (oferta instanceof Charla) {
            return charlaRepository.save((Charla) oferta);
        } else if (oferta instanceof Seminario) {
            return seminarioRepository.save((Seminario) oferta);
        }
        throw new IllegalArgumentException("Tipo de oferta no soportado");
    }
    public void eliminar(Long id, String tipo) {
        switch (tipo.toUpperCase()) {
            case "CURSO":
                Long cursoId = Long.valueOf(id);
                cursoRepository.deleteById(cursoId);
                break;
            case "FORMACION":
                formacionRepository.deleteById(id);
                break;
            case "CHARLA":
                charlaRepository.deleteById(id);
                break;
            case "SEMINARIO":
                seminarioRepository.deleteById(id);
                break;
        }
    }

    /**
     * Obtiene una oferta específica por ID y tipo
     */
    public Optional<OfertaAcademica> obtenerPorId(Long id, String tipo) {
        switch (tipo.toUpperCase()) {
            case "CURSO":
                return cursoRepository.findById(id).map(OfertaAcademica.class::cast);
            case "FORMACION":
                return formacionRepository.findById(id).map(OfertaAcademica.class::cast);
            case "CHARLA":
                return charlaRepository.findById(id).map(OfertaAcademica.class::cast);
            case "SEMINARIO":
                return seminarioRepository.findById(id).map(OfertaAcademica.class::cast);
            default:
                return Optional.empty();
        }
    }

    /**
     * Obtiene una oferta por ID, detectando automáticamente el tipo
     */
    public Optional<OfertaAcademica> obtenerPorId(Long id) {
        // Buscar en todos los repositorios
        Optional<OfertaAcademica> curso = cursoRepository.findById(id).map(OfertaAcademica.class::cast);
        if (curso.isPresent()) return curso;

        Optional<OfertaAcademica> formacion = formacionRepository.findById(id).map(OfertaAcademica.class::cast);
        if (formacion.isPresent()) return formacion;

        Optional<OfertaAcademica> charla = charlaRepository.findById(id).map(OfertaAcademica.class::cast);
        if (charla.isPresent()) return charla;

        return seminarioRepository.findById(id).map(OfertaAcademica.class::cast);
    }

    /**
     * Cambia el estado de una oferta académica
     */
    @Transactional
    public boolean cambiarEstado(Long id, EstadoOferta nuevoEstado) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id);
        
        if (!ofertaOpt.isPresent()) {
            return false;
        }
        
        OfertaAcademica oferta = ofertaOpt.get();
        
        // Usar el método del modelo para validar y cambiar estado
        boolean exito = oferta.cambiarEstado(nuevoEstado);
        
        if (exito) {
            guardar(oferta);
        }
        
        return exito;
    }

    /**
     * Da de baja una oferta académica
     */
    @Transactional
    public boolean darDeBaja(Long id) {
        return cambiarEstado(id, EstadoOferta.INACTIVA);
    }

    /**
     * Da de alta una oferta académica
     */
    @Transactional
    public boolean darDeAlta(Long id) {
        return cambiarEstado(id, EstadoOferta.ACTIVA);
    }

    /**
     * Verifica si una oferta puede ser eliminada
     */
    public boolean puedeEliminar(Long id) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id);
        
        if (!ofertaOpt.isPresent()) {
            return false;
        }
        
        return ofertaOpt.get().puedeSerEliminada();
    }

    /**
     * Elimina una oferta con validaciones
     */
    @Transactional
    public boolean eliminarConValidacion(Long id) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id);
        
        if (!ofertaOpt.isPresent()) {
            return false;
        }
        
        OfertaAcademica oferta = ofertaOpt.get();
        
        if (!oferta.puedeSerEliminada()) {
            return false;
        }
        
        String tipo = oferta.getTipoOferta();
        eliminar(id, tipo);
        return true;
    }

    /**
     * Modifica una oferta académica
     */
    @Transactional
    public boolean modificar(Long id, OfertaAcademica datosNuevos) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id);
        
        if (!ofertaOpt.isPresent()) {
            return false;
        }
        
        OfertaAcademica ofertaExistente = ofertaOpt.get();
        
        // Verificar si puede ser editada
        if (!ofertaExistente.puedeSerEditada()) {
            return false;
        }
        
        // Validar datos nuevos
        List<String> errores = datosNuevos.validarDatos();
        if (!errores.isEmpty()) {
            return false;
        }
        
        // Aplicar modificaciones básicas
        ofertaExistente.modificarDatosBasicos(
            datosNuevos.getNombre(),
            datosNuevos.getDescripcion(),
            datosNuevos.getDuracion(),
            datosNuevos.getFechaInicio(),
            datosNuevos.getFechaFin(),
            datosNuevos.getModalidad(),
            datosNuevos.getCupos(),
            datosNuevos.getVisibilidad(),
            datosNuevos.getCostoInscripcion(),
            datosNuevos.getCertificado()
        );
        
        // Aplicar modificaciones específicas según el tipo
        if (ofertaExistente instanceof Curso && datosNuevos instanceof Curso) {
            Curso cursoExistente = (Curso) ofertaExistente;
            Curso cursoNuevo = (Curso) datosNuevos;
            
            cursoExistente.modificarDatosCurso(
                cursoNuevo.getTemario(),
                cursoNuevo.getDocentes(),
                cursoNuevo.getCostoCuota(),
                cursoNuevo.getCostoMora(),
                cursoNuevo.getNrCuotas(),
                cursoNuevo.getDiaVencimiento()
            );
        } else if (ofertaExistente instanceof Formacion && datosNuevos instanceof Formacion) {
            Formacion formacionExistente = (Formacion) ofertaExistente;
            Formacion formacionNueva = (Formacion) datosNuevos;
            
            formacionExistente.modificarDatosFormacion(
                formacionNueva.getPlan(),
                formacionNueva.getDocentes(),
                formacionNueva.getCostoCuota(),
                formacionNueva.getCostoMora(),
                formacionNueva.getNrCuotas(),
                formacionNueva.getDiaVencimiento()
            );
        } else if (ofertaExistente instanceof Charla && datosNuevos instanceof Charla) {
            Charla charlaExistente = (Charla) ofertaExistente;
            Charla charlaNueva = (Charla) datosNuevos;
            
            charlaExistente.modificarDatosCharla(
                charlaNueva.getLugar(),
                charlaNueva.getEnlace(),
                charlaNueva.getDuracionEstimada(),
                charlaNueva.getDisertantes(),
                charlaNueva.getPublicoObjetivo()
            );
        } else if (ofertaExistente instanceof Seminario && datosNuevos instanceof Seminario) {
            Seminario seminarioExistente = (Seminario) ofertaExistente;
            Seminario seminarioNuevo = (Seminario) datosNuevos;
            
            seminarioExistente.modificarDatosSeminario(
                seminarioNuevo.getLugar(),
                seminarioNuevo.getEnlace(),
                seminarioNuevo.getPublicoObjetivo(),
                seminarioNuevo.getDuracionMinutos(),
                seminarioNuevo.getDisertantes()
            );
        }
        
        // Guardar cambios
        guardar(ofertaExistente);
        return true;
    }

    /**
     * Obtiene ofertas por estado
     */
    public List<OfertaAcademica> obtenerPorEstado(EstadoOferta estado) {
        List<OfertaAcademica> ofertas = new ArrayList<>();
        ofertas.addAll(cursoRepository.findByEstado(estado));
        ofertas.addAll(formacionRepository.findByEstado(estado));
        ofertas.addAll(charlaRepository.findByEstado(estado));
        ofertas.addAll(seminarioRepository.findByEstado(estado));
        return ofertas;
    }

    /**
     * Busca ofertas por nombre
     */
    public List<OfertaAcademica> buscarPorNombre(String nombre) {
        List<OfertaAcademica> ofertas = new ArrayList<>();
        ofertas.addAll(cursoRepository.findByNombreContainingIgnoreCase(nombre));
        ofertas.addAll(formacionRepository.findByNombreContainingIgnoreCase(nombre));
        ofertas.addAll(charlaRepository.findByNombreContainingIgnoreCase(nombre));
        ofertas.addAll(seminarioRepository.findByNombreContainingIgnoreCase(nombre));
        return ofertas;
    }
}
