package com.technovald.security;

import com.technovald.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;


@Component
public class CredentialGenerator {

    /**
     * Longitud exacta de las contraseñas generadas.
     */
    private static final int PASSWORD_LENGTH = 16;

    /**
     * Número máximo de candidatos que se prueban al buscar un username libre.
     */
    private static final int USERNAME_MAX_ATTEMPTS = 100000;

    /**
     * Mayúsculas sin la {@code I} ni la {@code O} (ambiguas con {@code 1} y
     * {@code 0}).
     */
    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    /**
     * Minúsculas sin la {@code l} (ambigua con {@code 1}).
     */
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";

    /**
     * Dígitos sin el {@code 0} ni el {@code 1} (ambiguos con {@code O} y
     * {@code l}).
     */
    private static final String DIGITS = "23456789";

    /**
     * Símbolos seguros para JSON, HTML y shell: sin comillas, sin barras, sin
     * {@code < > &} y sin {@code $}.
     */
    private static final String SYMBOLS = "!@#%*+-=?_";

    /**
     * Alfabeto completo (67 caracteres) con el que se rellena el resto de la
     * contraseña.
     */
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SYMBOLS;

    /**
     * Username base cuando ni el nombre ni el apellido aportan ningún carácter
     * válido.
     */
    private static final String USERNAME_FALLBACK = "usuario";

    /**
     * Fuente de aleatoriedad criptográfica del generador.
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Genera una contraseña temporal que cumple la política.
     *
     * <p>
     * La composición mínima está garantizada por construcción: se colocan los
     * cuatro caracteres obligatorios en las primeras posiciones, se rellena el
     * resto con el alfabeto completo y se mezcla el array con Fisher-Yates.
     * </p>
     *
     * @return contraseña en texto plano de {@value #PASSWORD_LENGTH}
     *         caracteres.
     */
    public String generatePassword() {
        char[] password = new char[PASSWORD_LENGTH];

        password[0] = randomChar(UPPERCASE);
        password[1] = randomChar(LOWERCASE);
        password[2] = randomChar(DIGITS);
        password[3] = randomChar(SYMBOLS);
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            password[i] = randomChar(ALL_CHARS);
        }

        shuffle(password);
        return new String(password);
    }

    /**
     * Genera un username único a partir del nombre y el apellido.
     *
     * <p>
     * Normaliza los acentos, sanea a {@code [a-z0-9]} y une el primer nombre
     * con el primer apellido mediante un punto ({@code jose.garcia}). Si ese
     * candidato ya está ocupado según el predicado, prueba
     * {@code jose.garcia1}, {@code jose.garcia2}… hasta agotar
     * {@value #USERNAME_MAX_ATTEMPTS} candidatos.
     * </p>
     *
     * @param nombre   nombre de la persona.
     * @param apellido apellido de la persona.
     * @param exists   predicado que devuelve {@code true} si el candidato ya
     *                 existe. Se recibe como parámetro para no acoplar este
     *                 componente al repositorio de usuarios.
     * @return username único que cumple el patrón {@code [a-z0-9.]+}.
     * @throws BusinessRuleException si todos los candidatos probados ya están ocupados.
     */
    public String generateUsername(String nombre, String apellido, Predicate<String> exists) {
        String base = buildBaseUsername(nombre, apellido);

        /*
         * Intento 0 -> "jose.garcia"; intento N -> "jose.garciaN".
         * En el peor caso se consulta el predicado exactamente
         * USERNAME_MAX_ATTEMPTS veces.
         */
        for (int attempt = 0; attempt < USERNAME_MAX_ATTEMPTS; attempt++) {
            String candidate = attempt == 0 ? base : base + attempt;
            if (!exists.test(candidate)) {
                return candidate;
            }
        }

        throw new BusinessRuleException("No se pudo generar un username único para '"
                + base + "' tras " + USERNAME_MAX_ATTEMPTS + " intentos");
    }

    /**
     * Devuelve un carácter aleatorio del alfabeto indicado.
     *
     * @param alphabet conjunto de caracteres candidatos.
     * @return carácter aleatorio del conjunto.
     */
    private char randomChar(String alphabet) {
        return alphabet.charAt(secureRandom.nextInt(alphabet.length()));
    }

    /**
     * Mezcla el array in-place con el algoritmo de Fisher-Yates usando
     * {@link SecureRandom}, de modo que la posición de los caracteres
     * obligatorios no sea predecible.
     *
     * @param chars array de caracteres que se mezcla.
     */
    private void shuffle(char[] chars) {
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char swap = chars[i];
            chars[i] = chars[j];
            chars[j] = swap;
        }
    }

    /**
     * Construye el username base (sin sufijo) a partir del primer nombre y el
     * primer apellido.
     *
     * @param nombre   nombre de la persona.
     * @param apellido apellido de la persona.
     * @return username base saneado.
     */
    private String buildBaseUsername(String nombre, String apellido) {
        String firstName = sanitize(firstToken(nombre));
        String firstSurname = sanitize(firstToken(apellido));

        if (firstName.isEmpty() && firstSurname.isEmpty()) {
            return USERNAME_FALLBACK;
        }
        if (firstName.isEmpty()) {
            return firstSurname;
        }
        if (firstSurname.isEmpty()) {
            return firstName;
        }
        return firstName + "." + firstSurname;
    }

    /**
     * Extrae el primer token de un valor de texto.
     *
     * @param value valor original (puede ser {@code null}).
     * @return primer token sin espacios, o cadena vacía si no hay ninguno.
     */
    private String firstToken(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed.split("\\s+")[0];
    }

    /**
     * Normaliza un texto a un fragmento de username válido.
     *
     * <p>
     * Descompone en NFD y elimina los diacríticos ({@code José} → {@code Jose},
     * {@code ñ} → {@code n}), pasa a minúsculas con {@link Locale#ROOT} (evita
     * el comportamiento dependiente del locale del servidor, como la
     * conversión de {@code I} en {@code ı} con el locale turco) y descarta
     * cualquier carácter que no sea {@code [a-z0-9]} (apóstrofos, puntos,
     * espacios, guiones y símbolos).
     * </p>
     *
     * @param value texto original.
     * @return fragmento saneado, posiblemente vacío.
     */
    private String sanitize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}