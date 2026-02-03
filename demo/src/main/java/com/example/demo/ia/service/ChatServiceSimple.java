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
import com.example.demo.enums.EstadoOferta;

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

    @Autowired
    private com.example.demo.repository.DocenteRepository docenteRepository;

    @Autowired
    private com.example.demo.repository.FormacionRepository formacionRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final int MAX_MESSAGES_PER_HOUR = 50;
    private static final int MAX_CONTEXT_MESSAGES = 4;  // Reducido de 10 a 4 para menos contaminación
    private static final int MAX_INTENTOS_INSULTOS = 3;
    
    // Lista básica de palabras prohibidas
    private static final List<String> PALABRAS_PROHIBIDAS = Arrays.asList(
        "idiota", "estupido", "imbecil", "mierda", "basura", "inutil", 
        "tonto", "tarado", "maldito", "puta", "carajo", "verga", "pendejo",
        "zorra", "cabron", "chinga", "coño", "gilipollas", "bobo"
    );
    
    // Regex para PII
    private static final String DNI_REGEX = "\\b\\d{7,8}\\b";
    private static final String EMAIL_REGEX = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b";
    private static final String CARD_REGEX = "\\b(?:\\d[ -]*?){13,16}\\b";

    // --- DEFENSA POR CAPAS: Definición de Intenciones Críticas ---
    private enum CriticalIntent {
        PAGO,
        INSCRIPCION,
        DATOS_PERSONALES,
        PROBLEMA_TECNICO,
        NONE
    }

    private static final Map<CriticalIntent, List<String>> CRITICAL_KEYWORDS = Map.of(
        CriticalIntent.PAGO, List.of(
            "pagar", "cobro", "tarjeta", "precio", "descuento", "factura", "comprar", "costo", "dinero", "abonar"
        ),
        CriticalIntent.INSCRIPCION, List.of(
            "inscribir", "matricular", "anotar", "registro", "registrarse", "cupo", "vacante"
        ),
        CriticalIntent.DATOS_PERSONALES, List.of(
            "contraseña", "password", "clave", "dni", "telefono", "mail", "correo", "direccion", "cambiar mis datos"
        ),
        CriticalIntent.PROBLEMA_TECNICO, List.of(
            "error", "bug", "falla", "no funciona", "roto", "caido", "lento", "sistema"
        )
    );

    // --- Respuestas Estáticas Seguras ---
    private static final String RESP_PAGO =
        "Para temas de pagos, costos o inscripciones, por favor dirígete a la sección 'Mis Ofertas' o contacta a administración. Yo solo puedo responder dudas sobre el contenido educativo.";
    
    private static final String RESP_INSCRIPCION =
        "La gestión de inscripciones se realiza desde tu panel de usuario en la sección de ofertas. ¿Te ayudo con información sobre el contenido del curso?";

    private static final String RESP_DATOS =
        "Por seguridad, no puedo gestionar datos personales ni contraseñas. Por favor, visita tu perfil de usuario para realizar esos cambios.";

    private static final String RESP_TECNICO =
        "Parece que tienes un problema técnico. Te sugiero contactar a soporte a través del formulario de contacto oficial.";


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

        // ------------------------------------------------------------
        // CAPA 1: PRE-FILTRO CRÍTICO (DEFENSA POR CAPAS)
        // ------------------------------------------------------------
        Optional<String> criticalResponse = prefilterCriticalIntent(mensajeSaneado);
        if (criticalResponse.isPresent()) {
            chatMessage.setAiResponse(criticalResponse.get());
            chatMessage.setResponseTimeMs(5L);
            chatMessage.setMessageType(ChatMessage.MessageType.SOPORTE_TECNICO);
            
            if (!"ANONIMO".equals(userDni) && validarPermisoGuardado(usuario)) {
                return chatMessageRepository.save(chatMessage);
            } else {
                return chatMessage;
            }
        }

        // ------------------------------------------------------------
        // NUEVA LÓGICA: RESPUESTAS PREDEFINIDAS
        // ------------------------------------------------------------
        String respuestaPredefinida = obtenerRespuestaPredefinida(mensajeSaneado, userDni);
        if (respuestaPredefinida != null) {
            chatMessage.setAiResponse(respuestaPredefinida);
            chatMessage.setResponseTimeMs(10L); // Tiempo simulado muy rápido
            chatMessage.setMessageType(determinarTipoMensaje(userMessage));
            
            if (!"ANONIMO".equals(userDni) && validarPermisoGuardado(usuario)) {
                return chatMessageRepository.save(chatMessage);
            } else {
                return chatMessage;
            }
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Obtener contexto de la conversación
            List<Map<String, Object>> messages = construirHistorialMensajes(sessionId, mensajeSaneado, userDni);
            System.out.println("📚 Historial construido con " + messages.size() + " mensajes");
            
            // Generar respuesta de IA usando el patrón de chat de Ollama
            String rawResponse = generarRespuestaConChat(messages);
            
            // CAPA 2: POST-FILTRO (Sanitización)
            String aiResponse = postFilterIaResponse(rawResponse);

            System.out.println("✅ Respuesta generada (filtrada): " + (aiResponse != null ? aiResponse.substring(0, Math.min(100, aiResponse.length())) + "..." : "null"));
            
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

    /**
     * CAPA 1: PRE-FILTRO CRÍTICO
     * Detecta intenciones críticas (Pagos, PII, Inscripciones) antes de ejecutar cualquier lógica de IA.
     * Implementa el patrón "Defense in Depth".
     */
    private Optional<String> prefilterCriticalIntent(String message) {
        if (message == null) return Optional.empty();
        String lowerMsg = message.toLowerCase();

        // 1. Verificación rápida de palabras clave críticas
        for (Map.Entry<CriticalIntent, List<String>> entry : CRITICAL_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                // Buscamos coincidencia
                if (lowerMsg.contains(keyword)) {
                    // Lógica adicional para evitar falsos positivos si fuera necesaria
                    return Optional.of(getSafeResponse(entry.getKey()));
                }
            }
        }
        return Optional.empty();
    }

    private String getSafeResponse(CriticalIntent intent) {
        switch (intent) {
            case PAGO: return RESP_PAGO;
            case INSCRIPCION: return RESP_INSCRIPCION;
            case DATOS_PERSONALES: return RESP_DATOS;
            case PROBLEMA_TECNICO: return RESP_TECNICO;
            default: return "Para esa consulta, por favor contacta a administración.";
        }
    }

    /**
     * CAPA 2: POST-FILTRO
     * Sanitiza la respuesta de la IA para asegurar que no se colaron alucinaciones peligrosas.
     */
    private String postFilterIaResponse(String aiResponse) {
        if (aiResponse == null) return null;
        
        // Reglas de seguridad post-generación
        String filtered = aiResponse;
        
        // 1. Eliminar promesas de acción o confirmaciones falsas
        if (filtered.toLowerCase().contains("he inscrito") || filtered.toLowerCase().contains("te inscribí") || filtered.toLowerCase().contains("registrado correctamente")) {
            filtered += "\n\n(NOTA DE SEGURIDAD: Soy una IA informativa. Por favor verifica tu inscripción oficial en el sistema.)";
        }
        
        // 2. Advertencia si menciona precios sospechosos (opcional)
        // if (filtered.contains("$") && !filtered.contains("lista")) ...

        return filtered;
    }

    /**
     * Lógica simple para respuestas predefinidas y evitar consumo de tokens en casos triviales.
     */
    private String obtenerRespuestaPredefinida(String mensaje, String userDni) {
        if (mensaje == null) return null;
        String m = mensaje.toLowerCase().trim();

        // 1. Saludos comunes
        List<String> saludos = Arrays.asList("hola", "buen dia", "buenos dias", "buenas tardes", "buenas noches", "que tal", "hello", "hi");
        // Verificar igualdad exacta o si el mensaje es muy corto y contiene el saludo
        boolean esSaludo = saludos.contains(m);
        if (!esSaludo) {
            // "hola, como estas?"
            for (String s : saludos) {
                if (m.startsWith(s) && m.length() < s.length() + 20) {
                   esSaludo = true;
                   break; 
                }
            }
        }

        if (esSaludo) {
            return "¡Hola! Soy el asistente virtual de Aurea. ¿En qué puedo ayudarte hoy?\n\n" +
                   "Puedo informarte sobre:\n" +
                   "• Ofertas académicas disponibles\n" +
                   "• Fechas de exámenes\n" +
                   "• Estado de tus cuentas (si iniciaste sesión)";
        }

        // 2. Solicitud explícita de recomendaciones generales
        // "que me recomiendas", "que puedo estudiar", "cuales son las ofertas", "ofertas disponibles"
        if (m.contains("recomienda") || m.contains("estudiar") || m.contains("ofertas disponibles") || m.contains("que hay para mi")) {
            String ofertas = obtenerContextoOfertas(userDni, null);
            return "Basado en nuestro sistema, estas son las ofertas académicas disponibles actualmente:\n\n" + 
                   ofertas + "\n\n¿Te gustaría saber más detalles sobre alguna de ellas?";
        }

        // 3. NUEVO: Filtrado por precio (MANEJO DIRECTO - NO DELEGAR A IA)
        Double precioMaximo = extraerLimitePrecio(m);
        if (precioMaximo != null) {
            return filtrarOfertasPorPrecio(userDni, precioMaximo);
        }

        return null; // Si no hay match, devolver null para que procese la IA
    }
    
    /**
     * Extrae el límite de precio de consultas como:
     * "cursos de menos de 500", "ofertas menores a $100", "baratos menos de 1000"
     */
    private Double extraerLimitePrecio(String mensaje) {
        if (mensaje == null) return null;
        
        // Patrones comunes de consulta de precio
        if (!mensaje.contains("menos") && !mensaje.contains("menor") && 
            !mensaje.contains("hasta") && !mensaje.contains("máximo") &&
            !mensaje.contains("max") && !mensaje.contains("barato")) {
            return null;
        }
        
        // Extraer número del mensaje
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+[.,]?\\d*)");
        java.util.regex.Matcher matcher = pattern.matcher(mensaje);
        
        if (matcher.find()) {
            try {
                String numeroStr = matcher.group(1).replace(",", ".");
                return Double.parseDouble(numeroStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Filtra ofertas por precio DIRECTAMENTE (sin delegar a IA)
     * para evitar problemas de comparación numérica del modelo LLM
     */
    private String filtrarOfertasPorPrecio(String userDni, Double precioMaximo) {
        try {
            List<OfertaAcademica> todasOfertas = obtenerOfertasSinDocente(userDni);
            
            // Filtrar por precio de inscripción
            List<OfertaAcademica> ofertasFiltradas = todasOfertas.stream()
                .filter(o -> o.getCostoInscripcion() != null && o.getCostoInscripcion() < precioMaximo)
                .sorted((o1, o2) -> Double.compare(o1.getCostoInscripcion(), o2.getCostoInscripcion()))
                .toList();
            
            if (ofertasFiltradas.isEmpty()) {
                return String.format("❌ No encontré ofertas académicas con inscripción menor a $%.0f.\n\n" +
                    "💡 Sugerencia: Puedes ajustar tu presupuesto o consultar por todas las ofertas disponibles.", 
                    precioMaximo);
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("✅ Encontré %d oferta(s) con inscripción menor a $%.0f:\n\n", 
                ofertasFiltradas.size(), precioMaximo));
            
            for (OfertaAcademica o : ofertasFiltradas) {
                String tipo = (o instanceof Formacion) ? "FORMACIÓN" : "CURSO";
                
                // Refinar tipo según categorías
                if(o.getCategorias() != null) {
                    for(Categoria cat : o.getCategorias()) {
                         String catName = cat.getNombre().trim().toUpperCase();
                         if(catName.contains("CHARLA") || catName.contains("WEBINAR")) tipo = "CHARLA";
                         else if(catName.contains("SEMINARIO") || catName.contains("TALLER")) tipo = "SEMINARIO";
                    }
                }
                
                sb.append(String.format("📚 [%s] %s\n", tipo, o.getNombre()));
                sb.append(String.format("   💰 INSCRIPCIÓN: $%.0f", o.getCostoInscripcion()));
                
                // Agregar info de cuotas si aplica
                if (o instanceof Curso) {
                    Curso c = (Curso) o;
                    if (c.getCostoCuota() != null && c.getCostoCuota() > 0) {
                        sb.append(String.format(" | CUOTA: $%.0f (x%d cuotas)", c.getCostoCuota(), c.getNrCuotas()));
                    }
                } else if (o instanceof Formacion) {
                    Formacion f = (Formacion) o;
                    if (f.getCostoCuota() != null && f.getCostoCuota() > 0) {
                        sb.append(String.format(" | CUOTA: $%.0f (x%d cuotas)", f.getCostoCuota(), f.getNrCuotas()));
                    }
                }
                
                sb.append("\n");
                if (o.getDescripcion() != null) {
                    sb.append("   📝 ").append(o.getDescripcion()).append("\n");
                }
                sb.append("\n");
            }
            
            sb.append("💡 ¿Te gustaría más información sobre alguna de estas ofertas?");
            
            return sb.toString();
            
        } catch (Exception e) {
            System.err.println("❌ Error filtrando ofertas por precio: " + e.getMessage());
            e.printStackTrace();
            return "Hubo un error al buscar ofertas por precio. Por favor, intenta de nuevo.";
        }
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

    /**
     * Detecta si el mensaje actual representa un cambio significativo de tema
     * respecto a la conversación anterior, lo cual requiere limpiar el historial
     * para evitar contaminación de datos.
     */
    private boolean detectarCambioTema(String currentMessage, String sessionId) {
        if (currentMessage == null || currentMessage.trim().isEmpty()) return false;
        
        // Palabras clave que indican búsqueda de ofertas específicas (cambio de tema)
        String msg = currentMessage.toLowerCase();
        List<String> palabrasCambioTema = Arrays.asList(
            "busco", "quiero", "necesito", "recomendame", "recomienda",
            "ofertas", "cursos", "carreras", "formaciones", "charlas",
            "menor", "mayor", "precio", "barato", "económico", "gratis",
            "disponibles", "hay algún", "tienen"
        );
        
        // Si el mensaje contiene palabras de búsqueda/filtrado, es cambio de tema
        for (String palabra : palabrasCambioTema) {
            if (msg.contains(palabra)) {
                return true;
            }
        }
        
        // Verificar si el último mensaje fue hace más de 5 minutos (nueva conversación)
        try {
            List<ChatMessage> recent = chatMessageRepository
                .findSessionMessagesSince(sessionId, LocalDateTime.now().minusMinutes(5));
            return recent.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private List<Map<String, Object>> construirHistorialMensajes(String sessionId, String userMessage, String userDni) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // Obtener información de ofertas académicas activas para el contexto (PÚBLICO)
        String contextoOfertas = obtenerContextoOfertas(userDni, null);

        System.out.println("=== CONTEXTO DE OFERTAS ENVIADO A LA IA ===\n" + contextoOfertas);
        
        // Contexto específico del usuario (PRIVADO - solo si autenticado)
        String contextoUsuario = "";
        if (!"ANONIMO".equals(userDni)) {
            contextoUsuario = obtenerContextoUsuario(userDni);
        }
        
        String systemPrompt = "Eres el asistente virtual oficial de Aurea.\n\n" +
            "=== LISTA DE OFERTAS ACADÉMICAS VERIFICADAS (LA ÚNICA VERDAD) ===\n" +
            contextoOfertas + "\n" +
            "===============================================================\n\n" +
            "⚠️ REGLAS ABSOLUTAS - LECTURA OBLIGATORIA ⚠️\n\n" +
            "REGLA #0: PROHIBICIÓN TOTAL DE MEZCLAR DATOS\n" +
            "- NUNCA combines el nombre de un curso con el precio de otro\n" +
            "- NUNCA combines la descripción de un curso con datos de otro\n" +
            "- NUNCA inventes precios que no estén explícitamente en el listado\n" +
            "- Si un curso tiene INSCRIPCIÓN: $12, NO lo menciones con otro precio\n" +
            "- Si un curso tiene INSCRIPCIÓN: $12000, NO lo menciones como $12\n" +
            "- CADA OFERTA ES UNA UNIDAD COMPLETA: nombre + tipo + precio de inscripción + precio de cuota\n" +
            "- Si no estás 100% seguro de qué datos corresponden a qué curso, di 'Necesito verificar esa información'\n\n" +
            "REGLA #1: FILTRADO POR PRECIO\n" +
            "- Cuando el usuario pida 'ofertas menores a $X', compara el valor de INSCRIPCIÓN\n" +
            "- Ejemplo: Si pide 'menores a $15', solo incluye ofertas donde INSCRIPCIÓN sea menor a 15\n" +
            "- NO incluyas ofertas con INSCRIPCIÓN: $12000 si piden menores a $15\n" +
            "- El símbolo '$' significa pesos, no miles. $12 es DOCE PESOS, $12000 es DOCE MIL PESOS\n\n" +
            "REGLAS CRÍTICAS DE CLASIFICACIÓN:\n" +
            "1. ¡MIRA EL TIPO DE LEGAJO ANTES DE RESPONDER!\n" +
            "2. [FORMACIÓN] = Larga duración, Carrera.\n" +
            "3. [CURSO] = Curso de meses, pago cuotas.\n" +
            "4. [TALLER] / [SEMINARIO] / [CHARLA] / [WEBINAR] = Eventos cortos, usualmente de 1 día.\n\n" +
            "CASOS DE USO ESPECÍFICOS:\n" +
            "--> Usuario dice 'Busco charlas' o 'Quiero escuchar una conferencia':\n" +
            "    RESPONDE: 'Aquí tienes las charlas y webinars disponibles:' y lista SOLO los del LISTADO 4.\n" +
            "    ¡PROHIBIDO LISTAR CURSOS EN ESTE CASO!\n\n" +
            "--> Usuario dice 'Quiero hacer un curso' o 'Aprender a programar':\n" +
            "    RESPONDE: 'Estos son los cursos disponibles:' y lista SOLO los del LISTADO 2.\n\n" +
            "--> Usuario dice 'Carreras' o 'Formación completa':\n" +
            "    RESPONDE: 'Tenemos estas formaciones:' y lista SOLO los del LISTADO 1.\n\n" +
            "--> Usuario dice 'ofertas menores a $X' o 'cursos baratos':\n" +
            "    1. Identifica el precio límite X\n" +
            "    2. Busca EN CADA LISTADO ofertas donde INSCRIPCIÓN < X\n" +
            "    3. Copia EXACTAMENTE el texto completo de esa oferta (nombre, tipo, precios, todo)\n" +
            "    4. NO modifiques ningún dato, NO inventes nada\n\n" +
            "REGLAS GENERALES:\n" +
            "- ¡NO INVENTES! Si no hay ofertas en la lista solicitada, di 'No hay ofertas de ese tipo disponibles'.\n" +
            "- No confirmes inscripciones, no tienes acceso a la DB de pagos.\n" +
            "- Sé amable y breve.\n" +
            "- Cuando menciones una oferta, copia TEXTUALMENTE su información del listado\n" +
            "- Si dudas sobre algún dato, NO lo inventes, pide aclaración al usuario\n";


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
        // OPTIMIZACIÓN: Si detectamos cambio de tema, no incluir historial para evitar contaminación
        boolean esCambioTema = detectarCambioTema(userMessage, sessionId);
        
        List<ChatMessage> recentMessages = new ArrayList<>();
        if (!esCambioTema) {
            recentMessages = chatMessageRepository
                .findSessionMessagesSince(sessionId, LocalDateTime.now().minusHours(2))
                .stream()
                .limit(MAX_CONTEXT_MESSAGES)
                .toList();
        } else {
            System.out.println("🔄 Cambio de tema detectado - limpiando historial para evitar contaminación");
        }
        
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
                // Agregar lógica para formaciones y prohibiciones
                List<Formacion> misFormaciones = formacionRepository.findByDocentesId(docente.getId());
                List<String> ofertasProhibidas = new ArrayList<>();
                if (misCursos != null) misCursos.forEach(c -> ofertasProhibidas.add(c.getNombre()));
                if (misFormaciones != null) misFormaciones.forEach(f -> ofertasProhibidas.add(f.getNombre()));
                
                if (!ofertasProhibidas.isEmpty()) {
                    sb.append("⚠️ AVISO IMPORTANTE: Eres docente en estas ofertas: ")
                      .append(String.join(", ", ofertasProhibidas))
                      .append(". NO te recomiendes inscribirte en ellas, ya que eres docente designado.\n\n");
                }

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
    
    private String obtenerContextoOfertas(String userDni, Double precioMaximoFiltro) {
        try {
            // Buscamos ofertas ACTIVAS y EN CURSO
            List<OfertaAcademica> ofertas = obtenerOfertasSinDocente(userDni);
            
            // Aplicar filtro de precio si se especifica
            if (precioMaximoFiltro != null) {
                ofertas = ofertas.stream()
                    .filter(o -> o.getCostoInscripcion() != null && o.getCostoInscripcion() < precioMaximoFiltro)
                    .toList();
            }

            if (ofertas.isEmpty()) {
                return "No hay información de ofertas académicas disponible en este momento.";
            }
            
            java.time.LocalDate hoy = java.time.LocalDate.now();
            Set<String> categorias = new HashSet<>();
            StringBuilder sb = new StringBuilder("=== CATÁLOGO DE OFERTAS ACADÉMICAS ===\n");
            
            // 4 BUCKETS SEPARADOS
            StringBuilder sbFormaciones = new StringBuilder(); // 1
            StringBuilder sbCursos = new StringBuilder();      // 2
            StringBuilder sbTalleres = new StringBuilder();    // 3 (Talleres y Seminarios)
            StringBuilder sbCharlas = new StringBuilder();     // 4 (Charlas y Webinars)
            
            boolean hayFormaciones = false;
            boolean hayCursos = false;
            boolean hayTalleres = false;
            boolean hayCharlas = false;

            for (com.example.demo.model.OfertaAcademica o : ofertas) {
                // Recolectar categorías
                o.getCategorias().forEach(c -> categorias.add(c.getNombre()));
                
                // 1. Determinar STATUS
                String estadoStr = "PRÓXIMAMENTE";
                if (o.getEstado() == com.example.demo.enums.EstadoOferta.ENCURSO || 
                   (o.getFechaInicio() != null && !o.getFechaInicio().isAfter(hoy))) {
                    estadoStr = "EN CURSO / DISPONIBLE";
                } else if (o.getFechaInicio() != null) {
                    estadoStr = "INICIA: " + o.getFechaInicio().toString();
                }

                // 2. Determinar TIPO EXACTO
                String tipo = (o instanceof com.example.demo.model.Formacion) ? "FORMACIÓN" : "CURSO";
                String tituloUpper = o.getNombre() != null ? o.getNombre().toUpperCase() : "";

                if(o.getCategorias() != null) {
                    for(Categoria cat : o.getCategorias()) {
                         String catName = cat.getNombre().trim().toUpperCase();
                         if(catName.contains("CHARLA")) { tipo = "CHARLA"; }
                         else if(catName.contains("SEMINARIO")) { tipo = "SEMINARIO"; }
                         else if(catName.contains("TALLER")) { tipo = "SEMINARIO"; }
                         else if(catName.contains("VIDEO")) { tipo = "CHARLA"; }
                         else if(catName.contains("WEBINAR")) { tipo = "CHARLA "; }
                    }
                }
                // Refuerzo con el Título
                if(tituloUpper.contains("CHARLA")) { tipo = "CHARLA"; }
                else if(tituloUpper.contains("TALLER")) { tipo = "SEMINARIO"; }
                else if(tituloUpper.contains("SEMINARIO")) { tipo = "SEMINARIO"; }
                else if(tituloUpper.contains("WEBINAR")) { tipo = "CHARLA"; }

                // 3. Construir la línea de detalle
                StringBuilder item = new StringBuilder();
                item.append("• [").append(tipo).append("] ").append(o.getNombre())
                    .append(" (Estado: ").append(estadoStr).append(")");

                // Precios
                if (o.getCostoInscripcion() == null || o.getCostoInscripcion() == 0.0) {
                    item.append(" | INSCRIPCIÓN: GRATIS ($0)");
                } else {
                    item.append(" | INSCRIPCIÓN: $").append(String.format("%.0f", o.getCostoInscripcion()));
                }

                if (o instanceof com.example.demo.model.Curso) {
                    com.example.demo.model.Curso c = (com.example.demo.model.Curso) o;
                    if (c.getCostoCuota() != null && c.getCostoCuota() > 0) {
                        item.append(" | CUOTA MENSUAL: $").append(String.format("%.0f", c.getCostoCuota()))
                            .append(" (x").append(c.getNrCuotas()).append(" cuotas)");
                    }
                } else if (o instanceof com.example.demo.model.Formacion) {
                    com.example.demo.model.Formacion f = (com.example.demo.model.Formacion) o;
                    if (f.getCostoCuota() != null && f.getCostoCuota() > 0) {
                        item.append(" | CUOTA MENSUAL: $").append(String.format("%.0f", f.getCostoCuota()))
                            .append(" (x").append(f.getNrCuotas()).append(" cuotas)");
                    }
                }

                item.append(" | Info: ").append(o.getDescripcion()).append("\n");

                // 4. Asignar al bucket correcto
                if (tipo.equals("FORMACIÓN")) {
                    sbFormaciones.append(item); hayFormaciones = true;
                } else if (tipo.equals("CHARLA") || tipo.equals("WEBINAR") || tipo.equals("VIDEO CONFERENCIA")) {
                    sbCharlas.append(item); hayCharlas = true;
                } else if (tipo.equals("TALLER") || tipo.equals("SEMINARIO")) {
                    sbTalleres.append(item); hayTalleres = true;
                } else {
                    sbCursos.append(item); hayCursos = true;
                }
            }
            
            // 5. Ensamblar respuesta final SEPARADA
            if (hayFormaciones) {
                sb.append("\n=== LISTADO 1: FORMACIONES Y CARRERAS ===\n");
                sb.append(sbFormaciones);
            }

            if (hayCursos) {
                sb.append("\n=== LISTADO 2: CURSOS REGULARES ===\n");
                sb.append(sbCursos);
            }

            if (hayTalleres) {
                sb.append("\n=== LISTADO 3: TALLERES Y SEMINARIOS ===\n");
                sb.append(sbTalleres);
            }
            
            if (hayCharlas) {
                sb.append("\n=== LISTADO 4: CHARLAS Y CONFERENCIAS ===\n");
                sb.append(sbCharlas);
            }
            
            sb.append("\n=== GUÍA DE RESPUESTA PARA LA IA ===\n")
              .append("1. USA ESTA CLASIFICACIÓN RIGUROSAMENTE.\n")
              .append("2. Si preguntan por 'Carreras' o 'Formaciones', usa el LISTADO 1.\n")
              .append("3. Si preguntan por 'Cursos', usa el LISTADO 2.\n")
              .append("4. Si preguntan por 'Seminarios' o 'Talleres', usa el LISTADO 3.\n")
              .append("5. Si preguntan por 'Charlas', usa el LISTADO 4.\n");
            
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
                "temperature", 0.1,      // Reducido de 0.2 a 0.1 para máxima precisión
                "top_p", 0.85,           // Reducido de 0.9 a 0.85 para menos creatividad
                "top_k", 10,             // Agregado: limita vocabulario a 10 tokens más probables
                "repeat_penalty", 1.2,   // Agregado: penaliza repeticiones
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
            String systemPrompt = "Eres un redactor académico experto. Tu tarea es generar un resumen coherente, fluido y bien estructurado de una clase virtual a partir de su transcripción. " +
                    "El texto debe tener sentido completo, conectando las ideas de manera lógica. " +
                    "Formato de salida: TEXTO PLANO (sin Markdown complejo, sin HTML). " +
                    "Estructura requerida: " +
                    "1. Introducción: Contexto general de la clase. " +
                    "2. Desarrollo: Los temas principales explicados en profundidad y con cohesión. " +
                    "3. Conclusiones: Cierre sintetizando lo aprendido. " +
                    "IMPORTANTE: Redacta en tercera persona, usa un tono formal y educativo. Céntrate en el contenido académico. Evita frases como 'En esta transcripción' o 'El profesor dijo'.";

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

    /**
 * Obtiene todas las ofertas académicas activas y en curso,
 * excluyendo aquellas en las que el usuario es docente.
 */
private List<OfertaAcademica> obtenerOfertasSinDocente(String userDni) {
    Usuario usuario = usuarioRepository.findByDni(userDni).orElse(null);
    if (usuario == null || !(usuario instanceof Docente)) {
        // Si no es docente, retorna todas las ofertas activas/en curso
        return ofertaAcademicaRepository.findByEstadoIn(
            Arrays.asList(EstadoOferta.ACTIVA, EstadoOferta.ENCURSO)
        );
    }
    Docente docente = (Docente) usuario;
    // Ofertas donde el docente está asignado
    List<Curso> cursosDocente = cursoRepository.findByDocentesId(docente.getId());
    List<Formacion> formacionesDocente = formacionRepository.findByDocentesId(docente.getId());
    Set<Long> idsExcluidos = new HashSet<>();
    cursosDocente.forEach(c -> idsExcluidos.add(c.getIdOferta()));
    formacionesDocente.forEach(f -> idsExcluidos.add(f.getIdOferta()));

    // Todas las ofertas activas/en curso
    List<OfertaAcademica> todas = ofertaAcademicaRepository.findByEstadoIn(
        Arrays.asList(EstadoOferta.ACTIVA, EstadoOferta.ENCURSO)
    );
    // Filtrar las que no están en idsExcluidos
    return todas.stream()
        .filter(o -> !idsExcluidos.contains(o.getIdOferta()))
        .toList();
}
}