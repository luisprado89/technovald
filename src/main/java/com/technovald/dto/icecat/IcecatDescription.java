package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Descripción de la ficha. Del objeto completo solo se usa la versión
 * larga, que llega en HTML y se pinta tal cual en el detalle.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatDescription {

    /** Descripción larga en HTML; puede contener etiquetas. */
    private String LongDesc;
}