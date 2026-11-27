package com.technovald.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida la política de contraseñas de {@link StrongPassword}.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    /** Longitud mínima. */
    private static final int MINIMO = 10;

    /** Límite real de BCrypt, en bytes UTF-8. */
    private static final int MAXIMO_BYTES = 72;

    private static final Pattern MAYUSCULA = Pattern.compile("[A-Z]");
    private static final Pattern MINUSCULA = Pattern.compile("[a-z]");
    private static final Pattern DIGITO = Pattern.compile("\\d");
    private static final Pattern SIMBOLO = Pattern.compile("[^A-Za-z0-9]");

    /** Contraseñas frecuentes que se rechazan aunque cumplan la composición. */
    private static final Set<String> COMUNES = Set.of(
            "password", "password1", "password123", "passw0rd", "123456", "1234567", "12345678",
            "123456789", "1234567890", "qwerty", "qwerty123", "abc123", "1q2w3e4r",
            "iloveyou", "admin", "admin123", "welcome", "monkey", "letmein", "dragon",
            "football", "master", "sunshine", "princess", "shadow", "superman",
            "contraseña", "hola1234", "usuario", "root", "toor", "zxcvbnm", "asdfgh");

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null) {
            return true;
        }
        if (valor.length() < MINIMO
                || valor.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            return false;
        }

        int clases = 0;
        if (MAYUSCULA.matcher(valor).find()) {
            clases++;
        }
        if (MINUSCULA.matcher(valor).find()) {
            clases++;
        }
        if (DIGITO.matcher(valor).find()) {
            clases++;
        }
        if (SIMBOLO.matcher(valor).find()) {
            clases++;
        }

        return clases >= 3 && !COMUNES.contains(valor.toLowerCase(Locale.ROOT));
    }
}
