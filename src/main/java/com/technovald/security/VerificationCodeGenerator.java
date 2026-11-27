package com.technovald.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Genera códigos numéricos de un solo uso con {@link SecureRandom}.
 */
@Component
public class VerificationCodeGenerator {

    /** Número de dígitos del código. */
    private static final int DIGITOS = 6;

    /** Rango de valores posibles: 10^6. */
    private static final int RANGO = 1_000_000;

    /** Fuente de aleatoriedad criptográfica. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera un código de seis dígitos, conservando los ceros a la izquierda.
     *
     * @return el código como {@code String}.
     */
    public String generarCodigo() {
        return String.format(Locale.ROOT, "%0" + DIGITOS + "d", RANDOM.nextInt(RANGO));
    }
}
