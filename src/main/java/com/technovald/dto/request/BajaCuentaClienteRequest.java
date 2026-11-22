package com.technovald.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Motivo que el cliente indica al solicitar la baja de su propia cuenta.
 *
 * <p>La cuenta se desactiva, pero no se eliminan sus datos. El motivo se usa
 * para la notificación por correo y para el registro de la operación.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BajaCuentaClienteRequest {

    /**
     * Motivo de la baja indicado por el cliente.
     */
    @NotBlank(message = "El motivo de la baja es obligatorio")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String motivo;
}
