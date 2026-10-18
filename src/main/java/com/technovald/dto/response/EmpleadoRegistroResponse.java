package com.technovald.dto.response;

import lombok.*;

/**
 * Respuesta del alta de un empleado ({@code POST /api/admin/empleados}).
 *
 * <p>
 * Devuelve las credenciales generadas: el {@code username} y la contraseña
 * inicial en claro, que el administrador debe entregar al empleado. En la base
 * de datos solo queda el hash BCrypt, y esta es la única respuesta en la que la
 * contraseña viaja en claro.
 * </p>
 *
 * <p>
 * A diferencia de {@link ClienteRegistroResponse}, <b>no incluye token</b>:
 * quien crea la cuenta ya está autenticado, y el token lo obtiene el empleado
 * al iniciar sesión con las credenciales que recibe.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EmpleadoRegistroResponse {

    /**
     * Identificador del perfil de empleado creado.
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
     * Username generado automáticamente ({@code nombre.apellido}).
     */
    private String username;

    /**
     * Contraseña inicial generada aleatoriamente (texto plano).
     *
     * <p>
     * <b>Se devuelve únicamente en esta respuesta</b>. El administrador tiene
     * que entregarla al empleado; no se envía por ningún otro canal.
     * </p>
     */
    private String passwordInicial;

    /**
     * Rol asignado en el alta: {@code ADMIN} o {@code TRABAJADOR}.
     */
    private String rol;
}