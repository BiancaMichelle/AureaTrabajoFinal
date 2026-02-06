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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.service.UsuarioJpaService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioJpaService usuarioJpaService;
    private final CustomLoginSuccessHandler loginSuccessHandler;
    private final CustomLogoutSuccessHandler logoutSuccessHandler;
    private final CustomAuthenticationFailureHandler failureHandler; // ✅ Inyectado

    @Value("${app.base-url}")
    private String baseUrl;

    public SecurityConfig(UsuarioJpaService usuarioJpaService, 
                         CustomLoginSuccessHandler loginSuccessHandler,
                         CustomLogoutSuccessHandler logoutSuccessHandler,
                         CustomAuthenticationFailureHandler failureHandler) {
        this.usuarioJpaService = usuarioJpaService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.failureHandler = failureHandler;
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
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/**",
                                "/ia/**",
                                "/alumno/ia/**",
                                "/clase/docente-entrar/**",
                                "/clase/docente/salir/**",
                                "/pago/webhook",
                                "/pool/**",
                                "/actividad/**",
                                "/aula/api/asistencia/registrar"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .userDetailsService(usuarioJpaService)
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login?timeout"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/publico/**", "/login", "/register", "/register/**", "/terminos",
                                "/provincias/**", "/ciudades/**", "/api/ubicaciones/**",
                                "/email/**", "/css/**", "/js/**", "/style/**", "/img/**",
                                "/api/**", "/admin/configuracion/carrusel/**",
                                "/crear-admin-temporal", "/forgot-password", "/recuperacion/**",
                                "/api/usuarios/**", "/admin/debug-user", "/api/categorias/**",
                                "/pago/**", "/api/**", "/pago-resultado",
                                "/charla/videoconferencia/**")
                        .permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/alumno/notificar-inscripcion/**").authenticated() // Permitir a cualquier usuario autenticado
                        .requestMatchers("/alumno/**", "/inscribirse/**").hasAnyAuthority("ALUMNO", "DOCENTE")
                        .requestMatchers("/docente/**").hasAuthority("DOCENTE")
                        .requestMatchers("/ia/chat/**", "/ia/status", "/ia/health").permitAll()
                        .requestMatchers("/ia/**").hasAnyAuthority("ALUMNO", "DOCENTE", "ADMIN")
                        .requestMatchers("/modulo/**", "/crearModulo", "/ofertaAcademica/**")
                        .hasAnyAuthority("DOCENTE", "ADMIN")
                        .requestMatchers("/pool/**")
                        .hasAnyAuthority("DOCENTE", "ADMIN")
                        // Permitir a alumnos ver materiales y descargar archivos
                        .requestMatchers("/actividad/material/*/archivos", "/archivo/*/descargar")
                        .hasAnyAuthority("ALUMNO", "DOCENTE", "ADMIN")
                        // Resto de endpoints de actividad solo para docentes/admin
                        .requestMatchers("/actividad/**")
                        .hasAnyAuthority("DOCENTE", "ADMIN")
                        .requestMatchers("/examen/**").hasAnyAuthority("ALUMNO", "DOCENTE", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("dni") // ✅ Verificación por DNI
                        .passwordParameter("password")
                        .failureHandler(failureHandler) // ✅ Manejador personalizado
                        .successHandler(loginSuccessHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .permitAll());
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

    @Bean("locacionRestTemplate")
    public RestTemplate locacionRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-CSCAPI-KEY", apiKey);
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}
