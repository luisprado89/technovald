package com.technovald.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Datos de entrada para crear o editar un proveedor. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProveedorRequest {

    /** Nombre de la empresa proveedora (ej. "ASUS España"). */
    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 255, message = "El nombre de la empresa no puede superar los 255 caracteres")
    private String nombreEmpresa;

    /** CIF de la empresa (único en el sistema). */
    @NotBlank(message = "El CIF es obligatorio")
    @Size(max = 20, message = "El CIF no puede superar los 20 caracteres")
    private String cif;

    /** Persona de contacto en la empresa proveedora. */
    @NotBlank(message = "El contacto es obligatorio")
    @Size(max = 255, message = "El contacto no puede superar los 255 caracteres")
    private String contacto;

    /** Teléfono de contacto de la empresa. */
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono;

    /** Correo electrónico de contacto de la empresa. */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    /** Dirección física o fiscal de la empresa. */
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;
}