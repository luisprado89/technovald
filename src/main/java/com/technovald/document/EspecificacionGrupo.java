package com.technovald.document;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Grupo de especificaciones embebido en la ficha. El nombre de la
 * característica vive en {@link EspecificacionItem}, así que puede contener
 * puntos sin romper MongoDB.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EspecificacionGrupo {

    /** Nombre del grupo (Pantalla, Diseño, Procesador...). */
    private String grupo;

    /** Características del grupo. */
    @Builder.Default
    private List<EspecificacionItem> items = new ArrayList<>();
}