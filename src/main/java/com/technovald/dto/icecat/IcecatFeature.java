package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Característica técnica concreta. Icecat entrega el valor ya formateado
 * ("810 g", "Bluetooth 5.1"), así que se guarda tal cual.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatFeature {

    /** Valor formateado de la característica. */
    private String PresentationValue;

    /** Nombre de la característica. */
    private IcecatNameWrapper Feature;
}