package com.example.demo.configSecurity;


import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ← AÑADE ESTO
        .userDetailsService(usuarioJpaService)
        .sessionManagement(session -> session
              .invalidSessionUrl("/?timeout")
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/publico", "/login", "/register", 
                           "/css/**", "/js/**", "/style/**", "/img/**")
            .permitAll()
            .requestMatchers("/admin/**").hasAuthority("ADMIN")
            .requestMatchers("/alumno/**").hasAuthority("ALUMNO")
            .requestMatchers("/docente/**").hasAuthority("DOCENTE")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/") 
            .loginProcessingUrl("/perform_login")
            .failureUrl("/?error=true")
            .successHandler((request, response, authentication) -> {
                var roles = authentication.getAuthorities()
                                        .stream()
                                        .map(a -> a.getAuthority())
                                        .toList();
                if (roles.contains("ADMIN")) {
                    response.sendRedirect("/admin/dashboard");
                } else if (roles.contains("ALUMNO")) {
                    response.sendRedirect("/alumno");
                } else if (roles.contains("DOCENTE")) {
                    response.sendRedirect("/docente");
                } else {
                    response.sendRedirect("/");  
                }
            })
            .permitAll()
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/?logout=true")
            .permitAll()
        );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Permite todos los orígenes
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Value("${csc.api.key}")
    private String apiKey;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-CSCAPI-KEY", apiKey);
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}
