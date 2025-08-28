package com.example.demo.configSecurity;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.service.UsuarioJpaService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioJpaService usuarioJpaService;

    public SecurityConfig(UsuarioJpaService usuarioJpaService) {
        this.usuarioJpaService = usuarioJpaService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring inyecta el AuthenticationConfiguration y te devuelve el manager
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        .userDetailsService(usuarioJpaService)
        .sessionManagement(session -> session
              .invalidSessionUrl("/login?timeout")
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/publico", "/login", "/register", "/css/**", "/js/**")
                .permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/alumno/**").hasRole("ESTUDIANTE")
            .requestMatchers("/docente/**").hasRole("DOCENTE")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .successHandler((request, response, authentication) -> {
                var roles = authentication.getAuthorities()
                                        .stream()
                                        .map(a -> a.getAuthority())
                                        .toList();
                if (roles.contains("ROLE_ADMIN")) {
                    response.sendRedirect("/admin/dashboard");
                } else if (roles.contains("ROLE_ESTUDIANTE")) {
                    response.sendRedirect("/alumno");
                } else if (roles.contains("ROLE_DOCENTE")) {
                    response.sendRedirect("/docente");
                } else {
                    response.sendRedirect("/");  
                }
            })
            .permitAll()
        )
        .logout(logout -> logout.permitAll());
        return http.build();
    }
}
