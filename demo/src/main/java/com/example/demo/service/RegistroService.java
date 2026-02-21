package com.example.demo.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Ciudad;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DisponibilidadDocenteRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.HorarioRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class RegistroService {
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;
    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final EmailService emailService; 
    private final LocacionAPIService locacionApiService;
    private final HorarioRepository horarioRepository;
    private final DisponibilidadDocenteRepository disponibilidadDocenteRepository;
    private final DisponibilidadDocenteService disponibilidadDocenteService;
    private final ObjectMapper objectMapper;
    private final InscripcionRepository inscripcionRepository;
    private final CursoRepository cursoRepository;


    public RegistroService(UsuarioRepository usuarioRepository,
                          AlumnoRepository alumnoRepository,
                          DocenteRepository docenteRepository,
                          PaisRepository paisRepository,
                          ProvinciaRepository provinciaRepository,
                          CiudadRepository ciudadRepository,
                          PasswordEncoder passwordEncoder,
                          RolRepository rolRepository,
                          EmailService emailService,
                          LocacionAPIService locacionApiService,
                          HorarioRepository horarioRepository,
                          DisponibilidadDocenteRepository disponibilidadDocenteRepository,
                          DisponibilidadDocenteService disponibilidadDocenteService,
                          ObjectMapper objectMapper,
                          InscripcionRepository inscripcionRepository,
                          CursoRepository cursoRepository) { 
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.docenteRepository = docenteRepository;
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.emailService = emailService; 
        this.locacionApiService = locacionApiService;
        this.horarioRepository = horarioRepository;
        this.disponibilidadDocenteRepository = disponibilidadDocenteRepository;
        this.disponibilidadDocenteService = disponibilidadDocenteService;
        this.objectMapper = objectMapper;
        this.inscripcionRepository = inscripcionRepository;
        this.cursoRepository = cursoRepository;
    }

    // 🔑 MÉTODO PARA GENERAR CONTRASEÑA (se mantiene igual)
    private String generarContraseñaAleatoria() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder contraseña = new StringBuilder();
        Random random = new Random();
        
        contraseña.append((char) (random.nextInt(26) + 'A'));
        contraseña.append((char) (random.nextInt(26) + 'a'));
        contraseña.append((char) (random.nextInt(10) + '0'));
        contraseña.append("!@#$%".charAt(random.nextInt(5)));
        
        for (int i = 4; i < 12; i++) {
            contraseña.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        
        char[] arrayContraseña = contraseña.toString().toCharArray();
        for (int i = arrayContraseña.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = arrayContraseña[i];
            arrayContraseña[i] = arrayContraseña[j];
            arrayContraseña[j] = temp;
        }
        
        return new String(arrayContraseña);
    }

    // 📧 MÉTODO PARA ENVIAR EMAIL (se mantiene igual)
    private void enviarCredencialesPorEmail(String correo, String nombre, String contraseña, String rol) {
        try {
            String subject = "🎓 Bienvenido a Espacio Virtual ICEP - Sus Credenciales de Acceso";
            String body = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                "        .container { max-width: 600px; margin: 30px auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
                "        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; }" +
                "        .header h1 { margin: 0; font-size: 28px; }" +
                "        .content { padding: 30px; color: #333; }" +
                "        .welcome { font-size: 18px; color: #555; margin-bottom: 20px; }" +
                "        .credentials-box { background: #f8f9fa; border-left: 4px solid #667eea; padding: 20px; margin: 20px 0; border-radius: 5px; }" +
                "        .credential-item { margin: 10px 0; font-size: 15px; }" +
                "        .credential-label { font-weight: bold; color: #667eea; display: inline-block; width: 180px; }" +
                "        .credential-value { color: #333; font-family: 'Courier New', monospace; background: white; padding: 5px 10px; border-radius: 3px; }" +
                "        .alert-box { background: #fff3cd; border: 1px solid #ffc107; border-radius: 5px; padding: 15px; margin: 20px 0; }" +
                "        .alert-icon { color: #ff9800; font-size: 20px; }" +
                "        .btn { display: inline-block; background: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                "        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #777; font-size: 13px; }" +
                "        .divider { height: 2px; background: linear-gradient(to right, transparent, #667eea, transparent); margin: 20px 0; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>🎓 Espacio Virtual ICEP</h1>" +
                "            <p style='margin: 10px 0 0 0; font-size: 14px;'>Tu plataforma de educación virtual</p>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p class='welcome'>Estimado/a <strong>%s</strong>,</p>" +
                "            <p>¡Le damos la más cordial bienvenida a <strong>Espacio Virtual ICEP</strong>! Nos complace tenerle como parte de nuestra comunidad educativa.</p>" +
                "            <div class='divider'></div>" +
                "            <h3 style='color: #667eea;'>📋 Sus Credenciales de Acceso</h3>" +
                "            <div class='credentials-box'>" +
                "                <div class='credential-item'>" +
                "                    <span class='credential-label'>📧 Correo electrónico:</span>" +
                "                    <span class='credential-value'>%s</span>" +
                "                </div>" +
                "                <div class='credential-item'>" +
                "                    <span class='credential-label'>🔑 Contraseña temporal:</span>" +
                "                    <span class='credential-value'>%s</span>" +
                "                </div>" +
                "                <div class='credential-item'>" +
                "                    <span class='credential-label'>👤 Rol asignado:</span>" +
                "                    <span class='credential-value'>%s</span>" +
                "                </div>" +
                "            </div>" +
                "            <div class='alert-box'>" +
                "                <p style='margin: 0;'><span class='alert-icon'>⚠️</span> <strong>Importante:</strong> Por su seguridad, le recomendamos cambiar su contraseña después del primer acceso al sistema.</p>" +
                "            </div>" +
                "            <div style='text-align: center;'>" +
                "                <a href='http://localhost:8080/login' class='btn'>🚀 Acceder al Sistema</a>" +
                "            </div>" +
                "            <div class='divider'></div>" +
                "            <p style='font-size: 14px; color: #777;'>Si tiene alguna dificultad para acceder o necesita asistencia, no dude en contactarnos.</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p style='margin: 5px 0;'><strong>Espacio Virtual ICEP</strong></p>" +
                "            <p style='margin: 5px 0;'>© 2025 - Todos los derechos reservados</p>" +
                "            <p style='margin: 5px 0; font-size: 11px;'>Este es un mensaje automático, por favor no responda a este correo.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>",
                nombre, correo, contraseña, rol
            );
            
            emailService.sendEmail(correo, subject, body);
            System.out.println("✅ Email enviado a: " + correo);
            
        } catch (Exception e) {
            System.out.println("❌ Error enviando email: " + e.getMessage());
        }
    }

    private Pais buscarOCrearPais(String paisCodigo) {
        Objects.requireNonNull(paisCodigo, "paisCodigo no puede ser nulo");
        System.out.println("🌎 Buscando país en BD con código: " + paisCodigo);

        Optional<Pais> paisExistente = paisRepository.findByCodigo(paisCodigo);
        if (paisExistente.isPresent()) {
            System.out.println("✅ País encontrado en BD, se reutiliza: " + paisExistente.get().getNombre());
            return paisExistente.get();
        }

        System.out.println("🔁 País no encontrado en BD, consultando API externa...");

        try {
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            for (Pais p : paises) {
                if (paisCodigo.equals(p.getCodigo())) {
                    Pais nuevoPais = new Pais();
                    nuevoPais.setCodigo(p.getCodigo());
                    nuevoPais.setNombre(p.getNombre());
                    nuevoPais = paisRepository.save(nuevoPais);
                    System.out.println("✅ País creado desde API: " + nuevoPais.getNombre());
                    return nuevoPais;
                }
            }

            throw new RuntimeException("❌ País con código '" + paisCodigo + "' no encontrado en API");

        } catch (Exception e) {
            System.out.println("❌ Error obteniendo/creando país: " + e.getMessage());
            throw new RuntimeException("Error al obtener país desde API: " + e.getMessage(), e);
        }
    }

    private Provincia buscarOCrearProvincia(String provinciaCodigo, Pais pais) {
        Objects.requireNonNull(provinciaCodigo, "provinciaCodigo no puede ser nulo");
        Objects.requireNonNull(pais, "pais no puede ser nulo");
        System.out.println("🏙️ Buscando provincia en BD con código: " + provinciaCodigo);

        Optional<Provincia> provinciaExistente = provinciaRepository.findByCodigo(provinciaCodigo);
        if (provinciaExistente.isPresent()) {
            Provincia provincia = provinciaExistente.get();
            if (provincia.getPais() == null || !provincia.getPais().getCodigo().equals(pais.getCodigo())) {
                provincia.setPais(pais);
                provincia = provinciaRepository.save(provincia);
            }
            System.out.println("✅ Provincia encontrada en BD, se reutiliza: " + provincia.getNombre());
            return provincia;
        }

        System.out.println("🔁 Provincia no encontrada en BD, consultando API externa...");

        try {
            List<Provincia> provincias = locacionApiService.obtenerProvinciasPorPais(pais.getCodigo());
            for (Provincia p : provincias) {
                if (provinciaCodigo.equals(p.getCodigo())) {
                    Provincia nuevaProvincia = new Provincia();
                    nuevaProvincia.setCodigo(p.getCodigo());
                    nuevaProvincia.setNombre(p.getNombre());
                    nuevaProvincia.setPais(pais);
                    nuevaProvincia = provinciaRepository.save(nuevaProvincia);
                    System.out.println("✅ Provincia creada desde API: " + nuevaProvincia.getNombre());
                    return nuevaProvincia;
                }
            }

            throw new RuntimeException("❌ Provincia con código '" + provinciaCodigo + "' no encontrada en API para país " + pais.getCodigo());

        } catch (Exception e) {
            System.out.println("❌ Error obteniendo/creando provincia: " + e.getMessage());
            throw new RuntimeException("Error al obtener provincia desde API: " + e.getMessage(), e);
        }
    }

    private Ciudad buscarOCrearCiudad(Long ciudadId, Provincia provincia, String paisCodigo, String provinciaCodigo) {
        if (ciudadId == null) {
            return null;
        }
        Objects.requireNonNull(provincia, "provincia no puede ser nula");
        Objects.requireNonNull(paisCodigo, "paisCodigo no puede ser nulo");
        Objects.requireNonNull(provinciaCodigo, "provinciaCodigo no puede ser nulo");
        System.out.println("🏡 Buscando ciudad en BD con ID: " + ciudadId);

        Optional<Ciudad> ciudadExistente = ciudadRepository.findById(ciudadId);
        if (ciudadExistente.isPresent()) {
            Ciudad ciudad = ciudadExistente.get();
            if (ciudad.getProvincia() == null || !ciudad.getProvincia().getCodigo().equals(provincia.getCodigo())) {
                ciudad.setProvincia(provincia);
                ciudad = ciudadRepository.save(ciudad);
            }
            System.out.println("✅ Ciudad encontrada en BD, se reutiliza: " + ciudad.getNombre());
            return ciudad;
        }

        System.out.println("🔁 Ciudad no encontrada en BD, consultando API externa...");

        try {
            List<Ciudad> ciudades = locacionApiService.obtenerCiudadesPorProvincia(paisCodigo, provinciaCodigo);
            for (Ciudad c : ciudades) {
                if (ciudadId.equals(c.getId())) {
                    Ciudad nuevaCiudad = new Ciudad();
                    nuevaCiudad.setId(c.getId());
                    nuevaCiudad.setNombre(c.getNombre());
                    nuevaCiudad.setProvincia(provincia);
                    nuevaCiudad = ciudadRepository.save(nuevaCiudad);
                    System.out.println("✅ Ciudad creada desde API: " + nuevaCiudad.getNombre());
                    return nuevaCiudad;
                }
            }

            throw new RuntimeException("❌ Ciudad con ID '" + ciudadId + "' no encontrada en API para provincia " + provinciaCodigo);

        } catch (Exception e) {
            System.out.println("❌ Error obteniendo/creando ciudad: " + e.getMessage());
            throw new RuntimeException("Error al obtener ciudad desde API: " + e.getMessage(), e);
        }
    }

    // 👤 MÉTODO UNIFICADO PRINCIPAL (MEJORADO CON BUSCAR O CREAR)
    public Usuario registrarUsuario(Usuario usuario, 
                                   String paisCodigo, 
                                   String provinciaCodigo, 
                                   Long ciudadId, 
                                   String rolPrincipal,
                                   boolean esRegistroAdministrativo) {  // ✅ NUEVO PARÁMETRO
        
        System.out.println("🔍 Iniciando registro para: " + usuario.getNombre());
        System.out.println("🎯 Rol asignado: " + rolPrincipal);
        System.out.println("👨‍💼 Registro administrativo: " + esRegistroAdministrativo);
        
        try {
            // 1. Verificar si el DNI ya existe
            if (usuarioRepository.existsByDni(usuario.getDni())) {
                throw new RuntimeException("El DNI ya está registrado");
            }

            // 2. Verificar si el email ya existe
            if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
                throw new RuntimeException("El correo electrónico ya está registrado");
            }

            // ✅ 3. BUSCAR O CREAR UBICACIONES AUTOMÁTICAMENTE (MEJORADO)
            Pais pais = buscarOCrearPais(paisCodigo);
            Provincia provincia = buscarOCrearProvincia(provinciaCodigo, pais);
            Ciudad ciudad = buscarOCrearCiudad(ciudadId, provincia, paisCodigo, provinciaCodigo);

            // 4. ASIGNAR UBICACIONES AL USUARIO
            usuario.setPais(pais);
            usuario.setProvincia(provincia);
            usuario.setCiudad(ciudad);

            System.out.println("📍 Ubicaciones asignadas:");
            System.out.println("   - País: " + pais.getNombre());
            System.out.println("   - Provincia: " + provincia.getNombre());
            System.out.println("   - Ciudad: " + (ciudad != null ? ciudad.getNombre() : "Sin ciudad"));

            // 5. LÓGICA DE CONTRASEÑA MODIFICADA
            String contraseñaPlana;
            if (esRegistroAdministrativo) {
                // ✅ REGISTRO ADMINISTRATIVO: Generar contraseña aleatoria
                contraseñaPlana = generarContraseñaAleatoria();
                System.out.println("🔑 Contraseña generada: " + contraseñaPlana);
            } else {
                // ✅ REGISTRO PÚBLICO: Usar la contraseña que el usuario ingresó
                contraseñaPlana = usuario.getContraseña();
                System.out.println("🔑 Contraseña proporcionada por usuario");
            }
            
            // 6. Encriptar contraseña
            usuario.setContraseña(passwordEncoder.encode(contraseñaPlana));

            // 7. Establecer estado por defecto
            usuario.setEstado(true);
            usuario.setEstadoCuenta(true);

            // 8. ASIGNAR ROLES según el tipo de usuario
            asignarRoles(usuario, rolPrincipal);

            // 9. Guardar según el tipo de usuario
            Usuario usuarioGuardado;
            
            switch (rolPrincipal.toUpperCase()) {
                case "ALUMNO":
                    usuarioGuardado = guardarAlumno((Alumno) usuario);
                    break;
                case "DOCENTE":
                    usuarioGuardado = guardarDocente((Docente) usuario);
                    break;
                case "ADMIN":
                case "COORDINADOR":
                    usuarioGuardado = guardarUsuarioBase(usuario);
                    break;
                default:
                    throw new RuntimeException("Rol no válido: " + rolPrincipal);
            }

            // ✅ 10. ENVIAR EMAIL SOLO EN REGISTRO ADMINISTRATIVO
            if (esRegistroAdministrativo) {
                enviarCredencialesPorEmail(
                    usuario.getCorreo(), 
                    usuario.getNombre() + " " + usuario.getApellido(),
                    contraseñaPlana,
                    rolPrincipal
                );
                System.out.println("📧 Email enviado al usuario");
            } else {
                System.out.println("📧 Email NO enviado - Registro público");
            }

            System.out.println("✅ Registro completado. ID: " + usuarioGuardado.getId() + " - Rol: " + rolPrincipal);
            return usuarioGuardado;
            
        } catch (Exception e) {
            System.out.println("❌ Error en registro: " + e.getMessage());
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }

    // 👨‍🎓 MÉTODO PARA REGISTRO PÚBLICO DE ALUMNOS (MODIFICADO)
    public void registrarUsuario(Alumno alumno, String paisCodigo, String provinciaCodigo, Long ciudadId) {
        registrarUsuario(alumno, paisCodigo, provinciaCodigo, ciudadId, "ALUMNO", false); // ✅ NO es administrativo
    }

    // 👨‍💼 MÉTODO PARA REGISTRO ADMINISTRATIVO (MODIFICADO)
    public Usuario registrarUsuarioAdministrativo(
            String dni,
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            TipoGenero genero,
            String correo,
            String telefono,
            String paisCodigo,
            String provinciaCodigo,
            Long ciudadId,
            String rolPrincipal,
            String matricula,
            Integer experiencia,
            String colegioEgreso,
            Integer añoEgreso,
            String ultimosEstudios,
            List<Map<String, String>> horarios) {
        
        System.out.println("👤 Registro administrativo para: " + nombre + " " + apellido);
        
        // Crear el usuario según el rol
        Usuario usuario;
        
        switch (rolPrincipal.toUpperCase()) {
            case "ALUMNO":
                Alumno alumno = new Alumno();
                alumno.setColegioEgreso(colegioEgreso);
                alumno.setAñoEgreso(añoEgreso);
                alumno.setUltimosEstudios(ultimosEstudios);
                usuario = alumno;
                break;
                
            case "DOCENTE":
                Docente docente = new Docente();
                docente.setMatricula(matricula);
                docente.setAñosExperiencia(experiencia);
                usuario = docente;
                break;
                
            case "ADMIN":
            case "COORDINADOR":
                usuario = new Usuario();
                break;
                
            default:
                throw new RuntimeException("Rol no válido: " + rolPrincipal);
        }
        
        // Setear campos comunes
        usuario.setDni(dni);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setFechaNacimiento(fechaNacimiento);
        usuario.setGenero(genero);
        usuario.setCorreo(correo);
        usuario.setNumTelefono(telefono);
        
        // ✅ NO establecer contraseña - se generará automáticamente
        usuario.setContraseña(null);
        
        Usuario usuarioGuardado = registrarUsuario(usuario, paisCodigo, provinciaCodigo, ciudadId, rolPrincipal, true);
        
        // ✅ GUARDAR HORARIOS SI ES DOCENTE
        if ("DOCENTE".equals(rolPrincipal) && horarios != null && !horarios.isEmpty()) {
            guardarHorariosDocente((Docente) usuarioGuardado, horarios);
        }
        
        return usuarioGuardado;
    }

    public Usuario actualizarUsuarioAdministrativo(
            Usuario usuarioExistente,
            String nuevoDni,
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            TipoGenero genero,
            String correo,
            String telefono,
            String paisCodigo,
            String provinciaCodigo,
            Long ciudadId,
            String rolPrincipal,
            String matricula,
            Integer experiencia,
            String colegioEgreso,
            Integer añoEgreso,
            String ultimosEstudios,
            String horariosDisponibilidad,
            String estadoLiteral) {

        if (usuarioExistente == null) {
            throw new IllegalArgumentException("El usuario a actualizar no existe");
        }

        try {
            System.out.println("🔄 RegistroService.actualizarUsuarioAdministrativo - Iniciando actualización");
            System.out.println("📍 Ubicación a guardar: pais=" + paisCodigo + ", provincia=" + provinciaCodigo + ", ciudad=" + ciudadId);
            
            String rolActual = usuarioExistente.getRoles().stream()
                    .findFirst()
                    .map(Rol::getNombre)
                    .orElse("")
                    .toUpperCase();

            String rolDestino = (rolPrincipal != null && !rolPrincipal.isBlank())
                    ? rolPrincipal.toUpperCase()
                    : rolActual;

            System.out.println("👤 Rol: " + rolDestino);

            if (!rolActual.equalsIgnoreCase(rolDestino)) {
                throw new RuntimeException("No se permite cambiar el rol principal del usuario desde la edición");
            }

            if (!usuarioExistente.getDni().equals(nuevoDni) && usuarioRepository.existsByDni(nuevoDni)) {
                throw new RuntimeException("El DNI ingresado ya está registrado en otro usuario");
            }

            if (!usuarioExistente.getCorreo().equalsIgnoreCase(correo) && usuarioRepository.existsByCorreo(correo)) {
                throw new RuntimeException("El correo electrónico ingresado ya está registrado en otro usuario");
            }

            if (fechaNacimiento == null) {
                throw new RuntimeException("La fecha de nacimiento es obligatoria");
            }

            if (Period.between(fechaNacimiento, LocalDate.now()).getYears() < 16) {
                throw new RuntimeException("El usuario debe tener al menos 16 años");
            }

            Pais pais = buscarOCrearPais(paisCodigo);
            Provincia provincia = buscarOCrearProvincia(provinciaCodigo, pais);
            Ciudad ciudad = buscarOCrearCiudad(ciudadId, provincia, paisCodigo, provinciaCodigo);

            System.out.println("✅ Entidades de ubicación encontradas: pais=" + pais.getNombre() + ", provincia=" + provincia.getNombre() + ", ciudad=" + (ciudad != null ? ciudad.getNombre() : "Sin ciudad"));

            usuarioExistente.setDni(nuevoDni);
            usuarioExistente.setNombre(nombre);
            usuarioExistente.setApellido(apellido);
            usuarioExistente.setFechaNacimiento(fechaNacimiento);
            usuarioExistente.setGenero(genero);
            usuarioExistente.setCorreo(correo);

            String telefonoFinal = (telefono != null && !telefono.isBlank())
                    ? telefono
                    : usuarioExistente.getNumTelefono();
            usuarioExistente.setNumTelefono(telefonoFinal);

            usuarioExistente.setPais(pais);
            usuarioExistente.setProvincia(provincia);
            usuarioExistente.setCiudad(ciudad);

            System.out.println("✅ Ubicación asignada al usuario: pais=" + usuarioExistente.getPais().getNombre() + ", provincia=" + usuarioExistente.getProvincia().getNombre() + ", ciudad=" + (usuarioExistente.getCiudad() != null ? usuarioExistente.getCiudad().getNombre() : "Sin ciudad"));

            boolean estadoActivo = estadoLiteral == null || estadoLiteral.isBlank() || !"INACTIVO".equalsIgnoreCase(estadoLiteral);
            usuarioExistente.setEstado(estadoActivo);
            usuarioExistente.setEstadoCuenta(estadoActivo);

            asignarRoles(usuarioExistente, rolDestino);

            List<Map<String, String>> horariosList = new ArrayList<>();
            if ("DOCENTE".equalsIgnoreCase(rolDestino) && horariosDisponibilidad != null && !horariosDisponibilidad.isBlank()) {
                horariosList = objectMapper.readValue(
                        horariosDisponibilidad,
                        new TypeReference<List<Map<String, String>>>() {}
                );
            }

            switch (rolDestino) {
                case "ALUMNO":
                    if (!(usuarioExistente instanceof Alumno)) {
                        throw new RuntimeException("El usuario seleccionado no es un alumno");
                    }
                    Alumno alumno = (Alumno) usuarioExistente;
                    alumno.setColegioEgreso(colegioEgreso);
                    alumno.setAñoEgreso(añoEgreso);
                    alumno.setUltimosEstudios(ultimosEstudios);
                    Usuario alumnoActualizado = alumnoRepository.save(alumno);
                    return alumnoActualizado;

                case "DOCENTE":
                    if (!(usuarioExistente instanceof Docente)) {
                        throw new RuntimeException("El usuario seleccionado no es un docente");
                    }
                    Docente docente = (Docente) usuarioExistente;
                    System.out.println("👨‍🏫 Guardando datos de docente: matricula=" + matricula + ", experiencia=" + experiencia);
                    docente.setMatricula(matricula);
                    docente.setAñosExperiencia(experiencia);
                    Docente docenteGuardado = docenteRepository.save(docente);
                    System.out.println("✅ Docente guardado. Actualizando horarios...");
                    actualizarHorariosDocente(docenteGuardado, horariosList);
                    System.out.println("✅ Horarios actualizados. Total horarios: " + horariosList.size());
                    return docenteGuardado;

                default:
                    Usuario usuarioActualizado = usuarioRepository.save(usuarioExistente);
                    return usuarioActualizado;
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar usuario: " + e.getMessage(), e);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void eliminarUsuarioAdministrativo(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario a eliminar no existe");
        }

        try {
            // ✅ VALIDACIÓN: Alumno con inscripciones activas
            if (usuario instanceof Alumno) {
                // Ya no bloqueamos la eliminación de alumnos con inscripciones activas
                // Se asume que el frontend ya mostró la advertencia requerida y el usuario confirmó.
                
                // Opcional: Podríamos cancelar las inscripciones aquí si se desea lógica de limpieza,
                // pero "baja lógica" suele mantener el histórico.
            }

            // ✅ VALIDACIÓN: Docente con cursos activos
            if (usuario instanceof Docente) {
                // Usamos lista y filtro en memoria para mayor seguridad
                List<Curso> cursos = cursoRepository.findByDocentesId(usuario.getId());
                
                for (Curso curso : cursos) {
                    if (curso.getEstado() == EstadoOferta.ACTIVA || curso.getEstado() == EstadoOferta.ENCURSO) {
                        // REGLA DE NEGOCIO ESTRICTA: No se puede dar de baja si está asociado a un curso activo
                        throw new IllegalStateException("El docente tiene cursos activos asignados ('" + curso.getNombre() + "'). No se puede dar de baja mientras el curso esté en curso o activo.");
                        
                        /* Lógica anterior (más permisiva) removida por requerimiento estricto
                        if (curso.getDocentes().size() <= 1) {
                            throw new IllegalStateException(...);
                        } else {
                             curso.getDocentes().remove(usuario);
                             ...
                        }
                        */
                    }
                }
            }

            // ✅ BAJA LÓGICA (No eliminar físicamente)
            usuario.setEstado(false); // false = INACTIVO / BAJA
            usuarioRepository.save(usuario);
            
            System.out.println("✅ Usuario dado de baja lógicamente: " + usuario.getDni());
            
        } catch (Exception e) {
            throw e;
        }
    }

    private void guardarHorariosDocente(Docente docente, List<Map<String, String>> horarios) {
        try {
            System.out.println("📅 Guardando " + horarios.size() + " horarios de disponibilidad para docente ID: " + docente.getId());
            
            // Usar el nuevo servicio de disponibilidad
            disponibilidadDocenteService.actualizarDisponibilidades(docente, horarios);
            
            System.out.println("🎯 Total de " + horarios.size() + " disponibilidades guardadas exitosamente");

        } catch (Exception e) {
            System.out.println("❌ Error guardando disponibilidades: " + e.getMessage());
            e.printStackTrace();
            // No lanzar excepción para no interrumpir el registro del usuario
        }
    }

    private void actualizarHorariosDocente(Docente docente, List<Map<String, String>> horarios) {
        if (docente == null) {
            return;
        }

        // Eliminar disponibilidades anteriores y guardar las nuevas
        disponibilidadDocenteRepository.deleteByDocente(docente);

        if (horarios != null && !horarios.isEmpty()) {
            guardarHorariosDocente(docente, horarios);
        }
    }

    // 🔧 MÉTODOS AUXILIARES (se mantienen igual)
    private void asignarRoles(Usuario usuario, String rolPrincipal) {
        Rol rol = rolRepository.findByNombre(rolPrincipal.toUpperCase())
        .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolPrincipal));
    
        usuario.getRoles().clear();
        usuario.getRoles().add(rol);
    }

    private Usuario guardarAlumno(Alumno alumno) {
        return alumnoRepository.save(Objects.requireNonNull(alumno, "alumno no puede ser nulo"));
    }

    private Usuario guardarDocente(Docente docente) {
        return docenteRepository.save(Objects.requireNonNull(docente, "docente no puede ser nulo"));
    }

    private Usuario guardarUsuarioBase(Usuario usuario) {
        return usuarioRepository.save(Objects.requireNonNull(usuario, "usuario no puede ser nulo"));
    }

    @org.springframework.transaction.annotation.Transactional
    public void reactivarUsuarioAdministrativo(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario a reactivar no existe");
        }
        usuario.setEstado(true);
        usuarioRepository.save(usuario);
        System.out.println("✅ Usuario reactivado: " + usuario.getDni());
    }
}
