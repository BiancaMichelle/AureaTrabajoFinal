package com.example.demo.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.CustomUsuarioDetails;
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
        
        // Usar la consulta que carga los roles para evitar lazy loading issues
        Usuario user = usuarioRepository.findByDniWithRoles(dni)
            .orElseThrow(() -> new UsernameNotFoundException("No existe " + dni));

        System.out.println("🔐 Cargando usuario: " + user.getNombre() + " " + user.getApellido());
        System.out.println("🎭 Roles del usuario: " + user.getRoles().size());
        user.getRoles().forEach(rol -> System.out.println("  - " + rol.getNombre()));

        // Devolver CustomUsuarioDetails en lugar del UserDetails estándar
        return new CustomUsuarioDetails(user);
    }

    public boolean existePorDni(String dni) {
        return usuarioRepository.findByDni(dni).isPresent();
    }

    public boolean existePorDniYPais(String dni, String paisCodigo) {
        if (paisCodigo == null || paisCodigo.isBlank()) {
            return false;
        }
        return usuarioRepository.existsByDniAndPais_Codigo(dni, paisCodigo);
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
