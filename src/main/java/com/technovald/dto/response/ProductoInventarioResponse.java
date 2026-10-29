package com.technovald.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** Datos de un producto del inventario devueltos al panel de administración. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoInventarioResponse {

    /** Identificador único del producto. */
    private Long id;

    /** Código de barras EAN. */
    private String ean;

    /** Nombre operativo del producto. */
    private String nombreCorto;

    /** Unidades disponibles actualmente. */
    private Integer stockActual;

    /** Umbral mínimo de stock para alertar de reposición. */
    private Integer stockMinimo;

    /** Precio de compra al proveedor. */
    private BigDecimal precioCompra;

    /** Precio de venta al público. */
    private BigDecimal precioVenta;

    /** Marca del producto. */
    private String marca;

    /** ID de la categoría asociada. */
    private Long categoriaId;

    /** Nombre de la categoría (aplanado para la tabla). */
    private String categoriaNombre;

    /** ID del proveedor asociado. */
    private Long proveedorId;

    /** Nombre del proveedor (aplanado para la tabla). */
    private String proveedorNombre;

    /** Estado lógico: true si está activo, false si ha sido dado de baja. */
    private Boolean activo;
}