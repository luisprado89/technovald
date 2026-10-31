package com.technovald.service;

import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ProductoCatalogoResponse;

import java.util.List;

/** Gestión del catálogo público de productos (MongoDB). */
public interface CatalogoService {

    /** Devuelve todas las fichas del catálogo, enriquecidas con precios y stock de MySQL. */
    List<ProductoCatalogoResponse> listarProductosCatalogo();

    /**
     * Obtiene una ficha por su EAN.
     *
     * @throws com.technovald.exception.ResourceNotFoundException si no existe ninguna ficha con ese EAN.
     */
    ProductoCatalogoResponse obtenerProductoCatalogoPorEan(String ean);


    /**
     * Crea o actualiza una ficha de catálogo.
     * Si el EAN ya existe, actualiza la ficha; si no, la crea (upsert).
     */
    ProductoCatalogoResponse guardarProductoCatalogo(ProductoCatalogoRequest productoCatalogoRequest);
}