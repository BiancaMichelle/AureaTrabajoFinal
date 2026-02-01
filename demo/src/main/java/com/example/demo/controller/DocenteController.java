package com.example.demo.controller;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.demo.model.*;


@Controller
@RequestMapping("/docente")
public class DocenteController {

    @Value("${app.base-url}")
    private String baseUrl;

    private final CursoRepository cursoRepository;
    private final FormacionRepository formacionRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloRepository moduloRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ExamenRepository examenRepository;
    private final TareaRepository tareaRepository;
    private final EntregaRepository entregaRepository;
    private final ClaseRepository claseRepository;
    private final IntentoRepository intentoRepository;
    private final ObjectMapper objectMapper;

    public DocenteController(CursoRepository cursoRepository,
                           FormacionRepository formacionRepository,
                           OfertaAcademicaRepository ofertaAcademicaRepository,
                           UsuarioRepository usuarioRepository,
                           ModuloRepository moduloRepository,
                           InscripcionRepository inscripcionRepository,
                           ExamenRepository examenRepository,
                           TareaRepository tareaRepository,
                           EntregaRepository entregaRepository,
                           ClaseRepository claseRepository,
                           IntentoRepository intentoRepository,
                           ObjectMapper objectMapper) {
        this.cursoRepository = cursoRepository;
        this.formacionRepository = formacionRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.usuarioRepository = usuarioRepository;
        this.moduloRepository = moduloRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.examenRepository = examenRepository;
        this.tareaRepository = tareaRepository;
        this.entregaRepository = entregaRepository;
        this.claseRepository = claseRepository;
        this.intentoRepository = intentoRepository;
        this.objectMapper = objectMapper;
    }

