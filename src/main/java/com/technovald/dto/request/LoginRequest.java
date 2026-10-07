package com.technovald.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO de request para el endpoint de login
 * ({@code POST /api/auth/login}).
 *
 * <p>
 * Contiene las credenciales introducidas por el usuario en el
 * formulario de inicio de sesión. El servicio valida las credenciales
 * contra la base de datos MySQL y, si son correctas, genera un
 * token JWT que se devuelve en la respuesta.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginRequest {

    /**
     * Username generado automáticamente durante el registro
     * (ej: "luciano.garcia").
     */
    @NotBlank(message = "El username es obligatorio")
    private String username;

    /**
     * Contraseña en texto plano introducida por el usuario.
     * El servicio la compara con el hash BCrypt almacenado.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}