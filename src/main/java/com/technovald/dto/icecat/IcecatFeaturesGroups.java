package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

/**
 * Grupo de especificaciones técnicas (Pantalla, Procesador, Diseño...).
 * El mapper lo aplana a un mapa nombre → valor en el documento plano.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatFeaturesGroups {

    /** Nombre del grupo. */
    private IcecatNameWrapper FeatureGroup;

    /** Características que pertenecen al grupo. */
    private List<IcecatFeature> Features;
}