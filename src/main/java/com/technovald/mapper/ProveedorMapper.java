package com.technovald.mapper;

import com.technovald.dto.response.ProveedorResponse;
import com.technovald.entity.Proveedor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Mapper para convertir {@link Proveedor} a DTOs de respuesta. */
@Component
public class ProveedorMapper {

    /** Convierte una entidad Proveedor en un DTO de respuesta. */
    public ProveedorResponse toResponse(Proveedor proveedor) {
        return ProveedorResponse.builder()
                .id(proveedor.getId())
                .nombreEmpresa(proveedor.getNombreEmpresa())
                .cif(proveedor.getCif())
                .contacto(proveedor.getContacto())
                .telefono(proveedor.getTelefono())
                .email(proveedor.getEmail())
                .direccion(proveedor.getDireccion())
                .activo(proveedor.getActivo())
                .build();
    }

    /** Convierte una lista de entidades en una lista de DTOs. */
    public List<ProveedorResponse> toResponseList(List<Proveedor> proveedores) {
        return proveedores.stream()
                .map(this::toResponse)
                .toList();
    }
}