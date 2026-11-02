package com.technovald.repository.mysql;

import com.technovald.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de pedidos. Los listados filtran por {@code activo} y traen
 * {@code cliente.persona} en la misma consulta; las líneas se cargan en lote.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Busca un pedido activo por id. */
    Optional<Pedido> findByIdAndActivoTrue(Long id);

    /** Lista los pedidos activos del más reciente al más antiguo. */
    @EntityGraph(attributePaths = {"cliente.persona"})
    Page<Pedido> findAllByActivoTrueOrderByIdDesc(Pageable pageable);

    /** Lista los pedidos activos de un cliente, del más reciente al más antiguo. */
    @EntityGraph(attributePaths = {"cliente.persona"})
    Page<Pedido> findAllByClienteIdAndActivoTrueOrderByIdDesc(Long clienteId, Pageable pageable);
}