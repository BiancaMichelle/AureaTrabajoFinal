package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoAsistencia;
import com.example.demo.model.*;
import com.example.demo.repository.*;

@Service
public class AsistenciaEnVivoService {

    private final ClaseRepository claseRepository;
    private final RespuestaAsistenciaRepository respuestaAsistenciaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsistenciaService asistenciaService;
    private final AsistenciaRepository asistenciaRepository;
    private final Map<UUID, Map<String, LocalDateTime>> ingresosPorClase = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> acumuladoSegundosPorClase = new ConcurrentHashMap<>();

    public AsistenciaEnVivoService(ClaseRepository claseRepository,
                                   RespuestaAsistenciaRepository respuestaAsistenciaRepository,
                                   InscripcionRepository inscripcionRepository,
                                   UsuarioRepository usuarioRepository,
                                   AsistenciaService asistenciaService,
                                   AsistenciaRepository asistenciaRepository) {
        this.claseRepository = claseRepository;
        this.respuestaAsistenciaRepository = respuestaAsistenciaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.asistenciaService = asistenciaService;
        this.asistenciaRepository = asistenciaRepository;
    }

    private Map<String, LocalDateTime> obtenerIngresosClase(UUID claseId) {
        return ingresosPorClase.computeIfAbsent(claseId, key -> new ConcurrentHashMap<>());
    }

    private Map<String, Long> obtenerAcumuladoClase(UUID claseId) {
        return acumuladoSegundosPorClase.computeIfAbsent(claseId, key -> new ConcurrentHashMap<>());
    }

    private Clase obtenerClaseSilenciosa(UUID claseId) {
        return claseRepository.findById(claseId).orElse(null);
    }

    private LocalDateTime limitarATiempoClase(Clase clase, LocalDateTime momento) {
        if (momento == null || clase == null) {
            return momento;
        }
        LocalDateTime inicio = clase.getInicio();
        if (inicio != null && momento.isBefore(inicio)) {
            return inicio;
        }
        LocalDateTime fin = clase.getFin();
        if (fin != null && momento.isAfter(fin)) {
            return fin;
        }
        return momento;
    }

    public void registrarIngreso(UUID claseId, String alumnoDni) {
        Clase clase = obtenerClaseSilenciosa(claseId);
        LocalDateTime ahora = limitarATiempoClase(clase, LocalDateTime.now());
        Map<String, LocalDateTime> ingresos = obtenerIngresosClase(claseId);
        ingresos.putIfAbsent(alumnoDni, ahora);
    }

    public long registrarSalida(UUID claseId, String alumnoDni) {
        Clase clase = obtenerClaseSilenciosa(claseId);
        LocalDateTime ahora = limitarATiempoClase(clase, LocalDateTime.now());
        Map<String, LocalDateTime> ingresos = obtenerIngresosClase(claseId);
        LocalDateTime inicio = ingresos.remove(alumnoDni);
        if (inicio == null) {
            return obtenerAcumuladoClase(claseId).getOrDefault(alumnoDni, 0L);
        }

        LocalDateTime inicioReal = limitarATiempoClase(clase, inicio);
        long segundos = Duration.between(inicioReal, ahora).getSeconds();
        if (segundos < 0) {
            segundos = 0;
        }
        Map<String, Long> acumulados = obtenerAcumuladoClase(claseId);
        acumulados.merge(alumnoDni, segundos, Long::sum);
        long totalAcumulado = acumulados.getOrDefault(alumnoDni, 0L);

        // Intento extra: si ya cumplió el umbral, registrar asistencia al salir.
        if (clase != null
                && Boolean.TRUE.equals(clase.getAsistenciaAutomatica())
                && !Boolean.TRUE.equals(clase.getPreguntasAleatorias())) {
            try {
                registrarAsistenciaPorTiempo(claseId, alumnoDni);
            } catch (Exception e) {
                System.err.println("Error registrando asistencia al salir: " + e.getMessage());
            }
        }

        return totalAcumulado;
    }

    private long calcularSegundosAsistidos(UUID claseId, String alumnoDni, Clase clase) {
        long acumulado = obtenerAcumuladoClase(claseId).getOrDefault(alumnoDni, 0L);
        LocalDateTime inicio = obtenerIngresosClase(claseId).get(alumnoDni);
        if (inicio != null) {
            LocalDateTime ahora = limitarATiempoClase(clase, LocalDateTime.now());
            LocalDateTime inicioReal = limitarATiempoClase(clase, inicio);
            long segundos = Duration.between(inicioReal, ahora).getSeconds();
            if (segundos > 0) {
                acumulado += segundos;
            }
        }
        return acumulado;
    }

