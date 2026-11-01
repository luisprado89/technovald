package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

/**
 * Bloque {@code data} de Icecat. Agrupa las tres ramas que consume el
 * catálogo: datos generales, galería y especificaciones.
 *
 * <p>
 * Icecat devuelve más ramas ({@code Multimedia}, {@code Reviews},
 * {@code ProductRelated}...), pero el modelo plano no las usa y se
 * descartan con {@code ignoreUnknown}.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatData {

    /** Título, marca, categoría y descripción. */
    private IcecatGeneralInfo GeneralInfo;

    /** Imágenes del producto en varias resoluciones. */
    private List<IcecatGallery> Gallery;

    /** Especificaciones técnicas agrupadas por bloques. */
    private List<IcecatFeaturesGroups> FeaturesGroups;
}