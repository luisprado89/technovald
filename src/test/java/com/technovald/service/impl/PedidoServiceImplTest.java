package com.technovald.service.impl;

import com.technovald.dto.request.DetallePedidoRequest;
import com.technovald.dto.request.PedidoRequest;
import com.technovald.dto.response.PedidoResponse;
import com.technovald.entity.Cliente;
import com.technovald.entity.Pedido;
import com.technovald.entity.PedidoDetalle;
import com.technovald.entity.Persona;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PedidoDetalleRepository pedidoDetalleRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProductoInventarioRepository productoRepository;
    @Mock private PedidoMapper pedidoMapper;

    @InjectMocks private PedidoServiceImpl pedidoService;

    private Persona persona;
    private Usuario usuario;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        persona = Persona.builder().id(1L).nombre("Ana").apellido("García").build();
        usuario = Usuario.builder().username("ana").persona(persona).build();
        cliente = Cliente.builder().id(2L).persona(persona).build();
    }

    private PedidoRequest request(long productoId, int cantidad) {
        return PedidoRequest.builder()
                .detalles(List.of(DetallePedidoRequest.builder()
                        .productoId(productoId)
                        .cantidad(cantidad)
                        .build()))
                .build();
    }

    private ProductoInventario producto() {
        return ProductoInventario.builder()
                .id(10L)
                .ean("EAN-10")
                .nombreCorto("Teclado Mod")
                .precioVenta(new BigDecimal("100.00"))
                .stockActual(10)
                .build();
    }

    @Test
    void creaPedidoConTotalYDescuentoDeStock() {
        ProductoInventario producto = producto();
        PedidoResponse respuesta = PedidoResponse.builder().id(99L).build();

        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByPersonaIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(producto));
        when(productoRepository.decrementarStockAtomico(10L, 2)).thenReturn(1);
        when(pedidoMapper.toResponse(any(Pedido.class))).thenReturn(respuesta);

        PedidoResponse resultado = pedidoService.crearPedido(request(10L, 2), "ana");

        assertSame(respuesta, resultado);
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository, times(2)).save(captor.capture());
        Pedido persistido = captor.getAllValues().get(1);
        assertEquals(new BigDecimal("200.00"), persistido.getTotal());
        assertEquals(1, persistido.getPedidoDetalles().size());
        assertEquals(EstadoPedido.PENDIENTE, persistido.getEstado());
        verify(pedidoDetalleRepository).save(any(PedidoDetalle.class));
        verify(productoRepository).decrementarStockAtomico(10L, 2);
    }

    @Test
    void rechazaPedidoSinStockSuficiente() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByPersonaIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(producto()));
        when(productoRepository.decrementarStockAtomico(10L, 2)).thenReturn(0);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> pedidoService.crearPedido(request(10L, 2), "ana"));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(pedidoDetalleRepository, never()).save(any(PedidoDetalle.class));
    }

    @Test
    void rechazaProductoInexistente() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByPersonaIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> pedidoService.crearPedido(request(10L, 1), "ana"));

        verify(productoRepository, never()).decrementarStockAtomico(anyLong(), anyInt());
    }

    @Test
    void rechazaUsuarioSinPerfilDeCliente() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByPersonaIdAndActivoTrue(1L)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> pedidoService.crearPedido(request(10L, 1), "ana"));

        assertTrue(ex.getMessage().contains("perfil de cliente"));
        verify(productoRepository, never()).findByIdAndActivoTrue(anyLong());
    }

    @Test
    void avanzaEstadoEnElOrdenPermitido() {
        Pedido pedido = Pedido.builder().id(5L).estado(EstadoPedido.PENDIENTE).build();
        PedidoResponse respuesta = PedidoResponse.builder().id(5L).estado(EstadoPedido.PROCESANDO).build();

        when(pedidoRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(pedido));
        when(pedidoMapper.toResponse(pedido)).thenReturn(respuesta);

        PedidoResponse resultado = pedidoService.updateEstadoPedido(5L, EstadoPedido.PROCESANDO);

        assertSame(respuesta, resultado);
        assertEquals(EstadoPedido.PROCESANDO, pedido.getEstado());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void rechazaSaltoDeEstado() {
        Pedido pedido = Pedido.builder().id(5L).estado(EstadoPedido.PENDIENTE).build();
        when(pedidoRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(pedido));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> pedidoService.updateEstadoPedido(5L, EstadoPedido.ENVIADO));

        assertTrue(ex.getMessage().contains("Transición de estado no permitida"));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void rechazaReaperturaDePedidoEntregado() {
        Pedido pedido = Pedido.builder().id(5L).estado(EstadoPedido.ENTREGADO).build();
        when(pedidoRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(pedido));

        assertThrows(BusinessRuleException.class,
                () -> pedidoService.updateEstadoPedido(5L, EstadoPedido.PROCESANDO));

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }
}
