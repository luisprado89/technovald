package com.technovald.entity;

import com.technovald.enums.PropositoCodigo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Código de un solo uso para activar una cuenta o restablecer la contraseña.
 *
 * <p>Se guarda el hash BCrypt del código, nunca el código en claro.</p>
 */
@Entity
@Table(name = "auth_code", indexes = {
        @Index(name = "idx_auth_code_usuario_purpose", columnList = "usuario_id, purpose, used_at"),
        @Index(name = "idx_auth_code_expires", columnList = "expires_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuthCode {

    /** Identificador del código. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK a {@code usuario}; se mapea como columna suelta para no cargar la entidad. */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Propósito del código. */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", columnDefinition = "VARCHAR(30)", nullable = false)
    private PropositoCodigo purpose;

    /** Hash BCrypt del código. */
    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    /** Momento en el que deja de ser válido. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Momento en el que se consumió; {@code null} si sigue vigente. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** Intentos fallidos acumulados. */
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    /** Intentos permitidos antes de quemar el código. */
    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 5;

    /** Fecha de emisión. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
