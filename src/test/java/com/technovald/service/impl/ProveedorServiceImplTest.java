package com.technovald.service.impl;

import com.technovald.dto.request.ProveedorRequest;
import com.technovald.dto.response.ProveedorResponse;
import com.technovald.entity.Proveedor;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.ProveedorMapper;
import com.technovald.repository.mysql.ProveedorRepository;
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
class ProveedorServiceImplTest {

    @Mock private ProveedorRepository proveedorRepository;
    @Mock private ProveedorMapper proveedorMapper;

    @InjectMocks private ProveedorServiceImpl proveedorService;

    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        proveedor = Proveedor.builder()
                .id(1L)
                .nombreEmpresa("Suministros Vigo")
                .cif("B12345678")
                .contacto("Ana")
                .telefono("986000000")
                .email("ana@proveedor.test")
                .direccion("Calle Vigo 1")
                .activo(true)
                .build();
    }

    private ProveedorRequest request() {
        return ProveedorRequest.builder()
                .nombreEmpresa("  Suministros Vigo  ")
                .cif(" b12345678 ")
                .contacto(" Ana ")
                .telefono(" 986000000 ")
                .email(" ANA@PROVEEDOR.TEST ")
                .direccion(" Calle Vigo 1 ")
                .build();
    }

    @Test
    void creaProveedorNormalizandoCifYEmail() {
        ProveedorResponse respuesta = ProveedorResponse.builder().id(2L).build();
        when(proveedorRepository.existsByCifIgnoreCase("B12345678")).thenReturn(false);
        when(proveedorMapper.toResponse(any(Proveedor.class))).thenReturn(respuesta);

        ProveedorResponse resultado = proveedorService.crearProveedor(request());

        assertSame(respuesta, resultado);
        ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
        verify(proveedorRepository).save(captor.capture());
        assertEquals("B12345678", captor.getValue().getCif());
        assertEquals("ana@proveedor.test", captor.getValue().getEmail());
        assertEquals("Suministros Vigo", captor.getValue().getNombreEmpresa());
        assertTrue(captor.getValue().getActivo());
    }

    @Test
    void rechazaCifDuplicado() {
        when(proveedorRepository.existsByCifIgnoreCase("B12345678")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> proveedorService.crearProveedor(request()));

        assertTrue(ex.getMessage().contains("CIF"));
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    void listaTodosLosProveedores() {
        ProveedorResponse respuesta = ProveedorResponse.builder().id(1L).build();
        when(proveedorRepository.findAllByOrderByNombreEmpresaAsc()).thenReturn(List.of(proveedor));
        when(proveedorMapper.toResponseList(anyList())).thenReturn(List.of(respuesta));

        List<ProveedorResponse> resultado = proveedorService.listarProveedores();

        assertEquals(1, resultado.size());
        assertSame(respuesta, resultado.get(0));
    }

    @Test
    void listaSoloProveedoresActivos() {
        ProveedorResponse respuesta = ProveedorResponse.builder().id(1L).build();
        when(proveedorRepository.findAllByActivoTrueOrderByNombreEmpresaAsc())
                .thenReturn(List.of(proveedor));
        when(proveedorMapper.toResponseList(anyList())).thenReturn(List.of(respuesta));

        List<ProveedorResponse> resultado = proveedorService.listarProveedoresActivos();

        assertEquals(1, resultado.size());
        assertSame(respuesta, resultado.get(0));
    }

    @Test
    void rechazaActualizacionConCifDeOtroProveedor() {
        ProveedorRequest request = ProveedorRequest.builder()
                .nombreEmpresa("Suministros Vigo")
                .cif("B99999999")
                .contacto("Ana")
                .telefono("986000000")
                .email("ana@proveedor.test")
                .direccion("Calle Vigo 1")
                .build();
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(proveedorRepository.existsByCifIgnoreCaseAndIdNot("B99999999", 1L)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> proveedorService.actualizarProveedor(1L, request));

        assertTrue(ex.getMessage().contains("CIF"));
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    void desactivaProveedorSinBorrarlo() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));

        proveedorService.desactivarProveedor(1L);

        assertFalse(proveedor.getActivo());
        verify(proveedorRepository).save(proveedor);
    }

    @Test
    void reactivaProveedor() {
        proveedor.setActivo(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));

        proveedorService.reactivarProveedor(1L);

        assertTrue(proveedor.getActivo());
        verify(proveedorRepository).save(proveedor);
    }

    @Test
    void rechazaProveedorInexistente() {
        when(proveedorRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> proveedorService.desactivarProveedor(9L));

        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }
}
