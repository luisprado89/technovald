package com.technovald.service;

/** Envío de correos de la aplicación. */
public interface EmailService {

    /**
     * Envía al usuario sus credenciales de acceso tras una recuperación.
     *
     * @param email            email del destinatario.
     * @param username      nombre de usuario de la cuenta.
     * @param nombre        nombre de pila del destinatario.
     * @param nuevaPassword contraseña temporal generada.
     */
    void sendPasswordResetEmail(String email, String username, String nombre, String nuevaPassword);
}