package com.technovald.config;


import com.technovald.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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


/**
 * Configuración de seguridad de la API.
 *
 * <p>Autenticación por JWT: el login reparte el token y el
 * {@link JwtAuthenticationFilter} autentica cada petición que lo traiga en la
 * cabecera {@code Authorization}. Sin credenciales válidas la respuesta es
 * 401. Las reglas por rol se irán añadiendo a medida que existan endpoints
 * que las necesiten.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
     * @param http                   configuración de seguridad HTTP.
     * @param authenticationProvider proveedor que comprueba las credenciales
     *                               contra la tabla {@code usuario}.
     * @return la cadena de filtros configurada.
     * @throws Exception si la configuración no se puede construir.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationProvider authenticationProvider) throws Exception {
        http
                /*
                 * API sin formularios HTML: no hay sesión de navegador que
                 * proteger contra CSRF.
                 */
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // El login es la única ruta pública: es la que reparte los tokens.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        /*
                         * /error es el punto de entrada interno de Spring Boot para
                         * cualquier error (404, 500...). Es un forward que hace el propio
                         * framework, así que no tiene sentido exigir autenticación ahí:
                         * si fuera protegida, un 404 legítimo se convertiría en un 401
                         * engañoso.
                         */
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )

                /*
                 * Sin credenciales válidas la respuesta es 401. Sin un punto de
                 * entrada propio, Spring Security devolvería 403, que describe
                 * un problema de permisos y no de identidad.
                 */
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

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