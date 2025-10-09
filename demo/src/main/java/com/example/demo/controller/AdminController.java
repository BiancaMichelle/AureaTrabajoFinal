package com.example.demo.controller;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;
import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Categoria;
import com.example.demo.model.CarruselImagen;
import com.example.demo.model.Instituto;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pais;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CarruselImagenRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.service.ImagenService;
import com.example.demo.service.InstitutoService;
import com.example.demo.service.LocacionAPIService;
import com.example.demo.service.RegistroService;

@Controller
public class AdminController {

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private final LocacionAPIService locacionApiService;
    
    @Autowired
    private RegistroService registroService;
    
    @Autowired
    private InstitutoService institutoService;
    
    @Autowired
    private CarruselImagenRepository carruselImagenRepository;
    
    @Autowired
    private ImagenService imagenService;

    public AdminController(LocacionAPIService locacionApiService,
                           RegistroService registroService) {
        this.locacionApiService = locacionApiService;
        this.registroService = registroService;
    }

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

        try {
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            model.addAttribute("paises", paises);
        } catch (Exception e) {
            model.addAttribute("paises", List.of());
        }
        
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
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam String fechaNacimientoStr,
            @RequestParam TipoGenero genero,
            @RequestParam String paisCodigo,  // ✅ Cambiado de 'pais' a 'paisCodigo'
            @RequestParam String provinciaCodigo,
            @RequestParam String ciudadId,
            @RequestParam String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam String rol,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) Integer experiencia,
            @RequestParam(required = false) String horariosDisponibilidad,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false, defaultValue = "true") Boolean notificacionesEmail,
            @RequestParam(required = false, defaultValue = "false") Boolean cambiarPasswordPrimerAcceso,
            @RequestParam(required = false) String colegioEgreso,
            @RequestParam(required = false) Integer añoEgreso,
            @RequestParam(required = false) String ultimosEstudios) {
        
        try {
            System.out.println("📝 Registrando usuario desde admin:");
            System.out.println("   - DNI: " + dni);
            System.out.println("   - Nombre: " + nombre);
            System.out.println("   - Apellido: " + apellido);
            System.out.println("   - Rol: " + rol);
            System.out.println("   - País Código: " + paisCodigo); // ✅ Debug
            System.out.println("   - Provincia Código: " + provinciaCodigo); // ✅ Debug
            System.out.println("   - Ciudad ID: " + ciudadId); // ✅ Debug

            // Convertir fecha de DD/MM/AAAA a LocalDate
            LocalDate fechaNacimiento = null;
            if (fechaNacimientoStr != null && !fechaNacimientoStr.isEmpty()) {
                String[] partes = fechaNacimientoStr.split("/");
                if (partes.length == 3) {
                    int dia = Integer.parseInt(partes[0]);
                    int mes = Integer.parseInt(partes[1]);
                    int año = Integer.parseInt(partes[2]);
                    fechaNacimiento = LocalDate.of(año, mes, dia);
                    
                    // Validar edad mínima (16 años)
                    Period edad = Period.between(fechaNacimiento, LocalDate.now());
                    if (edad.getYears() < 16) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "El usuario debe tener al menos 16 años");
                        return ResponseEntity.badRequest().body(response);
                    }
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Formato de fecha inválido. Use DD/MM/AAAA");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Convertir ciudadId a Long
            Long ciudadIdLong = Long.valueOf(ciudadId);

            // Usar el servicio unificado
            Usuario nuevoUsuario = registroService.registrarUsuarioAdministrativo(
                dni, nombre, apellido, fechaNacimiento, genero,
                correo, telefono, paisCodigo, provinciaCodigo,  // ✅ Ahora enviamos códigos
                ciudadIdLong, rol, matricula,
                experiencia, colegioEgreso, añoEgreso, ultimosEstudios
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente. Las credenciales han sido enviadas al correo electrónico.");
            response.put("id", nuevoUsuario.getId());
            response.put("dni", nuevoUsuario.getDni());
            response.put("nombre", nuevoUsuario.getNombre() + " " + nuevoUsuario.getApellido());
            response.put("rol", rol);
            
            System.out.println("✅ Usuario registrado exitosamente desde admin: " + nuevoUsuario.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error al registrar usuario desde admin: " + e.getMessage());
            e.printStackTrace();
            
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

    // =================   CONFIGURACIONES INSTITUCIONALES   =================
    
    @GetMapping("/admin/configuracion")
    public String configuracionInstitucional(Model model) {
        // Obtener configuración actual del instituto
        Instituto instituto = institutoService.obtenerInstituto();
        
        // Obtener imágenes del carrusel
        List<CarruselImagen> imagenesCarrusel = carruselImagenRepository.findByInstitutoAndActivaTrueOrderByOrden(instituto);
        
        model.addAttribute("instituto", instituto);
        model.addAttribute("imagenesCarrusel", imagenesCarrusel);
        return "admin/configuraciones";
    }

    @PostMapping("/admin/configuracion/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarConfiguracion(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "logo", required = false) MultipartFile logo) {
        
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Validar campos requeridos
            if (params.get("nombreInstituto") == null || params.get("nombreInstituto").trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El nombre del instituto es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Obtener instituto actual
            Instituto instituto = institutoService.obtenerInstituto();
            
            // Actualizar campos
            instituto.setNombreInstituto(params.get("nombreInstituto"));
            instituto.setDescripcion(params.get("descripcion"));
            instituto.setMision(params.get("mision"));
            instituto.setVision(params.get("vision"));
            instituto.setDireccion(params.get("direccion"));
            instituto.setTelefono(params.get("telefono"));
            instituto.setEmail(params.get("email"));
            instituto.setFacebook(params.get("facebook"));
            instituto.setInstagram(params.get("instagram"));
            instituto.setX(params.get("x"));
            instituto.setMoneda(params.get("moneda"));
            instituto.setCuentaBancaria(params.get("cuentaBancaria"));
            instituto.setPoliticaPagos(params.get("politicaPagos"));
            
            // Configuraciones automáticas
            instituto.setPermisoBajaAutomatica("on".equals(params.get("permisoBajaAutomatica")));
            if (params.get("minimoAlumnoBaja") != null && !params.get("minimoAlumnoBaja").isEmpty()) {
                instituto.setMinimoAlumnoBaja(Integer.parseInt(params.get("minimoAlumnoBaja")));
            }
            if (params.get("inactividadBaja") != null && !params.get("inactividadBaja").isEmpty()) {
                instituto.setInactividadBaja(Integer.parseInt(params.get("inactividadBaja")));
            }
            instituto.setHabilitarIA("on".equals(params.get("habilitarIA")));
            instituto.setReportesAutomaticos("on".equals(params.get("reportesAutomaticos")));
            
            // Guardar colores institucionales
            List<String> colores = List.of(
                params.get("colorPrimario") != null ? params.get("colorPrimario") : "#1f2937",
                params.get("colorSecundario") != null ? params.get("colorSecundario") : "#f8fafc",
                params.get("colorTexto") != null ? params.get("colorTexto") : "#374151"
            );
            instituto.setColores(colores);
            
            // Procesar logo si se subió
            if (logo != null && !logo.isEmpty()) {
                String logoPath = guardarLogo(logo);
                instituto.setLogoPath(logoPath);
            }
            
            // Guardar instituto
            institutoService.guardarInstituto(instituto);
            
            response.put("success", true);
            response.put("message", "Configuración guardada exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al guardar la configuración: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // ================= GESTIÓN WEB DEL CARRUSEL =================
    
    @PostMapping("/admin/configuracion/carrusel/subir")
    public String subirImagenesCarruselWeb(
            @RequestParam("imagenes") MultipartFile[] imagenes,
            Model model) {
        
        System.out.println("=== INICIO SUBIDA DE IMÁGENES ===");
        System.out.println("Número de imágenes recibidas: " + (imagenes != null ? imagenes.length : "null"));
        
        try {
            // Validaciones
            if (imagenes == null || imagenes.length == 0) {
                System.out.println("ERROR: No se han seleccionado imágenes");
                model.addAttribute("error", "No se han seleccionado imágenes");
                return "redirect:/admin/configuracion?error=nofiles";
            }

            for (int i = 0; i < imagenes.length; i++) {
                MultipartFile imagen = imagenes[i];
                System.out.println("Imagen " + i + ": " + imagen.getOriginalFilename() + 
                                 " - Tamaño: " + imagen.getSize() + " bytes");
            }

            Instituto instituto = institutoService.obtenerInstituto();
            System.out.println("Instituto obtenido: " + (instituto != null ? "OK" : "null"));
            
            List<CarruselImagen> imagenesGuardadas = imagenService.guardarMultiplesImagenesCarrusel(imagenes, instituto);
            System.out.println("Imágenes guardadas exitosamente: " + imagenesGuardadas.size());
            
            model.addAttribute("mensaje", "Imágenes subidas exitosamente: " + imagenesGuardadas.size());
            return "redirect:/admin/configuracion?success=upload";
            
        } catch (IOException e) {
            System.out.println("ERROR IOException: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al procesar las imágenes: " + e.getMessage());
            return "redirect:/admin/configuracion?error=processing";
        } catch (Exception e) {
            System.out.println("ERROR Exception: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error interno del servidor: " + e.getMessage());
            return "redirect:/admin/configuracion?error=server";
        }
    }
    
    @PostMapping("/admin/configuracion/carrusel/eliminar/{id}")
    public String eliminarImagenCarruselWeb(@PathVariable Long id, Model model) {
        try {
            Optional<CarruselImagen> imagen = imagenService.obtenerImagenCarrusel(id);
            if (!imagen.isPresent()) {
                return "redirect:/admin/configuracion?error=notfound";
            }
            
            imagenService.eliminarImagenCarrusel(id);
            return "redirect:/admin/configuracion?success=delete";
            
        } catch (Exception e) {
            return "redirect:/admin/configuracion?error=deletefail";
        }
    }
    
    // ================= MÉTODOS AUXILIARES =================
    
    private String guardarLogo(MultipartFile logo) throws IOException {
        // Crear directorio si no existe
        Path directorioLogos = Paths.get("src/main/resources/static/img/logos");
        if (!Files.exists(directorioLogos)) {
            Files.createDirectories(directorioLogos);
        }
        
        // Generar nombre único
        String nombreOriginal = logo.getOriginalFilename();
        String extension = nombreOriginal != null ? nombreOriginal.substring(nombreOriginal.lastIndexOf(".")) : ".jpg";
        String nombreArchivo = "logo_" + UUID.randomUUID().toString() + extension;
        
        // Guardar archivo
        Path rutaArchivo = directorioLogos.resolve(nombreArchivo);
        Files.copy(logo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
        
        return "/img/logos/" + nombreArchivo;
    }
    
    // MÉTODO TEMPORAL PARA CREAR ADMIN - ELIMINAR DESPUÉS DE USAR
    @GetMapping("/crear-admin-temporal")
    public ResponseEntity<String> crearAdminTemporal() {
        try {
            registroService.crearUsuarioAdminTemporal();
            return ResponseEntity.ok("Usuario admin creado: DNI=admin, Password=admin123");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}