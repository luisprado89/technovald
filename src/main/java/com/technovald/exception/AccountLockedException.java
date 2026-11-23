package com.technovald.exception;

import java.time.LocalDateTime;

/**
 * La cuenta está bloqueada temporalmente por intentos de inicio de sesión fallidos.
 *
 * <p>El {@link GlobalExceptionHandler} la traduce a HTTP 423 con la cabecera
 * {@code Retry-After}.</p>
 */
public class AccountLockedException extends RuntimeException {

    /** Momento en el que el bloqueo deja de aplicarse. */
    private final LocalDateTime bloqueadoHasta;

    /**
     * Construye la excepción con el mensaje y el fin del bloqueo.
     *
     * @param message        mensaje que se devuelve al cliente.
     * @param bloqueadoHasta instante en el que expira el bloqueo.
     */
    public AccountLockedException(String message, LocalDateTime bloqueadoHasta) {
        super(message);
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /** Devuelve el instante en el que expira el bloqueo. */
    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
