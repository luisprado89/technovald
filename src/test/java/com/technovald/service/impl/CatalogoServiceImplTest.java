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
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    @Mock private ProductoMongoRepository productoMongoRepository;
    @Mock private ProductoInventarioRepository productoInventarioRepository;
    @Mock private ProductoCatalogoMapper productoCatalogoMapper;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoConverter mongoConverter;

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
        when(productoInventarioRepository.findByEan("EAN1")).thenReturn(Optional.of(inventario));
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
        when(productoInventarioRepository.findByEan("EAN1")).thenReturn(Optional.of(inventario));
        when(productoCatalogoMapper.toDetalle(documento, inventario)).thenReturn(detalle);
        when(mongoTemplate.getConverter()).thenReturn(mongoConverter);

        // Simula el converter real: escribe _id, un campo normal y comentarios.
        doAnswer(invocation -> {
            Document campos = invocation.getArgument(1);
            campos.put("_id", "doc-1");
            campos.put("titulo", "Teclado Mod");
            campos.put("comentarios", List.of());
            return null;
        }).when(mongoConverter).write(any(), any());

        ProductoCatalogoDetalleResponse resultado = catalogoService.guardarProductoCatalogo(request);

        assertSame(detalle, resultado);

        ArgumentCaptor<UpdateDefinition> captor = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).upsert(any(Query.class), captor.capture(), eq(ProductoDocumento.class));

        Document set = (Document) ((Update) captor.getValue()).getUpdateObject().get("$set");
        assertEquals("Teclado Mod", set.get("titulo"));
        assertFalse(set.containsKey("_id"), "el _id no puede formar parte del $set");
        assertFalse(set.containsKey("comentarios"),
                "los comentarios no deben reescribirse: se perdería un comentario concurrente");

        // La escritura ya no es un save() del documento completo.
        verify(productoMongoRepository, never()).save(any(ProductoDocumento.class));
    }
}
