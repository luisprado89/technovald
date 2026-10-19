package com.technovald.mapper;

import com.technovald.dto.request.ClienteRegistroRequest;
import com.technovald.dto.request.EmpleadoRegistroRequest;
import com.technovald.entity.Persona;
import org.springframework.stereotype.Component;

/**
 * Convierte los DTOs de registro en entidades {@link Persona}.
 *
 * <p>
 * {@code Persona} no se expone por la API: siempre nace como parte del alta de
 * un perfil (hoy, el de cliente o el de empleado), así que su construcción vive
 * aquí y no en el controlador ni en el servicio.
 * </p>
 */
@Component
public class PersonaMapper {

    /**
     * Construye la persona de un cliente a partir del DTO de registro.
     *
     * <p>
     * Recorta los espacios sobrantes de los campos de texto. El DNI y el email
     * no se normalizan aquí: lo hace el servicio, que es quien conoce las
     * reglas de unicidad (mayúsculas en el DNI, minúsculas en el email).
     * </p>
     *
     * @param request datos personales del cliente.
     * @return entidad Persona lista para persistir.
     */
    public Persona toEntity(ClienteRegistroRequest request) {
        return build(request.getNombre(), request.getApellido(), request.getDni(),
                request.getEmail(), request.getTelefono(), request.getDireccion());
    }

    /**
     * Construye la persona de un empleado a partir del DTO de alta.
     *
     * <p>
     * Los datos personales son los mismos que los del cliente; lo que cambia es
     * el perfil que se creará después ({@code empleado} en lugar de
     * {@code cliente}) y que el alta la pide un administrador.
     * </p>
     *
     * @param request datos personales del empleado.
     * @return entidad Persona lista para persistir.
     */
    public Persona toEntity(EmpleadoRegistroRequest request) {
        return build(request.getNombre(), request.getApellido(), request.getDni(),
                request.getEmail(), request.getTelefono(), request.getDireccion());
    }

    /**
     * Construye la persona con los campos comunes a cualquier alta.
     *
     * @param nombre    nombre de pila.
     * @param apellido  apellidos.
     * @param dni       DNI sin normalizar.
     * @param email     email sin normalizar.
     * @param telefono  teléfono.
     * @param direccion dirección postal.
     * @return entidad Persona lista para persistir.
     */
    private Persona build(String nombre, String apellido, String dni, String email,
                          String telefono, String direccion) {
        return Persona.builder()
                .nombre(nombre.trim())
                .apellido(apellido.trim())
                .dni(dni)
                .email(email)
                .telefono(telefono.trim())
                .direccion(direccion.trim())
                .activo(true)
                .build();
    }
}