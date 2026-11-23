package com.technovald.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Datos para ajustar el stock de un producto (positivo para entradas, negativo para salidas). */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoInventarioAjusteRequest {

    /** Cantidad a sumar (o restar, si es negativa) al stock actual. */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = -1_000_000, message = "Un ajuste no puede restar más de 1.000.000 de unidades")
    @Max(value = 1_000_000, message = "Un ajuste no puede sumar más de 1.000.000 de unidades")
    private Integer cantidad;
}