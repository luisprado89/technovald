package com.technovald.service;

import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.SincronizacionResponse;

/** Sincronización del catálogo con Icecat. */
public interface IcecatService {

    /**
     * Sincroniza una ficha por EAN; el EAN debe existir en el inventario.
     *
     * @param ean código de barras del producto.
     * @return la ficha completa guardada.
     * @throws com.technovald.exception.ResourceNotFoundException si el EAN no
     *         está en MySQL.
     * @throws com.technovald.exception.IcecatApiException si Icecat falla.
     */
    ProductoCatalogoDetalleResponse sincronizarProducto(String ean);

    /**
     * Recorre todo el inventario y sincroniza cada producto. Los fallos se
     * acumulan y no cortan el proceso.
     *
     * @return el resumen con total, sincronizados, fallidos y errores.
     */
    SincronizacionResponse sincronizarInventario();
}
