package com.technovald.dto.response;

import java.util.List;

/**
 * Resumen de una sincronización masiva con Icecat. El proceso no se
 * detiene en el primer fallo: cuenta los éxitos y devuelve los errores
 * para que el ADMIN sepa qué EAN hay que revisar.
 */
public record SincronizacionResponse(
        /** Productos del inventario recorridos. */
        int total,
        /** Productos cuya ficha se guardó o actualizó. */
        int sincronizados,
        /** Productos que fallaron (sin ficha, sin inventario, HTTP...). */
        int fallidos,
        /** Detalle de cada fallo, con formato "EAN: motivo". */
        List<String> errores
) {
}