package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoAsistencia;
import com.example.demo.model.Alumno;
import com.example.demo.model.Asistencia;
import com.example.demo.model.Clase;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.model.RespuestaAsistencia;
import com.example.demo.repository.ClaseRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.RespuestaAsistenciaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.AsistenciaRepository;

@Service
public class AsistenciaEnVivoService {

    private final ClaseRepository claseRepository;
    private final RespuestaAsistenciaRepository respuestaAsistenciaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsistenciaService asistenciaService;
    private final AsistenciaRepository asistenciaRepository;

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
        
        Long ofertaId = clase.getModulo().getCurso().getIdOferta();
        List<Inscripciones> inscritos = inscripcionRepository.findByOfertaIdOferta(ofertaId);
        
        int totalPreguntasEsperadas = clase.getCantidadPreguntas() != null ? 
                                      clase.getCantidadPreguntas() : 
                                      (int) (Duration.between(clase.getInicio(), clase.getFin()).toMinutes() / 
                                             (clase.getTiempoPreguntas() > 0 ? clase.getTiempoPreguntas() : 15));

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
}
