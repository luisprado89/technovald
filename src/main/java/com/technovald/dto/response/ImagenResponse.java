package com.technovald.dto.response;

import lombok.*;

/** Imagen de la galería en la respuesta del detalle. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ImagenResponse {

    /** URL de alta resolución. */
    private String urlAlta;

    /** URL de 500x500. */
    private String urlMedia;

    /** Indica si es la principal. */
    private boolean esPrincipal;
}