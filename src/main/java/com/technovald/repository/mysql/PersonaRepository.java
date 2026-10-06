package com.technovald.repository.mysql;

import com.technovald.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad {@link Persona}.
 *
 * <p>
 * Proporciona métodos para verificar la unicidad del DNI y email
 * durante el registro de clientes y empleados, y para buscar personas
 * activas por ID o por email (este último utilizado en el proceso
 * de "olvidé mi contraseña").
 * </p>
 */
@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

}
