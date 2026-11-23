package com.technovald.service.impl;

import com.technovald.dto.request.CategoriaRequest;
import com.technovald.dto.response.CategoriaResponse;
import com.technovald.entity.Categoria;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.CategoriaMapper;
import com.technovald.repository.mysql.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private CategoriaMapper categoriaMapper;

    @InjectMocks private CategoriaServiceImpl categoriaService;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder()
                .id(1L)
                .nombre("Redes")
                .descripcion("Infraestructura")
                .activo(true)
                .build();
    }

    @Test
    void listaCategoriasOrdenadas() {
        CategoriaResponse respuesta = CategoriaResponse.builder().id(1L).nombre("Redes").build();
        when(categoriaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(categoria));
        when(categoriaMapper.toResponseList(anyList())).thenReturn(List.of(respuesta));

        List<CategoriaResponse> resultado = categoriaService.listarCategorias();

        assertEquals(1, resultado.size());
        assertSame(respuesta, resultado.get(0));
    }

    @Test
    void creaCategoriaNormalizandoNombreYDescripcion() {
        CategoriaRequest request = CategoriaRequest.builder()
                .nombre("  Redes  ")
                .descripcion("  Infraestructura  ")
                .build();
        CategoriaResponse respuesta = CategoriaResponse.builder().id(2L).build();

        when(categoriaRepository.existsByNombre("Redes")).thenReturn(false);
        when(categoriaMapper.toResponse(any(Categoria.class))).thenReturn(respuesta);

        CategoriaResponse resultado = categoriaService.crearCategoria(request);

        assertSame(respuesta, resultado);
        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        assertEquals("Redes", captor.getValue().getNombre());
        assertEquals("Infraestructura", captor.getValue().getDescripcion());
        assertTrue(captor.getValue().getActivo());
    }

    @Test
    void rechazaCategoriaDuplicada() {
        CategoriaRequest request = CategoriaRequest.builder().nombre("Redes").build();
        when(categoriaRepository.existsByNombre("Redes")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> categoriaService.crearCategoria(request));

        assertTrue(ex.getMessage().contains("Ya existe"));
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void rechazaActualizacionConNombreDeOtraCategoria() {
        CategoriaRequest request = CategoriaRequest.builder().nombre("Servidores").build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNombreAndIdNot("Servidores", 1L)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> categoriaService.actualizarCategoria(1L, request));

        assertTrue(ex.getMessage().contains("Ya existe"));
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void desactivaCategoriaSinBorrarla() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        categoriaService.desactivarCategoria(1L);

        assertFalse(categoria.getActivo());
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void reactivaCategoria() {
        categoria.setActivo(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        categoriaService.reactivarCategoria(1L);

        assertTrue(categoria.getActivo());
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void rechazaCategoriaInexistente() {
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.desactivarCategoria(9L));

        verify(categoriaRepository, never()).save(any(Categoria.class));
    }
}
