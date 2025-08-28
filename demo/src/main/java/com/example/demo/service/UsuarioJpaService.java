package com.example.demo.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.Role;
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
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {
        
        Usuario user = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("No existe " + username));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles()
                .stream()
                .map(Role::getName)
                .toArray(String[]::new))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(!user.isEnabled())
            .build();
    }
}
