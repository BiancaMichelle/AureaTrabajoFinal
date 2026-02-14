package com.example.demo.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import com.example.demo.enums.IaGenerationStatus;
import com.example.demo.enums.TipoPregunta;
import com.example.demo.model.Archivo;
import com.example.demo.model.Material;
import com.example.demo.model.Opcion;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.ia.config.IAConfig;
import com.example.demo.repository.ArchivoRepository;
import com.example.demo.repository.MaterialRepository;
import com.example.demo.repository.OpcionRepository;
import com.example.demo.repository.PoolRepository;
import com.example.demo.repository.PreguntaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.transaction.Transactional;

@Service
public class IaPoolGeneratorService {

    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private PreguntaRepository preguntaRepository;
    
    @Autowired
    private OpcionRepository opcionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ArchivoRepository archivoRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IAConfig iaConfig;

    private static final Set<TipoPregunta> TIPOS_PERMITIDOS = EnumSet.of(
            TipoPregunta.MULTIPLE_CHOICE,
            TipoPregunta.VERDADERO_FALSO,
            TipoPregunta.UNICA_RESPUESTA
    );

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las",
            "por", "un", "para", "con", "no", "una", "su", "al", "lo",
            "como", "mas", "pero", "sus", "le", "ya", "o", "este", "si",
            "porque", "esta", "entre", "cuando", "muy", "sin", "sobre", "tambien",
            "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos",
            "durante", "todos", "uno", "les", "ni", "contra", "otros", "ese",
            "eso", "ante", "ellos", "e", "esto", "antes", "algunos", "que",
            "unos", "otro", "otras", "otra", "tanto", "esa", "estos", "mucho",
            "quienes", "nada", "muchos", "cual", "poco", "ella", "estar",
            "estas", "algunas", "algo", "nosotros", "mi", "mis", "tu", "tus",
            "ellas", "nosotras", "vosostros", "vosotras", "os", "mio", "mia",
            "mios", "mias", "tuyo", "tuya", "tuyos", "tuyas", "suyo",
            "suya", "suyos", "suyas", "nuestro", "nuestra", "nuestros",
            "nuestras", "vuestro", "vuestra", "vuestros", "vuestras", "es",
            "son", "ser", "fue", "fueron", "ha", "han", "hace", "hacer",
            "cada"
    ));


    @Async
    @Transactional
    public void generarPoolIAAsync(UUID poolId, String paramsJson) {
        try {
            // 1. Simular tiempo de procesamiento de IA (3-5 segundos)
            Thread.sleep(4000);
            
            // 2. Obtener pool (recargar desde BD)
            // Nota: En un entorno real asíncrono, hay que tener cuidado con las sesiones de Hibernate.
            // Aquí lo hacemos simple.
            manejarGeneracion(poolId, paramsJson);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
            marcarError(poolId, "Error interno en el proceso de generación: " + e.getMessage());
        }
    }
    
    // Método separado para manejar transaccionabilidad correctamente
    private void manejarGeneracion(UUID poolId, String paramsJson) {
        Pool pool = poolRepository.findById(poolId).orElse(null);
        if (pool == null) return;
        
        try {
            // Recoger IDs de materiales seleccionados
            java.util.List<Long> materialesIds = new ArrayList<>();
            String temas = "General";
            String contextoMaterial = "";
            String objetivos = "";
            String bibliografia = "";
            Integer cantidadParam = null;
            Integer minNuevasParam = null;
            List<String> excluirEnunciados = new ArrayList<>();
            
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(paramsJson);
                if (root.has("temas")) {
                    temas = root.get("temas").asText();
                } else if (root.has("tema")) {
                    temas = root.get("tema").asText();
                }

                if (root.has("contextoMaterial")) contextoMaterial = root.get("contextoMaterial").asText();
                if (root.has("objetivos")) objetivos = root.get("objetivos").asText();
                if (root.has("bibliografia")) bibliografia = root.get("bibliografia").asText();
                if (root.has("cantidad")) {
                    cantidadParam = root.get("cantidad").asInt();
                } else if (root.has("cantidadPreguntas")) {
                    cantidadParam = root.get("cantidadPreguntas").asInt();
                }
                if (root.has("minNuevas")) {
                    minNuevasParam = root.get("minNuevas").asInt();
                }
                
                if (root.has("materialesIds") && root.get("materialesIds").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode idNode : root.get("materialesIds")) {
                        materialesIds.add(idNode.asLong());
                    }
                }
                if (root.has("excluirEnunciados") && root.get("excluirEnunciados").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode enNode : root.get("excluirEnunciados")) {
                        String val = enNode.asText();
                        if (val != null && !val.isBlank()) {
                            excluirEnunciados.add(val.trim());
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore parse error
            }

            // Cargar metadata de materiales para generar contexto
            java.util.List<String> nombresMateriales = new ArrayList<>();
            String contextoMateriales = "";
            if (!materialesIds.isEmpty()) {
                List<Material> materiales = materialRepository.findAllById(materialesIds);
                for (Material m : materiales) {
                    nombresMateriales.add(m.getTitulo());
                }
                if (!nombresMateriales.isEmpty()) {
                    contextoMateriales = "Basado en los documentos: " + String.join(", ", nombresMateriales);
                }
            }

            if (contextoMateriales == null || contextoMateriales.isBlank()) {
                StringBuilder contextoExtra = new StringBuilder();
                if (contextoMaterial != null && !contextoMaterial.isBlank()) {
                    contextoExtra.append(contextoMaterial.trim());
                }
                if (objetivos != null && !objetivos.isBlank()) {
                    if (contextoExtra.length() > 0) contextoExtra.append(" | ");
                    contextoExtra.append("Objetivos: ").append(objetivos.trim());
                }
                if (bibliografia != null && !bibliografia.isBlank()) {
                    if (contextoExtra.length() > 0) contextoExtra.append(" | ");
                    contextoExtra.append("BibliografÃ­a: ").append(bibliografia.trim());
                }
                if (contextoExtra.length() > 0) {
                    contextoMateriales = contextoExtra.toString();
                }
            }

            // Extraer texto real de los archivos para contexto (PDF/DOCX/etc)
            String textoArchivos = extraerTextoMateriales(materialesIds);
            if (materialesIds != null && !materialesIds.isEmpty()
                    && (textoArchivos == null || textoArchivos.isBlank())) {
                throw new RuntimeException("No se pudo extraer texto de los archivos seleccionados. "
                        + "Verifica que el PDF tenga texto y no sea solo una imagen.");
            }
            ContextoIA contexto = construirContexto(temas, contextoMateriales, objetivos, bibliografia, textoArchivos);


            // GENERACION DE PREGUNTAS CON LLM (tipos permitidos)
            int cantidad = 3;
            if (cantidadParam != null && cantidadParam > 0) {
                cantidad = cantidadParam;
            } else if (pool.getCantidadPreguntas() != null && pool.getCantidadPreguntas() > 0) {
                cantidad = pool.getCantidadPreguntas();
            }
            if (cantidad > 8) {
                throw new RuntimeException("El maximo de preguntas para un pool IA es 8.");
            }
            int minNuevas = 0;
            if (minNuevasParam != null && minNuevasParam > 0) {
                minNuevas = Math.min(minNuevasParam, cantidad);
            }

            // Fix: No reemplazar la coleccion gestionada por Hibernate.
            if (pool.getPreguntas() != null) {
                pool.getPreguntas().clear();
            } else {
                pool.setPreguntas(new ArrayList<>());
            }

            if (excluirEnunciados.size() > 60) {
                excluirEnunciados = new ArrayList<>(excluirEnunciados.subList(0, 60));
            }

            Set<String> usados = new HashSet<>();
            Set<String> excluirSet = new HashSet<>();
            if (!excluirEnunciados.isEmpty()) {
                for (String ex : excluirEnunciados) {
                    if (ex == null || ex.isBlank()) continue;
                    excluirSet.add(normalizarEnunciado(ex));
                }
            }

            List<PreguntaGenerada> generadas = new ArrayList<>();
            int nuevasCount = 0;
            int maxBuffer = cantidad + 3;

            // Fase 1: priorizar preguntas nuevas (evitar enunciados excluidos)
            int intentos = 0;
            while (generadas.size() < cantidad && intentos < 3) {
                int falta = cantidad - generadas.size();
                int solicitar = Math.min(cantidad + 2, falta + 3);
                List<PreguntaGenerada> lote = generarPreguntasConLlm(
                        contexto,
                        temas,
                        solicitar,
                        excluirEnunciados,
                        new HashSet<>(usados)
                );
                nuevasCount = agregarFiltradas(
                        lote,
                        generadas,
                        usados,
                        excluirSet,
                        minNuevas,
                        nuevasCount,
                        true,
                        maxBuffer
                );
                intentos++;
            }

            if (generadas.size() < cantidad) {
                List<PreguntaGenerada> basicas = generarPreguntasBasicas(
                        contexto,
                        cantidad - generadas.size(),
                        new HashSet<>(usados)
                );
                nuevasCount = agregarFiltradas(
                        basicas,
                        generadas,
                        usados,
                        excluirSet,
                        minNuevas,
                        nuevasCount,
                        true,
                        maxBuffer
                );
            }

            // Fase 2: completar con repetidas si hace falta (sin excluir)
            if (generadas.size() < cantidad) {
                int intentosExtra = 0;
                while (generadas.size() < cantidad && intentosExtra < 2) {
                    int falta = cantidad - generadas.size();
                    int solicitar = Math.min(cantidad + 2, falta + 3);
                    List<PreguntaGenerada> lote = generarPreguntasConLlm(
                            contexto,
                            temas,
                            solicitar,
                            Collections.emptyList(),
                            new HashSet<>(usados)
                    );
                    nuevasCount = agregarFiltradas(
                            lote,
                            generadas,
                            usados,
                            excluirSet,
                            minNuevas,
                            nuevasCount,
                            false,
                            maxBuffer
                    );
                    intentosExtra++;
                }
            }

            if (generadas.size() < cantidad) {
                List<PreguntaGenerada> basicas = generarPreguntasBasicas(
                        contexto,
                        cantidad - generadas.size(),
                        new HashSet<>(usados)
                );
                nuevasCount = agregarFiltradas(
                        basicas,
                        generadas,
                        usados,
                        excluirSet,
                        minNuevas,
                        nuevasCount,
                        false,
                        maxBuffer
                );
            }

            if (generadas.size() < cantidad) {
                throw new RuntimeException("No se pudieron generar suficientes preguntas. Se generaron "
                        + generadas.size() + " de " + cantidad);
            }

            int creadas = 0;
            for (PreguntaGenerada generada : generadas) {
                if (creadas >= cantidad) {
                    break;
                }
                if (generada == null || generada.enunciado == null || generada.enunciado.isBlank()) {
                    continue;
                }
                TipoPregunta tipo = generada.tipo != null ? generada.tipo : TipoPregunta.MULTIPLE_CHOICE;
                if (!TIPOS_PERMITIDOS.contains(tipo)) {
                    continue;
                }

                List<Map<String, Object>> opciones = normalizarOpcionesPorTipo(tipo, generada.opciones, contexto);
                if (opciones.isEmpty()) {
                    continue;
                }

                String key = normalizarEnunciado(generada.enunciado);
                if (!key.isBlank()) {
                    usados.add(key);
                }

                Pregunta p = new Pregunta();
                p.setIdPregunta(UUID.randomUUID());
                p.setTipoPregunta(tipo);
                p.setPool(pool);
                p.setPuntaje(1.0f);
                p.setEnunciado(generada.enunciado);

                Pregunta pGuardada = preguntaRepository.save(p);
                for (Map<String, Object> map : opciones) {
                    crearOpcion(pGuardada, (String) map.get("desc"), (Boolean) map.get("val"));
                }
                creadas++;
            }

            if (creadas < cantidad) {
                throw new RuntimeException("No se pudieron crear preguntas suficientes. Se crearon "
                        + creadas + " de " + cantidad);
            }

            pool.setIaStatus(IaGenerationStatus.READY);
            poolRepository.save(pool);
            
        } catch (Exception e) {
            pool.setIaStatus(IaGenerationStatus.FAILED);
            pool.setIaErrorMessage(e.getMessage());
            poolRepository.save(pool);
        }
    }

    private int agregarFiltradas(List<PreguntaGenerada> lote,
            List<PreguntaGenerada> destino,
            Set<String> usados,
            Set<String> excluirSet,
            int minNuevas,
            int nuevasCount,
            boolean exigirNuevas,
            int maxCantidad) {
        if (lote == null || lote.isEmpty()) {
            return nuevasCount;
        }
        for (PreguntaGenerada pg : lote) {
            if (destino.size() >= maxCantidad) {
                break;
            }
            if (pg == null || pg.enunciado == null || pg.enunciado.isBlank()) {
                continue;
            }
            String key = normalizarEnunciado(pg.enunciado);
            if (key.isBlank() || (usados != null && usados.contains(key))) {
                continue;
            }
            boolean esNueva = excluirSet == null || !excluirSet.contains(key);
            if (exigirNuevas && minNuevas > 0 && nuevasCount < minNuevas && !esNueva) {
                continue;
            }
            destino.add(pg);
            if (usados != null) {
                usados.add(key);
            }
            if (esNueva) {
                nuevasCount++;
            }
        }
        return nuevasCount;
    }
    
    private String generarEnunciadoContextual(int index, String tema) {
         String[] templates = {
            "¿Cuál es la característica fundamental de " + tema + " según el material analizado?",
            "Identifique la afirmación correcta respecto al concepto de " + tema + ".",
            "En el contexto estudiado, ¿qué implicaciones tiene el desarrollo de " + tema + "?",
            "Seleccione la opción que mejor describe la relación principal expuesta en el texto.",
            "¿Qué conclusión se puede obtener sobre " + tema + " basándose en la documentación?",
            "El material sugiere que un aspecto crítico de este tema es:"
        };
        return templates[index % templates.length];
    }

    
    
    private PreguntaGenerada construirPregunta(TipoPregunta tipo, int index, String temaRaw, ContextoIA contexto, int intento) {
        String tema = resolverTema(temaRaw, contexto, index);
        String materialHint = construirMaterialHint(contexto);
        boolean compleja = (index % 2 == 1);

        List<Map<String, Object>> opciones = new ArrayList<>();
        Set<String> opcionesUsadas = new HashSet<>();
        String enunciado;

        switch (tipo) {
            case MULTIPLE_CHOICE: {
                String[] templates = compleja ? new String[] {
                        "Selecciona TODAS las afirmaciones correctas sobre " + tema + " y su aplicacion." + materialHint,
                        "Cuales enunciados son verdaderos respecto a " + tema + " en el material?" + materialHint,
                        "Marca las opciones correctas segun el material y el contexto de " + tema + "." + materialHint
                } : new String[] {
                        "Selecciona TODAS las afirmaciones correctas sobre " + tema + "." + materialHint,
                        "Cuales enunciados son verdaderos respecto a " + tema + "?" + materialHint,
                        "Marca las opciones correctas segun el material sobre " + tema + "." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];

                String correcta1 = obtenerAfirmacionCorrecta(contexto, tema, compleja, index + intento);
                String correcta2 = obtenerAfirmacionCorrecta(contexto, tema, compleja, index + intento + 1);
                String incorrecta1 = obtenerAfirmacionIncorrecta(contexto, tema, compleja, correcta1);
                String incorrecta2 = obtenerAfirmacionIncorrecta(contexto, tema, compleja, correcta2);

                agregarOpcionUnica(opciones, opcionesUsadas, correcta1, true);
                agregarOpcionUnica(opciones, opcionesUsadas, correcta2, true);
                agregarOpcionUnica(opciones, opcionesUsadas, incorrecta1, false);
                agregarOpcionUnica(opciones, opcionesUsadas, incorrecta2, false);
                Collections.shuffle(opciones);
                break;
            }
            case VERDADERO_FALSO: {
                boolean esVerdadera = ThreadLocalRandom.current().nextBoolean();
                String base = obtenerAfirmacionCorrecta(contexto, tema, compleja, index + intento);
                if (!esVerdadera) {
                    base = mutarSentencia(base, contexto, tema);
                }
                enunciado = limitarTexto(base, 220);
                opciones.add(Map.of("desc", "Verdadero", "val", esVerdadera));
                opciones.add(Map.of("desc", "Falso", "val", !esVerdadera));
                break;
            }
            case RESPUESTA_CORTA: {
                String[] templates = compleja ? new String[] {
                        "Resume en una frase la idea clave de " + tema + " segun el material." + materialHint,
                        "Menciona dos aspectos centrales de " + tema + " en el material." + materialHint
                } : new String[] {
                        "En una frase, define " + tema + " segun el material." + materialHint,
                        "Resume brevemente que es " + tema + "." + materialHint,
                        "Explica en una oracion el concepto de " + tema + "." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];
                String sugerencia = contexto != null ? contexto.pickKeyword(index + intento) : "";
                String respuesta = sugerencia != null && !sugerencia.isBlank()
                        ? "Respuesta esperada: mencion de " + sugerencia + " y concepto central."
                        : "Respuesta esperada: definicion clara y precisa del concepto.";
                agregarOpcionUnica(opciones, opcionesUsadas, respuesta, true);
                break;
            }
            case DESCRIPCION_LARGA: {
                String[] templates = compleja ? new String[] {
                        "Analiza como se relaciona " + tema + " con el contexto del material." + materialHint,
                        "Explica con ejemplos la aplicacion de " + tema + " segun el material." + materialHint
                } : new String[] {
                        "Describa detalladamente el impacto de " + tema + " en el contexto estudiado." + materialHint,
                        "Desarrolle con ejemplos como se aplica " + tema + " segun el material." + materialHint,
                        "Analice criticamente el rol de " + tema + " en el marco del contenido visto." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];
                agregarOpcionUnica(opciones, opcionesUsadas,
                    "Respuesta modelo: desarrollo coherente con ejemplos y conceptos del material.", true);
                break;
            }
            case EMPAREJAMIENTO: {
                String a = contexto != null ? contexto.pickKeyword(index) : "";
                String b = contexto != null ? contexto.pickKeyword(index + 1) : "";
                String c = contexto != null ? contexto.pickKeyword(index + 2) : "";
                if (a == null || a.isBlank()) a = "Concepto A";
                if (b == null || b.isBlank()) b = "Concepto B";
                if (c == null || c.isBlank()) c = "Concepto C";

                enunciado = "Relaciona conceptos con sus definiciones principales sobre " + tema + ".";
                String correcta = "A->1, B->2, C->3";
                String incorrecta1 = "A->2, B->1, C->3";
                String incorrecta2 = "A->3, B->2, C->1";

                agregarOpcionUnica(opciones, opcionesUsadas,
                    "Emparejamiento correcto: " + correcta + " (" + a + ", " + b + ", " + c + ")", true);
                agregarOpcionUnica(opciones, opcionesUsadas,
                    "Emparejamiento alternativo: " + incorrecta1, false);
                agregarOpcionUnica(opciones, opcionesUsadas,
                    "Emparejamiento alternativo: " + incorrecta2, false);
                Collections.shuffle(opciones);
                break;
            }
            case AUTOCOMPLETADO: {
                String palabra = contexto != null ? contexto.pickKeyword(index + intento) : "";
                if (palabra == null || palabra.isBlank()) {
                    palabra = "dinamico";
                }
                String[] templates = {
                    "Complete la siguiente frase: El concepto de " + tema + " se define como ________." + materialHint,
                    "Complete: " + tema + " es un proceso ________ que permite la integracion de sistemas." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];

                agregarOpcionUnica(opciones, opcionesUsadas, palabra, true);
                String distr1 = contexto != null ? contexto.pickKeyword(index + intento + 1) : "";
                String distr2 = contexto != null ? contexto.pickKeyword(index + intento + 2) : "";
                if (distr1 == null || distr1.isBlank() || distr1.equalsIgnoreCase(palabra)) distr1 = "aislado";
                if (distr2 == null || distr2.isBlank() || distr2.equalsIgnoreCase(palabra)) distr2 = "irrelevante";
                agregarOpcionUnica(opciones, opcionesUsadas, distr1, false);
                agregarOpcionUnica(opciones, opcionesUsadas, distr2, false);
                Collections.shuffle(opciones);
                break;
            }
            case UNICA_RESPUESTA:
            default: {
                String[] templates = compleja ? new String[] {
                        "Segun el material, que implicacion se desprende de " + tema + "?" + materialHint,
                        "Que afirmacion resume mejor la aplicacion de " + tema + " en el contexto visto?" + materialHint
                } : new String[] {
                        generarEnunciadoContextual(index + intento, tema),
                        "Segun el material, cual es la idea central de " + tema + "?" + materialHint,
                        "Que afirmacion describe mejor " + tema + " en el contexto visto?" + materialHint,
                        "Seleccione la opcion correcta sobre " + tema + "." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];
                String correcta = obtenerAfirmacionCorrecta(contexto, tema, compleja, index + intento);
                agregarOpcionUnica(opciones, opcionesUsadas, correcta, true);
                agregarOpcionUnica(opciones, opcionesUsadas, obtenerAfirmacionIncorrecta(contexto, tema, compleja, correcta), false);
                agregarOpcionUnica(opciones, opcionesUsadas, obtenerAfirmacionIncorrecta(contexto, tema, compleja, correcta + " alt"), false);
                agregarOpcionUnica(opciones, opcionesUsadas, "Ninguna de las opciones describe adecuadamente el concepto.", false);
                Collections.shuffle(opciones);
                if (opciones.size() > 4) opciones = opciones.subList(0, 4);
                break;
            }
        }

        if (opciones.isEmpty()) {
            agregarOpcionUnica(opciones, opcionesUsadas, "Respuesta esperada segun el material.", true);
        }

        return new PreguntaGenerada(enunciado, opciones, tipo);
    }

    private PreguntaGenerada construirPreguntaFallback(TipoPregunta tipo, int index, String temaRaw, ContextoIA contexto) {
        String tema = resolverTema(temaRaw, contexto, index);
        String enunciado = "Explique brevemente " + tema + " (variante " + (index + 1) + ").";
        String hint = construirMaterialHint(contexto);
        if (!hint.isBlank()) {
            enunciado += hint;
        }
        List<Map<String, Object>> opciones = new ArrayList<>();
        opciones.add(Map.of("desc", "Respuesta esperada coherente con el material.", "val", true));
        return new PreguntaGenerada(enunciado, opciones, tipo);
    }

    private String resolverTema(String temaRaw, ContextoIA contexto, int index) {
        if (temaRaw != null && !temaRaw.isBlank()) {
            return temaRaw.trim();
        }
        if (contexto != null) {
            String kw = contexto.pickKeyword(index);
            if (kw != null && !kw.isBlank()) {
                return kw;
            }
        }
        return "el tema principal";
    }

    private String construirMaterialHint(ContextoIA contexto) {
        if (contexto == null || contexto.hint == null || contexto.hint.isBlank()) {
            return "";
        }
        return " " + limitarTexto(contexto.hint, 180);
    }

    private String obtenerAfirmacionCorrecta(ContextoIA contexto, String tema, boolean compleja, int seed) {
        if (contexto != null) {
            String sentencia = contexto.pickSentence(seed);
            if (sentencia != null && !sentencia.isBlank()) {
                return limitarTexto(sentencia, 220);
            }
        }
        String baseTema = tema != null && !tema.isBlank() ? tema : "el tema";
        String[] templates = compleja ? new String[] {
                "Segun el material, " + baseTema + " influye en el desempeno del sistema.",
                "El material indica que " + baseTema + " tiene implicaciones directas en el contexto estudiado."
        } : new String[] {
                "El material define " + baseTema + " como un concepto central del tema.",
                baseTema + " es fundamental para comprender el contenido presentado."
        };
        return templates[Math.abs(seed) % templates.length];
    }

    private String obtenerAfirmacionIncorrecta(ContextoIA contexto, String tema, boolean compleja, String base) {
        String mutada = mutarSentencia(base, contexto, tema);
        if (mutada != null && !mutada.equals(base)) {
            return limitarTexto(mutada, 220);
        }
        return generarRespuestaSimulada(tema, false);
    }

    private String mutarSentencia(String sentencia, ContextoIA contexto, String tema) {
        if (sentencia == null || sentencia.isBlank()) {
            return sentencia;
        }
        String resultado = sentencia;
        if (contexto != null && contexto.keywords.size() >= 2) {
            for (String kw : contexto.keywords) {
                if (kw == null || kw.isBlank()) continue;
                if (resultado.toLowerCase(Locale.ROOT).contains(kw.toLowerCase(Locale.ROOT))) {
                    for (String reemplazo : contexto.keywords) {
                        if (reemplazo == null || reemplazo.isBlank()) continue;
                        if (!reemplazo.equalsIgnoreCase(kw)) {
                            return resultado.replaceFirst("(?i)\\b" + Pattern.quote(kw) + "\\b", reemplazo);
                        }
                    }
                }
            }
        }
        if (resultado.toLowerCase(Locale.ROOT).contains(" es ")) {
            return resultado.replaceFirst(" es ", " no es ");
        }
        if (resultado.toLowerCase(Locale.ROOT).contains(" son ")) {
            return resultado.replaceFirst(" son ", " no son ");
        }
        return "No es correcto afirmar que " + resultado;
    }

    private String limitarTexto(String texto, int max) {
        if (texto == null) return "";
        String limpio = texto.replaceAll("\\s+", " ").trim();
        if (limpio.length() <= max) return limpio;
        return limpio.substring(0, Math.max(0, max - 3)) + "...";
    }

    private boolean tieneSoporteEnMaterial(String texto, String materialNorm) {
        if (texto == null || texto.isBlank()) return false;
        if (materialNorm == null || materialNorm.isBlank()) return false;
        String norm = normalizarEnunciado(texto);
        if (norm.isBlank()) return false;
        String padded = " " + materialNorm + " ";
        String[] tokens = norm.split("\\s+");
        int total = 0;
        int match = 0;
        for (String tok : tokens) {
            if (tok.length() < 4) continue;
            if (STOPWORDS.contains(tok)) continue;
            total++;
            if (padded.contains(" " + tok + " ")) {
                match++;
            }
        }
        if (total == 0) {
            return padded.contains(" " + norm + " ");
        }
        int needed = (int) Math.ceil(total * 0.6);
        return match >= Math.max(1, needed);
    }


    
    private List<PreguntaGenerada> generarPreguntasConLlm(ContextoIA contexto, String temas, int cantidad,
            List<String> excluirEnunciados, Set<String> usados) {
        String textoBase = (contexto != null && contexto.texto != null) ? contexto.texto : "";
        String material = limitarTexto(textoBase, 8000);
        String materialNorm = normalizarEnunciado(textoBase);
        String keywords = (contexto != null && contexto.keywords != null && !contexto.keywords.isEmpty())
                ? String.join(", ", contexto.keywords)
                : "";

        String systemPrompt = "Eres un docente experto. Generas preguntas basadas SOLO en el material. " +
                "No inventes datos. Si no hay evidencia, no generes la pregunta. " +
                "Devuelves exclusivamente JSON valido sin texto extra.";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Genera ").append(cantidad).append(" preguntas de opcion multiple. ");
        userPrompt.append("Requisitos: 4 opciones por pregunta, exactamente 1 correcta, preguntas originales, ");
        userPrompt.append("mezcla de basicas y algo complejas, sin inventar fuera del material. ");
        userPrompt.append("Usa SOLO estos tipos: MULTIPLE_CHOICE, VERDADERO_FALSO, UNICA_RESPUESTA. ");
        userPrompt.append("Incluye el campo 'type' en cada pregunta. ");
        userPrompt.append("Si type es VERDADERO_FALSO, usa afirmaciones literales del material (Verdadero). ");
        userPrompt.append("Evita inferencias: usa datos o frases del material. ");
        userPrompt.append("Piensa antes de responder y verifica que todo este respaldado por el material. ");
        userPrompt.append("Incluye un campo 'evidence' con una cita corta y literal del material ");
        userPrompt.append("(max 160 caracteres) que justifique la respuesta correcta. ");
        userPrompt.append("Si no puedes, deja evidence vacio. ");
        userPrompt.append("Formato JSON EXACTO: {\"questions\":[{\"type\":\"MULTIPLE_CHOICE\",\"question\":\"...\",\"evidence\":\"...\",\"options\":[");
        userPrompt.append("{\"text\":\"...\",\"correct\":true},{\"text\":\"...\",\"correct\":false},");
        userPrompt.append("{\"text\":\"...\",\"correct\":false},{\"text\":\"...\",\"correct\":false}]}]}");
        userPrompt.append("\n\nTemas: ").append(temas != null ? temas : "");
        userPrompt.append("\nKeywords: ").append(keywords);
        if (excluirEnunciados != null && !excluirEnunciados.isEmpty()) {
            int max = Math.min(excluirEnunciados.size(), 20);
            userPrompt.append("\nNo repitas ni parafrasees estas preguntas:");
            for (int i = 0; i < max; i++) {
                userPrompt.append("\n- ").append(excluirEnunciados.get(i));
            }
        }
        userPrompt.append("\nMaterial:\n").append(material);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt.toString()));

        String raw = llamarOllamaChat(messages);
        String json = extraerJson(raw);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        try {
            Map<String, Object> root = objectMapper.readValue(
                    json, new TypeReference<Map<String, Object>>() {});
            Object questionsObj = root.get("questions");
            if (!(questionsObj instanceof List<?> qList)) {
                return new ArrayList<>();
            }

            Set<String> usadosLocal = (usados != null) ? usados : new HashSet<>();
            List<PreguntaGenerada> result = new ArrayList<>();
            for (Object qObj : qList) {
                if (!(qObj instanceof Map<?, ?> qMap)) {
                    continue;
                }
                String question = toStringSafe(qMap.get("question"));
                if (question == null || question.isBlank()) {
                    continue;
                }
                String key = normalizarEnunciado(question);
                if (key.isBlank() || usadosLocal.contains(key)) {
                    continue;
                }

                String evidence = toStringSafe(qMap.get("evidence"));
                boolean evidenciaValida = false;
                if (evidence != null && !evidence.isBlank()) {
                    evidenciaValida = tieneSoporteEnMaterial(evidence, materialNorm);
                }

                String typeRaw = toStringSafe(qMap.get("type"));
                TipoPregunta tipo = parseTipoPregunta(typeRaw);
                if (tipo == null) {
                    tipo = TipoPregunta.MULTIPLE_CHOICE;
                }
                if (!TIPOS_PERMITIDOS.contains(tipo)) {
                    continue;
                }

                List<Map<String, Object>> opciones = new ArrayList<>();
                Object optionsObj = qMap.get("options");
                Object correctIndexObj = qMap.get("correctIndex");
                int correctIndex = -1;
                if (correctIndexObj instanceof Number) {
                    correctIndex = ((Number) correctIndexObj).intValue();
                }

                boolean tieneCorrecta = false;
                if (optionsObj instanceof List<?> optList) {
                    int idx = 0;
                    for (Object optObj : optList) {
                        if (optObj instanceof Map<?, ?> optMap) {
                            String textOpt = toStringSafe(optMap.get("text"));
                            Object correctObj = optMap.get("correct");
                            boolean correct = correctObj instanceof Boolean && (Boolean) correctObj;
                            if (textOpt == null || textOpt.isBlank()) {
                                idx++;
                                continue;
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("desc", textOpt);
                            boolean isCorrect = correct || (correctIndex >= 0 && idx == correctIndex);
                            if (isCorrect) {
                                tieneCorrecta = true;
                            }
                            map.put("val", isCorrect);
                            opciones.add(map);
                        } else {
                            String textOpt = toStringSafe(optObj);
                            if (textOpt != null && !textOpt.isBlank()) {
                                Map<String, Object> map = new HashMap<>();
                                boolean isCorrect = correctIndex >= 0 && idx == correctIndex;
                                if (isCorrect) {
                                    tieneCorrecta = true;
                                }
                                map.put("desc", textOpt);
                                map.put("val", isCorrect);
                                opciones.add(map);
                            }
                        }
                        idx++;
                    }
                }

                if (tipo == TipoPregunta.VERDADERO_FALSO) {
                    boolean soportada = evidenciaValida || tieneSoporteEnMaterial(question, materialNorm);
                    if (!soportada) {
                        continue;
                    }
                    // Forzar Verdadero como correcto cuando la afirmacion esta en el material
                    opciones = Collections.emptyList();
                } else {
                    if (!tieneCorrecta) {
                        continue;
                    }
                    String correctText = "";
                    for (Map<String, Object> opt : opciones) {
                        boolean val = opt.get("val") instanceof Boolean && (Boolean) opt.get("val");
                        if (val) {
                            correctText = toStringSafe(opt.get("desc"));
                            break;
                        }
                    }
                    String correctNorm = normalizarEnunciado(correctText);
                    boolean soportada = evidenciaValida
                            || tieneSoporteEnMaterial(question, materialNorm)
                            || tieneSoporteEnMaterial(correctText, materialNorm);
                    if (!soportada) {
                        continue;
                    }
                }

                usadosLocal.add(key);
                result.add(new PreguntaGenerada(question, opciones, tipo));
                if (result.size() >= cantidad) {
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String llamarOllamaChat(List<Map<String, Object>> messages) {
        try {
            String modelo = iaConfig.getModelName();
            if (modelo == null || modelo.isBlank()) {
                modelo = "llama3";
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelo);
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                    "temperature", 0.15,
                    "top_p", 0.9,
                    "top_k", 40,
                    "repeat_penalty", 1.1,
                    "num_predict", 1600
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Object responseObj = restTemplate.postForObject(
                    iaConfig.getChatEndpoint(),
                    request,
                    Object.class
            );

            if (responseObj instanceof Map<?, ?> response && response.containsKey("message")) {
                Object messageObj = response.get("message");
                if (messageObj instanceof Map<?, ?> message && message.containsKey("content")) {
                    Object content = message.get("content");
                    if (content instanceof String) {
                        return (String) content;
                    }
                }
            }
            return "";
        } catch (ResourceAccessException e) {
            throw new RuntimeException("No se pudo conectar con Ollama en " + iaConfig.getOllamaBaseUrl(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar preguntas con LLM: " + e.getMessage(), e);
        }
    }

    private String extraerJson(String raw) {
        if (raw == null) return "";
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw.trim();
    }

    
    private List<Map<String, Object>> normalizarOpcionesPorTipo(TipoPregunta tipo, List<Map<String, Object>> opciones, ContextoIA contexto) {
        if (tipo == TipoPregunta.VERDADERO_FALSO) {
            return construirOpcionesVerdaderoFalso(opciones);
        }
        int maxCorrect = (tipo == TipoPregunta.MULTIPLE_CHOICE) ? 2 : 1;
        return normalizarOpcionesBase(opciones, contexto, maxCorrect);
    }

    private List<Map<String, Object>> construirOpcionesVerdaderoFalso(List<Map<String, Object>> opciones) {
        boolean correctaVerdadero = true;
        if (opciones != null) {
            for (Map<String, Object> op : opciones) {
                String desc = toStringSafe(op.get("desc")).toLowerCase(Locale.ROOT);
                boolean val = op.get("val") instanceof Boolean && (Boolean) op.get("val");
                if (val && desc.contains("falso")) {
                    correctaVerdadero = false;
                    break;
                }
                if (val && desc.contains("verdadero")) {
                    correctaVerdadero = true;
                    break;
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of("desc", "Verdadero", "val", correctaVerdadero));
        result.add(Map.of("desc", "Falso", "val", !correctaVerdadero));
        return result;
    }

    private List<Map<String, Object>> normalizarOpcionesBase(List<Map<String, Object>> opciones, ContextoIA contexto, int maxCorrect) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (opciones == null) {
            opciones = new ArrayList<>();
        }

        Set<String> usados = new HashSet<>();
        List<Map<String, Object>> correctas = new ArrayList<>();

        for (Map<String, Object> op : opciones) {
            String desc = toStringSafe(op.get("desc"));
            if (desc == null || desc.isBlank()) continue;
            String key = normalizarEnunciado(desc);
            if (!usados.add(key)) continue;

            boolean val = op.get("val") instanceof Boolean && (Boolean) op.get("val");
            Map<String, Object> map = new HashMap<>();
            map.put("desc", desc);
            map.put("val", val);
            if (val) {
                correctas.add(map);
            }
            result.add(map);
        }

        if (correctas.isEmpty() && !result.isEmpty()) {
            result.get(0).put("val", true);
            correctas.add(result.get(0));
        }

        if (correctas.size() > maxCorrect) {
            int keep = 0;
            for (Map<String, Object> op : result) {
                if (Boolean.TRUE.equals(op.get("val"))) {
                    if (keep < maxCorrect) {
                        keep++;
                    } else {
                        op.put("val", false);
                    }
                }
            }
        }

        int intentos = 0;
        while (result.size() < 4 && intentos < 6) {
            String distractor = generarDistractor(contexto, usados, intentos);
            String key = normalizarEnunciado(distractor);
            if (usados.add(key)) {
                Map<String, Object> map = new HashMap<>();
                map.put("desc", distractor);
                map.put("val", false);
                result.add(map);
            }
            intentos++;
        }

        if (result.size() > 4) {
            result = result.subList(0, 4);
        }

        boolean tieneCorrecta = result.stream().anyMatch(op -> Boolean.TRUE.equals(op.get("val")));
        if (!tieneCorrecta && !result.isEmpty()) {
            result.get(0).put("val", true);
        }

        return result;
    }

    private TipoPregunta parseTipoPregunta(String raw) {
        if (raw == null) return null;
        String val = raw.trim().toUpperCase(Locale.ROOT);
        if ("MULTIPLE_CHOICE".equals(val)) return TipoPregunta.MULTIPLE_CHOICE;
        if ("VERDADERO_FALSO".equals(val)) return TipoPregunta.VERDADERO_FALSO;
        if ("UNICA_RESPUESTA".equals(val)) return TipoPregunta.UNICA_RESPUESTA;
        return null;
    }

private String generarDistractor(ContextoIA contexto, Set<String> usados, int index) {
        if (contexto != null && contexto.keywords != null && !contexto.keywords.isEmpty()) {
            for (int i = 0; i < contexto.keywords.size(); i++) {
                String kw = contexto.keywords.get((index + i) % contexto.keywords.size());
                if (kw == null || kw.isBlank()) continue;
                String cand = "Relacionado con " + kw;
                if (!usados.contains(normalizarEnunciado(cand))) {
                    return cand;
                }
            }
        }
        return "No se menciona en el material";
    }

    private String toStringSafe(Object value) {
        return value == null ? "" : value.toString();
    }

private ContextoIA construirContexto(String temas, String contextoMateriales, String objetivos, String bibliografia, String textoArchivos) {
        StringBuilder base = new StringBuilder();
        if (textoArchivos != null && !textoArchivos.isBlank()) {
            base.append(textoArchivos).append(" ");
        }
        if (temas != null && !temas.isBlank()) {
            base.append(temas).append(". ");
        }
        if (objetivos != null && !objetivos.isBlank()) {
            base.append("Objetivos: ").append(objetivos).append(". ");
        }
        if (bibliografia != null && !bibliografia.isBlank()) {
            base.append("Bibliografia: ").append(bibliografia).append(". ");
        }

        String textoBase = base.toString();
        String limpio = limpiarTexto(textoBase);
        List<String> sentencias = extraerSentencias(textoBase);
        List<String> keywords = extraerKeywords(limpio);
        String snippet = !limpio.isBlank() ? limitarTexto(limpio, 240) : "";
        String hint = (contextoMateriales != null && !contextoMateriales.isBlank())
                ? contextoMateriales
                : (snippet.isBlank() ? "" : "Contexto: " + snippet);

        return new ContextoIA(limpio, sentencias, keywords, hint);
    }

    private String extraerTextoMateriales(List<Long> materialesIds) {
        if (materialesIds == null || materialesIds.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        List<Archivo> archivos = archivoRepository.findByMaterial_IdActividadIn(materialesIds);
        for (Archivo archivo : archivos) {
            String texto = extraerTextoArchivo(archivo);
            if (texto != null && !texto.isBlank()) {
                sb.append(texto).append(" ");
            }
            if (sb.length() > 12000) {
                break;
            }
        }

        return compactarTexto(sb.toString(), 12000);
    }

    private String extraerTextoArchivo(Archivo archivo) {
        if (archivo == null || archivo.getContenido() == null || archivo.getContenido().length == 0) {
            return "";
        }
        String nombre = archivo.getNombre() != null ? archivo.getNombre() : "";
        String mime = archivo.getTipoMime() != null ? archivo.getTipoMime() : "";
        String ext = obtenerExtension(nombre);

        try {
            if (mime.contains("pdf") || "pdf".equals(ext)) {
                return extraerTextoPdf(archivo.getContenido());
            }
            if (mime.contains("word") || "docx".equals(ext)) {
                return extraerTextoDocx(archivo.getContenido());
            }
            if (mime.contains("presentation") || "pptx".equals(ext)) {
                return extraerTextoPptx(archivo.getContenido());
            }
            if (mime.contains("spreadsheet") || "xlsx".equals(ext)) {
                return extraerTextoXlsx(archivo.getContenido());
            }
            if (mime.startsWith("text/") || "txt".equals(ext)) {
                return compactarTexto(decodificarTextoPlano(archivo.getContenido()), 4000);
            }
        } catch (Exception e) {
            return "";
        }

        return "";
    }

    private String extraerTextoPdf(byte[] data) {
        PdfReader reader = null;
        try {
            reader = new PdfReader(data);
            StringBuilder sb = new StringBuilder();
            int pages = Math.min(reader.getNumberOfPages(), 15);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int i = 1; i <= pages; i++) {
                sb.append(extractor.getTextFromPage(i)).append(" ");
            }
            return compactarTexto(sb.toString(), 4000);
        } catch (Exception e) {
            return "";
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private String extraerTextoDocx(byte[] data) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return compactarTexto(extractor.getText(), 4000);
        } catch (IOException e) {
            return "";
        }
    }

    private String extraerTextoPptx(byte[] data) {
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(data))) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        sb.append(textShape.getText()).append(" ");
                        count++;
                    }
                }
                if (count > 200) break;
            }
            return compactarTexto(sb.toString(), 4000);
        } catch (IOException e) {
            return "";
        }
    }

    private String extraerTextoXlsx(byte[] data) {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            StringBuilder sb = new StringBuilder();
            DataFormatter formatter = new DataFormatter();
            int cells = 0;
            for (Sheet sheet : wb) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        String value = formatter.formatCellValue(cell);
                        if (value != null && !value.isBlank()) {
                            sb.append(value).append(" ");
                            cells++;
                            if (cells > 2000) {
                                return compactarTexto(sb.toString(), 4000);
                            }
                        }
                    }
                }
            }
            return compactarTexto(sb.toString(), 4000);
        } catch (IOException e) {
            return "";
        }
    }

    private String decodificarTextoPlano(byte[] data) {
        String utf8 = new String(data, StandardCharsets.UTF_8);
        if (utf8.contains("�")) {
            return new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        }
        return utf8;
    }

    private String obtenerExtension(String nombre) {
        if (nombre == null) return "";
        int idx = nombre.lastIndexOf('.');
        if (idx < 0 || idx == nombre.length() - 1) return "";
        return nombre.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String compactarTexto(String texto, int max) {
        return limitarTexto(limpiarTexto(texto), max);
    }

    private String limpiarTexto(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("\\s+", " ").trim();
    }

    private List<String> extraerSentencias(String texto) {
        List<String> sentencias = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return sentencias;
        }
        String[] partes = texto.split("(?<=[.!?])\\s+");
        for (String parte : partes) {
            String s = limpiarTexto(parte);
            if (s.isBlank()) continue;
            int palabras = s.split("\\s+").length;
            if (palabras < 6 || palabras > 28) continue;
            if (!sentencias.contains(s)) {
                sentencias.add(s);
            }
            if (sentencias.size() >= 30) break;
        }
        return sentencias;
    }

    private List<PreguntaGenerada> generarPreguntasBasicas(ContextoIA contexto, int cantidad, Set<String> usados) {
        List<PreguntaGenerada> result = new ArrayList<>();
        if (contexto == null || cantidad <= 0) {
            return result;
        }
        List<String> sentencias = contexto.sentencias != null
                ? new ArrayList<>(contexto.sentencias)
                : new ArrayList<>();
        List<String> keywords = contexto.keywords != null
                ? new ArrayList<>(contexto.keywords)
                : new ArrayList<>();

        int idx = 0;
        int tipoIdx = 0;
        while (result.size() < cantidad && idx < sentencias.size()) {
            String sentencia = limitarTexto(sentencias.get(idx), 180);
            idx++;
            if (sentencia == null || sentencia.isBlank()) {
                continue;
            }
            String key = normalizarEnunciado(sentencia);
            if (key.isBlank() || usados.contains(key)) {
                continue;
            }

            TipoPregunta tipo = switch (tipoIdx % 3) {
                case 0 -> TipoPregunta.VERDADERO_FALSO;
                case 1 -> TipoPregunta.UNICA_RESPUESTA;
                default -> TipoPregunta.MULTIPLE_CHOICE;
            };
            tipoIdx++;

            List<String> kwSent = extraerKeywords(sentencia);
            if (tipo != TipoPregunta.VERDADERO_FALSO && kwSent.isEmpty()) {
                tipo = TipoPregunta.VERDADERO_FALSO;
            }

            List<Map<String, Object>> opciones = new ArrayList<>();
            String enunciado;

            if (tipo == TipoPregunta.VERDADERO_FALSO) {
                enunciado = sentencia;
                opciones = Collections.emptyList(); // Verdadero por defecto
            } else if (tipo == TipoPregunta.UNICA_RESPUESTA) {
                String correcta = kwSent.get(0);
                enunciado = "En el material se menciona: \"" + sentencia + "\" Que termino clave aparece?";
                opciones.add(Map.of("desc", correcta, "val", true));
                for (String kw : keywords) {
                    if (opciones.size() >= 4) break;
                    if (kw == null || kw.isBlank()) continue;
                    if (kw.equalsIgnoreCase(correcta)) continue;
                    opciones.add(Map.of("desc", kw, "val", false));
                }
            } else {
                enunciado = "En el material se menciona: \"" + sentencia + "\" Selecciona TODOS los terminos que aparecen.";
                String correcta1 = kwSent.get(0);
                String correcta2 = kwSent.size() > 1 ? kwSent.get(1) : null;
                opciones.add(Map.of("desc", correcta1, "val", true));
                if (correcta2 != null && !correcta2.equalsIgnoreCase(correcta1)) {
                    opciones.add(Map.of("desc", correcta2, "val", true));
                }
                for (String kw : keywords) {
                    if (opciones.size() >= 4) break;
                    if (kw == null || kw.isBlank()) continue;
                    if (kw.equalsIgnoreCase(correcta1)) continue;
                    if (correcta2 != null && kw.equalsIgnoreCase(correcta2)) continue;
                    opciones.add(Map.of("desc", kw, "val", false));
                }
            }

            if (tipo != TipoPregunta.VERDADERO_FALSO && opciones.size() < 3) {
                continue;
            }

            usados.add(key);
            result.add(new PreguntaGenerada(enunciado, opciones, tipo));
        }

        return result;
    }

    private List<String> extraerKeywords(String texto) {
        List<String> resultado = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return resultado;
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
        String[] tokens = normalizado.split("[^a-z0-9]+");
        Map<String, Integer> freq = new HashMap<>();
        for (String token : tokens) {
            if (token.length() < 4) continue;
            if (STOPWORDS.contains(token)) continue;
            freq.put(token, freq.getOrDefault(token, 0) + 1);
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed());
        for (Map.Entry<String, Integer> e : entries) {
            resultado.add(e.getKey());
            if (resultado.size() >= 12) break;
        }
        return resultado;
    }

    private static class ContextoIA {
        private final String texto;
        private final List<String> sentencias;
        private final List<String> keywords;
        private final String hint;

        private ContextoIA(String texto, List<String> sentencias, List<String> keywords, String hint) {
            this.texto = texto;
            this.sentencias = sentencias != null ? sentencias : new ArrayList<>();
            this.keywords = keywords != null ? keywords : new ArrayList<>();
            this.hint = hint != null ? hint : "";
        }

        private String pickSentence(int index) {
            if (sentencias == null || sentencias.isEmpty()) return "";
            return sentencias.get(Math.abs(index) % sentencias.size());
        }

        private String pickKeyword(int index) {
            if (keywords == null || keywords.isEmpty()) return "";
            return keywords.get(Math.abs(index) % keywords.size());
        }
    }

    private void agregarOpcionUnica(List<Map<String, Object>> opciones, Set<String> usados, String desc, boolean val) {
        if (desc == null || desc.isBlank()) return;
        String clave = normalizarEnunciado(desc);
        if (usados.add(clave)) {
            opciones.add(Map.of("desc", desc, "val", val));
        }
    }

    private String normalizarEnunciado(String texto) {
        if (texto == null) return "";
        String norm = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .toLowerCase();
        return norm.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static class PreguntaGenerada {
        private final String enunciado;
        private final List<Map<String, Object>> opciones;
        private final TipoPregunta tipo;

        private PreguntaGenerada(String enunciado, List<Map<String, Object>> opciones, TipoPregunta tipo) {
            this.enunciado = enunciado;
            this.opciones = opciones;
            this.tipo = tipo;
        }
    }
    
    private String generarRespuestaSimulada(String tema, boolean esCorrecta) {
        String[] correctas = {
            "Es un mecanismo que facilita la integración sistemática de los componentes.",
            "Constituye la base teórica fundamental para comprender el fenómeno.",
            "Optimiza los recursos disponibles mejorando la eficiencia global.",
            "Representa una evolución significativa respecto a los modelos anteriores.",
            "Permite una adaptabilidad superior frente a cambios en el entorno."
        };
        
        String[] incorrectas = {
            "Es una metodología que ha caído en desuso por su ineficiencia.",
            "No presenta correlación directa con los objetivos planteados.",
            "Aumenta la complejidad del sistema sin aportar valor real.",
            "Contradice los principios básicos establecidos en la introducción.",
            "Funciona de manera aislada y no afecta al resto del proceso."
        };
        
        if (esCorrecta) {
            return correctas[(int) (Math.random() * correctas.length)];
        } else {
            return incorrectas[(int) (Math.random() * incorrectas.length)];
        }
    }

    private String generarAfirmacion(int index, String tema, boolean esVerdadera) {
        if (esVerdadera) {
            String[] verdaderas = {
                "El concepto de " + tema + " es central para entender la dinámica del sistema propuesto.",
                "Según el texto, " + tema + " tiene un impacto directo en la eficiencia operativa.",
                "La evidencia sugiere que " + tema + " facilita la integración de nuevos componentes."
            };
            return verdaderas[index % verdaderas.length];
        } else {
            String[] falsas = {
                "El documento afirma explícitamente que " + tema + " carece de relevancia en el contexto actual.",
                "Es incorrecto asumir que " + tema + " influye en los resultados finales del proceso.",
                "El autor rechaza la idea de que " + tema + " sea un factor determinante."
            };
            return falsas[index % falsas.length];
        }
    }

    private void crearOpcion(Pregunta p, String desc, boolean correcta) {
        Opcion op = new Opcion();
        op.setIdOpcion(UUID.randomUUID());
        op.setDescripcion(desc);
        op.setEsCorrecta(correcta);
        op.setPregunta(p);
        opcionRepository.save(op);
    }

    private void marcarError(UUID poolId, String mensaje) {
        try {
            Pool pool = poolRepository.findById(poolId).orElse(null);
            if (pool != null) {
                pool.setIaStatus(IaGenerationStatus.FAILED);
                pool.setIaErrorMessage(mensaje);
                poolRepository.save(pool);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
