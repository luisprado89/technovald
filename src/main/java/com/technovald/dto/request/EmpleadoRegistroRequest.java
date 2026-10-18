package com.technovald.dto.request;

import com.technovald.enums.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos que el administrador envía para dar de alta a un empleado
 * ({@code POST /api/admin/empleados}).
 *
 * <p>
 * Repite los datos personales del alta de clientes
 * ({@link ClienteRegistroRequest}) y añade el {@code rol}, que en un empleado
 * solo puede ser {@code ADMIN} o {@code TRABAJADOR}: un empleado nunca es un
 * usuario final. El servicio rechaza {@code CLIENTE} con un error de negocio.
 * </p>
 *
 * <p>
 * Los límites de longitud son los mismos que los del alta de cliente: el mismo
 * dato no puede tener dos políticas distintas según el endpoint que lo escriba.
 * </p>
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EmpleadoRegistroRequest {

    /**
     * Nombre de pila. Se utiliza para generar el username
     * ({@code nombre.apellido}).
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    /**
     * Apellidos. El generador de credenciales usa solo el primero.
     */
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 150, message = "El apellido no puede superar los 150 caracteres")
    private String apellido;

    /**
     * DNI español (8 números + 1 letra mayúscula). Es único en el sistema.
     * El servicio lo normaliza a mayúsculas antes de persistir.
     */
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{8}[A-Z]$",
            message = "El DNI debe tener 8 números seguidos de una letra mayúscula")
    private String dni;

    /**
     * Correo electrónico del empleado. Único en toda la tabla {@code persona}.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    /**
     * Número de teléfono del empleado.
     */
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono;

    /**
     * Dirección postal del empleado.
     */
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;

    /**
     * Rol con el que se crea la cuenta: {@code ADMIN} o {@code TRABAJADOR}.
     * El valor {@code CLIENTE} lo rechaza el servicio.
     */
    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;
}