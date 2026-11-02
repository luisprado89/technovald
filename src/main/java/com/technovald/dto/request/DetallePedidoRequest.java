package com.technovald.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * Línea de un pedido: producto y cantidad. El precio no se envía; lo pone el
 * servidor con el precio de venta actual del inventario.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DetallePedidoRequest {

    /** Id del producto que se quiere comprar. */
    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    /** Unidades solicitadas; debe ser mayor que cero. */
    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor que 0")
    private Integer cantidad;
}