package com.technovald.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/** Datos de entrada para crear o editar un producto del inventario. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoInventarioRequest {

    /** Código de barras EAN (único en el sistema). */
    @NotBlank(message = "El EAN es obligatorio")
    @Size(min = 8, max = 15, message = "El EAN debe tener entre 8 y 15 caracteres")
    private String ean;

    /** Nombre operativo del producto para el inventario. */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
    private String nombreCorto;

    /** Unidades disponibles inicialmente. Solo se usa en el alta; para cambios se usa el ajuste. */
    @NotNull(message = "El stock actual es obligatorio")
    @PositiveOrZero(message = "El stock actual no puede ser negativo")
    private Integer stockActual;

    /** Umbral mínimo de stock para alertar de reposición. */
    @NotNull(message = "El stock mínimo es obligatorio")
    @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo;

    /** Precio de coste al proveedor. */
    @NotNull(message = "El precio de compra es obligatorio")
    @Positive(message = "El precio de compra debe ser mayor que 0")
    @Digits(integer = 8, fraction = 2, message = "El precio de compra admite 8 enteros y 2 decimales")
    private BigDecimal precioCompra;

    /** Precio de venta al público. */
    @NotNull(message = "El precio de venta es obligatorio")
    @Positive(message = "El precio de venta debe ser mayor que 0")
    @Digits(integer = 8, fraction = 2, message = "El precio de venta admite 8 enteros y 2 decimales")
    private BigDecimal precioVenta;

    /** Marca del producto. */
    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
    private String marca;

    /** ID de la categoría a la que pertenece el producto. */
    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    /** ID del proveedor habitual del producto. */
    @NotNull(message = "El proveedor es obligatorio")
    private Long proveedorId;
}