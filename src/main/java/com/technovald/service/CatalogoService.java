package com.technovald.service;

import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.ProductoCatalogoResumenResponse;

import java.util.List;

/** Catálogo público y alta manual de fichas. */
public interface CatalogoService {

    /**
     * Devuelve las fichas activas para las tarjetas del catálogo.
     *
     * @return las fichas activas ordenadas por nombre corto.
     */
    List<ProductoCatalogoResumenResponse> listarProductosCatalogo();

    /**
     * Obtiene la ficha completa de un producto activo.
     *
     * @param ean código de barras del producto.
     * @return la ficha con descripciones, galería, especificaciones y reseñas.
     * @throws com.technovald.exception.ResourceNotFoundException si no existe
     *         una ficha activa con ese EAN.
     */
    ProductoCatalogoDetalleResponse obtenerProductoCatalogoPorEan(String ean);

    /**
     * Crea o actualiza una ficha manualmente (upsert por EAN).
     *
     * @param request datos de la ficha.
     * @return la ficha completa guardada.
     */
    ProductoCatalogoDetalleResponse guardarProductoCatalogo(ProductoCatalogoRequest request);
}
