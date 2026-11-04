package com.technovald.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * Datos para crear una solicitud de reposición de stock.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SolicitudReposicionRequest {

    /** Producto cuyo stock se quiere reponer. */
    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    /** Proveedor al que se le solicita la mercancía. */
    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;

    /** Unidades solicitadas. */
    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor que 0")
    private Integer cantidad;
}