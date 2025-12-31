package com.example.demo.service;

import com.example.demo.model.CarruselImagen;
import com.example.demo.model.Instituto;
import com.example.demo.repository.CarruselImagenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ImagenService {

    @Autowired
    private CarruselImagenRepository carruselImagenRepository;

    public CarruselImagen guardarImagenCarrusel(MultipartFile archivo, Instituto instituto, Integer orden) throws IOException {
        CarruselImagen imagen = new CarruselImagen(
            archivo.getOriginalFilename(),
            archivo.getContentType(),
            archivo.getBytes(),
            archivo.getSize(),
            instituto,
            orden
        );
        CarruselImagen guardada = carruselImagenRepository.save(imagen);
        
        // Actualizar la ruta con el ID real
        guardada.setRutaArchivo("/api/carrusel/imagen/" + guardada.getId());
        return carruselImagenRepository.save(guardada);
    }

    public List<CarruselImagen> guardarMultiplesImagenesCarrusel(MultipartFile[] archivos, Instituto instituto) throws IOException {
        // Obtener el próximo orden
        CarruselImagen ultimaImagen = carruselImagenRepository.findTopByInstitutoOrderByOrdenDesc(instituto);
        int proximoOrden = ultimaImagen != null ? ultimaImagen.getOrden() + 1 : 1;
        
        for (int i = 0; i < archivos.length; i++) {
            MultipartFile archivo = archivos[i];
            if (!archivo.isEmpty()) {
                guardarImagenCarrusel(archivo, instituto, proximoOrden + i);
            }
        }
        return obtenerImagenesCarruselPorInstituto(instituto);
    }

    public Optional<CarruselImagen> obtenerImagenCarrusel(Long id) {
        return carruselImagenRepository.findById(id);
    }

    public List<CarruselImagen> obtenerImagenesCarruselPorInstituto(Instituto instituto) {
        return carruselImagenRepository.findByInstitutoAndActivaTrueOrderByOrden(instituto);
    }

    public List<CarruselImagen> obtenerTodasLasImagenesCarrusel(Instituto instituto) {
        return carruselImagenRepository.findByInstitutoOrderByOrden(instituto);
    }

    public void eliminarImagenCarrusel(Long id) {
        carruselImagenRepository.deleteById(id);
    }

    public CarruselImagen actualizarOrdenImagen(Long id, Integer nuevoOrden) {
        Optional<CarruselImagen> imagenOpt = carruselImagenRepository.findById(id);
        if (imagenOpt.isPresent()) {
            CarruselImagen imagen = imagenOpt.get();
            imagen.setOrden(nuevoOrden);
            return carruselImagenRepository.save(imagen);
        }
        return null;
    }

    public CarruselImagen actualizarAltText(Long id, String altText) {
        Optional<CarruselImagen> imagenOpt = carruselImagenRepository.findById(id);
        if (imagenOpt.isPresent()) {
            CarruselImagen imagen = imagenOpt.get();
            imagen.setAltText(altText);
            return carruselImagenRepository.save(imagen);
        }
        return null;
    }

    public void desactivarImagen(Long id) {
        Optional<CarruselImagen> imagenOpt = carruselImagenRepository.findById(id);
        if (imagenOpt.isPresent()) {
            CarruselImagen imagen = imagenOpt.get();
            imagen.setActiva(false);
            carruselImagenRepository.save(imagen);
        }
    }

    public void activarImagen(Long id) {
        Optional<CarruselImagen> imagenOpt = carruselImagenRepository.findById(id);
        if (imagenOpt.isPresent()) {
            CarruselImagen imagen = imagenOpt.get();
            imagen.setActiva(true);
            carruselImagenRepository.save(imagen);
        }
    }
}
