package com.technovald.service;

import com.technovald.dto.request.ProveedorRequest;
import com.technovald.dto.response.ProveedorResponse;

import java.util.List;

/** Gestión de proveedores (lectura para el staff, escritura para ADMIN). */
public interface ProveedorService {

    /** Devuelve todos los proveedores, incluidos los inactivos. */
    List<ProveedorResponse> listarProveedores();

    /** Devuelve solo los proveedores activos (para formularios de pedidos). */
    List<ProveedorResponse> listarProveedoresActivos();

    /**
     * Crea un nuevo proveedor.
     *
     * @throws com.technovald.exception.BusinessRuleException si el CIF ya existe.
     */
    ProveedorResponse crearProveedor(ProveedorRequest proveedorRequest);

    /**
     * Actualiza los datos de un proveedor existente.
     *
     * @throws com.technovald.exception.ResourceNotFoundException si no existe.
     * @throws com.technovald.exception.BusinessRuleException si el CIF ya es de otro proveedor.
     */
    ProveedorResponse actualizarProveedor(Long id, ProveedorRequest proveedorRequest);

    /** Baja lógica: desactiva el proveedor sin borrarlo de la base de datos. */
    void desactivarProveedor(Long id);

    /** Reactiva un proveedor previamente dado de baja. */
    void reactivarProveedor(Long id);
}