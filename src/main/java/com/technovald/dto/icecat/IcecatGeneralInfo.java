package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Datos generales de la ficha. Es la fuente del nombre, la marca, la
 * categoría y la descripción que se guardan en el documento plano.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatGeneralInfo {

    /** Título comercial completo; se guarda como nombre. */
    private String Title;

    /** Marca del producto. */
    private String Brand;

    /** Categoría con el nombre traducido. */
    private IcecatNameWrapper Category;

    /** Descripción larga en HTML. */
    private IcecatDescription Description;
}