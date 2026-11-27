package com.technovald.service.impl;

import com.technovald.entity.AuthCode;
import com.technovald.entity.Usuario;
import com.technovald.enums.PropositoCodigo;
import com.technovald.exception.InvalidCodeException;
import com.technovald.repository.mysql.AuthCodeRepository;
import com.technovald.security.AuthCodeAttemptService;
import com.technovald.security.VerificationCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCodeServiceImplTest {

    @Mock private AuthCodeRepository authCodeRepository;
    @Mock private VerificationCodeGenerator verificationCodeGenerator;
    @Mock private AuthCodeAttemptService authCodeAttemptService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthCodeServiceImpl authCodeService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        authCodeService = new AuthCodeServiceImpl(
                authCodeRepository, verificationCodeGenerator, authCodeAttemptService, passwordEncoder);
        usuario = Usuario.builder().id(7L).username("ana").build();
    }

    @Test
    void emitirInvalidaLosAnterioresYGuardaElHash() {
        when(verificationCodeGenerator.generarCodigo()).thenReturn("012345");

        String codigo = authCodeService.emitir(usuario, PropositoCodigo.ACTIVACION_CUENTA);

        assertEquals("012345", codigo);
        verify(authCodeRepository).invalidarVigentes(eq(7L), eq(PropositoCodigo.ACTIVACION_CUENTA), any());

        ArgumentCaptor<AuthCode> captor = ArgumentCaptor.forClass(AuthCode.class);
        verify(authCodeRepository).save(captor.capture());
        AuthCode guardado = captor.getValue();

        assertEquals(7L, guardado.getUsuarioId());
        assertEquals(PropositoCodigo.ACTIVACION_CUENTA, guardado.getPurpose());
        assertNotEquals("012345", guardado.getCodeHash());
        assertTrue(passwordEncoder.matches("012345", guardado.getCodeHash()));
        assertEquals(0, guardado.getAttempts());
        assertEquals(5, guardado.getMaxAttempts());
        assertTrue(guardado.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(9)));
    }

    @Test
    void consumeElCodigoCorrectoUnaSolaVez() {
        when(authCodeRepository.findFirstByUsuarioIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(7L), eq(PropositoCodigo.RESET_PASSWORD)))
                .thenReturn(Optional.of(fila("123456", LocalDateTime.now().plusMinutes(5), 0)));
        when(authCodeRepository.consumir(eq(1L), any())).thenReturn(1);

        assertDoesNotThrow(() ->
                authCodeService.verificarYConsumir(7L, PropositoCodigo.RESET_PASSWORD, "123456"));

        verify(authCodeRepository).consumir(eq(1L), any());
        verifyNoInteractions(authCodeAttemptService);
    }

    @Test
    void rechazaUnCodigoYaConsumido() {
        when(authCodeRepository.findFirstByUsuarioIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(7L), eq(PropositoCodigo.RESET_PASSWORD)))
                .thenReturn(Optional.of(fila("123456", LocalDateTime.now().plusMinutes(5), 0)));
        when(authCodeRepository.consumir(eq(1L), any())).thenReturn(0);

        assertThrows(InvalidCodeException.class, () ->
                authCodeService.verificarYConsumir(7L, PropositoCodigo.RESET_PASSWORD, "123456"));
    }

    @Test
    void rechazaUnCodigoCaducado() {
        when(authCodeRepository.findFirstByUsuarioIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(7L), eq(PropositoCodigo.RESET_PASSWORD)))
                .thenReturn(Optional.of(fila("123456", LocalDateTime.now().minusMinutes(1), 0)));

        assertThrows(InvalidCodeException.class, () ->
                authCodeService.verificarYConsumir(7L, PropositoCodigo.RESET_PASSWORD, "123456"));

        verify(authCodeRepository, never()).consumir(any(), any());
        verifyNoInteractions(authCodeAttemptService);
    }

    @Test
    void rechazaUnCodigoAgotado() {
        when(authCodeRepository.findFirstByUsuarioIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(7L), eq(PropositoCodigo.RESET_PASSWORD)))
                .thenReturn(Optional.of(fila("123456", LocalDateTime.now().plusMinutes(5), 5)));

        assertThrows(InvalidCodeException.class, () ->
                authCodeService.verificarYConsumir(7L, PropositoCodigo.RESET_PASSWORD, "123456"));

        verify(authCodeRepository, never()).consumir(any(), any());
        verifyNoInteractions(authCodeAttemptService);
    }

    @Test
    void registraElIntentoFallidoAntesDeRechazar() {
        when(authCodeRepository.findFirstByUsuarioIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(7L), eq(PropositoCodigo.RESET_PASSWORD)))
                .thenReturn(Optional.of(fila("123456", LocalDateTime.now().plusMinutes(5), 2)));

        assertThrows(InvalidCodeException.class, () ->
                authCodeService.verificarYConsumir(7L, PropositoCodigo.RESET_PASSWORD, "000000"));

        verify(authCodeAttemptService).registrarIntentoFallido(1L);
        verify(authCodeRepository, never()).consumir(any(), any());
    }

    @Test
    void rechazaSiNoHayCodigoVigente() {
        when(authCodeRepository.findFirstByUsuarioIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(7L), eq(PropositoCodigo.RESET_PASSWORD)))
                .thenReturn(Optional.empty());

        assertThrows(InvalidCodeException.class, () ->
                authCodeService.verificarYConsumir(7L, PropositoCodigo.RESET_PASSWORD, "123456"));

        verifyNoInteractions(authCodeAttemptService);
    }

    private AuthCode fila(String codigo, LocalDateTime expira, int intentos) {
        return AuthCode.builder()
                .id(1L)
                .usuarioId(7L)
                .purpose(PropositoCodigo.RESET_PASSWORD)
                .codeHash(passwordEncoder.encode(codigo))
                .expiresAt(expira)
                .attempts(intentos)
                .maxAttempts(5)
                .build();
    }
}
