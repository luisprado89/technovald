package com.technovald.service.impl;

import com.technovald.dto.request.ChangePasswordRequest;
import com.technovald.dto.request.ForgotPasswordRequest;
import com.technovald.dto.request.LoginRequest;
import com.technovald.dto.response.AuthResponse;
import com.technovald.entity.Persona;
import com.technovald.entity.Usuario;
import com.technovald.enums.RolUsuario;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.security.CredentialGenerator;
import com.technovald.security.JwtService;
import com.technovald.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CredentialGenerator credentialGenerator;
    @Mock private EmailService emailService;

    @InjectMocks private AuthServiceImpl authService;

    private Usuario usuario;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .username("ana")
                .password("oldHash")
                .rol(RolUsuario.ADMIN)
                .tokenVersion(0)
                .build();
        userDetails = User.withUsername("ana").password("oldHash").roles("ADMIN").build();
    }

    @Test
    void loginDevuelveTokenUsernameRolYExpiracion() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(
                LoginRequest.builder().username("ana").password("old").build());

        assertEquals("jwt-token", response.getToken());
        assertEquals("ana", response.getUsername());
        assertEquals("ADMIN", response.getRol());
        assertNotNull(response.getExpiracion());
    }

    @Test
    void loginPropagaCredencialesInvalidas() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(LoginRequest.builder().username("ana").password("bad").build()));

        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @Test
    void forgotPasswordNormalizaEmailYInvalidaTokens() {
        Persona persona = Persona.builder().id(1L).nombre("Ana").email("ana@test.local")
                .usuario(usuario).build();
        when(personaRepository.findByEmail("ana@test.local")).thenReturn(Optional.of(persona));
        when(credentialGenerator.generatePassword()).thenReturn("Tmp1234!");
        when(passwordEncoder.encode("Tmp1234!")).thenReturn("encodedTmp");

        authService.forgotPassword(ForgotPasswordRequest.builder().email(" Ana@Test.Local ").build());

        assertEquals("encodedTmp", usuario.getPassword());
        assertEquals(1, usuario.getTokenVersion());
        verify(usuarioRepository).save(usuario);
        verify(emailService).sendPasswordResetEmail("ana@test.local", "ana", "Ana", "Tmp1234!");
    }

    @Test
    void forgotPasswordNoFiltraSiElEmailNoExiste() {
        when(personaRepository.findByEmail("nadie@test.local")).thenReturn(Optional.empty());

        authService.forgotPassword(ForgotPasswordRequest.builder().email("nadie@test.local").build());

        verifyNoInteractions(usuarioRepository, emailService, credentialGenerator);
    }

    @Test
    void forgotPasswordIgnoraUnaCuentaInactiva() {
        usuario.setActivo(false);
        Persona persona = Persona.builder().id(1L).nombre("Ana").email("ana@test.local")
                .usuario(usuario).build();
        when(personaRepository.findByEmail("ana@test.local")).thenReturn(Optional.of(persona));

        authService.forgotPassword(ForgotPasswordRequest.builder().email("ana@test.local").build());

        verifyNoInteractions(emailService, credentialGenerator);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void changePasswordActualizaHashEInvalidaTokens() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("old", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("new", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("new")).thenReturn("newHash");

        authService.changePassword("ana",
                ChangePasswordRequest.builder().currentPassword("old").newPassword("new").build());

        assertEquals("newHash", usuario.getPassword());
        assertEquals(1, usuario.getTokenVersion());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void changePasswordRechazaPasswordActualIncorrecta() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", "oldHash")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.changePassword("ana",
                        ChangePasswordRequest.builder().currentPassword("wrong").newPassword("new").build()));

        assertTrue(ex.getMessage().contains("contraseña actual"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void changePasswordRechazaRepetirLaMismaPassword() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("old", "oldHash")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.changePassword("ana",
                        ChangePasswordRequest.builder().currentPassword("old").newPassword("old").build()));

        assertTrue(ex.getMessage().contains("distinta"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void changePasswordRechazaUsuarioInexistente() {
        when(usuarioRepository.findByUsernameAndActivoTrue("fantasma")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.changePassword("fantasma",
                        ChangePasswordRequest.builder().currentPassword("old").newPassword("new").build()));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
