package com.technovald.service.impl;

import com.technovald.document.ProductoDocumento;
import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.ProductoCatalogoResumenResponse;
import com.technovald.entity.ProductoInventario;
import com.technovald.exception.ResourceNotFoundException;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    @Mock private ProductoMongoRepository productoMongoRepository;
    @Mock private ProductoInventarioRepository productoInventarioRepository;
    @Mock private ProductoCatalogoMapper productoCatalogoMapper;

    @InjectMocks private CatalogoServiceImpl catalogoService;

    private ProductoDocumento documento;
    private ProductoInventario inventario;

    @BeforeEach
    void setUp() {
        documento = ProductoDocumento.builder()
                .id("doc-1")
                .ean("EAN1")
                .titulo("Teclado Mod")
                .nombreCorto("Teclado Mod")
                .marca("Marca")
                .activo(true)
                .build();
        inventario = ProductoInventario.builder()
                .id(10L)
                .ean("EAN1")
                .nombreCorto("Teclado Mod")
                .stockActual(5)
                .build();
    }

    @Test
    void listaFichasActivasConPrecioYStock() {
        ProductoCatalogoResumenResponse resumen = ProductoCatalogoResumenResponse.builder()
                .id("doc-1").nombreCorto("Teclado Mod").stockActual(5).build();
        when(productoMongoRepository.findAllByActivoTrueOrderByNombreCortoAsc())
                .thenReturn(List.of(documento));
        when(productoInventarioRepository.findAllByEanIn(List.of("EAN1")))
                .thenReturn(List.of(inventario));
        when(productoCatalogoMapper.toResumenList(List.of(documento), Map.of("EAN1", inventario)))
                .thenReturn(List.of(resumen));

        List<ProductoCatalogoResumenResponse> resultado = catalogoService.listarProductosCatalogo();

        assertEquals(1, resultado.size());
        assertSame(resumen, resultado.get(0));
    }

    @Test
    void obtieneDetallePorEanNormalizado() {
        ProductoCatalogoDetalleResponse detalle = ProductoCatalogoDetalleResponse.builder()
                .id("doc-1").ean("EAN1").build();
        when(productoMongoRepository.findByEanAndActivoTrue("EAN1")).thenReturn(Optional.of(documento));
        when(productoInventarioRepository.findByEanIgnoreCase("EAN1")).thenReturn(Optional.of(inventario));
        when(productoCatalogoMapper.toDetalle(documento, inventario)).thenReturn(detalle);

        ProductoCatalogoDetalleResponse resultado = catalogoService.obtenerProductoCatalogoPorEan(" ean1 ");

        assertSame(detalle, resultado);
    }

    @Test
    void rechazaDetalleDeEanInexistente() {
        when(productoMongoRepository.findByEanAndActivoTrue("NO-EXISTE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> catalogoService.obtenerProductoCatalogoPorEan(" no-existe "));

        verifyNoInteractions(productoCatalogoMapper);
    }

    @Test
    void guardaFichaExistenteConservandoDatos() {
        ProductoCatalogoRequest request = ProductoCatalogoRequest.builder()
                .ean(" ean1 ").titulo("Teclado Mod").build();
        ProductoCatalogoDetalleResponse detalle = ProductoCatalogoDetalleResponse.builder()
                .id("doc-1").ean("EAN1").build();
        when(productoMongoRepository.findByEan("EAN1")).thenReturn(Optional.of(documento));
        when(productoCatalogoMapper.toDocumento(request, documento)).thenReturn(documento);
        when(productoMongoRepository.save(documento)).thenReturn(documento);
        when(productoInventarioRepository.findByEanIgnoreCase("EAN1")).thenReturn(Optional.of(inventario));
        when(productoCatalogoMapper.toDetalle(documento, inventario)).thenReturn(detalle);

        ProductoCatalogoDetalleResponse resultado = catalogoService.guardarProductoCatalogo(request);

        assertSame(detalle, resultado);
        verify(productoMongoRepository).save(documento);
    }
}
