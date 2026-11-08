package com.technovald.dto.response;

import lombok.*;

/** Producto y unidades vendidas dentro del Top. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoMasVendidoResponse {

    /** Nombre corto del producto. */
    private String nombreProducto;

    /** Marca del producto. */
    private String marca;

    /** Unidades vendidas. */
    private Long cantidadTotalVendida;
}