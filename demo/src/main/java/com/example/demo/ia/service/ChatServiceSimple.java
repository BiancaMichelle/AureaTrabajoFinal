package com.example.demo.ia.service;

import com.example.demo.ia.config.IAConfig;
import com.example.demo.ia.model.ChatMessage;
import com.example.demo.ia.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import com.example.demo.repository.*;
import com.example.demo.model.*;
import com.example.demo.enums.EstadoCuota;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatServiceSimple {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private com.example.demo.repository.OfertaAcademicaRepository ofertaAcademicaRepository;
    
    @Autowired
    private IAConfig iaConfig;
    
    @Autowired
    private com.example.demo.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private com.example.demo.repository.InscripcionRepository inscripcionRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private ExamenRepository examenRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private com.example.demo.repository.CursoRepository cursoRepository;

    @Autowired
    private com.example.demo.repository.IntentoRepository intentoRepository;

    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final int MAX_MESSAGES_PER_HOUR = 50;
    private static final int MAX_CONTEXT_MESSAGES = 10;
    private static final int MAX_INTENTOS_INSULTOS = 3;
    
    // Lista básica de palabras prohibidas
    private static final List<String> PALABRAS_PROHIBIDAS = Arrays.asList(
        "idiota", "estupido", "imbecil", "mierda", "basura", "inutil", 
        "tonto", "tarado", "maldito", "puta", "carajo", "verga", "pendejo",
        "zorra", "cabron", "chinga", "coño", "gilipollas"
    );
    
    // Regex para PII
    private static final String DNI_REGEX = "\\b\\d{7,8}\\b";
    private static final String EMAIL_REGEX = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b";
    private static final String CARD_REGEX = "\\b(?:\\d[ -]*?){13,16}\\b";

    public ChatMessage procesarMensaje(String userMessage, String userDni, String sessionId) {
        // Saneamiento de PII antes de procesar
        String mensajeSaneado = sanearMensaje(userMessage);
        System.out.println("🚀 Procesando mensaje de usuario: " + userDni + " - " + mensajeSaneado);
        
        com.example.demo.model.Usuario usuario = null;
        
        // Verificar si el usuario está bloqueado (solo si no es anónimo)
        if (!"ANONIMO".equals(userDni)) {
            usuario = usuarioRepository.findByDni(userDni).orElse(null);
            if (usuario != null && usuario.getBloqueoChatHasta() != null) {
                if (usuario.getBloqueoChatHasta().isAfter(LocalDateTime.now())) {
                    throw new RuntimeException("Tu acceso al chat está bloqueado temporalmente por conducta inapropiada hasta: " + 
                        usuario.getBloqueoChatHasta().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                } else {
                    // Desbloquear si ya pasó el tiempo
                    usuario.setBloqueoChatHasta(null);
                    usuario.setIntentosFallidosChat(0);
                    usuarioRepository.save(usuario);
                }
            }
        }

        // 1. VALIDACIÓN DE CONTENIDO INAPROPIADO
        if (contieneLenguajeInapropiado(userMessage)) {
            if (usuario != null) {
                manejarLenguajeInapropiado(usuario);
            }
            
            ChatMessage chatMessage = new ChatMessage(userDni, sessionId, "Mensaje bloqueado por contenido inapropiado");
            chatMessage.setAiResponse("⚠️ **ADVERTENCIA DE CONDUCTA**\n\n" +
                "Hemos detectado lenguaje inapropiado u ofensivo en tu mensaje. " +
                "En Aurea mantenemos un ambiente de respeto mutuo.\n\n" +
                (usuario != null ? "Advertencia " + usuario.getIntentosFallidosChat() + " de " + MAX_INTENTOS_INSULTOS + ".\n" +
                "Si continúas, tu acceso al chat será bloqueado." : ""));
            chatMessage.setResponseTimeMs(0L);
            chatMessage.setMessageType(ChatMessage.MessageType.SOPORTE_TECNICO);
            return chatMessageRepository.save(chatMessage);
        }

        // Verificar límites de uso
        if (verificarLimitesUso(userDni)) {
            throw new RuntimeException("Has excedido el límite de mensajes por hora. Intenta más tarde.");
        }
        
        // Crear mensaje del usuario (guardamos el saneado por seguridad)
        ChatMessage chatMessage = new ChatMessage(userDni, sessionId, mensajeSaneado);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Obtener contexto de la conversación
            List<Map<String, Object>> messages = construirHistorialMensajes(sessionId, mensajeSaneado, userDni);
            System.out.println("📚 Historial construido con " + messages.size() + " mensajes");
            
            // Generar respuesta de IA usando el patrón de chat de Ollama
            String aiResponse = generarRespuestaConChat(messages);
            System.out.println("✅ Respuesta generada: " + (aiResponse != null ? aiResponse.substring(0, Math.min(100, aiResponse.length())) + "..." : "null"));
            
            long endTime = System.currentTimeMillis();
            
            // Configurar la respuesta
            chatMessage.setAiResponse(aiResponse);
            chatMessage.setResponseTimeMs(endTime - startTime);
            chatMessage.setMessageType(determinarTipoMensaje(userMessage));
            
            // Guardar en base de datos (SOLO SI NO ES ANÓNIMO Y ES ALUMNO O DOCENTE) - PRIVACIDAD & SEGURIDAD
            if (!"ANONIMO".equals(userDni) && validarPermisoGuardado(usuario)) {
                return chatMessageRepository.save(chatMessage);
            } else {
                return chatMessage; // Retornar sin guardar
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en procesarMensaje: " + e.getMessage());
            e.printStackTrace();
            chatMessage.setAiResponse(generarRespuestaError(e));
            chatMessage.setResponseTimeMs(0L);
            
            // Guardar errores solo de usuarios permitidos
            if (!"ANONIMO".equals(userDni) && validarPermisoGuardado(usuario)) {
                return chatMessageRepository.save(chatMessage);
            } else {
                return chatMessage;
            }
        }
    }
    
    // Método auxiliar para verificar si se debe guardar el historial
    private boolean validarPermisoGuardado(com.example.demo.model.Usuario usuario) {
        if (usuario == null) return false;
        return usuario.getRoles().stream()
                .anyMatch(r -> "ALUMNO".equalsIgnoreCase(r.getNombre()) || 
                               "DOCENTE".equalsIgnoreCase(r.getNombre()) || 
                               "ADMIN".equalsIgnoreCase(r.getNombre()));
    }
    
    private String sanearMensaje(String mensaje) {
        if (mensaje == null) return "";
        return mensaje.replaceAll(DNI_REGEX, "[DNI_OCULTO]")
                      .replaceAll(EMAIL_REGEX, "[EMAIL_OCULTO]")
                      .replaceAll(CARD_REGEX, "[TARJETA_OCULTA]");
    }
    
    // Método auxiliar para detectar malas palabras
    private boolean contieneLenguajeInapropiado(String mensaje) {
        if (mensaje == null || mensaje.trim().isEmpty()) return false;
        
        String mensajeNormalizado = mensaje.toLowerCase();
        // Eliminar acentos para mejor detección
        mensajeNormalizado = java.text.Normalizer.normalize(mensajeNormalizado, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        for (String palabra : PALABRAS_PROHIBIDAS) {
            if (mensajeNormalizado.contains(palabra)) {
                return true;
            }
        }
        return false;
    }

    private void manejarLenguajeInapropiado(com.example.demo.model.Usuario usuario) {
        if (usuario != null) {
            int intentos = usuario.getIntentosFallidosChat() + 1;
            usuario.setIntentosFallidosChat(intentos);
            
            if (intentos >= MAX_INTENTOS_INSULTOS) {
                // Bloquear por 24 horas
                usuario.setBloqueoChatHasta(LocalDateTime.now().plusHours(24));
            }
            
            usuarioRepository.save(usuario);
        }
    }

    private List<Map<String, Object>> construirHistorialMensajes(String sessionId, String userMessage, String userDni) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // Obtener información de ofertas académicas activas para el contexto (PÚBLICO)
        String contextoOfertas = obtenerContextoOfertas();
        
        // Contexto específico del usuario (PRIVADO - solo si autenticado)
        String contextoUsuario = "";
        if (!"ANONIMO".equals(userDni)) {
            contextoUsuario = obtenerContextoUsuario(userDni);
        }
        
        String systemPrompt = "Eres un asistente académico inteligente para una plataforma educativa llamada Aurea. " +
            "IMPORTANTE: SOLO responde con información que tengas explícitamente en el contexto a continuación. " +
            "Si no sabes algo o no está en el contexto, di 'Lo siento, no tengo información sobre eso'. " +
            "NO inventes cursos, fechas ni datos. NO alucines. NO des información de relleno.\n\n" +
            "REGLAS DE INTERACCIÓN:\n" +
            "1. Si el usuario saluda, responde amablemente el saludo siempre.\n" +
            "2. MUY IMPORTANTE: Los alumnos NO eligen las fechas. Las ofertas académicas tienen fechas de inicio y fin FIJAS definidas por la institución. Si el usuario habla de elegir fechas, corrígelo amablemente.\n\n" +
            "REGLAS PEDAGÓGICAS (CRÍTICO):\n" +
            "1. NO resuelvas tareas ni exámenes directamente. Si el alumno pide la respuesta, diles amablemente que no puedes hacer su tarea, o solo puedes guiarlo para que sepa como empezar.\n" +
            "2. Proporciona explicaciones conceptuales, guías de estudio, bibliografía recomendada o ejemplos similares.\n" +
            "3. Fomenta el aprendizaje y el pensamiento crítico.\n\n" +
            "INFORMACIÓN PÚBLICA DE LA PLATAFORMA:\n" + contextoOfertas + "\n\n";

        if (!contextoUsuario.isEmpty()) {
            systemPrompt += "INFORMACIÓN DEL USUARIO (PRIVADO - NO COMPARTIR CON TERCEROS):\n" + contextoUsuario + "\n" +
                            "NOTA: Tienes acceso a la información de cuotas pendientes y tareas próximas listadas arriba. Úsala para responder preguntas del usuario sobre su estado.\n" +
                            "Sin embargo, NO tienes acceso a detalles históricos de calificaciones pasadas ni a contraseñas. Si preguntan por notas antiguas, remítelos a su panel de alumno.\n";
        } else {
            systemPrompt += "NOTA: El usuario es ANÓNIMO. Puedes saludar y conversar, pero solo puedes dar información técnica sobre ofertas académicas públicas. " +
                            "Si pregunta por su situación personal, materiales internos o clases, indícale que debe iniciar sesión.\n";
        }
        
        // Mensaje del sistema
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        // Obtener contexto de conversaciones anteriores
        List<ChatMessage> recentMessages = chatMessageRepository
            .findSessionMessagesSince(sessionId, LocalDateTime.now().minusHours(2))
            .stream()
            .limit(MAX_CONTEXT_MESSAGES)
            .toList();
        
        // Agregar mensajes del historial
        for (ChatMessage msg : recentMessages) {
            if (msg.getUserMessage() != null) {
                Map<String, Object> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", msg.getUserMessage());
                messages.add(userMsg);
            }
            
            if (msg.getAiResponse() != null) {
                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", msg.getAiResponse());
                messages.add(assistantMsg);
            }
        }
        
        // Agregar el mensaje actual del usuario
        Map<String, Object> currentUserMessage = new HashMap<>();
        currentUserMessage.put("role", "user");
        currentUserMessage.put("content", userMessage);
        messages.add(currentUserMessage);
        
        return messages;
    }
    
    private String obtenerContextoUsuario(String userDni) {
        try {
            Usuario usuario = usuarioRepository.findByDni(userDni).orElse(null);
            if (usuario == null) return "";
            
            StringBuilder sb = new StringBuilder();
            sb.append("Usuario: ").append(usuario.getNombre()).append(" ").append(usuario.getApellido());
            if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
                sb.append(" (Roles: ");
                sb.append(usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.joining(", ")));
                sb.append(")\n");
            } else {
                sb.append("\n");
            }
            
            // --- Contexto para DOCENTES ---
            if (usuario instanceof Docente) {
                Docente docente = (Docente) usuario;
                List<Horario> horarios = docente.getHorario(); 
                
                sb.append("👨‍🏫 PANEL DOCENTE:\n");

                // 1. Horarios
                if (horarios != null && !horarios.isEmpty()) {
                    sb.append("📅 TUS HORARIOS DE CLASE:\n");
                    for (Horario h : horarios) {
                        String materia = (h.getOfertaAcademica() != null) ? h.getOfertaAcademica().getNombre() : "Clase";
                        sb.append("- ").append(materia).append(": ")
                          .append(h.getDia()).append(" de ").append(h.getHoraInicio()).append(" a ").append(h.getHoraFin()).append("\n");
                    }
                }

                // 2. Resumen de Cursos y Alumnos
                List<Curso> misCursos = cursoRepository.findByDocentesId(docente.getId());
                if(misCursos != null && !misCursos.isEmpty()){
                    sb.append("📊 ESTADÍSTICAS DE TUS CURSOS:\n");
                    for(Curso c : misCursos){
                        if(c.getEstado() == com.example.demo.enums.EstadoOferta.ACTIVA || c.getEstado() == com.example.demo.enums.EstadoOferta.ENCURSO){
                            long inscritos = inscripcionRepository.countByOfertaAndEstadoInscripcionTrue(c);
                            sb.append("- ").append(c.getNombre()).append(": ").append(inscritos).append(" alumnos activos.\n");
                            
                            // 3. Alertas de corrección (Exámenes)
                            if(c.getModulos() != null){
                                for(Modulo m : c.getModulos()){
                                    if(m.getActividades() != null){
                                        for(Actividad a : m.getActividades()){
                                            if(a instanceof Examen){
                                                List<Intento> intentos = intentoRepository.findByExamen_IdActividad(a.getIdActividad());
                                                long sinCorregir = intentos.stream().filter(i -> i.getCalificacion() == null).count();
                                                if(sinCorregir > 0){
                                                    sb.append("  ⚠️ Examen '").append(a.getTitulo()).append("' tiene ").append(sinCorregir).append(" intentos sin corregir.\n");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Contexto para ALUMNOS (Inscripciones, Pagos, Tareas) ---
            // Nota: Un docente también puede ser alumno en otra cosa, así que revisamos inscripciones para todos
            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumno(usuario);
            
            if (!inscripciones.isEmpty()) {
                sb.append("📚 TUS INSCRIPCIONES Y ESTADO ACADÉMICO:\n");
                for (Inscripciones inscripcion : inscripciones) {
                    if (inscripcion.getOferta() == null) continue;
                    
                    String nombreOferta = inscripcion.getOferta().getNombre();
                    Boolean estadoActivo = inscripcion.getEstadoInscripcion();
                    sb.append("🔹 Curso: ").append(nombreOferta)
                      .append(" (Estado: ").append(estadoActivo != null && estadoActivo ? "Activo" : "Inactivo").append(")\n");

                    // 1. Verificar Cuotas Pendientes
                    List<Cuota> cuotas = cuotaRepository.findByInscripcionIdInscripcion(inscripcion.getIdInscripcion());
                    long pendientes = cuotas.stream().filter(c -> c.getEstado() == EstadoCuota.PENDIENTE).count();
                    
                    if (pendientes > 0) {
                        sb.append("   ⚠️ Tienes ").append(pendientes).append(" cuotas PENDIENTES de pago en este curso.\n");
                    } else {
                        sb.append("   ✅ Estás al día con los pagos.\n");
                    }

                    // 2. Información del Módulo y Clases
                    if (Boolean.TRUE.equals(estadoActivo) && inscripcion.getOferta() instanceof Curso) {
                        Curso curso = (Curso) inscripcion.getOferta();
                        if (curso.getModulos() != null) {
                            for (Modulo modulo : curso.getModulos()) {
                                // Información del Contenido del Módulo (Bibliografía y Temario)
                                sb.append("   📖 Módulo: ").append(modulo.getNombre()).append("\n");
                                if (modulo.getDescripcion() != null) sb.append("      - Desc: ").append(modulo.getDescripcion()).append("\n");
                                if (modulo.getTemario() != null) sb.append("      - Temario: ").append(modulo.getTemario()).append("\n");
                                if (modulo.getBibliografia() != null) sb.append("      - Bibliografía: ").append(modulo.getBibliografia()).append("\n");

                                // Clases del Módulo
                                if (modulo.getClases() != null && !modulo.getClases().isEmpty()) {
                                    sb.append("      - Clases:\n");
                                    for (Clase clase : modulo.getClases()) {
                                        sb.append("        * ").append(clase.getTitulo())
                                          .append(" (").append(clase.getInicio().toLocalDate()).append(")\n");
                                    }
                                }

                                if (modulo.getActividades() != null) {
                                    for (Actividad actividad : modulo.getActividades()) {
                                        // Filtrar actividades pendientes/futuras
                                        if (actividad instanceof Tarea) {
                                            Tarea t = (Tarea) actividad;
                                            if (t.getLimiteEntrega() != null && t.getLimiteEntrega().isAfter(LocalDateTime.now())) {
                                                sb.append("   📝 Tarea pendiente: ").append(t.getTitulo())
                                                  .append(" (Vence: ").append(t.getLimiteEntrega().toLocalDate()).append(")\n");
                                            }
                                        } else if (actividad instanceof Examen) {
                                            Examen e = (Examen) actividad;
                                            if (e.getFechaCierre() != null && e.getFechaCierre().isAfter(LocalDateTime.now())) {
                                                sb.append("   📝 Examen próximo: ").append(e.getTitulo())
                                                  .append(" (Cierra: ").append(e.getFechaCierre().toLocalDate()).append(")\n");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (!(usuario instanceof Docente)) {
                sb.append("No tienes inscripciones activas actualmente.\n");
            }
            
            return sb.toString();
        } catch (Exception e) {
            System.err.println("Error recuperando contexto profundo: " + e.getMessage());
            e.printStackTrace();
            return "Error recuperando información detallada del usuario.";
        }
    }
    
    private String obtenerContextoOfertas() {
        try {
            // Buscamos ofertas ACTIVAS y EN CURSO
            List<com.example.demo.enums.EstadoOferta> estadosVisibles = Arrays.asList(
                com.example.demo.enums.EstadoOferta.ACTIVA,
                com.example.demo.enums.EstadoOferta.ENCURSO
            );
            
            List<com.example.demo.model.OfertaAcademica> ofertas = ofertaAcademicaRepository.findByEstadoIn(estadosVisibles);
            
            if (ofertas.isEmpty()) {
                return "No hay información de ofertas académicas disponible en este momento.";
            }
            
            java.time.LocalDate hoy = java.time.LocalDate.now();
            Set<String> categorias = new HashSet<>();
            StringBuilder sb = new StringBuilder("=== CATÁLOGO DE OFERTAS ACADÉMICAS ===\n");
            
            // Agrupar por estado para mejor organización
            List<com.example.demo.model.OfertaAcademica> proximas = new ArrayList<>();
            List<com.example.demo.model.OfertaAcademica> enCurso = new ArrayList<>();
            
            for (com.example.demo.model.OfertaAcademica o : ofertas) {
                // Recolectar categorías
                o.getCategorias().forEach(c -> categorias.add(c.getNombre()));
                
                if (o.getEstado() == com.example.demo.enums.EstadoOferta.ENCURSO || 
                   (o.getFechaInicio() != null && !o.getFechaInicio().isAfter(hoy))) {
                    enCurso.add(o);
                } else {
                    proximas.add(o);
                }
            }
            
            if (!proximas.isEmpty()) {
                sb.append("\n-- PRÓXIMOS INICIOS --\n");
                for (com.example.demo.model.OfertaAcademica oferta : proximas) {
                    sb.append("• ").append(oferta.getNombre())
                      .append(" (Inicia: ").append(oferta.getFechaInicio()).append(")")
                      .append(" - $").append(oferta.getCostoInscripcion())
                      .append(" - ").append(oferta.getDescripcion())
                      .append("\n");
                }
            }
            
            if (!enCurso.isEmpty()) {
                sb.append("\n-- EN CURSO / DISPONIBLES --\n");
                for (com.example.demo.model.OfertaAcademica oferta : enCurso) {
                    sb.append("• ").append(oferta.getNombre())
                      .append(" (Estado: ").append(oferta.getEstado()).append(")")
                       .append(" - ").append(oferta.getDescripcion())
                      .append("\n");
                }
            }
            
            sb.append("\n=== INSTRUCCIONES DE RECOMENDACIÓN ===\n")
              .append("1. Tienes acceso a TODA la lista de ofertas anterior (Próximas y En Curso).\n")
              .append("2. Si faltan detalles de una oferta específica, indícalo, pero usa la descripción provista.\n")
              .append("3. Categorías disponibles: ").append(String.join(", ", categorias)).append(".\n");
            
            return sb.toString();
        } catch (Exception e) {
            System.err.println("Error al obtener contexto de ofertas: " + e.getMessage());
            return "No se pudo obtener la información de ofertas académicas.";
        }
    }

    private String generarRespuestaConChat(List<Map<String, Object>> messages) {
        try {
            // Detectar modelo disponible automáticamente
            String modeloAUsar = detectarModeloDisponible();
            
            // Construir request siguiendo el patrón de la API de chat de Ollama
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modeloAUsar);
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.2,
                "top_p", 0.9,
                "num_predict", 512,
                "stop", Arrays.asList("[INST]", "[/INST]")
            ));
            
            System.out.println("🤖 Usando modelo: " + modeloAUsar);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Llamar a Ollama Chat API
            Map<String, Object> response = restTemplate.postForObject(
                iaConfig.getChatEndpoint(), 
                request, 
                Map.class
            );
            
            if (response != null && response.containsKey("message")) {
                Map<String, Object> message = (Map<String, Object>) response.get("message");
                if (message.containsKey("content")) {
                    return (String) message.get("content");
                }
            }
            
            return "Lo siento, no pude generar una respuesta en este momento.";
            
        } catch (ResourceAccessException e) {
            throw new RuntimeException("No se pudo conectar con el servidor de Ollama. " +
                                     "Verifica que Ollama esté ejecutándose en " + iaConfig.getOllamaBaseUrl(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar respuesta: " + e.getMessage(), e);
        }
    }
    
    private String generarRespuestaError(Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (errorMsg.contains("timed out") || (e.getCause() != null && e.getCause().getMessage() != null && e.getCause().getMessage().toLowerCase().contains("timed out"))) {
            return "⏱️ **El asistente está tardando demasiado**\n\n" +
                   "La consulta es compleja o el servidor está ocupado. Por favor:\n" +
                   "- Intenta con una pregunta más corta\n" +
                   "- Espera unos momentos y prueba de nuevo\n" +
                   "- Verifica que tu conexión sea estable";
        }

        if (e instanceof ResourceAccessException || e.getCause() instanceof ResourceAccessException) {
            return "🔌 **Servicio temporalmente no disponible**\n\n" +
                   "El asistente de IA no puede responder en este momento. Esto puede deberse a:\n" +
                   "- Ollama no está ejecutándose\n" +
                   "- Problemas de conectividad\n\n" +
                   "**Mientras tanto, puedes:**\n" +
                   "- Revisar la documentación de la plataforma\n" +
                   "- Contactar al soporte técnico\n" +
                   "- Intentar nuevamente en unos minutos";
        }
        
        return "⚠️ **Error temporal del sistema**\n\n" +
               "Estamos experimentando dificultades técnicas. Nuestro equipo está trabajando para resolverlo.\n\n" +
               "**Soluciones temporales:**\n" +
               "- Refrescar la página\n" +
               "- Contactar soporte si persiste";
    }
    
    private ChatMessage.MessageType determinarTipoMensaje(String mensaje) {
        String mensajeLower = mensaje.toLowerCase();
        
        if (mensajeLower.contains("curso") || mensajeLower.contains("materia") || mensajeLower.contains("clase")) {
            return ChatMessage.MessageType.AYUDA_CURSO;
        } else if (mensajeLower.contains("examen") || mensajeLower.contains("evaluación") || mensajeLower.contains("test")) {
            return ChatMessage.MessageType.PREGUNTA_EXAMEN;
        } else if (mensajeLower.contains("problema") || mensajeLower.contains("error") || mensajeLower.contains("no funciona")) {
            return ChatMessage.MessageType.SOPORTE_TECNICO;
        } else if (mensajeLower.contains("académic") || mensajeLower.contains("estudio") || mensajeLower.contains("material")) {
            return ChatMessage.MessageType.CONSULTA_ACADEMICA;
        } else {
            return ChatMessage.MessageType.CHAT_GENERAL;
        }
    }
    
    private boolean verificarLimitesUso(String userDni) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long messageCount = chatMessageRepository.countMessagesByUserSince(userDni, oneHourAgo);
        return messageCount >= MAX_MESSAGES_PER_HOUR;
    }
    
    public List<ChatMessage> obtenerHistorialSesion(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
    
    public void limpiarSesion(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }
    
    public String generarSessionId() {
        return UUID.randomUUID().toString();
    }
    
    // Método para verificar conectividad con Ollama
    public boolean verificarConexionOllama() {
        try {
            Map<String, Object> healthCheck = restTemplate.getForObject(
                iaConfig.getOllamaBaseUrl() + "/api/tags", 
                Map.class
            );
            return healthCheck != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Método para obtener modelos disponibles en Ollama
    public List<String> obtenerModelosDisponibles() {
        try {
            String url = iaConfig.getOllamaBaseUrl() + "/api/tags";
            System.out.println("🔗 Consultando modelos en: " + url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("models")) {
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
                List<String> modelNames = models.stream()
                    .map(model -> (String) model.get("name"))
                    .toList();
                System.out.println("📋 Encontrados " + modelNames.size() + " modelos: " + modelNames);
                return modelNames;
            } else {
                System.out.println("⚠️ Respuesta vacía o sin modelos");
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo modelos: " + e.getMessage());
            e.printStackTrace();
        }
        return Arrays.asList("No hay modelos disponibles");
    }
    
    // Método para detectar automáticamente un modelo disponible
    public String detectarModeloDisponible() {
        // Lista de modelos en orden de preferencia
        String[] modelosPreferidos = {
            iaConfig.getModelName(),
            "llama3.2:3b",
            "llama3.2:1b", 
            "llama3:8b",
            "gemma2:2b",
            "phi3:mini",
            "codellama:7b",
            "mistral:7b"
        };
        
        try {
            List<String> modelosDisponibles = obtenerModelosDisponibles();
            System.out.println("🔍 Modelos disponibles en Ollama: " + modelosDisponibles);
            
            // Buscar el primer modelo preferido que esté disponible
            for (String modeloPreferido : modelosPreferidos) {
                for (String modeloDisponible : modelosDisponibles) {
                    if (modeloDisponible.contains(modeloPreferido) || 
                        modeloPreferido.equals(modeloDisponible)) {
                        System.out.println("✅ Modelo encontrado: " + modeloDisponible);
                        return modeloDisponible;
                    }
                }
            }
            
            // Si no encuentra ninguno preferido, usar el primero disponible
            if (!modelosDisponibles.isEmpty() && !modelosDisponibles.get(0).equals("No hay modelos disponibles")) {
                String primerModelo = modelosDisponibles.get(0);
                System.out.println("⚠️ Usando primer modelo disponible: " + primerModelo);
                return primerModelo;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error detectando modelo: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fallback al modelo por defecto
        String modeloDefault = iaConfig.getModelName();
        System.out.println("🔄 Fallback al modelo por defecto: " + modeloDefault);
        return modeloDefault;
    }

    public String generarResumenClase(String transcripcion) {
        try {
            String systemPrompt = "Eres un asistente experto en educación. Tu tarea es generar un resumen formal, estructurado y visualmente atractivo de una clase virtual a partir de su transcripción. " +
                    "El resumen debe estar en formato HTML limpio (sin etiquetas <html>, <head>, <body>), utilizando estilos en línea (inline CSS) para dar formato. " +
                    "Usa una paleta de colores profesional (azules, grises, blancos). " +
                    "Estructura el resumen con: " +
                    "1. Un título <h1> centrado y con color distintivo. " +
                    "2. Una sección de 'Introducción' o 'Contexto'. " +
                    "3. Una lista de 'Temas Principales' (<ul> o <ol>). " +
                    "4. 'Puntos Clave' destacados. " +
                    "5. 'Conclusiones' o 'Cierre'. " +
                    "Asegúrate de que el HTML sea válido y se vea bien en un contenedor div.";

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "Genera el resumen para la siguiente transcripción:\n\n" + transcripcion);
            messages.add(userMessage);

            return generarRespuestaConChat(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return "<p>Error al generar el resumen: " + e.getMessage() + "</p>";
        }
    }
}