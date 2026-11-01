package com.technovald.exception;

/**
 * Error al consultar Icecat: fallo de red, HTTP o ficha inexistente
 * ({@code msg} distinto de "OK").
 *
 * <p>
 * Es una excepción no comprobada. El {@code GlobalExceptionHandler} la
 * traduce a HTTP 503, porque el problema está en el servicio externo. El
 * 404 de "EAN sin inventario" lo lanza {@code ResourceNotFoundException},
 * no esta clase.
 * </p>
 */
public class IcecatApiException extends RuntimeException {

    /**
     * Construye la excepción con el motivo del fallo.
     *
     * @param message mensaje que se devuelve al cliente.
     */
    public IcecatApiException(String message) {
        super(message);
    }
}