package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Resumen que genera Icecat cuando {@code Description} viene vacío. El corto
 * se usa para la tarjeta y el largo como respaldo de la descripción.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatSummaryDescription {

    /** Resumen de una línea. */
    private String ShortSummaryDescription;

    /** Resumen largo con las características principales. */
    private String LongSummaryDescription;
}