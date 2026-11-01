package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Imagen de la galería. El documento plano guarda una sola URL por imagen,
 * así que el mapper prefiere {@code Pic} y usa {@code Pic500x500} solo si
 * falta la primera.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatGallery {

    /** URL de alta resolución. */
    private String Pic;

    /** URL de 500x500, usada como respaldo. */
    private String Pic500x500;

    /** "Y" si es la imagen principal; decide cuál va primero. */
    private String IsMain;
}