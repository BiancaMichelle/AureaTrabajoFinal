package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Inscripciones;
import com.example.demo.model.IntervencionAcademica;
import com.example.demo.model.Notificacion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.Modulo;
import com.example.demo.model.Actividad;
import com.example.demo.model.Tarea;
import com.example.demo.model.Examen;
import com.example.demo.model.Intento;
import com.example.demo.model.Entrega;
import com.example.demo.model.Asistencia;
import com.example.demo.enums.EstadoAsistencia;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.IntervencionAcademicaRepository;
import com.example.demo.repository.NotificacionRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.EntregaRepository;
import com.example.demo.repository.IntentoRepository;
import com.example.demo.repository.AsistenciaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

@Service
public class AnalisisRendimientoService {

    private static final Logger log = LoggerFactory.getLogger(AnalisisRendimientoService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private IntervencionAcademicaRepository intervencionRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;
    
    @Autowired
    private EntregaRepository entregaRepository;
    
    @Autowired
    private IntentoRepository intentoRepository;
    
    @Autowired
    private AsistenciaRepository asistenciaRepository;

    // Se ejecuta todos los días a las 02:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void ejecutarAnalisisDiario() {
        log.info("🤖 Iniciando Análisis Automático de Rendimiento con IA...");
        
        List<Usuario> alumnos = usuarioRepository.findAll(); // Idealmente filtrar solo alumnos activos
        
        for (Usuario alumno : alumnos) {
            analizarAlumno(alumno);
        }
        
        log.info("✅ Análisis finalizado.");
    }

    public void analizarAlumno(Usuario alumno) {
        // Obtenemos inscripciones activas
        // Nota: Asumimos que podemos obtener inscripciones del alumno. Si no hay método directo en Repo, usamos el de InscripcionRepo
        // Para este ejemplo, simulamos lógica de negocio.
        
        // Simulación: Analizar inscripciones del alumno
        // En un caso real: List<Inscripcion> inscripciones = inscripcionRepository.findByUsuario(alumno);
        if (alumno.getInscripciones() == null || alumno.getInscripciones().isEmpty()) return;

        for (Inscripciones inscripcion : alumno.getInscripciones()) {
            if (Boolean.TRUE.equals(inscripcion.getEstadoInscripcion())) {
               analizarDesempenoEnOferta(alumno, inscripcion);
            }
        }
    }

    /**
     * Fuerza un análisis y devuelve el resultado para demostración manual
     */
    @Transactional
    public String analizarAlumnoForce(Usuario alumno) {
        // Recargamos inscripciones directamente del repositorio para asegurar datos frescos
        List<Inscripciones> inscripciones = inscripcionRepository.findByAlumno(alumno);
        
        if (inscripciones == null || inscripciones.isEmpty()) {
            return "No tienes inscripciones registradas.";
        }

        // 1. Filtrar SOLO Cursos o Formaciones activas (ignorar Charlas/Seminarios)
        List<Inscripciones> validas = inscripciones.stream()
            .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
            .filter(i -> (i.getOferta() instanceof Curso) || (i.getOferta() instanceof Formacion))
            .toList();

        if (validas.isEmpty()) {
             long totalActive = inscripciones.stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count();
             if (totalActive > 0) {
                 return "Tus inscripciones actuales (Charlas/Seminarios) no requieren análisis de rendimiento detallado. ¡Disfruta del contenido!";
             }
             return "No tienes cursos ni formaciones activas para analizar en este momento.";
        }

        // 2. Analizar cada curso/formación para buscar el de "Peor Desempeño"
        String ofertaPeor = "";
        double notaPeor = 101.0; 
        double asistenciaPeor = 101.0;
        
        // Criterio para selección del peor:
        // Prioridad 1: Asistencia Crítica (< 70%)
        // Prioridad 2: Nota más baja
        
        OfertaAcademica targetOferta = null;
        boolean hayRiesgoAsistencia = false;
        
        for (Inscripciones insc : validas) {
            OfertaAcademica oferta = insc.getOferta();
            double promedio = calcularPromedio(alumno, oferta);
            double asistencia = calcularAsistencia(alumno, oferta);
            
            // Lógica de comparación
            if (targetOferta == null) {
                targetOferta = oferta;
                notaPeor = promedio;
                asistenciaPeor = asistencia;
                ofertaPeor = oferta.getNombre();
                if (asistencia < 70.0) hayRiesgoAsistencia = true;
            } else {
                // Si ya teníamos uno en riesgo de asistencia y este también, comparamos asistencias
                if (hayRiesgoAsistencia && asistencia < 70.0) {
                    if (asistencia < asistenciaPeor) {
                        targetOferta = oferta;
                        asistenciaPeor = asistencia;
                        notaPeor = promedio;
                        ofertaPeor = oferta.getNombre();
                    }
                } 
                // Si el actual tiene riesgo y el previo no, este gana
                else if (asistencia < 70.0 && !hayRiesgoAsistencia) {
                    targetOferta = oferta;
                    asistenciaPeor = asistencia;
                    notaPeor = promedio;
                    ofertaPeor = oferta.getNombre();
                    hayRiesgoAsistencia = true;
                }
                // Si ninguno tiene riesgo crítico de asistencia, vamos por nota
                else if (!hayRiesgoAsistencia) {
                    if (promedio < notaPeor) {
                         targetOferta = oferta;
                         notaPeor = promedio;
                         asistenciaPeor = asistencia;
                         ofertaPeor = oferta.getNombre();
                    }
                }
            }
        }
        
        // 3. Generar mensaje
        StringBuilder sb = new StringBuilder();
        sb.append("He analizado tus ").append(validas.size()).append(" cursadas activas. ");
        
        if (targetOferta != null) {
            sb.append("Basado en tus métricas actuales, te recomiendo enfocarte en **").append(ofertaPeor).append("**. ");
            
            // Detalle de métricas 
            if (notaPeor == 100.0 && asistenciaPeor == 100.0) {
                 sb.append("Por ahora tienes puntaje perfecto, ¡Sigue así!");
            } else {
                String notaStr = (notaPeor == 100.0) ? "Sin calificaciones aún" : String.format("%.1f", notaPeor);
                sb.append("<br>- **Promedio**: ").append(notaStr);
                sb.append("<br>- **Asistencia**: ").append(String.format("%.1f%%", asistenciaPeor));
                
                if (asistenciaPeor < 70.0) {
                    sb.append("<br>⚠️ **Alerta**: Tu asistencia está por debajo del 70% requerido para aprobar.");
                } else if (notaPeor < 60.0 && notaPeor != 100.0) {
                    sb.append("<br>⚠️ Tu promedio es bajo. Aquí tienes algunas recomendaciones:");
                    sb.append(getConsejosPersonalizados(alumno, targetOferta));
                } else {
                    sb.append("<br>Vas bien, pero no te descuides.");
                }
            }
            
            // Guardamos la intervención real con los datos calculados
            generarIntervencion(alumno, targetOferta, "ANALISIS_REAL", "Solicitud del Usuario", sb.toString());
        }
        
        // Solo agregar el recordatorio general si la asistencia es baja
        if (asistenciaPeor < 70.0) {
             sb.append("<br><br>Recordá que para aprobar cursadas regulares necesitas un 70% de asistencia mínima.");
        }
        return sb.toString();
    }
    
