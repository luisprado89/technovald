package com.technovald.service.impl;

import com.technovald.dto.request.ProductoInventarioAjusteRequest;
import com.technovald.dto.request.ProductoInventarioRequest;
import com.technovald.dto.response.ProductoInventarioResponse;
import com.technovald.entity.Categoria;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Proveedor;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.ProductoInventarioMapper;
import com.technovald.repository.mysql.CategoriaRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.repository.mysql.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock private ProductoInventarioRepository productoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private ProductoInventarioMapper productoMapper;

    @InjectMocks private InventarioServiceImpl inventarioService;

    private Categoria categoria;
    private Proveedor proveedor;
    private ProductoInventario producto;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder().id(1L).nombre("Redes").build();
        proveedor = Proveedor.builder().id(2L).nombreEmpresa("Suministros Vigo").build();
        producto = ProductoInventario.builder()
                .id(10L)
                .ean("123")
                .nombreCorto("Teclado Mod")
                .stockActual(10)
                .stockMinimo(2)
                .activo(true)
                .build();
    }

    private ProductoInventarioRequest request() {
        return ProductoInventarioRequest.builder()
                .ean(" 123 ")
                .nombreCorto("Teclado Mod")
                .categoriaId(1L)
                .proveedorId(2L)
                .build();
    }

    @Test
    void creaProductoConEanUnico() {
        ProductoInventarioRequest request = request();
        ProductoInventarioResponse respuesta = ProductoInventarioResponse.builder().id(10L).build();
        when(productoMapper.normalizarEan(" 123 ")).thenReturn("123");
        when(productoRepository.existsByEan("123")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(proveedorRepository.findById(2L)).thenReturn(Optional.of(proveedor));
        when(productoMapper.toEntity(request, categoria, proveedor)).thenReturn(producto);
        when(productoMapper.toResponse(producto)).thenReturn(respuesta);

        ProductoInventarioResponse resultado = inventarioService.crearProducto(request);

        assertSame(respuesta, resultado);
        verify(productoRepository).save(producto);
    }

    @Test
    void rechazaEanDuplicado() {
        when(productoMapper.normalizarEan(" 123 ")).thenReturn("123");
        when(productoRepository.existsByEan("123")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> inventarioService.crearProducto(request()));

        assertTrue(ex.getMessage().contains("EAN"));
        verify(productoRepository, never()).save(any(ProductoInventario.class));
        verifyNoInteractions(categoriaRepository, proveedorRepository);
    }

    @Test
    void rechazaProductoSiLaCategoriaNoExiste() {
        when(productoMapper.normalizarEan(" 123 ")).thenReturn("123");
        when(productoRepository.existsByEan("123")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventarioService.crearProducto(request()));

        verify(productoRepository, never()).save(any(ProductoInventario.class));
        verifyNoInteractions(proveedorRepository);
    }

    @Test
    void sumaStockSinBajarDeCero() {
        ProductoInventarioResponse respuesta = ProductoInventarioResponse.builder().id(10L).build();
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoMapper.toResponse(producto)).thenReturn(respuesta);

        ProductoInventarioResponse resultado = inventarioService.ajustarStockProducto(
                10L, ProductoInventarioAjusteRequest.builder().cantidad(5).build());

        assertSame(respuesta, resultado);
        assertEquals(15, producto.getStockActual());
        verify(productoRepository).save(producto);
    }

    @Test
    void rechazaAjusteQueDejariaStockNegativo() {
        producto.setStockActual(2);
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> inventarioService.ajustarStockProducto(
                        10L, ProductoInventarioAjusteRequest.builder().cantidad(-5).build()));

        assertTrue(ex.getMessage().contains("negativo"));
        verify(productoRepository, never()).save(any(ProductoInventario.class));
    }

    @Test
    void listaStockBajo() {
        ProductoInventarioResponse respuesta = ProductoInventarioResponse.builder().id(10L).build();
        when(productoRepository.findLowStock()).thenReturn(List.of(producto));
        when(productoMapper.toResponseList(anyList())).thenReturn(List.of(respuesta));

        List<ProductoInventarioResponse> resultado = inventarioService.listarStockBajo();

        assertEquals(1, resultado.size());
        assertSame(respuesta, resultado.get(0));
    }

    @Test
    void desactivaProductoSinBorrarlo() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        inventarioService.desactivarProducto(10L);

        assertFalse(producto.getActivo());
        verify(productoRepository).save(producto);
    }

    @Test
    void reactivaProducto() {
        producto.setActivo(false);
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        inventarioService.reactivarProducto(10L);

        assertTrue(producto.getActivo());
        verify(productoRepository).save(producto);
    }

    @Test
    void rechazaProductoInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventarioService.desactivarProducto(99L));

        verify(productoRepository, never()).save(any(ProductoInventario.class));
    }
}
