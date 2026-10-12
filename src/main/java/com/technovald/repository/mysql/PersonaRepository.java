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

/**
* Comprueba si ya existe una persona con el DNI indicado.
*
* <p>
* Se usa en el registro para rechazar el alta con un 400 de negocio antes
* de intentar el INSERT. La garantía real de unicidad es la clave
* {@code uk_persona_dni} de la tabla.
* </p>
*
* @param dni DNI ya normalizado (sin espacios y en mayúsculas).
* @return {@code true} si el DNI ya está registrado.
*/
   boolean existsByDni(String dni);

/**
* Comprueba si ya existe una persona con el email indicado.
*
* @param email email ya normalizado (sin espacios y en minúsculas).
* @return {@code true} si el email ya está registrado.
*/
    boolean existsByEmail(String email);
}
