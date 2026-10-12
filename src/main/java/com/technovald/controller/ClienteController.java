package com.technovald.controller;

import com.technovald.dto.request.ClienteRegistroRequest;
import com.technovald.dto.response.ClienteRegistroResponse;
import com.technovald.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de clientes.
 *
 * <p>
 * Expone el alta pública de clientes. La creación afecta a tres tablas
 * ({@code persona}, {@code usuario} y {@code cliente}), pero el controlador no
 * lo sabe: delega en el servicio y se limita a traducir el resultado a HTTP.
 * </p>
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    /**
     * Servicio con la lógica de alta de clientes.
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
}