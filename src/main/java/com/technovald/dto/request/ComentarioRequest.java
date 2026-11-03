package com.technovald.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Cuerpo de {@code POST /api/comentarios}. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ComentarioRequest {

    /** Id del producto del inventario (MySQL) que se comenta. */
    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoInventarioId;

    /** Texto de la reseña. */
    @NotBlank(message = "El texto del comentario es obligatorio")
    private String texto;

    /** Estrellas de 1 a 5. */
    @NotNull(message = "La valoración es obligatoria")
    @Min(value = 1, message = "La valoración mínima es 1 estrella")
    @Max(value = 5, message = "La valoración máxima es 5 estrellas")
    private Integer valoracion;
}