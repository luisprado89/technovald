package com.technovald.repository.mysql;

import com.technovald.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repositorio JPA para {@link Proveedor}. */
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /** Lista todos los proveedores (activos e inactivos) ordenados por nombre de empresa. */
    List<Proveedor> findAllByOrderByNombreEmpresaAsc();

    /** Lista solo los proveedores activos, para los desplegables de pedidos y reposición. */
    List<Proveedor> findAllByActivoTrueOrderByNombreEmpresaAsc();

    /** Comprueba si un CIF ya está registrado (ignorando mayúsculas/minúsculas). */
    boolean existsByCifIgnoreCase(String cif);

    /** Comprueba si un CIF ya está registrado en otro proveedor distinto (para la edición). */
    boolean existsByCifIgnoreCaseAndIdNot(String cif, Long id);
}