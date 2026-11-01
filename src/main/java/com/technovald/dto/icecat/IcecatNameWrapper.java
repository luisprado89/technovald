package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Envoltorio {@code { "Name": { "Value": "..." } }} que Icecat repite en
 * categoría, grupo de especificaciones y nombre de característica.
 *
 * <p>
 * Una sola clase cubre los tres sitios: la forma del JSON es idéntica y
 * crear tres clases sería duplicar el mismo objeto.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatNameWrapper {

    /** Nombre del elemento, con el texto dentro de {@code Value}. */
    private IcecatNameValue Name;
}