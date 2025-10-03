package com.example.demo.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

/*Este servicio es utilizado cuando un usuario intenta acceder, busca al usuario en la base de datos
 * luego empaca todo para traerle a spring. Compara la contraseña que se tecleo con la que se guardo.
 * comprueba y usa los roles para saber a donde llevar el usuario.
 */

@Service
public class UsuarioJpaService implements UserDetailsService {
    
    private UsuarioRepository usuarioRepository;

    public UsuarioJpaService (UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String dni)
        throws UsernameNotFoundException {
        
        Usuario user = usuarioRepository.findByDni(dni)
            .orElseThrow(() -> new UsernameNotFoundException("No existe " + dni));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getDni())
            .password(user.getContraseña())
            .authorities(user.getRoles()
                .stream()
                .map(Rol::getNombre)
                .toArray(String[]::new))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .build();
    }

    public boolean existePorDni(String dni) {
        return usuarioRepository.findByDni(dni).isPresent();
    }

    public boolean existePorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).isPresent();
    }

    public Optional<Usuario> buscarPorDni(String dni) {
        return usuarioRepository.findByDni(dni);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }
    
}
