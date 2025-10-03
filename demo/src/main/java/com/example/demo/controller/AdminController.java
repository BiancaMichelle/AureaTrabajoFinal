package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        // Obtener estadísticas básicas
        long totalOfertas = ofertaAcademicaRepository.count();
        long totalCategorias = categoriaRepository.count();
        
        model.addAttribute("totalOfertas", totalOfertas);
        model.addAttribute("totalCategorias", totalCategorias);
        
        return "admin/panelAdmin";
    }

    @GetMapping("/admin/gestion-ofertas")
    public String gestionOfertas(Model model) {
        List<Categoria> categorias = categoriaRepository.findAll();
        
        model.addAttribute("categorias", categorias);
        model.addAttribute("modalidades", Modalidad.values());
        model.addAttribute("estados", EstadoOferta.values());
        
        return "admin/gestionOfertas";
    }

    @GetMapping("/admin/gestion-usuarios")
    public String gestionUsuarios(Model model) {
        // Aquí podrías agregar datos necesarios para el formulario
        // model.addAttribute("generos", Genero.values());
        // model.addAttribute("estados", EstadoUsuario.values());
        
        return "admin/gestionUsuarios";
    }

    @PostMapping("/admin/ofertas/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearOferta(@RequestBody Map<String, Object> datos) {
        try {
            // Crear una oferta académica básica para demostrar funcionalidad
            OfertaAcademica oferta = new OfertaAcademica();
            oferta.setNombre((String) datos.get("nombre"));
            oferta.setDescripcion((String) datos.get("descripcion"));
            
            if (datos.get("cupos") != null) {
                oferta.setCupos(Integer.valueOf(datos.get("cupos").toString()));
            }
            
            if (datos.get("costo") != null) {
                oferta.setCostoInscripcion(Double.valueOf(datos.get("costo").toString()));
            }
            
            if (datos.get("fechaInicio") != null) {
                oferta.setFechaInicio(LocalDate.parse((String) datos.get("fechaInicio")));
            }
            
            if (datos.get("fechaFin") != null) {
                oferta.setFechaFin(LocalDate.parse((String) datos.get("fechaFin")));
            }
            
            // Configurar modalidad
            String modalidadStr = (String) datos.get("modalidad");
            if (modalidadStr != null) {
                oferta.setModalidad(Modalidad.valueOf(modalidadStr.toUpperCase()));
            }
            
            // Configurar estado por defecto
            oferta.setEstado(EstadoOferta.ACTIVA);
            oferta.setVisibilidad(true);
            
            OfertaAcademica nuevaOferta = ofertaAcademicaRepository.save(oferta);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta creada exitosamente");
            response.put("id", nuevaOferta.getIdOferta());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al crear oferta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/admin/ofertas/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarOferta(@RequestParam String tipoOferta,
                                 @RequestParam String nombre,
                                 @RequestParam(required = false) String descripcion,
                                 @RequestParam(required = false) Integer cupos,
                                 @RequestParam(required = false) Double costo,
                                 @RequestParam(required = false) String fechaInicio,
                                 @RequestParam(required = false) String fechaFin,
                                 @RequestParam(required = false) String modalidad) {
        try {
            // Crear nueva oferta académica
            OfertaAcademica oferta = new OfertaAcademica();
            oferta.setNombre(nombre);
            oferta.setDescripcion(descripcion);
            
            if (cupos != null) {
                oferta.setCupos(cupos);
            }
            
            if (costo != null) {
                oferta.setCostoInscripcion(costo);
            }
            
            if (fechaInicio != null && !fechaInicio.isEmpty()) {
                oferta.setFechaInicio(LocalDate.parse(fechaInicio));
            }
            
            if (fechaFin != null && !fechaFin.isEmpty()) {
                oferta.setFechaFin(LocalDate.parse(fechaFin));
            }
            
            // Configurar modalidad
            if (modalidad != null && !modalidad.isEmpty()) {
                oferta.setModalidad(Modalidad.valueOf(modalidad.toUpperCase()));
            }
            
            // Configurar estado por defecto
            oferta.setEstado(EstadoOferta.ACTIVA);
            oferta.setVisibilidad(true);
            
            OfertaAcademica nuevaOferta = ofertaAcademicaRepository.save(oferta);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta registrada exitosamente");
            response.put("id", nuevaOferta.getIdOferta());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al registrar oferta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/ofertas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarOfertas() {
        try {
            List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ofertas);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener ofertas: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/ofertas/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarOfertasAjax() {
        try {
            List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ofertas);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener ofertas: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // =================== ENDPOINTS PARA USUARIOS ===================

    @PostMapping("/admin/usuarios/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarUsuario(
            @RequestParam String dni,
            @RequestParam String nombreCompleto,
            @RequestParam String fechaNacimiento,
            @RequestParam String genero,
            @RequestParam String pais,
            @RequestParam String provincia,
            @RequestParam String ciudad,
            @RequestParam String domicilio,
            @RequestParam String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam String password,
            @RequestParam(required = false) String[] roles,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) Integer experiencia,
            @RequestParam(required = false) String horariosDisponibilidad,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String notificacionesEmail,
            @RequestParam(required = false) String cambiarPasswordPrimerAcceso) {
        
        try {
            // Crear usuario básico por ahora (esto se puede expandir según tu modelo de datos)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente");
            response.put("dni", dni);
            response.put("nombre", nombreCompleto);
            
            // Aquí irían las validaciones y el guardado real en la base de datos
            // Usuario nuevoUsuario = new Usuario();
            // nuevoUsuario.setDni(dni);
            // nuevoUsuario.setNombreCompleto(nombreCompleto);
            // ... etc
            // usuarioRepository.save(nuevoUsuario);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al registrar usuario: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/usuarios/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarUsuarios() {
        try {
            // Por ahora devolvemos datos de ejemplo
            List<Map<String, Object>> usuarios = new java.util.ArrayList<>();
            
            // Datos de ejemplo
            Map<String, Object> usuario1 = new HashMap<>();
            usuario1.put("id", 1);
            usuario1.put("nombreCompleto", "Juan Pérez");
            usuario1.put("dni", "12345678");
            usuario1.put("correo", "juan.perez@ejemplo.com");
            usuario1.put("roles", java.util.Arrays.asList("ALUMNO"));
            usuario1.put("estado", "ACTIVO");
            usuario1.put("fechaRegistro", LocalDate.now().toString());
            usuarios.add(usuario1);
            
            Map<String, Object> usuario2 = new HashMap<>();
            usuario2.put("id", 2);
            usuario2.put("nombreCompleto", "María García");
            usuario2.put("dni", "87654321");
            usuario2.put("correo", "maria.garcia@ejemplo.com");
            usuario2.put("roles", java.util.Arrays.asList("DOCENTE"));
            usuario2.put("estado", "ACTIVO");
            usuario2.put("fechaRegistro", LocalDate.now().toString());
            usuarios.add(usuario2);
            
            // Aquí irían los datos reales de la base de datos
            // List<Usuario> usuarios = usuarioRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", usuarios);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener usuarios: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}