package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.example.demo.model.Instituto;
import com.example.demo.enums.EstadoOferta;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

@Service
public class AnalisisRendimientoService {

    private static final Logger log = LoggerFactory.getLogger(AnalisisRendimientoService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private InstitutoService institutoService;

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
    private AuditLogService auditLogService;
    
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

    // Se ejecuta todos los días a las 06:00 AM para verificar bajas automáticas
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void ejecutarAnalisisDeBajaAutomatica() {
        log.info("📉 Iniciando Análisis de Baja Automática de Ofertas...");
        
        Instituto instituto = institutoService.obtenerInstituto();
        // Verificar si la baja automatica esta activada
        if (!Boolean.TRUE.equals(instituto.getPermisoBajaAutomatica())) {
            log.info("⏹️ Baja automática desactivada en configuración institucional.");
            return;
        }

        int minAlumnos = instituto.getMinimoAlumnoBaja() != null ? instituto.getMinimoAlumnoBaja() : 5;
        int diasInactividad = instituto.getInactividadBaja() != null ? instituto.getInactividadBaja() : 30;

        List<OfertaAcademica> ofertasAnalizar = ofertaAcademicaRepository.findByEstadoIn(
            Arrays.asList(EstadoOferta.ACTIVA, EstadoOferta.ENCURSO)
        );

        for (OfertaAcademica oferta : ofertasAnalizar) {
            boolean darDeBaja = false;
            String motivoBaja = "";
            // Usamos la lista ya existente en el repo, optimizable con count query
            List<Inscripciones> ins = inscripcionRepository.findByOfertaAndEstadoInscripcionTrue(oferta);
            long inscripcionesCount = (ins != null) ? ins.size() : 0;

            // 1. Análisis de Inicio Inminente (Solo para ACTIVA, aun no empezó o está por empezar)
            // "falte de 3 a 1 dia para que comience"
            if (oferta.getEstado() == EstadoOferta.ACTIVA && oferta.getFechaInicio() != null) {
                long diasParaInicio = ChronoUnit.DAYS.between(LocalDate.now(), oferta.getFechaInicio());
                if (diasParaInicio >= 1 && diasParaInicio <= 3) {
                     if (inscripcionesCount <= minAlumnos) {
                         darDeBaja = true;
                         motivoBaja = "Inicio inminente (" + diasParaInicio + " días) con matrícula insuficiente (" + inscripcionesCount + " inscritos. Mínimo: " + minAlumnos + ")";
                     }
                }
            }

            // 2. Análisis de Matrícula e Inactividad (Solo para ENCURSO)
            if (!darDeBaja && oferta.getEstado() == EstadoOferta.ENCURSO) {
                 // "minimo de inscriptos"
                 if (inscripcionesCount < minAlumnos) {
                      darDeBaja = true;
                      motivoBaja = "Matrícula insuficiente durante el cursado (" + inscripcionesCount + " inscritos. Mínimo: " + minAlumnos + ")";
                 } else {
                     // Inactividad de entregas
                     List<Tarea> tareasCurso = new ArrayList<>();
                     if (oferta instanceof Curso) {
                         Curso c = (Curso) oferta;
                         if (c.getModulos() != null) {
                             c.getModulos().forEach(m -> {
                                 if (m.getActividades() != null) {
                                     m.getActividades().stream()
                                         .filter(a -> a instanceof Tarea)
                                         .map(a -> (Tarea) a)
                                         .forEach(tareasCurso::add);
                                 }
                             });
                         }
                     }
                     // Note: "Formacion" in this model does not contain nested Cursos;
                     // inactivity-by-tasks validation applies only to Curso instances.
                     
                     // Si la oferta REQUIERE tareas (tiene tareas configuradas), validamos inactividad
                     if (!tareasCurso.isEmpty()) {
                         LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasInactividad);
                         long entregasRecientes = entregaRepository.countByTareaInAndFechaEntregaAfter(tareasCurso, fechaLimite);
                         if (entregasRecientes == 0) {
                             darDeBaja = true;
                             motivoBaja = "Inactividad: Ninguna entrega de tareas en los últimos " + diasInactividad + " días.";
                         }
                     }
                 }
            }

            if (darDeBaja) {
                log.warn("⚠️ BAJA AUTOMÁTICA para oferta {}: {}", oferta.getNombre(), motivoBaja);
                oferta.setEstado(EstadoOferta.DE_BAJA);
                ofertaAcademicaRepository.save(oferta);

                // Registrar auditoría de baja automática
                try {
                    long now = System.currentTimeMillis();
                    com.example.demo.model.AuditLog audit = new com.example.demo.model.AuditLog();
                    audit.setFecha(new java.sql.Date(now));
                    audit.setHora(new java.sql.Time(now));
                    audit.setUsuario(null);
                    audit.setRol(null);
                    audit.setAccion("BAJA_AUTOMATICA_OFERTA");
                    audit.setAfecta("OfertaAcademica");
                    audit.setDetalles("ID:" + oferta.getIdOferta() + " | Nombre:" + oferta.getNombre() + " | Motivo:" + motivoBaja);
                    audit.setExito(true);
                    audit.setIp("SYSTEM");
                    auditLogService.registrar(audit);
                } catch (Exception e) {
                    log.error("Error registrando auditoría de baja automática: {}", e.getMessage());
                }
                
                // Notificar docentes (si aplica)
                List<? extends Usuario> docentes = new ArrayList<>();
                if (oferta instanceof Curso) {
                    docentes = ((Curso)oferta).getDocentes();
                } else if (oferta instanceof Formacion) {
                    docentes = ((Formacion)oferta).getDocentes();
                }
                
                if(docentes != null) {
                    for(Usuario doc : docentes) {
                        if (doc.getCorreo() != null) {
                            emailService.sendEmail(doc.getCorreo(), 
                                "Baja Automática de Oferta: " + oferta.getNombre(), 
                                "La oferta académica ha sido dada de baja automáticamente por el sistema.<br>" +
                                "<b>Motivo:</b> " + motivoBaja + "<br>" +
                                "Si considera que es un error, contacte con administración para reactivarla.");
                        }
                    }
                }
            }
        }
        log.info("✅ Análisis de baja finalizado.");
    }

