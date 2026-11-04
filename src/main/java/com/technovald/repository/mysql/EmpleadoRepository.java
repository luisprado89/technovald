package com.technovald.repository.mysql;

import com.technovald.entity.Empleado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Empleado}.
 *
 * <p>
 * Proporciona las consultas del alta y del directorio de empleados. Las dos
 * consultas de lectura declaran {@code @EntityGraph} para que el mapper reciba
 * ya cargadas la persona y su usuario: sin él, cada empleado dispararía dos
 * {@code SELECT} adicionales.
 * </p>
 */
@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    /**
     * Busca un empleado activo por su ID.
     *
     * <p>
     * Es la consulta de {@code GET /api/admin/empleados/{id}}. Solo devuelve
     * perfiles activos, así que un empleado dado de baja responde 404 igual
     * que uno inexistente.
     * </p>
     *
     * @param id id del perfil de empleado.
     * @return el empleado activo, o vacío si no existe.
     */
    @EntityGraph(attributePaths = {"persona", "persona.usuario"})
    Optional<Empleado> findByIdAndActivoTrue(Long id);

    /**
     * Lista todos los empleados ordenados por id ascendente.
     *
     * <p>
     * Es el directorio de {@code GET /api/admin/empleados}. No filtra por
     * {@code activo}: el administrador ve todas las fichas y el DTO expone el
     * estado de cada una.
     * </p>
     *
     * @return los empleados en orden de alta.
     */
    @EntityGraph(attributePaths = {"persona", "persona.usuario"})
    List<Empleado> findAllByOrderByIdAsc();

    /**
     * Busca el perfil de empleado activo de una persona.
     *
     * <p>
     * Es la consulta que usa la creación de solicitudes: del usuario
     * autenticado se pasa a su persona y de ahí a su ficha de empleado.
     * </p>
     *
     * @param personaId id de la persona.
     * @return el empleado activo, o vacío si esa persona no tiene ficha.
     */
    @EntityGraph(attributePaths = {"persona"})
    Optional<Empleado> findByPersonaIdAndActivoTrue(Long personaId);
}