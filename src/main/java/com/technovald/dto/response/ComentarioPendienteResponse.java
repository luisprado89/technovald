package com.technovald.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fila de la bandeja de comentarios sin respuesta. Añade el producto de
 * origen para que el staff sepa a qué ficha ir.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ComentarioPendienteResponse {

    /** UUID del comentario. */
    private String comentarioId;

    /** EAN del producto; solo se usa para resolver el id de MySQL. */
    @JsonIgnore
    private String productoEan;

    /** Id del producto en MySQL. */
    private Long productoId;

    /** Nombre del producto en el catálogo. */
    private String productoTitulo;

    /** Nombre del cliente (snapshot). */
    private String nombreCliente;

    /** Texto de la reseña. */
    private String texto;

    /** Estrellas de 1 a 5. */
    private Integer valoracion;

    /** Fecha de creación. */
    private LocalDateTime fechaCreacion;
}