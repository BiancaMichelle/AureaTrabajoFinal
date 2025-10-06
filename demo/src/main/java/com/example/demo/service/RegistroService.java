package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Ciudad;
import com.example.demo.model.Docente;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;

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
    private final LocacionAPIService locacionApiService; // ✅ AGREGAR ESTO

    public RegistroService(UsuarioRepository usuarioRepository,
                          AlumnoRepository alumnoRepository,
                          DocenteRepository docenteRepository,
                          PaisRepository paisRepository,
                          ProvinciaRepository provinciaRepository,
                          CiudadRepository ciudadRepository,
                          PasswordEncoder passwordEncoder,
                          RolRepository rolRepository,
                          EmailService emailService,
                          LocacionAPIService locacionApiService) { // ✅ AGREGAR ESTE PARÁMETRO
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.docenteRepository = docenteRepository;
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.emailService = emailService; 
        this.locacionApiService = locacionApiService; // ✅ INICIALIZAR
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
            String subject = "Bienvenido a Espacio Virtual ICEP - Sus Credenciales de Acceso";
            String body = String.format(
                "Estimado/a %s,\n\n" +
                "Le damos la bienvenida a Espacio Virtual ICEP.\n\n" +
                "Sus credenciales de acceso son:\n" +
                "Correo electrónico: %s\n" +
                "Contraseña temporal: %s\n" +
                "Rol: %s\n\n" +
                "Por su seguridad, le recomendamos cambiar su contraseña después del primer acceso.\n\n" +
                "Puede acceder al sistema en: http://localhost:8080/login\n\n" +
                "Saludos cordiales,\n" +
                "Equipo Espacio Virtual ICEP",
                nombre, correo, contraseña, rol
            );
            
            emailService.sendEmail(correo, subject, body);
            System.out.println("✅ Email enviado a: " + correo);
            
        } catch (Exception e) {
            System.out.println("❌ Error enviando email: " + e.getMessage());
        }
    }

    // 🌍 MÉTODOS PARA BUSCAR O CREAR UBICACIONES (NUEVOS - DE LA SEGUNDA VERSIÓN)
    private Pais buscarOCrearPais(String paisCodigo) {
        Optional<Pais> paisExistente = paisRepository.findByCodigo(paisCodigo);
        if (paisExistente.isPresent()) {
            System.out.println("✅ País encontrado: " + paisExistente.get().getNombre());
            return paisExistente.get();
        } else {
            System.out.println("🌎 Creando nuevo país: " + paisCodigo);
            try {
                List<Pais> paises = locacionApiService.obtenerTodosPaises();
                for (Pais p : paises) {
                    if (paisCodigo.equals(p.getCodigo())) {
                        // ✅ CREAR NUEVA INSTANCIA en lugar de usar la de la API
                        Pais nuevoPais = new Pais();
                        nuevoPais.setCodigo(p.getCodigo());
                        nuevoPais.setNombre(p.getNombre());
                        nuevoPais = paisRepository.save(nuevoPais);
                        System.out.println("✅ País creado desde API: " + nuevoPais.getNombre());
                        return nuevoPais;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Error obteniendo países de API: " + e.getMessage());
            }
            
            // Fallback
            Pais nuevoPais = new Pais();
            nuevoPais.setCodigo(paisCodigo);
            nuevoPais.setNombre("País " + paisCodigo);
            nuevoPais = paisRepository.save(nuevoPais);
            System.out.println("✅ País creado (fallback): " + nuevoPais.getNombre());
            return nuevoPais;
        }
    }
    
    private Provincia buscarOCrearProvincia(String provinciaCodigo, Pais pais) {
        Optional<Provincia> provinciaExistente = provinciaRepository.findByCodigo(provinciaCodigo);
        if (provinciaExistente.isPresent()) {
            System.out.println("✅ Provincia encontrada: " + provinciaExistente.get().getNombre());
            return provinciaExistente.get();
        } else {
            System.out.println("🏙️ Creando nueva provincia: " + provinciaCodigo);
            try {
                List<Provincia> provincias = locacionApiService.obtenerProvinciasPorPais(pais.getCodigo());
                for (Provincia p : provincias) {
                    if (provinciaCodigo.equals(p.getCodigo())) {
                        // ✅ CREAR NUEVA INSTANCIA
                        Provincia nuevaProvincia = new Provincia();
                        nuevaProvincia.setCodigo(p.getCodigo());
                        nuevaProvincia.setNombre(p.getNombre());
                        nuevaProvincia.setPais(pais); // Usar el pais de la transacción actual
                        nuevaProvincia = provinciaRepository.save(nuevaProvincia);
                        System.out.println("✅ Provincia creada desde API: " + nuevaProvincia.getNombre());
                        return nuevaProvincia;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Error obteniendo provincias de API: " + e.getMessage());
            }
            
            // Fallback
            Provincia nuevaProvincia = new Provincia();
            nuevaProvincia.setCodigo(provinciaCodigo);
            nuevaProvincia.setNombre("Provincia " + provinciaCodigo);
            nuevaProvincia.setPais(pais);
            nuevaProvincia = provinciaRepository.save(nuevaProvincia);
            System.out.println("✅ Provincia creada (fallback): " + nuevaProvincia.getNombre());
            return nuevaProvincia;
        }
    }
    
    private Ciudad buscarOCrearCiudad(Long ciudadId, Provincia provincia, String paisCodigo, String provinciaCodigo) {
        Optional<Ciudad> ciudadExistente = ciudadRepository.findById(ciudadId);
        if (ciudadExistente.isPresent()) {
            System.out.println("✅ Ciudad encontrada: " + ciudadExistente.get().getNombre());
            return ciudadExistente.get();
        } else {
            System.out.println("🏡 Creando nueva ciudad: " + ciudadId);
            try {
                List<Ciudad> ciudades = locacionApiService.obtenerCiudadesPorProvincia(paisCodigo, provinciaCodigo);
                for (Ciudad c : ciudades) {
                    if (ciudadId.equals(c.getId())) {
                        // ✅ CREAR NUEVA INSTANCIA
                        Ciudad nuevaCiudad = new Ciudad();
                        nuevaCiudad.setId(c.getId());
                        nuevaCiudad.setNombre(c.getNombre());
                        nuevaCiudad.setProvincia(provincia); // Usar la provincia de la transacción actual
                        nuevaCiudad = ciudadRepository.save(nuevaCiudad);
                        System.out.println("✅ Ciudad creada desde API: " + nuevaCiudad.getNombre());
                        return nuevaCiudad;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Error obteniendo ciudades de API: " + e.getMessage());
            }
            
            // Fallback
            Ciudad nuevaCiudad = new Ciudad();
            nuevaCiudad.setId(ciudadId);
            nuevaCiudad.setNombre("Ciudad " + ciudadId);
            nuevaCiudad.setProvincia(provincia);
            nuevaCiudad = ciudadRepository.save(nuevaCiudad);
            System.out.println("✅ Ciudad creada (fallback): " + nuevaCiudad.getNombre());
            return nuevaCiudad;
        }
    }

    // 👤 MÉTODO UNIFICADO PRINCIPAL (MEJORADO CON BUSCAR O CREAR)
    public Usuario registrarUsuario(Usuario usuario, 
                                   String paisCodigo, 
                                   String provinciaCodigo, 
                                   Long ciudadId, 
                                   String rolPrincipal) {
        
        System.out.println("🔍 Iniciando registro para: " + usuario.getNombre());
        System.out.println("🎯 Rol asignado: " + rolPrincipal);
        
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
            System.out.println("   - Ciudad: " + ciudad.getNombre());

            // 5. GENERAR CONTRASEÑA ALEATORIA si no se proporciona una
            String contraseñaPlana;
            if (usuario.getContraseña() == null || usuario.getContraseña().trim().isEmpty()) {
                contraseñaPlana = generarContraseñaAleatoria();
                System.out.println("🔑 Contraseña generada: " + contraseñaPlana);
            } else {
                contraseñaPlana = usuario.getContraseña();
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

            // ✅ 10. ENVIAR CREDENCIALES POR EMAIL
            enviarCredencialesPorEmail(
                usuario.getCorreo(), 
                usuario.getNombre() + " " + usuario.getApellido(),
                contraseñaPlana,
                rolPrincipal
            );

            System.out.println("✅ Registro completado. ID: " + usuarioGuardado.getId() + " - Rol: " + rolPrincipal);
            return usuarioGuardado;
            
        } catch (Exception e) {
            System.out.println("❌ Error en registro: " + e.getMessage());
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }

    // 👨‍🎓 MÉTODO PARA REGISTRO PÚBLICO DE ALUMNOS (se mantiene)
    public void registrarUsuario(Alumno alumno, String paisCodigo, String provinciaCodigo, Long ciudadId) {
        registrarUsuario(alumno, paisCodigo, provinciaCodigo, ciudadId, "ALUMNO");
    }

    // 👨‍💼 MÉTODO PARA REGISTRO ADMINISTRATIVO (CORREGIDO)
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
            String ultimosEstudios) {
        
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
        
        return registrarUsuario(usuario, paisCodigo, provinciaCodigo, ciudadId, rolPrincipal);
    }

    // 🔧 MÉTODOS AUXILIARES (se mantienen igual)
    private void asignarRoles(Usuario usuario, String rolPrincipal) {
        Rol rol = rolRepository.findByNombre(rolPrincipal.toUpperCase())
        .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolPrincipal));
    
        usuario.getRoles().clear();
        usuario.getRoles().add(rol);
    }

    private Usuario guardarAlumno(Alumno alumno) {
        return alumnoRepository.save(alumno);
    }

    private Usuario guardarDocente(Docente docente) {
        return docenteRepository.save(docente);
    }

    private Usuario guardarUsuarioBase(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }
}