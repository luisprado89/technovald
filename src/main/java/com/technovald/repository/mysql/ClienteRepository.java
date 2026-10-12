package com.technovald.repository.mysql;

import com.technovald.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    /**
     * Busca un perfil de cliente activo por el id de su persona.
     *
     * <p>
     * Es la consulta que usan {@code GET} y {@code PUT /api/clientes/me}: el
     * token identifica al usuario, el usuario a su persona y la persona a su
     * perfil. Solo devuelve perfiles activos, así que un cliente dado de baja
     * no puede seguir consultando ni editando sus datos.
     * </p>
     *
     * @param personaId id de la persona dueña del perfil.
     * @return el perfil de cliente activo, o vacío si esa persona no tiene.
     */
     Optional<Cliente> findByPersonaIdAndActivoTrue(Long personaId);
}