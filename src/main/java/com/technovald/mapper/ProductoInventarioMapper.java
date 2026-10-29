package com.technovald.mapper;

import com.technovald.dto.request.ProductoInventarioRequest;
import com.technovald.dto.response.ProductoInventarioResponse;
import com.technovald.entity.Categoria;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Proveedor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** Mapper para convertir {@link ProductoInventario} a DTOs y viceversa. */
@Component
public class ProductoInventarioMapper {

    /** Convierte una entidad ProductoInventario en un DTO de respuesta. */
    public ProductoInventarioResponse toResponse(ProductoInventario productoInventario) {
        return ProductoInventarioResponse.builder()
                .id(productoInventario.getId())
                .ean(productoInventario.getEan())
                .nombreCorto(productoInventario.getNombreCorto())
                .stockActual(productoInventario.getStockActual())
                .stockMinimo(productoInventario.getStockMinimo())
                .precioCompra(productoInventario.getPrecioCompra())
                .precioVenta(productoInventario.getPrecioVenta())
                .marca(productoInventario.getMarca())
                .categoriaId(productoInventario.getCategoria().getId())
                .categoriaNombre(productoInventario.getCategoria().getNombre())
                .proveedorId(productoInventario.getProveedor().getId())
                .proveedorNombre(productoInventario.getProveedor().getNombreEmpresa())
                .activo(productoInventario.getActivo())
                .build();
    }

    /** Convierte una lista de entidades en una lista de DTOs. */
    public List<ProductoInventarioResponse> toResponseList(List<ProductoInventario> productos) {
        return productos.stream()
                .map(this::toResponse)
                .toList();
    }

    /** Crea una nueva entidad ProductoInventario a partir del DTO de alta. */
    public ProductoInventario toEntity(ProductoInventarioRequest productoInventarioRequest,
                                       Categoria categoria,
                                       Proveedor proveedor) {
        return ProductoInventario.builder()
                .ean(normalizarEan(productoInventarioRequest.getEan()))
                .nombreCorto(productoInventarioRequest.getNombreCorto().trim())
                .stockActual(productoInventarioRequest.getStockActual())
                .stockMinimo(productoInventarioRequest.getStockMinimo())
                .precioCompra(productoInventarioRequest.getPrecioCompra())
                .precioVenta(productoInventarioRequest.getPrecioVenta())
                .marca(productoInventarioRequest.getMarca().trim())
                .categoria(categoria)
                .proveedor(proveedor)
                .activo(true)
                .build();
    }

    /**
     * Actualiza una entidad existente. El EAN y el stock no se tocan
     * (el stock se modifica mediante el ajuste).
     */
    public void updateEntity(ProductoInventario productoInventario,
                             ProductoInventarioRequest productoInventarioRequest,
                             Categoria categoria,
                             Proveedor proveedor) {
        productoInventario.setNombreCorto(productoInventarioRequest.getNombreCorto().trim());
        productoInventario.setStockMinimo(productoInventarioRequest.getStockMinimo());
        productoInventario.setPrecioCompra(productoInventarioRequest.getPrecioCompra());
        productoInventario.setPrecioVenta(productoInventarioRequest.getPrecioVenta());
        productoInventario.setMarca(productoInventarioRequest.getMarca().trim());
        productoInventario.setCategoria(categoria);
        productoInventario.setProveedor(proveedor);
    }

    /** Normaliza el EAN a mayúsculas y sin espacios. */
    public String normalizarEan(String ean) {
        return ean.trim().toUpperCase(Locale.ROOT);
    }
}