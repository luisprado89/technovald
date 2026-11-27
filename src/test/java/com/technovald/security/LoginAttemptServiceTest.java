package com.technovald.security;

import com.technovald.entity.Usuario;
import com.technovald.repository.mysql.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private LoginAttemptService loginAttemptService;

    @Test
    void registrarFalloIncrementaYBloquea() {
        loginAttemptService.registrarFallo(7L);

        verify(usuarioRepository).incrementarIntentosFallidos(7L);
        verify(usuarioRepository).bloquearUsuario(eq(7L), any(LocalDateTime.class));
    }

    @Test
    void registrarExitoResetea() {
        loginAttemptService.registrarExito(7L);

        verify(usuarioRepository).resetearIntentos(7L);
    }

    @Test
    void estaBloqueadoSiLaFechaEsFutura() {
        Usuario usuario = Usuario.builder()
                .id(7L)
                .bloqueadoHasta(LocalDateTime.now().plusMinutes(5))
                .build();

        assertTrue(loginAttemptService.estaBloqueado(usuario));
        verify(usuarioRepository, never()).resetearIntentos(any());
    }

    @Test
    void dejaDeEstarBloqueadoYReseteaSiLaFechaPaso() {
        Usuario usuario = Usuario.builder()
                .id(7L)
                .bloqueadoHasta(LocalDateTime.now().minusMinutes(1))
                .build();

        assertFalse(loginAttemptService.estaBloqueado(usuario));
        verify(usuarioRepository).resetearIntentos(7L);
    }

    @Test
    void sinBloqueoNoEstaBloqueado() {
        assertFalse(loginAttemptService.estaBloqueado(Usuario.builder().id(7L).build()));

        verifyNoInteractions(usuarioRepository);
    }
}
