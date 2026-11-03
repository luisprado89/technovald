package com.technovald.document;

import lombok.*;

/** Característica técnica: nombre y valor, ambos como texto. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EspecificacionItem {

    /** Nombre de la característica, por ejemplo "Cantidad de puertos USB 2.0". */
    private String nombre;

    /** Valor ya formateado por Icecat. */
    private String valor;
}