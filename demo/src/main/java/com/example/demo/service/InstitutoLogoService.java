package com.example.demo.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Instituto;
import com.example.demo.model.InstitutoLogo;
import com.example.demo.repository.InstitutoLogoRepository;

@Service
public class InstitutoLogoService {

    @Autowired
    private InstitutoLogoRepository institutoLogoRepository;

    public InstitutoLogo guardarLogo(MultipartFile archivo, Instituto instituto) throws IOException {
        Optional<InstitutoLogo> existente = institutoLogoRepository.findByInstituto(instituto);
        InstitutoLogo logo = existente.orElseGet(InstitutoLogo::new);
        logo.setInstituto(instituto);
        logo.setNombreArchivo(archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "logo");
        logo.setTipoMime(archivo.getContentType() != null ? archivo.getContentType() : "image/png");
        logo.setDatos(archivo.getBytes());
        logo.setTamanio(archivo.getSize());
        return institutoLogoRepository.save(logo);
    }

    public Optional<InstitutoLogo> obtenerPorId(Long id) {
        return institutoLogoRepository.findById(id);
    }

    public Optional<InstitutoLogo> obtenerPorInstituto(Instituto instituto) {
        return institutoLogoRepository.findByInstituto(instituto);
    }

    public void eliminar(Long id) {
        institutoLogoRepository.deleteById(id);
    }
}
