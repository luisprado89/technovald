package com.technovald.repository.mysql;

import com.technovald.entity.ProductoInventario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /** Busca un producto del inventario por su EAN. */
    Optional<ProductoInventario> findByEanIgnoreCase(String ean);

    /** Busca una lista de productos por sus EANs (para enriquecer el catálogo). */
    List<ProductoInventario> findAllByEanIn(Collection<String> eans);

    /** Busca un producto activo por id (para crear pedidos). */
    Optional<ProductoInventario> findByIdAndActivoTrue(Long id);

    /**
     * Descuenta stock en una sola sentencia. Devuelve 0 si no hay stock
     * suficiente o el producto está inactivo.
     */
    @Modifying
    @Query("""
            update ProductoInventario p
            set p.stockActual = p.stockActual - :cantidad
            where p.id = :id and p.activo = true and p.stockActual >= :cantidad
            """)
    int decrementarStockAtomico(@Param("id") Long id, @Param("cantidad") Integer cantidad);

    /** Productos activos por lista de EAN (para resolver la bandeja de reseñas). */
    List<ProductoInventario> findAllByEanInAndActivoTrue(Collection<String> eans);
}