package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Categoria;
import com.example.demo.repository.CategoriaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    /**
     * Obtiene todas las categorías
     */
    public List<Categoria> obtenerTodasLasCategorias() {
        return categoriaRepository.findAll();
    }
    
    /**
     * Obtiene una categoría por ID
     */
    public Optional<Categoria> obtenerCategoriaPorId(Long id) {
        return categoriaRepository.findById(id);
    }
    
    /**
     * Crea una nueva categoría verificando duplicados
     */
    public Categoria crearCategoria(String nombre, String descripcion) throws IllegalArgumentException {
        
        // Crear nueva categoría
        Categoria nuevaCategoria = new Categoria(nombre, descripcion);
        
        // Verificar si es válida usando el método del modelo
        if (!nuevaCategoria.esValida()) {
            throw new IllegalArgumentException("Los datos de la categoría no son válidos");
        }
        
        String nombreLimpio = nombre.trim();
        String descripcionLimpia = descripcion != null ? descripcion.trim() : "";
        
        // Verificar duplicados por nombre
        if (categoriaRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + nombreLimpio);
        }
        
        // Verificar duplicados por descripción (solo si la descripción no está vacía)
        if (!descripcionLimpia.isEmpty() && categoriaRepository.existsByDescripcionIgnoreCase(descripcionLimpia)) {
            throw new IllegalArgumentException("Ya existe una categoría con esa descripción");
        }
        
        // Actualizar con datos limpios
        nuevaCategoria.modificarar(nombreLimpio, descripcionLimpia);
        
        // Guardar en base de datos
        return categoriaRepository.save(nuevaCategoria);
    }
    
    /**
     * Actualiza una categoría existente
     */
    public Categoria actualizarCategoria(Long id, String nuevoNombre, String nuevaDescripcion) throws IllegalArgumentException {
        
        // Buscar la categoría existente
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(id);
        if (!categoriaOpt.isPresent()) {
            throw new IllegalArgumentException("No se encontró la categoría con ID: " + id);
        }
        
        Categoria categoria = categoriaOpt.get();
        
        // Validar nuevos datos
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        
        String nombreLimpio = nuevoNombre.trim();
        String descripcionLimpia = nuevaDescripcion != null ? nuevaDescripcion.trim() : "";
        
        // Verificar duplicados por nombre (excluyendo la categoría actual)
        Optional<Categoria> categoriaConMismoNombre = categoriaRepository.findByNombreIgnoreCase(nombreLimpio);
        if (categoriaConMismoNombre.isPresent() && !categoriaConMismoNombre.get().getIdCategoria().equals(id)) {
            throw new IllegalArgumentException("Ya existe otra categoría con el nombre: " + nombreLimpio);
        }
        
        // Verificar duplicados por descripción (excluyendo la categoría actual)
        if (!descripcionLimpia.isEmpty()) {
            List<Categoria> categoriasConMismaDescripcion = categoriaRepository.findAll().stream()
                .filter(c -> c.getDescripcion() != null && 
                            c.getDescripcion().equalsIgnoreCase(descripcionLimpia) &&
                            !c.getIdCategoria().equals(id))
                .toList();
            
            if (!categoriasConMismaDescripcion.isEmpty()) {
                throw new IllegalArgumentException("Ya existe otra categoría con esa descripción");
            }
        }
        
        // Actualizar usando el método del modelo
        categoria.modificarar(nombreLimpio, descripcionLimpia);
        
        // Verificar que sigue siendo válida
        if (!categoria.esValida()) {
            throw new IllegalArgumentException("Los datos actualizados no son válidos");
        }
        
        // Guardar cambios
        return categoriaRepository.save(categoria);
    }
    
    /**
     * Elimina una categoría por ID
     */
    public void eliminarCategoria(Long id) throws IllegalArgumentException {
        Optional<Categoria> categoriaOpt = categoriaRepository.findById(id);
        if (!categoriaOpt.isPresent()) {
            throw new IllegalArgumentException("No se encontró la categoría con ID: " + id);
        }
        
        categoriaRepository.deleteById(id);
    }
    
    /**
     * Busca categorías por nombre
     */
    public List<Categoria> buscarCategoriasPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return obtenerTodasLasCategorias();
        }
        return categoriaRepository.findByNombreContainingIgnoreCase(nombre.trim());
    }
    
    /**
     * Verifica si existe una categoría con el nombre dado
     */
    public boolean existeCategoriaPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        return categoriaRepository.existsByNombreIgnoreCase(nombre.trim());
    }
}
