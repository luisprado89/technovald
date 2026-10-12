package com.technovald.service;

import com.technovald.dto.request.ClienteRegistroRequest;
import com.technovald.dto.request.ClienteUpdateRequest;
import com.technovald.dto.response.ClienteRegistroResponse;
import com.technovald.dto.response.ClienteResponse;

/**
 * Operaciones de gestión de clientes.
 *
 * <p>
 * A diferencia de {@code AuthService}, que solo valida credenciales, aquí vive
 * el alta completa del cliente: perfil de dominio incluido. El registro es un
 * proceso público, así que el servicio es también quien genera las
 * credenciales.
 * </p>
 */
public interface ClienteService {

    /**
     * Registra un cliente nuevo desde la web pública.
     *
     * <p>
     * Crea la persona, su usuario con rol {@code CLIENTE} y su perfil de
     * cliente, y devuelve las credenciales generadas junto con un token JWT.
     * </p>
     *
     * @param clienteRegistroRequest datos personales del cliente.
     * @return las credenciales iniciales, el rol y el token del cliente creado.
     */
    ClienteRegistroResponse registerCliente(ClienteRegistroRequest clienteRegistroRequest);

     /**
     * Devuelve el perfil del cliente autenticado.
     *
     * @param username username del usuario autenticado, leído del token.
     * @return los datos del perfil de cliente.
     * @throws com.technovald.exception.ResourceNotFoundException si el usuario
     *         ya no existe o está inactivo, o si no tiene perfil de cliente
     *         activo.
     */
     ClienteResponse getMyCliente(String username);

     /**
     * Actualiza los datos de contacto del cliente autenticado.
     *
     * @param username username del usuario autenticado, leído del token.
     * @param clienteUpdateRequest  nuevos datos de contacto.
     * @return los datos del perfil ya actualizados.
     * @throws com.technovald.exception.ResourceNotFoundException si el usuario
     *         ya no existe o está inactivo, o si no tiene perfil de cliente
     *         activo.
     * @throws com.technovald.exception.BusinessRuleException si el email nuevo
     *         ya pertenece a otra persona.
     */
     ClienteResponse updateMyCliente(String username, ClienteUpdateRequest clienteUpdateRequest);
}