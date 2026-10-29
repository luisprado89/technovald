package com.technovald.service;

import com.technovald.dto.request.ProductoInventarioRequest;
import com.technovald.dto.request.ProductoInventarioAjusteRequest;
import com.technovald.dto.response.ProductoInventarioResponse;

import java.util.List;

/** Gestión del inventario. */
public interface InventarioService {

    /** Devuelve todos los productos, incluidos los inactivos. */
    List<ProductoInventarioResponse> listarProductos();

    /** Devuelve solo los productos activos con stock igual o inferior al mínimo. */
    List<ProductoInventarioResponse> listarStockBajo();

    /**
     * Crea un nuevo producto.
     *
     * @throws com.technovald.exception.BusinessRuleException     si el EAN ya existe.
     * @throws com.technovald.exception.ResourceNotFoundException si la categoría o proveedor no existen.
     */
    ProductoInventarioResponse crearProducto(ProductoInventarioRequest productoInventarioRequest);

    /**
     * Actualiza los datos del producto (excepto el EAN y el stock).
     *
     * @throws com.technovald.exception.ResourceNotFoundException si el producto, categoría o proveedor no existen.
     */
    ProductoInventarioResponse actualizarProducto(Long id, ProductoInventarioRequest productoInventarioRequest);

    /**
     * Suma la cantidad indicada al stock actual (positiva para entrada, negativa para salida).
     *
     * @throws com.technovald.exception.BusinessRuleException si el stock resultante fuese negativo.
     */
    ProductoInventarioResponse ajustarStockProducto(Long id, ProductoInventarioAjusteRequest productoInventarioAjusteRequest);

    /** Baja lógica: desactiva el producto sin borrarlo de la base de datos. */
    void desactivarProducto(Long id);

    /** Reactiva un producto previamente dado de baja. */
    void reactivarProducto(Long id);
}