package com.technovald.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa el perfil de cliente en TechNovaLD.
 *
 * <p>
 * Cliente es un perfil de dominio que "envuelve" a una {@link Persona}
 * para añadirle la dimensión de compras. Un cliente puede realizar
 * pedidos de productos y consultar su historial.
 * </p>
 *
 * <p>
 * La estructura del modelo de identidad de TechNovaLD separa los datos
 * personales (Persona), las credenciales (Usuario) y los perfiles de
 * dominio (Cliente, Empleado) en tablas independientes. Cliente es el
 * dueño de la relación con Persona: el FK {@code persona_id} vive en
 * la tabla {@code cliente}.
 * </p>
 *
 * <p>
 * El rol de un cliente es siempre {@code CLIENTE} en el
 * {@link Usuario} asociado. Por este motivo, la entidad Cliente no
 * tiene un campo {@code rol} redundante (a diferencia de
 * {@link Empleado} que sí lo tiene para distinguir
 * {@code ADMIN} de {@code TRABAJADOR}).
 * </p>
 *
 * <p>
 * Cliente no declara {@code CascadeType} en la relación con Persona ni
 * con Pedido. La filosofía es que cada entidad mantiene su propio
 * ciclo de vida y no debe eliminarse en cascada.
 * </p>
 *
 * <p><b>Índice:</b> definido con {@code @Index} usando {@code name = "idx_cliente_activo"}
 * y {@code columnList = "activo"}, para optimizar búsquedas frecuentes por estado
 * (clientes activos/inactivos).</p>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio de
 * un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE}) y
 * espacio en disco. Solo deben aplicarse sobre columnas usadas frecuentemente
 * en {@code WHERE}, {@code JOIN} u {@code ORDER BY}.</p>
 *
 * @see jakarta.persistence.Index
 */
@Entity
@Table(name = "cliente", indexes = {
        @Index(name = "idx_cliente_activo", columnList = "activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Cliente {

    /**
     * Identificador único del cliente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Persona a la que pertenece el perfil de cliente.
     *
     * <p>
     * Cliente es el dueño de la relación: el FK {@code persona_id}
     * vive en la tabla {@code cliente}. Una persona puede tener como
     * máximo un perfil de cliente.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos personales
     * cuando solo se necesita el identificador del cliente.
     * </p>
     *
     * <p>
     * {@code nullable = false} y {@code unique = true}: todo cliente
     * debe tener una persona, y una persona solo puede tener un
     * perfil de cliente.
     * </p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    /**
     * Lista de pedidos realizados por el cliente.
     *
     * <p>
     * Relación inversa: el FK {@code cliente_id} vive en la tabla
     * {@code pedido}. Esta lista se carga de forma perezosa
     * ({@code FetchType.LAZY}) para evitar recuperar todos los
     * pedidos del cliente cuando solo se necesita información del
     * propio perfil.
     * </p>
     *
     * <p>
     * Los pedidos mantienen su propio ciclo de vida y no se eliminan
     * al desactivar el cliente. Esto garantiza la persistencia del
     * historial de compras para auditoría.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} inicializa la lista como
     * {@code new ArrayList<>()}. Sin esta anotación, Lombok Builder
     * dejaría el campo en {@code null}.
     * </p>
     */
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();

    /**
     * Indica si el perfil de cliente se encuentra activo.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Al desactivar
     * un cliente, el servicio también desactiva la {@link Persona}
     * y el {@link Usuario} asociados, impidiendo que el cliente pueda
     * autenticarse.
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
