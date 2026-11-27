package com.technovald.security;

import com.technovald.entity.Usuario;
import com.technovald.repository.mysql.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Gestiona el bloqueo temporal de una cuenta por intentos de login fallidos.
 *
 * <p>Los métodos que escriben usan {@code REQUIRES_NEW}: el contador debe
 * persistir aunque el login acabe lanzando una excepción.</p>
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /** Intentos fallidos que disparan el bloqueo. Coincide con la condición del repositorio. */
    public static final int MAX_INTENTOS = 5;

    /** Duración del bloqueo temporal. */
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final UsuarioRepository usuarioRepository;

    /**
     * Suma un fallo y bloquea la cuenta si alcanza el máximo.
     *
     * @param usuarioId id del usuario.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(Long usuarioId) {
        usuarioRepository.incrementarIntentosFallidos(usuarioId);
        usuarioRepository.bloquearUsuario(usuarioId, LocalDateTime.now().plus(DURACION_BLOQUEO));
    }

    /**
     * Limpia el contador y el bloqueo tras un login correcto.
     *
     * @param usuarioId id del usuario.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(Long usuarioId) {
        usuarioRepository.resetearIntentos(usuarioId);
    }

    /**
     * Indica si la cuenta sigue bloqueada. Si el bloqueo ya expiró, lo limpia.
     *
     * @param usuario usuario cargado de base de datos.
     * @return {@code true} si el bloqueo está vigente.
     */
    @Transactional
    public boolean estaBloqueado(Usuario usuario) {
        LocalDateTime hasta = usuario.getBloqueadoHasta();
        if (hasta == null) {
            return false;
        }
        if (hasta.isAfter(LocalDateTime.now())) {
            return true;
        }
        usuarioRepository.resetearIntentos(usuario.getId());
        return false;
    }
}
