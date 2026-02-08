package com.example.demo.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.OfertaImagen;
import com.example.demo.repository.OfertaImagenRepository;

@Service
@Transactional
public class OfertaImagenService {

    @Autowired
    private OfertaImagenRepository ofertaImagenRepository;

    public OfertaImagen guardarImagenOferta(MultipartFile archivo) throws IOException {
        OfertaImagen imagen = new OfertaImagen(
            archivo.getOriginalFilename(),
            archivo.getContentType(),
            archivo.getBytes(),
            archivo.getSize()
        );
        return ofertaImagenRepository.save(imagen);
    }

    public Optional<OfertaImagen> obtenerImagenOferta(Long id) {
        return ofertaImagenRepository.findById(id);
    }
}
