package com.technovald.security;


import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Deja autenticada la petición cuando trae un token válido.
 *
 * <p>Si no hay cabecera {@code Authorization}, o el token no sirve, la petición
 * continúa sin autenticar: la decisión de responder 401 la toma la cadena de
 * seguridad, no este filtro.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            /*
             * extractUsername verifica la firma: si el token está caducado o
             * manipulado lanza JwtException aquí, antes de tocar la base de
             * datos.
             */
            String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                /*
                 * El usuario se vuelve a cargar de la base de datos: así un
                 * usuario dado de baja deja de entrar aunque su token siga
                 * siendo válido.
                 */
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                /*
                 * isTokenValid no lanza: devuelve false si el token ha caducado,
                 * es de otro usuario o su versión de credenciales ya no es la
                 * vigente.
                 */
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException | UsernameNotFoundException ex) {
            /*
             * Token ilegible o caducado, o usuario que ya no existe o está
             * inactivo. En los dos casos la petición sigue sin autenticar y la
             * cadena de seguridad responde 401.
             */
            log.debug("Petición sin autenticar: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}