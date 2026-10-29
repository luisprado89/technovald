package com.technovald.mapper;

import com.technovald.dto.response.CategoriaResponse;
import com.technovald.entity.Categoria;
import org.springframework.stereotype.Component;

import java.util.List;

/** Mapper para convertir {@link Categoria} a DTOs de respuesta. */
@Component
public class CategoriaMapper {

    /** Convierte una entidad Categoria en un DTO de respuesta. */
    public CategoriaResponse toResponse(Categoria categoria) {
        return CategoriaResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .activo(categoria.getActivo())
                .build();
    }

    /** Convierte una lista de entidades en una lista de DTOs. */
    public List<CategoriaResponse> toResponseList(List<Categoria> categorias) {
        return categorias.stream()
                .map(this::toResponse)
                .toList();
    }
}