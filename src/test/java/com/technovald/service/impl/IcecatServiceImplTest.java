package com.technovald.service.impl;

import com.technovald.client.IcecatClient;
import com.technovald.document.ProductoDocumento;
import com.technovald.dto.icecat.IcecatResponse;
import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.SincronizacionResponse;
import com.technovald.entity.ProductoInventario;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.IcecatMapper;
import com.technovald.mapper.ProductoCatalogoMapper;
import com.technovald.repository.mongo.ProductoMongoRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IcecatServiceImplTest {

    @Mock private IcecatClient icecatClient;
    @Mock private IcecatMapper icecatMapper;
    @Mock private ProductoMongoRepository productoMongoRepository;
    @Mock private ProductoInventarioRepository productoInventarioRepository;
    @Mock private ProductoCatalogoMapper productoCatalogoMapper;

    @InjectMocks private IcecatServiceImpl icecatService;

    private ProductoInventario inventario;
    private ProductoDocumento documento;

    @BeforeEach
    void setUp() {
        inventario = ProductoInventario.builder()
                .id(10L).ean("EAN1").nombreCorto("Teclado Mod").build();
        documento = ProductoDocumento.builder()
                .id("doc-1").ean("EAN1").titulo("Teclado Mod").activo(true).build();
    }

    @Test
    void sincronizaUnaFichaDesdeIcecat() {
        IcecatResponse respuesta = mock(IcecatResponse.class);
        ProductoCatalogoDetalleResponse detalle = ProductoCatalogoDetalleResponse.builder()
                .id("doc-1").ean("EAN1").build();
        when(productoInventarioRepository.findByEan("EAN1")).thenReturn(Optional.of(inventario));
        when(icecatClient.obtenerProducto("EAN1")).thenReturn(respuesta);
        when(productoMongoRepository.findByEan("EAN1")).thenReturn(Optional.empty());
        when(icecatMapper.toDocumento(respuesta, "EAN1", null)).thenReturn(documento);
        when(productoMongoRepository.save(documento)).thenReturn(documento);
        when(productoCatalogoMapper.toDetalle(documento, inventario)).thenReturn(detalle);

        ProductoCatalogoDetalleResponse resultado = icecatService.sincronizarProducto(" ean1 ");

        assertSame(detalle, resultado);
        verify(productoMongoRepository).save(documento);
    }

    @Test
    void rechazaSincronizacionSiElEanNoEstaEnInventario() {
        when(productoInventarioRepository.findByEan("NO-EXISTE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> icecatService.sincronizarProducto(" no-existe "));

        verifyNoInteractions(icecatClient, productoMongoRepository, icecatMapper);
    }

    @Test
    void laSincronizacionMasivaCuentaFallosSinDetenerse() {
        ProductoInventario segundo = ProductoInventario.builder()
                .id(11L).ean("EAN2").nombreCorto("Monitor").build();
        IcecatResponse respuesta = mock(IcecatResponse.class);
        ProductoCatalogoDetalleResponse detalle = ProductoCatalogoDetalleResponse.builder()
                .id("doc-1").ean("EAN1").build();

        when(productoInventarioRepository.findAllByOrderByNombreCortoAsc())
                .thenReturn(List.of(inventario, segundo));
        when(productoInventarioRepository.findByEan("EAN1")).thenReturn(Optional.of(inventario));
        when(productoInventarioRepository.findByEan("EAN2")).thenReturn(Optional.of(segundo));
        when(icecatClient.obtenerProducto("EAN1")).thenReturn(respuesta);
        when(icecatClient.obtenerProducto("EAN2")).thenThrow(new RuntimeException("Icecat no responde"));
        when(productoMongoRepository.findByEan("EAN1")).thenReturn(Optional.empty());
        when(icecatMapper.toDocumento(respuesta, "EAN1", null)).thenReturn(documento);
        when(productoMongoRepository.save(documento)).thenReturn(documento);
        when(productoCatalogoMapper.toDetalle(documento, inventario)).thenReturn(detalle);

        SincronizacionResponse resultado = icecatService.sincronizarInventario();

        assertEquals(2, resultado.total());
        assertEquals(1, resultado.sincronizados());
        assertEquals(1, resultado.fallidos());
        assertEquals(1, resultado.errores().size());
    }
}
