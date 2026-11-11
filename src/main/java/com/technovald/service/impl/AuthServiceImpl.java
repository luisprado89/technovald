package com.technovald.service.impl;

import com.technovald.dto.request.ChangePasswordRequest;
import com.technovald.dto.request.ForgotPasswordRequest;
import com.technovald.dto.request.LoginRequest;
import com.technovald.dto.response.AuthResponse;
import com.technovald.entity.Persona;
import com.technovald.entity.Usuario;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.security.CredentialGenerator;
import com.technovald.security.JwtService;
import com.technovald.service.AuthService;
import com.technovald.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Implementación de {@link AuthService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CredentialGenerator credentialGenerator;
    private final EmailService emailService;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {

        /*
         * Delegar en el AuthenticationManager es lo que hace que la contraseña
         * se compare con BCrypt contra el hash guardado. Si no coincide, lanza
         * BadCredentialsException y la cadena de seguridad responde 401.
         */
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

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
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(loginRequest.getUsername())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado o inactivo: " + loginRequest.getUsername()));

        return AuthResponse.builder().token(jwtService.generateToken(userDetails))
                .username(usuario.getUsername()).rol(usuario.getRol().name())
                .expiracion(LocalDateTime.now().plusSeconds(jwtService.getExpirationSeconds())).build();
    }

    /**
     * Si el email pertenece a una cuenta activa, sustituye su contraseña por
     * una temporal y la envía. La respuesta es la misma exista o no la cuenta.
     */
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        Persona persona = personaRepository.findByEmail(email).orElse(null);
        Usuario usuario = persona == null ? null : persona.getUsuario();

        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            log.info("Recuperación solicitada para un email sin cuenta activa: {}", email);
            return;
        }

        String passwordTemporal = credentialGenerator.generatePassword();
        usuario.setPassword(passwordEncoder.encode(passwordTemporal));
        // Cierra cualquier sesión abierta del usuario.
        usuario.invalidateTokens();
        usuarioRepository.save(usuario);

        emailService.sendPasswordResetEmail(email, usuario.getUsername(), persona.getNombre(), passwordTemporal);
        log.info("Contraseña temporal generada para el usuario '{}'", usuario.getUsername());
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     * Verifica la actual, aplica la nueva e invalida los tokens previos.
     *
     * @param username usuario autenticado extraído del JWT.
     * @param changePasswordRequest  DTO con la contraseña actual y la nueva.
     */
    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest changePasswordRequest) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado o inactivo: " + username));

        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), usuario.getPassword())) {
            throw new BusinessRuleException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), usuario.getPassword())) {
            throw new BusinessRuleException("La nueva contraseña debe ser distinta de la actual");
        }

        usuario.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        // Los tokens emitidos antes dejan de valer: habrá que volver a entrar.
        usuario.invalidateTokens();
        usuarioRepository.save(usuario);

        log.info("Contraseña cambiada por el usuario '{}'", username);
    }
}