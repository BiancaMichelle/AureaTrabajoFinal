package com.example.demo;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Docente;
import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@SpringBootApplication
@EnableScheduling
public class DemoApplication implements CommandLineRunner {

    private final RolRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CursoRepository cursoRepository;
    private final ModuloRepository moduloRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CategoriaRepository categoriaRepository;
    private final DocenteRepository docenteRepository;
    private final AlumnoRepository alumnoRepository;

    public DemoApplication(RolRepository roleRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            CursoRepository cursoRepository,
            ModuloRepository moduloRepository,
            InscripcionRepository inscripcionRepository,
            CategoriaRepository categoriaRepository,
            DocenteRepository docenteRepository,
            AlumnoRepository alumnoRepository) {
        this.roleRepository = roleRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.cursoRepository = cursoRepository;
        this.moduloRepository = moduloRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.categoriaRepository = categoriaRepository;
        this.docenteRepository = docenteRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        crearRolesYUsuarios();

    }

    private void crearRolesYUsuarios() {
        // Crear roles si no existen
        for (String name : List.of("ADMIN", "ALUMNO", "DOCENTE")) {
            if (roleRepository.findByNombre(name).isEmpty()) {
                Rol rol = new Rol();
                rol.setNombre(name);
                rol.setDescripcion(name);
                roleRepository.save(rol);
            }
        }

        // Crear admin si no existe
        if (usuarioRepository.findByDni("11111111").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setDni("11111111");
            admin.setNombre("Super");
            admin.setApellido("Admin");
            admin.setFechaNacimiento(LocalDate.of(1990, 1, 1));
            admin.setGenero(TipoGenero.MASCULINO);
            admin.setCorreo("admin@demo.com");
            admin.setNumTelefono("1234567890");
            admin.setContraseña(passwordEncoder.encode("123"));
            admin.setEstado(true);
            admin.setEstadoCuenta(true);

            // ✅ IMPORTANTE: Crear una NUEVA colección para cada usuario
            admin.setRoles(new HashSet<>());

            Rol rolAdmin = roleRepository.findByNombre("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));
            admin.getRoles().add(rolAdmin);

            usuarioRepository.save(admin);
        }

        // ✅ CREAR DOCENTE CON DNI 12345678
        crearUsuarioDocente("12345678", "Roberto", "García", "roberto.garcia@gmail.com");

        // Crear alumnos de prueba
        crearUsuarioAlumno("22222222", "Juan", "Pérez", "juan@gmail.com");
        crearUsuarioAlumno("33333333", "María", "Gómez", "maria@gmail.com");
        crearUsuarioAlumno("44444444", "Carlos", "López", "carlos@gmail.com");

        // Crear otro docente de prueba
        crearUsuarioDocente("55555555", "Ana", "Martínez", "ana@gmail.com");
    }

    private void crearUsuarioAlumno(String dni, String nombre, String apellido, String email) {
        if (usuarioRepository.findByDni(dni).isEmpty()) {
            // ✅ Cambiar de Usuario a Alumno
            Alumno alumno = new Alumno();
            alumno.setDni(dni);
            alumno.setNombre(nombre);
            alumno.setApellido(apellido);
            alumno.setFechaNacimiento(LocalDate.of(2000, 1, 1));
            alumno.setGenero(TipoGenero.MASCULINO);
            alumno.setCorreo(email);
            alumno.setNumTelefono("1234567890");
            alumno.setContraseña(passwordEncoder.encode("123"));
            alumno.setEstado(true);
            alumno.setEstadoCuenta(true);

            // ✅ Campos específicos de Alumno
            alumno.setColegioEgreso("Colegio Nacional");
            alumno.setAñoEgreso(2020);
            alumno.setUltimosEstudios("Secundario Completo");

            // ✅ IMPORTANTE: Crear una NUEVA colección para cada usuario
            alumno.setRoles(new HashSet<>());

            Rol rolAlumno = roleRepository.findByNombre("ALUMNO")
                    .orElseThrow(() -> new RuntimeException("Rol ALUMNO no encontrado"));
            alumno.getRoles().add(rolAlumno);

            // ✅ Guardar como Alumno (esto creará registros en ambas tablas)
            alumnoRepository.save(alumno);
            System.out.println("✅ Alumno creado: " + nombre + " " + apellido + " (DNI: " + dni + ")");
        }
    }

    private void crearUsuarioDocente(String dni, String nombre, String apellido, String email) {
        if (usuarioRepository.findByDni(dni).isEmpty()) {
            // ✅ Cambiar de Usuario a Docente
            Docente docente = new Docente();
            docente.setDni(dni);
            docente.setNombre(nombre);
            docente.setApellido(apellido);
            docente.setFechaNacimiento(LocalDate.of(1985, 1, 1));
            docente.setGenero(TipoGenero.MASCULINO);
            docente.setCorreo(email);
            docente.setNumTelefono("1234567890");
            docente.setContraseña(passwordEncoder.encode("123"));
            docente.setEstado(true);
            docente.setEstadoCuenta(true);

            // ✅ IMPORTANTE: Crear una NUEVA colección para cada usuario
            docente.setRoles(new HashSet<>());

            Rol rolDocente = roleRepository.findByNombre("DOCENTE")
                    .orElseThrow(() -> new RuntimeException("Rol DOCENTE no encontrado"));
            docente.getRoles().add(rolDocente);

            // ✅ Guardar como Docente (esto creará registros en ambas tablas)
            docenteRepository.save(docente);
            System.out.println("✅ Docente creado: " + nombre + " " + apellido + " (DNI: " + dni + ")");
            System.out.println("✅ Registro Docente creado en tabla docente: " + nombre + " " + apellido);
        }
    }
}