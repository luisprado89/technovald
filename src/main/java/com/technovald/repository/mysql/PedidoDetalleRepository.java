package com.technovald.repository.mysql;

import com.technovald.entity.PedidoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de líneas de pedido. La relación {@code Pedido -> detalles} no
 * tiene cascada, así que cada línea se persiste por aquí dentro de la
 * transacción del pedido.
 */
@Repository
public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {
}