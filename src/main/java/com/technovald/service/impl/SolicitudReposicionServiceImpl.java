package com.technovald.service.impl;

import com.technovald.dto.request.SolicitudReposicionRequest;
import com.technovald.dto.response.SolicitudReposicionResponse;
import com.technovald.entity.Empleado;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Proveedor;
import com.technovald.entity.SolicitudReposicion;
import com.technovald.entity.Usuario;
import com.technovald.enums.EstadoSolicitud;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.SolicitudReposicionMapper;
import com.technovald.repository.mysql.EmpleadoRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.repository.mysql.ProveedorRepository;
import com.technovald.repository.mysql.SolicitudReposicionRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.service.SolicitudReposicionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de {@link SolicitudReposicionService}.
 *
 * <p>
 * La recepción usa dos UPDATE condicionales (estado y stock) para evitar
 * dobles incrementos; el mapper se ejecuta dentro de la transacción porque
 * las relaciones son LAZY.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudReposicionServiceImpl implements SolicitudReposicionService {

    private final SolicitudReposicionRepository solicitudRepository;
    private final ProductoInventarioRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudReposicionMapper solicitudMapper;

    /**
     * Crea una solicitud PENDIENTE para el empleado autenticado.
     *
     * @param solicitudReposicionRequest  producto, proveedor y cantidad.
     * @param username usuario autenticado.
     * @return la solicitud creada.
     * @throws ResourceNotFoundException si el usuario, el producto o el proveedor no existen o están inactivos.
     * @throws BusinessRuleException     si el usuario no tiene perfil de empleado.
     */
    @Override
    @Transactional
    public SolicitudReposicionResponse crearSolicitud(SolicitudReposicionRequest solicitudReposicionRequest, String username) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Empleado empleado = empleadoRepository.findByPersonaIdAndActivoTrue(usuario.getPersona().getId())
                .orElseThrow(() -> new BusinessRuleException("El usuario no tiene perfil de empleado."));

        ProductoInventario producto = productoRepository.findByIdAndActivoTrue(solicitudReposicionRequest.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        Proveedor proveedor = proveedorRepository.findByIdAndActivoTrue(solicitudReposicionRequest.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

        SolicitudReposicion solicitud = SolicitudReposicion.builder()
                .cantidad(solicitudReposicionRequest.getCantidad())
                .producto(producto)
                .proveedor(proveedor)
                .empleado(empleado)
                .build();

        solicitudRepository.save(solicitud);
        log.info("Solicitud de reposición ID: {} creada por el empleado '{}'", solicitud.getId(), username);

        return solicitudMapper.toResponse(solicitud);
    }

    /**
     * Marca la solicitud como RECIBIDO y suma la cantidad al stock.
     *
     * <p>
     * La transición y el incremento son atómicos: si el stock no se
     * actualiza, la transacción revierte el estado.
     * </p>
     *
     * @param id id de la solicitud.
     * @return la solicitud actualizada.
     * @throws ResourceNotFoundException si no existe o está inactiva.
     * @throws BusinessRuleException     si ya fue recibida o el producto está inactivo.
     */
    @Override
    @Transactional
    public SolicitudReposicionResponse recibirSolicitud(Long id) {
        SolicitudReposicion solicitud = solicitudRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        // Se leen antes del UPDATE masivo: clearAutomatically vacía el contexto.
        Long productoId = solicitud.getProducto().getId();
        Integer cantidad = solicitud.getCantidad();

        int filasEstado = solicitudRepository.marcarComoRecibida(
                id, EstadoSolicitud.RECIBIDO, EstadoSolicitud.PENDIENTE);

        if (filasEstado == 0) {
            throw new BusinessRuleException("La solicitud ya ha sido recibida o no está activa.");
        }

        int filasStock = productoRepository.incrementarStockAtomico(productoId, cantidad);

        if (filasStock == 0) {
            throw new BusinessRuleException("Producto no encontrado o inactivo.");
        }

        // Relectura: las sentencias masivas han vaciado el contexto de persistencia.
        SolicitudReposicion actualizada = solicitudRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));

        log.info("Solicitud ID: {} recibida. Stock del producto {} incrementado en {} unidades.",
                id, actualizada.getProducto().getEan(), cantidad);

        return solicitudMapper.toResponse(actualizada);
    }

    /**
     * Lista las solicitudes activas, con filtro opcional por estado.
     *
     * @param estado estado a filtrar, o {@code null} para todas.
     * @return las solicitudes.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SolicitudReposicionResponse> getAllSolicitudes(EstadoSolicitud estado) {
        List<SolicitudReposicion> solicitudes = (estado != null)
                ? solicitudRepository.findAllByEstadoAndActivoTrue(estado)
                : solicitudRepository.findAllByActivoTrue();

        return solicitudMapper.toResponseList(solicitudes);
    }
}