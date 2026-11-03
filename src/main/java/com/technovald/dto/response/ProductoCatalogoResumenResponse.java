package com.technovald.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** Ficha ligera para las tarjetas del catálogo. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoCatalogoResumenResponse {

    /** Id de la ficha en MongoDB. */
    private String id;

    /** EAN del producto. */
    private String ean;

    /** Nombre corto para la tarjeta. */
    private String nombreCorto;

    /** Marca. */
    private String marca;

    /** Categoría. */
    private String categoria;

    /** Precio de venta (MySQL). */
    private BigDecimal precioVenta;

    /** Stock actual (MySQL). */
    private Integer stockActual;

    /** Indica si hay stock. */
    private Boolean disponible;

    /** URL de la imagen principal en 500x500 (o alta si no hay media). */
    private String imagenPrincipalUrl;
}