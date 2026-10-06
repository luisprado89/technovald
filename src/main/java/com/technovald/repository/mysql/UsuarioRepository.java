package com.technovald.repository.mysql;

import com.technovald.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Usuario}.
 *
 * <p>
 * Proporciona métodos para verificar la unicidad del username durante
 * la generación automática de credenciales, y para buscar usuarios
 * activos por username (utilizado por {@code CustomUserDetailsService} en el login) o por el email de su
 * Persona asociada (utilizado en "olvidé mi contraseña").
 * </p>
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario activo por su username.
     *
     * <p>
     * Es el método principal utilizado por
     * {@code CustomUserDetailsService} durante el login. Solo
     * devuelve usuarios con {@code activo = true}, por lo que los
     * usuarios desactivados no pueden autenticarse.
     * </p>
     */
    Optional<Usuario> findByUsernameAndActivoTrue(String username);
}
