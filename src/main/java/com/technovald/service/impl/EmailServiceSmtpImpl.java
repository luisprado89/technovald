package com.technovald.service.impl;

import com.technovald.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link EmailService} para el perfil de producción ("prod").
 *
 * <p>
 * Envía un correo HTML real por SMTP (Gmail) con la contraseña temporal. El
 * método es asíncrono para no bloquear la petición de recuperación.
 * </p>
 */
@Service
@Profile("prod")
@RequiredArgsConstructor
public class EmailServiceSmtpImpl implements EmailService {

    private final JavaMailSender mailSender;

    /** Remitente; coincide con spring.mail.username. */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envía el correo con la contraseña temporal.
     *
     * @param email         email del destinatario.
     * @param username      nombre de usuario de la cuenta.
     * @param nombre        nombre de pila del destinatario.
     * @param nuevaPassword contraseña temporal generada.
     */
    @Async
    @Override
    public void sendPasswordResetEmail(String email, String username, String nombre, String nuevaPassword) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "utf-8");

            String html = "<h2>Hola " + nombre + ",</h2>"
                    + "<p>Has solicitado restablecer tu contraseña en TechNovaLD.</p>"
                    + "<p>Tu nombre de usuario es: <b>" + username + "</b></p>"
                    + "<p>Tu nueva contraseña temporal es: "
                    + "<b style='font-size:16px;'>" + nuevaPassword + "</b></p>"
                    + "<p>Por favor, inicia sesión y cámbiala cuanto antes.</p>"
                    + "<br><p>Saludos,<br>Equipo de TechNovaLD</p>";

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Tu nueva contraseña en TechNovaLD");
            helper.setText(html, true);

            mailSender.send(mensaje);
        } catch (MessagingException ex) {
            throw new RuntimeException("Error al enviar el email de reseteo: " + ex.getMessage(), ex);
        }
    }
}