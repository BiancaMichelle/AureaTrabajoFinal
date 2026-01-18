package com.example.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

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

            // SIMULACION DE GENERACIÓN DE PREGUNTAS
            int cantidad = pool.getCantidadPreguntas() != null ? pool.getCantidadPreguntas() : 3;
            if (cantidad <= 0) cantidad = 3;
            
            // Fix: No reemplazar la colección gestionada por Hibernate.
            if (pool.getPreguntas() != null) {
                pool.getPreguntas().clear();
            } else {
                pool.setPreguntas(new ArrayList<>());
            }
            
            for (int i = 0; i < cantidad; i++) {
                Pregunta p = new Pregunta();
                p.setIdPregunta(UUID.randomUUID());
                
                // Seleccionar tipo de pregunta aleatorio
                TipoPregunta[] tiposDisponibles = {
                    TipoPregunta.UNICA_RESPUESTA, 
                    TipoPregunta.VERDADERO_FALSO, 
                    TipoPregunta.DESCRIPCION_LARGA,
                    TipoPregunta.AUTOCOMPLETADO
                };
                TipoPregunta tipoSeleccionado = tiposDisponibles[(int)(Math.random() * tiposDisponibles.length)];
                
                p.setTipoPregunta(tipoSeleccionado);
                p.setPool(pool);
                p.setPuntaje(1.0f);
                
                String enunciado = "";
                java.util.List<java.util.Map<String, Object>> opciones = new ArrayList<>();
                
                switch (tipoSeleccionado) {
                    case VERDADERO_FALSO:
                        // Generar afirmación
                        boolean esVerdadera = Math.random() < 0.5;
                        enunciado = generarAfirmacion(i, temas, esVerdadera);
                        opciones.add(java.util.Map.of("desc", "Verdadero", "val", esVerdadera));
                        opciones.add(java.util.Map.of("desc", "Falso", "val", !esVerdadera));
                        break;
                        
                    case DESCRIPCION_LARGA:
                        enunciado = "Describa detalladamente el impacto de " + temas + " según lo estudiado en el material.";
                        // Para preguntas abiertas, a veces no hay opciones, o se guarda una respuesta modelo.
                        opciones.add(java.util.Map.of("desc", "Respuesta modelo: El estudiante debe explicar las relaciones causales...", "val", true));
                        break;
                        
                    case AUTOCOMPLETADO:
                        enunciado = "Complete la siguiente frase: El concepto de " + temas + " se define como un proceso ________ que permite la integración de sistemas.";
                        opciones.add(java.util.Map.of("desc", "dinámico", "val", true)); // Palabra correcta
                        // En autocompletado a veces se usan distractores o solo se valida texto exacto.
                        break;
                        
                    case UNICA_RESPUESTA:
                    default:
                        // Lógica existente de múltiple opción
                        enunciado = generarEnunciadoContextual(i, temas);
                        opciones.add(java.util.Map.of("desc", generarRespuestaSimulada(temas, true), "val", true));
                        opciones.add(java.util.Map.of("desc", generarRespuestaSimulada(temas, false), "val", false));
                        opciones.add(java.util.Map.of("desc", generarRespuestaSimulada(temas, false), "val", false));
                        opciones.add(java.util.Map.of("desc", "Ninguna de las opciones describe adecuadamente el concepto.", "val", false));
                        Collections.shuffle(opciones);
                        // Limitar a 4
                        if (opciones.size() > 4) opciones = opciones.subList(0, 4);
                        break;
                }
                
                p.setEnunciado(enunciado);
                Pregunta pGuardada = preguntaRepository.save(p);
                
                for (java.util.Map<String, Object> map : opciones) {
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