    /**
     * Estructura DTO para exponer las ofertas en peligro al frontend
     */
    public static record OfertaRiesgo(Long id, String nombre, String motivo, long inscripcionesCount, String estado, java.time.LocalDate fechaInicio, int tareasCount) {}

    /**
     * DTO para el resultado de baja manual
     */
    public static record BajaResultado(Long id, boolean success, String motivo, String nombre) {}

    /**
     * Devuelve la lista de ofertas que cumplirían las condiciones de baja según las reglas
     * (NO aplica cambios en BD). Uso para mostrar modal al admin.
     */
    @Transactional(readOnly = true)
    public List<OfertaRiesgo> obtenerOfertasEnPeligro() {
        List<OfertaAcademica> ofertasAnalizar = ofertaAcademicaRepository.findByEstadoIn(
            Arrays.asList(EstadoOferta.ACTIVA, EstadoOferta.ENCURSO)
        );

        Instituto instituto = institutoService.obtenerInstituto();
        int minAlumnos = instituto.getMinimoAlumnoBaja() != null ? instituto.getMinimoAlumnoBaja() : 5;
        int diasInactividad = instituto.getInactividadBaja() != null ? instituto.getInactividadBaja() : 30;

        List<OfertaRiesgo> resultado = new ArrayList<>();

        for (OfertaAcademica oferta : ofertasAnalizar) {
            boolean darDeBaja = false;
            String motivoBaja = "";
            List<Inscripciones> ins = inscripcionRepository.findByOfertaAndEstadoInscripcionTrue(oferta);
            long inscripcionesCount = (ins != null) ? ins.size() : 0;

            if (oferta.getEstado() == EstadoOferta.ACTIVA && oferta.getFechaInicio() != null) {
                long diasParaInicio = ChronoUnit.DAYS.between(LocalDate.now(), oferta.getFechaInicio());
                if (diasParaInicio >= 1 && diasParaInicio <= 3) {
                     if (inscripcionesCount <= minAlumnos) {
                         darDeBaja = true;
                         motivoBaja = "Inicio inminente (" + diasParaInicio + " días) con matrícula insuficiente (" + inscripcionesCount + " inscritos. Mínimo: " + minAlumnos + ")";
                     }
                }
            }

            if (!darDeBaja && oferta.getEstado() == EstadoOferta.ENCURSO) {
                 if (inscripcionesCount < minAlumnos) {
                      darDeBaja = true;
                      motivoBaja = "Matrícula insuficiente durante el cursado (" + inscripcionesCount + " inscritos. Mínimo: " + minAlumnos + ")";
                 } else {
                     List<Tarea> tareasCurso = new ArrayList<>();
                     if (oferta instanceof Curso) {
                         Curso c = (Curso) oferta;
                         if (c.getModulos() != null) {
                             c.getModulos().forEach(m -> {
                                 if (m.getActividades() != null) {
                                     m.getActividades().stream()
                                         .filter(a -> a instanceof Tarea)
                                         .map(a -> (Tarea) a)
                                         .forEach(tareasCurso::add);
                                 }
                             });
                         }
                     }

                     if (!tareasCurso.isEmpty()) {
                         LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasInactividad);
                         long entregasRecientes = entregaRepository.countByTareaInAndFechaEntregaAfter(tareasCurso, fechaLimite);
                         if (entregasRecientes == 0) {
                             darDeBaja = true;
                             motivoBaja = "Inactividad: Ninguna entrega de tareas en los últimos " + diasInactividad + " días.";
                         }
                     }
                 }
            }

            if (darDeBaja) {
                int tareasCount = 0;
                if (oferta instanceof Curso) {
                    Curso c = (Curso) oferta;
                    if (c.getModulos() != null) {
                        for (Modulo m : c.getModulos()) {
                            if (m.getActividades() != null) {
                                tareasCount += (int) m.getActividades().stream().filter(a -> a instanceof Tarea).count();
                            }
                        }
                    }
                }
                resultado.add(new OfertaRiesgo(oferta.getIdOferta(), oferta.getNombre(), motivoBaja, inscripcionesCount, oferta.getEstado().name(), oferta.getFechaInicio(), tareasCount));
            }
        }

