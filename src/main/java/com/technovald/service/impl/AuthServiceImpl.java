package com.technovald.service.impl;

import com.technovald.dto.request.LoginRequest;
import com.technovald.dto.response.AuthResponse;
import com.technovald.entity.Usuario;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.security.JwtService;
import com.technovald.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Implementación de {@link AuthService}. */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Override
    public AuthResponse login(LoginRequest request) {

        /*
         * Delegar en el AuthenticationManager es lo que hace que la contraseña
         * se compare con BCrypt contra el hash guardado. Si no coincide, lanza
         * BadCredentialsException y la cadena de seguridad responde 401.
         */
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        /*
         * El proveedor de autenticación deja como principal el UserDetails que
         * devolvió CustomUserDetailsService, que es justo lo que necesita
         * JwtService para emitir el token.
         */
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        /*
         * El rol se lee de la entidad, que es la fuente de verdad, para que el
         * valor devuelto coincida con el claim "rol" del token.
         */
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado o inactivo: " + request.getUsername()));

        return AuthResponse.builder()
                .token(jwtService.generateToken(userDetails))
                .username(usuario.getUsername())
                .rol(usuario.getRol().name())
                .expiracion(LocalDateTime.now().plusSeconds(jwtService.getExpirationSeconds()))
                .build();
    }
}