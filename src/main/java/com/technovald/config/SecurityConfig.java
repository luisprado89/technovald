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
 * <p>Las rutas de administración exigen roles específicos: los empleados son
 * exclusivos de {@code ADMIN}, mientras que clientes, categorías, proveedores e
 * inventario están abiertos a {@code ADMIN} y {@code TRABAJADOR}. El catálogo
 * público es accesible sin token.</p>
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
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

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
     * @param http                    configuración de seguridad HTTP.
     * @param authenticationProvider  proveedor que comprueba las credenciales contra la tabla {@code usuario}.
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
                        // Rutas públicas (login, recuperación, registro)
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/clientes/register").permitAll()

                        // Catálogo público: se consulta sin token
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/**").permitAll()

                        // Frontend estático
                        .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/img/**", "/favicon.ico")
                        .permitAll()

                        // Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Punto de entrada interno de errores de Spring
                        .requestMatchers("/error").permitAll()

                        // Gestión de empleados: solo ADMIN
                        .requestMatchers("/api/admin/empleados/**").hasRole("ADMIN")

                        // Gestión de clientes: ADMIN y TRABAJADOR
                        .requestMatchers("/api/admin/clientes/**").hasAnyRole("ADMIN", "TRABAJADOR")

                        // Categorías: lectura para el staff, escritura solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/admin/categorias/**")
                        .hasAnyRole("ADMIN", "TRABAJADOR")
                        .requestMatchers("/api/admin/categorias/**").hasRole("ADMIN")

                        // Proveedores: lectura para el staff, escritura solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/admin/proveedores/**")
                        .hasAnyRole("ADMIN", "TRABAJADOR")
                        .requestMatchers("/api/admin/proveedores/**").hasRole("ADMIN")

                        // Inventario: lectura y ajuste de stock para el staff; el resto solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/admin/inventario/**")
                        .hasAnyRole("ADMIN", "TRABAJADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/inventario/*/stock")
                        .hasAnyRole("ADMIN", "TRABAJADOR")
                        .requestMatchers("/api/admin/inventario/**").hasRole("ADMIN")

                        // Gestión del catálogo (fichas técnicas): solo ADMIN
                        .requestMatchers("/api/admin/catalogo/**").hasRole("ADMIN")

                        // Pedidos: el cliente compra y consulta los suyos; el staff gestiona
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/me").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos")
                        .hasAnyRole("ADMIN", "TRABAJADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/estado")
                        .hasAnyRole("ADMIN", "TRABAJADOR")

                        // Comentarios: lectura pública; publica el cliente; responde el staff
                        .requestMatchers(HttpMethod.GET, "/api/comentarios/producto/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comentarios").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/comentarios/pendientes")
                        .hasAnyRole("ADMIN", "TRABAJADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/comentarios/*/responder")
                        .hasAnyRole("ADMIN", "TRABAJADOR")

                        // Solicitudes de reposición: solo ADMIN y TRABAJADOR
                        .requestMatchers("/api/solicitudes", "/api/solicitudes/**")
                        .hasAnyRole("ADMIN", "TRABAJADOR")

                        // Cambio de contraseña de un usuario autenticado
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()

                        // El resto requiere estar autenticado
                        .anyRequest().authenticated()
                )

                /*
                 * Sin credenciales válidas la respuesta es 401.
                 */
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))

                        /*
                         * Autenticado pero sin el rol exigido: 403. Se deja sin cuerpo.
                         */
                        .accessDeniedHandler((
                                request,
                                response,
                                accessDeniedException) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())))

                .authenticationProvider(authenticationProvider)

                /*
                 * El filtro del token va antes del filtro de usuario y contraseña.
                 */
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuración CORS con orígenes fijos.
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
     * Codificador de contraseñas (BCrypt).
     *
     * @return el codificador que se usa para guardar y comprobar contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Proveedor de autenticación.
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