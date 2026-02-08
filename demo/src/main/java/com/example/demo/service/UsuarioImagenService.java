package com.example.demo.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.UsuarioImagen;
import com.example.demo.repository.UsuarioImagenRepository;

@Service
@Transactional
public class UsuarioImagenService {

    @Autowired
    private UsuarioImagenRepository usuarioImagenRepository;

    public UsuarioImagen guardarImagenUsuario(MultipartFile archivo) throws IOException {
        UsuarioImagen imagen = new UsuarioImagen(
            archivo.getOriginalFilename(),
            archivo.getContentType(),
            archivo.getBytes(),
            archivo.getSize()
        );
        return usuarioImagenRepository.save(imagen);
    }

    public Optional<UsuarioImagen> obtenerImagenUsuario(Long id) {
        return usuarioImagenRepository.findById(id);
    }

    public void eliminarImagenUsuario(Long id) {
        usuarioImagenRepository.deleteById(id);
    }
}
