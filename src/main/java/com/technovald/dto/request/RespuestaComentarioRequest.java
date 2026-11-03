package com.technovald.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** Cuerpo de {@code PUT /api/comentarios/{id}/responder}. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RespuestaComentarioRequest {

    /** Texto de la respuesta oficial. */
    @NotBlank(message = "La respuesta no puede estar vacía")
    private String respuestaOficial;
}