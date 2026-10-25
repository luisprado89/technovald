package com.technovald.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Cambio de contraseña de un usuario autenticado. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    /** Contraseña actual para verificar la identidad. */
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    /** Nueva contraseña elegida por el usuario. */
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 64, message = "La nueva contraseña debe tener entre 8 y 64 caracteres")
    private String newPassword;
}