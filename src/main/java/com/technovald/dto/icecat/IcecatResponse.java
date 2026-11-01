package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Respuesta raíz de Icecat. El {@code IcecatClient} la deserializa y el
 * {@code IcecatMapper} la convierte en el documento de catálogo.
 *
 * <p>
 * Solo interesan dos cosas: si la ficha existe ({@code msg}) y el bloque de
 * datos del producto ({@code data}). El resto del JSON se ignora.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatResponse {

    /** Estado de la petición: "OK" cuando Icecat tiene la ficha. */
    private String msg;

    /** Datos del producto; el cliente no lo entrega si viene null. */
    private IcecatData data;
}