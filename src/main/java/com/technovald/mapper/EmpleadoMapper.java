package com.technovald.mapper;

import com.technovald.dto.response.EmpleadoResponse;
import com.technovald.entity.Empleado;
import com.technovald.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte entidades {@link Empleado} en el DTO de respuesta
 * {@link EmpleadoResponse}.
 *
 * <p>
 * Aplana la cadena {@code empleado → persona → usuario}: los datos personales
 * salen de la persona, el username del usuario asociado y el rol del propio
 * empleado. El cliente de la API nunca ve las entidades, así que el día que el
 * modelo gane relaciones o campos internos la forma de la respuesta no cambia
 * sola.
 * </p>
 */
@Component
public class EmpleadoMapper {

    /**
     * Aplana un empleado en un {@link EmpleadoResponse}.
     *
     * <p>
     * Se invoca dentro de la transacción del servicio. Las consultas del
     * repositorio llevan {@code @EntityGraph}, así que {@code persona} y
     * {@code persona.usuario} llegan ya cargados y el mapper no dispara
     * consultas perezosas.
     * </p>
     *
     * @param empleado entidad que se quiere convertir.
     * @return el DTO con los datos del empleado.
     */
    public EmpleadoResponse toResponse(Empleado empleado) {
        Usuario usuario = empleado.getPersona().getUsuario();

        return EmpleadoResponse.builder()
                .id(empleado.getId())
                .nombre(empleado.getPersona().getNombre())
                .apellido(empleado.getPersona().getApellido())
                .email(empleado.getPersona().getEmail())
                .telefono(empleado.getPersona().getTelefono())
                .direccion(empleado.getPersona().getDireccion())
                .username(usuario == null ? null : usuario.getUsername())
                .rol(empleado.getRol().name())
                .activo(empleado.getActivo())
                .build();
    }

    /**
     * Convierte una lista de empleados conservando el orden recibido.
     *
     * @param empleados empleados que se quieren convertir.
     * @return la lista de DTOs, en el mismo orden.
     */
    public List<EmpleadoResponse> toResponseList(List<Empleado> empleados) {
        return empleados.stream()
                .map(this::toResponse)
                .toList();
    }
}