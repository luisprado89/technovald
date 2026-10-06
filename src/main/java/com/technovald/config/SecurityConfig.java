package com.technovald.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad de la API.
 *
 * <p>En esta fase la autenticación es HTTP Basic contra la tabla
 * {@code usuario}. Cuando llegue el login con token, la cadena de filtros
 * cambiará Basic por un filtro que lea el JWT; las reglas por rol se irán
 * añadiendo a medida que existan endpoints que las necesiten.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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

                /*
                 * Autenticación HTTP Basic: el usuario y la contraseña viajan
                 * en la cabecera Authorization de cada petición y se comprueban
                 * contra la tabla usuario.
                 */
                .httpBasic(Customizer.withDefaults())

                /*
                 * Sin sesión: cada petición se autentica por sí misma, así que
                 * no se crea ni se guarda ningún HttpSession.
                 */
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /*
                 * Todas las rutas exigen autenticación: todavía no hay ningún
                 * endpoint público.
                 */
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())

                .authenticationProvider(authenticationProvider);

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
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}