package com.technovald.dto.response;

import lombok.*;

/**
 * Datos del perfil de un cliente que la API devuelve al consultarlo.
 *
 * <p>
 * Los datos personales (nombre, apellido, email, teléfono y dirección) se leen
 * de la {@code Persona} asociada: la fila de {@code cliente} solo guarda esa
 * relación y su estado lógico. El mapper es quien aplana las dos entidades en
 * este DTO, de modo que la forma del modelo de identidad no se filtra a la API.
 * </p>
 *
 * <p>
 * El DNI no aparece: es la clave de identidad de la persona y la API no la
 * expone en ninguna respuesta. El alta de cliente tampoco lo devuelve.
 * </p>
 */

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClienteResponse {

    /**
     * Identificador del perfil de cliente.
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
     * Estado lógico del perfil: {@code false} si está dado de baja.
     */
    private Boolean activo;
}