    /**
     * Verifica si hay una pregunta activa para el alumno en este momento
     */
    public Map<String, Object> verificarPreguntaActiva(UUID claseId, String alumnoDni) {
        Optional<Clase> claseOpt = claseRepository.findById(claseId);
        if (claseOpt.isEmpty()) {
            return null;
        }

        Clase clase = claseOpt.get();
        LocalDateTime ahora = LocalDateTime.now();

        // Validaciones básicas: clase iniciada y no terminada (con holgura)
        if (ahora.isBefore(clase.getInicio()) || ahora.isAfter(clase.getFin().plusMinutes(10))) {
            return null;
        }

        // Verificar si la asistencia automática está habilitada
        if (!Boolean.TRUE.equals(clase.getAsistenciaAutomatica()) || 
            !Boolean.TRUE.equals(clase.getPreguntasAleatorias())) {
            return null;
        }

        Integer intervalo = clase.getTiempoPreguntas();
        if (intervalo == null || intervalo <= 0) return null;

        // Calcular ronda actual
        long minutosTranscurridos = Duration.between(clase.getInicio(), ahora).toMinutes();
        int rondaActual = (int) (minutosTranscurridos / intervalo) + 1;

        // Verificar si excedimos la cantidad de preguntas configuradas
        if (clase.getCantidadPreguntas() != null && rondaActual > clase.getCantidadPreguntas()) {
            return null;
        }

        // Verificar si ya respondió esta ronda
        if (respuestaAsistenciaRepository.existsByClaseIdClaseAndAlumnoDniAndRonda(claseId, alumnoDni, rondaActual)) {
            return null;
        }

        // Generar pregunta (Mock o Real)
        return generarPregunta(clase, rondaActual);
    }

    private Map<String, Object> generarPregunta(Clase clase, int ronda) {
        // En una implementación completa, aquí buscaríamos del Pool de preguntas de la clase
        // Por ahora, generamos preguntas matemáticas simples como prueba de vida (anti-bot)
        
        Random rand = new Random();
        int a = rand.nextInt(10) + 1;
        int b = rand.nextInt(10) + 1;
        
        Map<String, Object> pregunta = new HashMap<>();
        pregunta.put("id", UUID.randomUUID().toString()); // ID temporal
        pregunta.put("tipo", "MATEMATICA");
        pregunta.put("enunciado", "¿Cuánto es " + a + " + " + b + "?");
        pregunta.put("opciones", List.of(
            String.valueOf(a + b),
            String.valueOf(a + b + 1),
            String.valueOf(a + b - 1),
            String.valueOf(a + b + 2)
        ));
        pregunta.put("ronda", ronda);
        pregunta.put("tiempoLimite", 180); // 3 minutos en segundos
        
        // Barajar opciones en un escenario real
        return pregunta;
    }

    @Transactional
    public boolean registrarRespuesta(UUID claseId, String alumnoDni, int ronda, String respuestaValor) {
        Clase clase = claseRepository.findById(claseId).orElseThrow();
        Alumno alumno = (Alumno) usuarioRepository.findByDni(alumnoDni).orElseThrow();

        // Validar respuesta (en este caso simple, siempre true si responde)
        // En futuro validar contra la respuesta correcta de la pregunta generada
        boolean esCorrecta = true; 

        RespuestaAsistencia respuesta = new RespuestaAsistencia();
        respuesta.setClase(clase);
        respuesta.setAlumno(alumno);
        respuesta.setRonda(ronda);
        respuesta.setFechaHora(LocalDateTime.now());
        respuesta.setEsCorrecta(esCorrecta);

        respuestaAsistenciaRepository.save(respuesta);
        
        return true;
    }
    
