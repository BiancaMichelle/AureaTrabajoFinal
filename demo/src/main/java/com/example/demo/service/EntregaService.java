package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Entrega;
import com.example.demo.model.Tarea;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EntregaRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class EntregaService {

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final String UPLOAD_DIR = "uploads/tareas/";

    @Transactional
    public void eliminarEntrega(Long idEntrega, String username) {
        // Buscar usuario
        Usuario estudiante = usuarioRepository.findByDni(username)
                .or(() -> usuarioRepository.findByCorreo(username))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Buscar entrega
        Entrega entrega = entregaRepository.findById(idEntrega)
                .orElseThrow(() -> new RuntimeException("Entrega no encontrada"));

        // Validar que la entrega pertenece al usuario
        if (!entrega.getEstudiante().getId().equals(estudiante.getId())) {
            throw new RuntimeException("No tienes permiso para eliminar esta entrega.");
        }

        Tarea tarea = entrega.getTarea();

        // Validar si la tarea permite modificaciones (que implica permitir eliminar para re-entregar)
        // O si simplemente está dentro del plazo.
        // Asumimos que si se puede modificar, se puede eliminar.
        if (!Boolean.TRUE.equals(tarea.getModificaciones())) {
             throw new RuntimeException("No se permiten modificaciones (ni eliminaciones) en esta tarea.");
        }

        // Validar plazo
        LocalDateTime ahora = LocalDateTime.now();
        boolean fueraDePlazo = tarea.getLimiteEntrega() != null && ahora.isAfter(tarea.getLimiteEntrega());
        
        // Si está fuera de plazo y NO se permiten entregas tardías, no se puede eliminar.
        // Si se permiten tardías, quizás se permite eliminar para subir una nueva (tardía).
        if (fueraDePlazo && !Boolean.TRUE.equals(tarea.getEntregasTardias())) {
            throw new RuntimeException("El plazo ha finalizado y no se permiten cambios.");
        }

        // Eliminar archivo físico
        if (entrega.getNombreArchivo() != null && entrega.getContenido() != null) {
            try {
                // La ruta del archivo se guarda en el campo contenido, a veces concatenada con texto
                String contenido = entrega.getContenido();
                
                // Lógica similar a EntregaController para extraer la ruta
                if (contenido.contains("uploads/tareas/")) {
                    String[] partes = contenido.split("\\|");
                    for (String parte : partes) {
                        if (parte.contains("uploads/tareas/")) {
                            // Limpiar prefijos si existen y obtener la ruta limpia
                            String rutaArchivo = parte.trim();
                            if (rutaArchivo.startsWith("Archivo: ")) {
                                rutaArchivo = rutaArchivo.replace("Archivo: ", "");
                            }
                            
                            Path path = Paths.get(rutaArchivo);
                            Files.deleteIfExists(path);
                        }
                    }
                } else {
                    // Intento fallback si solo está el nombre del archivo guardado en el sistema
                    // Pero como el nombre físico tiene timestamp, no podemos adivinarlo solo con getNombreArchivo()
                    // Si no está en contenido, probablemente no podamos borrarlo a menos que usemos archivoNombreGuardado si estuviera poblado
                    if (entrega.getArchivoNombreGuardado() != null) {
                        Path path = Paths.get(UPLOAD_DIR + entrega.getArchivoNombreGuardado());
                        Files.deleteIfExists(path);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error al eliminar archivo físico: " + e.getMessage());
                // No detenemos la transacción por fallo en borrado de archivo
            }
        }

        // Eliminar registro de base de datos
        entregaRepository.delete(entrega);
    }
}
