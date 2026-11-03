package com.technovald.mapper;

import com.technovald.document.Comentario;
import com.technovald.dto.response.ComentarioResponse;
import org.springframework.stereotype.Component;

/** Convierte reseñas embebidas en DTOs de respuesta. */
@Component
public class ComentarioMapper {

    /**
     * Convierte una reseña.
     *
     * @param comentario sub-documento de MongoDB.
     * @return el DTO con la reseña y, si existe, su respuesta.
     */
    public ComentarioResponse toResponse(Comentario comentario) {
        return ComentarioResponse.builder()
                .id(comentario.getId())
                .clienteId(comentario.getClienteId())
                .nombreCliente(comentario.getNombreCliente())
                .texto(comentario.getTexto())
                .valoracion(comentario.getValoracion())
                .fechaCreacion(comentario.getFechaCreacion())
                .respuestaOficial(comentario.getRespuestaOficial())
                .nombreRespondedor(comentario.getNombreRespondedor())
                .usernameRespondedor(comentario.getUsernameRespondedor())
                .fechaRespuesta(comentario.getFechaRespuesta())
                .build();
    }
}