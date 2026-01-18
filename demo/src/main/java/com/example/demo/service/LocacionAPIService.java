package com.example.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.model.Ciudad;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;

import java.util.Arrays;

@Service
public class LocacionAPIService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocacionAPIService.class);
    
    private final RestTemplate restTemplate;
    private final String API_URL = "https://api.countrystatecity.in/v1";
    
    public LocacionAPIService(@Qualifier("locacionRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Obtener todos los países desde la API
     */
    public List<Pais> obtenerTodosPaises() {
        try {
            logger.debug("Obteniendo todos los países desde la API");
            String url = API_URL + "/countries";
            Pais[] paisesArray = restTemplate.getForObject(url, Pais[].class);
            
            if (paisesArray != null) {
                logger.info("Se obtuvieron {} países correctamente", paisesArray.length);
                return Arrays.asList(paisesArray);
            } else {
                logger.warn("La API devolvió null para la lista de países");
                return List.of();
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error API Locacion: {} - Cuerpo: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            logger.error("Error al obtener países desde la API: {}", e.getMessage());
            throw new RuntimeException("Error al obtener países: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtener provincias/estados por código de país
     */
    public List<Provincia> obtenerProvinciasPorPais(String paisCodigo) {
        try {
            logger.debug("Obteniendo provincias para país: {}", paisCodigo);
            String url = API_URL + "/countries/" + paisCodigo + "/states";
            Provincia[] provinciasArray = restTemplate.getForObject(url, Provincia[].class);
            
            if (provinciasArray != null) {
                logger.info("Se obtuvieron {} provincias para país {}", provinciasArray.length, paisCodigo);
                return Arrays.asList(provinciasArray);
            } else {
                logger.warn("La API devolvió null para provincias del país {}", paisCodigo);
                return List.of();
            }
        } catch (Exception e) {
            logger.error("Error al obtener provincias para país {}: {}", paisCodigo, e.getMessage());
            throw new RuntimeException("Error al obtener provincias: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtener ciudades por código de país y provincia
     */
    public List<Ciudad> obtenerCiudadesPorProvincia(String paisCodigo, String provinciaCodigo) {
        try {
            logger.debug("Obteniendo ciudades para país {} y provincia {}", paisCodigo, provinciaCodigo);
            String url = API_URL + "/countries/" + paisCodigo + "/states/" + provinciaCodigo + "/cities";
            Ciudad[] ciudadesArray = restTemplate.getForObject(url, Ciudad[].class);
            
            if (ciudadesArray != null) {
                logger.info("Se obtuvieron {} ciudades para provincia {}", ciudadesArray.length, provinciaCodigo);
                return Arrays.asList(ciudadesArray);
            } else {
                logger.warn("La API devolvió null para ciudades del país {} provincia {}", paisCodigo, provinciaCodigo);
                return List.of();
            }
        } catch (Exception e) {
            logger.error("Error al obtener ciudades para país {} provincia {}: {}", paisCodigo, provinciaCodigo, e.getMessage());
            throw new RuntimeException("Error al obtener ciudades: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método adicional: Obtener información de un país específico
     */
    public Pais obtenerPaisPorCodigo(String paisCodigo) {
        try {
            logger.debug("Obteniendo información del país: {}", paisCodigo);
            String url = API_URL + "/countries/" + paisCodigo;
            return restTemplate.getForObject(url, Pais.class);
        } catch (Exception e) {
            logger.error("Error al obtener país {}: {}", paisCodigo, e.getMessage());
            throw new RuntimeException("Error al obtener país: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método adicional: Obtener información de una provincia específica
     */
    public Provincia obtenerProvinciaPorCodigo(String paisCodigo, String provinciaCodigo) {
        try {
            logger.debug("Obteniendo información de la provincia {} del país {}", provinciaCodigo, paisCodigo);
            String url = API_URL + "/countries/" + paisCodigo + "/states/" + provinciaCodigo;
            return restTemplate.getForObject(url, Provincia.class);
        } catch (Exception e) {
            logger.error("Error al obtener provincia {} del país {}: {}", provinciaCodigo, paisCodigo, e.getMessage());
            throw new RuntimeException("Error al obtener provincia: " + e.getMessage(), e);
        }
    }
}