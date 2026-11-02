package com.technovald.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/pedidos}. Solo trae las líneas: el cliente sale
 * del token, y los precios y el total los calcula el servicio.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PedidoRequest {

    /** Líneas del pedido; el máximo acota el trabajo de una sola petición. */
    @NotEmpty(message = "El pedido debe tener al menos un detalle")
    @Size(max = 100, message = "El pedido no puede tener más de 100 líneas")
    @Valid
    private List<DetallePedidoRequest> detalles;
}