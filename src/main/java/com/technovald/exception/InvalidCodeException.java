package com.technovald.exception;

/**
 * El código de activación o de reseteo no es válido, ha caducado o se ha agotado.
 *
 * <p>El mensaje es siempre el mismo para no distinguir el motivo.</p>
 */
public class InvalidCodeException extends RuntimeException {

    public InvalidCodeException() {
        super("Código inválido o expirado");
    }
}
