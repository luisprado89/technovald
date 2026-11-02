package com.technovald.dto.response;

import lombok.*;

import java.math.BigDecimal;

/**
 * Línea de pedido tal y como se devuelve al cliente o al staff. El nombre y
 * la marca se copian del producto para no obligar a más consultas.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DetallePedidoResponse {

    /** Id de la línea. */
    private Long id;

    /** Nombre corto del producto comprado. */
    private String nombreProducto;

    /** Marca del producto comprado. */
    private String marcaProducto;

    /** Unidades de esta línea. */
    private Integer cantidad;

    /** Precio congelado en el momento de la compra. */
    private BigDecimal precioUnitario;

    /** Cantidad por precio unitario. */
    private BigDecimal subtotal;
}