    private String getConsejosPersonalizados(Usuario alumno, OfertaAcademica oferta) {
        StringBuilder consejos = new StringBuilder();
        if (oferta instanceof Curso) {
             Curso curso = (Curso) oferta;
             if (curso.getModulos() != null) {
                 for(Modulo m : curso.getModulos()) {
                     if(m.getActividades() != null) {
                        for(Actividad a : m.getActividades()) {
                            Double grade = null;
                            String titulo = ""; 
                            // Usamos reflection o instanceof basico si getTitulo no está en Actividad (Actividad suele tener titulo o nombre)
                            // En el modelo proporcionado, Actividad tiene 'titulo'? No lo leí en el dump, pero Tarea y Examen sí.
                            // Asumiremos que comparten una base o casteamos.
                            
                            if (a instanceof Tarea) {
                                titulo = ((Tarea)a).getTitulo();
                                var opt = entregaRepository.findByTareaAndEstudiante((Tarea)a, alumno);
                                if(opt.isPresent() && opt.get().getCalificacion() != null) {
                                    grade = opt.get().getCalificacion();
                                }
                            } else if (a instanceof Examen) {
                                titulo = ((Examen)a).getTitulo();
                                List<Intento> intentos = intentoRepository.findByAlumno_IdAndExamen_IdActividad(alumno.getId(), a.getIdActividad());
                                if(!intentos.isEmpty()) {
                                    grade = intentos.stream()
                                        .map(in -> in.getCalificacion() != null ? in.getCalificacion().doubleValue() : 0.0)
                                        .max(Double::compareTo).orElse(0.0);
                                }
                            }
                            
                            // Check logic: < 6 or (< 60 && > 10)
                            if (grade != null) {
                                if (grade < 6.0 || (grade > 10.0 && grade < 60.0)) {
                                    consejos.append("<br>&nbsp;&nbsp;❌ **").append(titulo).append("** (Nota: ").append(grade).append("): ");
                                    consejos.append("Te sugiero repasar el contenido del **").append(m.getNombre()).append("** relacionado con esta actividad.");
                                }
                            }
                        }
                     }
                 }
             }
        }
        return consejos.toString();
    }

    private Double calcularPromedio(Usuario alumno, OfertaAcademica oferta) {
        if (oferta instanceof Curso) {
             Curso curso = (Curso) oferta;
             if (curso.getModulos() == null || curso.getModulos().isEmpty()) return 100.0;
             
             List<Double> grades = new ArrayList<>();
             for(Modulo m : curso.getModulos()) {
                 if(m.getActividades() != null) {
                    for(Actividad a : m.getActividades()) {
                        if(a instanceof Tarea) {
                           entregaRepository.findByTareaAndEstudiante((Tarea)a, alumno)
                               .ifPresent(e -> {
                                   if(e.getCalificacion() != null) grades.add(e.getCalificacion());
                               });
                        } else if (a instanceof Examen) {
                           List<Intento> intentos = intentoRepository.findByAlumno_IdAndExamen_IdActividad(alumno.getId(), a.getIdActividad());
                           if(!intentos.isEmpty()) {
                               double max = intentos.stream()
                                   .map(in -> in.getCalificacion() != null ? in.getCalificacion().doubleValue() : 0.0)
                                   .max(Double::compareTo).orElse(0.0);
                               grades.add(max);
                           }
                        }
                    }
                 }
             }
             if(grades.isEmpty()) return 100.0;
             return grades.stream().mapToDouble(d->d).average().orElse(0.0);
        }
        return 100.0; // Formacion o sin datos
    }
    
