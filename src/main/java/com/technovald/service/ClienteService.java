package com.technovald.service;

import com.technovald.dto.request.ClienteRegistroRequest;
import com.technovald.dto.response.ClienteRegistroResponse;

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
     * @param request datos personales del cliente.
     * @return las credenciales iniciales, el rol y el token del cliente creado.
     */
    ClienteRegistroResponse registerCliente(ClienteRegistroRequest request);
}