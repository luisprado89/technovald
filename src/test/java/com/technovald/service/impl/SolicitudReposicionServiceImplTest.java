package com.technovald.service.impl;

import com.technovald.dto.request.SolicitudReposicionRequest;
import com.technovald.dto.response.SolicitudReposicionResponse;
import com.technovald.entity.Empleado;
import com.technovald.entity.Persona;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Proveedor;
import com.technovald.entity.SolicitudReposicion;
import com.technovald.entity.Usuario;
import com.technovald.enums.EstadoSolicitud;
import com.technovald.exception.BusinessRuleException;
import com.technovald.mapper.SolicitudReposicionMapper;
import com.technovald.repository.mysql.EmpleadoRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.repository.mysql.ProveedorRepository;
import com.technovald.repository.mysql.SolicitudReposicionRepository;
import com.technovald.repository.mysql.UsuarioRepository;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudReposicionServiceImplTest {

    @Mock private SolicitudReposicionRepository solicitudRepository;
    @Mock private ProductoInventarioRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SolicitudReposicionMapper solicitudMapper;

    @InjectMocks private SolicitudReposicionServiceImpl solicitudService;

    private Persona persona;
    private Usuario usuario;
    private Empleado empleado;
    private ProductoInventario producto;
    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        persona = Persona.builder().id(1L).nombre("Luis").apellido("Pino").build();
        usuario = Usuario.builder().username("empleado").persona(persona).build();
        empleado = Empleado.builder().id(3L).persona(persona).build();
        producto = ProductoInventario.builder().id(10L).ean("EAN-10").nombreCorto("Monitor").build();
        proveedor = Proveedor.builder().id(20L).nombreEmpresa("Proveedor Demo").build();
    }

    private SolicitudReposicion solicitud() {
        return SolicitudReposicion.builder()
                .id(1L)
                .cantidad(5)
                .producto(producto)
                .proveedor(proveedor)
                .empleado(empleado)
                .build();
    }

    @Test
    void creaSolicitudPendienteParaElEmpleado() {
        SolicitudReposicionResponse respuesta = SolicitudReposicionResponse.builder().id(1L).build();
        SolicitudReposicionRequest request = SolicitudReposicionRequest.builder()
                .productoId(10L).proveedorId(20L).cantidad(5).build();

        when(usuarioRepository.findByUsernameAndActivoTrue("empleado")).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findByPersonaIdAndActivoTrue(1L)).thenReturn(Optional.of(empleado));
        when(productoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(producto));
        when(proveedorRepository.findByIdAndActivoTrue(20L)).thenReturn(Optional.of(proveedor));
        when(solicitudMapper.toResponse(any(SolicitudReposicion.class))).thenReturn(respuesta);

        SolicitudReposicionResponse resultado = solicitudService.crearSolicitud(request, "empleado");

        assertSame(respuesta, resultado);
        ArgumentCaptor<SolicitudReposicion> captor = ArgumentCaptor.forClass(SolicitudReposicion.class);
        verify(solicitudRepository).save(captor.capture());
        SolicitudReposicion guardada = captor.getValue();
        assertEquals(5, guardada.getCantidad());
        assertEquals(EstadoSolicitud.PENDIENTE, guardada.getEstado());
        assertSame(producto, guardada.getProducto());
        assertSame(proveedor, guardada.getProveedor());
        assertSame(empleado, guardada.getEmpleado());
    }

    @Test
    void rechazaSolicitudSiElUsuarioNoEsEmpleado() {
        SolicitudReposicionRequest request = SolicitudReposicionRequest.builder()
                .productoId(10L).proveedorId(20L).cantidad(5).build();

        when(usuarioRepository.findByUsernameAndActivoTrue("empleado")).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findByPersonaIdAndActivoTrue(1L)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> solicitudService.crearSolicitud(request, "empleado"));

        assertTrue(ex.getMessage().contains("perfil de empleado"));
        verify(productoRepository, never()).findByIdAndActivoTrue(anyLong());
    }

    @Test
    void recibeSolicitudEIncrementaStock() {
        SolicitudReposicion pendiente = solicitud();
        SolicitudReposicion recibida = solicitud();
        recibida.setEstado(EstadoSolicitud.RECIBIDO);
        SolicitudReposicionResponse respuesta = SolicitudReposicionResponse.builder()
                .id(1L).estado(EstadoSolicitud.RECIBIDO).build();

        when(solicitudRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(pendiente), Optional.of(recibida));
        when(solicitudRepository.marcarComoRecibida(1L, EstadoSolicitud.RECIBIDO,
                EstadoSolicitud.PENDIENTE)).thenReturn(1);
        when(productoRepository.incrementarStockAtomico(10L, 5)).thenReturn(1);
        when(solicitudMapper.toResponse(recibida)).thenReturn(respuesta);

        SolicitudReposicionResponse resultado = solicitudService.recibirSolicitud(1L);

        assertSame(respuesta, resultado);
        verify(solicitudRepository).marcarComoRecibida(1L, EstadoSolicitud.RECIBIDO,
                EstadoSolicitud.PENDIENTE);
        verify(productoRepository).incrementarStockAtomico(10L, 5);
    }

    @Test
    void rechazaRecepcionDuplicada() {
        when(solicitudRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(solicitud()));
        when(solicitudRepository.marcarComoRecibida(1L, EstadoSolicitud.RECIBIDO,
                EstadoSolicitud.PENDIENTE)).thenReturn(0);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> solicitudService.recibirSolicitud(1L));

        assertTrue(ex.getMessage().contains("ya ha sido recibida"));
        verify(productoRepository, never()).incrementarStockAtomico(anyLong(), any(Integer.class));
    }

    @Test
    void rechazaRecepcionSiElProductoEstaInactivo() {
        when(solicitudRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(solicitud()));
        when(solicitudRepository.marcarComoRecibida(1L, EstadoSolicitud.RECIBIDO,
                EstadoSolicitud.PENDIENTE)).thenReturn(1);
        when(productoRepository.incrementarStockAtomico(10L, 5)).thenReturn(0);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> solicitudService.recibirSolicitud(1L));

        assertTrue(ex.getMessage().contains("Producto no encontrado o inactivo"));
        verify(solicitudRepository, times(1)).findByIdAndActivoTrue(1L);
    }

    @Test
    void listaSolicitudesSinFiltro() {
        SolicitudReposicion solicitud = solicitud();
        SolicitudReposicionResponse respuesta = SolicitudReposicionResponse.builder().id(1L).build();

        when(solicitudRepository.findAllByActivoTrue()).thenReturn(List.of(solicitud));
        when(solicitudMapper.toResponseList(anyList())).thenReturn(List.of(respuesta));

        List<SolicitudReposicionResponse> resultado = solicitudService.getAllSolicitudes(null);

        assertEquals(1, resultado.size());
        assertSame(respuesta, resultado.get(0));
    }

    @Test
    void listaSolicitudesFiltradasPorEstado() {
        SolicitudReposicion solicitud = solicitud();
        SolicitudReposicionResponse respuesta = SolicitudReposicionResponse.builder()
                .id(1L).estado(EstadoSolicitud.PENDIENTE).build();

        when(solicitudRepository.findAllByEstadoAndActivoTrue(EstadoSolicitud.PENDIENTE))
                .thenReturn(List.of(solicitud));
        when(solicitudMapper.toResponseList(anyList())).thenReturn(List.of(respuesta));

        List<SolicitudReposicionResponse> resultado =
                solicitudService.getAllSolicitudes(EstadoSolicitud.PENDIENTE);

        assertEquals(1, resultado.size());
        assertEquals(EstadoSolicitud.PENDIENTE, resultado.get(0).getEstado());
    }
}
