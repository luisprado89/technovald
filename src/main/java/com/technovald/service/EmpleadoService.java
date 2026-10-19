package com.technovald.service;

import com.technovald.dto.request.EmpleadoRegistroRequest;
import com.technovald.dto.response.EmpleadoRegistroResponse;
import com.technovald.dto.response.EmpleadoResponse;

import java.util.List;

/**
 * Operaciones de gestión de empleados.
 *
 * <p>
 * Estas operaciones las ejecuta el administrador, así que viven separadas de
 * {@link ClienteService}: un cliente se auto-registra, mientras que un empleado
 * lo da de alta otro usuario. Que solo el rol {@code ADMIN} pueda llegar aquí
 * lo decide la cadena de seguridad de
 * {@code com.technovald.config.SecurityConfig}, no el servicio.
 * </p>
 */
public interface EmpleadoService {

    /**
     * Da de alta un empleado con el rol indicado.
     *
     * <p>
     * Crea la persona, su usuario con el rol recibido y su perfil de empleado,
     * y devuelve las credenciales generadas. No emite token: quien llama ya
     * está autenticado.
     * </p>
     *
     * @param empleadoRegistroRequest datos personales y rol del empleado.
     * @return las credenciales iniciales y el rol del empleado creado.
     * @throws com.technovald.exception.BusinessRuleException si el rol no es
     *         {@code ADMIN} ni {@code TRABAJADOR}, o si el DNI o el email ya
     *         existen.
     */
    EmpleadoRegistroResponse createEmpleado(EmpleadoRegistroRequest empleadoRegistroRequest);

    /**
     * Devuelve el directorio completo de empleados, ordenado por id.
     *
     * @return todos los empleados, en orden de alta.
     */
    List<EmpleadoResponse> getAllEmpleados();

    /**
     * Devuelve un empleado activo por su identificador.
     *
     * @param id identificador del perfil de empleado.
     * @return los datos del empleado.
     * @throws com.technovald.exception.ResourceNotFoundException si no existe
     *         ningún empleado activo con ese id.
     */
    EmpleadoResponse getEmpleadoById(Long id);
}