    private Double calcularAsistencia(Usuario alumno, OfertaAcademica oferta) {
        List<Asistencia> records = asistenciaRepository.findByOfertaIdOfertaAndAlumnoDni(oferta.getIdOferta(), alumno.getDni());
        if(records.isEmpty()) return 100.0; 
        
        long present = records.stream()
            .filter(a -> a.getEstado() == EstadoAsistencia.PRESENTE || a.getEstado() == EstadoAsistencia.TARDANZA)
            .count();
        long total = records.size();
        
        if (total == 0) return 100.0; // Safer
        return (double) present / total * 100.0;
    }

    private void analizarDesempenoEnOferta(Usuario alumno, Inscripciones inscripcion) {
        OfertaAcademica oferta = inscripcion.getOferta();
        
        // 1. Detección de Inasistencias (Simulada)
        // Lógica real: Calcular % asistencia desde entidad Asistencia
        boolean alertaInasistencia = simularAnalisisAsistencia(alumno.getId());
        
        if (alertaInasistencia) {
            generarIntervencion(alumno, oferta, "BAJO_ASISTENCIA", 
                "Hemos notado que has faltado a las últimas clases de " + oferta.getNombre() + ".");
        }

        // 2. Detección de Notas Bajas (Simulada)
        boolean notasBajas = simularAnalisisNotas(alumno.getId());
        if (notasBajas) {
            generarIntervencion(alumno, oferta, "BAJO_RENDIMIENTO", 
                "Tu rendimiento reciente en " + oferta.getNombre() + " ha disminuido un poco.");
        }
    }

    private void generarIntervencion(Usuario alumno, OfertaAcademica oferta, String tipo, String motivo) {
        // Verificar si ya existe una intervención reciente para no spammear
        // ... Logica de deduplicación ...

        // Generar Sugerencia con "IA" (Simulada por Templates/Heurística)
        String sugerencia = generarSugerenciaIA(tipo, oferta.getNombre());
        
        generarIntervencion(alumno, oferta, tipo, motivo, sugerencia);
    }

    private void generarIntervencion(Usuario alumno, OfertaAcademica oferta, String tipo, String motivo, String sugerencia) {
        // Guardar Intervención
        IntervencionAcademica intervencion = new IntervencionAcademica();
        intervencion.setAlumno(alumno);
        intervencion.setOferta(oferta);
        intervencion.setTipoIntervencion(tipo);
        intervencion.setMotivoDetectado(motivo);
        intervencion.setSugerenciaIA(sugerencia);
        intervencion.setEnviadaAlumno(true);
        intervencionRepository.save(intervencion);

        // Enviar Notificación (Simula Chat/Alerta sistema)
        enviarAlertaChat(alumno, "🤖 Asistente Académico: " + sugerencia);
        
        log.info("⚠️ Intervención generada para alumno {}: {}", alumno.getDni(), tipo);
    }


    private String generarSugerenciaIA(String tipo, String nombreCurso) {
        // Aquí iría la llamada a OpenAI/Gemini
        // Fallback local:
        if ("BAJO_ASISTENCIA".equals(tipo)) {
            return "Hola! He notado que te perdiste algunas clases de " + nombreCurso + ". " +
                   "Te sugiero ver las grabaciones en el Aula Virtual para ponerte al día. " +
                   "¿Necesitas ayuda con algún tema en específico?";
        } else if ("BAJO_RENDIMIENTO".equals(tipo)) {
            return "Hola! Parece que " + nombreCurso + " se está poniendo difícil. " +
                   "Te recomiendo revisar el Módulo 3 y hacer los ejercicios de práctica nuevamente. " +
                   "¡No te desanimes, venías muy bien!";
        }
        return "Hola! Estoy aquí para ayudarte con tu cursada.";
    }

    private void enviarAlertaChat(Usuario alumno, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(alumno);
        notificacion.setTitulo("Sugerencia de Estudio");
        notificacion.setMensaje(mensaje);
        notificacion.setTipo("CHAT_IA");
        notificacion.setLeida(false);
        notificacionRepository.save(notificacion);
    }

    // --- MOCKS para simulación ---
    private boolean simularAnalisisAsistencia(UUID alumnoId) {
        // Logica real iría aqui (ej: consultar registros por alumnoId)
        return new Random().nextInt(100) < 5; // 5% de probabilidad de alerta para demo
    }

    private boolean simularAnalisisNotas(UUID alumnoId) {
        return new Random().nextInt(100) < 5; // 5% de probabilidad
    }
}