    // Mi Espacio - Dashboard del docente con calendario
    @GetMapping("/mi-espacio")
    public String miEspacio(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario docente = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            // 1. Obtener Cursos y Formaciones donde es docente
            List<Curso> cursosDocente = cursoRepository.findByDocentesId(docente.getId()).stream()
                .filter(c -> c.getEstado() != EstadoOferta.DE_BAJA && c.getEstado() != EstadoOferta.FINALIZADA)
                .collect(Collectors.toList());
            
            List<Formacion> formacionesDocente = formacionRepository.findByDocentesId(docente.getId()).stream()
                .filter(f -> f.getEstado() != EstadoOferta.DE_BAJA && f.getEstado() != EstadoOferta.FINALIZADA)
                .collect(Collectors.toList());

            List<OfertaAcademica> todasOfertasActivas = new ArrayList<>();
            todasOfertasActivas.addAll(cursosDocente);
            todasOfertasActivas.addAll(formacionesDocente);

            long totalcursosActivos = todasOfertasActivas.size();

            // 2. Contar Alumnos
            long totalAlumnos = 0;
            for (OfertaAcademica oferta : todasOfertasActivas) {
                totalAlumnos += inscripcionRepository.countByOfertaAndEstadoInscripcionTrue(oferta);
            }

            // 3. Contar Correcciones Pendientes (Entregas sin nota + Exámenes por corregir)
            List<Tarea> tareasCursos = new ArrayList<>();
            List<Examen> examenesCursos = new ArrayList<>();
            
            if (!todasOfertasActivas.isEmpty()) {
                tareasCursos.addAll(tareaRepository.findByModuloCursoIn(todasOfertasActivas));
                examenesCursos.addAll(examenRepository.findByModulo_CursoIn(todasOfertasActivas));
            }

            long entregasSinCalificar = 0;
            if (!tareasCursos.isEmpty()) {
                List<Entrega> entregasDeTareas = entregaRepository.findByTareaIn(tareasCursos);
                 entregasSinCalificar = entregasDeTareas.stream()
                     .filter(e -> e.getCalificacion() == null)
                     .count();
            }
            
            long examenesPorCorregir = 0;
            if(!examenesCursos.isEmpty()){
                List<Long> examenIds = examenesCursos.stream()
                    .map(Examen::getIdActividad)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                
                if (!examenIds.isEmpty()) {
                    List<Intento> intentosPendientes = intentoRepository.findByExamenIdsAndEstado(examenIds, com.example.demo.enums.EstadoIntento.PENDIENTE_CORRECCION);
                    examenesPorCorregir = intentosPendientes.size();
                }
            }
            
            long correccionesPendientes = entregasSinCalificar + examenesPorCorregir;

            // 4. Eventos Calendario (Exámenes y Entregas próximas)
            LocalDateTime ahora = LocalDateTime.now();
            List<Map<String, String>> events = new ArrayList<>();

            LocalDateTime fechaInicioCal = ahora.minusMonths(6);
            LocalDateTime fechaFinCal = ahora.plusMonths(12);

            // 1. Exámenes Calendario (Range)
            // Filtramos en memoria de la lista completa para evitar mas queries
            List<Examen> examenesCalendario = examenesCursos.stream()
                .filter(ex -> ex.getFechaApertura() != null && 
                              ex.getFechaApertura().isAfter(fechaInicioCal) && 
                              ex.getFechaApertura().isBefore(fechaFinCal))
                .collect(Collectors.toList());
            
            examenesCalendario.forEach(ex -> {
                Map<String, String> event = new HashMap<>();
                event.put("title", "Examen: " + ex.getTitulo());
                event.put("start", ex.getFechaApertura().toLocalDate().toString());
                event.put("className", "event-examen"); 
                events.add(event);
            });
            
            // Exámenes Panel (Future)
            List<Examen> proximosExamenes = examenesCalendario.stream()
                .filter(e -> e.getFechaApertura().isAfter(ahora))
                .sorted(Comparator.comparing(Examen::getFechaApertura))
                .limit(5)
                .collect(Collectors.toList());

            // 2. Tareas Calendario
            tareasCursos.stream()
                .filter(t -> t.getLimiteEntrega() != null 
                        && t.getLimiteEntrega().isAfter(fechaInicioCal)
                        && t.getLimiteEntrega().isBefore(fechaFinCal))
                .forEach(t -> {
                     Map<String, String> event = new HashMap<>();
                     event.put("title", "Entrega: " + t.getTitulo());
                     event.put("start", t.getLimiteEntrega().toLocalDate().toString());
                     event.put("className", "event-entrega");
                     events.add(event);
                });
            
            // Tareas Panel (Future)
            List<Tarea> proximasTareas = tareasCursos.stream()
                .filter(t -> t.getLimiteEntrega() != null && t.getLimiteEntrega().isAfter(ahora))
                .sorted(Comparator.comparing(Tarea::getLimiteEntrega))
                .limit(5)
                .collect(Collectors.toList());

            // 3. Clases Calendario
            List<Clase> clasesCalendario = claseRepository.findByDocente_DniAndInicioBetweenOrderByInicioAsc(docente.getDni(), fechaInicioCal, fechaFinCal);
            clasesCalendario.forEach(c -> {
                Map<String, String> event = new HashMap<>();
                String nombreCurso = c.getModulo() != null ? c.getModulo().getCurso().getNombre() : (c.getCurso() != null ? c.getCurso().getNombre() : "Clase");
                event.put("title", "Clase: " + c.getTitulo() + " (" + nombreCurso + ")");
                event.put("start", c.getInicio().toLocalDate().toString());
                event.put("className", "event-clase");
                events.add(event);
            });
            
            // Clases Panel (Future)
            List<Clase> proximasClasesPanel = clasesCalendario.stream()
                 .filter(c -> c.getInicio().isAfter(ahora))
                 .limit(5)
                 .collect(Collectors.toList());


            model.addAttribute("docente", docente);
            model.addAttribute("esDocente", true);
            
            // Datos Estadísticos
            model.addAttribute("cursosActivos", totalcursosActivos);
            model.addAttribute("totalAlumnos", totalAlumnos);
            model.addAttribute("correccionesPendientes", correccionesPendientes);

            // Datos Calendario
            model.addAttribute("eventsJson", objectMapper.writeValueAsString(events));
            
            // Listas para paneles laterales (opcional)
            model.addAttribute("proximosExamenes", proximosExamenes);
            model.addAttribute("proximasTareas", proximasTareas);
            model.addAttribute("proximasClases", proximasClasesPanel);

            return "docente/mi-espacio";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mi-espacio (docente): " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar tu espacio");
            return "redirect:/";
        }
    }
    
    // Mis Pagos
    @GetMapping("/mis-pagos")
    public String misPagos(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario docente = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            model.addAttribute("docente", docente);
            model.addAttribute("esDocente", true);
            
            return "docente/mis-pagos";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mis-pagos (docente): " + e.getMessage());
            model.addAttribute("error", "Error al cargar tus pagos");
            return "redirect:/";
        }
    }

    @GetMapping("/mis-ofertas")
    public String misOfertas(Model model, Principal principal) {
        try {
            String username = principal.getName();
            System.out.println("🔍 Username del Principal: " + username);
            
            // Buscar por DNI
            Optional<Usuario> docenteOpt = usuarioRepository.findByDni(username);
            
            if (docenteOpt.isEmpty()) {
                System.out.println("❌ No se encontró docente con DNI: " + username);
                model.addAttribute("error", "No se pudo encontrar docente con DNI: " + username);
                return "misOfertasAcademicas";
            }
            
            Usuario docente = docenteOpt.get();
            System.out.println("✅ Docente encontrado: " + docente.getNombre() + " " + docente.getApellido());
            
            // ========================================
            // 1. OFERTAS DONDE ES DOCENTE
            // ========================================
            List<Curso> cursosDelDocente = cursoRepository.findByDocentesId(docente.getId());
            // Filtrar cursos de baja
            cursosDelDocente = cursosDelDocente.stream()
                .filter(c -> c.getEstado() != EstadoOferta.DE_BAJA)
                .collect(Collectors.toList());
            System.out.println("�‍🏫 Cursos como docente: " + cursosDelDocente.size());
            
            List<Formacion> formacionesDelDocente = formacionRepository.findByDocentesId(docente.getId());
            // Filtrar formaciones de baja
            formacionesDelDocente = formacionesDelDocente.stream()
                .filter(f -> f.getEstado() != EstadoOferta.DE_BAJA)
                .collect(Collectors.toList());
            System.out.println("👨‍🏫 Formaciones como docente: " + formacionesDelDocente.size());
            
            List<OfertaAcademica> ofertasComoDocente = new ArrayList<>();
            ofertasComoDocente.addAll(cursosDelDocente);
            ofertasComoDocente.addAll(formacionesDelDocente);
            
            // ========================================
            // 2. OFERTAS DONDE ESTÁ INSCRITO COMO ALUMNO
            // ========================================
            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDni(username);
            System.out.println("🎓 Inscripciones como alumno: " + inscripciones.size());
            
            List<OfertaAcademica> ofertasComoAlumno = inscripciones.stream()
                    .filter(ins -> ins.getEstadoInscripcion())
                    .map(Inscripciones::getOferta)
                    .filter(oferta -> oferta.getEstado() != EstadoOferta.DE_BAJA) // Filtrar ofertas de baja
                    .collect(Collectors.toList());
            
            // ========================================
            // 3. COMBINAR TODAS (sin duplicados)
            // ========================================
            List<OfertaAcademica> todasLasOfertas = new ArrayList<>(ofertasComoDocente);
            
            // Agregar ofertas como alumno que NO estén ya en la lista (evitar duplicados)
            for (OfertaAcademica ofertaAlumno : ofertasComoAlumno) {
                boolean yaExiste = todasLasOfertas.stream()
                        .anyMatch(o -> o.getIdOferta().equals(ofertaAlumno.getIdOferta()));
                if (!yaExiste) {
                    todasLasOfertas.add(ofertaAlumno);
                }
            }
            
            System.out.println("📋 Total de ofertas académicas: " + todasLasOfertas.size());
            
            // Separar listas por Rol y Estado
            List<OfertaAcademica> docenteActivos = new ArrayList<>();
            List<OfertaAcademica> docenteFinalizados = new ArrayList<>();
            List<OfertaAcademica> alumnoActivos = new ArrayList<>();
            List<OfertaAcademica> alumnoFinalizados = new ArrayList<>();

            // 1. Clasificar ofertas como DOCENTE
            for (OfertaAcademica oferta : ofertasComoDocente) {
                boolean esFinalizada = (oferta.getEstado() == EstadoOferta.FINALIZADA) ||
                        (oferta.getFechaFin() != null && oferta.getFechaFin().isBefore(LocalDate.now()));
                if (esFinalizada) {
                    docenteFinalizados.add(oferta);
                } else {
                    docenteActivos.add(oferta);
                }
            }

            // 2. Clasificar ofertas como ALUMNO
            for (OfertaAcademica oferta : ofertasComoAlumno) {
                boolean esFinalizada = (oferta.getEstado() == EstadoOferta.FINALIZADA) ||
                        (oferta.getFechaFin() != null && oferta.getFechaFin().isBefore(LocalDate.now()));
                if (esFinalizada) {
                    alumnoFinalizados.add(oferta);
                } else {
                    alumnoActivos.add(oferta);
                }
            }

            // Crear SET de IDs para verificación rápida en vista
            Set<Long> idsOfertasDocente = ofertasComoDocente.stream()
                    .map(OfertaAcademica::getIdOferta)
                    .collect(Collectors.toSet());
            Set<Long> idsOfertasAlumno = ofertasComoAlumno.stream()
                    .map(OfertaAcademica::getIdOferta)
                    .collect(Collectors.toSet());

            model.addAttribute("docenteActivos", docenteActivos);
            model.addAttribute("docenteFinalizados", docenteFinalizados);
            model.addAttribute("alumnoActivos", alumnoActivos);
            model.addAttribute("alumnoFinalizados", alumnoFinalizados);
            
            model.addAttribute("cursos", todasLasOfertas); // Mantenemos por compatibilidad si es necesario
            model.addAttribute("idsOfertasDocente", idsOfertasDocente);
            model.addAttribute("idsOfertasAlumno", idsOfertasAlumno);
            model.addAttribute("docente", docente);
            model.addAttribute("esDocente", true);
            
            return "misOfertasAcademicas";
            
        } catch (Exception e) {
            System.err.println("❌ Error en misOfertas: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar tus ofertas académicas: " + e.getMessage());
            return "misOfertasAcademicas";
        }
    }

    // Acceder al aula de una oferta académica (Curso o Formación) como DOCENTE
    @GetMapping("/aula/{cursoId}")
    public String accederAlAula(@PathVariable Long cursoId,
                            Principal principal,
                            Model model) {
        try {
            String dni = principal.getName();
            System.out.println("🎓 Docente accediendo al aula para oferta ID: " + cursoId + ", usuario: " + dni);
            
            // Buscar la oferta académica (puede ser Curso o Formación)
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(cursoId)
                    .orElseThrow(() -> new RuntimeException("Oferta académica no encontrada"));

            System.out.println("📋 Tipo de oferta: " + oferta.getClass().getSimpleName());
            
            // Verificar si el docente está asignado a esta oferta
            boolean esDocenteDeLaOferta = false;
            List<Docente> docentesAsignados = new ArrayList<>();
            
            if (oferta instanceof Curso) {
                Curso curso = (Curso) oferta;
                docentesAsignados = curso.getDocentes();
                esDocenteDeLaOferta = curso.getDocentes().stream()
                        .anyMatch(docente -> docente.getDni().equals(dni));
            } else if (oferta instanceof Formacion) {
                Formacion formacion = (Formacion) oferta;
                docentesAsignados = formacion.getDocentes();
                esDocenteDeLaOferta = formacion.getDocentes().stream()
                        .anyMatch(docente -> docente.getDni().equals(dni));
            }
            
            System.out.println("� Docentes asignados:");
            for (Docente docente : docentesAsignados) {
                System.out.println("   - " + docente.getDni() + " | " + docente.getNombre());
            }
            
            System.out.println("🔍 Es docente de la oferta: " + esDocenteDeLaOferta);
            System.out.println("🔍 DNI buscado: " + dni);
            
            if (!esDocenteDeLaOferta) {
                System.out.println("❌ Acceso denegado: el docente no está asignado a esta oferta");
                return "redirect:/docente/mis-ofertas";
            }

            // Buscar módulos de esta oferta
            List<Modulo> modulos = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(oferta);
            
            // Inicializar colecciones perezosas para evitar problemas en la vista
            for (Modulo m : modulos) {
                m.getClases().size();
                m.getActividades().size();
            }

            Usuario docente = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            model.addAttribute("curso", oferta); // Mantener nombre "curso" para compatibilidad con la vista
            model.addAttribute("modulos", modulos);
            model.addAttribute("docente", docente);
            model.addAttribute("puedeModificar", true);
            
            System.out.println("✅ Model attributes:");
            System.out.println("   - puedeModificar: " + true);
            System.out.println("   - docente: " + docente.getNombre());
            System.out.println("   - oferta: " + oferta.getNombre());
            System.out.println("   - modulos: " + modulos.size());
            
            return "aula";
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/docente/mis-ofertas";
        }
    }
}