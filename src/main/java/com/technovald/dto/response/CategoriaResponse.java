package com.technovald.dto.response;

import lombok.*;

/** Datos de una categoría devueltos al panel de administración. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CategoriaResponse {

    /** Identificador único de la categoría. */
    private Long id;

    /** Nombre de la categoría. */
    private String nombre;

    /** Descripción detallada de la categoría. */
    private String descripcion;

    /** Estado lógico: true si está activa, false si ha sido dada de baja. */
    private Boolean activo;
}