package com.technovald.repository.mysql;

/**
 * Proyección del Top de productos más vendidos.
 *
 * <p>
 * Los alias de la JPQL deben coincidir con los accesores: {@code nombreCorto},
 * {@code marca} y {@code cantidadTotal}.
 * </p>
 */
public interface ProductoMasVendidoProjection {

    /** Nombre corto del producto. */
    String getNombreCorto();

    /** Marca del producto. */
    String getMarca();

    /** Suma de unidades vendidas. */
    Long getCantidadTotal();
}