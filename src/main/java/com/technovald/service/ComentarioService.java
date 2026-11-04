package com.technovald.service;

import com.technovald.dto.request.ComentarioRequest;
import com.technovald.dto.request.RespuestaComentarioRequest;
import com.technovald.dto.response.ComentarioPendienteResponse;
import com.technovald.dto.response.ComentarioResponse;

import java.util.List;

/** Reseñas de clientes y respuestas del staff en el catálogo. */
public interface ComentarioService {

    /**
     * Añade una reseña a la ficha del producto.
     *
     * @param comentarioRequest  producto, texto y valoración.
     * @param username username del cliente autenticado.
     * @return la reseña creada.
     */
    ComentarioResponse crearComentario(ComentarioRequest comentarioRequest, String username);

    /**
     * Añade o sustituye la respuesta oficial de una reseña.
     *
     * @param comentarioId UUID de la reseña.
     * @param respuestaComentarioRequest      texto de la respuesta.
     * @param username     username del staff autenticado.
     * @return la reseña actualizada.
     */
    ComentarioResponse responderComentario(String comentarioId,
                                           RespuestaComentarioRequest respuestaComentarioRequest,
                                           String username);

    /**
     * Reseñas activas de un producto, en orden de llegada.
     *
     * @param productoInventarioId id del producto en MySQL.
     * @return lista de reseñas.
     */
    List<ComentarioResponse> getComentariosByProducto(Long productoInventarioId);

    /**
     * Reseñas que todavía no tienen respuesta oficial.
     *
     * @return filas con el producto de origen ya resuelto.
     */
    List<ComentarioPendienteResponse> getComentariosPendientes();
}