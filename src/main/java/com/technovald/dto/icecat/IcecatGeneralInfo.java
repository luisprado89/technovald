package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Datos generales de la ficha: título, nombres, marca, categoría, descripción
 * y resumen.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatGeneralInfo {

    /** Título comercial completo. */
    private String Title;

    /** Marca del producto. */
    private String Brand;

    /** Nombre largo de Icecat (respaldo del nombre corto). */
    private String ProductName;

    /** Nombre corto internacional. */
    private IcecatProductNameInfo ProductNameInfo;

    /** Categoría con el nombre traducido. */
    private IcecatNameWrapper Category;

    /** Descripción larga en HTML. */
    private IcecatDescription Description;

    /** Resumen corto y largo. */
    private IcecatSummaryDescription SummaryDescription;
}