package com.technovald.service.impl;

import com.technovald.dto.request.DetallePedidoRequest;
import com.technovald.dto.request.PedidoRequest;
import com.technovald.dto.response.PaginaResponse;
import com.technovald.dto.response.PedidoResponse;
import com.technovald.entity.Cliente;
import com.technovald.entity.Pedido;
import com.technovald.entity.PedidoDetalle;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Usuario;
import com.technovald.enums.EstadoPedido;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.PedidoMapper;
import com.technovald.repository.mysql.ClienteRepository;
import com.technovald.repository.mysql.PedidoDetalleRepository;
import com.technovald.repository.mysql.PedidoRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.service.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Implementación de {@link PedidoService}. El stock se descuenta con un
 * UPDATE atómico: comprobar y restar ocurren en la misma sentencia, así que
 * dos compras concurrentes no pueden sobrevender.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository pedidoDetalleRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoInventarioRepository productoRepository;
    private final PedidoMapper pedidoMapper;

    /** Ciclo de vida lineal; ENTREGADO es terminal. */
    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            EstadoPedido.PENDIENTE, Set.of(EstadoPedido.PROCESANDO),
            EstadoPedido.PROCESANDO, Set.of(EstadoPedido.ENVIADO),
            EstadoPedido.ENVIADO, Set.of(EstadoPedido.ENTREGADO),
            EstadoPedido.ENTREGADO, Set.of()
    );

    /**
     * Crea el pedido, congela el precio de cada línea y descuenta stock.
     *
     * @param pedidoRequest  líneas solicitadas.
     * @param username username del cliente autenticado.
     * @return el pedido creado.
     * @throws ResourceNotFoundException si el usuario o un producto no existen.
     * @throws BusinessRuleException     si no hay stock o el usuario no es cliente.
     */
    @Override
    @Transactional
    public PedidoResponse crearPedido(PedidoRequest pedidoRequest, String username) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findByPersonaIdAndActivoTrue(usuario.getPersona().getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "El usuario no tiene perfil de cliente para comprar."));

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .total(BigDecimal.ZERO)
                .build();
        pedidoRepository.save(pedido);

        BigDecimal total = BigDecimal.ZERO;

        for (DetallePedidoRequest detallePedidoRequest : pedidoRequest.getDetalles()) {
            ProductoInventario producto = productoRepository.findByIdAndActivoTrue(detallePedidoRequest.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + detallePedidoRequest.getProductoId()));

            int filas = productoRepository.decrementarStockAtomico(
                    producto.getId(), detallePedidoRequest.getCantidad());

            if (filas == 0) {
                throw new BusinessRuleException(
                        "Stock insuficiente para el producto: " + producto.getNombreCorto());
            }

            BigDecimal precioUnitario = producto.getPrecioVenta();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(detallePedidoRequest.getCantidad()));
            total = total.add(subtotal);

            PedidoDetalle detalle = PedidoDetalle.builder()
                    .pedido(pedido)
                    .producto(producto)
                    .cantidad(detallePedidoRequest.getCantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotal)
                    .build();

            pedidoDetalleRepository.save(detalle);
            pedido.getPedidoDetalles().add(detalle);
        }

        pedido.setTotal(total);
        pedidoRepository.save(pedido);

        log.info("Pedido {} creado por '{}' por {} €", pedido.getId(), username, total);

        return pedidoMapper.toResponse(pedido);
    }

    /**
     * Historial del cliente autenticado, del más reciente al más antiguo.
     *
     * @param username username del cliente.
     * @param pageable página y tamaño.
     * @return la página de pedidos.
     * @throws ResourceNotFoundException si el usuario no existe.
     * @throws BusinessRuleException     si no tiene perfil de cliente.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<PedidoResponse> getMisPedidos(String username, Pageable pageable) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findByPersonaIdAndActivoTrue(usuario.getPersona().getId())
                .orElseThrow(() -> new BusinessRuleException("El usuario no tiene perfil de cliente."));

        Page<Pedido> pagina = pedidoRepository
                .findAllByClienteIdAndActivoTrueOrderByIdDesc(cliente.getId(), pageable);

        return PaginaResponse.de(pedidoMapper.toResponsePage(pagina));
    }

    /**
     * Avanza el pedido al estado indicado. Solo se permiten los pasos del
     * ciclo lineal; retroceder, saltar o repetir estado devuelve 400.
     *
     * @param id          id del pedido.
     * @param nuevoEstado estado de destino.
     * @return el pedido actualizado.
     * @throws ResourceNotFoundException si el pedido no existe o está inactivo.
     * @throws BusinessRuleException     si la transición no está permitida.
     */
    @Override
    @Transactional
    public PedidoResponse updateEstadoPedido(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));

        EstadoPedido actual = pedido.getEstado();

        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevoEstado)) {
            throw new BusinessRuleException("Transición de estado no permitida: " + actual
                    + " -> " + nuevoEstado + ". Desde " + actual + " solo se permite: "
                    + TRANSICIONES.getOrDefault(actual, Set.of()));
        }

        pedido.setEstado(nuevoEstado);
        pedidoRepository.save(pedido);

        log.info("Pedido {} actualizado al estado {}", id, nuevoEstado);

        return pedidoMapper.toResponse(pedido);
    }

    /**
     * Listado de pedidos activos para el staff.
     *
     * @param pageable página y tamaño.
     * @return la página de pedidos.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<PedidoResponse> getAllPedidos(Pageable pageable) {
        Page<Pedido> pagina = pedidoRepository.findAllByActivoTrueOrderByIdDesc(pageable);
        return PaginaResponse.de(pedidoMapper.toResponsePage(pagina));
    }
}