package com.technovald.repository.mysql;

import com.technovald.entity.AuthCode;
import com.technovald.enums.PropositoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/** Repositorio JPA de los códigos de un solo uso. */
@Repository
public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {

    /** Código vigente más reciente de un usuario para un propósito. */
    Optional<AuthCode> findFirstByUsuarioIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            Long usuarioId, PropositoCodigo purpose);

    /**
     * Consume el código en una sola sentencia.
     *
     * @return 1 si esta petición lo consumió; 0 si ya estaba usado, caducado o sin intentos.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthCode c
               set c.usedAt = :ahora
             where c.id = :id
               and c.usedAt is null
               and c.expiresAt > :ahora
               and c.attempts < c.maxAttempts
            """)
    int consumir(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    /** Marca como usados todos los códigos vigentes del usuario para ese propósito. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthCode c
               set c.usedAt = :ahora
             where c.usuarioId = :usuarioId
               and c.purpose = :purpose
               and c.usedAt is null
            """)
    int invalidarVigentes(@Param("usuarioId") Long usuarioId,
                          @Param("purpose") PropositoCodigo purpose,
                          @Param("ahora") LocalDateTime ahora);

    /** Suma un intento fallido al código. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthCode c
               set c.attempts = c.attempts + 1
             where c.id = :id and c.usedAt is null
            """)
    int incrementarIntentos(@Param("id") Long id);

    /** Borra los códigos caducados o usados anteriores a la fecha indicada. */
    @Modifying
    @Query("delete from AuthCode c where c.expiresAt < :limite or c.usedAt < :limite")
    int purgar(@Param("limite") LocalDateTime limite);
}
