package com.technovald.enums;

/**
 * Enumeración que define los estados del ciclo de vida de una solicitud en TechNovaLD
 * <p>
 * Una solicitud de reposición se crea cuando un trabajador detecta
 * que el stock de un producto ha bajado del mínimo y solicita al
 * proveedor correspondiente que envíe más unidades.
 * </p>
 *
 * <p>
 * El flujo de estados es lineal y muy simple:
 * </p>
 *
 * <pre>
 * PENDIENTE → RECIBIDO
 * </pre>
 *
 * <p>Este enum se persiste como texto literal en MySQL mediante
 * {@code @Enumerated(EnumType.STRING)}, lo que permite consultar
 * solicitudes por estado de forma legible directamente desde SQL.
 * </p>
 */
public enum EstadoSolicitud {
    /**
     * Estado inicial. La solicitud ha sido creada por un trabajador.
     *
     * <p>
     * El producto {@code ProductoInventario} tiene su stock por debajo
     * del mínimo ({@code stock < stockMinimo}) y se ha solicitado al
     * proveedor una cantidad determinada para reponerlo.
     * </p>
     *
     * <p>
     * En este estado el stock del producto todavía NO se ha modificado.
     * Solo se incrementará cuando la solicitud pase a {@code RECIBIDO}.
     * </p>
     */
    PENDIENTE,
    /**
     * Estado terminal. La mercancía ha llegado al almacén.
     *
     * <p>
     * El proveedor ha entregado las unidades solicitadas. En este
     * momento el sistema debe incrementar el stock del
     * {@code ProductoInventario} asociado en la cantidad indicada
     * en la solicitud.
     * </p>
     *
     * <p>
     * A partir de este estado la solicitud no puede cambiar.
     * </p>
     */
    RECIBIDO
}
