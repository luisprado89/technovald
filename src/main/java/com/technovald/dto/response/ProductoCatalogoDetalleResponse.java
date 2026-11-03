package com.technovald.dto.response;

import com.technovald.document.EspecificacionGrupo;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Ficha completa para la página de detalle. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoCatalogoDetalleResponse {

    /** Id de la ficha en MongoDB. */
    private String id;

    /** EAN del producto. */
    private String ean;

    /** Título largo. */
    private String titulo;

    /** Nombre corto. */
    private String nombreCorto;

    /** Marca. */
    private String marca;

    /** Categoría. */
    private String categoria;

    /** Descripción de una línea. */
    private String descripcionCorta;

    /** Descripción larga. */
    private String descripcionLarga;

    /** Galería con alta, media y principal. */
    private List<ImagenResponse> imagenes;

    /** Especificaciones agrupadas. */
    private List<EspecificacionGrupo> especificaciones;

    /** Reseñas activas. */
    private List<ComentarioResponse> comentarios;

    /** Precio de venta (MySQL). */
    private BigDecimal precioVenta;

    /** Stock actual (MySQL). */
    private Integer stockActual;

    /** Indica si hay stock. */
    private Boolean disponible;

    /** Última sincronización. */
    private LocalDateTime fechaSincronizacion;

    /** Estado de la ficha. */
    private Boolean activo;
}