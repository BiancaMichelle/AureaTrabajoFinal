package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Categoria;
import com.example.demo.repository.CategoriaRepository;

@Service
public class CategoriaService {
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    /**
     * Crea una nueva categoría verificando duplicados
     */
    public Categoria crearCategoria(String nombre, String descripcion) throws IllegalArgumentException {
        
        // Validaciones
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción de la categoría es obligatoria");
        }
        
        String nombreLimpio = nombre.trim();
        String descripcionLimpia = descripcion.trim();
        
        // Verificar duplicados por nombre
        if (categoriaRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + nombreLimpio);
        }
        
        // Verificar duplicados por descripción
        if (categoriaRepository.existsByDescripcionIgnoreCase(descripcionLimpia)) {
            throw new IllegalArgumentException("Ya existe una categoría con esa descripción");
        }
        
        // Crear y guardar
        Categoria nuevaCategoria = new Categoria(nombreLimpio, descripcionLimpia);
        return categoriaRepository.save(nuevaCategoria);
    }
}
