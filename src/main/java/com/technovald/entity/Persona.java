package com.technovald.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad central que almacena los datos personales de una persona
 * en el sistema TechNovaLD.
 *
 * <p>
 * Persona es el núcleo del modelo de identidad. Contiene la información
 * civil (nombre, apellido, DNI, email, teléfono, dirección) y es
 * referenciada por tres entidades perfil mediante una relación 1:1:
 * </p>
 *
 * <ul>
 *     <li>{@code Usuario}: credenciales de autenticación (username,
 *     password, rol). El FK {@code persona_id} vive en la tabla
 *     {@code usuario}.</li>
 *     <li>{@code Cliente}: perfil de compras para usuarios finales.
 *     El FK {@code persona_id} vive en la tabla {@code cliente}.</li>
 *     <li>{@code Empleado}: perfil laboral para trabajadores y
 *     administradores. El FK {@code persona_id} vive en la tabla
 *     {@code empleado}.</li>
 * </ul>
 *
 * <p>
 * Esta separación permite que una misma Persona pueda tener
 * simultáneamente un {@code Usuario} (para autenticarse) y un perfil
 * de dominio ({@code Cliente} o {@code Empleado}), cada uno con su
 * propio ciclo de vida y su propio campo {@code activo} para borrado
 * lógico independiente.
 * </p>
 *
 * <p>
 * Persona no declara {@code CascadeType} en ninguna de sus relaciones
 * inversas. La filosofía es que Persona tiene su propio ciclo de vida
 * y no debe eliminarse en cascada cuando se modifica o desactiva un
 * Usuario, Cliente o Empleado.
 * </p>
 *
 *
 * <p><b>Índice:</b> definido con {@code @Index} usando {@code name = "idx_persona_activo"}
 * y {@code columnList = "activo"}, para optimizar búsquedas frecuentes por estado
 * (personas activos/inactivos).</p>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio de
 * un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE}) y
 * espacio en disco. Solo deben aplicarse sobre columnas usadas frecuentemente
 * en {@code WHERE}, {@code JOIN} u {@code ORDER BY}.</p>
 *
 * @see jakarta.persistence.Index
 */
@Entity
@Table(name = "persona", indexes = {
        @Index(name = "idx_persona_activo", columnList = "activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Persona {
    /**
     * Identificador único de la persona
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Nombre de la persona
     */
    @Column(name = "nombre", nullable = false)
    private String nombre;
    /**
     * Apellidos de la persona.
     * <p>Puede contener uno o varios apellidos separados por espacios. El algoritmo de generación de username
     * utiliza únicamente el primer apellido.</p>
     */
    @Column(name = "apellido", nullable = false)
    private String apellido;

    /**
     * DNI de la persona, formato válido: 8 dígitos seguidos de una letra mayúscula (ej: {@code 12345678X})).
     * <p>Restricción {@code unique = true} a nivel columna para garantizar que no existan dos personas con el mismo DNI.</p>
     */
    @Column(name = "dni", nullable = false, unique = true, length = 9)
    private String dni;

    /**
     * Email de la persona.
     * <p>Se utiliza tanto como dato de contacto, como de mecanismo de recuperación de contraseña en el proceso de
     * "olvidé mi contraseña" para clientes.</p>
     * <p>Restricción {@code unique = true} a nivel columna para garantizar que no existan dos personas con el mismo Email.</p>
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * Número de teléfono de la persona.
     */
    @Column(name = "telefono", nullable = false)
    private String telefono;

    /**
     * <p>
     * El FK {@code persona_id} vive en la tabla {@code usuario},
     * por lo que esta relación es {@code mappedBy} (lado inverso).
     * Una persona puede tener como máximo un usuario.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar el usuario cuando
     * solo se necesitan los datos personales.
     * </p>
     */
    @OneToOne(mappedBy = "persona", fetch = FetchType.LAZY)
    private Usuario usuario;

    /**
     * Cliente asociado a la persona (relación inversa).
     *
     * <p>
     * El FK {@code persona_id} vive en la tabla {@code cliente}.
     * Una persona puede tener como máximo un perfil de cliente.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar el cliente cuando
     * solo se necesitan los datos personales.
     * </p>
     */
    @OneToOne(mappedBy = "persona", fetch = FetchType.LAZY)
    private Cliente cliente;

    /**
     * Empleado asociado a la persona (relación inversa).
     *
     * <p>
     * El FK {@code persona_id} vive en la tabla {@code empleado}.
     * Una persona puede tener como máximo un perfil de empleado.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar el empleado cuando
     * solo se necesitan los datos personales.
     * </p>
     */
    @OneToOne(mappedBy = "persona", fetch = FetchType.LAZY)
    private Empleado empleado;

    /**
     * Indica si la persona se encuentra activa en el sistema.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Cuando una
     * persona se desactiva, también deberían desactivarse su
     * {@code Usuario}, {@code Cliente} y {@code Empleado} asociados,
     * aunque esta lógica se gestiona en la capa de servicio, no
     * mediante cascade JPA.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} es vital aquí: sin esta anotación,
     * Lombok Builder dejaría el campo en {@code null} en lugar de
     * {@code true}.
     * </p>
     */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
