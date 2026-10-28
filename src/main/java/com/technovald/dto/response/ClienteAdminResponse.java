package com.technovald.dto.response;

import lombok.*;

/**
 * DTO de respuesta que representa a un cliente cuando es consultado por el staff
 * (ADMIN o TRABAJADOR) desde el panel de administración.
 *
 * <p>
 * A diferencia de {@link ClienteResponse} (que se usa para el perfil propio del cliente),
 * este DTO incluye el {@code username} para facilitar tareas de soporte, pero sigue
 * ocultando datos sensibles como el DNI.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClienteAdminResponse {


    /**
     * Identificador único del cliente en MySQL.
     */
    private Long id;

    /**
     * Nombre de pila del cliente (de la Persona asociada).
     */
    private String nombre;

    /**
     * Apellidos del cliente (de la Persona asociada).
     */
    private String apellido;

    /**
     * Correo electrónico del cliente (de la Persona asociada).
     */
    private String email;

    /**
     * Número de teléfono del cliente (de la Persona asociada).
     */
    private String telefono;

    /**
     * Dirección del cliente (de la Persona asociada).
     */
    private String direccion;

    /**
     * Username del cliente (de la Persona asociada) generado automaticamente.
     */
    private String username;
    /**
     * Indica si el perfil del cliente se encuentra activo (soft delete).
     */
    private Boolean activo;
}