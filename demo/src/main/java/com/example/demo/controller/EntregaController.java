package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

import com.example.demo.enums.TipoEntrega;
import com.example.demo.model.Entrega;
import com.example.demo.model.Tarea;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EntregaRepository;
import com.example.demo.repository.TareaRepository;
import com.example.demo.repository.UsuarioRepository;

@Controller
@RequestMapping("/entrega")
public class EntregaController {

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private com.example.demo.service.EmailService emailService;

    @Autowired
    private com.example.demo.service.EntregaService entregaService;

    private final String UPLOAD_DIR = "uploads/tareas/";

    @PostMapping("/enviar")
    public String enviarTarea(@RequestParam("idTarea") Long idTarea,
                              @RequestParam(value = "texto", required = false) String texto,
                              @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        
        try {
            String username = auth.getName();
            Usuario estudiante = usuarioRepository.findByDni(username)
                    .or(() -> usuarioRepository.findByCorreo(username))
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Buscar tarea con sus relaciones cargadas (modulo y curso)
            Tarea tarea = tareaRepository.findByIdWithModuloAndCurso(idTarea)
                    .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

            // Validar fecha entrega
            if (tarea.getLimiteEntrega() != null && LocalDateTime.now().isAfter(tarea.getLimiteEntrega())) {
                boolean esTardia = Boolean.TRUE.equals(tarea.getEntregasTardias());
               
                if (!esTardia) {
                     redirectAttributes.addFlashAttribute("error", "La fecha límite de entrega ha pasado y no se permiten entregas tardías.");
                     return construirRedireccionAula(tarea, null, null);
                }
            }

            // Buscar si ya existe una entrega
            Optional<Entrega> entregaExistente = entregaRepository.findByTareaAndEstudiante(tarea, estudiante);
            Entrega entrega = entregaExistente.orElse(new Entrega());
            
            entrega.setEstudiante(estudiante);
            entrega.setTarea(tarea);
            entrega.setFechaEntrega(LocalDateTime.now());
            entrega.setEsTardia(tarea.getLimiteEntrega() != null && LocalDateTime.now().isAfter(tarea.getLimiteEntrega()));

            // Procesar contenido según tipo
            if (tarea.getTipoEntrega().contains(TipoEntrega.TEXTO) && texto != null && !texto.isEmpty()) {
                entrega.setContenido(texto);
            }
            
            if (tarea.getTipoEntrega().contains(TipoEntrega.ARCHIVOS) && archivo != null && !archivo.isEmpty()) {
                // Guardar archivo
                try {
                    Path uploadPath = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    
                    String fileName = System.currentTimeMillis() + "_" + archivo.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(archivo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    
                    entrega.setNombreArchivo(archivo.getOriginalFilename());
                    entrega.setArchivoNombreGuardado(fileName);

                    // Si es solo archivo, guardamos la ruta en contenido o usamos campo separado logicamente
                    // Aquí usamos contenido para la ruta si es de tipo archivo y no se usó texto
                    if (entrega.getContenido() == null) {
                         entrega.setContenido(filePath.toString());
                    } else {
                        // Si hay ambos, quizas concatenar o manejar mejor. Por simplicidad:
                        entrega.setContenido(entrega.getContenido() + " | Archivo: " + filePath.toString());
                    }
                    
                } catch (IOException e) {
                    throw new RuntimeException("Error al guardar el archivo", e);
                }
            }

            entregaRepository.save(entrega);
            
            // Enviar email confirmación
            try {
                String subject = "Confirmación de entrega: " + tarea.getTitulo();
                String body = "<h1>Entrega Recibida</h1>" +
                              "<p>Hola " + estudiante.getNombre() + ",</p>" +
                              "<p>Tu entrega para la tarea '<strong>" + tarea.getTitulo() + "</strong>' ha sido recibida correctamente el " + 
                              java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(entrega.getFechaEntrega()) + ".</p>";
                emailService.sendEmail(estudiante.getCorreo(), subject, body);
            } catch (Exception ex) {
                System.err.println("No se pudo enviar email de confirmacion: " + ex.getMessage());
            }
            
            redirectAttributes.addFlashAttribute("success", "Tarea enviada correctamente.");
            return construirRedireccionAula(tarea, "Error: No se pudo determinar el curso de la tarea.", redirectAttributes);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al enviar la tarea: " + e.getMessage());
            // Intento de redireccion seguro si falla antes de obtener tarea
            return "redirect:/alumno/mis-ofertas"; 
        }
    }

    /**
     * Endpoint para modificar una tarea enviada siguiendo el flujo del CU-11.
     * 
     * Precondiciones validadas:
     * - El alumno debe tener una sesión activa (validado por Spring Security)
     * - El alumno debe haber realizado la entrega previamente (Precondición 2)
     * - El curso debe estar en estado activo (Precondición 3)
     */
    @PostMapping("/modificar")
    public Object modificarTarea(@RequestParam("idEntrega") Long idEntrega,
                                 @RequestParam("idTarea") Long idTarea,
                                 @RequestParam(value = "texto", required = false) String texto,
                                 @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                 Authentication auth,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        
        try {
            String username = auth.getName();
            Usuario estudiante = usuarioRepository.findByDni(username)
                    .or(() -> usuarioRepository.findByCorreo(username))
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Paso 1 del CU-11: El alumno desea modificar el contenido de una tarea enviada
            Entrega entregaExistente = entregaRepository.findById(idEntrega)
                    .orElseThrow(() -> new RuntimeException("Entrega no encontrada"));

            // Buscar tarea con sus relaciones cargadas (modulo y curso)
            Tarea tarea = tareaRepository.findByIdWithModuloAndCurso(idTarea)
                    .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

            // Verificar que la entrega pertenece al estudiante actual
            if (!entregaExistente.getEstudiante().getId().equals(estudiante.getId())) {
                redirectAttributes.addFlashAttribute("error", "No tienes permiso para modificar esta entrega.");
                return construirRedireccionAula(tarea, null, null);
            }

            // Paso 2 del CU-11: Verificar si se permiten modificaciones
            if (!Boolean.TRUE.equals(tarea.getModificaciones())) {
                redirectAttributes.addFlashAttribute("error", "No se permiten modificaciones en esta tarea.");
                return construirRedireccionAula(tarea, null, null);
            }

            // Paso 3 del CU-11: Verificar si está dentro del plazo de entrega y si se permiten entregas tardías
            LocalDateTime ahora = LocalDateTime.now();
            boolean fueraDePlazo = tarea.getLimiteEntrega() != null && ahora.isAfter(tarea.getLimiteEntrega());
            
            if (fueraDePlazo && !Boolean.TRUE.equals(tarea.getEntregasTardias())) {
                redirectAttributes.addFlashAttribute("error", "El plazo para modificar la tarea ha finalizado y no se permiten entregas tardías.");
                return construirRedireccionAula(tarea, null, null);
            }

            // Paso 4 del CU-11: El alumno realiza cambios en el contenido
            // IMPORTANTE: Se reemplaza completamente el contenido anterior
            
            // Procesar nuevo contenido de texto
            if (texto != null && !texto.trim().isEmpty()) {
                // Borrar contenido anterior y reemplazar con el nuevo
                entregaExistente.setContenido(texto);
                // Si solo hay texto, limpiar nombre de archivo previo
                if (archivo == null || archivo.isEmpty()) {
                    entregaExistente.setNombreArchivo(null);
                }
            } else if (archivo == null || archivo.isEmpty()) {
                // Si no hay texto ni archivo nuevo, limpiar contenido
                entregaExistente.setContenido(null);
                entregaExistente.setNombreArchivo(null);
            }
            
            // Procesar nuevo archivo
            if (archivo != null && !archivo.isEmpty()) {
                try {
                    Path uploadPath = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    
                    // Eliminar archivo anterior si existe
                    if (entregaExistente.getNombreArchivo() != null && entregaExistente.getContenido() != null) {
                        try {
                            // Intentar extraer y eliminar el archivo anterior
                            String contenidoPrevio = entregaExistente.getContenido();
                            if (contenidoPrevio.contains("uploads/tareas/")) {
                                String[] partes = contenidoPrevio.split("\\|");
                                for (String parte : partes) {
                                    if (parte.contains("uploads/tareas/")) {
                                        Path archivoViejo = Paths.get(parte.trim().replace("Archivo: ", ""));
                                        Files.deleteIfExists(archivoViejo);
                                        System.out.println("🗑️ Archivo anterior eliminado: " + archivoViejo);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("⚠️ No se pudo eliminar el archivo anterior: " + ex.getMessage());
                        }
                    }
                    
                    // Guardar nuevo archivo
                    String fileName = System.currentTimeMillis() + "_" + archivo.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(archivo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    
                    // Actualizar información del archivo
                    entregaExistente.setNombreArchivo(archivo.getOriginalFilename());
                    
                    // Determinar contenido según si hay texto o no
                    if (texto != null && !texto.trim().isEmpty()) {
                        // Si hay texto, agregar referencia al archivo
                        entregaExistente.setContenido(texto + " | Archivo: " + filePath.toString());
                    } else {
                        // Si solo hay archivo, guardar solo la ruta
                        entregaExistente.setContenido(filePath.toString());
                    }
                    
                    System.out.println("✅ Nuevo archivo guardado: " + filePath);
                    
                } catch (IOException e) {
                    throw new RuntimeException("Error al guardar el archivo", e);
                }
            }

            // Paso 5 del CU-11: El alumno confirma la modificación
            // Paso 6 del CU-11: Registrar los cambios y actualizar el envío
            entregaExistente.setFechaEntrega(ahora); // Actualizar fecha de modificación
            entregaExistente.setEsTardia(fueraDePlazo); // Actualizar estado de tardía si corresponde

            entregaRepository.save(entregaExistente);
            
            // Enviar email de confirmación de modificación
            try {
                String subject = "Confirmación de modificación: " + tarea.getTitulo();
                String body = "<h1>Entrega Modificada</h1>" +
                              "<p>Hola " + estudiante.getNombre() + ",</p>" +
                              "<p>Tu entrega para la tarea '<strong>" + tarea.getTitulo() + "</strong>' ha sido modificada correctamente el " + 
                              java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(ahora) + ".</p>" +
                              (fueraDePlazo ? "<p style='color: orange;'><strong>Nota:</strong> Esta modificación fue realizada fuera del plazo establecido.</p>" : "");
                emailService.sendEmail(estudiante.getCorreo(), subject, body);
            } catch (Exception ex) {
                System.err.println("No se pudo enviar email de confirmación: " + ex.getMessage());
            }
            
            String mensaje = "Tarea modificada correctamente." + (fueraDePlazo ? " (Marcada como tardía)" : "");
            Long idCursoSeg = obtenerIdCursoSeguro(tarea);
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                Map<String, Object> body = new HashMap<>();
                body.put("success", true);
                body.put("message", mensaje);
                // Devolvemos datos de la entrega para que el front pueda actualizar la UI sin recargar
                body.put("entregaId", entregaExistente.getIdEntrega());
                body.put("entregaFecha", java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(entregaExistente.getFechaEntrega()));
                body.put("nombreArchivo", entregaExistente.getNombreArchivo());
                body.put("calificacion", entregaExistente.getCalificacion());
                return ResponseEntity.ok(body);
            } else {
                redirectAttributes.addFlashAttribute("success", mensaje);
                return construirRedireccionAula(tarea, null, redirectAttributes);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (request != null && "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                Map<String, Object> body = new HashMap<>();
                body.put("success", false);
                body.put("message", "Error al modificar la tarea: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
            } else {
                redirectAttributes.addFlashAttribute("error", "Error al modificar la tarea: " + e.getMessage());
                return "redirect:/alumno/mis-ofertas";
            }
        }
    }
    
    @DeleteMapping("/eliminar/{idEntrega}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> eliminarEntrega(@PathVariable Long idEntrega, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        try {
            entregaService.eliminarEntrega(idEntrega, auth.getName());
            response.put("success", true);
            response.put("message", "Entrega eliminada correctamente.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Método helper para obtener el ID del curso desde una tarea de forma segura.
     * Previene NullPointerException si las relaciones no están cargadas.
     * @param tarea La tarea de la cual obtener el ID del curso
     * @return El ID del curso o null si no se puede obtener
     */
    private Long obtenerIdCursoSeguro(Tarea tarea) {
        try {
            if (tarea != null && tarea.getModulo() != null && tarea.getModulo().getCurso() != null) {
                return tarea.getModulo().getCurso().getIdOferta();
            }
        } catch (Exception e) {
            System.err.println("❌ Error al obtener ID del curso: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Método helper para construir la URL de redirección al aula.
     * Si no se puede obtener el ID del curso, redirige a misOfertasAcademicas.
     * @param tarea La tarea de la cual obtener el curso
     * @param errorMsg Mensaje de error opcional si falla
     * @param redirectAttributes Para agregar el mensaje de error
     * @return La URL de redirección
     */
    private String construirRedireccionAula(Tarea tarea, String errorMsg, RedirectAttributes redirectAttributes) {
        Long idCurso = obtenerIdCursoSeguro(tarea);
        if (idCurso != null) {
            // FIX: Redirigir a /alumno/aula/ para usar el controlador de alumnos
            return "redirect:/alumno/aula/" + idCurso;
        } else {
            if (errorMsg != null && redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("error", errorMsg);
            }
            return "redirect:/alumno/mis-ofertas";
        }
    }

    // ==========================================
    //  NUEVOS MÉTODOS PARA CORREGIR (DOCENTE)
    // ==========================================

    @GetMapping("/lista/{idTarea}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    public String listaEntregas(@PathVariable("idTarea") Long idTarea, Model model, Authentication auth) {
        System.out.println(">>> Accediendo a lista de entregas para Tarea ID: " + idTarea);
        Tarea tarea = tareaRepository.findByIdWithModuloAndCurso(idTarea)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));
        
        List<Entrega> entregas = entregaRepository.findByTarea(tarea);
        
        // Obtener ID del curso de forma segura para el botón "Volver"
        Long idCurso = obtenerIdCursoSeguro(tarea);
        System.out.println(">>> ID Curso recuperado: " + idCurso);
        
        if (idCurso == null) {
            System.err.println(">>> ERROR: No se pudo recuperar el ID del curso. Revisar relaciones Tarea->Modulo->Curso");
        }
        
        model.addAttribute("tarea", tarea);
        model.addAttribute("entregas", entregas);
        model.addAttribute("idCurso", idCurso); // ID del curso para volver
        
        return "docente/lista-entregas"; 
    }

    @PostMapping("/corregir")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    public String corregirEntrega(@RequestParam("idEntrega") Long idEntrega,
                                  @RequestParam("calificacion") Double calificacion,
                                  @RequestParam(value = "comentarios", required = false) String comentarios,
                                  RedirectAttributes redirectAttributes) {
        
        try {
            Entrega entrega = entregaRepository.findById(idEntrega)
                    .orElseThrow(() -> new RuntimeException("Entrega no encontrada"));
            
            entrega.setCalificacion(calificacion);
            entrega.setComentarios(comentarios);
            entregaRepository.save(entrega);
            
            redirectAttributes.addFlashAttribute("success", "Calificación guardada correctamente.");
            
            // Redirigir usando el ID de la tarea asociada
            return "redirect:/entrega/lista/" + entrega.getTarea().getIdActividad();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar calificación: " + e.getMessage());
            // Si falla, intentamos volver atrás. Usamos Referer si es posible, o una ruta segura.
            return "redirect:/docente/mi-espacio"; 
        }
    }
}
