package com.technovald.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no existe o está inactivo
 * (borrado lógico).
 *
 * <p>Es capturada por {@link GlobalExceptionHandler} y devuelta al cliente
 * como HTTP 404 (Not Found) con un mensaje descriptivo.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje descriptivo.
     *
     * @param message mensaje que describe el recurso no encontrado.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}