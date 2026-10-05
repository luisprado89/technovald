package com.technovald.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa una línea de detalle de un pedido en
 * TechNovaLD.
 *
 * <p>
 * Cada {@link Pedido} está formado por una o varias líneas de detalle.
 * Cada línea asocia un {@link ProductoInventario} con una cantidad y
 * un precio unitario.
 * </p>
 *
 * <p>
 * El campo {@code precioUnitario} es un <b>snapshot</b> del precio del
 * producto en el momento en que se realizó el pedido. Esto significa
 * que aunque el precio del producto cambie en el futuro en
 * {@link ProductoInventario#getPrecioVenta()} ()}, el detalle del pedido
 * histórico conservará el precio original. De esta forma el total del
 * pedido ({@link Pedido#getTotal()}) nunca se ve afectado por cambios
 * de precios posteriores.
 * </p>
 *
 * <p>
 * El campo {@code subtotal} se calcula como
 * {@code cantidad * precioUnitario} y se almacena en la base de datos
 * para evitar recalcularlo en cada consulta.
 * </p>
 *
 * <p>
 * PedidoDetalle no declara {@code CascadeType} en ninguna relación.
 * Cada entidad mantiene su propio ciclo de vida.
 * </p>
 */
@Entity
@Table(name = "pedido_detalle")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PedidoDetalle {

    /**
     * Identificador único de la línea de detalle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Pedido al que pertenece esta línea de detalle.
     *
     * <p>
     * PedidoDetalle es el dueño de la relación: el FK
     * {@code pedido_id} vive en la tabla {@code pedido_detalle}.
     * Toda línea de detalle debe tener un pedido asociado.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar la cabecera del
     * pedido cuando solo se necesita información de la línea.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /**
     * Producto solicitado en esta línea de detalle.
     *
     * <p>
     * PedidoDetalle es el dueño de la relación: el FK
     * {@code producto_id} vive en la tabla {@code pedido_detalle}.
     * Toda línea de detalle debe tener un producto asociado.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos del
     * producto cuando solo se necesita información de la línea.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoInventario producto;

    /**
     * Cantidad de unidades del producto solicitadas en esta línea.
     *
     * <p>
     * El servicio valida que el stock del producto sea suficiente
     * ({@code producto.stock >= cantidad}) antes de persistir el
     * detalle. Si no hay stock suficiente, lanza una
     * {@code BusinessRuleException}.
     * </p>
     */
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /**
     * Precio unitario del producto en el momento de realizar el pedido.
     *
     * <p>
     * Es un <b>snapshot</b> del precio del producto
     * ({@link ProductoInventario#getPrecioVenta()} ()}) en el instante en que
     * se creó el pedido. Aunque el precio del producto cambie en el
     * futuro, este detalle conservará el precio original.
     * </p>
     *
     * <p>
     * {@code precision = 10, scale = 2} permite almacenar importes
     * de hasta 99.999.999,99.
     * </p>
     */
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * Subtotal de la línea, calculado como
     * {@code cantidad * precioUnitario}.
     *
     * <p>
     * Se calcula y almacena en el momento de la creación del detalle
     * para evitar recalcularlo en cada consulta. El servicio es
     * responsable de calcularlo antes de persistir.
     * </p>
     *
     * <p>
     * {@code precision = 10, scale = 2} permite almacenar importes
     * de hasta 99.999.999,99.
     * </p>
     */
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Indica si la línea de detalle se encuentra activa.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Las líneas
     * desactivadas permanecen en la base de datos para auditoría.
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
