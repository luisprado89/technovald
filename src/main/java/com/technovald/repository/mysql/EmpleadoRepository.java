package com.technovald.repository.mysql;

import com.technovald.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad {@link Empleado}.
 *
 * <p>
 * Proporciona métodos para buscar empleados activos por ID y para
 * verificar si una Persona ya tiene un perfil de empleado asociado.
 * </p>
 */
@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
}
