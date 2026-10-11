package com.technovald.repository.mysql;

import com.technovald.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad {@link Cliente}.
 *
 * <p>
 * El alta de un cliente solo necesita el {@code save()} que ya hereda de
 * {@link JpaRepository}: la fila de {@code cliente} únicamente guarda la
 * relación con su {@code Persona} y el estado lógico. No declara consultas
 * propias porque todavía no hay ninguna que las use.
 * </p>
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}