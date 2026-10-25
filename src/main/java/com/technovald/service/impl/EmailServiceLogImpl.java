package com.technovald.service.impl;

import com.technovald.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link EmailService} para el perfil de desarrollo ("dev").
 * Simula el envío imprimiendo el contenido del correo en la consola.
 */
@Service
@Profile("dev")
@Slf4j
public class EmailServiceLogImpl implements EmailService {

    @Override
    public void sendPasswordResetEmail(String email, String username, String nombre, String nuevaPassword) {
        log.info("""
                === EMAIL SIMULADO (DEV) ===
                To: {}
                Subject: Recuperación de acceso a TechNovaLD
                Body: Hola {},
                      Hemos recibido una solicitud para restablecer tu contraseña.
                      Tu nombre de usuario es: {}
                      Tu nueva contraseña temporal es: {}
                      Por seguridad, te recomendamos cambiarla al iniciar sesión.
                ============================""", email, nombre, username, nuevaPassword);
    }
}