    /**
     * Calcula y registra la asistencia final basada en las respuestas
     */
    @Transactional
    public void consolidarAsistenciaClase(UUID claseId) {
        Clase clase = claseRepository.findById(claseId).orElseThrow();
        
        if (!Boolean.TRUE.equals(clase.getAsistenciaAutomatica())) return;
        if (!Boolean.TRUE.equals(clase.getPreguntasAleatorias())) return;
        
        Long ofertaId = clase.getModulo().getCurso().getIdOferta();
        List<Inscripciones> inscritos = inscripcionRepository.findByOfertaIdOferta(ofertaId);
        
        Integer tiempoPreguntas = clase.getTiempoPreguntas();
        int intervalo = tiempoPreguntas != null && tiempoPreguntas > 0 ? tiempoPreguntas : 15;
        int totalPreguntasEsperadas = clase.getCantidadPreguntas() != null ?
                                      clase.getCantidadPreguntas() :
                                      (int) (Duration.between(clase.getInicio(), clase.getFin()).toMinutes() / intervalo);

        // Umbral: Al menos 50% de respuestas
        int umbralAprobacion = totalPreguntasEsperadas / 2;
        if (umbralAprobacion < 1) umbralAprobacion = 1;

        for (Inscripciones inscripcion : inscritos) {
            // Validar que el usuario inscrito sea un Alumno antes de procesar
            if (!(inscripcion.getAlumno() instanceof Alumno)) {
                continue;
            }
            // Validar si la inscripción está activa
            if (!Boolean.TRUE.equals(inscripcion.getEstadoInscripcion())) {
                continue;
            }
            Alumno alumno = (Alumno) inscripcion.getAlumno();
            
            int respuestas = respuestaAsistenciaRepository.contarRespuestasPorClaseYAlumno(claseId, alumno.getDni());
            
            EstadoAsistencia estadoFinal;
            if (respuestas >= umbralAprobacion) {
                estadoFinal = EstadoAsistencia.PRESENTE;
            } else if (respuestas > 0) {
                 // Si respondió algo pero no llegó al 50%, podemos poner Inasistencia o Tardanza según regla de negocio
                 // El CU dice: "Si la recopilación... es menor a la mitad... se marca con inasistencia"
                estadoFinal = EstadoAsistencia.AUSENTE;
            } else {
                estadoFinal = EstadoAsistencia.AUSENTE;
            }
            
            // Verificar si ya existe registro para no duplicar (o actualizar)
            List<Asistencia> existentes = asistenciaRepository.findByOfertaIdOfertaAndAlumnoDni(ofertaId, alumno.getDni());
            // Filtrar por fecha de clase
            Optional<Asistencia> asistenciaHoy = existentes.stream()
                .filter(a -> a.getFecha().isEqual(clase.getInicio().toLocalDate()))
                .findFirst();

            if (asistenciaHoy.isPresent()) {
                Asistencia a = asistenciaHoy.get();
                a.setEstado(estadoFinal);
                asistenciaRepository.save(a);
            } else {
                asistenciaService.registrarAsistencia(ofertaId, alumno.getDni(), clase.getInicio().toLocalDate(), estadoFinal, claseId);
            }
        }
    }

    @Transactional
    public Map<String, Object> registrarAsistenciaPorTiempo(UUID claseId, String alumnoDni) {
        Clase clase = claseRepository.findById(claseId).orElseThrow();

        if (!Boolean.TRUE.equals(clase.getAsistenciaAutomatica())) {
            return Map.of("success", false, "message", "La asistencia automática no está habilitada para esta clase.");
        }
        if (Boolean.TRUE.equals(clase.getPreguntasAleatorias())) {
            return Map.of("success", false, "message", "La asistencia de esta clase se registra mediante preguntas aleatorias.");
        }

        if (clase.getInicio() == null || clase.getFin() == null) {
            return Map.of("success", false, "message", "La clase no tiene horario definido.");
        }

        long duracionMinutos = Duration.between(clase.getInicio(), clase.getFin()).toMinutes();
        if (duracionMinutos <= 0) {
            return Map.of("success", false, "message", "La duración de la clase es inválida.");
        }

        long minutosAsistidos = calcularSegundosAsistidos(claseId, alumnoDni, clase) / 60;
        long umbralMinutos = (duracionMinutos / 2) + 1; // más de la mitad

        if (minutosAsistidos <= umbralMinutos - 1) {
            return Map.of(
                "success", false,
                "message", "Aún no alcanzaste el tiempo mínimo de asistencia.",
                "minutosAsistidos", minutosAsistidos,
                "minutosRequeridos", umbralMinutos,
                "duracionMinutos", duracionMinutos
            );
        }

        Long ofertaId = clase.getModulo().getCurso().getIdOferta();
        asistenciaService.registrarAsistencia(
            ofertaId,
            alumnoDni,
            clase.getInicio().toLocalDate(),
            EstadoAsistencia.PRESENTE,
            claseId
        );

        return Map.of(
            "success", true,
            "message", "Asistencia registrada correctamente.",
            "minutosAsistidos", minutosAsistidos,
            "minutosRequeridos", umbralMinutos,
            "duracionMinutos", duracionMinutos
        );
    }
}
