package com.technovald.enums;

/**
 * Enumeración que define los estados del ciclo de vida de un pedido en TechNovaLD
 * <p>El flujo de estados es estrictamente lineal y secuencial:</p>
 * <pre>
 * PENDIENTE -> PROCESANDO -> ENVIADO -> ENTREGADO
 * </pre>
 *
 * <p>
 * Este enum se persiste como texto literal en MySQL mediante
 * {@code @Enumerated(EnumType.STRING)}, lo que permite consultar
 * pedidos por estado de forma legible directamente desde SQL.
 * </p>
 */
public enum EstadoPedido {
    /**
     * Estado inicial del pedido.
     * El cliente ha realizado un pedido, pero aún no ha sido procesado. El pedido está a la
     * espera de que el sistema de pagos o un trabajador validen la transacción.
     */
    PENDIENTE,
    /**
     * El pago ha sido confirmado y el pedido está siendo preparado.
     * En este estado, el cliente ya ha pagado, pero todavía no ha recibido el pedido. Un trabajador está
     * empaquetando los productos y preparando el envío.
     * <p>
     * Este estado cubre la duda común: "el pedido está pagado pero no entregado".
     * </p>
     */
    PROCESANDO,
    /**
     * <p>El pedido ha sido enviado y está en tránsito.</p>
     * <p>Los productos han salido del almacén y están en camino al cliente.</p>
     */
    ENVIADO,
    /**
     * <p>El pedido ha sido entregado al cliente.</p>
     * <p>
     * A partir de este estado el pedido no puede cambiar. El cliente
     * puede valorar los productos y dejar comentarios en el catálogo.
     * </p>
     */
    ENTREGADO
}
