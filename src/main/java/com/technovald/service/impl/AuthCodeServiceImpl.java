package com.technovald.service.impl;

import com.technovald.entity.AuthCode;
import com.technovald.entity.Usuario;
import com.technovald.enums.PropositoCodigo;
import com.technovald.exception.InvalidCodeException;
import com.technovald.repository.mysql.AuthCodeRepository;
import com.technovald.security.AuthCodeAttemptService;
import com.technovald.security.VerificationCodeGenerator;
import com.technovald.service.AuthCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementación de {@link AuthCodeService}.
 *
 * <p>El código se guarda hasheado con BCrypt y se consume con un UPDATE
 * condicional, de forma que dos peticiones simultáneas no lo usan dos veces.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthCodeServiceImpl implements AuthCodeService {

    private final AuthCodeRepository authCodeRepository;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final AuthCodeAttemptService authCodeAttemptService;
    private final PasswordEncoder passwordEncoder;

    /** Minutos de validez de un código. */
    @Value("${app.auth.code.ttl-minutos:10}")
    private long ttlMinutos = 10;

    /** Intentos permitidos por código. */
    @Value("${app.auth.code.max-intentos:5}")
    private int maxIntentos = 5;

    @Override
    @Transactional
    public String emitir(Usuario usuario, PropositoCodigo purpose) {
        LocalDateTime ahora = LocalDateTime.now();
        authCodeRepository.invalidarVigentes(usuario.getId(), purpose, ahora);

        String codigo = verificationCodeGenerator.generarCodigo();
        authCodeRepository.save(AuthCode.builder()
                .usuarioId(usuario.getId())
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(codigo))
                .expiresAt(ahora.plusMinutes(ttlMinutos))
                .attempts(0)
                .maxAttempts(maxIntentos)
                .build());

        return codigo;
    }

    @Override
    @Transactional
    public void verificarYConsumir(Long usuarioId, PropositoCodigo purpose, String codigo) {
        LocalDateTime ahora = LocalDateTime.now();

        AuthCode fila = authCodeRepository
                .findFirstByUsuarioIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(usuarioId, purpose)
                .orElseThrow(InvalidCodeException::new);

        if (fila.getExpiresAt().isBefore(ahora) || fila.getAttempts() >= fila.getMaxAttempts()) {
            throw new InvalidCodeException();
        }

        if (codigo == null || !passwordEncoder.matches(codigo, fila.getCodeHash())) {
            authCodeAttemptService.registrarIntentoFallido(fila.getId());
            throw new InvalidCodeException();
        }

        if (authCodeRepository.consumir(fila.getId(), ahora) != 1) {
            throw new InvalidCodeException();
        }
    }
}
