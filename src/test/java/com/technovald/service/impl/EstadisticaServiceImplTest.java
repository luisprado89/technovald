package com.technovald.service.impl;

import com.technovald.dto.response.ProductoMasVendidoResponse;
import com.technovald.repository.mysql.PedidoDetalleRepository;
import com.technovald.repository.mysql.ProductoMasVendidoProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticaServiceImplTest {

    @Mock private PedidoDetalleRepository pedidoDetalleRepository;

    @InjectMocks private EstadisticaServiceImpl estadisticaService;

    private ProductoMasVendidoProjection proyeccion(String nombre, String marca, long cantidad) {
        ProductoMasVendidoProjection p = mock(ProductoMasVendidoProjection.class);
        when(p.getNombreCorto()).thenReturn(nombre);
        when(p.getMarca()).thenReturn(marca);
        when(p.getCantidadTotal()).thenReturn(cantidad);
        return p;
    }

    @Test
    void devuelveSoloLosCincoPrimerosYConservaElOrden() {
        ProductoMasVendidoProjection p1 = proyeccion("P1", "Marca A", 6);
        ProductoMasVendidoProjection p2 = proyeccion("P2", "Marca A", 5);
        ProductoMasVendidoProjection p3 = proyeccion("P3", "Marca B", 4);
        ProductoMasVendidoProjection p4 = proyeccion("P4", "Marca B", 3);
        ProductoMasVendidoProjection p5 = proyeccion("P5", "Marca C", 2);
        ProductoMasVendidoProjection p6 = mock(ProductoMasVendidoProjection.class);
        lenient().when(p6.getNombreCorto()).thenReturn("P6");
        lenient().when(p6.getMarca()).thenReturn("Marca C");
        lenient().when(p6.getCantidadTotal()).thenReturn(1L);

        when(pedidoDetalleRepository.findTopProductosMasVendidos())
                .thenReturn(List.of(p1, p2, p3, p4, p5, p6));

        List<ProductoMasVendidoResponse> resultado = estadisticaService.getProductosMasVendidos();

        assertEquals(5, resultado.size());
        assertEquals("P1", resultado.get(0).getNombreProducto());
        assertEquals("Marca A", resultado.get(0).getMarca());
        assertEquals(6L, resultado.get(0).getCantidadTotalVendida());
        assertEquals("P5", resultado.get(4).getNombreProducto());
        assertEquals(2L, resultado.get(4).getCantidadTotalVendida());
    }

    @Test
    void devuelveListaVaciaSinVentas() {
        when(pedidoDetalleRepository.findTopProductosMasVendidos()).thenReturn(List.of());

        List<ProductoMasVendidoResponse> resultado = estadisticaService.getProductosMasVendidos();

        assertTrue(resultado.isEmpty());
    }
}
