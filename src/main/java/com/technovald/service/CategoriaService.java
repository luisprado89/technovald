package com.technovald.service;

import com.technovald.dto.request.CategoriaRequest;
import com.technovald.dto.response.CategoriaResponse;

import java.util.List;

/** Gestión de categorías (lectura para el staff, escritura para ADMIN). */
public interface CategoriaService {

    /** Todas las categorías, incluidas las inactivas. */
    List<CategoriaResponse> listarCategorias();

    /**
     * @throws com.technovald.exception.BusinessRuleException si el nombre ya existe.
     */
    CategoriaResponse crearCategoria(CategoriaRequest categoriaRequest);

    /**
     * @throws com.technovald.exception.ResourceNotFoundException si no existe.
     * @throws com.technovald.exception.BusinessRuleException si el nombre ya es de otra categoría.
     */
    CategoriaResponse actualizarCategoria(Long id, CategoriaRequest categoriaRequest);

    /** Baja lógica. */
    void desactivarCategoria(Long id);

    /** Vuelve a activarla. */
    void reactivarCategoria(Long id);
}