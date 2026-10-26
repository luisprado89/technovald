package com.technovald.dto.request;

import com.technovald.enums.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos editables de un empleado. El DNI y el username quedan fuera a
 * propósito: identifican a la persona y a la cuenta.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EmpleadoUpdateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 150, message = "El apellido no puede superar los 150 caracteres")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;

    /** {@code ADMIN} o {@code TRABAJADOR}. El servicio rechaza {@code CLIENTE}. */
    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;
}