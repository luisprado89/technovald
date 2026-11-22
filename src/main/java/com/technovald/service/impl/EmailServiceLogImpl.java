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

    @Override
    public void sendBajaCuentaEmail(String email, String nombre, String motivo) {
        log.info("""
                === EMAIL SIMULADO (DEV) ===
                To: {}
                Subject: Baja de tu cuenta en TechNovaLD
                Body: Hola {},
                      Tu cuenta en TechNovaLD ha sido dada de baja.
                      Motivo: {}
                      La cuenta queda desactivada; tus datos no se eliminan.
                      Si crees que se trata de un error, contacta con soporte.
                ============================""", email, nombre, motivo);
    }
}