        return resultado;
    }

    /**
     * Aplica la baja (cambia estado y notifica) para la lista de ofertas indicadas por ID.
     * Retorna lista de resultados con éxito o fallo por cada oferta.
     */
    @Transactional
    public List<BajaResultado> aplicarBajasSeleccionadas(List<Long> ofertaIds) {
        List<BajaResultado> resultados = new ArrayList<>();
        if (ofertaIds == null || ofertaIds.isEmpty()) return resultados;
        
        for (Long id : ofertaIds) {
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(id).orElse(null);
            
            if (oferta == null) {
                resultados.add(new BajaResultado(id, false, "Oferta no encontrada", "ID " + id));
                continue;
            }
            
            if (oferta.getEstado() == EstadoOferta.DE_BAJA) {
                resultados.add(new BajaResultado(id, false, "La oferta ya se encuentra dada de baja", oferta.getNombre()));
                continue;
            }

            // Validar inscripciones activas para impedir la baja
            int inscripcionesActivas = inscripcionRepository.countByOfertaAndEstadoInscripcionTrue(oferta);
            if (inscripcionesActivas > 0) {
                resultados.add(new BajaResultado(id, false, "Tiene " + inscripcionesActivas + " inscripciones activas", oferta.getNombre()));
                continue;
            }

            // Aplicar baja si no hay impedimentos
            oferta.setEstado(EstadoOferta.DE_BAJA);
            ofertaAcademicaRepository.save(oferta);

            List<? extends Usuario> docentes = new ArrayList<>();
            if (oferta instanceof Curso) {
                docentes = ((Curso)oferta).getDocentes();
            } else if (oferta instanceof Formacion) {
                docentes = ((Formacion)oferta).getDocentes();
            }

            String motivo = "Dada de baja por administración: Confirmada por el usuario desde Reportes.";

            if (docentes != null) {
                for (Usuario doc : docentes) {
                    if (doc.getCorreo() != null) {
                        try {
                            emailService.sendEmail(doc.getCorreo(), "Baja de Oferta: " + oferta.getNombre(), "La oferta ha sido dada de baja por administración.<br><b>Motivo:</b> " + motivo);
                        } catch (Exception e) {
                            log.error("Error enviando email a docente {}: {}", doc.getDni(), e.getMessage());
                        }
                    }
                }
            }
            resultados.add(new BajaResultado(id, true, "Baja aplicada correctamente", oferta.getNombre()));
        }
        return resultados;
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
    
    public void solicitarTutoria(Usuario alumno, Long ofertaId) {
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
            .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
            
        List<? extends Usuario> docentes = new ArrayList<>();
        if (oferta instanceof Curso) {
            docentes = ((Curso) oferta).getDocentes();
        } else if (oferta instanceof Formacion) {
            docentes = ((Formacion) oferta).getDocentes();
        }
        
        if (docentes == null || docentes.isEmpty()) {
            log.warn("Alumno {} solicitó tutoría para {} pero no hay docentes asignados.", alumno.getApellido(), oferta.getNombre());
            return;
        }

        String asunto = "Solicitud de Tutoría - " + oferta.getNombre();
        String cuerpoBase = "El alumno(a) <b>" + alumno.getNombre() + " " + alumno.getApellido() + "</b> (DNI: " + alumno.getDni() + ")" +
                            " ha solicitado una tutoría.<br><br>" +
                            "<b>Motivo:</b> Rendimiento bajo detectado automáticamente / Solicitud directa desde panel IA.<br>" +
                            "<b>Curso:</b> " + oferta.getNombre() + "<br><br>" +
                            "Por favor, póngase en contacto con el estudiante a la brevedad.<br>" +
                            "Correo del estudiante: " + alumno.getCorreo();

        for (Usuario docente : docentes) {
            if (docente.getCorreo() != null && !docente.getCorreo().isEmpty()) {
                emailService.sendEmail(docente.getCorreo(), asunto, cuerpoBase);
            }
             // Notificacion interna
            Notificacion notif = new Notificacion();
            notif.setUsuario(docente);
            notif.setTitulo("🆘 Solicitud de Tutoría: " + alumno.getApellido());
            notif.setMensaje("El alumno solicita ayuda en " + oferta.getNombre() + ". Revisar correo para más detalles.");
            notif.setTipo("ALERTA");
            notif.setLeida(false);
            notificacionRepository.save(notif);
        }
        log.info("📧 Solicitud de tutoría enviada a {} docentes de la oferta {}", docentes.size(), oferta.getNombre());
    }

    @Transactional
    public String analizarCurso(Long ofertaId) {
        log.info("🔎 Analizando curso ID: {}", ofertaId);
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId).orElse(null);
        if (oferta == null) return "Curso no encontrado";

        List<Inscripciones> inscripciones = inscripcionRepository.findByOfertaAndEstadoInscripcionTrue(oferta);
        int alumnosAnalizados = 0;
        
        // Estructuras para agregación
        List<String> listadoRiesgoHtml = new ArrayList<>();
        double sumaPromedios = 0;
        double sumaAsistencia = 0;
        int countConNotas = 0;
        // int countConAsistencia = 0; 
        
        int riesgoAsistencia = 0;
        int riesgoRendimiento = 0;
        int actividadesPendientes = 0;

        for (Inscripciones insc : inscripciones) {
            alumnosAnalizados++;
            Usuario alumno = insc.getAlumno();
            
            // 1. Recopilar métricas individuales para agregados
            double promedio = calcularPromedio(alumno, oferta);
            double asistencia = calcularAsistencia(alumno, oferta);
            
            if (promedio != 100.0) { // 100.0 es el valor centinela para "sin notas"
                sumaPromedios += promedio;
                countConNotas++;
            }
            sumaAsistencia += asistencia;
            
            // 2. Detectar alertas individuales
            List<String> alertas = analizarDesempenoEnOfertaDetallado(alumno, insc, false); 
            
            if (!alertas.isEmpty()) {
                StringBuilder alumnoHtml = new StringBuilder();
                alumnoHtml.append("<div style='background-color:#fff3cd; padding:10px; margin-bottom:10px; border-left:4px solid #ffc107; border-radius:4px;'>");
                alumnoHtml.append("<strong>👤 ").append(alumno.getNombre()).append(" ").append(alumno.getApellido()).append("</strong><br>");
                
                String tipoPrincipal = "GENERICO";
                for(String alerta : alertas) {
                    alumnoHtml.append("• ").append(alerta).append("<br>");
                    if(alerta.contains("Asistencia")) { riesgoAsistencia++; tipoPrincipal = "BAJO_ASISTENCIA"; }
                    if(alerta.contains("Rendimiento")) { riesgoRendimiento++; tipoPrincipal = "BAJO_RENDIMIENTO"; }
                    if(alerta.contains("vencidas")) { actividadesPendientes++; if(tipoPrincipal.equals("GENERICO")) tipoPrincipal = "ACTIVIDADES_PENDIENTES"; }
                }

                // Botón Acción
                alumnoHtml.append("<div style='margin-top:8px;'>")
                          .append("<form action='/docente/ia/preparar-correo' method='get' target='_blank' style='display:inline;'>")
                          .append("<input type='hidden' name='alumnoId' value='").append(alumno.getId()).append("'>")
                          .append("<input type='hidden' name='ofertaId' value='").append(oferta.getIdOferta()).append("'>")
                          .append("<input type='hidden' name='tipo' value='").append(tipoPrincipal).append("'>")
                          .append("<button type='submit' class='btn-accion-ia' style='background-color:#0d6efd; color:white; border:none; padding:4px 12px; border-radius:15px; font-size:12px; cursor:pointer;'>")
                          .append("📧 Redactar Aviso</button>")
                          .append("</form></div>");
                
                alumnoHtml.append("</div>");
                listadoRiesgoHtml.add(alumnoHtml.toString());
            }
        }
        
        // 3. Calcular Agregados
        double promedioGeneral = (countConNotas > 0) ? (sumaPromedios / countConNotas) : 0.0;
        double asistenciaGeneral = (alumnosAnalizados > 0) ? (sumaAsistencia / alumnosAnalizados) : 0.0;
        
        // 4. Generar Sugerencia Pedagógica
        String sugerenciaPedagogica = generarSugerenciaPedagogica(oferta.getNombre(), promedioGeneral, asistenciaGeneral, riesgoAsistencia, riesgoRendimiento, alumnosAnalizados);

        // 5. Construir Reporte Final
        StringBuilder reporte = new StringBuilder();
        reporte.append("<h3>📊 Reporte Estratégico: ").append(oferta.getNombre()).append("</h3>");
        
        // Seccion Resumen
        reporte.append("<div style='display:flex; justify-content:space-around; margin-bottom:15px; background:#f8f9fa; padding:10px; border-radius:8px;'>")
               .append("<div style='text-align:center;'><strong>").append(alumnosAnalizados).append("</strong><br><small style='color:gray'>Alumnos</small></div>")
               .append("<div style='text-align:center; color:").append(promedioGeneral < 6 && countConNotas > 0 ? "#dc3545":"#198754").append(";'><strong>").append(countConNotas > 0 ? String.format("%.1f", promedioGeneral) : "-").append("</strong><br><small style='color:gray'>Promedio</small></div>")
               .append("<div style='text-align:center; color:").append(asistenciaGeneral < 70 ? "#dc3545":"#198754").append(";'><strong>").append(String.format("%.1f%%", asistenciaGeneral)).append("</strong><br><small style='color:gray'>Asistencia</small></div>")
               .append("</div>");
               
        // Seccion IA Contextual
        reporte.append("<div style='background-color:#e7f5ff; padding:12px; border-left:4px solid #0d6efd; border-radius:4px; margin-bottom:20px;'>")
               .append("<strong>🤖 Sugerencia Pedagógica (IA):</strong><br>")
               .append(sugerenciaPedagogica)
               .append("</div>");

        // Seccion Estudiantes Prioritarios
        if (!listadoRiesgoHtml.isEmpty()) {
            reporte.append("<h5>⚠️ Alumnos que requieren atención (").append(listadoRiesgoHtml.size()).append(")</h5>");
            for(String item : listadoRiesgoHtml) {
                reporte.append(item);
            }
        } else {
            reporte.append("<div style='text-align:center; color:#198754; padding:20px;'>✅ <strong>¡Excelente!</strong> No se detectaron estudiantes en riesgo en este momento.</div>");
        }

        return reporte.toString();
    }
    
    private String generarSugerenciaPedagogica(String curso, double promedio, double asistencia, int nAsistencia, int nRendimiento, int total) {
        StringBuilder sb = new StringBuilder();
        // Analisis de Asistencia
        if (asistencia < 75.0 || (total > 0 && nAsistencia > (total * 0.3))) {
            sb.append("📉 <strong>Retención:</strong> Se detecta un nivel de asistencia preocupante. ")
              .append("Considere enviar un mensaje motivacional a todo el grupo o revisar si el horario de las sesiones síncronas es conveniente. ")
              .append("Recuerde que no se suben clases grabadas, por lo que la asistencia síncrona es vital.<br><br>");
        }
        
        // Analisis de Rendimiento
        if (promedio > 0.1 && (promedio < 6.0 || (total > 0 && nRendimiento > (total * 0.25)))) {
            sb.append("🧠 <strong>Comprensión:</strong> El promedio general es bajo. Los estudiantes podrían estar teniendo dificultades con los temas recientes. ")
              .append("<strong>Sugerencia:</strong> Programar una sesión de repaso o publicar una guía de ejercicios resueltos antes de la próxima evaluación.");
        } else if (promedio > 8.5) {
            sb.append("🚀 <strong>Desempeño:</strong> El grupo muestra un excelente dominio de los contenidos. ")
              .append("<strong>Sugerencia:</strong> Proponer desafíos opcionales avanzados para mantener el interés de los estudiantes más destacados.");
        } else {
            sb.append("📈 <strong>Progreso:</strong> El rendimiento es estable. Continúe con la planificación actual, pero monitoree a los casos puntuales.");
        }
        return sb.toString();
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
    public String analizarAlumnoCompleto(Usuario alumno) {
        StringBuilder html = new StringBuilder();
        
        // --- Integración del Análisis General (Método Anterior) ---
        // Llamamos a analizarAlumnoForce para:
        // 1. Obtener el resumen textual del "peor caso".
        // 2. Generar/Guardar la Intervención en Base de Datos.
        String resumenTexto = analizarAlumnoForce(alumno);
        
        // Formatear el resumen Markdown a HTML básico
        String resumenHtml = resumenTexto
            .replace("\n", "<br>")
            .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");

        html.append("<div class='space-y-6'>");
        
        // Cabecera Resumen Global (Con el texto del análisis force)
        html.append("<div class='bg-white p-4 rounded shadow-sm border-l-4 border-indigo-500'>");
        html.append("<h4 class='text-lg font-bold text-gray-800'>🎓 Tu Análisis de Rendimiento</h4>");
        html.append("<div class='mt-2 p-3 bg-indigo-50 text-indigo-900 rounded text-sm'>").append(resumenHtml).append("</div>");
        html.append("</div>");

        List<Inscripciones> inscripciones = inscripcionRepository.findByAlumno(alumno);
        // Filtramos solo cursos/formaciones activas
        List<Inscripciones> validas = inscripciones == null ? List.of() : inscripciones.stream()
            .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
            .filter(i -> (i.getOferta() instanceof Curso) || (i.getOferta() instanceof Formacion))
            .toList();

        if (validas.isEmpty()) {
            return "<div class='p-4 text-center'>No tienes cursadas activas para analizar.</div>";
        }

        // Detalle por Curso
        for (Inscripciones insc : validas) {
            OfertaAcademica oferta = insc.getOferta();
            double promedio = calcularPromedio(alumno, oferta);
            double asistencia = calcularAsistencia(alumno, oferta);
            List<String> pendientes = detectarActividadesVencidasSinEntrega(alumno, oferta);
            
            // Determinar estado visual
            boolean lowGrade = (promedio < 6.0 || (promedio > 10.0 && promedio < 60.0)) && promedio != 100.0;
            // Riesgo si hay promedio bajo, asistencia baja, pendientes O si hay consejos detallados (ej. nota baja en examen especifico aunque promedio ok)
            boolean riesgo = lowGrade || asistencia < 75.0 || !pendientes.isEmpty() || !getConsejosPersonalizados(alumno, oferta).isEmpty();
            String borderClass = riesgo ? "border-red-400" : "border-green-400";
            String bgClass = riesgo ? "bg-red-50" : "bg-green-50";

            html.append("<div class='card shadow-sm mb-3 border ").append(borderClass).append("'>");
            html.append("<div class='card-header ").append(bgClass).append(" font-bold'>").append(oferta.getNombre()).append("</div>");
            html.append("<div class='card-body'>");
            
            // Grid de Métricas
            html.append("<div class='row text-center mb-3'>");
            // Promedio
            html.append("<div class='col-4'>");
            html.append("<h5 class='").append(lowGrade ? "text-danger" : "text-success").append(" font-bold'>").append(promedio == 100.0 ? "-" : String.format("%.1f", promedio)).append("</h5>");
            html.append("<small class='text-muted'>Promedio</small>");
            html.append("</div>");
            // Asistencia
            html.append("<div class='col-4'>");
            html.append("<h5 class='").append(asistencia < 70.0 ? "text-danger" : "text-success").append(" font-bold'>").append(String.format("%.1f%%", asistencia)).append("</h5>");
            html.append("<small class='text-muted'>Asistencia</small>");
            html.append("</div>");
            // Entregas Pendientes
            html.append("<div class='col-4'>");
            html.append("<h5 class='").append(!pendientes.isEmpty() ? "text-danger" : "text-success").append(" font-bold'>").append(pendientes.size()).append("</h5>");
            html.append("<small class='text-muted'>Pendientes</small>");
            html.append("</div>");
            html.append("</div>"); // End row
            
            // Recomendaciones Específicas
            html.append("<div class='mt-3 bg-light p-3 rounded'>");
            html.append("<h6>💡 Recomendaciones:</h6>");
            html.append("<ul class='list-unstyled mb-0 text-sm'>");
            
            boolean algunaRecomendacion = false;
            // Detectamos promedio bajo considerando escalas 0-10 (<6) y 0-100 (<60)
            boolean promedioBajo = (promedio < 6.0 || (promedio > 10.0 && promedio < 60.0)) && promedio != 100.0;
                        // Integración de Consejos Personalizados por Actividad (Detalle Específico)
            String consejosDetallados = getConsejosPersonalizados(alumno, oferta);
            if (!consejosDetallados.isEmpty()) {
                // Convertimos el string markdown-like del metodo a HTML simple si es necesario
                // El metodo devuelve saltos <br> y **negritas**, es compatible.
                html.append("<li class='mb-2'><strong>Detalles específicos:</strong>").append(consejosDetallados).append("</li>");
                algunaRecomendacion = true;
            }
            if (promedioBajo) {
                // Busqueda de proximo examen
                Examen nextExam = null;
                Modulo nextExamModule = null;
                if (oferta instanceof Curso) {
                    Curso c = (Curso) oferta;
                    if (c.getModulos() != null) {
                        LocalDateTime now = LocalDateTime.now();
                        for (Modulo m : c.getModulos()) {
                            if (m.getActividades() != null) {
                                for (Actividad a : m.getActividades()) {
                                    if (a instanceof Examen) {
                                        Examen ex = (Examen) a;
                                        if (ex.getFechaCierre() != null && ex.getFechaCierre().isAfter(now)) {
                                            if (nextExam == null || ex.getFechaCierre().isBefore(nextExam.getFechaCierre())) {
                                                nextExam = ex;
                                                nextExamModule = m;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (nextExam != null) {
                     html.append("<li class='mb-2'>📅 <strong>Próximo Examen:</strong> Se aproxima <u>").append(nextExam.getTitulo()).append("</u> (Cierra: ").append(nextExam.getFechaCierre().toLocalDate()).append(").<br>")
                         .append("⚠️ Tu promedio es bajo, ¡es clave que apruebes este examen!<br>")
                         .append("📝 <strong>Temas a evaluar:</strong> ").append(nextExamModule.getTemario() != null ? nextExamModule.getTemario() : "Contenidos del módulo.").append("<br>")
                         .append("📚 <strong>Material de estudio:</strong> ").append(nextExamModule.getBibliografia() != null ? nextExamModule.getBibliografia() : "Recursos del aula virtual.").append("<br>")
                         .append("💡 <strong>Sugerencia de estudio:</strong> Revisa los objetivos del módulo: <em>").append(nextExamModule.getObjetivos() != null ? nextExamModule.getObjetivos() : "Completar lecturas y prácticos").append("</em>.</li>");
                } else {
                    html.append("<li class='mb-2'>📚 <strong>Material de Estudio:</strong> Tu promedio es bajo. Te recomendamos repasar los módulos anteriores y realizar los tests de autoevaluación.</li>");
                }
                algunaRecomendacion = true;
            }
            if (asistencia < 75.0) {
                html.append("<li class='mb-2'>📹 <strong>Clases en Vivo:</strong> Tu asistencia es baja (").append(String.format("%.1f", asistencia)).append("%). Recuerda que las clases no se graban, ¡es fundamental que asistas!</li>");
                algunaRecomendacion = true;
            }
            if (!pendientes.isEmpty()) {
                html.append("<li class='mb-2'>⚠️ <strong>Entregas:</strong> Prioriza subir: ").append(pendientes.stream().limit(2).collect(Collectors.joining(", ")));
                if (pendientes.size() > 2) html.append(" y otras...");
                html.append(".</li>");
                algunaRecomendacion = true;
            }
            
            if (!algunaRecomendacion) {
                if (promedio >= 9.0) {
                    html.append("<li>🌟 <strong>¡Excelente trabajo!</strong> Estás llevando la materia al día. ¿Te interesaría ayudar a compañeros en el foro?</li>");
                } else {
                    html.append("<li>✅ <strong>Todo en orden:</strong> Mantén este ritmo. Revisa regularmente las novedades del curso.</li>");
                }
            }
            html.append("</ul>");
            html.append("</div>");

            // Botones de acción (Simulados)
            html.append("<div class='mt-3 d-flex gap-2 justify-content-end'>");
            if (lowGrade || !consejosDetallados.isEmpty()) {
                 html.append("<button onclick='(window.aiChat || window.simpleChatInstance).solicitarTutoria(").append(oferta.getIdOferta()).append(")' class='btn btn-sm btn-warning text-white font-weight-bold' style='background-color:rgb(245, 158, 11); border:none;'>📧 Solicitar Tutoría</button>");
            }
            html.append("<a href='/alumno/aula/").append(oferta.getIdOferta()).append("' class='btn btn-sm btn-primary'>Ir al Curso</a>");
            html.append("</div>");

            html.append("</div></div>");
        }
        
        html.append("</div>"); // End space-y-6
        return html.toString();
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
                } else if ((notaPeor < 6.0 || (notaPeor > 10.0 && notaPeor < 60.0)) && notaPeor != 100.0) {
                    sb.append("<br>⚠️ Tu promedio es bajo. Aquí tienes algunas recomendaciones:");
                    sb.append(getConsejosPersonalizados(alumno, targetOferta));
                    
                    // Botón para solicitar tutoría
                    sb.append("<br><div style='margin-top: 10px;'>");
                    sb.append("<button onclick=\"(window.aiChat || window.simpleChatInstance).solicitarTutoria(").append(targetOferta.getIdOferta()).append(")\" ");
                    sb.append("style=\"background-color: #f59e0b; color: white; padding: 8px 12px; border: none; border-radius: 5px; cursor: pointer; font-weight: bold; font-size: 0.9em; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\">");
                    sb.append("📧 Solicitar Tutoría con Docente");
                    sb.append("</button>");
                    sb.append("</div>");
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
                                    consejos.append("<br>&nbsp;&nbsp;⚠️ <strong>").append(titulo).append("</strong>: No entregaste a tiempo.");
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
                                    consejos.append("<br>&nbsp;&nbsp;⚠️ <strong>").append(titulo).append("</strong>: No realizaste el examen a tiempo.");
                                    continue;
                                }
                            }
                            
                            // Check logic: < 6 or (< 60 && > 10)
                            if (grade != null) {
                                if (grade < 6.0 || (grade > 10.0 && grade < 60.0)) {
                                    consejos.append("<br>&nbsp;&nbsp;❌ <strong>").append(titulo).append("</strong> (Nota: ").append(grade).append("): ");
                                    consejos.append("Te sugiero repasar el contenido del <strong>").append(m.getNombre()).append("</strong> relacionado con esta actividad.");
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
            return "El alumno muestra signos de ausentismo crónico. Se recomienda contactarlo para verificar si tiene problemas de conectividad, salud o laborales. Recordarle que no se dispone de grabaciones.";
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
                   "Te recuerdo que en este curso no se utilizan grabaciones, por lo que la asistencia a los encuentros en vivo es muy importante para no perder el hilo.\n\n" +
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
                   "Recuerda que las clases son 100% síncronas y no quedan grabadas. " +
                   "¿Necesitas ayuda para ponerte al día con los apuntes?";
        } else if ("BAJO_RENDIMIENTO".equals(tipo)) {
            return "Hola! Parece que " + nombreCurso + " se está poniendo difícil. " +
                   "Te recomiendo revisar el Módulo 3 y hacer los ejercicios de práctica nuevamente. " +
                   "¡No te desanimes, venías muy bien!";
        }
        return "Hola! Estoy aquí para ayudarte con tu cursada.";
    }

}
