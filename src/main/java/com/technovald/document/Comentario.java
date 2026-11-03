package com.technovald.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Reseña embebida dentro de {@link ProductoDocumento}. No tiene colección
 * propia: viaja en el array {@code comentarios} de la ficha.
 *
 * <p>
 * Guarda el nombre del cliente y del respondedor como copia (snapshot), para
 * que el catálogo no tenga que consultar MySQL. El {@code id} lo genera el
 * servicio con un UUID: al ser un sub-documento, MongoDB no crea ObjectId.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Comentario {

    /** UUID del comentario; permite localizarlo dentro del array. */
    @Id
    @Indexed
    private String id;

    /** Id del cliente en MySQL (referencia lógica, no FK). */
    private Long clienteId;

    /** Nombre del cliente en el momento de escribir la reseña. */
    private String nombreCliente;

    /** Texto de la reseña. */
    private String texto;

    /** Puntuación de 1 a 5 estrellas. */
    private Integer valoracion;

    /** Fecha en la que se escribió. */
    private LocalDateTime fechaCreacion;

    /** Respuesta oficial del staff; null si aún no la hay. */
    private String respuestaOficial;

    /** Nombre del trabajador que respondió. */
    private String nombreRespondedor;

    /** Username del trabajador que respondió. */
    private String usernameRespondedor;

    /** Fecha de la respuesta oficial; null si no hay. */
    private LocalDateTime fechaRespuesta;

    /** Baja lógica: los comentarios inactivos no se muestran. */
    @Builder.Default
    private Boolean activo = true;
}