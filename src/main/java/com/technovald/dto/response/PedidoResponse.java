package com.technovald.dto.response;

import com.technovald.enums.EstadoPedido;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pedido completo: cabecera y líneas. Se usa en el historial del cliente y en
 * el listado del staff.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PedidoResponse {

    /** Id del pedido. */
    private Long id;

    /** Fecha de creación. */
    private LocalDateTime fechaPedido;

    /** Estado actual dentro del ciclo de vida. */
    private EstadoPedido estado;

    /** Importe total con los precios congelados. */
    private BigDecimal total;

    /** Nombre del cliente que hizo el pedido. */
    private String nombreCliente;

    /** Líneas del pedido. */
    private List<DetallePedidoResponse> detalles;
}