package com.technovald.enums;

/**
 * Propósito de un código de un solo uso.
 *
 * <p>Se persiste como texto con {@code @Enumerated(EnumType.STRING)}.</p>
 */
public enum PropositoCodigo {

    /** Activa una cuenta recién creada y fija su contraseña. */
    ACTIVACION_CUENTA,

    /** Restablece la contraseña de una cuenta ya activa. */
    RESET_PASSWORD
}
