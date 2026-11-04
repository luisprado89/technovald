package com.technovald.mapper;

import com.technovald.document.EspecificacionGrupo;
import com.technovald.document.EspecificacionItem;
import com.technovald.document.Imagen;
import com.technovald.document.ProductoDocumento;
import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ComentarioResponse;
import com.technovald.dto.response.ImagenResponse;
import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.ProductoCatalogoResumenResponse;
import com.technovald.entity.ProductoInventario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Mapper entre la ficha de MongoDB y los DTOs de catálogo. */
@Component
@RequiredArgsConstructor
public class ProductoCatalogoMapper {

    private final ComentarioMapper comentarioMapper;

    /**
     * Convierte el alta manual en un documento de MongoDB.
     *
     * <p>
     * Si la ficha ya existe se reutiliza el documento para conservar sus
     * comentarios. El nombre corto cae al título cuando no se envía.
     * </p>
     *
     * @param request   datos de la ficha.
     * @param existente documento previo, o {@code null} si es nueva.
     * @return el documento listo para guardar.
     */
    public ProductoDocumento toDocumento(ProductoCatalogoRequest request, ProductoDocumento existente) {
        ProductoDocumento documento = existente != null ? existente : new ProductoDocumento();

        String titulo = request.getTitulo().trim();
        documento.setEan(request.getEan().trim().toUpperCase(Locale.ROOT));
        documento.setTitulo(titulo);
        documento.setNombreCorto(texto(request.getNombreCorto()) != null ? request.getNombreCorto().trim() : titulo);
        documento.setMarca(texto(request.getMarca()));
        documento.setCategoria(texto(request.getCategoria()));
        documento.setDescripcionCorta(texto(request.getDescripcionCorta()));
        documento.setDescripcionLarga(texto(request.getDescripcionLarga()));
        documento.setImagenes(aImagenes(request.getImagenes()));
        documento.setEspecificaciones(aGrupos(request.getEspecificaciones()));

        if (request.getActivo() != null) {
            documento.setActivo(request.getActivo());
        } else if (existente == null) {
            documento.setActivo(true);
        }

        return documento;
    }

    /**
     * Construye la ficha ligera del listado.
     *
     * @param documento  ficha de MongoDB.
     * @param inventario producto de MySQL, o {@code null} si no está.
     * @return la tarjeta con precio, stock e imagen principal.
     */
    public ProductoCatalogoResumenResponse toResumen(ProductoDocumento documento, ProductoInventario inventario) {
        return ProductoCatalogoResumenResponse.builder()
                .id(documento.getId())
                .ean(documento.getEan())
                .nombreCorto(documento.getNombreCorto())
                .marca(documento.getMarca())
                .categoria(documento.getCategoria())
                .precioVenta(precio(inventario))
                .stockActual(stock(inventario))
                .disponible(disponible(inventario))
                .imagenPrincipalUrl(imagenPrincipal(documento))
                .build();
    }

    /**
     * Construye la ficha completa del detalle.
     *
     * @param documento  ficha de MongoDB.
     * @param inventario producto de MySQL, o {@code null} si no está.
     * @return la ficha con descripciones, galería, especificaciones y reseñas.
     */
    public ProductoCatalogoDetalleResponse toDetalle(ProductoDocumento documento, ProductoInventario inventario) {
        return ProductoCatalogoDetalleResponse.builder()
                .id(documento.getId())
                .ean(documento.getEan())
                .titulo(documento.getTitulo())
                .nombreCorto(documento.getNombreCorto())
                .marca(documento.getMarca())
                .categoria(documento.getCategoria())
                .descripcionCorta(documento.getDescripcionCorta())
                .descripcionLarga(documento.getDescripcionLarga())
                .imagenes(aImagenesResponse(documento.getImagenes()))
                .especificaciones(documento.getEspecificaciones())
                .comentarios(aComentarios(documento))
                .precioVenta(precio(inventario))
                .stockActual(stock(inventario))
                .disponible(disponible(inventario))
                .fechaSincronizacion(documento.getFechaSincronizacion())
                .activo(documento.getActivo())
                .build();
    }

    /**
     * Convierte una lista de fichas en tarjetas de catálogo.
     *
     * @param documentos       fichas de MongoDB.
     * @param inventarioPorEan productos de MySQL indexados por EAN.
     * @return las tarjetas, en el mismo orden.
     */
    public List<ProductoCatalogoResumenResponse> toResumenList(List<ProductoDocumento> documentos,
                                                               Map<String, ProductoInventario> inventarioPorEan) {
        return documentos.stream()
                .map(documento -> toResumen(documento, inventarioPorEan.get(documento.getEan())))
                .toList();
    }

    private List<ImagenResponse> aImagenesResponse(List<Imagen> imagenes) {
        if (imagenes == null) {
            return List.of();
        }
        return imagenes.stream()
                .map(imagen -> ImagenResponse.builder()
                        .urlAlta(imagen.getUrlAlta())
                        .urlMedia(imagen.getUrlMedia())
                        .esPrincipal(imagen.isEsPrincipal())
                        .build())
                .toList();
    }

    private List<ComentarioResponse> aComentarios(ProductoDocumento documento) {
        if (documento.getComentarios() == null) {
            return List.of();
        }
        return documento.getComentarios().stream()
                .filter(comentario -> Boolean.TRUE.equals(comentario.getActivo()))
                .map(comentarioMapper::toResponse)
                .toList();
    }

    private String imagenPrincipal(ProductoDocumento documento) {
        if (documento.getImagenes() == null || documento.getImagenes().isEmpty()) {
            return null;
        }
        Imagen principal = documento.getImagenes().stream()
                .filter(Imagen::isEsPrincipal)
                .findFirst()
                .orElse(documento.getImagenes().get(0));

        return principal.getUrlMedia() != null ? principal.getUrlMedia() : principal.getUrlAlta();
    }

    /** URLs del alta manual: alta = URL, media = null, la primera es principal. */
    private List<Imagen> aImagenes(List<String> urls) {
        if (urls == null) {
            return List.of();
        }
        List<Imagen> imagenes = new ArrayList<>();
        for (String url : urls) {
            if (texto(url) != null) {
                imagenes.add(Imagen.builder()
                        .urlAlta(url.trim())
                        .urlMedia(null)
                        .esPrincipal(imagenes.isEmpty())
                        .build());
            }
        }
        return imagenes;
    }

    /** El alta manual manda un mapa; se guarda en un único grupo. */
    private List<EspecificacionGrupo> aGrupos(Map<String, String> valores) {
        if (valores == null || valores.isEmpty()) {
            return List.of();
        }
        List<EspecificacionItem> items = new ArrayList<>();
        valores.forEach((clave, valor) -> {
            if (clave != null && !clave.isBlank()) {
                items.add(EspecificacionItem.builder()
                        .nombre(clave.trim())
                        .valor(valor == null ? "" : valor.trim())
                        .build());
            }
        });
        if (items.isEmpty()) {
            return List.of();
        }
        return List.of(EspecificacionGrupo.builder().grupo("Especificaciones").items(items).build());
    }

    private BigDecimal precio(ProductoInventario inventario) {
        return inventario != null ? inventario.getPrecioVenta() : null;
    }

    private Integer stock(ProductoInventario inventario) {
        return inventario != null ? inventario.getStockActual() : null;
    }

    private Boolean disponible(ProductoInventario inventario) {
        return inventario != null && inventario.getStockActual() != null && inventario.getStockActual() > 0;
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}