package com.technovald.config;

import com.technovald.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad de la API.
 *
 * <p>Autenticación por JWT: el login reparte el token y el
 * {@link JwtAuthenticationFilter} autentica cada petición que lo traiga en la
 * cabecera {@code Authorization}. Sin credenciales válidas la respuesta es
 * 401; con credenciales pero sin el rol exigido, 403.</p>
 *
 * <p>La gestión de empleados ({@code /api/admin/empleados/**}) es la primera
 * ruta con regla por rol ({@code ADMIN}); el resto de rutas siguen exigiendo
 * solo autenticación. Las reglas por rol se irán añadiendo aquí a medida que
 * existan nuevos endpoints de administración.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Orígenes del frontend local. Si cambian, se tocan aquí.
    private static final List<String> ALLOWED_ORIGINS = List.of(
            // --- Puertos por defecto de frameworks JS ---
            "http://localhost:3000",    // Create React App, Next.js, Express
            "http://localhost:5173",    // Vite (React, Vue, Svelte)
            "http://localhost:4200",    // Angular
            "http://localhost:4321",    // Astro

            // --- Herramientas de desarrollo estático ---
            "http://localhost:5500",    // VS Code Live Server (HTML/JS Vanilla)
            "http://127.0.0.1:5500",    // Live Server (a veces usa la IP directa)

            // --- Puertos alternativos útiles ---
            "http://localhost:8081",    // Frontend alternativo / Microservicio
            "http://localhost:4000"     // Servidor Node.js / GraphQL
    );
    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Construye la configuración con el filtro de autenticación por token.
     *
     * @param jwtAuthenticationFilter filtro que valida el token en cada petición.
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Cadena de filtros de seguridad.
     *
     * @param http configuración de seguridad HTTP.
     * @param authenticationProvider proveedor que comprueba las credenciales contra la tabla {@code usuario}.
     * @param corsConfigurationSource fuente de configuración para CORS.
     * @return la cadena de filtros configurada.
     * @throws Exception si la configuración no se puede construir.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationProvider authenticationProvider,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                /*
                 * API sin formularios HTML: no hay sesión de navegador que
                 * proteger contra CSRF.
                 */
                .csrf(csrf -> csrf.disable())

                // Configuración de CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        /*
                         * Rutas públicas: el login, que reparte los tokens, y el
                         * registro de clientes, que da de alta a alguien que
                         * todavía no tiene credenciales con las que autenticarse.
                         */
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/clientes/register").permitAll()

                        /*
                         * Swagger se abre sin token; el resto de rutas, no.
                         */
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        /*
                         * /error es el punto de entrada interno de Spring Boot para
                         * cualquier error (404, 500...). Es un forward que hace el propio
                         * framework, así que no tiene sentido exigir autenticación ahí:
                         * si fuera protegida, un 404 legítimo se convertiría en un 401
                         * engañoso.
                         */
                        .requestMatchers("/error").permitAll()

                        /*
                         * Gestión de empleados: es la primera regla por rol de la API.
                         * Un usuario autenticado sin rol ADMIN recibe 403; sin token,
                         * 401. La regla va por ruta, en un único sitio, para que no
                         * pueda existir un endpoint de administración sin proteger.
                         */
                        .requestMatchers("/api/admin/empleados/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                /*
                 * Sin credenciales válidas la respuesta es 401. Sin un punto de
                 * entrada propio, Spring Security devolvería 403, que describe
                 * un problema de permisos y no de identidad.
                 */
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))

                        /*
                         * Autenticado pero sin el rol exigido: 403. Se deja sin
                         * cuerpo por el mismo motivo que el 401: la decisión la
                         * toma la cadena de filtros, antes de que la petición
                         * llegue al dispatcher.
                         */
                        .accessDeniedHandler((
                                request,
                                response,
                                accessDeniedException) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())))

                .authenticationProvider(authenticationProvider)

                /*
                 * El filtro del token va antes del filtro de usuario y
                 * contraseña: cuando la petición llegue ahí, o ya está
                 * autenticada por token, o no hay nada que autenticar.
                 */
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuración CORS con orígenes fijos (localhost:3000 y localhost:5173).
     * La API autentica con Bearer, por lo que no se permiten cookies.
     *
     * @return fuente de configuración CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Codificador de contraseñas. BCrypt aplica un hash con sal aleatoria, así
     * que la misma contraseña produce un hash distinto cada vez.
     *
     * @return el codificador que se usa para guardar y comprobar contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Proveedor de autenticación: combina el
     * {@link com.technovald.security.CustomUserDetailsService} con el
     * codificador para comparar la contraseña recibida con el hash almacenado.
     *
     * @param userDetailsService servicio que carga el usuario desde MySQL.
     * @param passwordEncoder    codificador de contraseñas.
     * @return el proveedor de autenticación.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Expone el gestor de autenticación que usa el inicio de sesión.
     *
     * @param configuration configuración de autenticación de Spring Security.
     * @return el gestor de autenticación.
     * @throws Exception si no se puede construir.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}