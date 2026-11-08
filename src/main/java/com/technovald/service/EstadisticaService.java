package com.technovald.service;

import com.technovald.dto.response.ProductoMasVendidoResponse;

import java.util.List;

/** Métricas de ventas. */
public interface EstadisticaService {

    /**
     * Top 5 de productos por unidades vendidas.
     *
     * @return los productos ordenados de más a menos ventas.
     */
    List<ProductoMasVendidoResponse> getProductosMasVendidos();
}