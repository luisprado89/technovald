package com.technovald.repository.mysql;

import com.technovald.entity.ProductoInventario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repositorio JPA para {@link ProductoInventario}. */
@Repository
public interface ProductoInventarioRepository extends JpaRepository<ProductoInventario, Long> {

    /** Lista todos los productos (activos e inactivos) ordenados por nombre. */
    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    List<ProductoInventario> findAllByOrderByNombreCortoAsc();

    /** Lista solo los productos activos con stock igual o inferior al mínimo. */
    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    @Query("""
            select p from ProductoInventario p
            where p.activo = true and p.stockActual <= p.stockMinimo
            order by p.nombreCorto
            """)
    List<ProductoInventario> findLowStock();

    /** Comprueba si ya existe un producto con ese EAN. */
    boolean existsByEanIgnoreCase(String ean);

    /** Comprueba si el EAN pertenece a otro producto distinto (para la edición). */
    boolean existsByEanIgnoreCaseAndIdNot(String ean, Long id);
}