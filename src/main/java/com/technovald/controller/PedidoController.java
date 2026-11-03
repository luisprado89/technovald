package com.technovald.controller;

import com.technovald.dto.request.PedidoRequest;
import com.technovald.dto.response.PaginaResponse;
import com.technovald.dto.response.PedidoResponse;
import com.technovald.enums.EstadoPedido;
import com.technovald.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pedidos. El cliente compra y consulta su historial; el staff lista todos y
 * avanza el estado. Los permisos se aplican por URL en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    /**
     * Crea un pedido con las líneas del carrito.
     *
     * @param pedidoRequest     líneas del pedido.
     * @param userDetails cliente autenticado.
     * @return el pedido creado (201).
     */
    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(
            @Valid @RequestBody PedidoRequest pedidoRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoService.crearPedido(pedidoRequest, userDetails.getUsername()));
    }

    /**
     * Historial de pedidos del cliente autenticado.
     *
     * @param userDetails cliente autenticado.
     * @param pageable    página y tamaño.
     * @return la página de pedidos.
     */
    @GetMapping("/me")
    public ResponseEntity<PaginaResponse<PedidoResponse>> getMisPedidos(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(pedidoService.getMisPedidos(userDetails.getUsername(), pageable));
    }

    /**
     * Cambia el estado de un pedido.
     *
     * @param id          id del pedido.
     * @param nuevoEstado estado de destino.
     * @return el pedido actualizado.
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido nuevoEstado) {

        return ResponseEntity.ok(pedidoService.updateEstadoPedido(id, nuevoEstado));
    }

    /**
     * Listado de todos los pedidos activos, para el staff.
     *
     * @param pageable página y tamaño.
     * @return la página de pedidos.
     */
    @GetMapping
    public ResponseEntity<PaginaResponse<PedidoResponse>> listarPedidos(
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(pedidoService.getAllPedidos(pageable));
    }
}