package com.technovald.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos de contacto que un cliente autenticado puede cambiar de su propio perfil ({@code PUT /api/clientes/me}).
 *
 * <p>
 * Solo incluye los campos modificables. El nombre, el apellido y el DNI quedan fuera a propósito: identifican a la
 * persona y no son datos de contacto, así que cambiarlos no es actualizar un perfil sino cambiar de identidad, algo
 * que esta operación no cubre.
 * </p>
 *
 * <p>
 * Los límites de longitud son los mismos que los del alta de cliente ({@link ClienteRegistroRequest}): el mismo dato
 * no puede tener dos políticas distintas según el endpoint que lo escriba.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClienteUpdateRequest {

    /**
     * Nuevo email de contacto. Debe tener formato válido y no pertenecer a
     * otra persona: la columna tiene una clave única.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    /**
     * Nuevo teléfono de contacto.
     */
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono;

    /**
     * Nueva dirección postal.
     */
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;
}
