package com.technovald.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Cuerpo de {@code PUT /api/comentarios/{id}/responder}. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RespuestaComentarioRequest {

    /** Texto de la respuesta oficial. */
    @NotBlank(message = "La respuesta no puede estar vacía")
    @Size(max = 5000, message = "La respuesta no puede superar los 5000 caracteres")
    private String respuestaOficial;
}