package com.technovald.entity;

import com.technovald.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un pedido realizado por un cliente en TechNovaLD.
 *
 * <p>
 * Un pedido es la cabecera de una operación de compra. Contiene la
 * información general (cliente, fecha, estado, total) y está formado por
 * una o varias líneas de detalle ({@link PedidoDetalle}), cada una
 * correspondiente a un producto y una cantidad.
 * </p>
 *
 * <p>
 * El ciclo de vida de un pedido es lineal y está gobernado por la
 * enumeración {@link EstadoPedido}:
 * </p>
 *
 * <pre>
 * PENDIENTE → PROCESANDO → ENVIADO → ENTREGADO
 * </pre>
 *
 * <p>
 * No existe un estado {@code CANCELADO}. Si un pedido necesita anularse,
 * se gestiona mediante el borrado lógico del campo {@code activo}.
 * </p>
 *
 * <p>
 * El campo {@code total} es una desnormalización intencional: se calcula
 * sumando los subtotales de los {@link PedidoDetalle} en el momento de
 * la creación y se almacena en la cabecera. De esta forma no es necesario
 * recalcular el total cada vez que se consulta el pedido, y el total
 * histórico se conserva aunque los precios de los productos cambien en
 * el futuro.
 * </p>
 *
 * <p>
 * El campo {@code fechaPedido} se establece automáticamente mediante
 * {@code @PrePersist} al momento de persistir la entidad. No es
 * necesario setearlo manualmente en el servicio.
 * </p>
 *
 * <p>
 * Pedido no declara {@code CascadeType} en la relación con
 * {@link PedidoDetalle}. Los detalles mantienen su propio ciclo de vida
 * y se persisten individualmente dentro de la misma transacción.
 * </p>
 *
 * <p><b>Índices definidos:</b></p>
 * <ul>
 *   <li>{@code @Index(name = "idx_pedido_activo", columnList = "activo")}:
 *       optimiza búsquedas frecuentes por soft delete (pedidos activos/inactivos).</li>
 *   <li>{@code @Index(name = "idx_pedido_cliente_activo", columnList = "cliente_id, activo")}:
 *       índice compuesto que optimiza búsquedas de pedidos filtrados por cliente
 *       y estado simultáneamente.</li>
 *   <li>{@code @Index(name = "idx_pedido_estado_activo", columnList = "estado, activo")}:
 *       optimiza listados de pedidos filtrados por estado
 *       ({@code PENDIENTE}, {@code PROCESANDO}, {@code ENVIADO}, {@code ENTREGADO}).</li>
 * </ul>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio de
 * un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE}) y
 * espacio en disco. Solo deben aplicarse sobre columnas usadas frecuentemente
 * en {@code WHERE}, {@code JOIN} u {@code ORDER BY}.</p>
 *
 * @see jakarta.persistence.Index
 */
@Entity
@Table(name = "pedido", indexes = {
        @Index(name = "idx_pedido_activo", columnList = "activo"),
        @Index(name = "idx_pedido_cliente_activo", columnList = "cliente_id, activo"),
        @Index(name = "idx_pedido_estado_activo", columnList = "estado, activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Pedido {

    /**
     * Identificador único del pedido.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Fecha en la que se realizó el pedido.
     *
     * <p>
     * Se establece automáticamente mediante {@code @PrePersist} al
     * momento de persistir la entidad. El flag {@code updatable = false}
     * garantiza que no pueda modificarse en futuras actualizaciones.
     * </p>
     */
    @Column(name = "fecha_pedido", nullable = false, updatable = false)
    private LocalDateTime fechaPedido;

    /**
     * Estado actual del pedido dentro del ciclo de vida lineal.
     *
     * <p>
     * Se inicializa como {@code PENDIENTE} de forma predeterminada
     * mediante {@code @Builder.Default}. El servicio de pedidos es
     * el responsable de validar las transiciones de estado legales.
     * </p>
     *
     * <p>
     * Se persiste como texto literal en MySQL mediante
     * {@code @Enumerated(EnumType.STRING)}. Se define explícitamente como
     * {@code VARCHAR(50)} usando {@code columnDefinition} para evitar que
     * Hibernate 6+ intente alterar o recrear la columna como un tipo
     * {@code ENUM} nativo en cada arranque, garantizando mayor portabilidad
     * y permitiendo consultar pedidos por estado de forma legible desde SQL.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)", nullable = false)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    /**
     * Importe total del pedido.
     *
     * <p>
     * Se calcula sumando los subtotales de todos los
     * {@link PedidoDetalle} asociados. Se almacena en la cabecera como
     * desnormalización para evitar recalcularlo en cada consulta y
     * para preservar el total histórico aunque los precios de los
     * productos cambien en el futuro.
     * </p>
     *
     * <p>
     * {@code precision = 10, scale = 2} permite almacenar importes
     * de hasta 99.999.999,99.
     * </p>
     */
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    /**
     * Cliente que realizó el pedido.
     *
     * <p>
     * Pedido es el dueño de la relación: el FK {@code cliente_id}
     * vive en la tabla {@code pedido}. Todo pedido debe tener un
     * cliente asociado ({@code nullable = false}).
     * </p>
     *
     * <p>
     * Esta relación sustituye cualquier FK directo a {@link Usuario}
     * que pudiera existir en versiones anteriores del proyecto.
     * La asociación con {@link Cliente} garantiza que el pedido está
     * vinculado al perfil de compras, no a las credenciales de
     * autenticación.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos del cliente
     * cuando solo se necesita información del propio pedido.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /**
     * Lista de líneas de detalle del pedido.
     *
     * <p>
     * Relación inversa: el FK {@code pedido_id} vive en la tabla
     * {@code pedido_detalle}. Cada detalle contiene un producto y
     * una cantidad.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar todos los detalles
     * cuando solo se necesita la cabecera del pedido. El servicio
     * puede utilizar {@code JOIN FETCH} en consultas JPQL cuando
     * necesite recuperar el pedido con sus detalles en una sola
     * consulta.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} inicializa la lista como
     * {@code new ArrayList<>()}. Sin esta anotación, Lombok Builder
     * dejaría el campo en {@code null}.
     * </p>
     */
    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PedidoDetalle> pedidoDetalles = new ArrayList<>();

    /**
     * Indica si el pedido se encuentra activo en el sistema.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Los pedidos
     * desactivados permanecen en la base de datos para auditoría pero
     * no aparecen en las consultas estándar.
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

    /**
     * Callback de JPA que se ejecuta automáticamente antes de persistir
     * la entidad por primera vez (INSERT).
     *
     * <p>
     * Establece la fecha del pedido al momento actual. De esta forma
     * no es necesario preocuparse por setear este campo en la capa de
     * servicio.
     * </p>
     */
    @PrePersist
    protected void onCreate() {
        this.fechaPedido = LocalDateTime.now();
    }
}
