package com.technovald.mapper;

import com.technovald.dto.request.ClienteRegistroRequest;
import com.technovald.entity.Persona;
import org.springframework.stereotype.Component;

/**
 * Convierte los DTOs de registro en entidades {@link Persona}.
 *
 * <p>
 * {@code Persona} no se expone por la API: siempre nace como parte del alta de
 * un perfil (hoy, el de cliente), así que su construcción vive aquí y no en el
 * controlador ni en el servicio.
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
        return Persona.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .dni(request.getDni())
                .email(request.getEmail())
                .telefono(request.getTelefono().trim())
                .direccion(request.getDireccion().trim())
                .activo(true)
                .build();
    }
}