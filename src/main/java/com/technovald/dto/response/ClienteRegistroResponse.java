package com.technovald.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClienteRegistroResponse {

    /**
     * ID del cliente creado.
     */
    private Long id;

    /**
     * Nombre del cliente (de la Persona registrada).
     */
    private String nombre;

    /**
     * Apellidos del cliente (de la Persona registrada).
     */
    private String apellido;

    /**
     * Username generado automáticamente ({@code nombre.apellido}).
     */
    private String username;

    /**
     * Contraseña inicial generada aleatoriamente (texto plano).
     *
     * <p>
     * <b>Se devuelve únicamente en esta respuesta</b>. En la base
     * de datos solo se almacena el hash BCrypt. El cliente debe
     * cambiarla cuanto antes vía {@code /api/auth/change-password}.
     * </p>
     */
    private String passwordInicial;

    /**
     * Rol asignado en el alta: siempre {@code CLIENTE}.
     */
    private String rol;

    /**
     * Token JWT emitido para el cliente recién creado.
     */
    private String token;
}
