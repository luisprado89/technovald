package com.technovald.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de response para el login exitoso
 * ({@code POST /api/auth/login}).
 *
 * <p>
 * Contiene el token JWT que el frontend debe almacenar en
 * {@code localStorage} e incluir en la cabecera
 * {@code Authorization: Bearer <token>} en todas las peticiones
 * posteriores. También incluye el username, el rol y la fecha de
 * expiración del token.
 * </p>
 *
 * <p>
 * El frontend utiliza el campo {@code rol} para mostrar/ocultar
 * botones y secciones del dashboard según los permisos del usuario.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuthResponse {

    /**
     * Token JWT generado por el servicio de autenticación.
     */
    private String token;

    /**
     * Username del usuario autenticado.
     */
    private String username;

    /**
     * Rol del usuario ({@code ADMIN}, {@code TRABAJADOR} o
     * {@code CLIENTE}).
     */
    private String rol;

    /**
     * Fecha y hora de expiración del token JWT.
     * El frontend puede usarla para saber cuándo debe renovar.
     */
    private LocalDateTime expiracion;
}