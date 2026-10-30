package com.technovald.mapper;

import com.technovald.document.ProductoDocumento;
import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ProductoCatalogoResponse;
import com.technovald.entity.ProductoInventario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Mapper para convertir entre las fichas de catálogo (MongoDB) y sus DTOs. */
@Component
public class ProductoCatalogoMapper {

    /** Convierte un DTO de petición a un documento de MongoDB. */
    public ProductoDocumento toDocumento(ProductoCatalogoRequest request, ProductoDocumento existente) {
        ProductoDocumento documento = existente != null ? existente : new ProductoDocumento();

        documento.setEan(request.getEan().trim().toUpperCase(Locale.ROOT));
        documento.setNombre(request.getNombre().trim());
        documento.setDescripcionLarga(texto(request.getDescripcionLarga()));
        documento.setMarca(texto(request.getMarca()));
        documento.setCategoria(texto(request.getCategoria()));
        documento.setImagenes(limpiarLista(request.getImagenes()));
        documento.setEspecificaciones(limpiarMapa(request.getEspecificaciones()));

        return documento;
    }

    /** Convierte un documento de MongoDB a DTO de respuesta, enriqueciéndolo con datos de MySQL. */
    public ProductoCatalogoResponse toResponse(ProductoDocumento documento, ProductoInventario inventario) {
        BigDecimal precio = inventario == null ? null : inventario.getPrecioVenta();
        Integer stock = inventario == null ? null : inventario.getStockActual();

        return ProductoCatalogoResponse.builder()
                .id(documento.getId())
                .ean(documento.getEan())
                .nombre(documento.getNombre())
                .descripcionLarga(documento.getDescripcionLarga())
                .marca(documento.getMarca())
                .categoria(documento.getCategoria())
                .imagenes(documento.getImagenes())
                .especificaciones(documento.getEspecificaciones())
                .fechaCreacion(documento.getFechaCreacion())
                .fechaActualizacion(documento.getFechaActualizacion())
                .precioVenta(precio)
                .stockActual(stock)
                .disponible(stock != null && stock > 0)
                .build();
    }

    /** Convierte una lista de documentos a una lista de DTOs, enlazando con el inventario por EAN. */
    public List<ProductoCatalogoResponse> toResponseList(List<ProductoDocumento> documentos,
                                                         Map<String, ProductoInventario> inventarioPorEan) {
        return documentos.stream()
                .map(documento -> toResponse(documento, inventarioPorEan.get(documento.getEan())))
                .toList();
    }

    /** Convierte cadenas vacías en {@code null} para no guardar basura en Mongo. */
    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Limpia la lista de imágenes eliminando elementos vacíos o espacios. */
    private List<String> limpiarLista(List<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .toList();
    }

    /**
     * Limpia el mapa de especificaciones: elimina claves vacías y recorta los valores.
     * Usa {@link LinkedHashMap} para mantener el orden de inserción de las claves.
     */
    private Map<String, String> limpiarMapa(Map<String, String> valores) {
        if (valores == null) {
            return Map.of();
        }
        Map<String, String> limpio = new LinkedHashMap<>();
        valores.forEach((clave, valor) -> {
            if (clave != null && !clave.isBlank()) {
                limpio.put(clave.trim(), valor == null ? "" : valor.trim());
            }
        });
        return limpio;
    }
}