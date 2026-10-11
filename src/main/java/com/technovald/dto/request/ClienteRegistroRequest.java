package com.technovald.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClienteRegistroRequest {

    /**
     * Nombre de pila del cliente.
     * Se utiliza para generar el username ({@code nombre.apellido}).
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    /**
     * Apellidos del cliente.
     * Se utiliza para generar el username ({@code nombre.apellido}).
     */
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 150, message = "El apellido no puede superar los 150 caracteres")
    private String apellido;

    /**
     * DNI español (8 números + 1 letra mayúscula), (ej: {@code 12345678Z}). Es único en el sistema.
     * El servicio lo normaliza a mayúsculas antes de persistir.
     */
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{8}[A-Z]$",
            message = "El DNI debe tener 8 números seguidos de una letra mayúscula")
    private String dni;

    /**
     * Correo electrónico del cliente.
     * Se utiliza en el proceso de "olvidé mi contraseña".
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    /**
     * Número de teléfono del cliente.
     */
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono;

    /**
     * Dirección postal del cliente.
     */
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;
}
