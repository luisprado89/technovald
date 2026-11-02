package com.technovald.mapper;

import com.technovald.dto.response.DetallePedidoResponse;
import com.technovald.entity.PedidoDetalle;
import org.springframework.stereotype.Component;

import java.util.List;

/** Convierte líneas de pedido en DTOs de respuesta. */
@Component
public class PedidoDetalleMapper {

    /**
     * Convierte una línea de pedido.
     *
     * @param detalle línea con su producto cargado.
     * @return el DTO con nombre y marca ya resueltos.
     */
    public DetallePedidoResponse toResponse(PedidoDetalle detalle) {
        return DetallePedidoResponse.builder()
                .id(detalle.getId())
                .nombreProducto(detalle.getProducto().getNombreCorto())
                .marcaProducto(detalle.getProducto().getMarca())
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(detalle.getSubtotal())
                .build();
    }

    /**
     * Convierte todas las líneas de un pedido.
     *
     * @param detalles líneas del pedido.
     * @return la lista de DTOs, en el mismo orden.
     */
    public List<DetallePedidoResponse> toResponseList(List<PedidoDetalle> detalles) {
        return detalles.stream()
                .map(this::toResponse)
                .toList();
    }
}