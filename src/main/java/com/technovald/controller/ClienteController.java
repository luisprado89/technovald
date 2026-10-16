package com.technovald.controller;

import com.technovald.dto.request.ClienteRegistroRequest;
import com.technovald.dto.request.ClienteUpdateRequest;
import com.technovald.dto.response.ClienteRegistroResponse;
import com.technovald.dto.response.ClienteResponse;
import com.technovald.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de clientes.
 *
 * <p>
 * Expone el alta pública de clientes y la gestión del propio perfil. La
 * creación afecta a tres tablas ({@code persona}, {@code usuario} y
 * {@code cliente}), pero el controlador no lo sabe: delega en el servicio y se
 * limita a traducir el resultado a HTTP.
 * </p>
 *
 * <p>
 * Las rutas {@code /me} no llevan id en la URL a propósito: el cliente se
 * identifica con el token, así que no puede pedir el perfil de otro ni
 * intentarlo.
 * </p>
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    /**
     * Servicio con la lógica de alta y de perfil de clientes.
     */
    private final ClienteService clienteService;

    /**
     * Registra un cliente nuevo.
     *
     * <p>
     * Es una ruta pública: quien se registra todavía no tiene credenciales con
     * las que autenticarse. Devuelve {@code 201} sin cabecera {@code Location}
     * porque todavía no existe un endpoint que consulte un cliente por su id:
     * apuntar a una ruta que responde 404 sería peor que no apuntar a ninguna.
     * </p>
     *
     * @param request datos personales del cliente.
     * @return las credenciales generadas y el token del cliente creado.
     */
    @PostMapping("/register")
    public ResponseEntity<ClienteRegistroResponse> register(
            @Valid @RequestBody ClienteRegistroRequest request) {

        ClienteRegistroResponse response = clienteService.registerCliente(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Devuelve el perfil del cliente autenticado.
     *
     * <p>
     * Exige token: la ruta no está en la lista de públicas, así que cae en la
     * regla general de la cadena de seguridad. El usuario autenticado se toma
     * del contexto de seguridad que dejó el {@code JwtAuthenticationFilter}.
     * </p>
     *
     * @param userDetails usuario autenticado, con el username del token.
     * @return los datos del perfil del cliente.
     */
    @GetMapping("/me")
    public ResponseEntity<ClienteResponse> getMyCliente(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(clienteService.getMyCliente(userDetails.getUsername()));
    }

    /**
     * Actualiza los datos de contacto del cliente autenticado.
     *
     * <p>
     * Solo se pueden cambiar el email, el teléfono y la dirección: el nombre,
     * el apellido y el DNI identifican a la persona y no se tocan por aquí.
     * </p>
     *
     * @param userDetails usuario autenticado, con el username del token.
     * @param request     datos de contacto nuevos.
     * @return los datos del perfil ya actualizados.
     */
    @PutMapping("/me")
    public ResponseEntity<ClienteResponse> updateMyCliente(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ClienteUpdateRequest request) {

        return ResponseEntity.ok(
                clienteService.updateMyCliente(userDetails.getUsername(), request));
    }
}