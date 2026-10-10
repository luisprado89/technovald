package com.technovald.exception;

/**
 * Excepción lanzada cuando se infringe una regla de negocio.
 *
 * <p>Es capturada por {@link GlobalExceptionHandler} y devuelta al cliente
 * como HTTP 400 (Bad Request) con un mensaje descriptivo.</p>
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje descriptivo.
     *
     * @param message mensaje que describe la regla de negocio infringida.
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
