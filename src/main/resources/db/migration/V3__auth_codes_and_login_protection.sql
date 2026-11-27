-- ============================================================================
-- V3 · Códigos de un solo uso y protección de login
-- ----------------------------------------------------------------------------
-- Parte de una base de datos vacía: no hay backfill de datos.
-- `activo` sigue siendo la puerta de acceso; `email_verificado` solo indica si
-- la cuenta se confirmó con un código.
-- ============================================================================

ALTER TABLE usuario
    ADD COLUMN email_verificado        bit(1)      NOT NULL DEFAULT b'0' AFTER activo,
    ADD COLUMN intentos_fallidos_login int         NOT NULL DEFAULT 0   AFTER email_verificado,
    ADD COLUMN bloqueado_hasta         datetime(6) NULL                 AFTER intentos_fallidos_login;

-- Códigos de activación y de reseteo. Nunca se guarda el código en claro,
-- solo su hash BCrypt.
CREATE TABLE auth_code
(
    id           bigint       NOT NULL AUTO_INCREMENT,
    usuario_id   bigint       NOT NULL,
    purpose      varchar(30)  NOT NULL,
    code_hash    varchar(255) NOT NULL,
    expires_at   datetime(6)  NOT NULL,
    used_at      datetime(6)  NULL,
    attempts     int          NOT NULL DEFAULT 0,
    max_attempts int          NOT NULL DEFAULT 5,
    created_at   datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_auth_code_usuario_purpose (usuario_id, purpose, used_at),
    KEY idx_auth_code_expires (expires_at),
    CONSTRAINT fk_auth_code_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_auth_code_purpose
        CHECK (purpose IN ('ACTIVACION_CUENTA', 'RESET_PASSWORD')),
    CONSTRAINT ck_auth_code_attempts CHECK (attempts >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
