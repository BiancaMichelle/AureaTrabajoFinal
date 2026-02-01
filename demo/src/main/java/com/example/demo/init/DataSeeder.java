package com.example.demo.init;

import java.time.LocalDate;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.enums.Dias;
import com.example.demo.enums.EstadoOferta;
import com.example.demo.enums.Modalidad;
import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Formacion;
import com.example.demo.model.Horario;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Instituto;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Rol;
import com.example.demo.model.Seminario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.CharlaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.FormacionRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.InstitutoRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.SeminarioRepository;
import com.example.demo.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RolRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CursoRepository cursoRepository;
    private final ModuloRepository moduloRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CategoriaRepository categoriaRepository;
    private final DocenteRepository docenteRepository;
    private final AlumnoRepository alumnoRepository;
    private final InstitutoRepository institutoRepository;
    private final CharlaRepository charlaRepository;
    private final SeminarioRepository seminarioRepository;
    private final FormacionRepository formacionRepository;
    private final com.example.demo.repository.OfertaAcademicaRepository ofertaAcademicaRepository;
 
    public DataSeeder(RolRepository roleRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            CursoRepository cursoRepository,
            ModuloRepository moduloRepository,
            InscripcionRepository inscripcionRepository,
            CategoriaRepository categoriaRepository,
            DocenteRepository docenteRepository,
            AlumnoRepository alumnoRepository,
            InstitutoRepository institutoRepository,
            CharlaRepository charlaRepository,
            SeminarioRepository seminarioRepository,
            FormacionRepository formacionRepository,
            com.example.demo.repository.OfertaAcademicaRepository ofertaAcademicaRepository) {
        this.roleRepository = roleRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.cursoRepository = cursoRepository;
        this.moduloRepository = moduloRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.categoriaRepository = categoriaRepository;
        this.docenteRepository = docenteRepository;
        this.alumnoRepository = alumnoRepository;
        this.institutoRepository = institutoRepository;
        this.charlaRepository = charlaRepository;
        this.seminarioRepository = seminarioRepository;
        this.formacionRepository = formacionRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        crearRolesYUsuarios();
        crearInstitutoDefault();
        seedMasUsuarios();
        seedOfertas();
        repararHorariosHuerfanos();
    }

    // Método para corregir ofertas existentes con horarios sin docente
    private void repararHorariosHuerfanos() {
        System.out.println("SEED: Verificando integridad de horarios...");
        List<OfertaAcademica> ofertas = new ArrayList<>();
        ofertas.addAll(cursoRepository.findAll());
        ofertas.addAll(formacionRepository.findAll());
        
        int fixedCount = 0;
        for (OfertaAcademica oferta : ofertas) {
             boolean isDirty = false;
             if (oferta.getHorarios() != null) {
                 for (Horario h : oferta.getHorarios()) {
                     if (h.getDocente() == null) {
                        List<Docente> docentes = null;
                        if (oferta instanceof Curso) docentes = ((Curso)oferta).getDocentes();
                        else if (oferta instanceof Formacion) docentes = ((Formacion)oferta).getDocentes();
                        
                        if (docentes != null && !docentes.isEmpty()) {
                            Docente d = docentes.get(0); // Asignar al primer docente
                            h.setDocente(d);
                            // Ensure bi-directional consistency if needed, though JPA handles via save
                            isDirty = true;
                        }
                     }
                 }
             }
             if (isDirty) {
                 if (oferta instanceof Curso) cursoRepository.save((Curso)oferta);
                 else if (oferta instanceof Formacion) formacionRepository.save((Formacion)oferta);
                 fixedCount++;
             }
        }
        if (fixedCount > 0) System.out.println("✅ Se repararon horarios en " + fixedCount + " ofertas.");
    }

    private void seedMasUsuarios() {
        // Asegurar 5 docentes (ya tenemos 2 creados en crearRolesYUsuarios: Roberto y Ana)
        System.out.println("SEED: Creando docentes adicionales...");
        // Usamos DNIs numéricos válidos (8 dígitos) para pasar la validación @Pattern(regexp = "\\d{7,8}")
        crearUsuarioDocente("10000003", "Laura", "Fernandez", "laura.fernandez@gmail.com");
        crearUsuarioDocente("10000004", "Miguel", "Rodriguez", "miguel.rodriguez@gmail.com");
        crearUsuarioDocente("10000005", "Sofia", "Ramirez", "sofia.ramirez@gmail.com");

        // Crear 30 alumnos (ya tenemos 3 creados: Juan, Maria, Carlos)
        System.out.println("SEED: Creando 30 alumnos...");
        for (int i = 1; i <= 30; i++) {
            // Generar DNI numérico de 8 dígitos: 60000001, 60000002...
            String dni = "6000" + String.format("%04d", i);
            String nombre = "Alumno" + i;
            String apellido = "Test" + i;
            String email = "alumno" + i + "@test.com";
            crearUsuarioAlumno(dni, nombre, apellido, email);
        }
    }

    private void seedOfertas() {
        System.out.println("SEED: Iniciando carga de Ofertas Académicas...");

        List<Docente> docentes = docenteRepository.findAll();
        if (docentes.isEmpty()) {
            System.err.println("SEED ERROR: No hay docentes para asignar a cursos.");
            return;
        }

        Random rand = new Random();

        // --- 3 CURSOS ---
        crearCursoSiNoExiste(new Curso(
            "Java Spring Avanzado", "Dominando Spring Boot 3", Modalidad.VIRTUAL, 15000.0, true, 
            LocalDate.now().minusDays(30), LocalDate.now().plusMonths(3), 30, 3, 500.0, 10,
            Arrays.asList(docentes.get(0), docentes.get(1))
        ));

        crearCursoSiNoExiste(new Curso(
            "Introducción a Python", "Fundamentos de programación en Python", Modalidad.VIRTUAL, 12000.0, true, 
            LocalDate.now().minusDays(15), LocalDate.now().plusMonths(3), 30, 3, 500.0, 10,
            Arrays.asList(docentes.get(rand.nextInt(docentes.size())))
        ));

        crearCursoSiNoExiste(new Curso(
            "Desarrollo Web Fullstack", "De cero a experto en HTML, CSS, JS y Java", Modalidad.HIBRIDA, 20000.0, true, 
             LocalDate.now().plusDays(10), LocalDate.now().plusMonths(3), 30, 3, 500.0, 10,
            Arrays.asList(docentes.get(0), docentes.get(2))
        ));

        // --- 2 FORMACIONES ---
        crearFormacionSiNoExiste(new Formacion(
            "Experto en Ciencia de Datos", "Formación completa en Data Science", "Data Engineer", Modalidad.VIRTUAL, 25000.0,
            LocalDate.now().plusDays(15), LocalDate.now().plusMonths(6), 20, 6, 600.0, 5,
            Arrays.asList(docentes.get(1), docentes.get(3))
        ));

        crearFormacionSiNoExiste(new Formacion(
            "Gestión de Proyectos IT", "Metodologías ágiles y tradicionales", "Project Manager", Modalidad.PRESENCIAL, 18000.0,
            LocalDate.now().plusDays(15), LocalDate.now().plusMonths(6), 20, 6, 600.0, 5,
            Arrays.asList(docentes.get(2))
        ));

        // --- 3 CHARLAS ---
        crearCharlaSiNoExiste(new Charla(
            "El Futuro de la IA", "Impacto de la IA generativa", "Público General", Modalidad.VIRTUAL,
            LocalDate.now().plusDays(5), "https://zoom.us/meet/123",
            Arrays.asList("Dr. Alan Turing", "Ada Lovelace")
        ));

        crearCharlaSiNoExiste(new Charla(
            "Ciberseguridad en 2026", "Nuevos desafíos y amenazas", "Estudiantes IT", Modalidad.VIRTUAL,
            LocalDate.now().plusDays(5), "https://zoom.us/meet/123",
            Arrays.asList("Kevin Mitnick")
        ));

        crearCharlaSiNoExiste(new Charla(
            "Bienestar Digital", "Equilibrio vida-trabajo", "Todo público", Modalidad.VIRTUAL,
            LocalDate.now().plusDays(5), "https://zoom.us/meet/123",
            Arrays.asList("Arianna Huffington")
        ));

        // --- 2 SEMINARIOS ---
        crearSeminarioSiNoExiste(new Seminario(
            "Arquitectura de Microservicios", "Patrones y antipatrones", "Arquitectos de Software", Modalidad.PRESENCIAL, 5000.0,
            LocalDate.now().plusDays(20), LocalDate.now().plusDays(21), "Auditorio Central", 120,
            Arrays.asList("Martin Fowler", "Robert C. Martin")
        ));

        crearSeminarioSiNoExiste(new Seminario(
            "Marketing para Desarrolladores", "Marca personal y visibilidad", "Freelancers", Modalidad.PRESENCIAL, 5000.0,
            LocalDate.now().plusDays(20), LocalDate.now().plusDays(21), "Auditorio Central", 90,
            Arrays.asList("Seth Godin")
        ));
        
        // --- Nuevo: Crear 2 Formaciones en curso con docente 12345678 y alumno 44444444 ---
        Docente docenteRoberto = docenteRepository.findAll().stream()
                .filter(d -> "12345678".equals(d.getDni()))
                .findFirst().orElse(null);
        // Fetch alumno directly via AlumnoRepository to avoid casting issues
        Alumno alumnoCarlos = alumnoRepository.findByDni("44444444").orElse(null);

        if (docenteRoberto == null) {
            System.err.println("SEED WARN: Docente 12345678 no encontrado. No se crean formaciones en curso.");
        } else {
            LocalDate inicio = LocalDate.now().minusMonths(1);
            LocalDate fin = LocalDate.now().plusMonths(3);

            // Nombres y parámetros
            String name1 = "Formación en Curso A";
            String name2 = "Formación en Curso B";

            // Helper: find existing exact (case-insensitive) using OfertaAcademicaRepository to avoid Optional import issues
            java.util.Optional<Formacion> optF1 = ofertaAcademicaRepository.findByNombreIgnoreCase(name1)
                    .filter(o -> o instanceof Formacion)
                    .map(o -> (Formacion) o);
            java.util.Optional<Formacion> optF2 = ofertaAcademicaRepository.findByNombreIgnoreCase(name2)
                    .filter(o -> o instanceof Formacion)
                    .map(o -> (Formacion) o);

            Formacion f1 = optF1.orElse(null);
            if (f1 == null) {
                f1 = new Formacion(
                    name1,
                    "Formación en curso A",
                    "Especialización A",
                    Modalidad.VIRTUAL,
                    15000.0,
                    inicio,
                    fin,
                    20,
                    4,
                    500.0,
                    5,
                    Arrays.asList(docenteRoberto)
                );
                f1.setEstado(EstadoOferta.ENCURSO);
                // horarios L-V 09:00 - 10:30
                for (Dias d : Arrays.asList(Dias.LUNES, Dias.MARTES, Dias.MIERCOLES, Dias.JUEVES, Dias.VIERNES)) {
                    Horario h = new Horario();
                    h.setDia(d);
                    h.setHoraInicio(Time.valueOf("09:00:00"));
                    h.setHoraFin(Time.valueOf("10:30:00"));
                    h.setDocente(docenteRoberto);
                    f1.addHorario(h);
                }
                formacionRepository.save(f1);
                System.out.println("✅ Formacion en curso creada: " + f1.getNombre());
            } else {
                System.out.println("ℹ️ Formacion ya existe: " + f1.getNombre());
            }

            // Ensure docente association and horarios for f1
            if (docenteRoberto != null) {
                if (f1.getDocentes() == null) f1.setDocentes(new ArrayList<>());
                boolean hasDoc = f1.getDocentes().stream().anyMatch(d -> d.getId().equals(docenteRoberto.getId()));
                if (!hasDoc) {
                    f1.getDocentes().add(docenteRoberto);
                    formacionRepository.save(f1);
                    System.out.println("✅ Docente 12345678 asociado a la formación: " + f1.getNombre());
                }
                // Ensure horario entries exist for weekdays
                if (f1.getHorarios() == null || f1.getHorarios().isEmpty()) {
                    for (Dias d : Arrays.asList(Dias.LUNES, Dias.MARTES, Dias.MIERCOLES, Dias.JUEVES, Dias.VIERNES)) {
                        Horario h = new Horario();
                        h.setDia(d);
                        h.setHoraInicio(Time.valueOf("09:00:00"));
                        h.setHoraFin(Time.valueOf("10:30:00"));
                        h.setDocente(docenteRoberto);
                        f1.addHorario(h);
                    }
                    formacionRepository.save(f1);
                    System.out.println("✅ Horarios agregados para: " + f1.getNombre());
                }

                // Ensure docente has this formacion in its list
                if (docenteRoberto.getFormaciones() == null) docenteRoberto.setFormaciones(new ArrayList<>());
                final String f1Nombre = f1.getNombre();
                boolean alreadyLinked1 = docenteRoberto.getFormaciones().stream()
                    .anyMatch(ff -> ff.getNombre().equalsIgnoreCase(f1Nombre));
                if (!alreadyLinked1) {
                    docenteRoberto.getFormaciones().add(f1);
                    docenteRepository.save(docenteRoberto);
                    System.out.println("✅ Docente 12345678 asociado a la formación (lado docente): " + f1.getNombre());
                }
            }

            Formacion f2 = optF2.orElse(null);
            if (f2 == null) {
                f2 = new Formacion(
                    name2,
                    "Formación en curso B",
                    "Especialización B",
                    Modalidad.VIRTUAL,
                    14000.0,
                    inicio,
                    fin,
                    20,
                    4,
                    500.0,
                    5,
                    Arrays.asList(docenteRoberto)
                );
                f2.setEstado(EstadoOferta.ENCURSO);
                for (Dias d : Arrays.asList(Dias.LUNES, Dias.MARTES, Dias.MIERCOLES, Dias.JUEVES, Dias.VIERNES)) {
                    Horario h = new Horario();
                    h.setDia(d);
                    h.setHoraInicio(Time.valueOf("09:00:00"));
                    h.setHoraFin(Time.valueOf("10:30:00"));
                    h.setDocente(docenteRoberto);
                    f2.addHorario(h);
                }
                formacionRepository.save(f2);
                System.out.println("✅ Formacion en curso creada: " + f2.getNombre());
            } else {
                System.out.println("ℹ️ Formacion ya existe: " + f2.getNombre());
            }

            // Ensure docente association and horarios for f2
            if (docenteRoberto != null) {
                if (f2.getDocentes() == null) f2.setDocentes(new ArrayList<>());
                boolean hasDoc2 = f2.getDocentes().stream().anyMatch(d -> d.getId().equals(docenteRoberto.getId()));
                if (!hasDoc2) {
                    f2.getDocentes().add(docenteRoberto);
                    formacionRepository.save(f2);
                    System.out.println("✅ Docente 12345678 asociado a la formación: " + f2.getNombre());
                }
                if (f2.getHorarios() == null || f2.getHorarios().isEmpty()) {
                    for (Dias d : Arrays.asList(Dias.LUNES, Dias.MARTES, Dias.MIERCOLES, Dias.JUEVES, Dias.VIERNES)) {
                        Horario h = new Horario();
                        h.setDia(d);
                        h.setHoraInicio(Time.valueOf("09:00:00"));
                        h.setHoraFin(Time.valueOf("10:30:00"));
                        h.setDocente(docenteRoberto);
                        f2.addHorario(h);
                    }
                    formacionRepository.save(f2);
                    System.out.println("✅ Horarios agregados para: " + f2.getNombre());
                }

                final String f2Nombre = f2.getNombre();
                boolean alreadyLinked2 = docenteRoberto.getFormaciones().stream()
                    .anyMatch(ff -> ff.getNombre().equalsIgnoreCase(f2Nombre));
                if (!alreadyLinked2) {
                    docenteRoberto.getFormaciones().add(f2);
                    docenteRepository.save(docenteRoberto);
                    System.out.println("✅ Docente 12345678 asociado a la formación (lado docente): " + f2.getNombre());
                }
            }

            // Inscribir alumno 44444444 en ambas formaciones
            if (alumnoCarlos != null) {
                // Evitar lambdas que capturan variables no-final: usar chequeo explícito
                if (inscripcionRepository.findByAlumnoAndOferta(alumnoCarlos, f1).isEmpty()) {
                    Inscripciones ins1 = new Inscripciones();
                    ins1.setAlumno(alumnoCarlos);
                    ins1.setOferta(f1);
                    ins1.setFechaInscripcion(LocalDate.now().minusDays(5));
                    ins1.setEstadoInscripcion(true);
                    inscripcionRepository.save(ins1);
                }

                if (inscripcionRepository.findByAlumnoAndOferta(alumnoCarlos, f2).isEmpty()) {
                    Inscripciones ins2 = new Inscripciones();
                    ins2.setAlumno(alumnoCarlos);
                    ins2.setOferta(f2);
                    ins2.setFechaInscripcion(LocalDate.now().minusDays(5));
                    ins2.setEstadoInscripcion(true);
                    inscripcionRepository.save(ins2);
                }

                System.out.println("✅ Inscrito alumno 44444444 en las formaciones creadas (o ya existente).");
            } else {
                System.err.println("SEED WARN: Alumno 44444444 no encontrado. Omisión de inscripciones.");
            }
        }
        
        System.out.println("SEED: Proceso de ofertas finalizado.");
    }

    private void crearCursoSiNoExiste(Curso curso) {
        boolean existe = cursoRepository.findAll().stream()
                .anyMatch(c -> c.getNombre().equalsIgnoreCase(curso.getNombre()));
        // Use model-level validation to check duplicates
        java.util.List<String> errores = curso.validarDuplicado(ofertaAcademicaRepository);
        if (errores.isEmpty()) {
            asegurarHorarios(curso);
            cursoRepository.save(curso);
            System.out.println("✅ Curso creado: " + curso.getNombre());
        } else {
            System.out.println("ℹ️ Curso no creado (duplicado): " + curso.getNombre() + " - " + errores);
        }
    }

    private void crearFormacionSiNoExiste(Formacion formacion) {
        boolean existe = formacionRepository.findAll().stream()
                .anyMatch(f -> f.getNombre().equalsIgnoreCase(formacion.getNombre()));
        java.util.List<String> errores = formacion.validarDuplicado(ofertaAcademicaRepository);
        if (errores.isEmpty()) {
            asegurarHorarios(formacion);
            formacionRepository.save(formacion);
            System.out.println("✅ Formación creada: " + formacion.getNombre());
        } else {
            System.out.println("ℹ️ Formación no creada (duplicado): " + formacion.getNombre() + " - " + errores);
        }
    }

    private void crearCharlaSiNoExiste(Charla charla) {
        java.util.List<String> errores = charla.validarDuplicado(ofertaAcademicaRepository);
        if (errores.isEmpty()) {
            asegurarHorarios(charla);
            charlaRepository.save(charla);
            System.out.println("✅ Charla creada: " + charla.getNombre());
        } else {
            System.out.println("ℹ️ Charla no creada (duplicado): " + charla.getNombre() + " - " + errores);
        }
    }

    private void crearSeminarioSiNoExiste(Seminario seminario) {
        java.util.List<String> errores = seminario.validarDuplicado(ofertaAcademicaRepository);
        if (errores.isEmpty()) {
            asegurarHorarios(seminario);
            seminarioRepository.save(seminario);
            System.out.println("✅ Seminario creado: " + seminario.getNombre());
        } else {
            System.out.println("ℹ️ Seminario no creado (duplicado): " + seminario.getNombre() + " - " + errores);
        }
    }

    private void crearInstitutoDefault() {
        if (institutoRepository.findByNombreInstituto("ICEP").isEmpty()) {
            Instituto instituto = new Instituto();
            instituto.setNombreInstituto("ICEP");
            instituto.setDescripcion("Instituto de Capacitación y Educación Profesional");
            instituto.setMision("Brindar educación de calidad accesible para todos, fomentando el desarrollo profesional y personal de nuestros estudiantes a través de metodologías innovadoras y tecnología de vanguardia.");
            instituto.setVision("Ser reconocidos como una institución líder en educación profesional, referente por nuestra excelencia académica, compromiso social y capacidad de adaptación a las necesidades del mercado laboral global.");
            
            // Colores predefinidos: Rojo (Primario) y Azul Oscuro (Secundario/Footer)
            instituto.setColores(Arrays.asList("#E5383B", "#0D1B2A"));
            
            // Configuraciones automáticas
            instituto.setPermisoBajaAutomatica(true);
            instituto.setMinimoAlumnoBaja(5);
            instituto.setInactividadBaja(30);
            
            // Configuración de pagos
            instituto.setMoneda("ARS");
            instituto.setCuentaBancaria("0000000000000000000000");
            instituto.setPoliticaPagos("Los pagos deben realizarse del 1 al 10 de cada mes. Pasada esa fecha se aplicará un recargo por mora.");
            
            // Configuración de bloqueos
            instituto.setDiasMoraBloqueoExamen(15);
            instituto.setDiasMoraBloqueoMaterial(30);
            instituto.setDiasMoraBloqueoActividad(20);
            instituto.setDiasMoraBloqueoAula(60);
            
            // Configuraciones del sistema
            instituto.setHabilitarIA(true);
            instituto.setReportesAutomaticos(true);
            
            // Información de contacto
            instituto.setDireccion("Av. Siempre Viva 123");
            instituto.setTelefono("+54 11 1234-5678");
            instituto.setEmail("contacto@icep.edu.ar");
            
            // Redes sociales
            instituto.setFacebook("https://facebook.com/icep");
            instituto.setX("https://x.com/icep");
            instituto.setInstagram("https://instagram.com/icep");
            
            // Certificaciones
            instituto.setCertificacionesAvales("Certificación ISO 9001, Aval del Ministerio de Educación");
            
            institutoRepository.save(instituto);
            System.out.println("✅ Instituto por defecto creado: ICEP");
        }
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

    private void asegurarHorarios(OfertaAcademica oferta) {
        if (oferta.getHorarios() == null || oferta.getHorarios().isEmpty()) {
            List<Docente> docentes = new ArrayList<>();
            if (oferta instanceof Curso) {
                docentes = ((Curso) oferta).getDocentes();
            } else if (oferta instanceof Formacion) {
                docentes = ((Formacion) oferta).getDocentes();
            }

            if (docentes != null && !docentes.isEmpty()) {
                for (Docente docente : docentes) {
                    Horario h = new Horario();
                    h.setDia(Dias.LUNES);
                    h.setHoraInicio(Time.valueOf("09:00:00"));
                    h.setHoraFin(Time.valueOf("11:00:00"));
                    
                    docente.addHorario(h);
                    oferta.addHorario(h);
                }
            } else {
                Horario h = new Horario();
                h.setDia(Dias.LUNES);
                h.setHoraInicio(Time.valueOf("09:00:00"));
                h.setHoraFin(Time.valueOf("11:00:00"));
                // h.setOfertaAcademica(oferta); // Gestionado por addHorario
                
                oferta.addHorario(h);
            }
        }
    }
}
