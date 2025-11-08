package com.example.demo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;
import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Categoria;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Instituto;
import com.example.demo.model.Modulo;
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
    crearInstitutoYCategorias();
    crearCursosYContenido();
    crearInscripciones();
    // asignarDocenteACursos();  // ✅ COMENTA ESTA LÍNEA
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
    crearUsuarioDocente("12345678", "Roberto", "García", "roberto.garcia@demo.com");

    // Crear alumnos de prueba
    crearUsuarioAlumno("22222222", "Juan", "Pérez", "juan@demo.com");
    crearUsuarioAlumno("33333333", "María", "Gómez", "maria@demo.com");
    crearUsuarioAlumno("44444444", "Carlos", "López", "carlos@demo.com");

    // Crear otro docente de prueba
    crearUsuarioDocente("55555555", "Ana", "Martínez", "ana@demo.com");
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

    private void crearInstitutoYCategorias() {

        // Crear categorías si no existen
        if (categoriaRepository.count() == 0) {
            List<String> nombresCategorias = List.of(
                "Programación", "Diseño", "Negocios", "Idiomas", 
                "Ciencias", "Arte", "Tecnología", "Desarrollo Personal"
            );
            
            for (String nombre : nombresCategorias) {
                Categoria categoria = new Categoria();
                categoria.setNombre(nombre);
                categoria.setDescripcion("Cursos de " + nombre);
                categoriaRepository.save(categoria);
            }
            System.out.println("✅ " + nombresCategorias.size() + " categorías creadas");
        }
    }

    private void crearCursosYContenido() {
        // Crear cursos si no existen
        if (cursoRepository.count() == 0) {
            // ✅ ELIMINAR la dependencia del instituto
            List<Categoria> categorias = categoriaRepository.findAll();
            
            // ✅ CREAR CURSOS SIN INSTITUTO Y SIN DOCENTES INICIALMENTE
            List<Usuario> usuariosVacios = new ArrayList<>();
            
            // Curso 1: Programación Java
            Curso cursoJava = crearCurso(
                "Programación Java desde Cero",
                "Aprende Java desde los fundamentos hasta conceptos avanzados de programación orientada a objetos",
                "3 meses",
                5000.0,
                Modalidad.VIRTUAL,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "Dominar los conceptos fundamentales de Java y POO",
                true,
                EstadoOferta.ACTIVA,
                50,
                true,
                null, // ✅ INSTITUTO NULL
                categorias.subList(0, Math.min(2, categorias.size())), // ✅ EVITAR IndexOutOfBounds
                usuariosVacios,
                "Temario completo de Java: sintaxis, POO, colecciones, excepciones, etc.",
                1500.0,
                300.0,
                3,
                10
            );
    
            // Curso 2: Spring Boot
            Curso cursoSpring = crearCurso(
                "Spring Boot y Microservicios",
                "Desarrollo de aplicaciones modernas con Spring Boot y arquitectura de microservicios",
                "2 meses",
                7500.0,
                Modalidad.VIRTUAL,
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                "Crear aplicaciones empresariales con Spring Boot",
                true,
                EstadoOferta.ACTIVA,
                30,
                true,
                null, // ✅ INSTITUTO NULL
                categorias.subList(0, Math.min(1, categorias.size())), // ✅ EVITAR IndexOutOfBounds
                usuariosVacios,
                "Spring Boot, Spring Data, Spring Security, Microservicios, Docker",
                2500.0,
                500.0,
                3,
                15
            );
    
            // Curso 3: Diseño UX/UI
            Curso cursoDiseño = crearCurso(
                "Diseño UX/UI Avanzado",
                "Aprende diseño de experiencias de usuario e interfaces modernas",
                "4 meses",
                6000.0,
                Modalidad.VIRTUAL,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusMonths(4),
                "Diseñar interfaces centradas en el usuario",
                true,
                EstadoOferta.ACTIVA,
                25,
                true,
                null, // ✅ INSTITUTO NULL
                categorias.subList(1, Math.min(3, categorias.size())), // ✅ EVITAR IndexOutOfBounds
                usuariosVacios,
                "Research, Wireframes, Prototipado, Testing de usabilidad",
                2000.0,
                400.0,
                4,
                5
            );
    
            // ✅ SOLO CREAR MÓDULOS SIN CLASES
            crearModulosParaCurso(cursoJava);
            crearModulosParaCurso(cursoSpring);
            crearModulosParaCurso(cursoDiseño);
    
            System.out.println("✅ " + 3 + " cursos creados exitosamente con sus módulos");
        }
    }
    
    // ✅ NUEVO MÉTODO SIMPLIFICADO: SOLO CREA MÓDULOS
    private void crearModulosParaCurso(Curso curso) {
        System.out.println("🔄 Creando módulos para curso: " + curso.getNombre() + " (ID: " + curso.getIdOferta() + ")");
        
        // Módulo 1: Fundamentos
        crearModulo(
            "Módulo 1: Fundamentos de " + curso.getNombre(),
            "Conceptos básicos y fundamentos esenciales del curso " + curso.getNombre(),
            LocalDate.now(),  // Ahora LocalDate
            LocalDate.now().plusDays(30),
            "Comprender los conceptos fundamentales y bases teóricas de " + curso.getNombre(),
            true,
            curso
        );
        
        // Módulo 2: Contenido Intermedio
        crearModulo(
            "Módulo 2: Contenido Intermedio de " + curso.getNombre(), 
            "Profundización en temas específicos y aplicaciones prácticas de " + curso.getNombre(),
            LocalDate.now(),  // Ahora LocalDate
            LocalDate.now().plusDays(30),
            "Aplicar conocimientos en situaciones reales y casos prácticos de " + curso.getNombre(),
            true,
            curso
        );
        
        // Módulo 3: Nivel Avanzado
        crearModulo(
            "Módulo 3: Nivel Avanzado de " + curso.getNombre(),
            "Temas complejos y especializados del área de estudio de " + curso.getNombre(),
            LocalDate.now(),  // Ahora LocalDate
            LocalDate.now().plusDays(30),
            "Dominar conceptos avanzados y técnicas especializadas de " + curso.getNombre(),
            true,
            curso
        );
        
        // Módulo 4: Proyecto Final
        crearModulo(
            "Módulo 4: Proyecto Final de " + curso.getNombre(),
            "Desarrollo del proyecto integrador que aplica todos los conocimientos de " + curso.getNombre(),
            LocalDate.now(),  // Ahora LocalDate
            LocalDate.now().plusDays(30),
            "Integrar y aplicar todos los conocimientos en un proyecto real de " + curso.getNombre(),
            true,
            curso
        );
    
        // Verificar que los módulos se guardaron correctamente
        List<Modulo> modulosGuardados = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(curso);
        System.out.println("📊 Módulos guardados en BD para " + curso.getNombre() + ": " + modulosGuardados.size());
        
        for (Modulo modulo : modulosGuardados) {
            System.out.println("   - " + modulo.getNombre() + " (ID: " + modulo.getIdModulo() + ")");
        }
        
        System.out.println("🎯 4 módulos creados para: " + curso.getNombre());
    }
    

    private Curso crearCurso(String nombre, String descripcion, String duracion, 
                       Double costoInscripcion, Modalidad modalidad, 
                       LocalDate fechaInicio, LocalDate fechaFin, String objetivo,
                       Boolean certificado, EstadoOferta estado, Integer cupos,
                       Boolean visibilidad, Instituto instituto, List<Categoria> categorias,
                       List<Usuario> docentes, String temario, Double costoCuota,
                       Double costoMora, Integer nrCuotas, Integer diaVencimiento) {
    
    Curso curso = new Curso();
    
    // Campos de OfertaAcademica
    curso.setNombre(nombre);
    curso.setDescripcion(descripcion);
    curso.setDuracion(duracion);
    curso.setCostoInscripcion(costoInscripcion);
    curso.setModalidad(modalidad);
    curso.setFechaInicio(fechaInicio);
    curso.setFechaFin(fechaFin);
    curso.setObjetivo(objetivo);
    curso.setCertificado(certificado);
    curso.setEstado(estado);
    curso.setCupos(cupos);
    curso.setVisibilidad(visibilidad);
    curso.setInstituto(instituto); // ✅ PUEDE SER NULL
    curso.setCategorias(categorias);
    curso.setInscripciones(new ArrayList<>());
    
    // Campos específicos de Curso
    curso.setTemario(temario);
    curso.setDocentes(convertirUsuariosADocentes(docentes));
    curso.setRequisitos(new ArrayList<>());
    curso.setCostoCuota(costoCuota);
    curso.setCostoMora(costoMora);
    curso.setNrCuotas(nrCuotas);
    curso.setDiaVencimiento(diaVencimiento);
    curso.setModulos(new ArrayList<>());
    curso.setClases(new ArrayList<>());

    return cursoRepository.save(curso);
}

    private List<Docente> convertirUsuariosADocentes(List<Usuario> usuarios) {
        List<Docente> docentes = new ArrayList<>();
        
        for (Usuario usuario : usuarios) {
            // Buscar si ya existe un docente con este DNI
            Optional<Docente> docenteExistente = docenteRepository.findByDni(usuario.getDni());
            
            if (docenteExistente.isPresent()) {
                docentes.add(docenteExistente.get());
            } else {
                // Si no existe, crear nuevo docente
                Docente docente = new Docente();
                docente.setDni(usuario.getDni());
                docente.setNombre(usuario.getNombre());
                docente.setApellido(usuario.getApellido());
                docente.setCorreo(usuario.getCorreo());
                docente.setFechaNacimiento(usuario.getFechaNacimiento());
                docente.setGenero(usuario.getGenero());
                docente.setNumTelefono(usuario.getNumTelefono());
                docente.setEstado(true);
                docente.setEstadoCuenta(true);
                
                Docente docenteGuardado = docenteRepository.save(docente);
                docentes.add(docenteGuardado);
            }
        }
        
        return docentes;
    }

    private Modulo crearModulo(String nombre, String descripcion, LocalDate  fechaInicio, 
    LocalDate  fechaFin, String objetivos, Boolean visibilidad, Curso curso) {
        Modulo modulo = new Modulo();
        modulo.setNombre(nombre);
        modulo.setDescripcion(descripcion);
        modulo.setFechaInicioModulo(fechaInicio);
        modulo.setFechaFinModulo(fechaFin);
        modulo.setObjetivos(objetivos);
        modulo.setVisibilidad(visibilidad);
        modulo.setCurso(curso); // ✅ Establecer la relación con el curso
        modulo.setClases(new ArrayList<>());
        modulo.setActividades(new ArrayList<>());
        
        Modulo moduloGuardado = moduloRepository.save(modulo);
        System.out.println("💾 Módulo guardado: " + moduloGuardado.getNombre() + " para curso: " + curso.getNombre());
        
        return moduloGuardado;
    }


    private void crearInscripciones() {
        // ✅ Obtener ALUMNOS, no usuarios
        List<Alumno> alumnos = alumnoRepository.findAll();
        List<Curso> cursos = cursoRepository.findAll();
    
        System.out.println("👥 Creando inscripciones para " + alumnos.size() + " alumnos en " + cursos.size() + " cursos");
    
        for (Alumno alumno : alumnos) {
            for (Curso curso : cursos) {
                // ✅ Verificar si ya existe la inscripción para este ALUMNO
                if (inscripcionRepository.findByAlumnoAndOferta(alumno, curso).isEmpty()) {
                    Inscripciones inscripcion = new Inscripciones();
                    inscripcion.setAlumno(alumno); // ✅ Ahora es Alumno, no Usuario
                    inscripcion.setOferta(curso);
                    inscripcion.setFechaInscripcion(LocalDate.now());
                    inscripcion.setEstadoInscripcion(true);
                    inscripcion.setObservaciones("Inscripción automática - Datos de prueba");
                    inscripcion.setCuotas(new ArrayList<>());
                    
                    inscripcionRepository.save(inscripcion);
                    
                    System.out.println("✅ " + alumno.getNombre() + " inscrito en " + curso.getNombre());
                }
            }
        }
        
        System.out.println("🎉 " + (alumnos.size() * cursos.size()) + " inscripciones creadas exitosamente");
    }
}