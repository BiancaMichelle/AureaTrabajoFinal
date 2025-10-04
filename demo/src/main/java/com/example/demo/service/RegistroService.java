package com.example.demo.service;

import java.time.LocalDate;

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

    public RegistroService(UsuarioRepository usuarioRepository,
                          AlumnoRepository alumnoRepository,
                          DocenteRepository docenteRepository,
                          PaisRepository paisRepository,
                          ProvinciaRepository provinciaRepository,
                          CiudadRepository ciudadRepository,
                          PasswordEncoder passwordEncoder,
                          RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.docenteRepository = docenteRepository;
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
    }

    /**
     * Método unificado para registrar cualquier tipo de usuario
     */
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

            // 3. BUSCAR UBICACIONES
            Pais pais = paisRepository.findByCodigo(paisCodigo)
                .orElseThrow(() -> new RuntimeException("País no encontrado. Completa el paso de ubicación nuevamente."));
            
            Provincia provincia = provinciaRepository.findByCodigo(provinciaCodigo)
                .orElseThrow(() -> new RuntimeException("Provincia no encontrada. Completa el paso de ubicación nuevamente."));
            
            Ciudad ciudad = ciudadRepository.findById(ciudadId)
                .orElseThrow(() -> new RuntimeException("Ciudad no encontrada. Completa el paso de ubicación nuevamente."));

            // 4. ASIGNAR UBICACIONES AL USUARIO
            usuario.setPais(pais);
            usuario.setProvincia(provincia);
            usuario.setCiudad(ciudad);

            System.out.println("📍 Ubicaciones asignadas:");
            System.out.println("   - País: " + pais.getNombre());
            System.out.println("   - Provincia: " + provincia.getNombre());
            System.out.println("   - Ciudad: " + ciudad.getNombre());

            // 5. Encriptar contraseña
            usuario.setContraseña(passwordEncoder.encode(usuario.getContraseña()));

            // 6. Establecer estado por defecto
            usuario.setEstado(true);
            usuario.setEstadoCuenta(true);

            // 7. ASIGNAR ROLES según el tipo de usuario
            asignarRoles(usuario, rolPrincipal);

            // 8. Guardar según el tipo de usuario
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

            System.out.println("✅ Registro completado. ID: " + usuarioGuardado.getId() + " - Rol: " + rolPrincipal);
            return usuarioGuardado;
            
        } catch (Exception e) {
            System.out.println("❌ Error en registro: " + e.getMessage());
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Método específico para registro público de alumnos (mantener compatibilidad)
     */
    public void registrarUsuario(Alumno alumno, String paisCodigo, String provinciaCodigo, Long ciudadId) {
        registrarUsuario(alumno, paisCodigo, provinciaCodigo, ciudadId, "ALUMNO");
    }

    /**
     * Método para registro administrativo con todos los parámetros
     */
    public Usuario registrarUsuarioAdministrativo(
            String dni,
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            TipoGenero genero,
            String correo,
            String telefono,
            String contraseña,
            String paisCodigo,
            String provinciaCodigo,
            Long ciudadId,
            String rolPrincipal,
            String matricula,
            Integer experiencia,
            String colegioEgreso,
            Integer añoEgreso,
            String ultimosEstudios) {
        
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
        usuario.setContraseña(contraseña);

        
        return registrarUsuario(usuario, paisCodigo, provinciaCodigo, ciudadId, rolPrincipal);
    }

    // Métodos auxiliares privados
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