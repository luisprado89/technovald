package com.technovald.mapper;

import com.technovald.dto.response.SolicitudReposicionResponse;
import com.technovald.entity.SolicitudReposicion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte solicitudes de reposición en DTOs.
 *
 * <p>
 * Debe ejecutarse dentro de la transacción del servicio: producto, proveedor
 * y empleado son relaciones {@code LAZY}.
 * </p>
 */
@Component
public class SolicitudReposicionMapper {

    /**
     * Convierte una solicitud.
     *
     * @param solicitud entidad con sus relaciones accesibles.
     * @return el DTO.
     */
    public SolicitudReposicionResponse toResponse(SolicitudReposicion solicitud) {
        return SolicitudReposicionResponse.builder()
                .id(solicitud.getId())
                .fechaSolicitud(solicitud.getFechaSolicitud())
                .cantidad(solicitud.getCantidad())
                .estado(solicitud.getEstado())
                .nombreProducto(solicitud.getProducto().getNombreCorto())
                .nombreProveedor(solicitud.getProveedor().getNombreEmpresa())
                .nombreEmpleado(solicitud.getEmpleado().getPersona().getNombre()
                        + " " + solicitud.getEmpleado().getPersona().getApellido())
                .build();
    }

    /**
     * Convierte una lista de solicitudes.
     *
     * @param solicitudes entidades con sus relaciones accesibles.
     * @return la lista de DTOs.
     */
    public List<SolicitudReposicionResponse> toResponseList(List<SolicitudReposicion> solicitudes) {
        return solicitudes.stream().map(this::toResponse).toList();
    }
}