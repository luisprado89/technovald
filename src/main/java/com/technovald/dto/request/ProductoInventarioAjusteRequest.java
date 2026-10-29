package com.technovald.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Datos para ajustar el stock de un producto (positivo para entradas, negativo para salidas). */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoInventarioAjusteRequest {

    /** Cantidad a sumar (o restar, si es negativa) al stock actual. */
    @NotNull(message = "La cantidad es obligatoria")
    private Integer cantidad;
}