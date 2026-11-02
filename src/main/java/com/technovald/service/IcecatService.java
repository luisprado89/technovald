package com.technovald.service;

import com.technovald.dto.response.ProductoCatalogoResponse;
import com.technovald.dto.response.SincronizacionResponse;

/** Sincronización del catálogo (MongoDB) con Icecat. */
public interface IcecatService {

    /**
     * Sincroniza una ficha por EAN. Exige que el EAN exista en el inventario.
     *
     * @throws com.technovald.exception.ResourceNotFoundException si el EAN no está en MySQL.
     * @throws com.technovald.exception.IcecatApiException si Icecat falla.
     */
    ProductoCatalogoResponse sincronizarProducto(String ean);

    /** Recorre todo el inventario y sincroniza; los fallos no cortan el proceso. */
    SincronizacionResponse sincronizarInventario();
}