package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Texto traducido de Icecat ({@code { "Value": "..." }}). Se reutiliza en
 * categorías, grupos de especificaciones y nombres de características.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatNameValue {

    /** Texto ya traducido al idioma de la petición. */
    private String Value;
}