package com.technovald.repository.mysql;

import com.technovald.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /** Busca un usuario por username, esté activo o no (para activar cuentas pendientes). */
    Optional<Usuario> findByUsername(String username);

    /** Busca el usuario dueño de un email. */
    Optional<Usuario> findByPersonaEmail(String email);

    /**
     * Suma un intento de login fallido de forma atómica.
     *
     * @param id id del usuario.
     * @return filas afectadas.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.intentosFallidosLogin = u.intentosFallidosLogin + 1 where u.id = :id")
    int incrementarIntentosFallidos(@Param("id") Long id);

    /**
     * Bloquea la cuenta si ya alcanzó los cinco intentos fallidos.
     *
     * @param id    id del usuario.
     * @param hasta fin del bloqueo.
     * @return filas afectadas.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Usuario u
               set u.bloqueadoHasta = :hasta
             where u.id = :id and u.intentosFallidosLogin >= 5
            """)
    int bloquearUsuario(@Param("id") Long id, @Param("hasta") LocalDateTime hasta);

    /**
     * Resetea el contador de fallos y limpia el bloqueo.
     *
     * @param id id del usuario.
     * @return filas afectadas.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Usuario u
               set u.intentosFallidosLogin = 0, u.bloqueadoHasta = null
             where u.id = :id
            """)
    int resetearIntentos(@Param("id") Long id);

     /**
     *  Comprueba si ya existe un usuario con el username indicado.
     * 
     *  <p>
     *  Lo usa el generador de credenciales para encontrar un username libre
     *  ({@code jose.garcia}, {@code jose.garcia1}…) durante el registro. La
     *  garantía real de unicidad es la clave {@code uk_usuario_username}.
     *  </p>
     * 
     *  @param username candidato a username.
     *  @return {@code true} si el username ya está ocupado.
     */
     boolean existsByUsername(String username);
}
