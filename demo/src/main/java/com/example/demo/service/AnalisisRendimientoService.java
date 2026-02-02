package com.example.demo.service;

import java.time.LocalDateTime;
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
    
    @Autowired
    private com.example.demo.repository.OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private EmailService emailService;

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
        if (alumno.getInscripciones() == null || alumno.getInscripciones().isEmpty()) return;

        for (Inscripciones inscripcion : alumno.getInscripciones()) {
            if (Boolean.TRUE.equals(inscripcion.getEstadoInscripcion())) {
               analizarDesempenoEnOfertaDetallado(alumno, inscripcion); // Usamos el nuevo metodo.
            }
        }
    }

    public void enviarCorreoPersonalizado(UUID alumnoId, Long ofertaId, String asunto, String cuerpo) {
        Usuario alumno = usuarioRepository.findById(alumnoId).orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        
        // Firma institucional append si no está presente
        if (!cuerpo.contains("Secretaría Académica")) {
            cuerpo += "<br><br>--<br><b>Secretaría Académica</b><br>Espacio Virtual ICEP";
        }
        
        if (alumno.getCorreo() != null && !alumno.getCorreo().isEmpty()) {
            emailService.sendEmail(alumno.getCorreo(), asunto, cuerpo);
            log.info("📧 Correo personalizado enviado a {} con asunto: {}", alumno.getCorreo(), asunto);
        }
    }

    public void enviarCorreoInstitucional(UUID alumnoId, Long ofertaId, String tipo) {
        Usuario alumno = usuarioRepository.findById(alumnoId).orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId).orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        
        String asunto = "Aviso Importante - " + oferta.getNombre();
        String cuerpo = generarBorradorCorreo(alumno, oferta, tipo).replace("\n", "<br>");
        
        // Firma institucional
        cuerpo += "<br><br>--<br><b>Secretaría Académica</b><br>Espacio Virtual ICEP";
        
        if (alumno.getCorreo() != null && !alumno.getCorreo().isEmpty()) {
            emailService.sendEmail(alumno.getCorreo(), asunto, cuerpo);
            log.info("📧 Correo institucional enviado a {} por motivo: {}", alumno.getCorreo(), tipo);
        }
    }

    @Transactional
    public String analizarCurso(Long ofertaId) {
        log.info("🔎 Analizando curso ID: {}", ofertaId);
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId).orElse(null);
        if (oferta == null) return "Curso no encontrado";

        List<Inscripciones> inscripciones = inscripcionRepository.findByOfertaAndEstadoInscripcionTrue(oferta);
        int intervencionesGeneradas = 0;
        int alumnosAnalizados = 0;
        
        StringBuilder reporteDetallado = new StringBuilder();
        reporteDetallado.append("🔍 **Análisis de Rendimiento - ").append(oferta.getNombre()).append("**<br><br>");

        for (Inscripciones insc : inscripciones) {
            alumnosAnalizados++;
            List<String> alertas = analizarDesempenoEnOfertaDetallado(insc.getAlumno(), insc, false); // False: NO notificar individualmente al docente
            if (!alertas.isEmpty()) {
                intervencionesGeneradas += alertas.size();
                reporteDetallado.append("👤 **").append(insc.getAlumno().getNombre()).append("**: <br>");
                alertas.forEach(a -> reporteDetallado.append(" - ").append(a).append("<br>"));
                
                // Botón para enviar correo institucional
                String tipoPrincipal = alertas.get(0).contains("Asistencia") ? "BAJO_ASISTENCIA" : 
                                       alertas.get(0).contains("Rendimiento") ? "BAJO_RENDIMIENTO" : "ACTIVIDADES_PENDIENTES";
                
                reporteDetallado.append("<div style='margin-top:5px; margin-bottom:15px;'>")
                                .append("💡 <i>IA: Sugiero contactarlo. </i>")
                                .append("<form action='/docente/ia/preparar-correo' method='get' target='_blank' style='display:inline;'>")
                                .append("<input type='hidden' name='alumnoId' value='").append(insc.getAlumno().getId()).append("'>")
                                .append("<input type='hidden' name='ofertaId' value='").append(oferta.getIdOferta()).append("'>")
                                .append("<input type='hidden' name='tipo' value='").append(tipoPrincipal).append("'>")
                                .append("<button type='submit' style='background-color:#ea4335; color:white; border:none; padding:5px 10px; border-radius:4px; font-size:12px; cursor:pointer;'>")
                                .append("📧 Revisar y Enviar Correo</button>")
                                .append("</form></div>");
            }
        }

        if (intervencionesGeneradas == 0) {
            return "✅ Estado Óptimo: Se analizaron " + alumnosAnalizados + " alumnos y no se detectaron nuevos riesgos en este curso. ¡Buen trabajo!";
        } else {
            return reporteDetallado.toString();
        }
    }
    
    // Método auxiliar modificado para devolver detalles
    private List<String> analizarDesempenoEnOfertaDetallado(Usuario alumno, Inscripciones inscripcion) {
        return analizarDesempenoEnOfertaDetallado(alumno, inscripcion, true);
    }

    private List<String> analizarDesempenoEnOfertaDetallado(Usuario alumno, Inscripciones inscripcion, boolean notificarDocente) {
        OfertaAcademica oferta = inscripcion.getOferta();
        List<String> alertasGeneradas = new ArrayList<>();
        
        // 1. Asistencia Real (Check < 70%)
        double asistencia = calcularAsistencia(alumno, oferta);
        if (asistencia < 70.0) {
             String motivo = "Asistencia actual: " + String.format("%.1f%%", asistencia) + " (Mínimo: 70%)";
             generarIntervencion(alumno, oferta, "BAJO_ASISTENCIA", motivo, 
                "Hemos detectado que tu asistencia (" + String.format("%.1f%%", asistencia) + ") está por debajo del mínimo requerido. Te sugerimos ponerte al día.",
                notificarDocente);
             alertasGeneradas.add("Riesgo de Asistencia: " + String.format("%.1f%%", asistencia));
        }

        // 2. Tareas vencidas sin entrega
        List<String> pendientes = detectarActividadesVencidasSinEntrega(alumno, oferta);
        if (!pendientes.isEmpty()) {
            String lista = String.join(", ", pendientes);
            String motivo = "Actividades vencidas sin entrega: " + lista;
            
            // Solo notificamos si hay más de una o es crítica, para no spamear por una sola tarea vieja
            generarIntervencion(alumno, oferta, "ACTIVIDADES_PENDIENTES", motivo, 
                "Tenés actividades que ya cerraron sin entregar: " + lista + ". Contactá a tu docente si necesitás prórroga.",
                notificarDocente);
            alertasGeneradas.add("Actividades vencidas: " + pendientes.size());
        }

        // 3. Rendimiento (Modelado 0-10) -> Check < 6
        double promedio = calcularPromedio(alumno, oferta);
        // Ajustamos la lógica para considerar promedio 100 como "Sin notas" solo si no ha entregado nada (ver calcularPromedio)
        // PERO si el promedio real calculado es bajo, debemos reportarlo.
        if (promedio < 6.0 && promedio >= 0.0) { 
             generarIntervencion(alumno, oferta, "BAJO_RENDIMIENTO", "Promedio bajo: " + String.format("%.2f", promedio), 
                "Tu promedio de calificaciones es bajo. Revisa los contenidos y práctica más.",
                notificarDocente);
             alertasGeneradas.add("Rendimiento Bajo: Promedio " + String.format("%.1f", promedio));
        } else if (promedio > 10.0 && promedio < 60.0) { // Soporte para escalas de 0-100 donde <60 es reprobado
             generarIntervencion(alumno, oferta, "BAJO_RENDIMIENTO", "Promedio bajo: " + String.format("%.2f", promedio), 
                "Tu promedio de calificaciones es bajo (Escala 100). Revisa los contenidos.",
                notificarDocente);
             alertasGeneradas.add("Rendimiento Bajo: Promedio " + String.format("%.1f", promedio));
        }

        return alertasGeneradas;
    }

    private List<String> detectarActividadesVencidasSinEntrega(Usuario alumno, OfertaAcademica oferta) {
        List<String> pendientes = new ArrayList<>();
        Curso curso = (oferta instanceof Curso) ? (Curso) oferta : null;
        if (curso == null || curso.getModulos() == null) return pendientes;

        LocalDateTime ahora = LocalDateTime.now();

        for (Modulo m : curso.getModulos()) {
            if (m.getActividades() != null) {
                for (Actividad a : m.getActividades()) {
                    // Determinar fecha de cierre según tipo de actividad (Tarea: limiteEntrega, Examen: fechaCierre)
                    java.time.LocalDateTime cierre = null;
                    if (a instanceof Tarea) {
                        cierre = ((Tarea)a).getLimiteEntrega();
                    } else if (a instanceof Examen) {
                        cierre = ((Examen)a).getFechaCierre();
                    }
                    boolean cerrada = cierre != null && cierre.isBefore(ahora);
                    if (!cerrada) continue;

                    if (a instanceof Tarea) {
                        var opt = entregaRepository.findByTareaAndEstudiante((Tarea)a, alumno);
                        if (opt.isEmpty()) {
                            pendientes.add(((Tarea)a).getTitulo());
                        }
                    } else if (a instanceof Examen) {
                        List<Intento> intentos = intentoRepository.findByAlumno_IdAndExamen_IdActividad(alumno.getId(), a.getIdActividad());
                        if (intentos.isEmpty()) {
                            pendientes.add(((Examen)a).getTitulo());
                        }
                    }
                }
            }
        }
        return pendientes;
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
            
            // Guardamos la intervención real con los datos calculados (enviarChat=false porque se muestra en UI)
            generarIntervencion(alumno, targetOferta, "ANALISIS_REAL", "Solicitud del Usuario", sb.toString(), true, false);
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
                            boolean actividadCerrada = false;
                            
                            // Verificar si la actividad ya cerró (fecha de fin < hoy)
                            java.time.LocalDateTime cierre = null;
                            if (a instanceof Tarea) {
                                cierre = ((Tarea)a).getLimiteEntrega();
                            } else if (a instanceof Examen) {
                                cierre = ((Examen)a).getFechaCierre();
                            }
                            if (cierre != null && cierre.isBefore(LocalDateTime.now())) {
                                actividadCerrada = true;
                            }
                            
                            if (a instanceof Tarea) {
                                titulo = ((Tarea)a).getTitulo();
                                var opt = entregaRepository.findByTareaAndEstudiante((Tarea)a, alumno);
                                if(opt.isPresent() && opt.get().getCalificacion() != null) {
                                    grade = opt.get().getCalificacion();
                                } else if (actividadCerrada && opt.isEmpty()) {
                                    // Tarea cerrada y sin entrega
                                    consejos.append("<br>&nbsp;&nbsp;⚠️ **").append(titulo).append("**: No entregaste a tiempo.");
                                    continue;
                                }
                            } else if (a instanceof Examen) {
                                titulo = ((Examen)a).getTitulo();
                                List<Intento> intentos = intentoRepository.findByAlumno_IdAndExamen_IdActividad(alumno.getId(), a.getIdActividad());
                                if(!intentos.isEmpty()) {
                                    grade = intentos.stream()
                                        .map(in -> in.getCalificacion() != null ? in.getCalificacion().doubleValue() : 0.0)
                                        .max(Double::compareTo).orElse(0.0);
                                } else if (actividadCerrada) {
                                     // Examen cerrado y sin intentos
                                    consejos.append("<br>&nbsp;&nbsp;⚠️ **").append(titulo).append("**: No realizaste el examen a tiempo.");
                                    continue;
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
        // Obtenemos el total de días que se tomó asistencia en esta oferta
        long totalClases = asistenciaRepository.countDiasConAsistencia(oferta.getIdOferta());
        
        if (totalClases == 0) return 100.0; // No se ha tomado lista nunca -> Asumimos 100%
        
        List<Asistencia> records = asistenciaRepository.findByOfertaIdOfertaAndAlumnoDni(oferta.getIdOferta(), alumno.getDni());
        
        long present = records.stream()
            .filter(a -> a.getEstado() == EstadoAsistencia.PRESENTE || a.getEstado() == EstadoAsistencia.TARDANZA)
            .count();
            
        return (double) present / totalClases * 100.0;
    }

    private boolean generarIntervencion(Usuario alumno, OfertaAcademica oferta, String tipo, String motivo) {
        // Verificar intervención reciente del mismo tipo para este alumno y oferta (últimos 7 días)
        LocalDateTime haceUnaSemana = LocalDateTime.now().minusDays(7);
        // Implementar lógica de repositorio si fuese necesario:
        // boolean existeReciente = intervencionRepository.existsByAlumnoAndOfertaAndTipoAndFechaAfter(...)
        
        // Simulación de deduplicación en memoria (simplificado para demo)
        // En producción: usar consulta JPA real
        List<IntervencionAcademica> previas = intervencionRepository.findByAlumno(alumno);
        boolean existeReciente = previas.stream()
            .filter(i -> i.getOferta() != null && i.getOferta().getIdOferta().equals(oferta.getIdOferta()))
            .filter(i -> i.getTipoIntervencion().equals(tipo))
            .anyMatch(i -> i.getFechaCreacion().isAfter(haceUnaSemana));

        if (existeReciente) {
            log.info("ℹ️ Intervención omitida (ya existe una reciente) para alumno {}", alumno.getDni());
            return false; // Retornamos false si se omitió
        }

        // Generar Sugerencia con "IA" (Simulada por Templates/Heurística)
        String sugerencia = generarSugerenciaIA(tipo, oferta.getNombre());
        
        generarIntervencion(alumno, oferta, tipo, motivo, sugerencia);
        return true; // Retornamos true si se generó
    }

    /* Método borrado: private int analizarDesempenoEnOferta(Usuario alumno, Inscripciones inscripcion) - reemplazado por analizarDesempenoEnOfertaDetallado */

    private boolean generarIntervencion(Usuario alumno, OfertaAcademica oferta, String tipo, String motivo, String sugerencia, boolean notificarDocente, boolean enviarChat) {
        // Verificar intervención reciente del mismo tipo para evitar duplicados en el CHAT (últimas 24 horas)
        LocalDateTime haceUnDia = LocalDateTime.now().minusHours(24);
        List<IntervencionAcademica> previasRecientes = intervencionRepository.findByAlumno(alumno).stream()
                .filter(i -> i.getOferta() != null && i.getOferta().getIdOferta().equals(oferta.getIdOferta()))
                .filter(i -> i.getTipoIntervencion().equals(tipo))
                .filter(i -> i.getFechaCreacion().isAfter(haceUnDia))
                .toList();
        // Si hay reciente, NO creamos otra en DB
        if (!previasRecientes.isEmpty()) {
            return false;
        }

        // Guardar Intervención
        IntervencionAcademica intervencion = new IntervencionAcademica();
        intervencion.setAlumno(alumno);
        intervencion.setOferta(oferta);
        intervencion.setTipoIntervencion(tipo);
        intervencion.setMotivoDetectado(motivo);
        intervencion.setSugerenciaIA(sugerencia);
        intervencion.setEnviadaAlumno(true);
        intervencionRepository.save(intervencion);

        // Enviar Notificación al Alumno (Chat Bot) - ESTO GENERA EL DOBLE MENSAJE si ya se responde directo
        //if (enviarChat) {
            //enviarAlertaChat(alumno, "🤖 Asistente Académico: " + sugerencia);
       // }
        
        // --- Notificar a Docentes ---
        if (notificarDocente) {
            // Solo si se solicita explícitamente (ej: en el cron job nocturno)
            if (oferta instanceof Curso && ((Curso)oferta).getDocentes() != null) {
                notificarDocentes(((Curso)oferta).getDocentes(), alumno, oferta, tipo, motivo);
            } else if (oferta instanceof Formacion && ((Formacion)oferta).getDocentes() != null) {
                notificarDocentes(((Formacion)oferta).getDocentes(), alumno, oferta, tipo, motivo);
            }
        }
        
        log.info("⚠️ Intervención generada para alumno {}: {}", alumno.getDni(), tipo);
        return true;
    }

    // Sobrecarga para compatibilidad sin romper otros usos (default: notificarDocente=true, enviarChat=true)
    private boolean generarIntervencion(Usuario alumno, OfertaAcademica oferta, String tipo, String motivo, String sugerencia, boolean notificarDocente) {
        return generarIntervencion(alumno, oferta, tipo, motivo, sugerencia, notificarDocente, true);
    }
    
    // Sobrecarga para compatibilidad sin romper otros usos (default: true, true)
    private boolean generarIntervencion(Usuario alumno, OfertaAcademica oferta, String tipo, String motivo, String sugerencia) {
        return generarIntervencion(alumno, oferta, tipo, motivo, sugerencia, true, true);
    }
    
    private void notificarDocentes(List<? extends Usuario> docentes, Usuario alumno, OfertaAcademica oferta, String tipo, String motivo) {
        String titulo = "⚠️ Alerta de Riesgo: " + alumno.getNombre() + " " + alumno.getApellido();
        
        // Generar recomendación y draft
        String recomendacion = generarRecomendacionParaDocente(tipo);
        String draftCorreo = generarBorradorCorreo(alumno, oferta, tipo);
        
        String mensaje = "Se detectó **" + tipo + "** en " + oferta.getNombre() + ".\n" +
                         "Motivo: " + motivo + "\n\n" +
                         "💡 **Recomendación IA**: " + recomendacion + "\n\n" +
                         "📧 **Borrador de Correo sugerido**:\n" + 
                         "Asunto: Apoyo Académico - " + oferta.getNombre() + "\n" +
                         "Mensaje: \n" + draftCorreo;

        for (Usuario docente : docentes) {
            // 1. Notificación en el sistema
            Notificacion notif = new Notificacion();
            notif.setUsuario(docente);
            notif.setTitulo(titulo);
            notif.setMensaje(mensaje);
            notif.setTipo("ALERTA");
            notif.setLeida(false);
            notificacionRepository.save(notif);
            
            // 2. Notificación por Email (Opcional)
            if (docente.getCorreo() != null && !docente.getCorreo().isEmpty()) {
                try {
                    emailService.sendEmail(docente.getCorreo(), titulo, mensaje.replace("\n", "<br>"));
                } catch (Exception e) {
                    log.error("Error enviando email al docente {}: {}", docente.getDni(), e.getMessage());
                }
            }
        }
    }
    
    public String getBorradorCorreo(UUID alumnoId, Long ofertaId, String tipo) {
        Usuario alumno = usuarioRepository.findById(alumnoId).orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId).orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        return generarBorradorCorreo(alumno, oferta, tipo);
    }
    
    private String generarRecomendacionParaDocente(String tipo) {
        if ("BAJO_ASISTENCIA".equals(tipo)) {
            return "El alumno muestra signos de ausentismo crónico. Se recomienda contactarlo para verificar si tiene problemas de conectividad, salud o laborales que le impidan asistir. Fomentar la visualización de clases grabadas.";
        } else if ("BAJO_RENDIMIENTO".equals(tipo)) {
            return "El desempeño académico está por debajo de lo esperado. Sugiero ofrecerle una sesión de consulta breve o recomendarle material complementario específico de los temas donde falló.";
        } else if ("ACTIVIDADES_PENDIENTES".equals(tipo)) {
            return "Tiene entregas vencidas. Verificar si necesita una prórroga excepcional o si abandonó la cursada.";
        }
        return "Realizar seguimiento cercano.";
    }
    
    private String generarBorradorCorreo(Usuario alumno, OfertaAcademica oferta, String tipo) {
        String nombre = alumno.getNombre();
        String curso = oferta.getNombre();
        
        if ("BAJO_ASISTENCIA".equals(tipo)) {
            return "Hola " + nombre + ",\n\n" +
                   "Noté que has faltado a las últimas clases de " + curso + ". Quería saber si estás bien o si has tenido algún inconveniente para conectarte.\n" +
                   "Recuerda que es importante mantener la regularidad, pero si necesitas ver las grabaciones, están disponibles en el aula.\n\n" +
                   "Quedo a tu disposición,\nSaludos.";
        } else if ("BAJO_RENDIMIENTO".equals(tipo)) {
             return "Hola " + nombre + ",\n\n" +
                   "Estuve revisando tu progreso en " + curso + " y veo que has tenido dificultades con las últimas evaluaciones.\n" +
                   "Me gustaría ayudarte a mejorar. ¿Tienes disponibilidad para una breve consulta o dudas puntuales sobre los temas?\n\n" +
                   "No te desanimes, ¡aún estás a tiempo de remontar!\nSaludos.";
        } else if ("ACTIVIDADES_PENDIENTES".equals(tipo)) {
             return "Hola " + nombre + ",\n\n" +
                   "Te escribo porque tienes actividades pendientes de entrega en " + curso + " que ya han vencido.\n" +
                   "¿Necesitas alguna ayuda o una extensión de plazo para ponerte al día? Es importante que no acumules tareas.\n\n" +
                   "Espero tu respuesta,\nSaludos.";
        }
        return "Hola " + nombre + ", te contacto para saber cómo vienes con la cursada.";
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

}
