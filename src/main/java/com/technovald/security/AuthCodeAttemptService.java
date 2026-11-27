package com.technovald.security;

import com.technovald.repository.mysql.AuthCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra los intentos fallidos de un código en una transacción independiente.
 */
@Service
@RequiredArgsConstructor
public class AuthCodeAttemptService {

    private final AuthCodeRepository authCodeRepository;

    /**
     * Suma un intento fallido al código y lo confirma aunque la transacción del llamador falle.
     *
     * @param authCodeId id del código.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoFallido(Long authCodeId) {
        authCodeRepository.incrementarIntentos(authCodeId);
    }
}
