package com.technovald.repository.mysql;

import com.technovald.entity.PedidoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de líneas de pedido. La relación {@code Pedido -> detalles} no
 * tiene cascada, así que cada línea se persiste por aquí dentro de la
 * transacción del pedido.
 */
@Repository
public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {

    /**
     * Calcula el Top de productos por unidades vendidas.
     *
     * <p>
     * Solo cuentan las líneas activas de pedidos activos. La agrupación incluye
     * id, nombre corto y marca para cumplir {@code ONLY_FULL_GROUP_BY} de MySQL 8.
     * </p>
     *
     * @return proyecciones ordenadas de más a menos unidades vendidas.
     */
    @Query("""
            SELECT pd.producto.nombreCorto AS nombreCorto,
                   pd.producto.marca AS marca,
                   SUM(pd.cantidad) AS cantidadTotal
            FROM PedidoDetalle pd
            WHERE pd.activo = true AND pd.pedido.activo = true
            GROUP BY pd.producto.id, pd.producto.nombreCorto, pd.producto.marca
            ORDER BY SUM(pd.cantidad) DESC
            """)
    List<ProductoMasVendidoProjection> findTopProductosMasVendidos();
}