package com.technovald.mapper;

import com.technovald.dto.response.PedidoResponse;
import com.technovald.entity.Pedido;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Convierte pedidos en DTOs. Debe ejecutarse dentro de la transacción del
 * servicio: el cliente y las líneas son relaciones LAZY.
 */
@Component
@RequiredArgsConstructor
public class PedidoMapper {

    private final PedidoDetalleMapper pedidoDetalleMapper;

    /**
     * Convierte un pedido y sus líneas.
     *
     * @param pedido pedido con cliente y líneas accesibles.
     * @return el DTO completo.
     */
    public PedidoResponse toResponse(Pedido pedido) {
        return PedidoResponse.builder()
                .id(pedido.getId())
                .fechaPedido(pedido.getFechaPedido())
                .estado(pedido.getEstado())
                .total(pedido.getTotal())
                .nombreCliente(pedido.getCliente().getPersona().getNombre())
                .detalles(pedidoDetalleMapper.toResponseList(pedido.getPedidoDetalles()))
                .build();
    }

    /**
     * Convierte una página de pedidos conservando la paginación.
     *
     * @param pedidos página de entidades.
     * @return página de DTOs.
     */
    public Page<PedidoResponse> toResponsePage(Page<Pedido> pedidos) {
        return pedidos.map(this::toResponse);
    }
}