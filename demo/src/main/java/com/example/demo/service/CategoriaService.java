package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Categoria;
import com.example.demo.repository.CategoriaRepository;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<Categoria> findAll() {
        return categoriaRepository.findAll();
    }

    public Optional<Categoria> findById(Long id) {
        return categoriaRepository.findById(id);
    }

    public Categoria save(Categoria categoria) {
        // Validaciones básicas
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría no puede ser nula");
        }
        if (categoria.getNombre() == null || categoria.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es requerido");
        }
        
        // Limpiar nombre y descripciÃ³n
        String nombreLimpio = categoria.getNombre().trim();
        categoria.setNombre(nombreLimpio);
        
        String descripcionLimpia = categoria.getDescripcion();
        if (descripcionLimpia != null) {
            descripcionLimpia = descripcionLimpia.trim();
            if (descripcionLimpia.isEmpty()) {
                descripcionLimpia = null;
            }
            categoria.setDescripcion(descripcionLimpia);
        }
        
        Long idActual = categoria.getIdCategoria();
        
        // Validar nombre Ãºnico (case-insensitive)
        categoriaRepository.findByNombreIgnoreCase(nombreLimpio).ifPresent(existing -> {
            if (idActual == null || !existing.getIdCategoria().equals(idActual)) {
                throw new IllegalArgumentException("Ya existe una categorÃ­a con ese nombre");
            }
        });
        
        // Validar descripciÃ³n Ãºnica (case-insensitive) si se proporcionÃ³
        if (descripcionLimpia != null) {
            categoriaRepository.findByDescripcionIgnoreCase(descripcionLimpia).ifPresent(existing -> {
                if (idActual == null || !existing.getIdCategoria().equals(idActual)) {
                    throw new IllegalArgumentException("Ya existe una categorÃ­a con esa descripciÃ³n");
                }
            });
        }
        
        return categoriaRepository.save(categoria);
    }

    public void deleteById(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada con ID: " + id);
        }
        
        // Verificar si la categoría está siendo usada en alguna oferta
        // Esto depende de tu implementación específica
        // Optional<Long> count = categoriaRepository.countOfertasByCategoriaId(id);
        // if (count.isPresent() && count.get() > 0) {
        //     throw new RuntimeException("No se puede eliminar la categoría porque está siendo utilizada en ofertas");
        // }
        
        categoriaRepository.deleteById(id);
    }

    public List<Categoria> findByNombreContainingIgnoreCase(String nombre) {
        return categoriaRepository.findByNombreContainingIgnoreCase(nombre);
    }

    public boolean existsByNombre(String nombre) {
        return categoriaRepository.existsByNombreIgnoreCase(nombre);
    }
}

