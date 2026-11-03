package com.technovald.service;

import com.technovald.dto.request.PedidoRequest;
import com.technovald.dto.response.PaginaResponse;
import com.technovald.dto.response.PedidoResponse;
import com.technovald.enums.EstadoPedido;
import org.springframework.data.domain.Pageable;

/** Gestión de pedidos: creación, historial del cliente y gestión del staff. */
public interface PedidoService {

    /**
     * Crea un pedido para el cliente autenticado, validando y descontando stock.
     *
     * @param pedidoRequest  líneas del pedido.
     * @param username username del cliente autenticado.
     * @return el pedido creado, con precios congelados y total calculado.
     */
    PedidoResponse crearPedido(PedidoRequest pedidoRequest, String username);

    /**
     * Historial del cliente autenticado, paginado.
     *
     * @param username username del cliente.
     * @param pageable página y tamaño.
     * @return la página de pedidos del cliente.
     */
    PaginaResponse<PedidoResponse> getMisPedidos(String username, Pageable pageable);

    /**
     * Cambia el estado de un pedido respetando el ciclo lineal.
     *
     * @param id          id del pedido.
     * @param nuevoEstado estado de destino.
     * @return el pedido actualizado.
     */
    PedidoResponse updateEstadoPedido(Long id, EstadoPedido nuevoEstado);

    /**
     * Listado de todos los pedidos activos, para ADMIN y TRABAJADOR.
     *
     * @param pageable página y tamaño.
     * @return la página de pedidos.
     */
    PaginaResponse<PedidoResponse> getAllPedidos(Pageable pageable);
}