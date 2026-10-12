package com.technovald.mapper;

import com.technovald.dto.response.ClienteResponse;
import com.technovald.entity.Cliente;
import org.springframework.stereotype.Component;


/**
 * Convierte entidades {@link Cliente} en el DTO de respuesta
 * {@link ClienteResponse}.
 *
 * <p>
 * El cliente de la API nunca ve la entidad: ve este DTO. Así, el día que el
 * modelo gane relaciones o campos internos, la forma de la respuesta no cambia
 * sola.
 * </p>
 */
@Component
public class ClienteMapper {

    /**
     * Aplana un cliente y su persona en un {@link ClienteResponse}.
     *
     * <p>
     * Debe invocarse con la transacción abierta: {@code persona} se carga de forma perezosa.
     * </p>
     *
     * @param cliente entidad que se quiere convertir.
     * @return el DTO con los datos del perfil.
     */
    public ClienteResponse toResponse(Cliente cliente) {
        return ClienteResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getPersona().getNombre())
                .apellido(cliente.getPersona().getApellido())
                .email(cliente.getPersona().getEmail())
                .telefono(cliente.getPersona().getTelefono())
                .direccion(cliente.getPersona().getDireccion())
                .activo(cliente.getActivo())
                .build();
    }
}
