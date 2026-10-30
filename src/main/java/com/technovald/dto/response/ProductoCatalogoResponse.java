package com.technovald.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Datos de una ficha de catálogo enriquecidos con información del inventario (MySQL). */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoCatalogoResponse {

    /** ID de la ficha en MongoDB. */
    private String id;

    /** Código EAN del producto. */
    private String ean;

    /** Nombre comercial. */
    private String nombre;

    /** Descripción detallada. */
    private String descripcionLarga;

    /** Marca del producto. */
    private String marca;

    /** Categoría del producto. */
    private String categoria;

    /** Lista de URLs de imágenes. */
    private List<String> imagenes;

    /** Especificaciones técnicas. */
    private Map<String, String> especificaciones;

    /** Fecha de creación de la ficha. */
    private LocalDateTime fechaCreacion;

    /** Fecha de la última actualización. */
    private LocalDateTime fechaActualizacion;

    /** Precio de venta (obtenido de MySQL). */
    private BigDecimal precioVenta;

    /** Stock actual (obtenido de MySQL). */
    private Integer stockActual;

    /** Indica si el producto tiene stock disponible (stockActual > 0). */
    private Boolean disponible;
}