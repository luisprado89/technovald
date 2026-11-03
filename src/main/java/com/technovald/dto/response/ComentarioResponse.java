package com.technovald.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** Reseña tal y como se devuelve en la ficha del producto. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ComentarioResponse {

    /** UUID del comentario. */
    private String id;

    /** Id del cliente que lo escribió. */
    private Long clienteId;

    /** Nombre del cliente (snapshot). */
    private String nombreCliente;

    /** Texto de la reseña. */
    private String texto;

    /** Estrellas de 1 a 5. */
    private Integer valoracion;

    /** Fecha de creación. */
    private LocalDateTime fechaCreacion;

    /** Respuesta oficial, si existe. */
    private String respuestaOficial;

    /** Nombre del trabajador que respondió. */
    private String nombreRespondedor;

    /** Username del trabajador que respondió. */
    private String usernameRespondedor;

    /** Fecha de la respuesta. */
    private LocalDateTime fechaRespuesta;
}