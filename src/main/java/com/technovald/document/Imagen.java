package com.technovald.document;

import lombok.*;

/**
 * Imagen embebida en la ficha. Guarda las dos resoluciones de Icecat y cuál es
 * la principal, para que el listado use la media y el detalle la alta.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Imagen {

    /** URL de alta resolución, para el detalle. */
    private String urlAlta;

    /** URL de 500x500, para las tarjetas del catálogo. */
    private String urlMedia;

    /** Indica si es la imagen principal. */
    @Builder.Default
    private boolean esPrincipal = false;
}