package com.example.demo.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.enums.IaGenerationStatus;
import com.example.demo.enums.TipoPregunta;
import com.example.demo.model.Opcion;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.repository.OpcionRepository;
import com.example.demo.repository.PoolRepository;
import com.example.demo.repository.PreguntaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private com.example.demo.repository.MaterialRepository materialRepository;

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
            
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(paramsJson);
                if (root.has("temas")) temas = root.get("temas").asText();
                
                if (root.has("materialesIds") && root.get("materialesIds").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode idNode : root.get("materialesIds")) {
                        materialesIds.add(idNode.asLong());
                    }
                }
            } catch (Exception e) {
                // Ignore parse error
            }

            // Cargar metadata de materiales para generar contexto
            java.util.List<String> nombresMateriales = new ArrayList<>();
            String contextoMateriales = "";
            if (!materialesIds.isEmpty()) {
                java.util.List<com.example.demo.model.Material> materiales = materialRepository.findAllById(materialesIds);
                for (com.example.demo.model.Material m : materiales) {
                    nombresMateriales.add(m.getTitulo());
                }
                if (!nombresMateriales.isEmpty()) {
                    contextoMateriales = "Basado en los documentos: " + String.join(", ", nombresMateriales);
                }
            }

            // SIMULACION DE GENERACIÓN DE PREGUNTAS (con deduplicación y variedad)
            int cantidad = pool.getCantidadPreguntas() != null ? pool.getCantidadPreguntas() : 3;
            if (cantidad <= 0) cantidad = 3;
            
            // Fix: No reemplazar la colección gestionada por Hibernate.
            if (pool.getPreguntas() != null) {
                pool.getPreguntas().clear();
            } else {
                pool.setPreguntas(new ArrayList<>());
            }
            
            Set<String> enunciadosUsados = new HashSet<>();
            List<TipoPregunta> tiposDisponibles = new ArrayList<>(Arrays.asList(
                TipoPregunta.MULTIPLE_CHOICE,
                TipoPregunta.VERDADERO_FALSO,
                TipoPregunta.RESPUESTA_CORTA,
                TipoPregunta.DESCRIPCION_LARGA,
                TipoPregunta.UNICA_RESPUESTA,
                TipoPregunta.AUTOCOMPLETADO
            ));
            Collections.shuffle(tiposDisponibles);

            for (int i = 0; i < cantidad; i++) {
                TipoPregunta tipoSeleccionado = tiposDisponibles.get(i % tiposDisponibles.size());
                PreguntaGenerada generada = null;

                for (int intento = 0; intento < 10 && generada == null; intento++) {
                    generada = construirPregunta(tipoSeleccionado, i, temas, contextoMateriales, nombresMateriales, intento);
                    if (generada == null || generada.enunciado == null || generada.enunciado.isBlank()) {
                        generada = null;
                        continue;
                    }
                    String clave = normalizarEnunciado(generada.enunciado);
                    if (enunciadosUsados.contains(clave)) {
                        generada = null;
                    } else {
                        enunciadosUsados.add(clave);
                    }
                }

                if (generada == null) {
                    generada = construirPreguntaFallback(tipoSeleccionado, i, temas, contextoMateriales);
                }

                Pregunta p = new Pregunta();
                p.setIdPregunta(UUID.randomUUID());
                p.setTipoPregunta(tipoSeleccionado);
                p.setPool(pool);
                p.setPuntaje(1.0f);
                p.setEnunciado(generada.enunciado);

                Pregunta pGuardada = preguntaRepository.save(p);
                for (Map<String, Object> map : generada.opciones) {
                    crearOpcion(pGuardada, (String) map.get("desc"), (Boolean) map.get("val"));
                }
            }
            
            pool.setIaStatus(IaGenerationStatus.READY);
            poolRepository.save(pool);
            
        } catch (Exception e) {
            pool.setIaStatus(IaGenerationStatus.FAILED);
            pool.setIaErrorMessage(e.getMessage());
            poolRepository.save(pool);
        }
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

    private PreguntaGenerada construirPregunta(TipoPregunta tipo, int index, String temaRaw, String contextoMateriales, List<String> materiales, int intento) {
        String tema = (temaRaw == null || temaRaw.isBlank()) ? "el tema principal" : temaRaw.trim();
        String materialHint = "";
        String contexto = "";

        List<Map<String, Object>> opciones = new ArrayList<>();
        Set<String> opcionesUsadas = new HashSet<>();
        String enunciado;

        switch (tipo) {
            case MULTIPLE_CHOICE: {
                String[] templates = {
                    "Selecciona TODAS las afirmaciones correctas sobre " + tema + "." + materialHint,
                    "¿Cuáles enunciados son verdaderos respecto a " + tema + "?" + materialHint,
                    "Marca las opciones correctas según el material sobre " + tema + "." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];

                // 2 correctas y 2 incorrectas
                agregarOpcionUnica(opciones, opcionesUsadas, generarRespuestaSimulada(tema, true), true);
                agregarOpcionUnica(opciones, opcionesUsadas, generarRespuestaSimulada(tema, true), true);
                agregarOpcionUnica(opciones, opcionesUsadas, generarRespuestaSimulada(tema, false), false);
                agregarOpcionUnica(opciones, opcionesUsadas, generarRespuestaSimulada(tema, false), false);
                Collections.shuffle(opciones);
                break;
            }
            case VERDADERO_FALSO: {
                boolean esVerdadera = ThreadLocalRandom.current().nextBoolean();
                enunciado = generarAfirmacion(index + intento, tema, esVerdadera);
                opciones.add(Map.of("desc", "Verdadero", "val", esVerdadera));
                opciones.add(Map.of("desc", "Falso", "val", !esVerdadera));
                break;
            }
            case RESPUESTA_CORTA: {
                String[] templates = {
                    "En una frase, define " + tema + " según el material." + materialHint,
                    "Resume brevemente qué es " + tema + "." + materialHint,
                    "Explica en una oración el concepto de " + tema + "." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];
                agregarOpcionUnica(opciones, opcionesUsadas,
                    "Respuesta esperada: definición clara y precisa del concepto.", true);
                break;
            }
            case DESCRIPCION_LARGA: {
                String[] templates = {
                    "Describa detalladamente el impacto de " + tema + " en el contexto estudiado." + materialHint,
                    "Desarrolle con ejemplos cómo se aplica " + tema + " según el material." + materialHint,
                    "Analice críticamente el rol de " + tema + " en el marco del contenido visto." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];
                agregarOpcionUnica(opciones, opcionesUsadas,
                    "Respuesta modelo: desarrollo coherente con ejemplos y conceptos del material.", true);
                break;
            }
            case EMPAREJAMIENTO: {
                enunciado = "Relaciona conceptos con sus definiciones principales sobre " + tema + ".";
                String a = "Concepto A";
                String b = "Concepto B";
                String c = "Concepto C";
                String correcta = "A→1, B→2, C→3";
                String incorrecta1 = "A→2, B→1, C→3";
                String incorrecta2 = "A→3, B→2, C→1";

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
                String[] templates = {
                    "Complete la siguiente frase: El concepto de " + tema + " se define como ________." + materialHint,
                    "Complete: " + tema + " es un proceso ________ que permite la integración de sistemas." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];
                agregarOpcionUnica(opciones, opcionesUsadas, "dinámico", true);
                agregarOpcionUnica(opciones, opcionesUsadas, "aislado", false);
                agregarOpcionUnica(opciones, opcionesUsadas, "irrelevante", false);
                Collections.shuffle(opciones);
                break;
            }
            case UNICA_RESPUESTA:
            default: {
                String[] templates = {
                    generarEnunciadoContextual(index + intento, tema),
                    "Según el material, ¿cuál es la idea central de " + tema + "?" + materialHint,
                    "¿Qué afirmación describe mejor " + tema + " en el contexto visto?" + materialHint,
                    "Seleccione la opción correcta sobre " + tema + "." + materialHint
                };
                enunciado = templates[(index + intento) % templates.length];
                agregarOpcionUnica(opciones, opcionesUsadas, generarRespuestaSimulada(tema, true), true);
                agregarOpcionUnica(opciones, opcionesUsadas, generarRespuestaSimulada(tema, false), false);
                agregarOpcionUnica(opciones, opcionesUsadas, generarRespuestaSimulada(tema, false), false);
                agregarOpcionUnica(opciones, opcionesUsadas, "Ninguna de las opciones describe adecuadamente el concepto.", false);
                Collections.shuffle(opciones);
                if (opciones.size() > 4) opciones = opciones.subList(0, 4);
                break;
            }
        }

        if (opciones.isEmpty()) {
            agregarOpcionUnica(opciones, opcionesUsadas, "Respuesta esperada según el material.", true);
        }

        return new PreguntaGenerada(enunciado, opciones);
    }

    private PreguntaGenerada construirPreguntaFallback(TipoPregunta tipo, int index, String temaRaw, String contextoMateriales) {
        String tema = (temaRaw == null || temaRaw.isBlank()) ? "el tema principal" : temaRaw.trim();
        String enunciado = "Explique brevemente " + tema + " (variante " + (index + 1) + ").";
        if (contextoMateriales != null && !contextoMateriales.isBlank()) {
            enunciado += " " + contextoMateriales;
        }
        List<Map<String, Object>> opciones = new ArrayList<>();
        opciones.add(Map.of("desc", "Respuesta esperada coherente con el material.", "val", true));
        return new PreguntaGenerada(enunciado, opciones);
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

        private PreguntaGenerada(String enunciado, List<Map<String, Object>> opciones) {
            this.enunciado = enunciado;
            this.opciones = opciones;
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
