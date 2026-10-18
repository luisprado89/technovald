package com.technovald.dto.response;

import lombok.*;

/**
 * Datos de un empleado que la API devuelve al consultar el directorio.
 *
 * <p>
 * Aplana la cadena {@code empleado → persona → usuario}: los datos personales
 * salen de la persona, el {@code username} del usuario asociado y el
 * {@code rol} del propio empleado. El DNI no aparece, igual que en el resto de
 * respuestas de la API.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EmpleadoResponse {

    /**
     * Identificador del perfil de empleado.
     */
    private Long id;

    /**
     * Nombre de pila (de la persona asociada).
     */
    private String nombre;

    /**
     * Apellidos (de la persona asociada).
     */
    private String apellido;

    /**
     * Email de contacto (de la persona asociada).
     */
    private String email;

    /**
     * Teléfono de contacto (de la persona asociada).
     */
    private String telefono;

    /**
     * Dirección postal (de la persona asociada).
     */
    private String direccion;

    /**
     * Nombre de usuario con el que el empleado inicia sesión.
     */
    private String username;

    /**
     * Rol del empleado: {@code ADMIN} o {@code TRABAJADOR}.
     */
    private String rol;

    /**
     * Estado lógico del perfil de empleado.
     */
    private Boolean activo;
}